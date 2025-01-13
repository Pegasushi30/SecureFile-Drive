package com.example.securedrive.service.util;

import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AESUtilTest {

    @Test
    void testGenerateAESKey() throws NoSuchAlgorithmException {
        // Given
        int expectedKeyLength = 32;

        // When
        String aesKey = AESUtil.generateAESKey();

        // Then
        assertNotNull(aesKey, "Generated key should not be null");
        byte[] decodedKey = Base64.getDecoder().decode(aesKey);
        assertEquals(expectedKeyLength, decodedKey.length, "Generated key should be 256 bits (32 bytes)");
    }

    @Test
    void testEncryptAndDecrypt() throws Exception {
        // Given
        String key = AESUtil.generateAESKey();
        String originalText = "This is a test message.";
        byte[] originalData = originalText.getBytes();

        // When
        byte[] encryptedData = AESUtil.encrypt(originalData, key);
        byte[] decryptedData = AESUtil.decrypt(encryptedData, key);

        // Then
        assertNotNull(encryptedData, "Encrypted data should not be null");
        assertNotNull(decryptedData, "Decrypted data should not be null");
        assertArrayEquals(originalData, decryptedData, "Decrypted data should match the original data");
        assertEquals(originalText, new String(decryptedData), "Decrypted text should match the original text");
    }

    @Test
    void testDecryptWithInvalidKey() {
        try {
            // Given
            String validKey = AESUtil.generateAESKey();
            String invalidKey = AESUtil.generateAESKey();
            String originalText = "This is a test message.";
            byte[] originalData = originalText.getBytes();

            // When
            byte[] encryptedData = AESUtil.encrypt(originalData, validKey);

            // Then
            assertThrows(Exception.class, () -> AESUtil.decrypt(encryptedData, invalidKey),
                    "Decrypting with an invalid key should throw an exception");
        } catch (Exception e) {
            fail("Unexpected exception occurred: " + e.getMessage());
        }
    }

    @Test
    void testEncryptWithInvalidKey() {
        // Given
        String invalidKey = "shortkey";
        String originalText = "This is a test message.";
        byte[] originalData = originalText.getBytes();

        // Then
        assertThrows(Exception.class, () -> AESUtil.encrypt(originalData, invalidKey),
                "Encrypting with an invalid key should throw an exception");
    }
}

