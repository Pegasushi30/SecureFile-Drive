package com.example.securedrive.dto;

import java.util.UUID;

public record AuthenticationResponse(
        String accessToken,
        String refreshToken,
        Long userId
) {
}
