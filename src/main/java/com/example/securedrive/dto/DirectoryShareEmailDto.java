package com.example.securedrive.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DirectoryShareEmailDto {
    // Getters and Setters
    private Long directoryId;
    private String sharedWithEmail;

    public DirectoryShareEmailDto(Long directoryId, String sharedWithEmail) {
        this.directoryId = directoryId;
        this.sharedWithEmail = sharedWithEmail;
    }

}
