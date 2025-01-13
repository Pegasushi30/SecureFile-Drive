// com/example/securedrive/service/FileManagementService.java
package com.example.securedrive.service;

import com.example.securedrive.dto.FileDto;
import com.example.securedrive.dto.FileShareDto;
import com.example.securedrive.dto.FileVersionDto;
import com.example.securedrive.model.Directory;
import com.example.securedrive.model.File;
import com.example.securedrive.model.User;

import java.util.List;
import java.util.Map;
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
    List<FileDto> getUserFilesInRootDirectoryAsDto(User user);
    List<FileDto> getFilesByDirectoryAsDto(Directory directory);
    List<FileShareDto> getSharedFilesByUsername(String username);
    List<FileDto> getFilesByUsername(String username);
    long getTotalStorage(String username);
    long getUsedStorage(String username);
    List<FileVersionDto> getLastAccessedFileVersions(String username, int limit);
    List<FileVersionDto> getLastUploadedFileVersions(String username, int limit);
    String getFileNameByVersionId(Long versionId);
    Map<Long, String> createFileNameMap(List<FileVersionDto> versions);
    Map<Long, String> createFileSizeMap(List<FileVersionDto> versions);
    Map<String, Map<String, List<FileShareDto>>> groupFileSharesByOwnerAndDirectory(List<FileShareDto> sharedFiles);
}

