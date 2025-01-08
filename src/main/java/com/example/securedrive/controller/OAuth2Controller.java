package com.example.securedrive.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
public class OAuth2Controller {
    // Azure B2C giriş akışını başlatır
    @GetMapping("/oauth2/authorization/azureb2c")
    public String redirectToAzureOAuth2() {
        return "redirect:/oauth2/authorization/azureb2c";
    }

    // Azure B2C kimlik doğrulama geri dönüşünü ele alır
    @GetMapping("/login/oauth2/code/azureb2c")
    public String handleAzureOAuth2Callback() {
        return "redirect:/home"; // Giriş başarılıysa yönlendirilecek sayfa
    }

    @GetMapping("/forgot-password")
    public void forgotPassword(HttpServletResponse response) throws IOException {
        String redirectUrl = "https://sdfile.b2clogin.com/sdfile.onmicrosoft.com/B2C_1_passwordreset/oauth2/v2.0/authorize" +
                "?p=B2C_1_passwordreset" +
                "&client_id=6c4e0c80-e9f5-4922-a3d3-097549f658d2" +
                "&nonce=defaultNonce" +
                "&redirect_uri=https%3A%2F%2Flocalhost%3A8443%2Fpassword-reset-callback" + // Şifre sıfırlama dönüş URL'si
                "&scope=openid" +
                "&response_type=code" +
                "&prompt=login";
        response.sendRedirect(redirectUrl);
    }
    @GetMapping("/password-reset-callback")
    public String handlePasswordResetCallback(@RequestParam(value = "error", required = false) String error) {
        if ("access_denied".equals(error)) {
            // Kullanıcı "cancel" tuşuna bastıysa home sayfasına yönlendirin
            return "redirect:/home";
        }
        // Başarılı şifre sıfırlama sonrası bir bilgi mesajı gösterebilirsiniz
        return "redirect:/home?resetSuccess=true";
    }

}

