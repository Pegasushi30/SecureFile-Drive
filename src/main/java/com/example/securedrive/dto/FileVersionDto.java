package com.example.securedrive.dto;

import java.time.LocalDateTime;

public record FileVersionDto(
        Long id,
        String versionNumber,
        String deltaPath,
        String hash,
        LocalDateTime timestamp,
        Long size,
        Long directoryId // <-- Yeni alan
) {}

