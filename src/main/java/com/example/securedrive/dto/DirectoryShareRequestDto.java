package com.example.securedrive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DirectoryShareRequestDto {
    private Long directoryId;
    private String username; // Paylaşan kişinin kullanıcı adı
    private String sharedWithUserEmail; // Paylaşılan kişinin email adresi
}
