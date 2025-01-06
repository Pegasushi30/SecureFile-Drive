package com.example.securedrive.service.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test class for SuffixAutomaton.
 */
public class SuffixAutomatonTest {

    /**
     * Test with an empty original string.
     */
    @Test
    @DisplayName("testEmptyOriginal - Empty original string")
    void testEmptyOriginal() {
        byte[] original = "".getBytes();
        SuffixAutomaton automaton = new SuffixAutomaton(original);

        byte[] modified = "any string".getBytes();
        MatchResult result = automaton.findLongestMatch(modified, 0);

        assertEquals(-1, result.offset(), "Offset should be -1 for no matches.");
        assertEquals(0, result.length(), "Length should be 0 for no matches.");
    }

    /**
     * Test with a single character original string.
     */
    @Test
    @DisplayName("testSingleCharacterOriginal - Single character original string")
    void testSingleCharacterOriginal() {
        byte[] original = "a".getBytes();
        SuffixAutomaton automaton = new SuffixAutomaton(original);

        byte[] modified1 = "a".getBytes();
        MatchResult result1 = automaton.findLongestMatch(modified1, 0);

        assertEquals(0, result1.offset(), "Offset should be 0 for single character match.");
        assertEquals(1, result1.length(), "Length should be 1 for single character match.");

        byte[] modified2 = "b".getBytes();
        MatchResult result2 = automaton.findLongestMatch(modified2, 0);

        assertEquals(-1, result2.offset(), "Offset should be -1 for no match.");
        assertEquals(0, result2.length(), "Length should be 0 for no match.");
    }

    /**
     * Test with repeated characters in the original string.
     */
    @Test
    @DisplayName("testRepeatedCharactersOriginal - Repeated characters in original string")
    void testRepeatedCharactersOriginal() {
        byte[] original = "aaaaa".getBytes();
        SuffixAutomaton automaton = new SuffixAutomaton(original);

        byte[] modified = "aaaab".getBytes();
        MatchResult result = automaton.findLongestMatch(modified, 0);

        assertEquals(0, result.offset(), "Offset should be 0 for repeated 'a's.");
        assertEquals(4, result.length(), "Length should be 4 for the longest 'a' match.");
    }

    /**
     * Test with unique characters in the original string.
     */
    @Test
    @DisplayName("testUniqueCharactersOriginal - Unique characters in original string")
    void testUniqueCharactersOriginal() {
        byte[] original = "abcdef".getBytes();
        SuffixAutomaton automaton = new SuffixAutomaton(original);

        byte[] modified1 = "abcxyz".getBytes();
        MatchResult result1 = automaton.findLongestMatch(modified1, 0);

        assertEquals(0, result1.offset(), "Offset should be 0 for 'abc' match.");
        assertEquals(3, result1.length(), "Length should be 3 for 'abc' match.");

        byte[] modified2 = "xyzabc".getBytes();
        MatchResult result2 = automaton.findLongestMatch(modified2, 3);

        assertEquals(0, result2.offset(), "Offset should be 0 for 'abc' match starting at position 3.");
        assertEquals(3, result2.length(), "Length should be 3 for 'abc' match.");
    }

    /**
     * Test with overlapping substrings.
     */
    @Test
    @DisplayName("testOverlappingSubstrings - Overlapping substrings")
    void testOverlappingSubstrings() {
        byte[] original = "ababab".getBytes();
        SuffixAutomaton automaton = new SuffixAutomaton(original);

        byte[] modified = "ababa".getBytes();
        MatchResult result = automaton.findLongestMatch(modified, 0);

        assertEquals(0, result.offset(), "Offset should be 0 for 'ababa' match.");
        assertEquals(5, result.length(), "Length should be 5 for 'ababa' match.");
    }

    /**
     * Test findLongestMatch with different starting positions.
     */
    @Test
    @DisplayName("testFindLongestMatchDifferentPositions - Different starting positions")
    void testFindLongestMatchDifferentPositions() {
        byte[] original = "banana".getBytes();
        SuffixAutomaton automaton = new SuffixAutomaton(original);

        byte[] modified1 = "nana".getBytes();
        MatchResult result1 = automaton.findLongestMatch(modified1, 0);

        assertEquals(2, result1.offset(), "Offset should be 2 for 'nana' match.");
        assertEquals(4, result1.length(), "Length should be 4 for 'nana' match.");

        byte[] modified2 = "ana".getBytes();
        MatchResult result2 = automaton.findLongestMatch(modified2, 0);

        assertEquals(1, result2.offset(), "Offset should be 1 for 'ana' match.");
        assertEquals(3, result2.length(), "Length should be 3 for 'ana' match.");

        byte[] modified3 = "apple".getBytes();
        MatchResult result3 = automaton.findLongestMatch(modified3, 0);

        assertEquals(1, result3.offset(), "Offset should be 1 for 'a' match in 'apple'.");
        assertEquals(1, result3.length(), "Length should be 1 for 'a' match in 'apple'.");
    }

    /**
     * Test with the entire modified string matching the original.
     */
    @Test
    @DisplayName("testFullMatch - Full string match")
    void testFullMatch() {
        byte[] original = "hello world".getBytes();
        SuffixAutomaton automaton = new SuffixAutomaton(original);

        byte[] modified = "hello world".getBytes();
        MatchResult result = automaton.findLongestMatch(modified, 0);

        assertEquals(0, result.offset(), "Offset should be 0 for full match.");
        assertEquals(original.length, result.length(), "Length should match the original length.");
    }

    /**
     * Test with partial matches and no matches.
     */
    @Test
    @DisplayName("testPartialAndNoMatches - Partial and no matches")
    void testPartialAndNoMatches() {
        byte[] original = "mississippi".getBytes();
        SuffixAutomaton automaton = new SuffixAutomaton(original);

        byte[] modified1 = "issip".getBytes();
        MatchResult result1 = automaton.findLongestMatch(modified1, 0);

        assertEquals(4, result1.offset(), "Offset should be 4 for 'issip' match.");
        assertEquals(5, result1.length(), "Length should be 5 for 'issip' match.");

        byte[] modified2 = "xyz".getBytes();
        MatchResult result2 = automaton.findLongestMatch(modified2, 0);

        assertEquals(-1, result2.offset(), "Offset should be -1 for no match.");
        assertEquals(0, result2.length(), "Length should be 0 for no match.");
    }

    /**
     * Test findLongestMatch with starting position beyond modified array.
     */
    @Test
    @DisplayName("testFindLongestMatchInvalidPosition - Invalid starting position")
    void testFindLongestMatchInvalidPosition() {
        byte[] original = "example".getBytes();
        SuffixAutomaton automaton = new SuffixAutomaton(original);

        byte[] modified = "sample".getBytes();
        // Hatalı olan satır: modified.length() yerine modified.length kullanmalısınız
        MatchResult result = automaton.findLongestMatch(modified, modified.length);

        assertEquals(-1, result.offset(), "Offset should be -1 for invalid starting position.");
        assertEquals(0, result.length(), "Length should be 0 for invalid starting position.");
    }

    /**
     * Test with original string containing all possible byte values.
     */
    @Test
    @DisplayName("testAllByteValues - Original string with all byte values")
    void testAllByteValues() {
        byte[] original = new byte[256];
        for (int i = 0; i < 256; i++) {
            original[i] = (byte) i;
        }
        SuffixAutomaton automaton = new SuffixAutomaton(original);

        byte[] modified = new byte[256];
        for (int i = 0; i < 256; i++) {
            modified[i] = (byte) i;
        }
        MatchResult result = automaton.findLongestMatch(modified, 0);

        assertEquals(0, result.offset(), "Offset should be 0 for full byte array match.");
        assertEquals(256, result.length(), "Length should be 256 for full byte array match.");

        // Test with a partial match
        byte[] modifiedPartial = new byte[100];
        System.arraycopy(original, 50, modifiedPartial, 0, 100);
        MatchResult resultPartial = automaton.findLongestMatch(modifiedPartial, 0);

        assertEquals(50, resultPartial.offset(), "Offset should be 50 for partial match.");
        assertEquals(100, resultPartial.length(), "Length should be 100 for partial match.");
    }

    /**
     * Test with original string containing repeated patterns.
     */
    @Test
    @DisplayName("testRepeatedPatterns - Repeated patterns in original string")
    void testRepeatedPatterns() {
        byte[] original = "abcabcabcabc".getBytes();
        SuffixAutomaton automaton = new SuffixAutomaton(original);

        byte[] modified1 = "abcabc".getBytes();
        MatchResult result1 = automaton.findLongestMatch(modified1, 0);

        assertEquals(0, result1.offset(), "Offset should be 0 for 'abcabc' match.");
        assertEquals(6, result1.length(), "Length should be 6 for 'abcabc' match.");

        byte[] modified2 = "bcabca".getBytes();
        MatchResult result2 = automaton.findLongestMatch(modified2, 0);

        assertEquals(1, result2.offset(), "Offset should be 1 for 'bcabca' match.");
        assertEquals(6, result2.length(), "Length should be 6 for 'bcabca' match.");

        byte[] modified3 = "abcabca".getBytes();
        MatchResult result3 = automaton.findLongestMatch(modified3, 0);

        assertEquals(0, result3.offset(), "Offset should be 0 for 'abcabca' match.");
        assertEquals(7, result3.length(), "Length should be 7 for 'abcabca' match.");
    }

    /**
     * Test the compareSuffixDetailed method.
     */
    @Test
    @DisplayName("testCompareSuffixDetailed - Direct comparison test")
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

    /**
     * Test the compareSuffixDetailed and getMatchLength methods together.
     */
    @Test
    @DisplayName("testCompareSuffixDetailedAndGetMatchLength - Combined testing")
    void testCompareSuffixDetailedAndGetMatchLength() {
        // Given
        byte[] data = "banana".getBytes();
        SuffixArray suffixArray = new SuffixArray(data);
        int saPos = suffixArray.suffixArray[3]; // Expected to be position 1 ('banana' suffix)

        // When
        SuffixArray.CompareResult cr = suffixArray.compareSuffixDetailed("banana".getBytes(), 1, saPos);
        int matchLength = suffixArray.getMatchLength("banana".getBytes(), 1, 3);

        // Then
        assertTrue(cr.cmp() <= 1 && cr.cmp() >= -1, "compareSuffixDetailed result should be between -1 and 1.");
        assertTrue(cr.matchLen() >= 2, "Expected match length to be at least 2 characters.");
        assertTrue(matchLength >= 2, "Expected match length to be at least 2 characters.");
    }
}
