package com.example.securedrive.service.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SuffixArrayTest {


    @Test
    @DisplayName("buildSuffixArray - Basic test to verify the correct length of the suffix array")
    void testBuildSuffixArray() {
        // Given
        byte[] data = "banana".getBytes();

        // When
        SuffixArray suffixArray = new SuffixArray(data);

        // Then
        assertEquals(data.length + 1, suffixArray.suffixArray.length,
                "Suffix array size should be (n+1).");
        assertNotNull(suffixArray.suffixArray, "Suffix array should not be null.");
        int[] expectedSA = {6, 5, 3, 1, 0, 4, 2};
        assertArrayEquals(expectedSA, suffixArray.suffixArray, "Suffix array should match the expected values.");
    }

    @Test
    @DisplayName("findLongestMatch - Full match test (searching the same data)")
    void testFindLongestMatchFullMatch() {
        // Given
        byte[] data = "banana".getBytes();
        SuffixArray suffixArray = new SuffixArray(data);
        byte[] modified = "banana".getBytes();

        // When
        SuffixArray.MatchResult result = suffixArray.findLongestMatch(modified, 0);
        String matchedSubstring = result.offset() >= 0
                ? new String(data, result.offset(), result.length())
                : "No match found";

        // Then
        System.out.println("Matched substring: \"" + matchedSubstring + "\"");
        System.out.println("Match length: " + result.length());
        System.out.println("Match offset: " + result.offset());

        assertEquals(6, result.length(), "Expecting a full string match of 6 characters.");
        assertTrue(result.offset() >= 0 && result.offset() < data.length,
                "Offset should be between 0 and data length (6).");
        assertEquals(0, result.offset(), "Best match offset should be 0.");
    }

    @Test
    @DisplayName("findLongestMatch - Partial match test (e.g., searching 'ban')")
    void testFindLongestMatchPartial() {
        // Given
        byte[] data = "banana".getBytes();
        SuffixArray suffixArray = new SuffixArray(data);
        byte[] modified = "ban".getBytes();

        // When
        SuffixArray.MatchResult result = suffixArray.findLongestMatch(modified, 0);

        // Then
        assertEquals(3, result.length(), "Expecting a 3-character match for 'ban'.");
        assertEquals(0, result.offset(), "Best match offset should be 0.");
    }

    @Test
    @DisplayName("findLongestMatch - No match test (e.g., searching 'zzz')")
    void testFindLongestMatchNoMatch() {
        // Given
        byte[] data = "banana".getBytes();
        SuffixArray suffixArray = new SuffixArray(data);
        byte[] modified = "zzz".getBytes();

        // When
        SuffixArray.MatchResult result = suffixArray.findLongestMatch(modified, 0);

        // Then
        assertEquals(-1, result.offset(), "Offset should be -1 when there is no match.");
        assertEquals(0, result.length(), "Match length should be 0 when there is no match.");
    }

    @Test
    @DisplayName("findLongestMatch - Single character test (e.g., searching 'a')")
    void testFindLongestMatchSingleChar() {
        // Given
        byte[] data = "banana".getBytes();
        SuffixArray suffixArray = new SuffixArray(data);
        byte[] modified = "a".getBytes();

        // When
        SuffixArray.MatchResult result = suffixArray.findLongestMatch(modified, 0);

        // Then
        assertEquals(1, result.length(), "Single character 'a' should have a match length of 1.");
        assertTrue(result.offset() == 1 || result.offset() == 3 || result.offset() == 5,
                "Offset should be one of the positions where 'a' occurs (1, 3, or 5).");
    }

    @Test
    @DisplayName("compareSuffixDetailed - Direct comparison test")
    void testCompareSuffixDetailed() {
        // Given
        byte[] data = "banana".getBytes();
        SuffixArray suffixArray = new SuffixArray(data);
        byte[] modified = "banana".getBytes();

        // When
        SuffixArray.CompareResult result = suffixArray.compareSuffixDetailed(modified, 0, 0);

        // Then
        System.out.println("Compare Result: cmp=" + result.cmp() + ", matchLen=" + result.matchLen());
        assertEquals(0, result.cmp(), "Expecting a full match comparison result of 0.");
        assertEquals(6, result.matchLen(), "Expecting a full match length of 6.");
    }

    @Test
    @DisplayName("findLongestMatch - modPos boundary errors")
    void testFindLongestMatchOutOfBounds() {
        // Given
        byte[] data = "banana".getBytes();
        SuffixArray suffixArray = new SuffixArray(data);
        byte[] modified = "banana".getBytes();

        // When
        SuffixArray.MatchResult resultNegative = suffixArray.findLongestMatch(modified, -1);
        SuffixArray.MatchResult resultOutOfRange = suffixArray.findLongestMatch(modified, modified.length);

        // Then
        assertEquals(-1, resultNegative.offset(), "Offset should be -1 for negative modPos.");
        assertEquals(0, resultNegative.length(), "Match length should be 0 for negative modPos.");
        assertEquals(-1, resultOutOfRange.offset(), "Offset should be -1 for out-of-range modPos.");
        assertEquals(0, resultOutOfRange.length(), "Match length should be 0 for out-of-range modPos.");
    }

    @Test
    @DisplayName("compareSuffixDetailed and getMatchLength - Direct testing")
    void testCompareSuffixDetailedAndGetMatchLength() {
        // Given
        byte[] data = "banana".getBytes();
        SuffixArray suffixArray = new SuffixArray(data);
        int saPos = suffixArray.suffixArray[3];

        // When
        SuffixArray.CompareResult cr = suffixArray.compareSuffixDetailed("banana".getBytes(), 1, saPos);
        int matchLength = suffixArray.getMatchLength("banana".getBytes(), 1, 3);

        // Then
        assertTrue(cr.cmp() <= 1 && cr.cmp() >= -1, "compareSuffixDetailed result should be between -1 and 1.");
        assertTrue(cr.matchLen() >= 2, "Expected match length to be at least 2 characters.");
        assertTrue(matchLength >= 2, "Expected match length to be at least 2 characters.");
    }
}
