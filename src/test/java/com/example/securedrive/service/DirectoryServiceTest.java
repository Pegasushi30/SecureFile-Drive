package com.example.securedrive.service;

import com.example.securedrive.dto.DirectoryDto;
import com.example.securedrive.dto.DirectoryShareDto;
import com.example.securedrive.dto.DirectoryShareRequestDto;
import com.example.securedrive.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DirectoryServiceTest {
    private AutoCloseable closeable;
    @Mock
    private DirectoryService directoryService;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testShareDirectory() {
        // Given
        User owner = new User();
        DirectoryShareRequestDto dto = new DirectoryShareRequestDto(1L, "ownerUsername", "sharedUser@example.com");
        doNothing().when(directoryService).shareDirectory(dto, owner);

        // When
        directoryService.shareDirectory(dto, owner);

        // Then
        verify(directoryService, times(1)).shareDirectory(dto, owner);
    }

    @Test
    void testRevokeDirectoryShare() {
        // Given
        User owner = new User();
        Long directoryId = 1L;
        String sharedWithEmail = "sharedUser@example.com";
        doNothing().when(directoryService).revokeDirectoryShare(directoryId, sharedWithEmail, owner);

        // When
        directoryService.revokeDirectoryShare(directoryId, sharedWithEmail, owner);

        // Then
        verify(directoryService, times(1)).revokeDirectoryShare(directoryId, sharedWithEmail, owner);
    }

    @Test
    void testGetSharedDirectories() {
        // Given
        User user = new User();
        DirectoryShareDto shareDto = new DirectoryShareDto(
                1L,
                2L,
                "Shared Directory",
                "sharedUser@example.com",
                "owner@example.com",
                "/path/to/shared"
        );
        when(directoryService.getSharedDirectories(user)).thenReturn(List.of(shareDto));

        // When
        List<DirectoryShareDto> sharedDirectories = directoryService.getSharedDirectories(user);

        // Then
        assertNotNull(sharedDirectories, "Shared directories should not be null");
        assertEquals(1, sharedDirectories.size(), "Shared directories size should be 1");
        assertEquals("Shared Directory", sharedDirectories.get(0).directoryName(), "Shared directory name should match");
        assertEquals("/path/to/shared", sharedDirectories.get(0).sharedPath(), "Shared path should match");
        verify(directoryService, times(1)).getSharedDirectories(user);
    }

    @Test
    void testGetMySharedDirectories() {
        // Given
        User owner = new User();
        DirectoryShareDto shareDto = new DirectoryShareDto(
                1L,
                2L,
                "My Shared Directory",
                "sharedUser@example.com",
                "owner@example.com",
                "/path/to/my/shared"
        );
        when(directoryService.getMySharedDirectories(owner)).thenReturn(List.of(shareDto));

        // When
        List<DirectoryShareDto> mySharedDirectories = directoryService.getMySharedDirectories(owner);

        // Then
        assertNotNull(mySharedDirectories, "My shared directories should not be null");
        assertEquals(1, mySharedDirectories.size(), "My shared directories size should be 1");
        assertEquals("My Shared Directory", mySharedDirectories.get(0).directoryName(), "My shared directory name should match");
        assertEquals("/path/to/my/shared", mySharedDirectories.get(0).sharedPath(), "Shared path should match");
        verify(directoryService, times(1)).getMySharedDirectories(owner);
    }
}
