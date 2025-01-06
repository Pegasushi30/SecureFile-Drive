// com/example/securedrive/controller/DirectoryController.java
package com.example.securedrive.controller;

import com.example.securedrive.dto.DirectoryDto;
import com.example.securedrive.dto.DirectoryShareDto;
import com.example.securedrive.dto.DirectoryShareRequestDto;
import com.example.securedrive.dto.FileDto;
import com.example.securedrive.model.Directory;
import com.example.securedrive.model.User;
import com.example.securedrive.repository.DirectoryRepository;
import com.example.securedrive.service.DirectoryService;
import com.example.securedrive.service.FileManagementService;
import com.example.securedrive.service.UserManagementService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/directories")
public class DirectoryController {

    private final UserManagementService userManagementService;
    private final DirectoryService directoryService;
    private final FileManagementService fileManagementService;
    private  final DirectoryRepository directoryRepository;
     private static final Logger logger = LoggerFactory.getLogger(DirectoryController.class);
    public DirectoryController(UserManagementService userManagementService,
                               DirectoryService directoryService,
                               FileManagementService fileManagementService,
                               DirectoryRepository directoryRepository) {
        this.userManagementService = userManagementService;
        this.directoryService = directoryService;
        this.fileManagementService = fileManagementService;
        this.directoryRepository = directoryRepository;
    }

    @GetMapping
    public ModelAndView listDirectories(Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView("directories");
        if (authentication != null) {
            String username = authentication.getName();
            User user = userManagementService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            logger.debug("Authenticated User ID: {}, Email: {}", user.getId(), user.getEmail());

            // Kök dizinler
            List<DirectoryDto> directories = directoryService.getRootDirectoriesAsDto(user);

            // root dosyalar (FileDto listesi)
            List<FileDto> files = fileManagementService.getUserFilesInRootDirectoryAsDto(user);

            // Kullanıcının paylaştığı dizinler
            List<DirectoryShareDto> mySharedDirectories = directoryService.getMySharedDirectories(user);
            logger.debug("Paylaşılan dizinler (sizin paylaştığınız): {}", mySharedDirectories);

            // Model'e ekle
            modelAndView.addObject("directories", directories);
            modelAndView.addObject("files", files);
            modelAndView.addObject("username", username);
            modelAndView.addObject("mySharedDirectories", mySharedDirectories);
        }
        return modelAndView;
    }






    @GetMapping("/{directoryId}")
    public ModelAndView directoryContents(@PathVariable Long directoryId, Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView("directory_contents");
        String username = authentication.getName();
        User user = userManagementService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Dizin bilgileri
        DirectoryDto currentDirectory = directoryService.getDirectoryByIdAsDto(directoryId, user);
        List<DirectoryDto> subdirectories = directoryService.getSubDirectoriesAsDto(user, directoryId);

        // Seçili dizindeki dosyalar
        List<FileDto> files = fileManagementService.getFilesByDirectoryAsDto(
                directoryService.getDirectoryById(directoryId)
                        .orElseThrow(() -> new RuntimeException("Directory not found"))
        );

        // root dosyalar
        List<FileDto> rootFiles = fileManagementService.getUserFilesInRootDirectoryAsDto(user);

        // Bu dizin için paylaşılan dizinler
        List<DirectoryShareDto> mySharedDirectories =
                directoryService.getMySharedDirectoriesForDirectory(user, directoryId);

        // Modele ekle
        modelAndView.addObject("currentDirectory", currentDirectory);
        modelAndView.addObject("subDirectories", subdirectories);
        modelAndView.addObject("files", files);                 // <-- FileDto listesi
        modelAndView.addObject("mySharedDirectories", mySharedDirectories);
        modelAndView.addObject("rootFiles", rootFiles);
        modelAndView.addObject("username", username);

        return modelAndView;
    }

    @PostMapping("/create")
    public String createDirectory(
            @RequestParam("name") String name,
            @RequestParam(value = "parentDirectoryId", required = false) Long parentDirectoryId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String username = authentication.getName();

        DirectoryDto directoryDto = new DirectoryDto(null, name, parentDirectoryId, username, List.of(),List.of());

        // Directory oluşturmak için DTO kullanın
        DirectoryDto createdDirectory = directoryService.createDirectory(directoryDto);
        logger.info("User '{}' created directory with ID {} and name '{}'", username, createdDirectory.id(), createdDirectory.name());
        // Flash attribute ekleyerek mesajı yönlendirmeye koruyun
        redirectAttributes.addFlashAttribute("message", "Dizin başarıyla oluşturuldu.");

        if (parentDirectoryId != null) {
            return "redirect:/directories/" + parentDirectoryId;
        } else {
            return "redirect:/directories";
        }
    }

    @DeleteMapping("/delete/{id}")
    public ModelAndView deleteDirectory(@PathVariable Long id, Authentication authentication) {
        ModelAndView modelAndView;

        String username = authentication.getName();
        User user = userManagementService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            // Silinecek dizinin üst dizin ID'sini al
            DirectoryDto directoryDto = directoryService.getDirectoryByIdAsDto(id, user);
            Long parentDirectoryId = directoryDto.parentDirectoryId();

            // Service üzerinden silme işlemi gerçekleştirin
            directoryService.deleteDirectoryById(id, user);

            // Silme sonrası yönlendirme
            modelAndView = new ModelAndView(parentDirectoryId != null
                    ? "redirect:/directories/" + parentDirectoryId
                    : "redirect:/directories");
            modelAndView.addObject("message", "Dizin başarıyla silindi.");
        } catch (RuntimeException e) {
            // Hata durumunda yönlendirme ve mesaj
            modelAndView = new ModelAndView("redirect:/directories");
            modelAndView.addObject("error", "Dizin silinirken bir hata oluştu: " + e.getMessage());
        }

        return modelAndView;
    }

    @PostMapping("/share")
    public ModelAndView shareDirectory(
            @RequestParam("directoryId") Long directoryId,
            @RequestParam("sharedWithUserEmail") String sharedWithEmail,
            Authentication authentication) {

        // Paylaşan kişinin kullanıcı adını al
        String username = authentication.getName();

        // Kullanıcı doğrulaması yap
        User owner = userManagementService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // DTO oluştur ve servis metodu çağır
        DirectoryShareRequestDto dto = new DirectoryShareRequestDto();
        dto.setDirectoryId(directoryId);
        dto.setUsername(username);
        dto.setSharedWithUserEmail(sharedWithEmail);

        directoryService.shareDirectory(dto, owner);

        return new ModelAndView("redirect:/directories");
    }

    @PostMapping("/revoke-share")
    public ModelAndView revokeDirectoryShare(
            @RequestParam("directoryId") Long directoryId,
            @RequestParam("sharedWithUserEmail") String sharedWithUserEmail,
            Authentication authentication) {

        // Paylaşan kişinin kullanıcı adını al
        String username = authentication.getName();

        // Kullanıcı doğrulaması yap
        User owner = userManagementService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Servis metodunu çağır
        directoryService.revokeDirectoryShare(directoryId, sharedWithUserEmail, owner);

        return new ModelAndView("redirect:/directories");
    }

    @GetMapping("/shared-with-me")
    public ModelAndView getSharedDirectories(Authentication authentication) {

        // Kullanıcı doğrulaması yap
        String username = authentication.getName();
        User user = userManagementService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Paylaşılan dizinleri getir
        List<DirectoryShareDto> sharedDirectories = directoryService.getSharedDirectories(user);

        // Paylaşılan dizinleri model ve view'a ekle
        ModelAndView modelAndView = new ModelAndView("shared_directories");
        modelAndView.addObject("sharedDirectories", sharedDirectories);
        return modelAndView;
    }


}
