package com.example.securedrive.service;

import com.example.securedrive.exception.AzureBlobStorageException;
import com.example.securedrive.model.Storage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AzureBlobStorageTest {
    private AutoCloseable closeable;
    @Mock
    private IAzureBlobStorage azureBlobStorage;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testWrite() throws AzureBlobStorageException {
        // Given
        Storage storage = new Storage("path/to/file", new byte[]{1, 2, 3});
        doNothing().when(azureBlobStorage).write(storage);

        // When
        azureBlobStorage.write(storage);

        // Then
        verify(azureBlobStorage, times(1)).write(storage);
    }

    @Test
    void testRead() throws AzureBlobStorageException {
        // Given
        Storage storage = new Storage("path/to/file", null);
        byte[] expectedData = new byte[]{1, 2, 3};
        when(azureBlobStorage.read(storage)).thenReturn(expectedData);

        // When
        byte[] actualData = azureBlobStorage.read(storage);

        // Then
        assertNotNull(actualData, "Data should not be null");
        assertArrayEquals(expectedData, actualData, "Data should match expected bytes");
        verify(azureBlobStorage, times(1)).read(storage);
    }

    @Test
    void testDelete() throws AzureBlobStorageException {
        // Given
        Storage storage = new Storage("path/to/file", null);
        doNothing().when(azureBlobStorage).delete(storage);

        // When
        azureBlobStorage.delete(storage);

        // Then
        verify(azureBlobStorage, times(1)).delete(storage);
    }

    @Test
    void testExists() throws AzureBlobStorageException {
        // Given
        Storage storage = new Storage("path/to/file", null);
        when(azureBlobStorage.exists(storage)).thenReturn(true);

        // When
        boolean exists = azureBlobStorage.exists(storage);

        // Then
        assertTrue(exists, "File should exist");
        verify(azureBlobStorage, times(1)).exists(storage);
    }

    @Test
    void testCreateDirectory() throws AzureBlobStorageException {
        // Given
        String directoryPath = "path/to/directory";
        doNothing().when(azureBlobStorage).createDirectory(directoryPath);

        // When
        azureBlobStorage.createDirectory(directoryPath);

        // Then
        verify(azureBlobStorage, times(1)).createDirectory(directoryPath);
    }

    @Test
    void testWriteThrowsException() throws AzureBlobStorageException {
        // Given
        Storage storage = new Storage("invalid/path", new byte[]{1, 2, 3});
        doThrow(new AzureBlobStorageException("Write failed")).when(azureBlobStorage).write(storage);

        // When
        AzureBlobStorageException exception = assertThrows(AzureBlobStorageException.class, () -> {
            azureBlobStorage.write(storage);
        });

        // Then
        assertEquals("Write failed", exception.getMessage());
        verify(azureBlobStorage, times(1)).write(storage);
    }
}
