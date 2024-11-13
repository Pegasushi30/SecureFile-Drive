
package com.example.securedrive.dto;

import java.util.UUID;

public record RegisterResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String aesKey
) {
}
