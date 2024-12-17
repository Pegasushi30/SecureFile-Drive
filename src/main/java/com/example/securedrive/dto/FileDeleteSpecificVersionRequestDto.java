// com/example/securedrive/dto/FileDeleteSpecificVersionRequestDto.java
package com.example.securedrive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class FileDeleteSpecificVersionRequestDto {
    private String username;
    private Long fileId;
    private String versionNumber;

}
