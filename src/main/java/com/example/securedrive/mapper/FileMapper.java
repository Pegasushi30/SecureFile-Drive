// com/example/securedrive/mapper/FileMapper.java

package com.example.securedrive.mapper;

import com.example.securedrive.dto.FileDto;
import com.example.securedrive.dto.FileShareDto;
import com.example.securedrive.dto.FileVersionDto;
import com.example.securedrive.dto.DirectoryDto;
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
    private final DirectoryMapper directoryMapper;
    private final ObjectMapper objectMapper; // JSON dönüşümü için

    public FileMapper(FileShareMapper fileShareMapper,
                      DirectoryMapper directoryMapper,
                      ObjectMapper objectMapper) {
        this.fileShareMapper = fileShareMapper;
        this.directoryMapper = directoryMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Tek DTO: Normal FileDto + fileSharesJson
     */
    public FileDto toDto(File file) {
        if (file == null) return null;

        // Versiyonları dönüştür
        List<FileVersionDto> versions = (file.getVersions() != null)
                ? file.getVersions().stream()
                .map(this::toVersionDto)
                .collect(Collectors.toList())
                : List.of();

        // file.fileShares -> List<FileShareDto>
        List<FileShareDto> fileShares = (file.getFileShares() != null)
                ? file.getFileShares().stream()
                .map(fileShareMapper::toDto)
                .collect(Collectors.toList())
                : List.of();

        // JSON string
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
                fileSharesJson  // Burada ekliyoruz
        );
    }

    // Yardımcı: FileVersion -> FileVersionDto
    private FileVersionDto toVersionDto(FileVersion version) {
        return new FileVersionDto(
                version.getId(),
                version.getVersionNumber(),
                version.getDeltaPath(),
                version.getHash(),
                version.getTimestamp(),
                version.getSize()
        );
    }
}
