package com.example.securedrive.security;

import com.example.securedrive.model.Role;
import com.example.securedrive.model.User;
import com.example.securedrive.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CustomOidcUserService extends OidcUserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) {
        System.out.println("loadUser çağrıldı!");
        OidcUser oidcUser = super.loadUser(userRequest);

        // Log attributes to inspect them
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

        // Declare a final variable for use in the lambda
        final String finalEmail = email;

        User user = userRepository.findByUsername(oid)
                .orElseGet(() -> createUser(oid, finalEmail, name != null ? name : "Anonymous"));

        return oidcUser;
    }



    private User createUser(String oid, String email, String name) {
        User newUser = new User();
        newUser.setUsername(oid);
        newUser.setEmail(email);
        newUser.setRole(Role.USER);
        newUser.setEncryptionKey(generateAESKey());
        return userRepository.save(newUser);
    }

    private String generateAESKey() {
        try {
            return AESUtil.generateAESKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate AES key", e);
        }
    }
}
