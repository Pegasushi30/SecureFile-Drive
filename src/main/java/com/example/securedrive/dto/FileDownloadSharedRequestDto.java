// com/example/securedrive/dto/FileDownloadSharedRequestDto.java
package com.example.securedrive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FileDownloadSharedRequestDto {
    private Long fileId;
    private String username;
}
