package com.example.securedrive.util;

import java.util.*;

public class SuffixArray {

    private final byte[] data;
    private final int[] suffixArray;

    public SuffixArray(byte[] data) {
        this.data = data;
        this.suffixArray = buildSuffixArray(data);
    }

    /**
     * Builds the suffix array using the SA-IS algorithm.
     *
     * @param data Original file data as byte array
     * @return Suffix array
     */
    private int[] buildSuffixArray(byte[] data) {
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
     * Finds the longest match using the suffix array.
     *
     * @param modified Modified file data
     * @param modPos   Position in modified data
     * @return MatchResult containing offset and length
     */
    public MatchResult findLongestMatch(byte[] modified, int modPos) {
        if (modPos < 0 || modPos >= modified.length) {
            return new MatchResult(-1, 0);
        }

        int left = 0;
        int right = suffixArray.length - 1;
        int bestOffset = -1;
        int bestLength = 0;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            int cmp = compareSuffix(modified, modPos, suffixArray[mid]);

            if (cmp == 0) {
                // Exact match found, find the maximum matching length
                int length = getMatchLength(modified, modPos, suffixArray[mid]);
                if (length > bestLength) {
                    bestLength = length;
                    bestOffset = suffixArray[mid];
                }

                // Check neighbors for longer matches
                int temp = mid - 1;
                while (temp >= left && compareSuffix(modified, modPos, suffixArray[temp]) == 0) {
                    int len = getMatchLength(modified, modPos, suffixArray[temp]);
                    if (len > bestLength) {
                        bestLength = len;
                        bestOffset = suffixArray[temp];
                    }
                    temp--;
                }

                temp = mid + 1;
                while (temp <= right && compareSuffix(modified, modPos, suffixArray[temp]) == 0) {
                    int len = getMatchLength(modified, modPos, suffixArray[temp]);
                    if (len > bestLength) {
                        bestLength = len;
                        bestOffset = suffixArray[temp];
                    }
                    temp++;
                }

                break;
            } else if (cmp < 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return new MatchResult(bestOffset, bestLength);
    }

    /**
     * Compares the suffix at a specific position with the modified data starting from modPos.
     */
    private int compareSuffix(byte[] modified, int modPos, int saPos) {
        if (modPos >= modified.length || saPos < 0 || saPos >= data.length) {
            // Out of bounds comparison, treat as unequal
            return 1;
        }

        int m = modPos;
        int s = saPos;
        while (m < modified.length && s < data.length) {
            int a = modified[m] & 0xFF;
            int b = data[s] & 0xFF;
            if (a != b) {
                return a - b;
            }
            m++;
            s++;
        }
        if (m == modified.length && s == data.length) {
            return 0;
        } else if (m == modified.length) {
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * Calculates the length of the matching prefix between the modified and original data.
     */
    private int getMatchLength(byte[] modified, int modPos, int saPos) {
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

    /**
     * MatchResult: Stores the offset and length of the match.
     */
    public record MatchResult(int offset, int length) {}
}
