package com.example.securedrive.controller;

import com.azure.storage.blob.models.BlobStorageException;
import com.example.securedrive.model.*;
import com.example.securedrive.repository.FileShareRepository;
import com.example.securedrive.security.AESUtil;
import com.example.securedrive.security.DeltaUtil;
import com.example.securedrive.security.KeyVaultService;
import com.example.securedrive.service.FileService;
import com.example.securedrive.service.FileVersionService;
import com.example.securedrive.service.UserService;
import com.example.securedrive.service.impl.AzureBlobStorageImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private static final Logger logger = LoggerFactory.getLogger(AzureBlobController.class);

    @PostMapping("/revoke-share/{fileId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    public ModelAndView revokeShare(
            @PathVariable Long fileId,
            @RequestParam("sharedWithUsername") String sharedWithUsername,
            @RequestParam("username") String username,
            Authentication authentication) {

        ModelAndView modelAndView = new ModelAndView();

        if (!authentication.getName().equals(username) && authentication.getAuthorities().stream()
                .noneMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Bu kullanıcı için paylaşımı iptal etme yetkiniz yok.");
            return modelAndView;
        }

        Optional<User> ownerOptional = userService.findByUsername(username);
        Optional<User> sharedWithUserOptional = userService.findByUsername(sharedWithUsername);
        Optional<FileShare> fileShareOptional = fileShareRepository.findByFileIdAndSharedWithUser(fileId, sharedWithUserOptional.orElse(null));

        if (ownerOptional.isEmpty() || sharedWithUserOptional.isEmpty() || fileShareOptional.isEmpty()) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Paylaşımı iptal etmek için gerekli bilgiler bulunamadı.");
            return modelAndView;
        }

        fileShareRepository.delete(fileShareOptional.get());
        modelAndView.setViewName("redirect:/files?success");
        return modelAndView;
    }


    @PostMapping("/share")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    public ModelAndView shareFile(
            @RequestParam("username") String username,
            @RequestParam("fileId") Long fileId,
            @RequestParam("sharedWithEmail") String sharedWithEmail,
            @RequestParam("version") String version,  // Versiyon bilgisi alınıyor
            Authentication authentication) {

        ModelAndView modelAndView = new ModelAndView();

        if (!authentication.getName().equals(username) && authentication.getAuthorities().stream()
                .noneMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Bu kullanıcı için dosya paylaşma yetkiniz yok.");
            return modelAndView;
        }

        // Kullanıcıları ve dosyayı al
        Optional<User> userOptional = userService.findByUsername(username);
        Optional<User> sharedWithUserOptional = userService.findByEmail(sharedWithEmail);
        Optional<File> fileOptional = fileService.findById(fileId);

        if (userOptional.isEmpty() || sharedWithUserOptional.isEmpty() || fileOptional.isEmpty()) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Kullanıcı veya dosya bulunamadı.");
            return modelAndView;
        }

        User user = userOptional.get();
        User sharedWithUser = sharedWithUserOptional.get();
        File file = fileOptional.get();

        // Dosya paylaş
        fileService.shareFileWithUser(file, user, sharedWithUser, version);

        modelAndView.setViewName("redirect:/files?shareSuccess");
        return modelAndView;
    }


    // View all files shared with a specific user
    @GetMapping("/viewSharedFiles")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    public ResponseEntity<List<File>> viewSharedFiles(@RequestParam("username") String username, Authentication authentication) {

        if (!authentication.getName().equals(username) && authentication.getAuthorities().stream()
                .noneMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(403).body(null);
        }

        // Retrieve user and their shared files
        Optional<User> userOptional = userService.findByUsername(username);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(404).body(null);
        }

        User user = userOptional.get();

        // Retrieve file shares for the user
        List<FileShare> fileShares = fileShareRepository.findAllBySharedWithUser(user);
        List<File> files = new ArrayList<>();
        for (FileShare fileShare : fileShares) {
            files.add(fileShare.getFile());
        }

        return ResponseEntity.ok(files);
    }

    @GetMapping("/download-shared/{fileId}")
    public ResponseEntity<?> downloadSharedFile(
            @PathVariable Long fileId,
            @RequestParam(value = "version", required = false, defaultValue = "v1") String version,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> sharedWithUserOptional = userService.findByUsername(username);
            if (sharedWithUserOptional.isEmpty()) {
                return ResponseEntity.status(401).body("Kullanıcı bulunamadı.");
            }

            User sharedWithUser = sharedWithUserOptional.get();

            // Dosya paylaşım kontrolü
            Optional<FileShare> fileShareOptional = fileShareRepository.findByFileIdAndSharedWithUser(fileId, sharedWithUser);
            if (fileShareOptional.isEmpty()) {
                return ResponseEntity.status(403).body("Bu dosyaya erişim yetkiniz yok.");
            }

            FileShare fileShare = fileShareOptional.get();
            File file = fileShare.getFile();
            User owner = fileShare.getOwner();

            String fileName = file.getFileName();

            // Dosyanın türünü kontrol et
            String fileNameLower = fileName.toLowerCase();
            boolean isBinary = fileNameLower.matches(".*\\.(jpg|png|mp4|docx?|xlsx|pdf|pptx)$");

            byte[] originalData;

            // Key Vault'tan şifreleme anahtarını alıyoruz
            String encryptionKey = keyVaultService.getEncryptionKeyFromKeyVault(owner.getUsername()); // Key Vault'tan alınan anahtar

            if (encryptionKey == null || encryptionKey.isEmpty()) {
                return ResponseEntity.status(500).body("Şifreleme anahtarı bulunamadı.");
            }

            if (isBinary) {
                // Binary dosya işlemleri
                String versionedFilePath = String.format(
                        "uploads/%s/%s/versions/%s/%s",
                        owner.getUsername(),
                        fileName,
                        version,
                        fileName
                );

                if (!azureBlobStorage.checkBlobExists(versionedFilePath)) {
                    return ResponseEntity.status(404).body("Belirtilen blob bulunamadı: " + versionedFilePath);
                }

                byte[] base64EncodedData = azureBlobStorage.read(new Storage(versionedFilePath, null));

                byte[] encryptedData = Base64.getDecoder().decode(base64EncodedData);
                originalData = AESUtil.decrypt(encryptedData, encryptionKey); // Key Vault anahtarını kullanarak şifreyi çözüyoruz
            } else {
                // Metin dosyası işlemleri
                String reconstructedContent = fileVersionService.reconstructFileContent(file, version, owner);

                // Metni tekrar şifrelemiyoruz
                originalData = reconstructedContent.getBytes(StandardCharsets.UTF_8);
            }

            ByteArrayResource resource = new ByteArrayResource(originalData);

            // İçerik tipini dosya uzantısına göre ayarlayabiliriz
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM; // İsterseniz dosya tipine göre ayarlayabilirsiniz

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentLength(originalData.length)
                    .contentType(mediaType)
                    .body(resource);
        } catch (BlobStorageException ex) {
            return ResponseEntity.status(404).body("Belirtilen blob bulunamadı: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Beklenmedik bir hata oluştu: " + ex.getMessage());
        }
    }









    @PostMapping("/upload")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    public ModelAndView uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("username") String username,
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
                // Anahtar yoksa, yeni anahtar oluştur ve Key Vault'a kaydet
                aesKey = AESUtil.generateAESKey();
                keyVaultService.saveEncryptionKeyToKeyVault(username, aesKey);
                logger.info("Kullanıcı için yeni AES anahtarı oluşturuldu: {}", username);
            }

            String uniqueFilePath = "uploads/" + username + "/" + file.getOriginalFilename();
            logger.info("Benzersiz dosya yolu oluşturuldu: {}", uniqueFilePath);

            File userFile = fileService.findByFileNameAndUser(file.getOriginalFilename(), currentUser);
            if (userFile == null) {
                userFile = new File();
                userFile.setFileName(file.getOriginalFilename());
                userFile.setPath(uniqueFilePath);
                userFile.setUser(currentUser);
                fileService.saveFile(userFile);
                logger.info("Dosya meta verileri oluşturuldu ve kaydedildi: {}", file.getOriginalFilename());
            }

            String versionNumber = (manualVersionNumber != null)
                    ? manualVersionNumber
                    : fileVersionService.generateNextVersion(userFile);
            logger.info("Versiyon numarası oluşturuldu: {}", versionNumber);

            // Dosya türünü belirle
            String fileName = Objects.requireNonNull(file.getOriginalFilename()).toLowerCase();
            boolean isBinary = fileName.endsWith(".jpg") || fileName.endsWith(".png") || fileName.endsWith(".mp4")
                    || fileName.endsWith(".doc") || fileName.endsWith(".docx")
                    || fileName.endsWith(".pdf") || fileName.endsWith(".xlsx") || fileName.endsWith(".pptx");

            if (isBinary) {
                // Binary dosyayı şifrele
                byte[] encryptedData = AESUtil.encrypt(file.getBytes(), aesKey);
                String base64EncodedData = Base64.getEncoder().encodeToString(encryptedData);
                String versionedFilePath = uniqueFilePath + "/versions/" + versionNumber + "/" + file.getOriginalFilename();
                azureBlobStorage.write(new Storage(versionedFilePath, base64EncodedData.getBytes(StandardCharsets.UTF_8)));
                FileVersion version = fileVersionService.createVersion(userFile, versionNumber, null);
                fileVersionService.saveFileVersion(version);
            } else {
                // Metin dosyasını işleme
                String newContent = new String(file.getBytes(), StandardCharsets.UTF_8);
                if (versionNumber.equals("v1")) {
                    byte[] encryptedData = AESUtil.encrypt(newContent.getBytes(StandardCharsets.UTF_8), aesKey);
                    String base64EncodedData = Base64.getEncoder().encodeToString(encryptedData);
                    String versionedFilePath = uniqueFilePath + "/versions/" + versionNumber + "/" + file.getOriginalFilename();
                    azureBlobStorage.write(new Storage(versionedFilePath, base64EncodedData.getBytes(StandardCharsets.UTF_8)));
                    FileVersion version = fileVersionService.createVersion(userFile, versionNumber, null);
                    fileVersionService.saveFileVersion(version);
                } else {
                    // Farklı versiyonlar için delta kaydetme
                    String latestContent = fileVersionService.getLatestContent(userFile, currentUser);
                    String delta = DeltaUtil.calculateDelta(latestContent, newContent);
                    String deltaPath = uniqueFilePath + "/versions/" + versionNumber + "/delta";
                    azureBlobStorage.write(new Storage(deltaPath, delta.getBytes(StandardCharsets.UTF_8)));
                    FileVersion version = fileVersionService.createVersion(userFile, versionNumber, deltaPath);
                    fileVersionService.saveFileVersion(version);
                }
            }

            modelAndView.setViewName("success");
            modelAndView.addObject("message", "Dosya başarıyla yüklendi. Versiyon: " + versionNumber);
            return modelAndView;
        } catch (Exception e) {
            logger.error("Dosya yükleme başarısız oldu: {}", e.getMessage(), e);
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Dosya yükleme başarısız: " + e.getMessage());
            return modelAndView;
        }
    }
    @GetMapping("/download/{username}/{fileName}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    public ResponseEntity<Resource> downloadSpecificVersion(
            @PathVariable("username") String username,
            @PathVariable("fileName") String fileName,
            @RequestParam("versionNumber") String versionNumber,
            Authentication authentication) {

        // Log the parameters for debugging
        logger.debug("Download request - Username: {}, FileName: {}, Version: {}", username, fileName, versionNumber);

        // Kullanıcı adı kontrolü ve yetki kontrolü
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
        File file = fileService.findByFileNameAndUser(fileName, user);
        if (file == null) {
            logger.error("Dosya bulunamadı: {}", fileName);
            return ResponseEntity.status(404).build();
        }

        try {
            String fileNameLower = fileName.toLowerCase();
            boolean isBinary = fileNameLower.endsWith(".jpg") || fileNameLower.endsWith(".png") || fileNameLower.endsWith(".mp4")
                    || fileNameLower.endsWith(".doc") || fileNameLower.endsWith(".docx")
                    || fileNameLower.endsWith(".pdf") || fileNameLower.endsWith(".xlsx") || fileNameLower.endsWith(".pptx");

            byte[] originalData;

            if (isBinary) {
                // Versiyonlu dosya yolu
                String versionedFilePath = file.getPath() + "/versions/" + versionNumber + "/" + fileName;

                // Azure Blob Storage'dan veriyi oku
                byte[] base64EncodedData = azureBlobStorage.read(new Storage(versionedFilePath, null));

                // Eğer veri boşsa hata döndür
                if (base64EncodedData == null) {
                    logger.error("Belirtilen blob boş: {}", versionedFilePath);
                    return ResponseEntity.status(404).build();
                }

                // Base64 encoded veriyi çöz
                byte[] encryptedData = Base64.getDecoder().decode(base64EncodedData);

                // AES anahtarını Key Vault'tan al
                String encryptionKey = keyVaultService.getEncryptionKeyFromKeyVault(user.getUsername());

                if (encryptionKey == null || encryptionKey.isEmpty()) {
                    logger.error("Kullanıcı için AES anahtarı bulunamadı: {}", user.getUsername());
                    return ResponseEntity.status(500).build();
                }

                // Şifre çözme işlemi
                originalData = AESUtil.decrypt(encryptedData, encryptionKey);
            } else {
                // Eğer dosya binary değilse (metin dosyası gibi), içerik yeniden oluşturuluyor
                String reconstructedContent = fileVersionService.reconstructFileContent(file, versionNumber, user);
                originalData = reconstructedContent.getBytes(StandardCharsets.UTF_8);
            }

            // Dosya içeriğini ByteArrayResource olarak döndür
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





    @DeleteMapping("/delete/{username}/{fileName}/{versionNumber}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    public ModelAndView deleteSpecificVersion(
            @PathVariable("username") String username,
            @PathVariable("fileName") String fileName,
            @PathVariable("versionNumber") String versionNumber,
            Authentication authentication) {

        ModelAndView modelAndView = new ModelAndView();

        // Kullanıcı doğrulaması
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
        File file = fileService.findByFileNameAndUser(fileName, user);
        if (file == null) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Dosya bulunamadı.");
            return modelAndView;
        }

        try {
            // Versiyon bulma ve silme işlemi
            FileVersion versionToDelete = fileVersionService.getVersionByFileAndNumber(file, versionNumber);
            if (versionToDelete == null) {
                modelAndView.setViewName("error");
                modelAndView.addObject("message", "Versiyon bulunamadı.");
                return modelAndView;
            }

            // Versiyonun Blob Storage'dan doğru silindiğini kontrol et
            String versionFilePath = "uploads/" + user.getUsername() + "/" + file.getFileName()
                    + "/versions/" + versionNumber + "/" + file.getFileName();

            if (azureBlobStorage.exists(new Storage(versionFilePath, null))) {
                azureBlobStorage.delete(new Storage(versionFilePath, null));
            } else {
                logger.error("Versiyon bulunamadı Blob Storage'ta: " + versionFilePath);
                modelAndView.setViewName("error");
                modelAndView.addObject("message", "Versiyon Blob Storage'ta bulunamadı.");
                return modelAndView;
            }

            // Delta dosyasını sil
            if (versionToDelete.getDeltaPath() != null) {
                String deltaFilePath = versionToDelete.getDeltaPath();
                if (azureBlobStorage.exists(new Storage(deltaFilePath, null))) {
                    azureBlobStorage.delete(new Storage(deltaFilePath, null));
                }
            }

            // Versiyonu veritabanından sil
            fileVersionService.deleteFileVersion(versionToDelete);

            // Eğer başka versiyon kalmadıysa dosyayı veritabanından sil
            if (fileVersionService.getAllVersions(file).isEmpty()) {
                fileService.deleteFile(file);
            }

            modelAndView.setViewName("redirect:/files?success");
            return modelAndView;

        } catch (Exception e) {
            logger.error("Versiyon silme sırasında hata oluştu: ", e);
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Versiyon silinemedi: " + e.getMessage());
            return modelAndView;
        }
    }




}
