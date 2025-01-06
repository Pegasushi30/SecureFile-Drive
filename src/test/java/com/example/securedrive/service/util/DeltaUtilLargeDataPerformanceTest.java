package com.example.securedrive.service.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class DeltaUtilLargeDataPerformanceTest {

    @Test
    void testCalculateDelta_LargeModification_WithThreads() throws InterruptedException, ExecutionException {
        byte[] original = new byte[10_000_000]; // 10MB
        byte[] modified = new byte[10_000_000];

        // Simulate large modifications
        IntStream.range(0, original.length).forEach(i -> original[i] = (byte) (i % 256));
        IntStream.range(0, modified.length).forEach(i -> modified[i] = (byte) ((i + 1) % 256));

        ExecutorService executor = Executors.newFixedThreadPool(4); // 4 threads
        Callable<List<BinaryDeltaUtil.DeltaCommand>> task = () -> BinaryDeltaUtil.calculateDelta(original, modified);

        Future<List<BinaryDeltaUtil.DeltaCommand>> future = executor.submit(task);

        // Wait for the delta computation to complete
        List<BinaryDeltaUtil.DeltaCommand> delta = future.get();

        executor.shutdown();

        System.out.println("Generated delta commands: " + delta.size());
        assertNotNull(delta);
        assertTrue(delta.size() > 0);
    }

    @Test
    void testApplyDelta_LargeFile_WithThreads() throws InterruptedException, ExecutionException {
        byte[] original = new byte[10_000_000]; // 10MB
        byte[] modified = new byte[10_000_000];

        // Simulate large modifications
        IntStream.range(0, original.length).forEach(i -> original[i] = (byte) (i % 256));
        IntStream.range(0, modified.length).forEach(i -> modified[i] = (byte) ((i + 1) % 256));

        // Calculate delta
        List<BinaryDeltaUtil.DeltaCommand> delta = BinaryDeltaUtil.calculateDelta(original, modified);

        ExecutorService executor = Executors.newFixedThreadPool(4); // 4 threads
        Callable<byte[]> task = () -> BinaryDeltaUtil.applyDelta(original, delta);

        Future<byte[]> future = executor.submit(task);

        // Wait for the delta application to complete
        byte[] reconstructed = future.get();

        executor.shutdown();

        System.out.println("Reconstructed data matches modified: " + (reconstructed.length == modified.length));
        assertArrayEquals(modified, reconstructed);
    }

    @Test
    void testErrorHandling_LargeFile_InvalidCopyCommand() {
        byte[] original = new byte[10_000_000]; // 10MB
        IntStream.range(0, original.length).forEach(i -> original[i] = (byte) (i % 256));

        List<BinaryDeltaUtil.DeltaCommand> delta = new ArrayList<>();
        delta.add(new BinaryDeltaUtil.DeltaCommand(BinaryDeltaUtil.CommandType.COPY, 0, 20_000_000, null)); // Invalid range

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            BinaryDeltaUtil.applyDelta(original, delta);
        });

        assertTrue(exception.getMessage().contains("Invalid COPY command"));
    }

    @Test
    void testMemoryUsage_LargeFiles() {
        byte[] original = new byte[50_000_000]; // 50MB
        byte[] modified = new byte[50_000_000];

        // Simulate large modifications
        IntStream.range(0, original.length).forEach(i -> original[i] = (byte) (i % 256));
        IntStream.range(0, modified.length).forEach(i -> modified[i] = (byte) ((i + 1) % 256));

        Runtime runtime = Runtime.getRuntime();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

        List<BinaryDeltaUtil.DeltaCommand> delta = BinaryDeltaUtil.calculateDelta(original, modified);

        long afterMemory = runtime.totalMemory() - runtime.freeMemory();

        System.out.println("Memory used for delta computation: " + (afterMemory - beforeMemory) + " bytes");
        assertNotNull(delta);
        assertTrue(delta.size() > 0);
        assertTrue((afterMemory - beforeMemory) < 500_000_000); // Ensure memory usage is under 500MB
    }
}
