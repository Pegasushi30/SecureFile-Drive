package com.example.securedrive.controller;

import com.example.securedrive.model.File;
import com.example.securedrive.model.User;
import com.example.securedrive.service.FileService;
import com.example.securedrive.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final UserService userService;
    private final FileService fileService;

    @Autowired
    public HomeController(UserService userService, FileService fileService) {
        this.userService = userService;
        this.fileService = fileService;
    }

    @GetMapping("/home")
    public ModelAndView home(Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView("home");

        if (authentication != null && authentication.getPrincipal() instanceof DefaultOAuth2User principal) {
            // Kullanıcı bilgileri
            String username = authentication.getName();
            String email = principal.getAttribute("emails") != null
                    ? ((List<String>) principal.getAttribute("emails")).get(0)
                    : "E-posta bulunamadı";
            String displayName = principal.getAttribute("name");
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // Model bilgileri
            modelAndView.addObject("username", username);
            modelAndView.addObject("email", email);
            modelAndView.addObject("displayName", displayName != null ? displayName : "İsim bulunamadı");
            modelAndView.addObject("roles", roles);

        } else {
            // Misafir kullanıcısı için varsayılan değerler
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
                List<File> files = fileService.getFilesByUser(userOptional.get());
                System.out.println("Files retrieved: " + files); // Debug log
                modelAndView.addObject("files", files);
                modelAndView.addObject("username", username);
            } else {
                modelAndView.addObject("files", List.of());
            }
        } else {
            modelAndView.addObject("files", List.of());
        }

        return modelAndView;
    }




    @GetMapping("/upload")
    public ModelAndView uploadPage(Authentication authentication) {
        ModelAndView modelAndView = new ModelAndView("upload");
        modelAndView.addObject("username", authentication != null ? authentication.getName() : "Misafir");
        return modelAndView;
    }
}
