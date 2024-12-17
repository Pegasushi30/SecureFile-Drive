// com/example/securedrive/dto/FileShareDto.java
package com.example.securedrive.dto;

public record FileShareDto(
        Long id,
        String sharedWithUserEmail,
        String version,
        String sasUrl
) {}
