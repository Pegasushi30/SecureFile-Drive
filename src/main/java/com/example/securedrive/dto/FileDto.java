// com/example/securedrive/dto/FileDto.java
package com.example.securedrive.dto;

import java.util.List;

public record FileDto(
        Long id,
        String fileName,
        String path,
        String ownerUsername,
        Long directoryId,
        List<FileVersionDto> versions,
        List<FileShareDto> fileShares // Yeni eklenen alan
) {}


