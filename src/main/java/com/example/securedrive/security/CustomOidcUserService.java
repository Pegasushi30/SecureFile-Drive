package com.example.securedrive.security;

import com.example.securedrive.model.Role;
import com.example.securedrive.model.User;
import com.example.securedrive.repository.UserRepository;
import com.example.securedrive.service.util.AESUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CustomOidcUserService extends OidcUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KeyVaultService keyVaultService;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) {
        System.out.println("loadUser çağrıldı!");
        OidcUser oidcUser = super.loadUser(userRequest);

        // Kullanıcı özelliklerini al ve logla
        Map<String, Object> attributes = oidcUser.getAttributes();
        System.out.println("OIDC User Attributes: " + attributes);

        String oid = oidcUser.getSubject();
        String email = (String) attributes.get("email");

        if (email == null) {
            Object emailsAttr = attributes.get("emails");
            if (emailsAttr instanceof List<?> emailsList && !emailsList.isEmpty()) {
                email = (String) emailsList.get(0);
            }
        }

        if (email == null) {
            throw new IllegalStateException("Email attribute is missing in OIDC token");
        }

        String name = (String) attributes.get("name");
        if (name == null) {
            name = "Anonymous";
        }

        // Kullanıcıyı veritabanında ara
        User user = userRepository.findByUsername(oid).orElse(null);
        if (user == null) {
            // Kullanıcı yoksa oluştur
            user = createUser(oid, email, name);
        }

        return oidcUser;
    }

    private User createUser(String oid, String email, String name) {
        User newUser = new User();
        newUser.setUsername(oid);
        newUser.setEmail(email);
        newUser.setRole(Role.USER);

        // AES anahtarı oluştur ve Key Vault'a kaydet
        try {
            String encryptionKey = AESUtil.generateAESKey();
            keyVaultService.saveEncryptionKeyToKeyVault(oid, encryptionKey); // Key Vault'a kaydet
            System.out.println("AES anahtarı başarıyla oluşturuldu ve Key Vault'a kaydedildi.");
        } catch (Exception e) {
            System.err.println("AES anahtarı oluşturulurken hata oluştu: " + e.getMessage());
            throw new RuntimeException("Failed to generate AES key for user: " + oid, e);
        }

        // Kullanıcıyı veritabanına kaydet
        return userRepository.save(newUser);
    }
}
