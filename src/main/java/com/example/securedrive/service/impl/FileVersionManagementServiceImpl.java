// com/example/securedrive/service/impl/FileVersionManagementServiceImpl.java
package com.example.securedrive.service.impl;

import com.example.securedrive.exception.AzureBlobStorageException;
import com.example.securedrive.model.File;
import com.example.securedrive.model.FileVersion;
import com.example.securedrive.model.Storage;
import com.example.securedrive.model.User;
import com.example.securedrive.repository.FileVersionRepository;
import com.example.securedrive.security.AESUtil;
import com.example.securedrive.security.DeltaUtil;
import com.example.securedrive.security.KeyVaultService;
import com.example.securedrive.service.FileVersionManagementService;
import com.example.securedrive.service.impl.AzureBlobStorageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class FileVersionManagementServiceImpl implements FileVersionManagementService {

    private final FileVersionRepository fileVersionRepository;
    private final AzureBlobStorageImpl azureBlobStorage;
    private final KeyVaultService keyVaultService;

    private static final Logger logger = LoggerFactory.getLogger(FileVersionManagementServiceImpl.class);

    @Autowired
    public FileVersionManagementServiceImpl(
            FileVersionRepository fileVersionRepository,
            AzureBlobStorageImpl azureBlobStorage,
            KeyVaultService keyVaultService
    ) {
        this.fileVersionRepository = fileVersionRepository;
        this.azureBlobStorage = azureBlobStorage;
        this.keyVaultService = keyVaultService;
    }

    @Override
    public void saveFileVersion(FileVersion version) {
        fileVersionRepository.save(version);
    }

    @Override
    public List<FileVersion> getAllVersions(File file) {
        return fileVersionRepository.findAllByFile(file);
    }

    @Override
    public FileVersion createVersion(File file, String versionNumber, String deltaPath) {
        FileVersion version = new FileVersion();
        version.setFile(file);
        version.setVersionNumber(versionNumber);
        version.setTimestamp(java.time.LocalDateTime.now());
        version.setDeltaPath(deltaPath);
        return version;
    }

    @Override
    public String generateNextVersion(File file) {
        List<FileVersion> versions = getAllVersions(file);
        int nextVersion = versions.size() + 1;
        return String.format("v%d", nextVersion);
    }

    @Override
    public String getLatestContent(File file, User user) throws Exception {
        List<FileVersion> versions = getAllVersions(file);
        if (versions.isEmpty()) {
            return "";
        }

        StringBuilder content = new StringBuilder();
        String initialVersionFullPath = "uploads/" + user.getUsername() + "/" + file.getFileName()
                + "/versions/v1/" + file.getFileName();

        // İlk versiyon kontrolü ve okuma
        if (!azureBlobStorage.checkBlobExists(initialVersionFullPath)) {
            throw new AzureBlobStorageException("Initial version blob not found at path: " + initialVersionFullPath);
        }

        byte[] initialEncryptedDataBase64 = azureBlobStorage.read(new Storage(initialVersionFullPath, null));
        if (initialEncryptedDataBase64 == null) {
            throw new Exception("Failed to read initial version blob");
        }

        // Base64 çözümleme ve şifre çözme
        byte[] initialEncryptedData = Base64.getDecoder().decode(initialEncryptedDataBase64);
        String encryptionKey = keyVaultService.getEncryptionKeyFromKeyVault(user.getUsername());
        byte[] decryptedData = AESUtil.decrypt(initialEncryptedData, encryptionKey);
        String initialContent = new String(decryptedData, StandardCharsets.UTF_8);
        content.append(initialContent);

        // Tüm versiyonların deltalarını uygula
        for (int i = 1; i < versions.size(); i++) {
            FileVersion version = versions.get(i);
            String deltaPath = version.getDeltaPath();

            if (deltaPath == null) {
                throw new Exception("Delta path is null for version: " + version.getVersionNumber());
            }

            if (!azureBlobStorage.checkBlobExists(deltaPath)) {
                throw new AzureBlobStorageException("Delta blob not found at path: " + deltaPath);
            }

            // Delta okuma ve Base64 çözme
            byte[] deltaBase64Data = azureBlobStorage.read(new Storage(deltaPath, null));
            if (deltaBase64Data == null) {
                throw new Exception("Failed to read delta blob for version: " + version.getVersionNumber());
            }

            byte[] deltaData = Base64.getDecoder().decode(deltaBase64Data);
            String delta = new String(deltaData, StandardCharsets.UTF_8);

            // Delta uygula
            logger.info("Applying delta for version {}: \n{}", version.getVersionNumber(), delta);
            content = new StringBuilder(DeltaUtil.applyDelta(content.toString(), delta));
        }

        return content.toString();
    }


    @Override
    public String reconstructFileContent(File file, String versionNumber, User user) throws Exception {
        List<FileVersion> versions = getVersionsUpTo(file, versionNumber);
        if (versions.isEmpty()) {
            throw new Exception("No versions found up to version: " + versionNumber);
        }

        logger.info("reconstructFileContent - Versiyon sayısı: {}", versions.size());
        versions.forEach(v -> logger.info("Versiyon: {} - Delta: {}", v.getVersionNumber(), v.getDeltaPath()));

        StringBuilder content = new StringBuilder();
        String initialVersionFullPath = "uploads/" + user.getUsername() + "/" + file.getFileName()
                + "/versions/v1/" + file.getFileName();

        logger.info("Initial version path: {}", initialVersionFullPath);

        if (!azureBlobStorage.checkBlobExists(initialVersionFullPath)) {
            logger.error("Initial version blob not found at path: {}", initialVersionFullPath);
            throw new AzureBlobStorageException("Initial version blob not found at path: " + initialVersionFullPath);
        }

        byte[] initialEncryptedDataBase64 = azureBlobStorage.read(new Storage(initialVersionFullPath, null));
        if (initialEncryptedDataBase64 == null) {
            logger.error("Failed to read initial version blob at: {}", initialVersionFullPath);
            throw new Exception("Failed to read initial version blob");
        }

        logger.info("Initial encrypted base64 data length: {}", initialEncryptedDataBase64.length);

        byte[] initialEncryptedData = Base64.getDecoder().decode(initialEncryptedDataBase64);
        logger.info("Base64 decode successful for initial version. Length: {}", initialEncryptedData.length);

        String encryptionKey = keyVaultService.getEncryptionKeyFromKeyVault(user.getUsername());
        logger.info("Encryption key retrieved for user {}: {}", user.getUsername(), encryptionKey != null ? "FOUND" : "NOT FOUND");

        byte[] decryptedData = AESUtil.decrypt(initialEncryptedData, encryptionKey);
        logger.info("Initial version decrypt successful. Length: {}", decryptedData.length);

        String initialContent = new String(decryptedData, StandardCharsets.UTF_8);
        logger.info("Initial content: {}", initialContent);

        content.append(initialContent);

        for (int i = 1; i < versions.size(); i++) {
            FileVersion version = versions.get(i);
            String deltaPath = version.getDeltaPath();

            logger.info("Applying delta for version: {}", version.getVersionNumber());
            logger.info("Delta path: {}", deltaPath);

            if (deltaPath == null) {
                logger.error("Delta path is null for version: {}", version.getVersionNumber());
                throw new Exception("Delta path is null for version: " + version.getVersionNumber());
            }

            if (!azureBlobStorage.checkBlobExists(deltaPath)) {
                logger.error("Delta blob not found at path: {}", deltaPath);
                throw new AzureBlobStorageException("Delta blob not found at path: " + deltaPath);
            }

            byte[] deltaBase64Data = azureBlobStorage.read(new Storage(deltaPath, null));
            if (deltaBase64Data == null) {
                logger.error("Failed to read delta blob at: {}", deltaPath);
                throw new Exception("Failed to read delta blob for version: " + version.getVersionNumber());
            }

            byte[] deltaData = Base64.getDecoder().decode(deltaBase64Data);
            String delta = new String(deltaData, StandardCharsets.UTF_8);

            logger.info("Read decoded delta (length: {}):\n{}", deltaData.length, delta);

            logger.info("Content before applyDelta:\n{}", content);

            String appliedContent = DeltaUtil.applyDelta(content.toString(), delta);
            logger.info("Content after applyDelta:\n{}", appliedContent);

            content = new StringBuilder(appliedContent);

            if (version.getVersionNumber().equals(versionNumber)) {
                break;
            }
        }

        logger.info("Final reconstructed content:\n{}", content.toString());
        return content.toString();
    }

    @Override
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
