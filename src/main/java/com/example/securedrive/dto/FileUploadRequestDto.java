package com.example.securedrive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadRequestDto {
    private String username;
    private Long directoryId;
    private MultipartFile file;
    private String versionNumber;
}

