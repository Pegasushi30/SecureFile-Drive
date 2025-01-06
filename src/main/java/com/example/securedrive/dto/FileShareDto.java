package com.example.securedrive.dto;

public record FileShareDto(
        Long id,
        String sharedWithUserEmail,
        String version,
        String sasUrl,
        String fileName,
        String ownerEmail,
        String directoryName,  // Dizin adı
        String directoryPath   // Dizin yolu
) {}
