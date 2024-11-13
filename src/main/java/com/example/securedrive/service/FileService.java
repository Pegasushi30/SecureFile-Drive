package com.example.securedrive.service;

import com.example.securedrive.model.File;
import com.example.securedrive.model.User;
import com.example.securedrive.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    public List<File> getFilesByUser(User user) {
        return fileRepository.findAllByUser(user);
    }

    public File saveFile(File file) {
        return fileRepository.save(file);  // File kaydedildiğinde user ilişkisi de veritabanına işlenir
    }

    public File findByFileNameAndUser(String fileName, User user) {
        return fileRepository.findByFileNameAndUser(fileName, user);
    }

    public void deleteFile(Long id) {
        fileRepository.deleteById(id);
    }
}

