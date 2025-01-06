package com.example.securedrive.service.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SAIS {

    public static void buildSuffixArray(int[] s, int[] sa) {
        int n = s.length;

        // Find the maximum value in the alphabet (K)
        int maxVal = 0;
        for (int val : s) {
            if (val > maxVal) {
                maxVal = val;
            }
        }

        // Start SA-IS algorithm
        sais(s, sa, n, maxVal);
    }

    /**
     * SA-IS algorithm implementation.
     */
    private static void sais(int[] s, int[] sa, int n, int K) {
        // Step 1: Determine L/S type array
        boolean[] isS = new boolean[n];
        isS[n - 1] = true;  // Sentinel is S-type
        for (int i = n - 2; i >= 0; i--) {
            if (s[i] < s[i + 1]) {
                isS[i] = true;  // S-type
            } else if (s[i] > s[i + 1]) {
                isS[i] = false; // L-type
            } else {
                isS[i] = isS[i + 1]; // Propagate S-type status
            }
        }
        // Step 2: Find LMS positions
        List<Integer> lmsList = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            if (isLMS(i, isS)) {
                lmsList.add(i);
            }
        }

        int lmsCount = lmsList.size();
        if (lmsCount == 0) {
            for (int i = 0; i < n; i++) {
                sa[i] = i;
            }
            return;
        }

        // Step 3: Frequency counting
        int[] freq = new int[K + 1];
        for (int val : s) {
            freq[val]++;
        }
        for (int i = 1; i <= K; i++) {
            freq[i] += freq[i - 1];
        }

        // Step 4: Initialize bucket heads
        int[] bucketHead = new int[K + 1];
        bucketHead[0] = 0;
        System.arraycopy(freq, 0, bucketHead, 1, K);

        // Step 5: Place LMS characters
        Arrays.fill(sa, -1);
        int[] tail = Arrays.copyOf(freq, freq.length);

        for (int i = lmsCount - 1; i >= 0; i--) {
            int pos = lmsList.get(i);
            sa[--tail[s[pos]]] = pos;
        }


        // Step 6: L-type sorting
        int[] head = Arrays.copyOf(bucketHead, bucketHead.length);
        lTypeSorting(sa, s, head, isS);


        // Step 7: S-type sorting
        tail = Arrays.copyOf(freq, freq.length);
        sTypeSorting(sa, s, tail, isS);

        // Step 8: Name LMS substrings
        int[] lmsIdx = new int[lmsCount];
        int idx = 0;

        for (int p : sa) {
            if (isLMS(p, isS)) {
                lmsIdx[idx++] = p;
            }
        }

        int[] name = new int[n];
        Arrays.fill(name, -1);

        name[lmsIdx[0]] = 0;

        int nameCount = 1;

        for (int i = 1; i < lmsCount; i++) {
            int cur = lmsIdx[i];
            int prev = lmsIdx[i - 1];
            boolean isEqual = equalLMSsubstring(s, isS, cur, prev);

            if (!isEqual) {
                name[cur] = nameCount++;
            } else {
                name[cur] = nameCount - 1; // Aynı isim atanır
            }
        }


        // Step 9: Create reduced problem array
        int[] s1 = new int[lmsCount];
        idx = 0;

        for (int pos : lmsIdx) {
            s1[idx++] = name[pos];
        }


        // Step 10: Recursive solution
        int[] sa1 = new int[lmsCount];

        if (nameCount < lmsCount) {
            sais(s1, sa1, lmsCount, nameCount - 1);
        } else {
            for (int i = 0; i < lmsCount; i++) {
                sa1[s1[i]] = i;
            }
        }


        // Step 11: Restore LMS positions in suffix array
        for (int i = 0; i < lmsCount; i++) {
            lmsIdx[i] = lmsList.get(lmsCount - 1 - sa1[i]);
        }

        // Step 12: Place LMS characters back into suffix array
        Arrays.fill(sa, -1);

        tail = Arrays.copyOf(freq, freq.length);

        for (int i = lmsCount - 1; i >= 0; i--) {
            sa[--tail[s[lmsIdx[i]]]] = lmsIdx[i];
        }

        // Step 13: Final L-type and S-type sorting
        head = Arrays.copyOf(bucketHead, bucketHead.length);
        lTypeSorting(sa, s, head, isS);

        tail = Arrays.copyOf(freq, freq.length);
        sTypeSorting(sa, s, tail, isS);

    }

    private static void lTypeSorting(int[] sa, int[] s, int[] head, boolean[] isS) {
        for (int i = 0; i < sa.length; i++) {
            int p = sa[i];
            if (p > 0 && !isS[p - 1]) {
                sa[head[s[p - 1]]++] = p - 1;
            }
        }
    }

    private static void sTypeSorting(int[] sa, int[] s, int[] tail, boolean[] isS) {
        for (int i = sa.length - 1; i >= 0; i--) {
            int p = sa[i];
            if (p > 0 && isS[p - 1]) {
                sa[--tail[s[p - 1]]] = p - 1;
            }
        }
    }

    private static boolean isLMS(int index, boolean[] isS) {
        return index > 0 && isS[index] && !isS[index - 1];
    }

    /**
     * Compares two LMS substrings starting at positions a and b.
     *
     * @param s    The input string as an integer array.
     * @param isS  The L/S type array.
     * @param a    The starting index of the first LMS substring.
     * @param b    The starting index of the second LMS substring.
     * @return True if the LMS substrings are identical; otherwise, false.
     */
    private static boolean equalLMSsubstring(int[] s, boolean[] isS, int a, int b) {
        if (a == b) return true; // Same positions are equal

        int n = s.length;

        while (a < n && b < n) {
            if (s[a] != s[b]) {
                return false;
            }

            a++;
            b++;

            if (isLMS(a, isS) && isLMS(b, isS)) {
                break; // Her iki substring de bir sonraki LMS pozisyonuna ulaştı
            }

            if (isLMS(a, isS) != isLMS(b, isS)) {
                return false;
            }
        }

        // Her iki substring de aynı noktada sonlandıysa eşittir
        return isLMS(a, isS) && isLMS(b, isS);
    }

}