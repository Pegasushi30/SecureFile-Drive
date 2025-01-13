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

        User user = userRepository.findByUsername(oid).orElse(null);
        if (user == null) {
            createUser(oid, email, name);
        }

        return oidcUser;
    }

    private void createUser(String oid, String email, String name) {
        User newUser = new User();
        newUser.setUsername(oid);
        newUser.setEmail(email);
        newUser.setRole(Role.USER);
        try {
            String encryptionKey = AESUtil.generateAESKey();
            keyVaultService.saveEncryptionKeyToKeyVault(oid, encryptionKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate AES key for user: " + oid, e);
        }
        userRepository.save(newUser);
    }
}
