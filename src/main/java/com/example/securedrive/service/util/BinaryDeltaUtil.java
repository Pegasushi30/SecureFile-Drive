package com.example.securedrive.service.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.zip.CRC32;


public class BinaryDeltaUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_BLOCK_SIZE = 64 * 1024;

    private static final String STRONG_HASH_ALGO = "SHA-256";

    public enum CommandType {
        COPY,
        LITERAL
    }


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


    private static int determineBlockSize(long fileSize) {
        if (fileSize < 1_000_000) {
            return 512;
        } else if (fileSize < 10_000_000) {
            return 2 * 1024;
        } else if (fileSize < 100_000_000) {
            return 8 * 1024;
        } else {
            return MAX_BLOCK_SIZE;
        }
    }


    public static List<DeltaCommand> calculateDelta(byte[] original, byte[] modified) {
        if (original.length == modified.length) {
            byte[] origHash = strongHash(original);
            byte[] modHash = strongHash(modified);
            boolean hashesMatch = Arrays.equals(origHash, modHash);
            if (hashesMatch) {
                List<DeltaCommand> singleCommand = new ArrayList<>();
                singleCommand.add(new DeltaCommand(CommandType.COPY, 0, original.length, null));
                return singleCommand;
            }
        }

        List<DeltaCommand> coarseDelta = blockBasedDiff(original, modified);

        for (DeltaCommand cmd : coarseDelta) {
            System.out.println(cmd);
        }

        SuffixAutomaton suffixAutomaton = new SuffixAutomaton(original);
        List<DeltaCommand> fineDelta = refineWithSuffixAutomaton(suffixAutomaton, coarseDelta);

        for (DeltaCommand cmd : fineDelta) {
            System.out.println(cmd);
        }

        List<DeltaCommand> merged = mergeCommands(fineDelta);

        for (DeltaCommand cmd : merged) {
            System.out.println(cmd);
        }
        return merged;
    }



    private static List<DeltaCommand> blockBasedDiff(byte[] original, byte[] modified) {
        int blockSize = determineBlockSize(original.length);


        Map<Long, List<Integer>> originalMap = buildIndexParallel(original, blockSize);

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
                if (mPos > literalStart) {
                    int litLen = mPos - literalStart;
                    byte[] litData = slice(modified, literalStart, litLen);
                    commands.add(new DeltaCommand(CommandType.LITERAL, 0, litLen, litData));
                }
                commands.add(new DeltaCommand(CommandType.COPY, matchedIndex, matchedLength, null));

                mPos += matchedLength;
                literalStart = mPos;
            } else {
                mPos++;
            }
        }

        if (literalStart < modified.length) {
            int len = modified.length - literalStart;
            byte[] litData = slice(modified, literalStart, len);
            commands.add(new DeltaCommand(CommandType.LITERAL, 0, len, litData));
        }

        return commands;
    }


    private static Map<Long, List<Integer>> buildIndexParallel(byte[] original, int blockSize) {
        int numBlocks = (int) Math.ceil((double) original.length / blockSize);

        ConcurrentHashMap<Long, List<Integer>> map = new ConcurrentHashMap<>();

        int parallelism = Runtime.getRuntime().availableProcessors();
        ForkJoinPool forkJoinPool = new ForkJoinPool(parallelism);

        try {
            forkJoinPool.submit(() ->
                    IntStream.range(0, numBlocks).parallel().forEach(blockNum -> {
                        int pos = blockNum * blockSize;
                        int len = Math.min(blockSize, original.length - pos);
                        byte[] block = slice(original, pos, len);
                        long crc = crc32(block);
                        map.computeIfAbsent(crc, k -> Collections.synchronizedList(new ArrayList<>())).add(pos);
                    })
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error during parallel index building", e);
        } finally {
            forkJoinPool.shutdown();
        }

        return map;
    }


    private static List<DeltaCommand> refineWithSuffixAutomaton(SuffixAutomaton suffixAutomaton, List<DeltaCommand> coarseDelta) {
        List<DeltaCommand> refined = new ArrayList<>();

        for (DeltaCommand cmd : coarseDelta) {
            if (cmd.type() == CommandType.COPY) {
                refined.add(cmd);
            } else {
                byte[] data = cmd.data();
                int current = 0;
                int last = 0;

                while (current < data.length) {
                    SuffixAutomaton.MatchResult match = suffixAutomaton.findLongestMatch(data, current);
                    if (match.getLength() > 4) {
                        if (current > last) {
                            byte[] litData = slice(data, last, current - last);
                            refined.add(new DeltaCommand(CommandType.LITERAL, 0, litData.length, litData));
                        }
                        refined.add(new DeltaCommand(CommandType.COPY, match.getOffset(), match.getLength(), null));
                        current += match.getLength();
                        last = current;
                    } else {
                        current++;
                    }
                }

                if (last < data.length) {
                    byte[] litData = slice(data, last, data.length - last);
                    refined.add(new DeltaCommand(CommandType.LITERAL, 0, litData.length, litData));
                }
            }
        }

        return refined;
    }




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


    private static boolean canMerge(DeltaCommand c1, DeltaCommand c2) {
        if (c1.type() != c2.type()) return false;
        if (c1.type() == CommandType.COPY) {
            return (c1.offset() + c1.length() == c2.offset());
        } else {
            return true;
        }
    }


    private static DeltaCommand mergeTwo(DeltaCommand c1, DeltaCommand c2) {
        if (c1.type() == CommandType.LITERAL) {
            byte[] mergedData = new byte[c1.length() + c2.length()];
            System.arraycopy(c1.data(), 0, mergedData, 0, c1.length());
            System.arraycopy(c2.data(), 0, mergedData, c1.length(), c2.length());
            return new DeltaCommand(CommandType.LITERAL, 0, mergedData.length, mergedData);
        } else {
            return new DeltaCommand(CommandType.COPY, c1.offset(), c1.length() + c2.length(), null);
        }
    }


    public static byte[] applyDelta(byte[] original, List<DeltaCommand> commands) {
        int totalSize = commands.stream().mapToInt(DeltaCommand::length).sum();
        byte[] result = new byte[totalSize];
        AtomicInteger pos = new AtomicInteger(0);

        for (DeltaCommand cmd : commands) {
            if (cmd.type() == CommandType.COPY) {
                if (cmd.offset() < 0 || cmd.offset() + cmd.length() > original.length) {
                    throw new IllegalArgumentException("Invalid COPY command: " + cmd);
                }
                System.arraycopy(original, cmd.offset(), result, pos.get(), cmd.length());
            } else if (cmd.type() == CommandType.LITERAL) {
                if (cmd.data() == null || cmd.data().length != cmd.length()) {
                    throw new IllegalArgumentException("Invalid LITERAL command: " + cmd);
                }
                System.arraycopy(cmd.data(), 0, result, pos.get(), cmd.length());
            }
            pos.addAndGet(cmd.length());
        }
        return result;
    }

    private static long crc32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }


    private static byte[] strongHash(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance(STRONG_HASH_ALGO);
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found!", e);
        }
    }


    private static byte[] slice(byte[] data, int start, int length) {
        length = Math.min(length, data.length - start);
        byte[] result = new byte[length];
        System.arraycopy(data, start, result, 0, length);
        return result;
    }


    public static byte[] loadBinaryFile(String filePath) throws IOException {
        System.out.println("Loading binary file: " + filePath);
        return Files.readAllBytes(Path.of(filePath));
    }


    public static void saveBinaryFile(byte[] data, String filePath) throws IOException {
        Path outputPath = Path.of(filePath);
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }


    public static void saveBinaryDeltaCommands(List<DeltaCommand> deltaCommands, String filePath) throws IOException {
        Path deltaPath = Path.of(filePath);
        Files.createDirectories(deltaPath.getParent());
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(deltaCommands);
        Files.writeString(deltaPath, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
