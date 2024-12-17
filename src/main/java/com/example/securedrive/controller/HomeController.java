package com.example.securedrive.controller;

import com.example.securedrive.dto.DirectoryDto;
import com.example.securedrive.dto.FileDto;
import com.example.securedrive.dto.FileShareDto;
import com.example.securedrive.dto.UserDto;
import com.example.securedrive.mapper.UserMapper;
import com.example.securedrive.service.DirectoryService;
import com.example.securedrive.service.FileManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@RestController
public class HomeController {

    private final FileManagementService fileManagementService;
    private final DirectoryService directoryService;
    private final UserMapper userMapper;

    @Autowired
    public HomeController(FileManagementService fileManagementService,
                          DirectoryService directoryService,
                          UserMapper userMapper) {
        this.fileManagementService = fileManagementService;
        this.directoryService = directoryService;
        this.userMapper = userMapper;
    }

    @GetMapping("/home")
    public ModelAndView home(Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView("home");
        UserDto userDto = authentication != null ? userMapper.toUserDTO(authentication) : null;

        if (userDto != null) {
            modelAndView.addObject("username", userDto.getUsername());
            modelAndView.addObject("email", userDto.getEmail());
            modelAndView.addObject("displayName", userDto.getDisplayName());
            modelAndView.addObject("roles", userDto.getRoles());
        } else {
            modelAndView.addObject("username", "Misafir");
            modelAndView.addObject("roles", List.of("Yok"));
            modelAndView.addObject("email", "E-posta bulunamadı");
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
            modelAndView.addObject("fileShares", sharedFiles);
            modelAndView.addObject("username", userDto.getUsername());
        } else {
            modelAndView.addObject("fileShares", List.of());
        }

        return modelAndView;
    }
}
