package com.example.securedrive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FileDownloadResponse {
    private String fileName;
    private byte[] fileData;
}
