package com.example.securedrive.service.impl;

import com.example.securedrive.dto.DirectoryDto;
import com.example.securedrive.dto.DirectoryShareDto;
import com.example.securedrive.dto.DirectoryShareRequestDto;
import com.example.securedrive.exception.AzureBlobStorageException;
import com.example.securedrive.exception.DirectoryNotFoundException;
import com.example.securedrive.mapper.DirectoryMapper;
import com.example.securedrive.model.*;
import com.example.securedrive.repository.*;
import com.example.securedrive.security.AzureBlobSASTokenGenerator;
import com.example.securedrive.service.AzureBlobStorageService;
import com.example.securedrive.service.DirectoryService;
import com.example.securedrive.service.UserManagementService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DirectoryServiceImpl implements DirectoryService {

    private static final Logger logger = LoggerFactory.getLogger(DirectoryServiceImpl.class);

    private final DirectoryRepository directoryRepository;
    private final AzureBlobStorageService azureBlobStorageService;
    private final FileRepository fileRepository;
    private final FileVersionRepository fileVersionRepository;
    private final DirectoryMapper directoryMapper;
    private final UserManagementService userManagementService;
    private final DirectoryShareRepository directoryShareRepository;
    private final FileShareRepository fileShareRepository;
    private final AzureBlobSASTokenGenerator azureBlobSASTokenGenerator;
    private final UserRepository userRepository;

    @Autowired
    public DirectoryServiceImpl(DirectoryRepository directoryRepository,
                                AzureBlobStorageService azureBlobStorageService,
                                FileRepository fileRepository,
                                FileVersionRepository fileVersionRepository,
                                DirectoryMapper directoryMapper,
                                UserManagementService userManagementService,
                                DirectoryShareRepository directoryShareRepository,
                                FileShareRepository fileShareRepository,
                                AzureBlobSASTokenGenerator azureBlobSASTokenGenerator,
                                UserRepository userRepository) {
        this.directoryRepository = directoryRepository;
        this.azureBlobStorageService = azureBlobStorageService;
        this.fileRepository = fileRepository;
        this.fileVersionRepository = fileVersionRepository;
        this.directoryMapper = directoryMapper;
        this.userManagementService = userManagementService;
        this.directoryShareRepository = directoryShareRepository;
        this.fileShareRepository = fileShareRepository;
        this.azureBlobSASTokenGenerator = azureBlobSASTokenGenerator;
        this.userRepository = userRepository;
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
            azureBlobStorageService.createDirectory(path);
            logger.info("Directory created with marker at path: {}", path);
        } catch (AzureBlobStorageException e) {
            logger.error("Error for directory creation in Blob Storage: {}", e.getMessage());
            throw new RuntimeException("Error for file creation in Blob Storage: " + e.getMessage());
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

    @Override
    @Transactional
    public void deleteDirectoryById(Long id, User user) {
        Directory directory = findByIdAndUser(id, user)
                .orElseThrow(() -> new DirectoryNotFoundException("Directory not found or not authorized"));
        deleteDirectory(directory);
    }

    public void deleteDirectory(Directory directory) {
        List<Directory> subDirectories = directory.getSubDirectories();
        for (Directory subDirectory : subDirectories) {
            try {
                deleteDirectory(subDirectory);
            } catch (Exception e) {
                logger.error("Error for subdirectory deletion: {} - {}", subDirectory.getId(), e.getMessage());
            }
        }

        List<File> files = directory.getFiles();
        for (File file : files) {
            try {
                fileVersionRepository.deleteAllByFile(file);
                fileRepository.delete(file);
            } catch (Exception e) {
                logger.error("Error for file deletion: {} - {}", file.getId(), e.getMessage());
            }
        }

        try {
            directoryRepository.delete(directory);
            logger.info("File deleted successfully: {}", directory.getId());
        } catch (Exception e) {
            logger.error("Error for file deletion: {} - {}", directory.getId(), e.getMessage());
        }
    }

    @Override
    public List<DirectoryDto> getDirectoriesByUsername(String username) {
        User user = userManagementService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<Directory> directories = getUserDirectories(user);
        return directories.stream().map(directoryMapper::toDto).toList();
    }

    @Override
    public void shareDirectory(DirectoryShareRequestDto dto, User owner) {
        Directory directory = directoryRepository.findById(dto.getDirectoryId())
                .orElseThrow(() -> new RuntimeException("Directory not found"));

        if (!directory.getUser().equals(owner)) {
            throw new SecurityException("You do not have permission to share this directory.");
        }

        User sharedWithUser = userManagementService.findByEmail(dto.getSharedWithUserEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + dto.getSharedWithUserEmail()));
        shareDirectoryRecursive(directory, sharedWithUser, owner, dto.getUsername());
    }

    private void shareDirectoryRecursive(Directory directory, User sharedWithUser, User owner, String username) {
        DirectoryShare directoryShare = new DirectoryShare();
        directoryShare.setDirectory(directory);
        directoryShare.setOwner(owner);
        directoryShare.setSharedWithUser(sharedWithUser);
        directoryShare.setSharedPath("directories/" + username + "/" + directory.getName());
        directoryShareRepository.save(directoryShare);
        for (File file : directory.getFiles()) {
            shareFileWithSasTokens(file, sharedWithUser, owner);
        }
        for (Directory subDirectory : directory.getSubDirectories()) {
            shareDirectoryRecursive(subDirectory, sharedWithUser, owner, username);
        }
    }

    private void shareFileWithSasTokens(File file, User sharedWithUser, User owner) {
        for (FileVersion version : file.getVersions()) {
            String versionPath = String.format("%s/versions/%s/%s", file.getPath(), version.getVersionNumber(), file.getFileName());
            String sasUrl = azureBlobSASTokenGenerator.getBlobUrl(versionPath);
            Optional<FileShare> existingShare = fileShareRepository.findByFileAndSharedWithUserAndVersion(file, sharedWithUser, version.getVersionNumber());
            FileShare fileShare;
            if (existingShare.isPresent()) {
                fileShare = existingShare.get();
                fileShare.setSasUrl(sasUrl);
            } else {
                fileShare = new FileShare();
                fileShare.setFile(file);
                fileShare.setOwner(owner);
                fileShare.setSharedWithUser(sharedWithUser);
                fileShare.setSasUrl(sasUrl);
                fileShare.setVersion(version.getVersionNumber());
            }
            fileShareRepository.save(fileShare);
        }
        owner.getContacts().add(sharedWithUser);
        userRepository.save(owner);
    }



    @Override
    public void revokeDirectoryShare(Long directoryId, String sharedWithEmail, User owner) {
        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new RuntimeException("Directory not found"));

        if (!directory.getUser().equals(owner)) {
            throw new SecurityException("You do not have permission to revoke share for this directory.");
        }

        User sharedWithUser = userManagementService.findByEmail(sharedWithEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + sharedWithEmail));
        revokeDirectoryShareRecursive(directory, sharedWithUser);
    }

    @Override
    public void revokeDirectoryShareRecursive(Directory directory, User sharedWithUser) {
        directoryShareRepository.findByDirectoryAndSharedWithUser(directory, sharedWithUser)
                .ifPresent(directoryShareRepository::delete);
        for (File file : directory.getFiles()) {
            revokeFileSharesForAllVersions(file, sharedWithUser);
        }
        for (Directory subDirectory : directory.getSubDirectories()) {
            revokeDirectoryShareRecursive(subDirectory, sharedWithUser);
        }
    }

    private void revokeFileSharesForAllVersions(File file, User sharedWithUser) {
        for (FileVersion version : file.getVersions()) {
            fileShareRepository.findByFileAndSharedWithUserAndVersion(file, sharedWithUser, version.getVersionNumber())
                    .ifPresent(fileShareRepository::delete);
        }
    }
    @Override
    public List<DirectoryShareDto> getSharedDirectories(User user) {
        List<DirectoryShare> sharedDirectories = directoryShareRepository.findBySharedWithUserId(user.getId());

        return sharedDirectories.stream()
                .map(share -> new DirectoryShareDto(
                        share.getId(),
                        share.getDirectory().getId(),
                        share.getDirectory().getName(),
                        share.getSharedWithUser().getEmail(),
                        share.getDirectory().getUser().getEmail(),
                        share.getSharedPath()
                ))
                .collect(Collectors.toList());
    }


    @Override
    public List<DirectoryShareDto> getMySharedDirectories(User owner) {
        List<DirectoryShare> sharedDirectories = directoryShareRepository.findByOwner(owner);

        return sharedDirectories.stream()
                .map(share -> new DirectoryShareDto(
                        share.getId(),
                        share.getDirectory().getId(),
                        share.getDirectory().getName(),
                        share.getSharedWithUser().getEmail(),
                        share.getOwner().getEmail(),
                        share.getSharedPath()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<DirectoryShareDto> getMySharedDirectoriesForDirectory(User owner, Long directoryId) {
        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new RuntimeException("Directory not found"));

        if (!directory.getUser().equals(owner)) {
            throw new SecurityException("You do not have permission to view shares for this directory.");
        }

        return directoryShareRepository.findByDirectoryAndOwner(directory, owner).stream()
                .map(share -> new DirectoryShareDto(
                        share.getId(),
                        share.getDirectory().getId(),
                        share.getDirectory().getName(),
                        share.getSharedWithUser().getEmail(),
                        directory.getUser().getEmail(),
                        share.getSharedPath()
                ))
                .collect(Collectors.toList());
    }




}
