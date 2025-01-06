package com.example.securedrive.service;

import com.example.securedrive.dto.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileFacadeServiceTest {
    private AutoCloseable closeable;
    @Mock
    private FileFacadeService fileFacadeService;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testRevokeShare() {
        // Given
        FileRevokeShareRequestDto dto = new FileRevokeShareRequestDto(1L, "shared@example.com", "owner", "v1");

        // When
        doNothing().when(fileFacadeService).revokeShare(dto, authentication);
        fileFacadeService.revokeShare(dto, authentication);

        // Then
        verify(fileFacadeService, times(1)).revokeShare(dto, authentication);
    }

    @Test
    void testShareFile() throws Exception {
        // Given
        FileShareRequestDto dto = new FileShareRequestDto("owner", 1L, "shared@example.com", "v1");

        // When
        doNothing().when(fileFacadeService).shareFile(dto, authentication);
        fileFacadeService.shareFile(dto, authentication);

        // Then
        verify(fileFacadeService, times(1)).shareFile(dto, authentication);
    }

    @Test
    void testDownloadSharedFile() throws Exception {
        // Given
        FileDownloadSharedRequestDto dto = new FileDownloadSharedRequestDto(1L, "user");
        byte[] originalData = new byte[0];
        String fileName = "file.txt";
        ByteArrayResource resource = new ByteArrayResource(originalData);
        FileDownloadSharedResponseDto expectedResponse = new FileDownloadSharedResponseDto(originalData, fileName, resource);

        when(fileFacadeService.downloadSharedFile(dto)).thenReturn(expectedResponse);

        // When
        FileDownloadSharedResponseDto actualResponse = fileFacadeService.downloadSharedFile(dto);

        // Then
        assertNotNull(actualResponse, "Response should not be null");
        assertEquals(expectedResponse.getFileName(), actualResponse.getFileName(), "File names should match");
        assertArrayEquals(expectedResponse.getOriginalData(), actualResponse.getOriginalData(), "Original data should match");
        assertEquals(expectedResponse.getResource(), actualResponse.getResource(), "Resources should match");
        verify(fileFacadeService, times(1)).downloadSharedFile(dto);
    }


    @Test
    void testUploadFile() {
        // Given
        FileUploadRequestDto dto = new FileUploadRequestDto("user", 1L, null, "v1");
        String expectedMessage = "File uploaded successfully";

        when(fileFacadeService.uploadFile(dto)).thenReturn(expectedMessage);

        // When
        String actualMessage = fileFacadeService.uploadFile(dto);

        // Then
        assertNotNull(actualMessage);
        assertEquals(expectedMessage, actualMessage);
        verify(fileFacadeService, times(1)).uploadFile(dto);
    }

    @Test
    void testDeleteSpecificVersion() {
        // Given
        FileDeleteSpecificVersionRequestDto dto = new FileDeleteSpecificVersionRequestDto("user", 1L, "v1");
        String expectedMessage = "File version deleted successfully";

        when(fileFacadeService.deleteSpecificVersion(dto)).thenReturn(expectedMessage);

        // When
        String actualMessage = fileFacadeService.deleteSpecificVersion(dto);

        // Then
        assertNotNull(actualMessage);
        assertEquals(expectedMessage, actualMessage);
        verify(fileFacadeService, times(1)).deleteSpecificVersion(dto);
    }

    @Test
    void testDownloadSpecificVersion() throws Exception {
        // Given
        FileDownloadSpecificVersionRequestDto dto = new FileDownloadSpecificVersionRequestDto("user", 1L, "v1");
        ByteArrayResource expectedResource = new ByteArrayResource(new byte[0]);

        when(fileFacadeService.downloadSpecificVersion(dto)).thenReturn(expectedResource);

        // When
        ByteArrayResource actualResource = fileFacadeService.downloadSpecificVersion(dto);

        // Then
        assertNotNull(actualResource);
        assertEquals(expectedResource, actualResource);
        verify(fileFacadeService, times(1)).downloadSpecificVersion(dto);
    }

    @Test
    void testGetRemainingShares() {
        // Given
        Long fileId = 1L;
        int expectedShares = 5;

        when(fileFacadeService.getRemainingShares(fileId)).thenReturn(expectedShares);

        // When
        int actualShares = fileFacadeService.getRemainingShares(fileId);

        // Then
        assertEquals(expectedShares, actualShares);
        verify(fileFacadeService, times(1)).getRemainingShares(fileId);
    }
}
