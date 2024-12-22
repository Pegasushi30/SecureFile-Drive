package com.example.securedrive.util;

import java.util.*;

/**
 * SA-IS algorithm implementation for building suffix arrays.
 */
public class SAIS {

    /**
     * Builds the suffix array for the given input.
     *
     * @param s  Input integer array (characters with sentinel)
     * @param sa Output suffix array
     */
    public static void buildSuffixArray(int[] s, int[] sa) {
        int n = s.length;
        int K = Arrays.stream(s).max().orElse(0) + 1; // Alphabet size

        // Step 1: Classify suffixes as S or L
        boolean[] isS = new boolean[n];
        isS[n - 1] = true;
        for (int i = n - 2; i >= 0; i--) {
            if (s[i] < s[i + 1]) {
                isS[i] = true;
            } else if (s[i] > s[i + 1]) {
                isS[i] = false;
            } else {
                isS[i] = isS[i + 1];
            }
        }

        // Step 2: Identify LMS (Leftmost S-type) positions
        List<Integer> lms = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            if (isS[i] && !isS[i - 1]) {
                lms.add(i);
            }
        }

        // Step 3: Initialize buckets
        int[] bucket = new int[K];
        for (int c : s) {
            bucket[c]++;
        }

        // Step 4: Sort LMS suffixes
        int[] bucketEnd = Arrays.copyOf(bucket, K);
        for (int i = 1; i < K; i++) {
            bucketEnd[i] += bucketEnd[i - 1];
        }

        Arrays.fill(sa, -1);
        for (int i = lms.size() - 1; i >= 0; i--) {
            int pos = lms.get(i);
            int c = s[pos];
            sa[--bucketEnd[c]] = pos;
        }

        // Step 5: Induce sort L and S suffixes
        induceSort(s, sa, isS, bucket);
    }

    /**
     * Performs induced sorting of L-type and S-type suffixes.
     *
     * @param s      Input integer array
     * @param sa     Output suffix array
     * @param isS    Boolean array indicating S-type suffixes
     * @param bucket Bucket sizes
     */
    private static void induceSort(int[] s, int[] sa, boolean[] isS, int[] bucket) {
        int n = s.length;

        // Calculate bucket starts
        int[] bucketStart = new int[bucket.length];
        for (int i = 1; i < bucket.length; i++) {
            bucketStart[i] = bucketStart[i - 1] + bucket[i - 1];
        }

        // Induce L-type suffixes
        for (int i = 0; i < n; i++) {
            int pos = sa[i] - 1;
            if (pos >= 0 && !isS[pos]) {
                sa[bucketStart[s[pos]]++] = pos;
            }
        }

        // Calculate bucket ends
        int[] bucketEnd = Arrays.copyOf(bucketStart, bucket.length);
        for (int i = 1; i < bucket.length; i++) {
            bucketEnd[i] += bucket[i];
        }

        // Induce S-type suffixes
        for (int i = n - 1; i >= 0; i--) {
            int pos = sa[i] - 1;
            if (pos >= 0 && isS[pos]) {
                sa[--bucketEnd[s[pos]]] = pos;
            }
        }
    }
}
