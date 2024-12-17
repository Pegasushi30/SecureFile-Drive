// com/example/securedrive/mapper/FileShareMapper.java
package com.example.securedrive.mapper;

import com.example.securedrive.dto.FileShareDto;
import com.example.securedrive.model.FileShare;
import org.springframework.stereotype.Component;

@Component
public class FileShareMapper {

    public FileShareDto toDto(FileShare fileShare) {
        if (fileShare == null) return null;
        return new FileShareDto(
                fileShare.getId(),
                fileShare.getSharedWithUser().getEmail(),
                fileShare.getVersion(),
                fileShare.getSasUrl()
        );
    }
}

