package com.example.securedrive.dto;

public record AuthenticationRequest(
        String username,
        String password
) {
}
