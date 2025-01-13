package com.example.securedrive.service.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class DeltaUtilLargeDataPerformanceTest {

    private static final int FILE_SIZE = 50_000_000;

    @Test
    void testCalculateDelta_LargeModification_WithThreads() throws InterruptedException, ExecutionException {
        // Given
        byte[] original = createLargeData(0);
        byte[] modified = createLargeData(1);

        for (int threadCount : new int[]{1, 2, 4, 8}) {
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // When
            Callable<List<BinaryDeltaUtil.DeltaCommand>> task = () -> BinaryDeltaUtil.calculateDelta(original, modified);
            Future<List<BinaryDeltaUtil.DeltaCommand>> future = executor.submit(task);
            List<BinaryDeltaUtil.DeltaCommand> delta = future.get();

            executor.shutdown();

            // Then
            System.out.println("Thread Count: " + threadCount + " | Generated delta commands: " + delta.size());
            assertNotNull(delta, "Delta should not be null");
            assertFalse(delta.isEmpty(), "Delta should not be empty");
        }
    }

    @Test
    void testApplyDelta_LargeFile_WithThreads() throws InterruptedException, ExecutionException {
        // Given
        byte[] original = createLargeData(0);
        byte[] modified = createLargeData(1);

        List<BinaryDeltaUtil.DeltaCommand> delta = BinaryDeltaUtil.calculateDelta(original, modified);

        for (int threadCount : new int[]{1, 2, 4, 8}) {
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // When
            Callable<byte[]> task = () -> BinaryDeltaUtil.applyDelta(original, delta);
            Future<byte[]> future = executor.submit(task);
            byte[] reconstructed = future.get();

            executor.shutdown();

            // Then
            System.out.println("Thread Count: " + threadCount + " | Reconstructed data matches modified file: " + (reconstructed.length == modified.length));
            assertArrayEquals(modified, reconstructed, "Reconstructed file should match the modified file");
        }
    }

    @Test
    void testErrorHandling_LargeFile_InvalidCopyCommand() {
        // Given
        byte[] original = createLargeData(0);

        List<BinaryDeltaUtil.DeltaCommand> delta = new ArrayList<>();
        delta.add(new BinaryDeltaUtil.DeltaCommand(BinaryDeltaUtil.CommandType.COPY, 40_000_000, 20_000_000, null)); // Invalid range

        // When/Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> BinaryDeltaUtil.applyDelta(original, delta));
        assertTrue(exception.getMessage().contains("Invalid COPY command"), "Exception should indicate invalid COPY command");
    }

    @Test
    void testMemoryUsage_LargeFiles() {
        // Given
        byte[] original = createLargeData(0);
        byte[] modified = createLargeData(1);

        for (int threadCount : new int[]{1, 2, 4, 8}) {
            Runtime runtime = Runtime.getRuntime();
            long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // When
            Callable<List<BinaryDeltaUtil.DeltaCommand>> task = () -> BinaryDeltaUtil.calculateDelta(original, modified);
            Future<List<BinaryDeltaUtil.DeltaCommand>> future;
            try {
                future = executor.submit(task);
                future.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                executor.shutdown();
            }

            long afterMemory = runtime.totalMemory() - runtime.freeMemory();

            // Then
            long memoryUsed = afterMemory - beforeMemory;
            System.out.println("Thread Count: " + threadCount + " | Memory used for delta computation: " + memoryUsed + " bytes");
        }
    }

    private byte[] createLargeData(int offset) {
        byte[] data = new byte[DeltaUtilLargeDataPerformanceTest.FILE_SIZE];
        IntStream.range(0, DeltaUtilLargeDataPerformanceTest.FILE_SIZE).forEach(i -> data[i] = (byte) ((i + offset) % 256));
        return data;
    }
}
