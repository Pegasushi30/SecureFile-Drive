package com.example.securedrive.service.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SuffixAutomatonTest {

    @Test
    void givenSuffixAutomaton_whenConstructed_thenConstructedSuccessfully() {
        // Given
        byte[] data = "exampledata".getBytes();

        // When
        SuffixAutomaton automaton = new SuffixAutomaton(data);

        // Then
        assertNotNull(automaton, "Automaton should be constructed successfully");
    }

    @Test
    void givenExactMatch_whenFindingLongestMatch_thenReturnsCorrectOffsetAndLength() {
        // Given
        byte[] data = "exampledata".getBytes();
        byte[] modified = "exampledata".getBytes();
        SuffixAutomaton automaton = new SuffixAutomaton(data);

        // When
        SuffixAutomaton.MatchResult result = automaton.findLongestMatch(modified, 0);

        // Then
        assertEquals(0, result.getOffset(), "Offset should be 0 for a full match");
        assertEquals(modified.length, result.getLength(), "Length should match the full string length");
    }

    @Test
    void givenPartialMatch_whenFindingLongestMatch_thenReturnsCorrectOffsetAndLength() {
        // Given
        byte[] data = "exampledata".getBytes();
        byte[] modified = "exam".getBytes();
        SuffixAutomaton automaton = new SuffixAutomaton(data);

        // When
        SuffixAutomaton.MatchResult result = automaton.findLongestMatch(modified, 0);

        // Then
        assertEquals(0, result.getOffset(), "Offset should be 0 for a partial match at the start");
        assertEquals(modified.length, result.getLength(), "Length should match the partial match length");
    }

    @Test
    void givenNoMatch_whenFindingLongestMatch_thenReturnsZeroLength() {
        // Given
        byte[] data = "exampledata".getBytes();
        byte[] modified = "nomatch".getBytes();
        SuffixAutomaton automaton = new SuffixAutomaton(data);

        // When
        SuffixAutomaton.MatchResult result = automaton.findLongestMatch(modified, 0);

        // Then
        assertEquals(-1, result.getOffset(), "Offset should be -1 for no match");
        assertEquals(0, result.getLength(), "Length should be 0 for no match");
    }

    @Test
    void givenMiddlePosition_whenFindingLongestMatch_thenReturnsCorrectOffsetAndLength() {
        // Given
        byte[] data = "exampledata".getBytes();
        byte[] modified = "pledata".getBytes();
        SuffixAutomaton automaton = new SuffixAutomaton(data);

        // When
        SuffixAutomaton.MatchResult result = automaton.findLongestMatch(modified, 2);

        // Then
        assertEquals(6, result.getOffset(), "Offset should be 6 for the match starting from 'pledata'");
        assertEquals(modified.length - 2, result.getLength(), "Length should match the portion of the string matched");
    }


    @Test
    void givenInvalidPosition_whenFindingLongestMatch_thenReturnsInvalidResult() {
        // Given
        byte[] data = "exampledata".getBytes();
        byte[] modified = "exampledata".getBytes();
        SuffixAutomaton automaton = new SuffixAutomaton(data);

        // When
        SuffixAutomaton.MatchResult result = automaton.findLongestMatch(modified, -1);

        // Then
        assertEquals(-1, result.getOffset(), "Offset should be -1 for an invalid position");
        assertEquals(0, result.getLength(), "Length should be 0 for an invalid position");
    }

    @Test
    void givenAutomaton_whenEmptyData_thenHandlesGracefully() {
        // Given
        byte[] data = "".getBytes();
        byte[] modified = "data".getBytes();

        // When
        SuffixAutomaton automaton = new SuffixAutomaton(data);
        SuffixAutomaton.MatchResult result = automaton.findLongestMatch(modified, 0);

        // Then
        assertEquals(-1, result.getOffset(), "Offset should be -1 for empty data");
        assertEquals(0, result.getLength(), "Length should be 0 for empty data");
    }
}

