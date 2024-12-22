package com.example.securedrive.util;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.CRC32;

/**
 * Advanced Delta Utility (Final Version):
 * ----------------------------------------------------
 * 1) Dynamic block size (based on file size)
 * 2) Rolling Hash (CRC32) + strong hash (SHA-256) for "coarse matching"
 * 3) Suffix Array (SA-IS) for "fine" matching (longest match)
 * 4) Merge consecutive delta commands
 * 5) Use built-in java.util.zip.CRC32
 * 6) Proper identical file handling
 */
public class BinaryDeltaUtil {

    /** Maximum block size (e.g., 64 KB). */
    private static final int MAX_BLOCK_SIZE = 64 * 1024;

    /** Strong checksum algorithm: SHA-256. */
    private static final String STRONG_HASH_ALGO = "SHA-256";

    /** Delta command types: COPY or LITERAL. */
    public enum CommandType {
        COPY,
        LITERAL
    }

    /**
     * Delta command (record):
     * type (COPY/LITERAL), offset (position in original file), length, data (only for LITERAL).
     *
     * @param data Only for LITERAL
     */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record DeltaCommand(CommandType type, int offset, int length, byte[] data) {

        @Override
            public String toString() {
                return "DeltaCommand{" +
                        "type=" + type +
                        ", offset=" + offset +
                        ", length=" + length +
                        ", data=" + (data == null ? "null" : "[... " + data.length + " bytes ...]") +
                        '}';
            }
        }

    /**
     * Determine dynamic block size based on file size.
     * - <1 MB => 512 B
     * - <10 MB => 2 KB
     * - <100 MB => 8 KB
     * - >=100 MB => 64 KB
     */
    private static int determineBlockSize(long fileSize) {
        if (fileSize < 1_000_000) {          // < 1 MB
            return 512;
        } else if (fileSize < 10_000_000) {  // < 10 MB
            return 2 * 1024;
        } else if (fileSize < 100_000_000) { // < 100 MB
            return 8 * 1024;
        } else {
            return MAX_BLOCK_SIZE;           // >= 100 MB
        }
    }

    /**
     * Main function: Calculate delta.
     * 1) Rolling-hash-based coarse diff
     * 2) Suffix Array (SA-IS) for fine matching
     * 3) Merge delta commands
     * Important Addition:
     * If original and modified byte arrays are identical,
     * return a single COPY command for faster processing.
     */
    public static List<DeltaCommand> calculateDelta(byte[] original, byte[] modified) {
        long startTime = System.nanoTime();

        // 0) Pre-check: Are files identical?
        if (original.length == modified.length) {
            byte[] origHash = strongHash(original);
            byte[] modHash = strongHash(modified);
            boolean hashesMatch = Arrays.equals(origHash, modHash);
            System.out.println("Original SHA-256: " + bytesToHex(origHash));
            System.out.println("Modified SHA-256: " + bytesToHex(modHash));
            if (hashesMatch) {
                // Single COPY command
                List<DeltaCommand> singleCommand = new ArrayList<>();
                singleCommand.add(new DeltaCommand(CommandType.COPY, 0, original.length, null));
                System.out.println("Files are identical. Returning single COPY command...");
                double ms = (System.nanoTime() - startTime) / 1_000_000.0;
                System.out.printf("Delta calculation took %.2f ms%n", ms);
                return singleCommand;
            }
        }

        // 1) Coarse matching (CRC32 + SHA-256)
        List<DeltaCommand> coarseDelta = blockBasedDiff(original, modified);

        // 2) Suffix Array (SA-IS) for fine matching
        SuffixArray suffixArray = new SuffixArray(original);
        List<DeltaCommand> fineDelta = refineWithSuffixArray(suffixArray, coarseDelta);

        // 3) Merge delta commands
        List<DeltaCommand> merged = mergeCommands(fineDelta);

        double ms = (System.nanoTime() - startTime) / 1_000_000.0;
        System.out.printf("Delta calculation took %.2f ms%n", ms);

        return merged;
    }

    /**
     * Coarse matching using CRC32 and SHA-256 block-based indexing.
     *
     * @param original Original file data
     * @param modified Modified file data
     * @return List of DeltaCommand (coarse)
     */
    private static List<DeltaCommand> blockBasedDiff(byte[] original, byte[] modified) {
        int blockSize = determineBlockSize(original.length);

        // Index the original file with CRC32
        Map<Long, List<Integer>> originalMap = buildIndex(original, blockSize);

        List<DeltaCommand> commands = new ArrayList<>();
        int mPos = 0;
        int literalStart = 0;

        while (mPos < modified.length) {
            int size = Math.min(blockSize, modified.length - mPos);
            byte[] window = slice(modified, mPos, size);

            long crc = crc32(window);
            List<Integer> candidates = originalMap.get(crc);

            int matchedIndex = -1;
            int matchedLength = 0;

            // Candidates verification with SHA-256
            if (candidates != null) {
                byte[] strongWin = strongHash(window);
                for (int cPos : candidates) {
                    if (cPos + size <= original.length) {
                        byte[] origBlock = slice(original, cPos, size);
                        byte[] strongOrig = strongHash(origBlock);
                        if (Arrays.equals(strongWin, strongOrig)) {
                            matchedIndex = cPos;
                            matchedLength = size;
                            break;
                        }
                    }
                }
            }

            if (matchedIndex >= 0) {
                // Add accumulated LITERAL if any
                if (mPos > literalStart) {
                    int litLen = mPos - literalStart;
                    byte[] litData = slice(modified, literalStart, litLen);
                    commands.add(new DeltaCommand(CommandType.LITERAL, 0, litLen, litData));
                }
                // Add COPY command
                commands.add(new DeltaCommand(CommandType.COPY, matchedIndex, matchedLength, null));

                // Move forward
                mPos += matchedLength;
                literalStart = mPos;
            } else {
                // No match, move forward by 1
                mPos++;
            }
        }

        // Add remaining LITERAL if any
        if (literalStart < modified.length) {
            int len = modified.length - literalStart;
            byte[] litData = slice(modified, literalStart, len);
            commands.add(new DeltaCommand(CommandType.LITERAL, 0, len, litData));
        }

        return commands;
    }

    /**
     * Build index for the original file using CRC32.
     *
     * @param original  Original file data
     * @param blockSize Block size
     * @return Map from CRC32 to list of block starting indices
     */
    private static Map<Long, List<Integer>> buildIndex(byte[] original, int blockSize) {
        Map<Long, List<Integer>> map = new HashMap<>();
        int pos = 0;
        while (pos < original.length) {
            int len = Math.min(blockSize, original.length - pos);
            byte[] block = slice(original, pos, len);
            long crc = crc32(block);
            map.computeIfAbsent(crc, k -> new ArrayList<>()).add(pos);
            pos += len;
        }
        return map;
    }

    /**
     * Refine coarse delta commands with Suffix Array to find longer matches.
     *
     * @param suffixArray Suffix Array built from original file
     * @param coarseDelta Coarse delta commands
     * @return List of refined DeltaCommand
     */
    private static List<DeltaCommand> refineWithSuffixArray(SuffixArray suffixArray, List<DeltaCommand> coarseDelta) {
        List<DeltaCommand> refined = new ArrayList<>();

        for (DeltaCommand cmd : coarseDelta) {
            if (cmd.type() == CommandType.COPY) {
                refined.add(cmd);
            } else {
                // LITERAL: try to find longer matches using suffix array
                byte[] data = cmd.data();
                int localPos = 0;

                while (localPos < data.length) {
                    SuffixArray.MatchResult match = suffixArray.findLongestMatch(data, localPos);
                    if (match.length() > 4) { // Minimum match length threshold
                        if (localPos > 0) {
                            byte[] litData = slice(data, 0, localPos);
                            refined.add(new DeltaCommand(CommandType.LITERAL, 0, litData.length, litData));
                        }
                        refined.add(new DeltaCommand(CommandType.COPY, match.offset(), match.length(), null));

                        int consumed = localPos + match.length();
                        if (consumed < data.length) {
                            data = slice(data, consumed, data.length - consumed);
                            localPos = 0;
                        } else {
                            break; // Avoid boundary issues
                        }
                    } else {
                        localPos++;
                    }
                }


                // Add remaining LITERAL
                if (data.length > 0) {
                    refined.add(new DeltaCommand(CommandType.LITERAL, 0, data.length, data));
                }
            }
        }

        return refined;
    }

    /**
     * Merge consecutive delta commands (COPY-COPY or LITERAL-LITERAL).
     *
     * @param commands List of DeltaCommand
     * @return Merged list of DeltaCommand
     */
    private static List<DeltaCommand> mergeCommands(List<DeltaCommand> commands) {
        if (commands.isEmpty()) return commands;

        List<DeltaCommand> merged = new ArrayList<>();
        DeltaCommand current = commands.get(0);

        for (int i = 1; i < commands.size(); i++) {
            DeltaCommand next = commands.get(i);
            if (canMerge(current, next)) {
                current = mergeTwo(current, next);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        return merged;
    }

    /**
     * Check if two delta commands can be merged.
     */
    private static boolean canMerge(DeltaCommand c1, DeltaCommand c2) {
        if (c1.type() != c2.type()) return false;
        if (c1.type() == CommandType.COPY) {
            // COPY: consecutive offsets
            return (c1.offset() + c1.length() == c2.offset());
        } else {
            // LITERAL: always can merge
            return true;
        }
    }

    /**
     * Merge two delta commands into one.
     */
    private static DeltaCommand mergeTwo(DeltaCommand c1, DeltaCommand c2) {
        if (c1.type() == CommandType.LITERAL) {
            byte[] mergedData = new byte[c1.length() + c2.length()];
            System.arraycopy(c1.data(), 0, mergedData, 0, c1.length());
            System.arraycopy(c2.data(), 0, mergedData, c1.length(), c2.length());
            return new DeltaCommand(CommandType.LITERAL, 0, mergedData.length, mergedData);
        } else {
            // COPY
            return new DeltaCommand(CommandType.COPY, c1.offset(), c1.length() + c2.length(), null);
        }
    }

    /**
     * Apply delta commands to original data to reconstruct modified data.
     *
     * @param original Original file data
     * @param commands Delta commands
     * @return Reconstructed modified data
     */
    public static byte[] applyDelta(byte[] original, List<DeltaCommand> commands) {
        long startTime = System.nanoTime();

        // Calculate total size
        int totalSize = commands.stream().mapToInt(DeltaCommand::length).sum();
        byte[] result = new byte[totalSize];
        int pos = 0;

        for (DeltaCommand cmd : commands) {
            if (cmd.type() == CommandType.COPY) {
                // Validate COPY command
                if (cmd.offset() < 0 || cmd.offset() + cmd.length() > original.length) {
                    throw new IllegalArgumentException("Invalid COPY command: " + cmd);
                }
                System.arraycopy(original, cmd.offset(), result, pos, cmd.length());
            } else if (cmd.type() == CommandType.LITERAL) {
                if (cmd.data() == null || cmd.data().length != cmd.length()) {
                    throw new IllegalArgumentException("Invalid LITERAL command: " + cmd);
                }
                System.arraycopy(cmd.data(), 0, result, pos, cmd.length());
            }
            pos += cmd.length();
        }

        double ms = (System.nanoTime() - startTime) / 1_000_000.0;
        System.out.printf("Delta apply took %.2f ms%n", ms);
        return result;
    }

    /* ======================================================
     * Helper Functions (CRC32, Hash, Slice, Hex Conversion)
     * ====================================================== */

    /**
     * Calculate CRC32 using java.util.zip.CRC32.
     *
     * @param data Input data
     * @return CRC32 checksum
     */
    private static long crc32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }

    /**
     * Calculate SHA-256 hash.
     *
     * @param data Input data
     * @return SHA-256 hash
     */
    private static byte[] strongHash(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance(STRONG_HASH_ALGO);
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found!", e);
        }
    }

    /**
     * Slice byte array from start with given length.
     *
     * @param data   Input data
     * @param start  Start index
     * @param length Length to slice
     * @return Sliced byte array
     */
    private static byte[] slice(byte[] data, int start, int length) {
        length = Math.min(length, data.length - start);
        byte[] result = new byte[length];
        System.arraycopy(data, start, result, 0, length);
        return result;
    }

    /**
     * Convert byte array to hex string.
     *
     * @param bytes Input byte array
     * @return Hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes){
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
