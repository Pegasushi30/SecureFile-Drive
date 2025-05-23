package com.example.securedrive.service.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test class for BinaryDeltaUtil.
 */
class BinaryDeltaUtilTest {

    @Test
    void testIdenticalFiles() {
        // Given
        byte[] original = "This is a test file content.".getBytes();
        byte[] modified = "This is a test file content.".getBytes();

        // When
        List<BinaryDeltaUtil.DeltaCommand> deltaCommands = BinaryDeltaUtil.calculateDelta(original, modified);

        // Then
        assertNotNull(deltaCommands, "Delta commands should not be null");
        assertEquals(1, deltaCommands.size(), "There should be exactly one delta command for identical files");
        BinaryDeltaUtil.DeltaCommand command = deltaCommands.get(0);
        assertEquals(BinaryDeltaUtil.CommandType.COPY, command.type(), "Command type should be COPY");
        assertEquals(0, command.offset(), "COPY command offset should be 0");
        assertEquals(original.length, command.length(), "COPY command length should match original length");
        assertNull(command.data(), "COPY command should not contain data");
    }

    @Test
    void testCalculateDeltaWithDifferences() {
        // Given
        byte[] original = "This is a test file content.".getBytes();
        byte[] modified = "This is a new test file content.".getBytes();

        // When
        List<BinaryDeltaUtil.DeltaCommand> deltaCommands = BinaryDeltaUtil.calculateDelta(original, modified);

        // Then
        assertNotNull(deltaCommands, "Delta commands should not be null");
        assertFalse(deltaCommands.isEmpty(), "Delta commands should not be empty");
        assertTrue(deltaCommands.stream().anyMatch(cmd -> cmd.type() == BinaryDeltaUtil.CommandType.LITERAL),
                "Delta commands should contain at least one LITERAL command");
    }

    @Test
    void testApplyDelta() {
        // Given
        byte[] original = "This is a test file content.".getBytes();
        byte[] modified = "This is a new test file content.".getBytes();

        // When
        List<BinaryDeltaUtil.DeltaCommand> deltaCommands = BinaryDeltaUtil.calculateDelta(original, modified);
        System.out.println("Delta Commands:");
        for (BinaryDeltaUtil.DeltaCommand cmd : deltaCommands) {
            System.out.println(cmd);
        }
        byte[] reconstructed = BinaryDeltaUtil.applyDelta(original, deltaCommands);
        System.out.println("Reconstructed Data: " + new String(reconstructed));

        // Then
        assertNotNull(reconstructed, "Reconstructed data should not be null");
        assertArrayEquals(modified, reconstructed, "Reconstructed data does not match modified data");
    }

    @Test
    void testInvalidDeltaCommand() {
        // Given
        byte[] original = "This is a test file content.".getBytes();
        byte[] modified = "This is a new test file content.".getBytes();
        List<BinaryDeltaUtil.DeltaCommand> deltaCommands = BinaryDeltaUtil.calculateDelta(original, modified);
        BinaryDeltaUtil.DeltaCommand invalidCommand = new BinaryDeltaUtil.DeltaCommand(
                BinaryDeltaUtil.CommandType.COPY, 999, 10, null
        );
        deltaCommands.add(invalidCommand);

        // When-Then
        assertThrows(IllegalArgumentException.class, () -> BinaryDeltaUtil.applyDelta(original, deltaCommands),
                "Applying invalid COPY command should throw IllegalArgumentException");
    }

    @Test
    void testMergeDeltaCommands() {
        // Given
        byte[] original = "ABCDEFGHIJKLMNOP".getBytes();
        byte[] modified = "ABCXEFGHIJKLMNOP".getBytes();

        // When
        List<BinaryDeltaUtil.DeltaCommand> deltaCommands = BinaryDeltaUtil.calculateDelta(original, modified);

        // Then
        assertNotNull(deltaCommands, "Delta commands list should not be null");
        assertFalse(deltaCommands.isEmpty(), "Delta commands should not be empty");

        deltaCommands.forEach(cmd ->
                System.out.println("Command: " + cmd.type() +
                        ", Offset: " + cmd.offset() +
                        ", Length: " + cmd.length() +
                        ", Data: " + (cmd.data() != null ? new String(cmd.data()) : "null"))
        );

        long copyCommands = deltaCommands.stream()
                .filter(cmd -> cmd.type() == BinaryDeltaUtil.CommandType.COPY)
                .count();

        long literalCommands = deltaCommands.stream()
                .filter(cmd -> cmd.type() == BinaryDeltaUtil.CommandType.LITERAL)
                .count();

        if (!Arrays.equals(original, modified)) {
            assertTrue(copyCommands >= 1, "Expected at least one COPY command, but none were found");
            assertTrue(literalCommands >= 1, "Expected at least one LITERAL command, but none were found");
        } else {
            assertEquals(1, copyCommands, "Expected exactly one COPY command for identical files");
            assertEquals(0, literalCommands, "Expected no LITERAL commands for identical files");
        }
    }

}
