package com.example.securedrive.service;

import com.example.securedrive.model.File;
import com.example.securedrive.model.User;
import com.example.securedrive.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

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

    // Dosyayı ID'ye göre sil
    public void deleteFile(Long id) {
        fileRepository.deleteById(id);
    }

    // Dosya ID'sine göre dosya getir
    public Optional<File> getFileById(Long id) {
        return fileRepository.findById(id);
    }
}
