package com.example.securedrive.controller;

import com.example.securedrive.dto.*;
import com.example.securedrive.mapper.UserMapper;
import com.example.securedrive.model.User;
import com.example.securedrive.service.util.FileSizeUtil;
import com.example.securedrive.service.DirectoryService;
import com.example.securedrive.service.FileManagementService;
import com.example.securedrive.service.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class HomeController {

    private final FileManagementService fileManagementService;
    private final DirectoryService directoryService;
    private final UserMapper userMapper;
    private final UserManagementService userManagementService;

    @Autowired
    public HomeController(FileManagementService fileManagementService,
                          DirectoryService directoryService,
                          UserMapper userMapper,
                          UserManagementService userManagementService) {
        this.fileManagementService = fileManagementService;
        this.directoryService = directoryService;
        this.userMapper = userMapper;
        this.userManagementService = userManagementService;
    }

    @GetMapping("/home")
    public ModelAndView home(Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView("home");
        UserDto userDto = authentication != null ? userMapper.toUserDTO(authentication) : null;

        if (userDto != null) {
            // Kullanıcı bilgileri
            modelAndView.addObject("username", userDto.getUsername());
            modelAndView.addObject("email", userDto.getEmail());
            modelAndView.addObject("displayName", userDto.getDisplayName());
            modelAndView.addObject("roles", userDto.getRoles());

            // Son yüklenen dosyalar
            List<FileVersionDto> lastUploadedFiles = fileManagementService.getLastUploadedFileVersions(userDto.getUsername(), 5);
            Map<Long, String> lastUploadedNames = lastUploadedFiles.stream()
                    .collect(Collectors.toMap(FileVersionDto::id,
                            f -> fileManagementService.getFileNameByVersionId(f.id())));

            Map<Long, String> lastUploadedSizes = lastUploadedFiles.stream()
                    .collect(Collectors.toMap(FileVersionDto::id,
                            f -> FileSizeUtil.formatSize(f.size())));

            modelAndView.addObject("lastUploadedFiles", lastUploadedFiles);
            modelAndView.addObject("lastUploadedNames", lastUploadedNames);
            modelAndView.addObject("lastUploadedSizes", lastUploadedSizes);

            // Son erişilen dosyalar
            List<FileVersionDto> lastAccessedFiles = fileManagementService.getLastAccessedFileVersions(userDto.getUsername(), 5);
            Map<Long, String> lastAccessedNames = lastAccessedFiles.stream()
                    .collect(Collectors.toMap(FileVersionDto::id,
                            f -> fileManagementService.getFileNameByVersionId(f.id())));

            Map<Long, String> lastAccessedSizes = lastAccessedFiles.stream()
                    .collect(Collectors.toMap(FileVersionDto::id,
                            f -> FileSizeUtil.formatSize(f.size())));

            modelAndView.addObject("lastAccessedFiles", lastAccessedFiles);
            modelAndView.addObject("lastAccessedNames", lastAccessedNames);
            modelAndView.addObject("lastAccessedSizes", lastAccessedSizes);

            // Toplam ve kullanılan depolama
            long totalStorage = fileManagementService.getTotalStorage(userDto.getUsername());
            long usedStorage = fileManagementService.getUsedStorage(userDto.getUsername());
            long remainingStorage = totalStorage - usedStorage;

            String totalStorageFormatted = FileSizeUtil.formatSize(totalStorage);
            String remainingStorageFormatted = FileSizeUtil.formatSize(remainingStorage);
            double remainingPercentage = (remainingStorage / (double) totalStorage) * 100;

            modelAndView.addObject("totalStorage", totalStorageFormatted);
            modelAndView.addObject("remainingStorage", remainingStorageFormatted);
            modelAndView.addObject("remainingPercentage", remainingPercentage);

            // Contacts
            Set<User> contacts = userManagementService.getContactsForUser(userDto.getUsername());
            modelAndView.addObject("contacts", contacts);

            // Seninle paylaşılan dosyalar
            List<FileShareDto> sharedFiles = fileManagementService.getSharedFilesByUsername(userDto.getUsername());
            modelAndView.addObject("sharedFiles", sharedFiles);
        }

        return modelAndView;
    }







    @GetMapping("/files")
    public ModelAndView filesPage(Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView("files");
        UserDto userDto = authentication != null ? userMapper.toUserDTO(authentication) : null;

        if (userDto != null) {
            List<FileDto> files = fileManagementService.getFilesByUsername(userDto.getUsername());
            List<DirectoryDto> directories = directoryService.getDirectoriesByUsername(userDto.getUsername());
            modelAndView.addObject("files", files);
            modelAndView.addObject("directories", directories);
            modelAndView.addObject("username", userDto.getUsername());
        } else {
            modelAndView.addObject("files", List.of());
            modelAndView.addObject("directories", List.of());
        }

        return modelAndView;
    }

    @GetMapping("/upload")
    public ModelAndView uploadPage(Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView("upload");
        UserDto userDto = authentication != null ? userMapper.toUserDTO(authentication) : null;

        if (userDto != null) {
            List<DirectoryDto> directories = directoryService.getDirectoriesByUsername(userDto.getUsername());
            modelAndView.addObject("directories", directories);
            modelAndView.addObject("username", userDto.getUsername());
        } else {
            modelAndView.addObject("directories", List.of());
        }

        return modelAndView;
    }

    @GetMapping("/shared-files")
    public ModelAndView sharedFilesPage(Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView("shared-files");
        UserDto userDto = authentication != null ? userMapper.toUserDTO(authentication) : null;

        if (userDto != null) {
            List<FileShareDto> sharedFiles = fileManagementService.getSharedFilesByUsername(userDto.getUsername());

            // Paylaşan e-posta adresine ve dizin yoluna göre gruplama
            Map<String, Map<String, List<FileShareDto>>> groupedShares = sharedFiles.stream()
                    .collect(Collectors.groupingBy(
                            FileShareDto::ownerEmail,
                            Collectors.groupingBy(FileShareDto::directoryPath)
                    ));

            modelAndView.addObject("groupedShares", groupedShares);
            modelAndView.addObject("username", userDto.getUsername());
        } else {
            modelAndView.addObject("groupedShares", Map.of());
        }

        return modelAndView;
    }


    @GetMapping("/intro")
    public ModelAndView introPage() {
        return new ModelAndView("intro");
    }

}
