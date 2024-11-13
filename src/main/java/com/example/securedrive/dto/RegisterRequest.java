package com.example.securedrive.dto;

public record RegisterRequest(
        String username,
        String password,
        String email
) {
}
