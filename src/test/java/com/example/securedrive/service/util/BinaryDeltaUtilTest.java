package com.example.securedrive.service.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BinaryDeltaUtilTest {

    @Test
    void testIdenticalFiles() {
        byte[] original = "This is a test file content.".getBytes();
        byte[] modified = "This is a test file content.".getBytes();

        List<BinaryDeltaUtil.DeltaCommand> deltaCommands = BinaryDeltaUtil.calculateDelta(original, modified);

        assertNotNull(deltaCommands);
        assertEquals(1, deltaCommands.size());
        BinaryDeltaUtil.DeltaCommand command = deltaCommands.get(0);
        assertEquals(BinaryDeltaUtil.CommandType.COPY, command.type());
        assertEquals(0, command.offset());
        assertEquals(original.length, command.length());
        assertNull(command.data());
    }

    @Test
    void testCalculateDeltaWithDifferences() {
        byte[] original = "This is a test file content.".getBytes();
        byte[] modified = "This is a new test file content.".getBytes();

        List<BinaryDeltaUtil.DeltaCommand> deltaCommands = BinaryDeltaUtil.calculateDelta(original, modified);

        assertNotNull(deltaCommands);
        assertFalse(deltaCommands.isEmpty());
        assertTrue(deltaCommands.stream().anyMatch(cmd -> cmd.type() == BinaryDeltaUtil.CommandType.LITERAL));
    }

    @Test
    void testApplyDelta() {
        byte[] original = "This is a test file content.".getBytes();
        byte[] modified = "This is a new test file content.".getBytes();

        List<BinaryDeltaUtil.DeltaCommand> deltaCommands = BinaryDeltaUtil.calculateDelta(original, modified);

        // Log delta commands for debugging
        System.out.println("Delta Commands:");
        for (BinaryDeltaUtil.DeltaCommand cmd : deltaCommands) {
            System.out.println(cmd);
        }

        byte[] reconstructed = BinaryDeltaUtil.applyDelta(original, deltaCommands);

        // Log reconstructed data for debugging
        System.out.println("Reconstructed Data: " + new String(reconstructed));

        assertNotNull(reconstructed, "Reconstructed data should not be null");
        assertArrayEquals(modified, reconstructed, "Reconstructed data does not match modified data");
    }


    @Test
    void testInvalidDeltaCommand() {
        byte[] original = "This is a test file content.".getBytes();
        byte[] modified = "This is a new test file content.".getBytes();

        List<BinaryDeltaUtil.DeltaCommand> deltaCommands = BinaryDeltaUtil.calculateDelta(original, modified);

        BinaryDeltaUtil.DeltaCommand invalidCommand = new BinaryDeltaUtil.DeltaCommand(
                BinaryDeltaUtil.CommandType.COPY, 999, 10, null
        );
        deltaCommands.add(invalidCommand);

        assertThrows(IllegalArgumentException.class, () -> BinaryDeltaUtil.applyDelta(original, deltaCommands));
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

        deltaCommands.forEach(cmd ->
                System.out.println("Command: " + cmd.type() +
                        ", Offset: " + cmd.offset() +
                        ", Length: " + cmd.length() +
                        ", Data: " + Arrays.toString(cmd.data()))
        );

        long copyCommands = deltaCommands.stream()
                .filter(cmd -> cmd.type() == BinaryDeltaUtil.CommandType.COPY)
                .count();

        long literalCommands = deltaCommands.stream()
                .filter(cmd -> cmd.type() == BinaryDeltaUtil.CommandType.LITERAL)
                .count();

        assertTrue(copyCommands >= 1, "Expected at least one COPY command, but none were found");
        assertTrue(literalCommands >= 1, "Expected at least one LITERAL command, but none were found");
        assertTrue(deltaCommands.size() <= 3, "Delta command count exceeds expected size");

        boolean hasValidCopyCommand = deltaCommands.stream()
                .filter(cmd -> cmd.type() == BinaryDeltaUtil.CommandType.COPY)
                .anyMatch(cmd -> {
                    int offset = cmd.offset();
                    int length = cmd.length();
                    return (offset >= 0 && offset + length <= original.length);
                });

        assertTrue(hasValidCopyCommand, "No valid COPY command references a correct offset/length in the original data");
    }




}
