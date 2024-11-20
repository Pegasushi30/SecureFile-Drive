package com.example.securedrive.controller;

import com.example.securedrive.exception.AzureBlobStorageException;
import com.example.securedrive.model.File;
import com.example.securedrive.model.FileVersion;
import com.example.securedrive.model.Storage;
import com.example.securedrive.model.User;
import com.example.securedrive.security.AESUtil;
import com.example.securedrive.security.DeltaUtil;
import com.example.securedrive.service.FileService;
import com.example.securedrive.service.FileVersionService;
import com.example.securedrive.service.UserService;
import com.example.securedrive.service.impl.AzureBlobStorageImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@RestController
@RequestMapping("/files")
public class AzureBlobController {

    @Autowired
    private AzureBlobStorageImpl azureBlobStorage;

    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileVersionService fileVersionService;

    private static final Logger logger = LoggerFactory.getLogger(AzureBlobController.class);

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("username") String username,
            @RequestParam(value = "versionNumber", required = false) String manualVersionNumber,
            Authentication authentication) {

        logger.info("Upload request received for user: {}", username);

        if (file.isEmpty()) {
            logger.warn("File upload failed: File is missing or empty!");
            return ResponseEntity.badRequest().body("File is missing or empty!");
        }

        if (!authentication.getName().equals(username)) {
            logger.warn("Unauthorized upload attempt by: {}", authentication.getName());
            return ResponseEntity.status(403).body("You are not authorized to upload files for this user.");
        }

        Optional<User> currentUserOptional = userService.findByUsername(username);
        if (currentUserOptional.isEmpty()) {
            logger.warn("User not found: {}", username);
            return ResponseEntity.status(401).body("User not found.");
        }

        User currentUser = currentUserOptional.get();

        try {
            String aesKey = currentUser.getEncryptionKey();
            if (aesKey == null || aesKey.isEmpty()) {
                aesKey = AESUtil.generateAESKey();
                currentUser.setEncryptionKey(aesKey);
                userService.saveUser(currentUser);
                logger.info("Generated new AES key for user: {}", username);
            }

            String uniqueFilePath = "uploads/" + username + "/" + file.getOriginalFilename();
            logger.info("Generated unique file path: {}", uniqueFilePath);

            File userFile = fileService.findByFileNameAndUser(file.getOriginalFilename(), currentUser);
            if (userFile == null) {
                userFile = new File();
                userFile.setFileName(file.getOriginalFilename());
                userFile.setPath(uniqueFilePath);
                userFile.setUser(currentUser);
                fileService.saveFile(userFile);
                logger.info("File metadata created and saved for file: {}", file.getOriginalFilename());
            }

            String versionNumber = (manualVersionNumber != null)
                    ? manualVersionNumber
                    : fileVersionService.generateNextVersion(userFile);
            logger.info("Version number generated: {}", versionNumber);

            String newContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            if (versionNumber.equals("v1")) {
                // İlk sürüm: Tam şifreli dosyayı sakla
                byte[] encryptedData = AESUtil.encrypt(newContent.getBytes(StandardCharsets.UTF_8), aesKey);
                String base64EncodedData = Base64.getEncoder().encodeToString(encryptedData);
                String encryptedFileFullPath = uniqueFilePath + "/versions/" + versionNumber + "/" + file.getOriginalFilename();
                byte[] base64EncodedDataBytes = base64EncodedData.getBytes(StandardCharsets.UTF_8);
                azureBlobStorage.write(new Storage(encryptedFileFullPath, base64EncodedDataBytes));
                logger.info("Encrypted file uploaded to Blob Storage at: {}", encryptedFileFullPath);

                // Versiyon bilgisini kaydet
                FileVersion version = fileVersionService.createVersion(userFile, versionNumber, null);
                fileVersionService.saveFileVersion(version);
                logger.info("File version saved successfully for version: {}", versionNumber);
            } else {
                // Sonraki sürümler: Sadece delta dosyasını sakla
                String latestContent = fileVersionService.getLatestContent(userFile, currentUser);
                String delta = DeltaUtil.calculateDelta(latestContent, newContent);
                logger.info("Delta calculated successfully for version: {}", versionNumber);

                String deltaFullPath = uniqueFilePath + "/versions/" + versionNumber + "/delta";
                byte[] deltaBytes = delta.getBytes(StandardCharsets.UTF_8);
                azureBlobStorage.write(new Storage(deltaFullPath, deltaBytes));
                logger.info("Delta file uploaded to Blob Storage at: {}", deltaFullPath);

                // Versiyon bilgisini kaydet
                FileVersion version = fileVersionService.createVersion(userFile, versionNumber, deltaFullPath);
                fileVersionService.saveFileVersion(version);
                logger.info("File version saved successfully for version: {}", versionNumber);
            }

            return ResponseEntity.ok("File successfully uploaded with version: " + versionNumber);
        } catch (Exception e) {
            logger.error("File upload failed for user: {}. Error: {}", username, e.getMessage(), e);
            return ResponseEntity.status(500).body("File upload failed: " + e.getMessage());
        }
    }


    @GetMapping("/download/{fileName}/{versionNumber}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #authentication.name == principal.username")
    public ResponseEntity<Resource> downloadSpecificVersion(
            @PathVariable("fileName") String fileName,
            @PathVariable("versionNumber") String versionNumber,
            Authentication authentication) {

        Optional<User> currentUser = userService.findByUsername(authentication.getName());
        if (currentUser.isEmpty()) {
            logger.error("User not found: {}", authentication.getName());
            return ResponseEntity.status(401).body(null);
        }

        User user = currentUser.get();
        File file = fileService.findByFileNameAndUser(fileName, user);
        if (file == null) {
            logger.error("File not found: {}", fileName);
            return ResponseEntity.status(404).body(null);
        }

        try {
            String reconstructedContent = fileVersionService.reconstructFileContent(file, versionNumber, user);
            byte[] originalData = reconstructedContent.getBytes(StandardCharsets.UTF_8);

            ByteArrayResource resource = new ByteArrayResource(originalData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentLength(originalData.length)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error during file download: ", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @DeleteMapping("/delete/{fileName}/{versionNumber}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #authentication.name == principal.username")
    public ResponseEntity<String> deleteSpecificVersion(
            @PathVariable("fileName") String fileName,
            @PathVariable("versionNumber") String versionNumber,
            Authentication authentication) {

        Optional<User> currentUser = userService.findByUsername(authentication.getName());
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        User user = currentUser.get();
        File file = fileService.findByFileNameAndUser(fileName, user);
        if (file == null) {
            return ResponseEntity.status(404).body("File not found");
        }

        try {
            FileVersion version = fileVersionService.getVersionByFileAndNumber(file, versionNumber);
            if (version == null) {
                return ResponseEntity.status(404).body("Version not found");
            }

            if (versionNumber.equals("v1")) {
                // İlk sürümse, tam şifreli dosyayı sil
                String encryptedFilePath = "uploads/" + user.getUsername() + "/" + fileName + "/versions/" + versionNumber + "/" + fileName;
                azureBlobStorage.delete(new Storage(encryptedFilePath, null));
            } else {
                // Diğer sürümlerde delta dosyasını sil
                if (version.getDeltaPath() != null) {
                    azureBlobStorage.delete(new Storage(version.getDeltaPath(), null));
                }
            }

            // Veritabanından versiyonu sil
            fileVersionService.deleteFileVersion(version);

            return ResponseEntity.ok("Version deleted: " + versionNumber);
        } catch (Exception e) {
            logger.error("Error during version deletion: ", e);
            return ResponseEntity.status(500).body("Failed to delete version");
        }
    }

}
