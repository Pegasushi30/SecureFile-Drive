package com.example.securedrive.service;

import com.example.securedrive.model.File;
import com.example.securedrive.model.FileVersion;
import com.example.securedrive.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileVersionManagementServiceTest {
    private AutoCloseable closeable;
    @Mock
    private FileVersionManagementService fileVersionManagementService;

    @BeforeEach
    void setUp() {
       closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testSaveFileVersion() {
        // Given
        FileVersion version = new FileVersion();
        doNothing().when(fileVersionManagementService).saveFileVersion(version);

        // When
        fileVersionManagementService.saveFileVersion(version);

        // Then
        verify(fileVersionManagementService, times(1)).saveFileVersion(version);
    }

    @Test
    void testGetAllVersions() {
        // Given
        File file = new File();
        FileVersion version1 = new FileVersion();
        FileVersion version2 = new FileVersion();
        List<FileVersion> expectedVersions = List.of(version1, version2);
        when(fileVersionManagementService.getAllVersions(file)).thenReturn(expectedVersions);

        // When
        List<FileVersion> actualVersions = fileVersionManagementService.getAllVersions(file);

        // Then
        assertNotNull(actualVersions, "Versions should not be null");
        assertEquals(2, actualVersions.size(), "Version count should match");
        assertTrue(actualVersions.contains(version1), "Version1 should be in the result");
        assertTrue(actualVersions.contains(version2), "Version2 should be in the result");
        verify(fileVersionManagementService, times(1)).getAllVersions(file);
    }

    @Test
    void testCreateVersion() {
        // Given
        File file = new File();
        String versionNumber = "v2";
        String deltaPath = "/delta/v2";
        FileVersion expectedVersion = new FileVersion();
        when(fileVersionManagementService.createVersion(file, versionNumber, deltaPath)).thenReturn(expectedVersion);

        // When
        FileVersion actualVersion = fileVersionManagementService.createVersion(file, versionNumber, deltaPath);

        // Then
        assertNotNull(actualVersion, "Created version should not be null");
        verify(fileVersionManagementService, times(1)).createVersion(file, versionNumber, deltaPath);
    }

    @Test
    void testGenerateNextVersion() {
        // Given
        File file = new File();
        String expectedVersionNumber = "v3";
        when(fileVersionManagementService.generateNextVersion(file)).thenReturn(expectedVersionNumber);

        // When
        String actualVersionNumber = fileVersionManagementService.generateNextVersion(file);

        // Then
        assertEquals(expectedVersionNumber, actualVersionNumber, "Generated version number should match");
        verify(fileVersionManagementService, times(1)).generateNextVersion(file);
    }

    @Test
    void testGetLatestContent() throws Exception {
        // Given
        File file = new File();
        User user = new User();
        String expectedContent = "Latest file content";
        when(fileVersionManagementService.getLatestContent(file, user)).thenReturn(expectedContent);

        // When
        String actualContent = fileVersionManagementService.getLatestContent(file, user);

        // Then
        assertEquals(expectedContent, actualContent, "Latest content should match");
        verify(fileVersionManagementService, times(1)).getLatestContent(file, user);
    }

    @Test
    void testReconstructFileContent() throws Exception {
        // Given
        File file = new File();
        String versionNumber = "v2";
        User user = new User();
        String expectedContent = "Reconstructed content";
        when(fileVersionManagementService.reconstructFileContent(file, versionNumber, user)).thenReturn(expectedContent);

        // When
        String actualContent = fileVersionManagementService.reconstructFileContent(file, versionNumber, user);

        // Then
        assertEquals(expectedContent, actualContent, "Reconstructed content should match");
        verify(fileVersionManagementService, times(1)).reconstructFileContent(file, versionNumber, user);
    }

    @Test
    void testGetVersionsUpTo() {
        // Given
        File file = new File();
        String versionNumber = "v2";
        FileVersion version1 = new FileVersion();
        FileVersion version2 = new FileVersion();
        List<FileVersion> expectedVersions = List.of(version1, version2);
        when(fileVersionManagementService.getVersionsUpTo(file, versionNumber)).thenReturn(expectedVersions);

        // When
        List<FileVersion> actualVersions = fileVersionManagementService.getVersionsUpTo(file, versionNumber);

        // Then
        assertNotNull(actualVersions, "Versions should not be null");
        assertEquals(2, actualVersions.size(), "Version count should match");
        assertTrue(actualVersions.contains(version1), "Version1 should be in the result");
        assertTrue(actualVersions.contains(version2), "Version2 should be in the result");
        verify(fileVersionManagementService, times(1)).getVersionsUpTo(file, versionNumber);
    }
}
