package com.example.securedrive.mapper;

import com.example.securedrive.dto.FileVersionDto;
import com.example.securedrive.model.FileVersion;
import org.springframework.stereotype.Component;

/**
 * Mapper class to transform FileVersion entities into FileVersionDto objects.
 */
@Component
public class FileVersionMapper {

    /**
     * Converts a FileVersion entity to a FileVersionDto.
     *
     * @param version the FileVersion entity
     * @return a FileVersionDto representing the FileVersion entity
     */
    public FileVersionDto toDto(FileVersion version) {
        return new FileVersionDto(
                version.getId(),
                version.getVersionNumber(),
                version.getDeltaPath(),
                version.getHash(),
                version.getTimestamp()
        );
    }
}
