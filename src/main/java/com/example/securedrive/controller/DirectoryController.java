package com.example.securedrive.controller;

import com.example.securedrive.model.Directory;
import com.example.securedrive.model.File;
import com.example.securedrive.model.User;
import com.example.securedrive.service.DirectoryService;
import com.example.securedrive.service.FileService;
import com.example.securedrive.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/directories")
public class DirectoryController {

    @Autowired
    private UserService userService;

    @Autowired
    private DirectoryService directoryService;

    @Autowired
    private FileService fileService;

    // Dizinleri listeleme
    @GetMapping
    public ModelAndView listDirectories(Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView("directories");

        if (authentication != null) {
            String username = authentication.getName();
            Optional<User> userOptional = userService.findByUsername(username);

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // Fetch only root directories (parentDirectory is null)
                List<Directory> directories = directoryService.getRootDirectories(user);

                // Fetch files in the root directory (if any)
                List<File> userFiles = fileService.getUserFilesInRootDirectory(user);

                modelAndView.addObject("directories", directories);
                modelAndView.addObject("files", userFiles);
                modelAndView.addObject("username", username);
            }
        }

        return modelAndView;
    }


    // Mevcut dizin içeriklerini listeleme
    @GetMapping("/{directoryId}")
    public ModelAndView directoryContents(@PathVariable Long directoryId, Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView("directory_contents");

        String username = authentication.getName();
        Optional<User> userOptional = userService.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Dizin bilgilerini al
            Optional<Directory> currentDirectoryOpt = directoryService.getDirectoryById(directoryId);

            if (currentDirectoryOpt.isPresent()) {
                Directory currentDirectory = currentDirectoryOpt.get();

                // Alt dizinler
                List<Directory> subdirectories = directoryService.getSubDirectories(user, currentDirectory);

                // Dosyalar
                List<File> files = fileService.getFilesByDirectory(currentDirectory);

                // Kullanıcının ana dizinindeki dosyaları da ekliyoruz
                List<File> rootFiles = fileService.getUserFilesInRootDirectory(user);

                // Model'e verileri ekle
                modelAndView.addObject("currentDirectory", currentDirectory);
                modelAndView.addObject("subDirectories", subdirectories);
                modelAndView.addObject("files", files);
                modelAndView.addObject("rootFiles", rootFiles); // Ana dizindeki dosyalar
                modelAndView.addObject("username", username);
            }
        }

        return modelAndView;
    }


    // Yeni dizin oluşturma
    @PostMapping("/create")
    public ModelAndView createDirectory(
            @RequestParam("name") String name,
            @RequestParam(value = "parentDirectoryId", required = false) Long parentDirectoryId,
            @RequestParam("username") String username,
            Authentication authentication) {

        ModelAndView modelAndView = new ModelAndView("redirect:/directories");

        if (!authentication.getName().equals(username)) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Bu kullanıcı için dizin oluşturma yetkiniz yok.");
            return modelAndView;
        }

        Optional<User> userOptional = userService.findByUsername(username);
        if (userOptional.isEmpty()) {
            modelAndView.setViewName("error");
            modelAndView.addObject("message", "Kullanıcı bulunamadı.");
            return modelAndView;
        }

        User user = userOptional.get();

        Directory directory = new Directory();
        directory.setName(name);
        directory.setUser(user);

        if (parentDirectoryId != null) {
            Optional<Directory> parentDirectoryOptional = directoryService.findByIdAndUser(parentDirectoryId, user);
            parentDirectoryOptional.ifPresent(directory::setParentDirectory);
        }

        directoryService.saveDirectory(directory);

        return modelAndView;
    }

    @DeleteMapping("/delete/{id}")
    public ModelAndView deleteDirectory(@PathVariable Long id, Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView("redirect:/directories");

        String username = authentication.getName();
        Optional<User> userOptional = userService.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            Optional<Directory> directoryOptional = directoryService.findByIdAndUser(id, user);
            if (directoryOptional.isPresent()) {
                try {
                    Directory directory = directoryOptional.get();
                    directoryService.deleteDirectory(directory);
                    modelAndView.addObject("message", "Dizin başarıyla silindi.");
                } catch (Exception e) {
                    modelAndView.addObject("error", "Dizin silinirken bir hata oluştu: " + e.getMessage());
                }
            } else {
                modelAndView.addObject("error", "Dizin bulunamadı veya yetkiniz yok.");
            }
        } else {
            modelAndView.addObject("error", "Kullanıcı bulunamadı.");
        }

        return modelAndView;
    }


}
