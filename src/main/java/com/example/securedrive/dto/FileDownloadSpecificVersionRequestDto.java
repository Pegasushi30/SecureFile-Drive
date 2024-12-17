// com/example/securedrive/dto/FileDownloadSpecificVersionRequestDto.java
package com.example.securedrive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FileDownloadSpecificVersionRequestDto {
    private String username;
    private Long fileId;
    private String versionNumber;
}


