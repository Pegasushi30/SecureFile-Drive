// DirectoryShareDto.java
package com.example.securedrive.dto;

public record DirectoryShareDto(
        Long id,
        Long directoryId,
        String directoryName,
        String sharedWithUserEmail,
        String ownerEmail,
        String sharedPath
) {}
