// com/example/securedrive/mapper/FileShareMapper.java
package com.example.securedrive.mapper;

import com.example.securedrive.dto.FileShareDto;
import com.example.securedrive.model.FileShare;
import org.springframework.stereotype.Component;

@Component
public class FileShareMapper {
    public FileShareDto toDto(FileShare fileShare) {
        if (fileShare == null) return null;

        String fileName = fileShare.getFile() != null ? fileShare.getFile().getFileName() : "Bilinmeyen Dosya";
        String ownerEmail = fileShare.getOwner() != null ? fileShare.getOwner().getEmail() : "Bilinmeyen Sahip";

        return new FileShareDto(
                fileShare.getId(),
                fileShare.getSharedWithUser().getEmail(),
                fileShare.getVersion(),
                fileShare.getSasUrl(),
                fileName,
                ownerEmail
        );
    }
}

