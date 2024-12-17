package com.example.securedrive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileShareRequestDto {
    private String username;
    private Long fileId;
    private String sharedWithEmail;
    private String version;
}

