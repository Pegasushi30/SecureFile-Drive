package com.example.securedrive.service;

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
    private UserService userService;

    @Autowired
    private AzureBlobSASTokenGenerator azureBlobSASTokenGenerator;

    // Belirli bir kullanıcıya ait dosyaları getir
    public List<File> getFilesByUser(User user) {
        return fileRepository.findAllByUser(user);
    }

    // Dosyayı kaydet veya güncelle
    public void saveFile(File file) {
        fileRepository.save(file);
    }

    // Belirli bir kullanıcıya ve ada göre dosya ara
    public File findByFileNameAndUser(String fileName, User user) {
        return fileRepository.findByFileNameAndUser(fileName, user);
    }

    // Dosya ID'sine göre dosya getir
    public Optional<File> findById(Long id) {
        return fileRepository.findById(id);
    }

    public void deleteFile(File file) {
        fileRepository.delete(file);
    }


    public void shareFileWithUser(File file, User owner, User sharedWithUser, String versionNumber) {
        // Versiyon için tam yolu oluştur
        String versionPath = "uploads/" + owner.getId() + "/" + file.getFileName() + "/" + versionNumber + "/" + file.getFileName();

        // SAS URL oluştur
        String sasUrl = azureBlobSASTokenGenerator.getBlobUrl(versionPath);

        // FileShare kaydı oluştur
        FileShare fileShare = new FileShare();
        fileShare.setFile(file);
        fileShare.setOwner(owner);
        fileShare.setSharedWithUser(sharedWithUser);
        fileShare.setSasUrl(sasUrl);

        // Kaydı veritabanına kaydet
        fileShareRepository.save(fileShare);
    }


    public boolean isFileSharedWithUser(File file, User sharedWithUser) {
        return fileShareRepository.existsByFileAndSharedWithUser(file, sharedWithUser);
    }

    public List<File> getFilesSharedWithUser(User sharedWithUser) {
        List<FileShare> shares = fileShareRepository.findAllBySharedWithUser(sharedWithUser);
        return shares.stream().map(FileShare::getFile).collect(Collectors.toList());
    }
}
