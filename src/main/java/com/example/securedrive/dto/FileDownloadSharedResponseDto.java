package com.example.securedrive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.ByteArrayResource;

@Getter
@Setter
@AllArgsConstructor
public class FileDownloadSharedResponseDto {
    private byte[] originalData;
    private String fileName;
    ByteArrayResource resource;
}
