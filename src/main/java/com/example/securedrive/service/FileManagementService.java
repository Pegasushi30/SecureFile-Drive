// com/example/securedrive/service/FileManagementService.java
package com.example.securedrive.service;

import com.example.securedrive.dto.FileDto;
import com.example.securedrive.dto.FileShareDto;
import com.example.securedrive.model.Directory;
import com.example.securedrive.model.File;
import com.example.securedrive.model.User;

import java.util.List;
import java.util.Optional;

public interface FileManagementService {
    List<File> getFilesByUser(User user);
    void saveFile(File file);
    Optional<File> findById(Long id);
    Optional<File> findByIdAndUser(Long id, User user);
    File findByFileNameAndUserDirectoryNull(String fileName, User user);
    File findByFileNameUserAndDirectory(String fileName, User user, Directory directory);
    List<File> getUserFilesInRootDirectory(User user);
    List<File> getFilesByDirectory(Directory directory);
    void shareFileWithUser(File file, User owner, User sharedWithUser, String versionNumber);
    // DTO bazlı ek metotlar
    List<FileDto> getUserFilesInRootDirectoryAsDto(User user);
    List<FileDto> getFilesByDirectoryAsDto(Directory directory);

    List<FileShareDto> getSharedFilesByUsername(String username);
    List<FileDto> getFilesByUsername(String username);
}

