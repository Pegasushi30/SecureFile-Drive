package com.example.securedrive.dto;

import java.util.List;

public record DirectoryDto(
        Long id,
        String name,
        Long parentDirectoryId,
        String username,
        List<DirectoryDto> subDirectories,
        List<DirectoryShareDto> directoryShares
) {}

