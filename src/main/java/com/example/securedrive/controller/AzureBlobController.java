package com.example.securedrive.controller;

import com.azure.storage.blob.models.BlobStorageException;
import com.example.securedrive.model.*;
import com.example.securedrive.repository.FileShareRepository;
import com.example.securedrive.security.AESUtil;
import com.example.securedrive.security.DeltaUtil;
import com.example.securedrive.security.HashUtil;
import com.example.securedrive.security.KeyVaultService;
import com.example.securedrive.service.DirectoryService;
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
import org.springframework.web.servlet.ModelAndView;

import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("files")
public class AzureBlobController {

    @Autowired
    private AzureBlobStorageImpl azureBlobStorage;

    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileVersionService fileVersionService;

    @Autowired
    private FileShareRepository fileShareRepository;

    @Autowired
    private KeyVaultService keyVaultService;

    @Autowired
    private DirectoryService directoryService; // Added DirectoryService

    private static final Logger logger = LoggerFactory.getLogger(AzureBlobController.class);

    @PostMapping("/revoke-share/{fileId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    public ModelAndView revokeShare(
            @PathVariable Long fileId,
            @RequestParam("sharedWithEmail") String sharedWithEmail,
            @RequestParam("username") String username,
            @RequestParam(value = "directoryId", required = false) Long directoryId, // directoryId optional
            Authentication authentication) {

        ModelAndView modelAndView = new ModelAndView();

        // Yetki kontrolü
        if (!authentication.getName().equals(username) && authentication.getAuthorities().stream()
                .noneMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Bu kullanıcı için paylaşımı iptal etme yetkiniz yok.");
            return modelAndView;
        }

        // Kullanıcıları al
        Optional<User> ownerOptional = userService.findByUsername(username);
        Optional<User> sharedWithUserOptional = userService.findByEmail(sharedWithEmail);

        if (ownerOptional.isEmpty() || sharedWithUserOptional.isEmpty()) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Kullanıcı bulunamadı.");
            return modelAndView;
        }

        User owner = ownerOptional.get();
        User sharedWithUser = sharedWithUserOptional.get();

        // FileShare'ı bul
        File file = fileService.findById(fileId).orElseThrow(() -> new IllegalArgumentException("File not found"));
        Optional<FileShare> fileShareOptional = fileShareRepository.findByFileAndSharedWithUser(file, sharedWithUser);

        if (fileShareOptional.isEmpty()) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Paylaşımı iptal etmek için gerekli bilgiler bulunamadı.");
            return modelAndView;
        }

        // Paylaşımı iptal et
        fileShareRepository.delete(fileShareOptional.get());

        // directoryId null ise /directories yönlendir
        if (directoryId == null) {
            modelAndView.setViewName("redirect:/directories");
        } else {
            modelAndView.setViewName("redirect:/directories/" + directoryId);
        }

        return modelAndView;
    }



    @PostMapping("/share")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    public ModelAndView shareFile(
            @RequestParam("username") String username,
            @RequestParam("fileId") Long fileId,
            @RequestParam("sharedWithEmail") String sharedWithEmail,
            @RequestParam(value = "directoryId", required = false) Long directoryId,
            @RequestParam("version") String version,
            Authentication authentication) {


        System.out.println(version);
        ModelAndView modelAndView = new ModelAndView();

        if (!authentication.getName().equals(username) && authentication.getAuthorities().stream()
                .noneMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Bu kullanıcı için dosya paylaşma yetkiniz yok.");
            return modelAndView;
        }

        // Kullanıcı ve dosya bilgilerini getir
        Optional<User> userOptional = userService.findByUsername(username);
        Optional<User> sharedWithUserOptional = userService.findByEmail(sharedWithEmail);
        Optional<File> fileOptional = fileService.findByIdAndUser(fileId, userOptional.orElse(null));

        if (userOptional.isEmpty() || sharedWithUserOptional.isEmpty() || fileOptional.isEmpty()) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Kullanıcı veya dosya bulunamadı.");
            return modelAndView;
        }

        User user = userOptional.get();
        User sharedWithUser = sharedWithUserOptional.get();
        File file = fileOptional.get();

        // Dosyayı paylaş
        fileService.shareFileWithUser(file, user, sharedWithUser, version);

        if (directoryId == null) {
            modelAndView.setViewName("redirect:/directories");
        } else {
            modelAndView.setViewName("redirect:/directories/" + directoryId);
        }

        return modelAndView;
    }


    @GetMapping("/download-shared/{fileId}")
    public ResponseEntity<?> downloadSharedFile(
            @PathVariable Long fileId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> sharedWithUserOptional = userService.findByUsername(username);
            if (sharedWithUserOptional.isEmpty()) {
                return ResponseEntity.status(401).body("User not found.");
            }

            User sharedWithUser = sharedWithUserOptional.get();

            // Fetch the FileShare entry for this file and user
            Optional<FileShare> fileShareOptional = fileShareRepository.findByFileAndSharedWithUser(
                    fileService.findById(fileId).orElseThrow(() -> new IllegalArgumentException("File not found")),
                    sharedWithUser
            );

            if (fileShareOptional.isEmpty()) {
                return ResponseEntity.status(403).body("You do not have access to this file.");
            }

            FileShare fileShare = fileShareOptional.get();
            File file = fileShare.getFile();
            User owner = fileShare.getOwner();

            String fileName = file.getFileName();

            // Retrieve the version from the FileShare object
            String version = fileShare.getVersion(); // Ensure `FileShare` has a `version` field to store the shared version.

            // Dosya tipini kontrol et
            String fileNameLower = fileName.toLowerCase();
            boolean isBinary = fileNameLower.matches(".*\\.(jpg|png|mp4|docx?|xlsx|pdf|pptx)$");

            byte[] originalData;

            // Şifreleme anahtarını al
            String encryptionKey = keyVaultService.getEncryptionKeyFromKeyVault(owner.getUsername());
            if (encryptionKey == null || encryptionKey.isEmpty()) {
                return ResponseEntity.status(500).body("Encryption key not found.");
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
                    return ResponseEntity.status(404).body("Blob not found: " + versionedFilePath);
                }

                byte[] base64EncodedData = azureBlobStorage.read(new Storage(versionedFilePath, null));
                byte[] encryptedData = Base64.getDecoder().decode(base64EncodedData);
                originalData = AESUtil.decrypt(encryptedData, encryptionKey);
            } else {
                // Metin dosya işlemleri
                String reconstructedContent = fileVersionService.reconstructFileContent(file, version, owner);
                originalData = reconstructedContent.getBytes(StandardCharsets.UTF_8);
            }

            ByteArrayResource resource = new ByteArrayResource(originalData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentLength(originalData.length)
                    .body(resource);
        } catch (BlobStorageException ex) {
            return ResponseEntity.status(404).body("Blob not found: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("An unexpected error occurred: " + ex.getMessage());
        }
    }



    @PostMapping("/upload")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    public ModelAndView uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("username") String username,
            @RequestParam(value = "directoryId", required = false) Long directoryId,
            Authentication authentication,
            @RequestParam(value = "versionNumber", required = false) String manualVersionNumber) {

        ModelAndView modelAndView = new ModelAndView();

        try {
            if (file.isEmpty()) {
                modelAndView.setViewName("error");
                modelAndView.addObject("message", "Dosya eksik veya boş!");
                return modelAndView;
            }

            if (!authentication.getName().equals(username) && authentication.getAuthorities().stream()
                    .noneMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                modelAndView.setViewName("error");
                modelAndView.addObject("message", "Bu kullanıcı için dosya yükleme yetkiniz yok.");
                return modelAndView;
            }

            Optional<User> currentUserOptional = userService.findByUsername(username);
            if (currentUserOptional.isEmpty()) {
                modelAndView.setViewName("error");
                modelAndView.addObject("message", "Kullanıcı bulunamadı.");
                return modelAndView;
            }

            User currentUser = currentUserOptional.get();

            // AES Anahtarını Key Vault'tan al
            String aesKey = keyVaultService.getEncryptionKeyFromKeyVault(username);
            if (aesKey == null || aesKey.isEmpty()) {
                aesKey = AESUtil.generateAESKey();
                keyVaultService.saveEncryptionKeyToKeyVault(username, aesKey);
                logger.info("Kullanıcı için yeni AES anahtarı oluşturuldu: {}", username);
            }

            // Dizin bilgilerini al
            Directory directory = null;
            if (directoryId != null) {
                Optional<Directory> directoryOptional = directoryService.findByIdAndUser(directoryId, currentUser);
                if (directoryOptional.isPresent()) {
                    directory = directoryOptional.get();
                } else {
                    modelAndView.setViewName("error");
                    modelAndView.addObject("message", "Dizin bulunamadı.");
                    return modelAndView;
                }
            }

            // Benzersiz dosya yolunu oluştur
            String uniqueFilePath = (directory != null)
                    ? String.format("uploads/%s/%s", username, getDirectoryPath(directory, file.getOriginalFilename()))
                    : String.format("uploads/%s/%s", username, file.getOriginalFilename());

            logger.info("Benzersiz dosya yolu oluşturuldu: {}", uniqueFilePath);

            File userFile;
            if (directory != null) {
                userFile = fileService.findByFileNameUserAndDirectory(file.getOriginalFilename(), currentUser, directory);
            } else {
                userFile = fileService.findByFileNameAndUserDirectoryNull(file.getOriginalFilename(), currentUser);
            }

            // Dosyanın hash'ini hesapla
            String fileHash = HashUtil.calculateHash(file.getBytes());
            logger.info("Hesaplanan dosya hash'i: {}", fileHash);

            if (userFile != null) {
                // Aynı hash değerine sahip dosya kontrolü
                boolean isDuplicate = userFile.getVersions().stream()
                        .anyMatch(version -> version.getHash().equals(fileHash));

                if (isDuplicate) {
                    modelAndView.setViewName("upload"); // Kullanıcıyı aynı sayfada tut
                    modelAndView.addObject("errorMessage",
                            "Bu dosya bu dizinde zaten mevcut. Versiyon: " +
                                    userFile.getVersions().stream()
                                            .filter(version -> version.getHash().equals(fileHash))
                                            .findFirst()
                                            .get()
                                            .getVersionNumber());
                    return modelAndView;
                }
            } else {
                // Yeni dosya oluştur
                userFile = new File();
                userFile.setFileName(file.getOriginalFilename());
                userFile.setPath(uniqueFilePath);
                userFile.setUser(currentUser);
                userFile.setDirectory(directory);
                fileService.saveFile(userFile);
                logger.info("Yeni dosya oluşturuldu: {}", file.getOriginalFilename());
            }

            // Versiyon numarasını oluştur
            String versionNumber = (manualVersionNumber != null)
                    ? manualVersionNumber
                    : fileVersionService.generateNextVersion(userFile);
            logger.info("Versiyon numarası oluşturuldu: {}", versionNumber);

            // Dosya türünü belirle
            String fileName = file.getOriginalFilename().toLowerCase();
            boolean isBinary = fileName.matches(".*\\.(jpg|png|mp4|docx?|xlsx|pdf|pptx)$");

            if (isBinary) {
                // Binary dosya işlemleri
                byte[] encryptedData = AESUtil.encrypt(file.getBytes(), aesKey);
                String base64EncodedData = Base64.getEncoder().encodeToString(encryptedData);
                String versionedFilePath = uniqueFilePath + "/versions/" + versionNumber + "/" + file.getOriginalFilename();
                azureBlobStorage.write(new Storage(versionedFilePath, base64EncodedData.getBytes(StandardCharsets.UTF_8)));

                // Yeni versiyonu kaydet
                FileVersion version = fileVersionService.createVersion(userFile, versionNumber, null);
                version.setHash(fileHash);
                fileVersionService.saveFileVersion(version);
            } else {
                // Metin dosya işlemleri
                String newContent = new String(file.getBytes(), StandardCharsets.UTF_8);
                if (versionNumber.equals("v1")) {
                    byte[] encryptedData = AESUtil.encrypt(newContent.getBytes(StandardCharsets.UTF_8), aesKey);
                    String base64EncodedData = Base64.getEncoder().encodeToString(encryptedData);
                    String versionedFilePath = uniqueFilePath + "/versions/" + versionNumber + "/" + file.getOriginalFilename();
                    azureBlobStorage.write(new Storage(versionedFilePath, base64EncodedData.getBytes(StandardCharsets.UTF_8)));

                    // Yeni versiyonu kaydet
                    FileVersion version = fileVersionService.createVersion(userFile, versionNumber, null);
                    version.setHash(fileHash);
                    fileVersionService.saveFileVersion(version);
                } else {
                    // Delta dosyası işlemleri
                    String latestContent = fileVersionService.getLatestContent(userFile, currentUser);
                    String delta = DeltaUtil.calculateDelta(latestContent, newContent);
                    String deltaPath = uniqueFilePath + "/versions/" + versionNumber + "/delta";
                    azureBlobStorage.write(new Storage(deltaPath, delta.getBytes(StandardCharsets.UTF_8)));

                    // Yeni versiyonu kaydet
                    FileVersion version = fileVersionService.createVersion(userFile, versionNumber, deltaPath);
                    version.setHash(fileHash);
                    fileVersionService.saveFileVersion(version);
                }
            }

            modelAndView.setViewName("success");
            modelAndView.addObject("message", "Dosya başarıyla yüklendi. Versiyon: " + versionNumber);
            return modelAndView;

        } catch (Exception e) {
            logger.error("Dosya yükleme sırasında hata oluştu: {}", e.getMessage(), e);
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Dosya yükleme başarısız: " + e.getMessage());
            return modelAndView;
        }
    }


    private String getDirectoryPath(Directory directory, String fileName) {
        StringBuilder path = new StringBuilder();
        Directory currentDirectory = directory;

        while (currentDirectory != null) {
            path.insert(0, currentDirectory.getId() + "/");  // Alt dizin bilgilerini başa ekleyin
            currentDirectory = currentDirectory.getParentDirectory();  // Parent dizini al
        }

        path.append(fileName);  // Son olarak dosya adını ekleyin
        return path.toString();
    }





    @GetMapping("/download/{username}/{fileId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    public ResponseEntity<Resource> downloadSpecificVersion(
            @PathVariable("username") String username,
            @PathVariable("fileId") Long fileId,
            @RequestParam("versionNumber") String versionNumber,
            Authentication authentication) {

        logger.debug("Download request - Username: {}, FileId: {}, Version: {}", username, fileId, versionNumber);

        // Yetkilendirme kontrolü
        if (!authentication.getName().equals(username) && authentication.getAuthorities().stream()
                .noneMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            logger.error("Erişim reddedildi. Kullanıcı adı eşleşmiyor veya yeterli yetki yok: {}", username);
            return ResponseEntity.status(403).build();
        }

        Optional<User> currentUserOptional = userService.findByUsername(username);
        if (currentUserOptional.isEmpty()) {
            logger.error("Kullanıcı bulunamadı: {}", username);
            return ResponseEntity.status(401).build();
        }

        User user = currentUserOptional.get();
        Optional<File> fileOptional = fileService.findByIdAndUser(fileId, user);
        if (fileOptional.isEmpty()) {
            logger.error("Dosya bulunamadı: {}", fileId);
            return ResponseEntity.status(404).build();
        }

        File file = fileOptional.get();
        String fileName = file.getFileName();

        try {
            String fileNameLower = fileName.toLowerCase();
            boolean isBinary = fileNameLower.matches(".*\\.(jpg|png|mp4|docx?|xlsx|pdf|pptx)$");

            byte[] originalData;

            if (isBinary) {
                // Versiyonlu dosya yolu
                String versionedFilePath = String.format("%s/versions/%s/%s", file.getPath(), versionNumber, fileName);

                // Azure Blob Storage'dan veriyi oku
                byte[] base64EncodedData = azureBlobStorage.read(new Storage(versionedFilePath, null));

                // Veriyi kontrol et
                if (base64EncodedData == null) {
                    logger.error("Blob boş: {}", versionedFilePath);
                    return ResponseEntity.status(404).build();
                }

                // Base64 veriyi çöz
                byte[] encryptedData = Base64.getDecoder().decode(base64EncodedData);

                // Şifreleme anahtarını Key Vault'tan al
                String encryptionKey = keyVaultService.getEncryptionKeyFromKeyVault(user.getUsername());
                if (encryptionKey == null || encryptionKey.isEmpty()) {
                    logger.error("Şifreleme anahtarı bulunamadı: {}", user.getUsername());
                    return ResponseEntity.status(500).build();
                }

                // Veriyi çöz
                originalData = AESUtil.decrypt(encryptedData, encryptionKey);
            } else {
                // Metin dosyasını yeniden oluştur
                String reconstructedContent = fileVersionService.reconstructFileContent(file, versionNumber, user);
                originalData = reconstructedContent.getBytes(StandardCharsets.UTF_8);
            }

            ByteArrayResource resource = new ByteArrayResource(originalData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentLength(originalData.length)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Dosya indirme sırasında hata oluştu: ", e);
            return ResponseEntity.status(500).build();
        }
    }


    @DeleteMapping("/delete/{username}/{fileId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    public ModelAndView deleteSpecificVersion(
            @PathVariable("username") String username,
            @PathVariable("fileId") Long fileId,
            @RequestParam("versionNumber") String versionNumber,
            Authentication authentication) {

        ModelAndView modelAndView = new ModelAndView();

        // Yetkilendirme kontrolü
        if (!authentication.getName().equals(username) && authentication.getAuthorities().stream()
                .noneMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Bu kullanıcı için dosya silme yetkiniz yok.");
            return modelAndView;
        }

        Optional<User> currentUser = userService.findByUsername(username);
        if (currentUser.isEmpty()) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Kullanıcı bulunamadı.");
            return modelAndView;
        }

        User user = currentUser.get();
        Optional<File> fileOptional = fileService.findByIdAndUser(fileId, user);
        if (fileOptional.isEmpty()) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Dosya bulunamadı.");
            return modelAndView;
        }

        File file = fileOptional.get();

        try {
            // Silinecek versiyonu bul
            FileVersion versionToDelete = fileVersionService.getVersionByFileAndNumber(file, versionNumber);
            if (versionToDelete == null) {
                modelAndView.setViewName("error");
                modelAndView.addObject("message", "Versiyon bulunamadı.");
                return modelAndView;
            }

            // Blob Storage'tan versiyonu sil
            String versionFilePath = String.format("%s/versions/%s/%s", file.getPath(), versionNumber, file.getFileName());
            if (azureBlobStorage.exists(new Storage(versionFilePath, null))) {
                azureBlobStorage.delete(new Storage(versionFilePath, null));
            }

            // Delta dosyasını sil (varsa)
            if (versionToDelete.getDeltaPath() != null) {
                String deltaFilePath = versionToDelete.getDeltaPath();
                if (azureBlobStorage.exists(new Storage(deltaFilePath, null))) {
                    azureBlobStorage.delete(new Storage(deltaFilePath, null));
                }
            }

            // Veritabanından versiyonu sil
            fileVersionService.deleteFileVersion(versionToDelete);

            // Eğer başka versiyon kalmadıysa dosyayı sil
            if (fileVersionService.getAllVersions(file).isEmpty()) {
                fileService.deleteFile(file);
            }

            modelAndView.setViewName("redirect:/directories");
            return modelAndView;

        } catch (Exception e) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Dosya silme işlemi başarısız oldu: " + e.getMessage());
            return modelAndView;
        }
    }


}
