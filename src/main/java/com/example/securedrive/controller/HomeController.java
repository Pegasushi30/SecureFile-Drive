package com.example.securedrive.controller;

import com.example.securedrive.model.Directory;
import com.example.securedrive.model.File;
import com.example.securedrive.model.FileShare;
import com.example.securedrive.model.User;
import com.example.securedrive.repository.FileShareRepository;
import com.example.securedrive.service.DirectoryService;
import com.example.securedrive.service.FileService;
import com.example.securedrive.service.UserService;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class HomeController {

    private final UserService userService;
    private final FileService fileService;
    private final DirectoryService directoryService;
    private final FileShareRepository fileShareRepository;

    @Autowired
    public HomeController(UserService userService, FileService fileService, DirectoryService directoryService, FileShareRepository fileShareRepository) {
        this.userService = userService;
        this.fileService = fileService;
        this.directoryService = directoryService;
        this.fileShareRepository = fileShareRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @GetMapping("/home")
    public ModelAndView home(Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView("home");

        if (authentication != null && authentication.getPrincipal() instanceof DefaultOAuth2User principal) {
            String username = authentication.getName();
            String email = principal.getAttribute("emails") != null
                    ? ((List<String>) principal.getAttribute("emails")).get(0)
                    : "E-posta bulunamadı";
            String displayName = principal.getAttribute("name") != null
                    ? principal.getAttribute("name")
                    : "İsim bulunamadı";
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            modelAndView.addObject("username", username);
            modelAndView.addObject("email", email);
            modelAndView.addObject("displayName", displayName);
            modelAndView.addObject("roles", roles);

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

        if (authentication != null) {
            String username = authentication.getName();
            Optional<User> userOptional = userService.findByUsername(username);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                List<File> files = fileService.getFilesByUser(user);

                files.forEach(file -> {
                    Hibernate.initialize(file.getVersions());
                    Hibernate.initialize(file.getFileShares());
                    file.getFileShares().forEach(fileShare -> Hibernate.initialize(fileShare.getSharedWithUser()));
                });

                // Burada directories'i ekliyoruz
                List<Directory> directories = directoryService.getUserDirectories(user);
                modelAndView.addObject("directories", directories);

                modelAndView.addObject("files", files);
                modelAndView.addObject("username", username);
            } else {
                modelAndView.addObject("files", List.of());
                modelAndView.addObject("directories", List.of());
            }
        } else {
            modelAndView.addObject("files", List.of());
            modelAndView.addObject("directories", List.of());
        }

        return modelAndView;
    }



    @GetMapping("/upload")
    public ModelAndView uploadPage(Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView("upload");

        if (authentication != null) {
            String username = authentication.getName();
            modelAndView.addObject("username", username);

            Optional<User> userOptional = userService.findByUsername(username);
            if (userOptional.isPresent()) {
                List<Directory> directories = directoryService.getUserDirectories(userOptional.get());
                modelAndView.addObject("directories", directories);
            } else {
                modelAndView.addObject("directories", List.of());
            }
        } else {
            modelAndView.addObject("directories", List.of());
        }

        return modelAndView;
    }

    @GetMapping("/shared-files")
    public ModelAndView sharedFilesPage(Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView("shared-files");

        if (authentication != null) {
            String username = authentication.getName();
            Optional<User> userOptional = userService.findByUsername(username);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                List<FileShare> sharedFileShares = fileShareRepository.findAllBySharedWithUser(user);

                sharedFileShares.forEach(fileShare -> {
                    Hibernate.initialize(fileShare.getFile());
                    Hibernate.initialize(fileShare.getOwner());
                    Hibernate.initialize(fileShare.getSharedWithUser());

                    // Loglama ile verilerin doğru şekilde yüklendiğini kontrol edin
                    logger.debug("FileShare - File ID: {}, File Name: {}, Version: {}, Owner: {}",
                            fileShare.getFile().getId(),
                            fileShare.getFile().getFileName(),
                            fileShare.getVersion(),
                            fileShare.getOwner().getEmail()); // owner.username yerine owner.email
                });

                modelAndView.addObject("fileShares", sharedFileShares);
                modelAndView.addObject("username", username);
            } else {
                modelAndView.addObject("fileShares", List.of());
            }
        } else {
            modelAndView.addObject("fileShares", List.of());
        }

        return modelAndView;
    }

}
