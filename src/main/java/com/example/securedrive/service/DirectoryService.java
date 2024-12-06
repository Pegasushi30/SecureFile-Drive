package com.example.securedrive.service;

import com.example.securedrive.model.Directory;
import com.example.securedrive.model.User;

import java.util.List;
import java.util.Optional;

public interface DirectoryService {
    List<Directory> getRootDirectories(User user);
    List<Directory> getSubDirectories(User user, Directory parentDirectory);
    void saveDirectory(Directory directory);
    Optional<Directory> findByIdAndUser(Long id, User user);

    List<Directory> getUserDirectories(User user);
    Optional<Directory> getDirectoryById(Long id);
}
