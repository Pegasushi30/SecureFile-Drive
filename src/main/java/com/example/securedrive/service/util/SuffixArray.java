package com.example.securedrive.service.util;

/**
 * Suffix Array implementation using SA-IS algorithm and optimized longest match finding using Suffix Automaton.
 */
public class SuffixArray {

    private final byte[] data;
    final int[] suffixArray;
    private final SuffixAutomaton suffixAutomaton;

    public SuffixArray(byte[] data) {
        this.data = data;
        this.suffixArray = buildSuffixArray(data);
        this.suffixAutomaton = new SuffixAutomaton(data);
    }

    /**
     * Builds the suffix array using the SA-IS algorithm.
     *
     * @param data Original file data as byte array
     * @return Suffix array
     */
    int[] buildSuffixArray(byte[] data) {
        int n = data.length;
        int[] s = new int[n + 1]; // Input with sentinel
        for (int i = 0; i < n; i++) {
            s[i] = (data[i] & 0xFF) + 1; // Shift values to 1-based
        }
        s[n] = 0; // Sentinel value

        int[] sa = new int[s.length];
        SAIS.buildSuffixArray(s, sa);
        return sa;
    }

    /**
     * CompareResult: Stores the result of comparison and the number of matched characters.
     */
    public record CompareResult(int cmp, int matchLen) {}

    /**
     * Compares the suffix at saPos with modified from modPos.
     * Returns a CompareResult object containing:
     *   - cmp: negative, zero, positive (like compareTo)
     *   - matchLen: number of matched characters so far
     */
    CompareResult compareSuffixDetailed(byte[] modified, int modPos, int saPos) {
        if (modPos < 0 || modPos >= modified.length || saPos < 0 || saPos > data.length) {
            return new CompareResult(1, 0);
        }

        int m = modPos;
        int s = saPos;
        int matched = 0;

        while (m < modified.length && s < data.length) {
            int a = modified[m] & 0xFF;
            int b = data[s] & 0xFF;

            if (a == b) {
                matched++;
                m++;
                s++;
            } else {
                return new CompareResult(a - b, matched);
            }
        }

        // Handle remaining elements
        if (m == modified.length && s == data.length) {
            return new CompareResult(0, matched); // Both arrays matched completely
        } else if (m == modified.length) {
            return new CompareResult(-1, matched); // Modified shorter than suffix
        } else {
            return new CompareResult(1, matched); // Suffix shorter than modified
        }
    }

    /**
     * Finds the longest match in the original data for the modified data starting at modPos.
     *
     * @param modified The modified data as byte array.
     * @param modPos   The starting position in the modified data.
     * @return MatchResult containing the offset in the original data and the length of the match.
     */
    public MatchResult findLongestMatch(byte[] modified, int modPos) {
        if (modPos < 0 || modPos >= modified.length) {
            return new MatchResult(-1, 0);
        }

        // Kullanılacak yöntem: Suffix Automaton ile O(n) zaman
        // Orijinal dize üzerinde oluşturulmuş suffix automaton kullanılır
        return suffixAutomaton.findLongestMatch(modified, modPos);
    }

    /**
     * Calculates the length of the matching prefix between the modified and original data.
     *
     * @param modified The modified data as byte array.
     * @param modPos   The starting position in the modified data.
     * @param saPos    The starting position in the suffix array.
     * @return The length of the matching prefix.
     */
    int getMatchLength(byte[] modified, int modPos, int saPos) {
        int count = 0;
        while (modPos < modified.length && saPos < data.length) {
            if (modified[modPos] == data[saPos]) {
                count++;
                modPos++;
                saPos++;
            } else {
                break;
            }
        }
        return count;
    }
}
