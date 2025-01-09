// com/example/securedrive/service/impl/FileFacadeServiceImpl.java
package com.example.securedrive.service.impl;

import com.example.securedrive.dto.*;
import com.example.securedrive.mapper.UserMapper;
import com.example.securedrive.model.*;
import com.example.securedrive.repository.FileRepository;
import com.example.securedrive.repository.FileShareRepository;
import com.example.securedrive.repository.FileVersionRepository;
import com.example.securedrive.repository.UserRepository;
import com.example.securedrive.service.util.AESUtil;
import com.example.securedrive.service.util.BinaryDeltaUtil;
import com.example.securedrive.service.util.DeltaUtil;
import com.example.securedrive.service.util.HashUtil;
import com.example.securedrive.security.KeyVaultService;
import com.example.securedrive.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;


@Service
public class FileFacadeServiceImpl implements FileFacadeService {

    private final AzureBlobStorageServiceImpl azureBlobStorage;
    private final UserManagementService userManagementService;
    private final FileManagementService fileManagementService;
    private final FileVersionManagementService fileVersionManagementService;
    private final FileShareRepository fileShareRepository;
    private final KeyVaultService keyVaultService;
    private final DirectoryService directoryService;
    private final FileVersionRepository fileVersionRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private static final Logger logger = LoggerFactory.getLogger(FileFacadeServiceImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FileFacadeServiceImpl(AzureBlobStorageServiceImpl azureBlobStorage,
                                 UserManagementService userManagementService,
                                 FileManagementService fileManagementService,
                                 FileVersionManagementService fileVersionManagementService,
                                 FileShareRepository fileShareRepository,
                                 KeyVaultService keyVaultService,
                                 DirectoryService directoryService,
                                 FileVersionRepository fileVersionRepository,
                                 FileRepository fileRepository,
                                 UserRepository userRepository,
                                 UserMapper userMapper) {
        this.azureBlobStorage = azureBlobStorage;
        this.userManagementService = userManagementService;
        this.fileManagementService = fileManagementService;
        this.fileVersionManagementService = fileVersionManagementService;
        this.fileShareRepository = fileShareRepository;
        this.keyVaultService = keyVaultService;
        this.directoryService = directoryService;
        this.fileVersionRepository = fileVersionRepository;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }
    @Override
    public void revokeShare(FileRevokeShareRequestDto dto, Authentication authentication) {
        UserDto authenticatedUser = userMapper.toUserDTO(authentication);

        // Yetki kontrolü
        if (!authenticatedUser.getUsername().equals(dto.getUsername()) && !authenticatedUser.hasRole("ROLE_ADMIN")) {
            throw new SecurityException("Bu kullanıcı için paylaşımı iptal etme yetkiniz yok.");
        }

        User owner = userManagementService.findByUsername(dto.getUsername())
                .orElseThrow(() -> new NoSuchElementException("Kullanıcı bulunamadı."));
        User sharedWithUser = userManagementService.findByEmail(dto.getSharedWithEmail())
                .orElseThrow(() -> new NoSuchElementException("Paylaşım yapılan kullanıcı bulunamadı."));
        File file = fileManagementService.findById(dto.getFileId())
                .orElseThrow(() -> new NoSuchElementException("Dosya bulunamadı."));

        // Sahiplik kontrolü
        if (!file.getUser().equals(owner)) {
            throw new SecurityException("Bu dosyanın sahibi değilsiniz.");
        }

        // Versiyon bazlı paylaşımı iptal et
        FileShare fileShare = fileShareRepository.findByFileAndSharedWithUserAndVersion(file, sharedWithUser, dto.getVersion())
                .orElseThrow(() -> new NoSuchElementException("Paylaşımı iptal etmek için gerekli bilgiler bulunamadı."));

        fileShareRepository.delete(fileShare);
    }


    @Override
    public void shareFile(FileShareRequestDto dto, Authentication authentication) {
        String username = dto.getUsername();
        Long fileId = dto.getFileId();
        String sharedWithEmail = dto.getSharedWithEmail();
        String version = dto.getVersion();

        UserDto authenticatedUser = userMapper.toUserDTO(authentication);
        if (authenticatedUser.getUsername() == null) {
            throw new SecurityException("Authenticated user's username could not be determined.");
        }
        if (!authenticatedUser.getUsername().equals(username) && !authenticatedUser.hasRole("ROLE_ADMIN")) {
            throw new SecurityException("Bu kullanıcı için dosya paylaşma yetkiniz yok.");
        }

        User owner = userManagementService.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Kullanıcı bulunamadı."));
        User sharedWithUser = userManagementService.findByEmail(sharedWithEmail)
                .orElseThrow(() -> new NoSuchElementException("Paylaşım yapılacak kullanıcı bulunamadı."));
        File file = fileManagementService.findByIdAndUser(fileId, owner)
                .orElseThrow(() -> new NoSuchElementException("Dosya bulunamadı."));

        fileManagementService.shareFileWithUser(file, owner, sharedWithUser, version);
    }

    @Override
    public int getRemainingShares(Long fileId) {
        List<FileShare> fileShares = fileShareRepository.findByFileId(fileId);
        return fileShares.size();
    }


    @Override
    public FileDownloadSharedResponseDto downloadSharedFile(FileDownloadSharedRequestDto dto) throws Exception {
        String username = dto.getUsername();
        Long sharedFileId = dto.getFileId();

        logger.info("Starting download for sharedFileId: {}, username: {}", sharedFileId, username);
        Optional<User> sharedWithUserOptional = userManagementService.findByUsername(username);
        if (sharedWithUserOptional.isEmpty()) {
            throw new RuntimeException("User not found.");
        }
        User sharedWithUser = sharedWithUserOptional.get();
        FileShare fileShare = fileShareRepository.findById(sharedFileId)
                .orElseThrow(() -> new IllegalArgumentException("FileShare not found for id: " + sharedFileId));
        if (!fileShare.getSharedWithUser().getId().equals(sharedWithUser.getId())) {
            logger.error("User {} does not have access to FileShare id: {}", username, sharedFileId);
            throw new RuntimeException("You do not have access to this file.");
        }
        File file = fileShare.getFile();
        if (file == null) {
            throw new RuntimeException("File not found for FileShare id: " + sharedFileId);
        }
        User owner = file.getUser();
        String fileName = file.getFileName();
        String version = fileShare.getVersion();
        String fileNameLower = fileName.toLowerCase();
        boolean isBinary = fileNameLower.matches(".*\\.(jpg|png|mp4|docx?|xlsx|pdf|pptx|mkv)$");

        byte[] originalData;
        String encryptionKey = keyVaultService.getEncryptionKeyFromKeyVault(owner.getUsername());
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            throw new RuntimeException("Encryption key not found.");
        }

        if (isBinary) {
            String reconstructedContentBase64 = fileVersionManagementService.reconstructFileContent(file, version, sharedWithUser);
            originalData = Base64.getDecoder().decode(reconstructedContentBase64);
        } else {
            String reconstructedContent = fileVersionManagementService.reconstructFileContent(file, version, owner);
            originalData = reconstructedContent.getBytes(StandardCharsets.UTF_8);
        }

        ByteArrayResource resource = new ByteArrayResource(originalData);

        return new FileDownloadSharedResponseDto(originalData, fileName, resource);
    }



    @Override
    public String uploadFile(FileUploadRequestDto dto) {
        try {
            User currentUser = userManagementService.findByUsername(dto.getUsername())
                    .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + dto.getUsername()));
            logger.info("Kullanıcı doğrulandı: {}", currentUser.getUsername());

            String aesKey = resolveOrCreateAESKey(dto.getUsername());

            Directory directory = null;
            if (dto.getDirectoryId() != null) {
                directory = directoryService.findByIdAndUser(dto.getDirectoryId(), currentUser)
                        .orElseThrow(() -> new RuntimeException("Dizin bulunamadı."));
                logger.info("Dizin bulundu: {}", directory.getName());
            }

            String uniqueFilePath = (directory != null)
                    ? String.format("uploads/%s/%s", dto.getUsername(), getDirectoryPath(directory, dto.getFile().getOriginalFilename()))
                    : String.format("uploads/%s/%s", dto.getUsername(), dto.getFile().getOriginalFilename());
            logger.info("Benzersiz dosya yolu oluşturuldu: {}", uniqueFilePath);

            File userFile = (directory != null)
                    ? fileManagementService.findByFileNameUserAndDirectory(dto.getFile().getOriginalFilename(), currentUser, directory)
                    : fileManagementService.findByFileNameAndUserDirectoryNull(dto.getFile().getOriginalFilename(), currentUser);
            logger.info("Dosya kontrolü tamamlandı.");

            String fileHash = HashUtil.calculateHash(dto.getFile().getBytes());

            if (userFile != null) {
                Optional<FileVersion> duplicateVersion = userFile.getVersions().stream()
                        .filter(version -> version.getHash().equals(fileHash))
                        .findFirst();
                if (duplicateVersion.isPresent()) {
                    String existingVersion = duplicateVersion.get().getVersionNumber();
                    logger.warn("Aynı dosya zaten mevcut: {} - Versiyon: {}", dto.getFile().getOriginalFilename(), existingVersion);
                    return String.format("Bu dosya bu dizinde zaten mevcut. Mevcut versiyon: %s", existingVersion);
                }
            } else {
                userFile = new File();
                userFile.setFileName(dto.getFile().getOriginalFilename());
                userFile.setPath(uniqueFilePath);
                userFile.setUser(currentUser);
                userFile.setDirectory(directory);
                fileManagementService.saveFile(userFile);
                logger.info("Yeni dosya oluşturuldu: {}", dto.getFile().getOriginalFilename());
            }

            String versionNumber = (dto.getVersionNumber() != null)
                    ? dto.getVersionNumber()
                    : fileVersionManagementService.generateNextVersion(userFile);
            logger.info("Versiyon numarası oluşturuldu: {}", versionNumber);

            processFile(dto, aesKey, uniqueFilePath, userFile, versionNumber);

            return "Dosya başarıyla yüklendi. Versiyon: " + versionNumber;

        } catch (Exception e) {
            logger.error("Dosya yükleme sırasında hata oluştu: {}", e.getMessage(), e);
            return "Dosya yükleme başarısız: " + e.getMessage();
        }
    }


    @Override
    public String deleteSpecificVersion(FileDeleteSpecificVersionRequestDto dto) {
        Optional<User> userOptional = userRepository.findByUsername(dto.getUsername());
        if (userOptional.isEmpty()) {
            return "Kullanıcı bulunamadı.";
        }
        User user = userOptional.get();

        Optional<File> fileOptional = fileRepository.findByIdAndUser(dto.getFileId(), user);
        if (fileOptional.isEmpty()) {
            return "Dosya bulunamadı.";
        }
        File file = fileOptional.get();

        FileVersion versionToDelete = fileVersionRepository.findByFileAndVersionNumber(file, dto.getVersionNumber());
        if (versionToDelete == null) {
            return "Versiyon bulunamadı.";
        }

        try {
            String versionFilePath = String.format("%s/versions/%s/%s", file.getPath(), dto.getVersionNumber(), file.getFileName());
            if (azureBlobStorage.exists(new Storage(versionFilePath, null))) {
                azureBlobStorage.delete(new Storage(versionFilePath, null));
            }

            if (versionToDelete.getDeltaPath() != null) {
                String deltaFilePath = versionToDelete.getDeltaPath();
                if (azureBlobStorage.exists(new Storage(deltaFilePath, null))) {
                    azureBlobStorage.delete(new Storage(deltaFilePath, null));
                }
            }

            fileVersionRepository.delete(versionToDelete);

            if (fileVersionRepository.findAllByFile(file).isEmpty()) {
                fileRepository.delete(file);
            }
            return "Dosya ve versiyon başarıyla silindi.";
        } catch (Exception e) {
            return "Dosya silme işlemi başarısız oldu: " + e.getMessage();
        }
    }

    @Override
    public ByteArrayResource downloadSpecificVersion(FileDownloadSpecificVersionRequestDto dto) throws Exception {
        User user = userManagementService.findByUsername(dto.getUsername())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + dto.getUsername()));
        logger.info("Kullanıcı doğrulandı: {}", user.getUsername());

        File file = fileManagementService.findByIdAndUser(dto.getFileId(), user)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı: " + dto.getFileId()));
        logger.info("Dosya doğrulandı: {}", file.getFileName());

        String fileName = file.getFileName();
        boolean isBinary = fileName.toLowerCase().matches(".*\\.(jpg|png|mp4|docx?|xlsx|pdf|pptx|mkv)$");

        byte[] originalData;

        if (isBinary) {
            // Binary dosya işlemleri
            // Reconstruct the content by applying deltas up to the specified version
            String reconstructedContentBase64 = fileVersionManagementService.reconstructFileContent(file, dto.getVersionNumber(), user);
            // Decode Base64 to get byte[]
            originalData = Base64.getDecoder().decode(reconstructedContentBase64);
        } else {
            // Metin dosya işlemleri
            String reconstructedContent = fileVersionManagementService.reconstructFileContent(file, dto.getVersionNumber(), user);
            originalData = reconstructedContent.getBytes(StandardCharsets.UTF_8);
        }

        return new ByteArrayResource(originalData);
    }

    private void processFile(FileUploadRequestDto dto, String aesKey, String uniqueFilePath, File userFile, String versionNumber) throws Exception {
        String fileNameLower = Objects.requireNonNull(dto.getFile().getOriginalFilename()).toLowerCase();
        byte[] fileData = dto.getFile().getBytes();
        String fileHash = HashUtil.calculateHash(fileData);


        // Binary veya metin dosya ayrımı
        boolean isBinaryFile = fileNameLower.matches(".*\\.(jpg|png|mp4|docx?|xlsx|pdf|pptx|mkv)$");

        if (isBinaryFile) {
            // Binary dosya işlemleri
            processBinaryFile(fileData, aesKey, uniqueFilePath, userFile, versionNumber, fileHash);
        } else {
            // Metin dosyası işlemleri
            processTextFile(fileData, aesKey, uniqueFilePath, userFile, versionNumber, fileHash);
        }
    }

    private void processBinaryFile(byte[] fileData, String aesKey, String uniqueFilePath, File userFile, String versionNumber, String fileHash) throws Exception {
        List<FileVersion> versions = fileVersionManagementService.getAllVersions(userFile);
        byte[] previousData = null;
        String deltaPath = null;

        if (!versions.isEmpty()) { // İlk versiyon değilse
            FileVersion previousVersion = versions.get(versions.size() - 1);
            String previousFilePath = String.format("%s/versions/%s/%s",
                    userFile.getPath(),
                    previousVersion.getVersionNumber(),
                    userFile.getFileName());

            if (azureBlobStorage.checkBlobExists(previousFilePath)) {
                byte[] previousEncryptedDataBase64 = azureBlobStorage.read(new Storage(previousFilePath, null));
                byte[] previousEncryptedData = Base64.getDecoder().decode(previousEncryptedDataBase64);
                previousData = AESUtil.decrypt(previousEncryptedData, aesKey);
            }
        }

        if (versionNumber.equals("v1")) {
            byte[] encryptedData = AESUtil.encrypt(fileData, aesKey);
            logger.info("Şifrelenmiş veri uzunluğu: {}", encryptedData.length);

            String versionedFilePath = uniqueFilePath + "/versions/" + versionNumber + "/" + userFile.getFileName();
            saveBlobToAzure(versionedFilePath, encryptedData);
        } else {
            // Sonraki versiyonlar: Sadece delta.json kaydet
            if (previousData != null) {
                // BinaryDeltaUtil ile delta hesaplama
                List<BinaryDeltaUtil.DeltaCommand> deltaCommands = BinaryDeltaUtil.calculateDelta(previousData, fileData);
                if (!deltaCommands.isEmpty()) {
                    // Delta komutlarını JSON olarak kaydet
                    deltaPath = uniqueFilePath + "/versions/" + versionNumber + "/delta.json";
                    saveBinaryDeltaCommands(deltaCommands, deltaPath);
                }
            }
        }

        FileVersion version = fileVersionManagementService.createVersion(userFile, versionNumber, deltaPath);
        version.setHash(fileHash);
        version.setSize((long) fileData.length);
        fileVersionManagementService.saveFileVersion(version);
    }



    // Metin dosyalarını işleme
    private void processTextFile(byte[] fileData, String aesKey, String uniqueFilePath, File userFile, String versionNumber, String fileHash) throws Exception {
        String newContent = new String(fileData, StandardCharsets.UTF_8);
        logger.info("Yeni içerik: {}", newContent);

        if (versionNumber.equals("v1")) {
            // İlk versiyon: Şifreleyip kaydet
            byte[] encryptedData = AESUtil.encrypt(newContent.getBytes(StandardCharsets.UTF_8), aesKey);
            String versionedFilePath = uniqueFilePath + "/versions/" + versionNumber + "/" + userFile.getFileName();
            saveBlobToAzure(versionedFilePath, encryptedData);

            FileVersion version = fileVersionManagementService.createVersion(userFile, versionNumber, null);
            version.setHash(fileHash);
            version.setSize((long) fileData.length);
            fileVersionManagementService.saveFileVersion(version);
        } else {
            // Son içerik al ve delta hesapla
            String latestContent = fileVersionManagementService.getLatestContent(userFile, userFile.getUser());
            logger.info("Son içerik: {}", latestContent);

            String delta = DeltaUtil.calculateDelta(latestContent, newContent);
            String deltaPath = uniqueFilePath + "/versions/" + versionNumber + "/delta";
            saveBlobToAzure(deltaPath, delta.getBytes(StandardCharsets.UTF_8));

            FileVersion version = fileVersionManagementService.createVersion(userFile, versionNumber, deltaPath);
            version.setHash(fileHash);
            version.setSize((long) fileData.length);
            fileVersionManagementService.saveFileVersion(version);
        }
    }



    private void saveBlobToAzure(String path, byte[] data) throws Exception {
        String base64EncodedData = Base64.getEncoder().encodeToString(data);
        logger.info("Azure Blob yazma işlemi başlatıldı. Path: {}, Veri boyutu: {}", path, data.length);
        azureBlobStorage.write(new Storage(path, base64EncodedData.getBytes(StandardCharsets.UTF_8)));
        logger.info("Blob başarıyla yazıldı: {}", path);
    }

    private String resolveOrCreateAESKey(String username) throws NoSuchAlgorithmException {
        String aesKey = keyVaultService.getEncryptionKeyFromKeyVault(username);
        if (aesKey == null || aesKey.isEmpty()) {
            aesKey = AESUtil.generateAESKey();
            keyVaultService.saveEncryptionKeyToKeyVault(username, aesKey);
            logger.info("Kullanıcı için yeni AES anahtarı oluşturuldu: {}", username);
        }
        return aesKey;
    }

    private String getDirectoryPath(Directory directory, String fileName) {
        StringBuilder path = new StringBuilder();
        Directory currentDirectory = directory;
        while (currentDirectory != null) {
            path.insert(0, currentDirectory.getId() + "/");
            currentDirectory = currentDirectory.getParentDirectory();
        }
        path.append(fileName);
        return path.toString();
    }


    private void saveBinaryDeltaCommands(List<BinaryDeltaUtil.DeltaCommand> deltaCommands, String deltaPath) throws Exception {
        // Serialize deltaCommands to JSON
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(deltaCommands);

        // Optionally compress the JSON (e.g., GZIP). Bu örnekte sıkıştırma yapılmamıştır.
        byte[] jsonData = json.getBytes(StandardCharsets.UTF_8);

        // Encode JSON data to Base64
        byte[] base64EncodedData = Base64.getEncoder().encode(jsonData);

        // Save Base64-encoded JSON to Azure Blob Storage
        azureBlobStorage.write(new Storage(deltaPath, base64EncodedData));

        logger.info("Delta komutları başarıyla kaydedildi: {}", deltaPath);
    }
}

