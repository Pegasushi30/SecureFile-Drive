package com.example.securedrive.service.impl;

import com.example.securedrive.dto.DirectoryDto;
import com.example.securedrive.exception.AzureBlobStorageException;
import com.example.securedrive.exception.DirectoryNotFoundException;
import com.example.securedrive.mapper.DirectoryMapper;
import com.example.securedrive.model.Directory;
import com.example.securedrive.model.File;
import com.example.securedrive.model.User;
import com.example.securedrive.repository.DirectoryRepository;
import com.example.securedrive.repository.FileRepository;
import com.example.securedrive.repository.FileVersionRepository;
import com.example.securedrive.service.DirectoryService;
import com.example.securedrive.service.IAzureBlobStorage;
import com.example.securedrive.service.UserManagementService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DirectoryServiceImpl implements DirectoryService {

    private static final Logger logger = LoggerFactory.getLogger(DirectoryServiceImpl.class);

    private final DirectoryRepository directoryRepository;
    private final IAzureBlobStorage azureBlobStorage;
    private final FileRepository fileRepository;
    private final FileVersionRepository fileVersionRepository;
    private final DirectoryMapper directoryMapper;
    private final UserManagementService userManagementService;

    @Autowired
    public DirectoryServiceImpl(DirectoryRepository directoryRepository,
                                IAzureBlobStorage azureBlobStorage,
                                FileRepository fileRepository,
                                FileVersionRepository fileVersionRepository,
                                DirectoryMapper directoryMapper,
                                UserManagementService userManagementService) {
        this.directoryRepository = directoryRepository;
        this.azureBlobStorage = azureBlobStorage;
        this.fileRepository = fileRepository;
        this.fileVersionRepository = fileVersionRepository;
        this.directoryMapper = directoryMapper;
        this.userManagementService = userManagementService;
    }

    @Override
    public List<Directory> getRootDirectories(User user) {
        return directoryRepository.findAllByUserAndParentDirectoryIsNull(user);
    }

    @Override
    public List<Directory> getSubDirectories(User user, Directory parentDirectory) {
        return directoryRepository.findAllByUserAndParentDirectory(user, parentDirectory);
    }

    @Override
    public void saveDirectory(Directory directory) {
        directoryRepository.save(directory);

        String path;
        if (directory.getParentDirectory() != null) {
            path = "directories/" + directory.getUser().getUsername() + "/" + buildDirectoryPath(directory);
        } else {
            path = "directories/" + directory.getUser().getUsername() + "/" + directory.getName() + "/";
        }

        try {
            azureBlobStorage.createDirectory(path);
            logger.info("Directory created with marker at path: {}", path);
        } catch (AzureBlobStorageException e) {
            logger.error("Blob Storage dizin oluşturulurken hata oluştu: {}", e.getMessage());
            throw new RuntimeException("Blob Storage dizin oluşturulurken hata oluştu: " + e.getMessage());
        }
    }

    private String buildDirectoryPath(Directory directory) {
        StringBuilder path = new StringBuilder();
        Directory current = directory.getParentDirectory(); // Root dizin hariç
        while (current != null) {
            path.insert(0, current.getName() + "/");
            current = current.getParentDirectory();
        }
        return path.toString();
    }

    @Override
    public Optional<Directory> findByIdAndUser(Long id, User user) {
        return directoryRepository.findByIdAndUser(id, user);
    }

    @Override
    public List<Directory> getUserDirectories(User user) {
        return directoryRepository.findAllByUser(user);
    }

    @Override
    public Optional<Directory> getDirectoryById(Long id) {
        return directoryRepository.findById(id);
    }

    // DTO bazlı metotlar
    @Override
    public List<DirectoryDto> getRootDirectoriesAsDto(User user) {
        return getRootDirectories(user).stream().map(directoryMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<DirectoryDto> getSubDirectoriesAsDto(User user, Long parentDirectoryId) {
        Directory parentDirectory = directoryRepository.findByIdAndUser(parentDirectoryId, user)
                .orElseThrow(() -> new DirectoryNotFoundException("Parent directory not found"));
        return getSubDirectories(user, parentDirectory).stream().map(directoryMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public DirectoryDto getDirectoryByIdAsDto(Long id, User user) {
        Directory directory = findByIdAndUser(id, user)
                .orElseThrow(() -> new DirectoryNotFoundException("Directory not found"));
        return directoryMapper.toDto(directory);
    }

    // Yeni eklenen DTO bazlı metot
    @Override
    public DirectoryDto createDirectory(DirectoryDto directoryDto) {
        Directory directory = new Directory();
        directory.setName(directoryDto.name());
        User user = userManagementService.findByUsername(directoryDto.username())
                .orElseThrow(() -> new RuntimeException("User not found"));
        directory.setUser(user);

        if (directoryDto.parentDirectoryId() != null) {
            Directory parentDirectory = directoryRepository.findByIdAndUser(directoryDto.parentDirectoryId(), user)
                    .orElseThrow(() -> new DirectoryNotFoundException("Parent directory not found"));
            directory.setParentDirectory(parentDirectory);
        }

        saveDirectory(directory);
        return directoryMapper.toDto(directory);
    }

    // Yeni eklenen silme metodu
    @Override
    @Transactional // Sadece bu metod @Transactional
    public void deleteDirectoryById(Long id, User user) {
        Directory directory = findByIdAndUser(id, user)
                .orElseThrow(() -> new DirectoryNotFoundException("Directory not found or not authorized"));
        deleteDirectory(directory); // Bu çağrı artık @Transactional kapsamındadır
    }

    // Private silme metodu, @Transactional kaldırıldı
    public void deleteDirectory(Directory directory) {
        List<Directory> subDirectories = directory.getSubDirectories();
        for (Directory subDirectory : subDirectories) {
            try {
                deleteDirectory(subDirectory);
            } catch (Exception e) {
                logger.error("Alt dizin silinirken hata: {} - {}", subDirectory.getId(), e.getMessage());
            }
        }

        List<File> files = directory.getFiles();
        for (File file : files) {
            try {
                fileVersionRepository.deleteAllByFile(file);
                fileRepository.delete(file);
            } catch (Exception e) {
                logger.error("Dosya silinirken hata: {} - {}", file.getId(), e.getMessage());
            }
        }

        try {
            directoryRepository.delete(directory);
            logger.info("Directory silindi: {}", directory.getId());
        } catch (Exception e) {
            logger.error("Dizin silinirken hata: {} - {}", directory.getId(), e.getMessage());
        }
    }

    @Override
    public List<DirectoryDto> getDirectoriesByUsername(String username) {
        User user = userManagementService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<Directory> directories = getUserDirectories(user); // Mevcut metot
        return directories.stream().map(directoryMapper::toDto).toList(); // DTO dönüşümü
    }


}
