package com.example.securedrive.service.util;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Suffix Automaton implementation for efficient string matching.
 * This class allows building a suffix automaton from a byte array and
 * finding the longest match for a substring within the original data.
 */
public class SuffixAutomaton {
    /**
     * State class represents a state in the suffix automaton.
     */
    private static class State {
        int len;
        int link;
        Map<Integer, Integer> next;
        int firstPos;

        State(int len, int link, int firstPos) {
            this.len = len;
            this.link = link;
            this.next = new HashMap<>();
            this.firstPos = firstPos;
        }
    }

    private final State[] st;
    private int size;
    private int last;

    /**
     * Constructs the suffix automaton for the given data.
     *
     * @param data The original byte array to build the automaton from.
     */
    public SuffixAutomaton(byte[] data) {
        st = new State[2 * data.length + 2];
        st[0] = new State(0, -1, -1);
        size = 1;
        last = 0;

        for (int i = 0; i < data.length; i++) {
            extend(data[i], i);
        }
    }

    /**
     * Extends the automaton with a new character.
     *
     * @param c   The new character to add (treated as unsigned).
     * @param pos The current position in the original string.
     */
    private void extend(byte c, int pos) {
        int uc = c & 0xFF;
        int curr = size++;
        st[curr] = new State(st[last].len + 1, 0, pos);
        int p = last;
        while (p != -1 && !st[p].next.containsKey(uc)) {
            st[p].next.put(uc, curr);
            p = st[p].link;
        }
        if (p == -1) {
            st[curr].link = 0;
        } else {
            int q = st[p].next.get(uc);
            if (st[p].len + 1 == st[q].len) {
                st[curr].link = q;
            } else {
                int clone = size++;
                st[clone] = new State(st[p].len + 1, st[q].link, st[q].firstPos);
                st[clone].next.putAll(st[q].next);
                while (p != -1 && st[p].next.get(uc) == q) {
                    st[p].next.put(uc, clone);
                    p = st[p].link;
                }
                st[q].link = clone;
                st[curr].link = clone;
            }
        }
        last = curr;
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

        int currentState = 0;
        int matchLength = 0;
        int bestLength = 0;
        int bestOffset = -1;

        for (int i = modPos; i < modified.length; i++) {
            int c = modified[i] & 0xFF;
            if (st[currentState].next.containsKey(c)) {
                currentState = st[currentState].next.get(c);
                matchLength++;
                if (matchLength > bestLength) {
                    bestLength = matchLength;
                    bestOffset = st[currentState].firstPos - matchLength + 1;
                }
            } else {
                break;
            }
        }

        return new MatchResult(bestOffset, bestLength);
    }

    /**
     * Match result class.
     */
    @Getter
    public static class MatchResult {
        private final int offset;
        private final int length;

        public MatchResult(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }

        @Override
        public String toString() {
            return "MatchResult{" +
                    "offset=" + offset +
                    ", length=" + length +
                    '}';
        }
    }
}
