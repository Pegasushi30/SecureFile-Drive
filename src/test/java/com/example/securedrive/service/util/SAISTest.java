package com.example.securedrive.service.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SAISTest {

    /**
     * Converts a string to an integer array with a sentinel.
     * Each character is converted to its ASCII value.
     *
     * @param str Input string
     * @return Integer array representation with sentinel
     */
    public static int[] stringToIntArray(String str) {
        int[] s = new int[str.length() + 1]; // Add space for sentinel
        for (int i = 0; i < str.length(); i++) {
            s[i] = str.charAt(i);
        }
        s[str.length()] = 0; // Sentinel at the last position
        return s;
    }

    @Test
    public void testEmptyString() {
        int[] s = {0}; // Only sentinel
        int[] sa = new int[s.length];
        SAIS.buildSuffixArray(s, sa);
        Assertions.assertArrayEquals(new int[]{0}, sa);
    }

    @Test
    public void testSingleCharacter() {
        String str = "a";
        int[] s = stringToIntArray(str);
        int[] sa = new int[s.length];
        SAIS.buildSuffixArray(s, sa);

        Assertions.assertArrayEquals(new int[]{1, 0}, sa);
    }

    @Test
    public void testSimpleString_Banana() {
        String str = "banana";
        int[] s = stringToIntArray(str);
        int[] sa = new int[s.length];

        SAIS.buildSuffixArray(s, sa);

        Assertions.assertArrayEquals(new int[]{6,5,3,1,0,4,2}, sa);
    }

    @Test
    public void testSimpleString_Apple() {
        String str = "apple";
        int[] s = stringToIntArray(str);

        int[] sa = new int[s.length];

        SAIS.buildSuffixArray(s, sa);

        Assertions.assertArrayEquals(new int[]{5,0,4,3,2,1}, sa);
    }

    @Test
    public void testRepeatingCharacters() {
        String str ="aaaaa";
        int[] s= stringToIntArray(str);
        int []sa= new int[s.length];

        SAIS.buildSuffixArray(s ,sa);

        Assertions.assertArrayEquals(new int[]{5 ,4 ,3 ,2 ,1 ,0},sa);
    }

    @Test
    public void testIncreasingCharacters() {
        String str ="abcde";
        int []s= stringToIntArray(str);
        int []sa= new int[s.length];

        SAIS.buildSuffixArray(s ,sa);

        Assertions.assertArrayEquals(new int[]{5 ,0 ,1 ,2 ,3 ,4},sa);
    }

    @Test
    public void testDecreasingCharacters() {
        String str ="edcba";
        int []s= stringToIntArray(str);
        int []sa= new int[s.length];

        SAIS.buildSuffixArray(s ,sa);

        Assertions.assertArrayEquals(new int[]{5 ,4 ,3 ,2 ,1 ,0},sa);
    }

    @Test
    public void testComplexString() {
        String str = "mississippi";
        int[] s = stringToIntArray(str);
        int[] sa = new int[s.length];
        SAIS.buildSuffixArray(s, sa);

        // Correct suffix array for "mississippi" with sentinel at index 11
        int[] expected = {11, 10, 7, 4, 1, 0, 9, 8, 6, 3, 5, 2};

        for (int i = 0; i < sa.length; i++) {
            int suffixIndex = sa[i];
            if (suffixIndex < 0) {
                System.out.printf("Suffix at SA[%d]: <INVALID: %d>%n", i, suffixIndex);
            } else if (suffixIndex >= str.length()) {
                // Possibly it's the sentinel or "empty" suffix.
                System.out.printf("Suffix at SA[%d]: <SENTINEL or out of range: %d>%n", i, suffixIndex);
            } else {
                // Valid index in [0..str.length()-1]
                System.out.printf("Suffix at SA[%d]: %s%n", i, str.substring(suffixIndex));
            }
        }

        Assertions.assertArrayEquals(expected, sa,
                "Suffix array for 'mississippi' is incorrect.");
    }
}
