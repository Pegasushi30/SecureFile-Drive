// com/example/securedrive/controller/AzureBlobController.java
package com.example.securedrive.controller;

import com.azure.storage.blob.models.BlobStorageException;
import com.example.securedrive.dto.*;
import com.example.securedrive.model.File;
import com.example.securedrive.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("files")
public class AzureBlobController {

    private final FileFacadeService fileFacadeService;
    private final FileManagementService fileManagementService;

    private static final Logger logger = LoggerFactory.getLogger(AzureBlobController.class);

    @Autowired
    public AzureBlobController(FileFacadeService fileFacadeService, FileManagementService fileManagementService) {
        this.fileFacadeService = fileFacadeService;
        this.fileManagementService = fileManagementService;
    }

    @PostMapping("/revoke-share")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> revokeShare(
            @RequestParam("fileId") Long fileId,
            @RequestParam("sharedWithEmail") String sharedWithEmail,
            @RequestParam("username") String username,
            @RequestParam("version") String version,
            Authentication authentication) {
        try {
            FileRevokeShareRequestDto dto = new FileRevokeShareRequestDto(fileId, sharedWithEmail, username, version);
            fileFacadeService.revokeShare(dto, authentication);
            int remainingShares = fileFacadeService.getRemainingShares(fileId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Share revoked successfully!");
            response.put("remainingShares", remainingShares);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/share")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    public ResponseEntity<Map<String, Object>> shareFile(
            @RequestParam("username") String username,
            @RequestParam("fileId") Long fileId,
            @RequestParam("sharedWithEmail") String sharedWithEmail,
            @RequestParam("version") String version,
            Authentication authentication) {
        try {
            FileShareRequestDto dto = new FileShareRequestDto(username, fileId, sharedWithEmail, version);
            fileFacadeService.shareFile(dto, authentication);
            String redirectUrl = "/directories";
            return ResponseEntity.ok(Map.of("message", "File shared successfully.", "redirectUrl", redirectUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/download-shared/{fileId}")
    public ResponseEntity<?> downloadSharedFile(
            @PathVariable Long fileId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            FileDownloadSharedRequestDto dto = new FileDownloadSharedRequestDto(fileId, username);
            FileDownloadSharedResponseDto response = fileFacadeService.downloadSharedFile(dto);
            String fileName = response.getFileName();
            byte[] originalData = response.getOriginalData();
            ByteArrayResource resource = response.getResource();

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

        ModelAndView modelAndView;

        try {
            if (!authentication.getName().equals(username) && authentication.getAuthorities().stream()
                    .noneMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                modelAndView = new ModelAndView("upload");
                modelAndView.addObject("errorMessage", "You do not have permission to upload files for this user.");
                return modelAndView;
            }

            FileUploadRequestDto dto = new FileUploadRequestDto(username, directoryId, file, manualVersionNumber);
            String message = fileFacadeService.uploadFile(dto);

            if (message.startsWith("This file already exists in this directory")) {
                modelAndView = new ModelAndView("upload");
                modelAndView.addObject("errorMessage", message);
            } else if (message.startsWith("File uploaded successfully")) {
                modelAndView = new ModelAndView("success");
                modelAndView.addObject("message", message);
            } else {
                modelAndView = new ModelAndView("upload");
                modelAndView.addObject("errorMessage", "An unknown error occurred.");
            }

        } catch (Exception e) {
            modelAndView = new ModelAndView("upload");
            modelAndView.addObject("errorMessage", "File upload failed: " + e.getMessage());
        }

        return modelAndView;
    }

    @GetMapping("/download/{username}/{fileId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    public ResponseEntity<?> downloadSpecificVersion(
            @PathVariable("username") String username,
            @PathVariable("fileId") Long fileId,
            @RequestParam("versionNumber") String versionNumber,
            Authentication authentication) {
        try {
            if (!authentication.getName().equals(username) && authentication.getAuthorities().stream()
                    .noneMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                return ResponseEntity.status(403).build();
            }
            FileDownloadSpecificVersionRequestDto dto = new FileDownloadSpecificVersionRequestDto(username, fileId, versionNumber);
            ByteArrayResource resource = fileFacadeService.downloadSpecificVersion(dto);
            File file = fileManagementService.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
            String originalFileName = file.getFileName();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFileName + "\"")
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/delete/{username}/{fileId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #username == authentication.name")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteSpecificVersionAjax(
            @PathVariable("username") String username,
            @PathVariable("fileId") Long fileId,
            @RequestParam("versionNumber") String versionNumber,
            Authentication authentication) {
        try {
            FileDeleteSpecificVersionRequestDto dto = new FileDeleteSpecificVersionRequestDto(username, fileId, versionNumber);
            String result = fileFacadeService.deleteSpecificVersion(dto);
            if (result.startsWith("File and version deleted successfully")) {
                return ResponseEntity.ok(Map.of("message", "File deleted successfully."));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", result));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
