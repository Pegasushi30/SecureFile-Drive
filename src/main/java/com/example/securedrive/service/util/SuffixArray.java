package com.example.securedrive.service.util;

public class SuffixArray {

    private final byte[] data;
    final int[] suffixArray;

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
            CompareResult cr = compareSuffixDetailed(modified, modPos, suffixArray[mid]);

            if (cr.matchLen() > bestLength) {
                bestLength = cr.matchLen();
                bestOffset = suffixArray[mid];
            }

            if (cr.cmp() == 0) {
                // Tam eşleşme bulundu, komşuları kontrol et
                int temp = mid - 1;
                while (temp >= left) {
                    CompareResult tempCr = compareSuffixDetailed(modified, modPos, suffixArray[temp]);
                    if (tempCr.cmp() != 0) break;
                    if (tempCr.matchLen() > bestLength) {
                        bestLength = tempCr.matchLen();
                        bestOffset = suffixArray[temp];
                    }
                    temp--;
                }

                temp = mid + 1;
                while (temp <= right) {
                    CompareResult tempCr = compareSuffixDetailed(modified, modPos, suffixArray[temp]);
                    if (tempCr.cmp() != 0) break;
                    if (tempCr.matchLen() > bestLength) {
                        bestLength = tempCr.matchLen();
                        bestOffset = suffixArray[temp];
                    }
                    temp++;
                }

                // Tam eşleşme bulundu, aramayı sonlandır
                break;
            } else if (cr.cmp() < 0) {
                // Modified dizesi suffix'ten küçük, sol yarıya daraltın
                right = mid - 1;
            } else {
                // Modified dizesi suffix'ten büyük veya eşitse, sağ yarıya daraltın
                left = mid + 1;
            }
        }
        return new MatchResult(bestOffset, bestLength);
    }





    /**
     * Calculates the length of the matching prefix between the modified and original data.
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

    /**
     * MatchResult: Stores the offset and length of the match.
     */
    public record MatchResult(int offset, int length) {}
}
