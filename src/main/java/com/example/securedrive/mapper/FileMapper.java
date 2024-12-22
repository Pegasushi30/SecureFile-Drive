// com/example/securedrive/mapper/FileMapper.java
package com.example.securedrive.mapper;

import com.example.securedrive.dto.FileDto;
import com.example.securedrive.dto.FileVersionDto;
import com.example.securedrive.dto.FileShareDto;
import com.example.securedrive.dto.DirectoryDto;
import com.example.securedrive.model.File;
import com.example.securedrive.model.FileVersion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FileMapper {

    private final FileShareMapper fileShareMapper;
    private final DirectoryMapper directoryMapper;

    public FileMapper(FileShareMapper fileShareMapper, DirectoryMapper directoryMapper) {
        this.fileShareMapper = fileShareMapper;
        this.directoryMapper = directoryMapper;
    }

    public FileDto toDto(File file) {
        if (file == null) return null;

        DirectoryDto directoryDto = file.getDirectory() != null ? directoryMapper.toDto(file.getDirectory()) : null;
        List<FileVersionDto> versions = file.getVersions() != null
                ? file.getVersions().stream().map(this::toVersionDto).collect(Collectors.toList())
                : List.of(); // Boş liste olarak ayarla
        List<FileShareDto> fileShares = file.getFileShares() != null
                ? file.getFileShares().stream().map(fileShareMapper::toDto).collect(Collectors.toList())
                : List.of(); // Boş liste olarak ayarla

        return new FileDto(
                file.getId(),
                file.getFileName(),
                file.getPath(),
                file.getUser().getUsername(),
                file.getDirectory() != null ? file.getDirectory().getId() : null,
                versions,
                fileShares
        );
    }

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

