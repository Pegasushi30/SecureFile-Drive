package com.example.securedrive.mapper;

import org.springframework.security.core.GrantedAuthority;
import com.example.securedrive.dto.UserDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    public UserDto toUserDTO(Authentication authentication) {
        if (authentication.getPrincipal() instanceof DefaultOidcUser oidcUser) {
            String username = oidcUser.getPreferredUsername();
            if (username == null) {
                username = oidcUser.getAttribute("sub");
                if (username == null) {
                    username = oidcUser.getEmail();
                }
            }

            if (username == null) {
                throw new IllegalArgumentException("Username could not be resolved from OIDC token");
            }

            String email = resolveEmail(oidcUser);

            String displayName = oidcUser.getFullName();
            if (displayName == null) {
                displayName = oidcUser.getAttribute("name");
                if (displayName == null) {
                    displayName = "Unknown User";
                }
            }

            List<String> roles = oidcUser.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            return new UserDto(username, email, displayName, roles);
        }

        throw new IllegalArgumentException("Unsupported authentication principal");
    }

    private String resolveEmail(DefaultOidcUser oidcUser) {
        // Primary email resolution
        String email = oidcUser.getEmail();
        if (email == null) {
            email = oidcUser.getAttribute("email");
        }

        if (email == null) {
            List<String> emails = oidcUser.getAttribute("emails");
            if (emails != null && !emails.isEmpty()) {
                email = emails.get(0);
            }
        }

        return email != null ? email : "Email address not resolved";
    }
}
