package com.example.securedrive.service.impl;

import com.example.securedrive.exception.AzureBlobStorageException;
import com.example.securedrive.model.Directory;
import com.example.securedrive.model.File;
import com.example.securedrive.model.User;
import com.example.securedrive.repository.FileRepository;
import com.example.securedrive.repository.FileVersionRepository;
import com.example.securedrive.service.DirectoryService;
import com.example.securedrive.service.IAzureBlobStorage;
import com.example.securedrive.repository.DirectoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DirectoryServiceImpl implements DirectoryService {

    @Autowired
    private DirectoryRepository directoryRepository;

    @Autowired
    private IAzureBlobStorage azureBlobStorage;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileVersionRepository fileVersionRepository;

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

        // Azure Blob Storage'da sanal dizin oluştur
        String path;
        if (directory.getParentDirectory() != null) {
            path = "directories/" + directory.getUser().getUsername() + "/" + buildDirectoryPath(directory);
        } else {
            path = "directories/" + directory.getUser().getUsername() + "/" + directory.getName() + "/";
        }

        try {
            azureBlobStorage.createDirectory(path);
        } catch (AzureBlobStorageException e) {
            throw new RuntimeException("Blob Storage dizin oluşturulurken hata oluştu: " + e.getMessage());
        }
    }

    private String buildDirectoryPath(Directory directory) {
        StringBuilder path = new StringBuilder();
        Directory current = directory;
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
    @Transactional
    public void deleteDirectory(Directory directory) {
        // Alt dizinleri sil
        List<Directory> subDirectories = directory.getSubDirectories();
        for (Directory subDirectory : subDirectories) {
            try {
                deleteDirectory(subDirectory); // Rekürsif silme
            } catch (Exception e) {
                System.out.println("Alt dizin silinirken hata: " + subDirectory.getId() + " - " + e.getMessage());
            }
        }

        // İlişkili dosyaları sil
        List<File> files = directory.getFiles();
        for (File file : files) {
            try {
                fileVersionRepository.deleteAllByFile(file);
                fileRepository.delete(file);
            } catch (Exception e) {
                System.out.println("Dosya silinirken hata: " + file.getId() + " - " + e.getMessage());
            }
        }

        // Dizin kaydını sil
        try {
            directoryRepository.delete(directory);
        } catch (Exception e) {
            System.out.println("Dizin silinirken hata: " + directory.getId() + " - " + e.getMessage());
        }
    }




}
