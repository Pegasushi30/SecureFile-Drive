package com.example.securedrive.service.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class HashUtilTest {

    @Test
    public void testCalculateHash_HelloWorld_Debug() {
        // Arrange
        String input = "hello world";
        String expectedHash = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9";

        // Debug: Byte'ları yazdır
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        System.out.println("Bytes length: " + inputBytes.length);
        for (byte b : inputBytes) {
            System.out.printf("%02x ", b);
        }
        System.out.println();

        // Act
        String actualHash = HashUtil.calculateHash(inputBytes);

        // Assert
        Assertions.assertEquals(expectedHash, actualHash, "SHA-256 hash mismatch for input 'hello world'");
    }


    @Test
    public void testCalculateHash_EmptyString() {
        // Arrange
        String input = "";
        // Boş stringin SHA-256 değeri (64 karakterlik hex):
        String expectedHash = "e3b0c44298fc1c149afbf4c8996fb924" +
                "27ae41e4649b934ca495991b7852b855";

        // Act
        String actualHash = HashUtil.calculateHash(input.getBytes(StandardCharsets.UTF_8));

        // Assert
        Assertions.assertEquals(expectedHash, actualHash);
    }
}
