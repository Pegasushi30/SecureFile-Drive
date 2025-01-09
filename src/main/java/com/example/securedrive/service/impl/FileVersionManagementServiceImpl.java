// com/example/securedrive/service/impl/FileVersionManagementServiceImpl.java
package com.example.securedrive.service.impl;

import com.example.securedrive.exception.AzureBlobStorageException;
import com.example.securedrive.model.File;
import com.example.securedrive.model.FileVersion;
import com.example.securedrive.model.Storage;
import com.example.securedrive.model.User;
import com.example.securedrive.repository.FileVersionRepository;
import com.example.securedrive.service.util.AESUtil;
import com.example.securedrive.service.util.BinaryDeltaUtil;
import com.example.securedrive.service.util.DeltaUtil;
import com.example.securedrive.security.KeyVaultService;
import com.example.securedrive.service.FileVersionManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Service
public class FileVersionManagementServiceImpl implements FileVersionManagementService {

    private final FileVersionRepository fileVersionRepository;
    private final AzureBlobStorageServiceImpl azureBlobStorage;
    private final KeyVaultService keyVaultService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(FileVersionManagementServiceImpl.class);

    @Autowired
    public FileVersionManagementServiceImpl(
            FileVersionRepository fileVersionRepository,
            AzureBlobStorageServiceImpl azureBlobStorage,
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

        // İlk versiyonu oku ve şifreyi çöz
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

        String fileNameLower = file.getFileName().toLowerCase();
        boolean isBinary = fileNameLower.matches(".*\\.(jpg|png|mp4|docx?|xlsx|pdf|pptx|mkv)$");

        if (isBinary) {
            // Binary dosya işlemleri
            byte[] contentBytes = decryptedData;

            // Tüm versiyonların deltalarını uygula (sadece delta.json varsa)
            for (int i = 1; i < versions.size(); i++) {
                FileVersion version = versions.get(i);
                String deltaPath = version.getDeltaPath();

                if (deltaPath == null) {
                    throw new Exception("Delta path is null for version: " + version.getVersionNumber());
                }

                if (!azureBlobStorage.checkBlobExists(deltaPath)) {
                    throw new AzureBlobStorageException("Delta blob not found at path: " + deltaPath);
                }

                byte[] deltaBase64Data = azureBlobStorage.read(new Storage(deltaPath, null));
                if (deltaBase64Data == null) {
                    throw new Exception("Failed to read delta blob for version: " + version.getVersionNumber());
                }

                byte[] deltaData = Base64.getDecoder().decode(deltaBase64Data);

                // Delta komutlarını JSON'dan deserialize et
                List<BinaryDeltaUtil.DeltaCommand> deltaCommands = Arrays.asList(
                        objectMapper.readValue(deltaData, BinaryDeltaUtil.DeltaCommand[].class)
                );

                // BinaryDeltaUtil ile delta uygulama
                contentBytes = BinaryDeltaUtil.applyDelta(contentBytes, deltaCommands);
            }

            return Base64.getEncoder().encodeToString(contentBytes); // İkili veriyi Base64 ile encode ederek döndür
        } else {
            // Metin dosya işlemleri
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

                byte[] deltaBase64Data = azureBlobStorage.read(new Storage(deltaPath, null));
                if (deltaBase64Data == null) {
                    throw new Exception("Failed to read delta blob for version: " + version.getVersionNumber());
                }

                byte[] deltaData = Base64.getDecoder().decode(deltaBase64Data);
                String delta = new String(deltaData, StandardCharsets.UTF_8);

                content = new StringBuilder(DeltaUtil.applyDelta(content.toString(), delta));
            }

            return content.toString();
        }
    }


    @Override
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

        String fileNameLower = file.getFileName().toLowerCase();
        boolean isBinary = fileNameLower.matches(".*\\.(jpg|png|mp4|docx?|xlsx|pdf|pptx|mkv)$");

        if (isBinary) {
            // Binary dosya işlemleri
            byte[] contentBytes = decryptedData;

            for (int i = 1; i < versions.size(); i++) {
                FileVersion version = versions.get(i);
                String deltaPath = version.getDeltaPath();

                if (deltaPath == null) {
                    throw new Exception("Delta path is null for version: " + version.getVersionNumber());
                }

                if (!azureBlobStorage.checkBlobExists(deltaPath)) {
                    throw new AzureBlobStorageException("Delta blob not found at path: " + deltaPath);
                }

                byte[] deltaBase64Data = azureBlobStorage.read(new Storage(deltaPath, null));
                if (deltaBase64Data == null) {
                    throw new Exception("Failed to read delta blob for version: " + version.getVersionNumber());
                }

                byte[] deltaData = Base64.getDecoder().decode(deltaBase64Data);

                // Delta komutlarını JSON'dan deserialize et
                List<BinaryDeltaUtil.DeltaCommand> deltaCommands = Arrays.asList(
                        objectMapper.readValue(deltaData, BinaryDeltaUtil.DeltaCommand[].class)
                );

                // BinaryDeltaUtil ile delta uygulama
                contentBytes = BinaryDeltaUtil.applyDelta(contentBytes, deltaCommands);

                if (version.getVersionNumber().equals(versionNumber)) {
                    break;
                }
            }

            return Base64.getEncoder().encodeToString(contentBytes); // İkili veriyi Base64 ile encode ederek döndür
        } else {
            // Metin dosya işlemleri
            String initialContent = new String(decryptedData, StandardCharsets.UTF_8);
            content.append(initialContent);

            for (int i = 1; i < versions.size(); i++) {
                FileVersion version = versions.get(i);
                String deltaPath = version.getDeltaPath();

                if (deltaPath == null) {
                    throw new Exception("Delta path is null for version: " + version.getVersionNumber());
                }

                if (!azureBlobStorage.checkBlobExists(deltaPath)) {
                    throw new AzureBlobStorageException("Delta blob not found at path: " + deltaPath);
                }

                byte[] deltaBase64Data = azureBlobStorage.read(new Storage(deltaPath, null));
                if (deltaBase64Data == null) {
                    throw new Exception("Failed to read delta blob for version: " + version.getVersionNumber());
                }

                byte[] deltaData = Base64.getDecoder().decode(deltaBase64Data);
                String delta = new String(deltaData, StandardCharsets.UTF_8);

                content = new StringBuilder(DeltaUtil.applyDelta(content.toString(), delta));

                if (version.getVersionNumber().equals(versionNumber)) {
                    break;
                }
            }

            return content.toString();
        }
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
