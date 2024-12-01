package com.example.securedrive.controller;

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
import org.springframework.web.servlet.ModelAndView;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

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

    private static final Logger logger = LoggerFactory.getLogger(AzureBlobController.class);

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

            String aesKey = currentUser.getEncryptionKey();
            if (aesKey == null || aesKey.isEmpty()) {
                aesKey = AESUtil.generateAESKey();
                currentUser.setEncryptionKey(aesKey);
                userService.saveUser(currentUser);
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
                byte[] encryptedData = AESUtil.encrypt(file.getBytes(), aesKey);
                String base64EncodedData = Base64.getEncoder().encodeToString(encryptedData);
                String versionedFilePath = uniqueFilePath + "/versions/" + versionNumber + "/" + file.getOriginalFilename();
                azureBlobStorage.write(new Storage(versionedFilePath, base64EncodedData.getBytes(StandardCharsets.UTF_8)));
                FileVersion version = fileVersionService.createVersion(userFile, versionNumber, null);
                fileVersionService.saveFileVersion(version);
            } else {
                String newContent = new String(file.getBytes(), StandardCharsets.UTF_8);
                if (versionNumber.equals("v1")) {
                    byte[] encryptedData = AESUtil.encrypt(newContent.getBytes(StandardCharsets.UTF_8), aesKey);
                    String base64EncodedData = Base64.getEncoder().encodeToString(encryptedData);
                    String versionedFilePath = uniqueFilePath + "/versions/" + versionNumber + "/" + file.getOriginalFilename();
                    azureBlobStorage.write(new Storage(versionedFilePath, base64EncodedData.getBytes(StandardCharsets.UTF_8)));
                    FileVersion version = fileVersionService.createVersion(userFile, versionNumber, null);
                    fileVersionService.saveFileVersion(version);
                } else {
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

    @GetMapping("/download/{username}/{fileName}/{versionNumber}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    public ResponseEntity<Resource> downloadSpecificVersion(
            @PathVariable("username") String username,
            @PathVariable("fileName") String fileName,
            @PathVariable("versionNumber") String versionNumber,
            Authentication authentication) {

        if (!authentication.getName().equals(username) && authentication.getAuthorities().stream()
                .noneMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(403).build();
        }

        Optional<User> currentUser = userService.findByUsername(username);
        if (currentUser.isEmpty()) {
            logger.error("Kullanıcı bulunamadı: {}", username);
            return ResponseEntity.status(401).build();
        }

        User user = currentUser.get();
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
                // Binary dosyalar: Şifreli ve Base64 kodlanmış veriyi oku
                String versionedFilePath = file.getPath() + "/versions/" + versionNumber + "/" + fileName;
                byte[] base64EncodedData = azureBlobStorage.read(new Storage(versionedFilePath, null));

                // Base64'ten çöz
                byte[] encryptedData = Base64.getDecoder().decode(base64EncodedData);

                // Veriyi deşifre et
                originalData = AESUtil.decrypt(encryptedData, user.getEncryptionKey());
            } else {
                // Metin dosyalar: Dosyayı delta hesaplaması ile yeniden oluştur
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

    @DeleteMapping("/delete/{username}/{fileName}/{versionNumber}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    public ModelAndView deleteSpecificVersion(
            @PathVariable("username") String username,
            @PathVariable("fileName") String fileName,
            @PathVariable("versionNumber") String versionNumber,
            Authentication authentication) {

        ModelAndView modelAndView = new ModelAndView();

        if (!authentication.getName().equals(username) && authentication.getAuthorities().stream()
                .noneMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Bu kullanıcı için dosya silme yetkiniz yok.");
            return modelAndView;
        }

        Optional<User> currentUser = userService.findByUsername(username);
        if (currentUser.isEmpty()) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Yetkisiz erişim");
            return modelAndView;
        }

        User user = currentUser.get();
        File file = fileService.findByFileNameAndUser(fileName, user);
        if (file == null) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Dosya bulunamadı");
            return modelAndView;
        }

        try {
            FileVersion version = fileVersionService.getVersionByFileAndNumber(file, versionNumber);
            if (version == null) {
                modelAndView.setViewName("error");
                modelAndView.addObject("message", "Versiyon bulunamadı");
                return modelAndView;
            }

            // Blob'dan sil
            if (versionNumber.equals("v1")) {
                String encryptedFilePath = file.getPath() + "/versions/" + versionNumber + "/" + fileName;
                azureBlobStorage.delete(new Storage(encryptedFilePath, null));
            } else {
                if (version.getDeltaPath() != null) {
                    azureBlobStorage.delete(new Storage(version.getDeltaPath(), null));
                }
            }

            // Versiyonu ve dosyayı MySQL'den sil
            fileVersionService.deleteFileVersion(version);

            // Eğer başka versiyon kalmadıysa dosyayı sil
            if (fileVersionService.getAllVersions(file).isEmpty()) {
                fileService.deleteFile(file);
            }

            // Dosya başarıyla silindi
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
