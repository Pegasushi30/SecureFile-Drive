package com.example.securedrive.service;

import com.example.securedrive.exception.AzureBlobStorageException;
import com.example.securedrive.model.File;
import com.example.securedrive.model.FileVersion;
import com.example.securedrive.model.Storage;
import com.example.securedrive.model.User;
import com.example.securedrive.repository.FileVersionRepository;
import com.example.securedrive.security.AESUtil;
import com.example.securedrive.security.DeltaUtil;
import com.example.securedrive.security.KeyVaultService;
import com.example.securedrive.service.impl.AzureBlobStorageImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class FileVersionService {

    @Autowired
    private FileVersionRepository fileVersionRepository;

    @Autowired
    private AzureBlobStorageImpl azureBlobStorage;

    @Autowired
    private KeyVaultService keyVaultService;

    public void saveFileVersion(FileVersion version) {
        fileVersionRepository.save(version);
    }

    public List<FileVersion> getAllVersions(File file) {
        return fileVersionRepository.findAllByFile(file);
    }

    public FileVersion createVersion(File file, String versionNumber, String deltaPath) {
        FileVersion version = new FileVersion();
        version.setFile(file);
        version.setVersionNumber(versionNumber);
        version.setTimestamp(java.time.LocalDateTime.now());
        version.setDeltaPath(deltaPath);
        return version;
    }

    public String generateNextVersion(File file) {
        List<FileVersion> versions = getAllVersions(file);
        int nextVersion = versions.size() + 1;
        return String.format("v%d", nextVersion);
    }

    public String getLatestContent(File file, User user) throws Exception {
        List<FileVersion> versions = getAllVersions(file);
        if (versions.isEmpty()) {
            return "";
        }

        StringBuilder content = new StringBuilder();
        String initialVersionFullPath = "uploads/" + user.getUsername() + "/" + file.getFileName()
                + "/versions/v1/" + file.getFileName();

        if (!azureBlobStorage.checkBlobExists(initialVersionFullPath)) {
            throw new AzureBlobStorageException("Initial version blob not found at path: " + initialVersionFullPath);
        }

        byte[] initialEncryptedDataBase64 = azureBlobStorage.read(new Storage(initialVersionFullPath, null));
        if (initialEncryptedDataBase64 == null) {
            throw new Exception("Failed to read initial version blob");
        }

        // Decode the base64 data
        byte[] initialEncryptedData = Base64.getDecoder().decode(initialEncryptedDataBase64);

        // Retrieve the AES key from Key Vault
        String encryptionKey = keyVaultService.getEncryptionKeyFromKeyVault(user.getUsername());

        // Decrypt the initial file content
        byte[] decryptedData = AESUtil.decrypt(initialEncryptedData, encryptionKey);
        String initialContent = new String(decryptedData, StandardCharsets.UTF_8);
        content.append(initialContent);

        // Apply deltas for subsequent versions
        for (int i = 1; i < versions.size(); i++) {
            FileVersion version = versions.get(i);
            String deltaPath = version.getDeltaPath();

            if (deltaPath == null) {
                throw new Exception("Delta path is null for version: " + version.getVersionNumber());
            }

            if (!azureBlobStorage.checkBlobExists(deltaPath)) {
                throw new AzureBlobStorageException("Delta blob not found at path: " + deltaPath);
            }

            byte[] deltaData = azureBlobStorage.read(new Storage(deltaPath, null));

            if (deltaData == null) {
                throw new Exception("Failed to read delta blob for version: " + version.getVersionNumber());
            }

            String delta = new String(deltaData, StandardCharsets.UTF_8);
            content = new StringBuilder(DeltaUtil.applyDelta(content.toString(), delta));
        }

        return content.toString();
    }


    public String reconstructFileContent(File file, String versionNumber, User user) throws Exception {
        List<FileVersion> versions = getVersionsUpTo(file, versionNumber);
        if (versions.isEmpty()) {
            throw new Exception("No versions found up to version: " + versionNumber);
        }

        StringBuilder content = new StringBuilder();
        String initialVersionFullPath = "uploads/" + user.getUsername() + "/" + file.getFileName()
                + "/versions/v1/" + file.getFileName();

        if (!azureBlobStorage.checkBlobExists(initialVersionFullPath)) {
            throw new AzureBlobStorageException("Initial version blob not found at path: " + initialVersionFullPath);
        }

        byte[] initialEncryptedDataBase64 = azureBlobStorage.read(new Storage(initialVersionFullPath, null));
        if (initialEncryptedDataBase64 == null) {
            throw new Exception("Failed to read initial version blob");
        }

        byte[] initialEncryptedData = Base64.getDecoder().decode(initialEncryptedDataBase64);
        String encryptionKey = keyVaultService.getEncryptionKeyFromKeyVault(user.getUsername());

        byte[] decryptedData = AESUtil.decrypt(initialEncryptedData, encryptionKey);
        String initialContent = new String(decryptedData, StandardCharsets.UTF_8);

        content.append(initialContent);

        // Apply deltas for versions up to the specified version number
        for (int i = 1; i < versions.size(); i++) {
            FileVersion version = versions.get(i);
            String deltaPath = version.getDeltaPath();

            if (!azureBlobStorage.checkBlobExists(deltaPath)) {
                throw new AzureBlobStorageException("Delta blob not found at path: " + deltaPath);
            }

            byte[] deltaData = azureBlobStorage.read(new Storage(deltaPath, null));
            if (deltaData == null) {
                throw new Exception("Failed to read delta blob for version: " + version.getVersionNumber());
            }

            String delta = new String(deltaData, StandardCharsets.UTF_8);
            content = new StringBuilder(DeltaUtil.applyDelta(content.toString(), delta));

            if (version.getVersionNumber().equals(versionNumber)) {
                break;
            }
        }

        return content.toString();
    }

    public FileVersion getVersionByFileAndNumber(File file, String versionNumber) {
        return fileVersionRepository.findByFileAndVersionNumber(file, versionNumber);
    }

    public void deleteFileVersion(FileVersion version) {
        fileVersionRepository.delete(version);
    }

    public List<FileVersion> getVersionsUpTo(File file, String versionNumber) {
        List<FileVersion> allVersions = getAllVersions(file);
        List<FileVersion> versionsUpTo = new ArrayList<>();

        for (FileVersion version : allVersions) {
            versionsUpTo.add(version);
            if (version.getVersionNumber().equals(versionNumber)) {
                break;
            }
        }
        return versionsUpTo;
    }
}
