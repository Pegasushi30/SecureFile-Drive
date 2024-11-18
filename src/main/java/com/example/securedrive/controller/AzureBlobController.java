package com.example.securedrive.controller;

import com.example.securedrive.exception.AzureBlobStorageException;
import com.example.securedrive.model.File;
import com.example.securedrive.model.Storage;
import com.example.securedrive.model.User;
import com.example.securedrive.security.AESUtil;
import com.example.securedrive.service.FileService;
import com.example.securedrive.service.UserService;
import com.example.securedrive.service.impl.AzureBlobStorageImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/files")
public class AzureBlobController {

    @Autowired
    private AzureBlobStorageImpl azureBlobStorage;

    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("username") String username,
            Authentication authentication) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is missing or empty!");
        }

        if (!authentication.getName().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to upload files for this user.");
        }

        Optional<User> currentUser = userService.findByUsername(username);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }

        try {
            // Dosyayı doğrudan yükle
            String uniqueFilePath = "uploads/" + username + "/" + file.getOriginalFilename();

            // Veritabanında dosya meta verilerini kaydet
            File userFile = new File();
            userFile.setFileName(file.getOriginalFilename());
            userFile.setPath(uniqueFilePath);
            userFile.setUser(currentUser.get());
            fileService.saveFile(userFile);

            // Azure Blob Storage'a yükle
            Storage storage = new Storage();
            storage.setPath("uploads/" + username);
            storage.setFileName(file.getOriginalFilename());
            storage.setInputStream(new ByteArrayInputStream(file.getBytes()));  // Şifrelenmemiş veri
            azureBlobStorage.write(storage);

            return ResponseEntity.ok("File successfully uploaded: " + file.getOriginalFilename());
        } catch (AzureBlobStorageException e) {
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("Upload failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed: " + e.getMessage());
        }
    }


    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #authentication.name == principal.username")
    public ResponseEntity<List<String>> getListFiles(Authentication authentication) {
        Optional<User> currentUser = userService.findByUsername(authentication.getName());

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        List<String> fileUrls = fileService.getFilesByUser(currentUser.get()).stream()
                .map(file -> MvcUriComponentsBuilder
                        .fromMethodName(AzureBlobController.class, "downloadFile", file.getFileName(), authentication)
                        .build().toString())
                .collect(Collectors.toList());

        return ResponseEntity.ok(fileUrls);
    }

    @GetMapping("/download/{fileName}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #authentication.name == principal.username")
    public ResponseEntity<Resource> downloadFile(@PathVariable("fileName") String fileName, Authentication authentication) {
        Optional<User> currentUser = userService.findByUsername(authentication.getName());

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        File file = fileService.findByFileNameAndUser(fileName, currentUser.get());

        if (file == null && authentication.getAuthorities().stream().noneMatch(role -> role.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        try {
            // Azure Blob Storage'dan dosyayı ham haliyle oku
            Storage storage = new Storage();
            storage.setPath("uploads/" + currentUser.get().getUsername());
            storage.setFileName(fileName);
            byte[] fileData = azureBlobStorage.read(storage);

            if (fileData == null || fileData.length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            ByteArrayResource resource = new ByteArrayResource(fileData);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentLength(fileData.length)
                    .body(resource);

        } catch (AzureBlobStorageException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }




    @DeleteMapping("/delete/{fileName}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #authentication.name == principal.username")
    public ResponseEntity<String> deleteFile(@PathVariable("fileName") String fileName, Authentication authentication) {
        Optional<User> currentUser = userService.findByUsername(authentication.getName());

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Kullanıcı doğrulanamadı.");
        }

        File file = fileService.findByFileNameAndUser(fileName, currentUser.get());

        if (file == null && authentication.getAuthorities().stream().noneMatch(role -> role.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Dosya bulunamadı.");
        }

        try {
            Storage storage = new Storage();
            storage.setPath("uploads/" + currentUser.get().getUsername());
            storage.setFileName(fileName);

            azureBlobStorage.delete(storage);
            assert file != null;
            fileService.deleteFile(file.getId());
            return ResponseEntity.ok("Dosya başarıyla silindi: " + fileName);
        } catch (AzureBlobStorageException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Silme işlemi başarısız: " + e.getMessage());
        }
    }
}
