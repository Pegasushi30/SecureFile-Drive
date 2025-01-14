package com.example.securedrive.mapper;

import com.example.securedrive.dto.FileDto;
import com.example.securedrive.dto.FileShareDto;
import com.example.securedrive.dto.FileVersionDto;
import com.example.securedrive.model.File;
import com.example.securedrive.model.FileVersion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FileMapper {

    private final FileShareMapper fileShareMapper;
    private final ObjectMapper objectMapper; // JSON dönüşümü için

    public FileMapper(FileShareMapper fileShareMapper,
                      ObjectMapper objectMapper) {
        this.fileShareMapper = fileShareMapper;
        this.objectMapper = objectMapper;
    }

    public FileDto toDto(File file) {
        if (file == null) return null;

        // FileVersionDto Listesi
        List<FileVersionDto> versions = (file.getVersions() != null)
                ? file.getVersions().stream()
                .map(this::toVersionDto)
                .collect(Collectors.toList())
                : List.of();

        // FileShareDto Listesi
        List<FileShareDto> fileShares = (file.getFileShares() != null)
                ? file.getFileShares().stream()
                .map(fileShareMapper::toDto)
                .collect(Collectors.toList())
                : List.of();

        // fileShares JSON string'i
        String fileSharesJson = "[]";
        try {
            fileSharesJson = objectMapper.writeValueAsString(fileShares);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return new FileDto(
                file.getId(),
                file.getFileName(),
                file.getPath(),
                file.getUser().getUsername(),
                (file.getDirectory() != null) ? file.getDirectory().getId() : null,
                versions,
                fileShares,
                fileSharesJson
        );
    }

    private FileVersionDto toVersionDto(FileVersion version) {
        // directoryId parametresi eklendi:
        Long directoryId = null;
        if (version.getFile() != null && version.getFile().getDirectory() != null) {
            directoryId = version.getFile().getDirectory().getId();
        }

        return new FileVersionDto(
                version.getId(),
                version.getVersionNumber(),
                version.getDeltaPath(),
                version.getHash(),
                version.getTimestamp(),
                version.getSize(),
                directoryId
        );
    }
}
