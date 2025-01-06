package com.example.securedrive.service;

import com.example.securedrive.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserManagementServiceTest {
    private AutoCloseable closeable;
    @Mock
    private UserManagementService userManagementService;



    @BeforeEach
    void setUp() {
        closeable =   MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close(); // AutoCloseable nesneyi kapatır
    }

    @Test
    void testFindByUsername() {
        // Given
        String username = "testuser";
        User user = new User();
        user.setUsername(username);
        when(userManagementService.findByUsername(username)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userManagementService.findByUsername(username);

        // Then
        assertTrue(result.isPresent(), "User should be found");
        assertEquals(username, result.get().getUsername(), "Username should match");
        verify(userManagementService, times(1)).findByUsername(username);
    }

    @Test
    void testFindByEmail() {
        // Given
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        when(userManagementService.findByEmail(email)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userManagementService.findByEmail(email);

        // Then
        assertTrue(result.isPresent(), "User should be found by email");
        assertEquals(email, result.get().getEmail(), "Email should match");
        verify(userManagementService, times(1)).findByEmail(email);
    }

    @Test
    void testGetContactsForUser() {
        // Given
        String username = "testuser";
        User contact1 = new User();
        contact1.setId(1L); // Benzersiz ID
        contact1.setUsername("contact1");
        User contact2 = new User();
        contact2.setId(2L); // Benzersiz ID
        contact2.setUsername("contact2");
        Set<User> contacts = Set.of(contact1, contact2);
        when(userManagementService.getContactsForUser(username)).thenReturn(contacts);

        // When
        Set<User> result = userManagementService.getContactsForUser(username);

        // Then
        assertNotNull(result, "Contacts should not be null");
        assertEquals(2, result.size(), "Contact size should match");
        assertTrue(result.contains(contact1), "Contact1 should be in the result");
        assertTrue(result.contains(contact2), "Contact2 should be in the result");
        verify(userManagementService, times(1)).getContactsForUser(username);
    }

}
