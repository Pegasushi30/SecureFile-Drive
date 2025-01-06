// com/example/securedrive/dto/FileRevokeShareRequestDto.java
package com.example.securedrive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileRevokeShareRequestDto {
    private Long fileId;
    private String sharedWithEmail;
    private String username;
    private String version;
}
