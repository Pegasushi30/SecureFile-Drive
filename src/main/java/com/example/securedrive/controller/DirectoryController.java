// com/example/securedrive/controller/DirectoryController.java
package com.example.securedrive.controller;

import com.example.securedrive.dto.DirectoryDto;
import com.example.securedrive.dto.DirectoryShareDto;
import com.example.securedrive.dto.DirectoryShareRequestDto;
import com.example.securedrive.dto.FileDto;
import com.example.securedrive.model.User;
import com.example.securedrive.service.DirectoryService;
import com.example.securedrive.service.FileManagementService;
import com.example.securedrive.service.UserManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/directories")
public class DirectoryController {

    private final UserManagementService userManagementService;
    private final DirectoryService directoryService;
    private final FileManagementService fileManagementService;

    public DirectoryController(UserManagementService userManagementService,
                               DirectoryService directoryService,
                               FileManagementService fileManagementService) {
        this.userManagementService = userManagementService;
        this.directoryService = directoryService;
        this.fileManagementService = fileManagementService;
    }

    @GetMapping
    public ModelAndView listDirectories(Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView("directories");
        if (authentication != null) {
            String username = authentication.getName();
            User user = userManagementService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            List<DirectoryDto> directories = directoryService.getRootDirectoriesAsDto(user);
            List<FileDto> files = fileManagementService.getUserFilesInRootDirectoryAsDto(user);
            List<DirectoryShareDto> mySharedDirectories = directoryService.getMySharedDirectories(user);
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
        DirectoryDto currentDirectory = directoryService.getDirectoryByIdAsDto(directoryId, user);
        List<DirectoryDto> subdirectories = directoryService.getSubDirectoriesAsDto(user, directoryId);
        List<FileDto> files = fileManagementService.getFilesByDirectoryAsDto(
                directoryService.getDirectoryById(directoryId)
                        .orElseThrow(() -> new RuntimeException("Directory not found"))
        );
        List<FileDto> rootFiles = fileManagementService.getUserFilesInRootDirectoryAsDto(user);
        List<DirectoryShareDto> mySharedDirectories =
                directoryService.getMySharedDirectoriesForDirectory(user, directoryId);
        modelAndView.addObject("currentDirectory", currentDirectory);
        modelAndView.addObject("subDirectories", subdirectories);
        modelAndView.addObject("files", files);
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
        directoryService.createDirectory(directoryDto);
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
            DirectoryDto directoryDto = directoryService.getDirectoryByIdAsDto(id, user);
            Long parentDirectoryId = directoryDto.parentDirectoryId();
            directoryService.deleteDirectoryById(id, user);
            modelAndView = new ModelAndView(parentDirectoryId != null
                    ? "redirect:/directories/" + parentDirectoryId
                    : "redirect:/directories");
            modelAndView.addObject("message", "Dizin başarıyla silindi.");
        } catch (RuntimeException e) {
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
        String username = authentication.getName();
        User owner = userManagementService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        DirectoryShareRequestDto dto = new DirectoryShareRequestDto();
        dto.setDirectoryId(directoryId);
        dto.setUsername(username);
        dto.setSharedWithUserEmail(sharedWithEmail);
        directoryService.shareDirectory(dto, owner);
        return new ModelAndView("redirect:/directories");
    }

    @PostMapping("/revoke-share")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> revokeDirectoryShare(
            @RequestParam("directoryId") Long directoryId,
            @RequestParam("sharedWithUserEmail") String sharedWithUserEmail,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User owner = userManagementService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            directoryService.revokeDirectoryShare(directoryId, sharedWithUserEmail, owner);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Paylaşım başarıyla iptal edildi!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/shared-with-me")
    public ModelAndView getSharedDirectories(Authentication authentication) {

        String username = authentication.getName();
        User user = userManagementService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<DirectoryShareDto> sharedDirectories = directoryService.getSharedDirectories(user);
        ModelAndView modelAndView = new ModelAndView("shared_directories");
        modelAndView.addObject("sharedDirectories", sharedDirectories);
        return modelAndView;
    }


}
