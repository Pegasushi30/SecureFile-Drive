package com.example.securedrive.service;

import com.example.securedrive.model.File;
import com.example.securedrive.model.FileVersion;
import com.example.securedrive.model.User;

import java.util.List;

public interface FileVersionManagementService {
    void saveFileVersion(FileVersion version);
    List<FileVersion> getAllVersions(File file);
    FileVersion createVersion(File file, String versionNumber, String deltaPath);
    String generateNextVersion(File file);
    String getLatestContent(File file, User user) throws Exception;
    String reconstructFileContent(File file, String versionNumber, User user) throws Exception;
    List<FileVersion> getVersionsUpTo(File file, String versionNumber);
}
