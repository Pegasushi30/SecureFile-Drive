package com.example.securedrive.dto;

public record RegisterResponse(
        Long userId,
        String aesKey
) {
}
