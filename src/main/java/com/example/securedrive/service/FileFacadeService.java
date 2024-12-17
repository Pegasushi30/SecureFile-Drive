// com/example/securedrive/service/FileFacadeService.java
package com.example.securedrive.service;

import com.example.securedrive.dto.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.core.Authentication;



public interface FileFacadeService {
    void revokeShare(FileRevokeShareRequestDto dto, Authentication authentication);
    void shareFile(FileShareRequestDto dto, Authentication authentication) throws Exception;
    FileDownloadSharedResponseDto downloadSharedFile(FileDownloadSharedRequestDto dto) throws Exception;
    String uploadFile(FileUploadRequestDto dto);
    String deleteSpecificVersion(FileDeleteSpecificVersionRequestDto dto);
    ByteArrayResource downloadSpecificVersion(FileDownloadSpecificVersionRequestDto dto) throws Exception;
}
