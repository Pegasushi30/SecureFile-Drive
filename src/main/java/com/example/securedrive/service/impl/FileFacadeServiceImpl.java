// com/example/securedrive/service/impl/FileFacadeServiceImpl.java
package com.example.securedrive.service.impl;

import com.azure.spring.cloud.core.implementation.factory.credential.UsernamePasswordCredentialBuilderFactory;
import com.example.securedrive.dto.*;
import com.example.securedrive.exception.UnauthorizedAccessException;
import com.example.securedrive.mapper.UserMapper;
import com.example.securedrive.model.*;
import com.example.securedrive.repository.FileRepository;
import com.example.securedrive.repository.FileShareRepository;
import com.example.securedrive.repository.FileVersionRepository;
import com.example.securedrive.repository.UserRepository;
import com.example.securedrive.security.AESUtil;
import com.example.securedrive.security.DeltaUtil;
import com.example.securedrive.security.HashUtil;
import com.example.securedrive.security.KeyVaultService;
import com.example.securedrive.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class FileFacadeServiceImpl implements FileFacadeService {

    private final AzureBlobStorageImpl azureBlobStorage;
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
    private final UsernamePasswordCredentialBuilderFactory usernamePasswordCredentialBuilderFactory;

    public FileFacadeServiceImpl(AzureBlobStorageImpl azureBlobStorage,
                                 UserManagementService userManagementService,
                                 FileManagementService fileManagementService,
                                 FileVersionManagementService fileVersionManagementService,
                                 FileShareRepository fileShareRepository,
                                 KeyVaultService keyVaultService,
                                 DirectoryService directoryService,
                                 FileVersionRepository fileVersionRepository,
                                 FileRepository fileRepository,
                                 UserRepository userRepository,
                                 UserMapper userMapper, UsernamePasswordCredentialBuilderFactory usernamePasswordCredentialBuilderFactory) {
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
        this.usernamePasswordCredentialBuilderFactory = usernamePasswordCredentialBuilderFactory;
    }

    @Override
    public void revokeShare(FileRevokeShareRequestDto dto, Authentication authentication) {

        UserDto authenticatedUser = userMapper.toUserDTO(authentication);

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
        FileShare fileShare = fileShareRepository.findByFileAndSharedWithUser(file, sharedWithUser)
                .orElseThrow(() -> new NoSuchElementException("Paylaşımı iptal etmek için gerekli bilgiler bulunamadı."));

        fileShareRepository.delete(fileShare);
    }

    @Override
    public void shareFile(FileShareRequestDto dto, Authentication authentication) {

        // Extract details from DTO
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

        User user = userManagementService.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Kullanıcı bulunamadı."));
        User sharedWithUser = userManagementService.findByEmail(sharedWithEmail)
                .orElseThrow(() -> new NoSuchElementException("Paylaşım yapılacak kullanıcı bulunamadı."));
        File file = fileManagementService.findByIdAndUser(fileId, user)
                .orElseThrow(() -> new NoSuchElementException("Dosya bulunamadı."));

        fileManagementService.shareFileWithUser(file, user, sharedWithUser, version);
    }

    @Override
    public FileDownloadSharedResponseDto downloadSharedFile(FileDownloadSharedRequestDto dto) throws Exception {
        String username = dto.getUsername();
        Long sharedFileId = dto.getFileId(); // Bu aslında FileShare tablosundaki id

        logger.info("Starting download for sharedFileId: {}, username: {}", sharedFileId, username);

        // Kullanıcıyı doğrula
        Optional<User> sharedWithUserOptional = userManagementService.findByUsername(username);
        if (sharedWithUserOptional.isEmpty()) {
            throw new RuntimeException("User not found.");
        }
        User sharedWithUser = sharedWithUserOptional.get();

        // FileShare kaydını getir
        FileShare fileShare = fileShareRepository.findById(sharedFileId)
                .orElseThrow(() -> new IllegalArgumentException("FileShare not found for id: " + sharedFileId));

        // Kullanıcının erişim yetkisi var mı kontrol et
        if (!fileShare.getSharedWithUser().getId().equals(sharedWithUser.getId())) {
            logger.error("User {} does not have access to FileShare id: {}", username, sharedFileId);
            throw new RuntimeException("You do not have access to this file.");
        }

        // FileShare'den file_id al ve File tablosundan kaydı getir
        File file = fileShare.getFile();
        if (file == null) {
            throw new RuntimeException("File not found for FileShare id: " + sharedFileId);
        }

        // Gerekli bilgileri al
        User owner = fileShare.getOwner();
        String fileName = file.getFileName();
        String version = fileShare.getVersion(); // FileShare'den versiyon al

        // Dosya tipini kontrol et
        String fileNameLower = fileName.toLowerCase();
        boolean isBinary = fileNameLower.matches(".*\\.(jpg|png|mp4|docx?|xlsx|pdf|pptx)$");

        byte[] originalData;

        // Şifreleme anahtarını al
        String encryptionKey = keyVaultService.getEncryptionKeyFromKeyVault(owner.getUsername());
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            throw new RuntimeException("Encryption key not found.");
        }

        if (isBinary) {
            // Binary dosya işlemleri
            String versionedFilePath = String.format(
                    "%s/versions/%s/%s",
                    file.getPath(),
                    version,
                    fileName
            );

            if (!azureBlobStorage.checkBlobExists(versionedFilePath)) {
                throw new RuntimeException("Blob not found: " + versionedFilePath);
            }

            byte[] base64EncodedData = azureBlobStorage.read(new Storage(versionedFilePath, null));
            byte[] encryptedData = Base64.getDecoder().decode(base64EncodedData);
            originalData = AESUtil.decrypt(encryptedData, encryptionKey);
        } else {
            // Metin dosya işlemleri
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
            logger.info("Hesaplanan dosya hash'i: {}", fileHash);

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
        boolean isBinary = fileName.toLowerCase().matches(".*\\.(jpg|png|mp4|docx?|xlsx|pdf|pptx)$");

        byte[] originalData;

        if (isBinary) {
            String versionedFilePath = String.format("%s/versions/%s/%s", file.getPath(), dto.getVersionNumber(), fileName);
            logger.info("Binary dosya yolu: {}", versionedFilePath);

            byte[] base64EncodedData = azureBlobStorage.read(new Storage(versionedFilePath, null));
            logger.info("Azure Blob'dan veri okundu. Boyut: {}", base64EncodedData.length);

            byte[] encryptedData = Base64.getDecoder().decode(base64EncodedData);
            String encryptionKey = keyVaultService.getEncryptionKeyFromKeyVault(user.getUsername());
            logger.info("Şifreleme anahtarı alındı: {}", encryptionKey);

            originalData = AESUtil.decrypt(encryptedData, encryptionKey);
            logger.info("Şifre çözme işlemi tamamlandı. Boyut: {}", originalData.length);
        } else {
            String reconstructedContent = fileVersionManagementService.reconstructFileContent(file, dto.getVersionNumber(), user);
            logger.info("Yeniden oluşturulan içerik: {}", reconstructedContent);

            originalData = reconstructedContent.getBytes(StandardCharsets.UTF_8);
        }

        return new ByteArrayResource(originalData);
    }

    private void processFile(FileUploadRequestDto dto, String aesKey, String uniqueFilePath, File userFile, String versionNumber) throws Exception {
        String fileNameLower = Objects.requireNonNull(dto.getFile().getOriginalFilename()).toLowerCase();
        byte[] fileData = dto.getFile().getBytes();
        String fileHash = HashUtil.calculateHash(fileData);

        logger.info("Dosya ismi: {}, Dosya hash'i: {}", fileNameLower, fileHash);

        if (fileNameLower.matches(".*\\.(jpg|png|mp4|docx?|xlsx|pdf|pptx)$")) {
            byte[] encryptedData = AESUtil.encrypt(fileData, aesKey);
            logger.info("Şifrelenmiş veri uzunluğu: {}", encryptedData.length);
            String versionedFilePath = uniqueFilePath + "/versions/" + versionNumber + "/" + dto.getFile().getOriginalFilename();
            saveBlobToAzure(versionedFilePath, encryptedData);

            FileVersion version = fileVersionManagementService.createVersion(userFile, versionNumber, null);
            version.setHash(fileHash);
            fileVersionManagementService.saveFileVersion(version);
        } else {
            String newContent = new String(fileData, StandardCharsets.UTF_8);
            logger.info("Yeni içerik: {}", newContent);

            if (versionNumber.equals("v1")) {
                byte[] encryptedData = AESUtil.encrypt(newContent.getBytes(StandardCharsets.UTF_8), aesKey);
                logger.info("Şifrelenmiş veri uzunluğu (v1): {}", encryptedData.length);
                String versionedFilePath = uniqueFilePath + "/versions/" + versionNumber + "/" + dto.getFile().getOriginalFilename();
                saveBlobToAzure(versionedFilePath, encryptedData);

                FileVersion version = fileVersionManagementService.createVersion(userFile, versionNumber, null);
                version.setHash(fileHash);
                fileVersionManagementService.saveFileVersion(version);
            } else {
                String latestContent = fileVersionManagementService.getLatestContent(userFile, userFile.getUser());
                logger.info("Son içerik: {}", latestContent);

                String delta = DeltaUtil.calculateDelta(latestContent, newContent);
                logger.info("Oluşturulan delta: {}", delta);

                String deltaPath = uniqueFilePath + "/versions/" + versionNumber + "/delta";
                saveBlobToAzure(deltaPath, delta.getBytes(StandardCharsets.UTF_8));

                FileVersion version = fileVersionManagementService.createVersion(userFile, versionNumber, deltaPath);
                version.setHash(fileHash);
                fileVersionManagementService.saveFileVersion(version);
            }
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
}

