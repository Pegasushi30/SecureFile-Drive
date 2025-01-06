package com.example.securedrive.service;

import com.example.securedrive.dto.FileDto;
import com.example.securedrive.model.Directory;
import com.example.securedrive.model.File;
import com.example.securedrive.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileManagementServiceTest {
    private AutoCloseable closeable;
    @Mock
    private FileManagementService fileManagementService;

    @BeforeEach
    void setUp() {
        closeable= MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }
    @Test
    void testGetFilesByUser() {
        // Given
        User user = new User();
        File file1 = new File();
        File file2 = new File();
        List<File> expectedFiles = List.of(file1, file2);
        when(fileManagementService.getFilesByUser(user)).thenReturn(expectedFiles);

        // When
        List<File> actualFiles = fileManagementService.getFilesByUser(user);

        // Then
        assertNotNull(actualFiles);
        assertEquals(2, actualFiles.size());
        verify(fileManagementService, times(1)).getFilesByUser(user);
    }

    @Test
    void testSaveFile() {
        // Given
        File file = new File();
        doNothing().when(fileManagementService).saveFile(file);

        // When
        fileManagementService.saveFile(file);

        // Then
        verify(fileManagementService, times(1)).saveFile(file);
    }

    @Test
    void testFindById() {
        // Given
        Long fileId = 1L;
        File file = new File();
        when(fileManagementService.findById(fileId)).thenReturn(Optional.of(file));

        // When
        Optional<File> result = fileManagementService.findById(fileId);

        // Then
        assertTrue(result.isPresent());
        verify(fileManagementService, times(1)).findById(fileId);
    }

    @Test
    void testFindByIdAndUser() {
        // Given
        Long fileId = 1L;
        User user = new User();
        File file = new File();
        when(fileManagementService.findByIdAndUser(fileId, user)).thenReturn(Optional.of(file));

        // When
        Optional<File> result = fileManagementService.findByIdAndUser(fileId, user);

        // Then
        assertTrue(result.isPresent());
        verify(fileManagementService, times(1)).findByIdAndUser(fileId, user);
    }

    @Test
    void testFindByFileNameAndUserDirectoryNull() {
        // Given
        String fileName = "testFile.txt";
        User user = new User();
        File file = new File();
        when(fileManagementService.findByFileNameAndUserDirectoryNull(fileName, user)).thenReturn(file);

        // When
        File result = fileManagementService.findByFileNameAndUserDirectoryNull(fileName, user);

        // Then
        assertNotNull(result);
        verify(fileManagementService, times(1)).findByFileNameAndUserDirectoryNull(fileName, user);
    }

    @Test
    void testFindByFileNameUserAndDirectory() {
        // Given
        String fileName = "testFile.txt";
        User user = new User();
        Directory directory = new Directory();
        File file = new File();
        when(fileManagementService.findByFileNameUserAndDirectory(fileName, user, directory)).thenReturn(file);

        // When
        File result = fileManagementService.findByFileNameUserAndDirectory(fileName, user, directory);

        // Then
        assertNotNull(result);
        verify(fileManagementService, times(1)).findByFileNameUserAndDirectory(fileName, user, directory);
    }

    @Test
    void testGetUserFilesInRootDirectory() {
        // Given
        User user = new User();
        File file1 = new File();
        File file2 = new File();
        List<File> expectedFiles = List.of(file1, file2);
        when(fileManagementService.getUserFilesInRootDirectory(user)).thenReturn(expectedFiles);

        // When
        List<File> actualFiles = fileManagementService.getUserFilesInRootDirectory(user);

        // Then
        assertNotNull(actualFiles);
        assertEquals(2, actualFiles.size());
        verify(fileManagementService, times(1)).getUserFilesInRootDirectory(user);
    }

    @Test
    void testShareFileWithUser() {
        // Given
        File file = new File();
        User owner = new User();
        User sharedWithUser = new User();
        String versionNumber = "v1";
        doNothing().when(fileManagementService).shareFileWithUser(file, owner, sharedWithUser, versionNumber);

        // When
        fileManagementService.shareFileWithUser(file, owner, sharedWithUser, versionNumber);

        // Then
        verify(fileManagementService, times(1)).shareFileWithUser(file, owner, sharedWithUser, versionNumber);
    }

    @Test
    void testGetFilesByUsername() {
        // Given
        String username = "testuser";
        List<FileDto> expectedFiles = getFileDtos();
        when(fileManagementService.getFilesByUsername(username)).thenReturn(expectedFiles);

        // When
        List<FileDto> actualFiles = fileManagementService.getFilesByUsername(username);

        // Then
        assertNotNull(actualFiles, "The returned files list should not be null");
        assertEquals(2, actualFiles.size(), "The size of the returned files list should match");
        assertEquals(expectedFiles, actualFiles, "The returned files list should match the expected list");
        verify(fileManagementService, times(1)).getFilesByUsername(username);
    }

    private static List<FileDto> getFileDtos() {
        FileDto file1 = new FileDto(
                1L,
                "file1.txt",
                "/path/to/file1.txt",
                "testuser",
                10L,
                List.of(),
                List.of(),
                "[]");
        FileDto file2 = new FileDto(
                2L,
                "file2.pdf",
                "/path/to/file2.pdf",
                "testuser",
                20L,
                List.of(),
                List.of(),
                "[]");
        return List.of(file1, file2);
    }


    @Test
    void testGetTotalStorage() {
        // Given
        String username = "testuser";
        long expectedStorage = 1024L;
        when(fileManagementService.getTotalStorage(username)).thenReturn(expectedStorage);

        // When
        long actualStorage = fileManagementService.getTotalStorage(username);

        // Then
        assertEquals(expectedStorage, actualStorage);
        verify(fileManagementService, times(1)).getTotalStorage(username);
    }

    @Test
    void testGetUsedStorage() {
        // Given
        String username = "testuser";
        long expectedUsedStorage = 512L;
        when(fileManagementService.getUsedStorage(username)).thenReturn(expectedUsedStorage);

        // When
        long actualUsedStorage = fileManagementService.getUsedStorage(username);

        // Then
        assertEquals(expectedUsedStorage, actualUsedStorage);
        verify(fileManagementService, times(1)).getUsedStorage(username);
    }
}
