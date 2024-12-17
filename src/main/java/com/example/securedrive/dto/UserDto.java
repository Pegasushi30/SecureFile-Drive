package com.example.securedrive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class UserDto {
    private String username;
    private String email;
    private String displayName;
    private List<String> roles;

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
