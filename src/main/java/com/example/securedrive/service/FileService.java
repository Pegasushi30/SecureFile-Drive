package com.example.securedrive.service;

import com.example.securedrive.model.Directory;
import com.example.securedrive.model.File;
import com.example.securedrive.model.FileShare;
import com.example.securedrive.model.User;
import com.example.securedrive.repository.FileRepository;
import com.example.securedrive.repository.FileShareRepository;
import com.example.securedrive.security.AzureBlobSASTokenGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileShareRepository fileShareRepository;

    @Autowired
    private AzureBlobSASTokenGenerator azureBlobSASTokenGenerator;

    // Kullanıcıya ait tüm dosyaları getir
    public List<File> getFilesByUser(User user) {
        return fileRepository.findAllByUser(user);
    }

    // Dosyayı kaydet veya güncelle
    public void saveFile(File file) {
        fileRepository.save(file);
    }

    // ID ile dosya bul
    public Optional<File> findById(Long id) {
        return fileRepository.findById(id);
    }

    // ID ve kullanıcı ile dosya bul
    public Optional<File> findByIdAndUser(Long id, User user) {
        return fileRepository.findByIdAndUser(id, user);
    }

    // Kullanıcı ve dosya adına göre dosya bul (ana dizinde ise directory null olmalı)
    public File findByFileNameAndUserDirectoryNull(String fileName, User user) {
        return fileRepository.findByUserAndFileNameAndDirectoryIsNull(user, fileName).orElse(null);
    }

    // Kullanıcı, dosya adı ve belirli bir dizine göre dosya bul
    public File findByFileNameUserAndDirectory(String fileName, User user, Directory directory) {
        return fileRepository.findByUserAndFileNameAndDirectory(user, fileName, directory).orElse(null);
    }

    // Dosyayı sil
    public void deleteFile(File file) {
        fileRepository.delete(file);
    }

    // Kullanıcının ana dizinindeki dosyaları alıyoruz
    public List<File> getUserFilesInRootDirectory(User user) {
        // Bu dosyalar `directoryId = null` olan dosyalar olabilir
        return fileRepository.findAllByUserAndDirectoryIsNull(user);
    }


    // Belirli bir dizindeki dosyaları getir
    public List<File> getFilesByDirectory(Directory directory) {
        return fileRepository.findAllByDirectory(directory);
    }

    // Dosyayı bir kullanıcıyla paylaş
    public void shareFileWithUser(File file, User owner, User sharedWithUser, String versionNumber) {
        // Versioned file path
        String versionPath = String.format("%s/versions/%s/%s",
                file.getPath(),
                versionNumber,
                file.getFileName());

        // Generate SAS URL for the versioned file
        String sasUrl = azureBlobSASTokenGenerator.getBlobUrl(versionPath);

        // Check if the file is already shared with the user
        Optional<FileShare> existingShare = fileShareRepository.findByFileAndSharedWithUser(file, sharedWithUser);
        FileShare fileShare;
        if (existingShare.isPresent()) {
            // If file already shared, update the SAS URL and version
            fileShare = existingShare.get();
            fileShare.setSasUrl(sasUrl);
            fileShare.setVersion(versionNumber); // Update version if it's already shared
        } else {
            // If not shared yet, create a new share record
            fileShare = new FileShare();
            fileShare.setFile(file);
            fileShare.setOwner(owner);
            fileShare.setSharedWithUser(sharedWithUser);
            fileShare.setSasUrl(sasUrl);
            fileShare.setVersion(versionNumber); // Set version on new share
        }

        // Save or update the file share
        fileShareRepository.save(fileShare);
    }

    // Bir dosyanın paylaşılıp paylaşılmadığını kontrol et
    public boolean isFileSharedWithUser(File file, User sharedWithUser) {
        return fileShareRepository.existsByFileAndSharedWithUser(file, sharedWithUser);
    }

    // Kullanıcıya paylaşılan dosyaları getir
    public List<File> getFilesSharedWithUser(User sharedWithUser) {
        List<FileShare> shares = fileShareRepository.findAllBySharedWithUser(sharedWithUser);
        return shares.stream().map(FileShare::getFile).collect(Collectors.toList());
    }
}
