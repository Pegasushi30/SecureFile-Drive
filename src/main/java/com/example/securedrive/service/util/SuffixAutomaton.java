package com.example.securedrive.service.util;

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
        int len; // Maximum length of the substring ending at this state
        int link; // Suffix link
        Map<Byte, Integer> next; // Transitions (character to state)
        int firstPos; // The first ending position of the substring in the original string

        State(int len, int link, int firstPos) {
            this.len = len;
            this.link = link;
            this.next = new HashMap<>();
            this.firstPos = firstPos;
        }
    }

    private final State[] st; // Array of states
    private int size; // Number of states
    private int last; // The index of the state representing the entire string

    /**
     * Constructs the suffix automaton for the given data.
     *
     * @param data The original byte array to build the automaton from.
     */
    public SuffixAutomaton(byte[] data) {
        // Maximum number of states is 2 * n + 2
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
     * @param c   The new character to add.
     * @param pos The current position in the original string.
     */
    private void extend(byte c, int pos) {
        int curr = size++;
        st[curr] = new State(st[last].len + 1, 0, pos);
        int p = last;
        while (p != -1 && !st[p].next.containsKey(c)) {
            st[p].next.put(c, curr);
            p = st[p].link;
        }
        if (p == -1) {
            st[curr].link = 0;
        } else {
            int q = st[p].next.get(c);
            if (st[p].len + 1 == st[q].len) {
                st[curr].link = q;
            } else {
                int clone = size++;
                st[clone] = new State(st[p].len + 1, st[q].link, st[q].firstPos);
                st[clone].next.putAll(st[q].next);
                while (p != -1 && st[p].next.get(c) == q) {
                    st[p].next.put(c, clone);
                    p = st[p].link;
                }
                st[q].link = clone;
                st[curr].link = clone;
            }
        }
        last = curr;
    }

    /**
     * Find the longest match for the substring starting at modPos in the modified data.
     *
     * @param modified The modified byte array to match.
     * @param modPos   The starting position in the modified array.
     * @return MatchResult containing the offset and length of the match.
     */
    public MatchResult findLongestMatch(byte[] modified, int modPos) {
        if (modPos < 0 || modPos >= modified.length) {
            return new MatchResult(-1, 0);
        }

        int currentState = 0;
        int currentLength = 0;
        int bestLength = 0;
        int bestOffset = -1;

        for (int i = modPos; i < modified.length; i++) {
            byte c = modified[i];
            if (st[currentState].next.containsKey(c)) {
                currentState = st[currentState].next.get(c);
                currentLength++;
                if (currentLength > bestLength) {
                    bestLength = currentLength;
                    bestOffset = st[currentState].firstPos - bestLength + 1;
                }
            } else {
                while (currentState != -1 && !st[currentState].next.containsKey(c)) {
                    currentState = st[currentState].link;
                }
                if (currentState == -1) {
                    currentState = 0;
                    currentLength = 0;
                } else {
                    currentLength = st[currentState].len + 1;
                    currentState = st[currentState].next.get(c);
                    if (currentLength > bestLength) {
                        bestLength = currentLength;
                        bestOffset = st[currentState].firstPos - bestLength + 1;
                    }
                }
            }
        }

        return new MatchResult(bestOffset, bestLength);
    }
}
