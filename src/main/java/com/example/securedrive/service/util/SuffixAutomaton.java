package com.example.securedrive.service.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced Suffix Automaton Implementation with Position Mapping
 */
public class SuffixAutomaton {
    private static class State {
        int len;
        int link;
        Map<Byte, Integer> next = new HashMap<>();
        int firstPos; // İlk ortaya çıktığı pozisyon
    }

    private final State[] st;
    private int size;
    private int last;

    /**
     * Constructs the Suffix Automaton for the given data.
     *
     * @param s The original data as a byte array
     */
    public SuffixAutomaton(byte[] s) {
        st = new State[2 * s.length + 2];
        for (int i = 0; i < st.length; i++) {
            st[i] = new State();
        }
        st[0].len = 0;
        st[0].link = -1;
        st[0].firstPos = -1; // Başlangıçta geçerli bir pozisyon yok
        size = 1;
        last = 0;
        for (int i = 0; i < s.length; i++) {
            extend(s[i], i); // 0 tabanlı pozisyon
        }
    }

    /**
     * Extends the automaton with a new character and its position.
     *
     * @param c        The new character to add
     * @param position The position of the new character in the original string (0-based)
     */
    private void extend(byte c, int position) {
        int curr = size++;
        st[curr].len = st[last].len + 1;
        st[curr].firstPos = position; // 'position' parametresi kullanılıyor
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
                st[clone].len = st[p].len + 1;
                st[clone].next.putAll(st[q].next);
                st[clone].link = st[q].link;
                st[clone].firstPos = st[q].firstPos;
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
     * Finds the longest match in the original data for the data starting at position in the new data.
     * This method runs in O(n) time for the entire modified data.
     *
     * @param newData     The modified data as a byte array
     * @param startPosNew The starting position in the modified data
     * @return MatchResult containing the offset and length of the longest match
     */
    public MatchResult findLongestMatch(byte[] newData, int startPosNew) {
        if (startPosNew < 0 || startPosNew >= newData.length) {
            return new MatchResult(-1, 0);
        }

        int currentState = 0;
        int currentLength = 0;
        int bestLength = 0;
        int bestOffset = -1;

        for (int i = startPosNew; i < newData.length; i++) {
            byte c = newData[i];
            if (st[currentState].next.containsKey(c)) {
                currentState = st[currentState].next.get(c);
                currentLength++;
                if (currentLength > bestLength) {
                    bestLength = currentLength;
                    bestOffset = st[currentState].firstPos - bestLength + 1; // Doğru offset hesaplaması
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
                        bestOffset = st[currentState].firstPos - bestLength + 1; // Doğru offset hesaplaması
                    }
                }
            }
        }

        return new MatchResult(bestOffset, bestLength);
    }

    /**
     * MatchResult: Stores the offset and length of the match.
     */
    public record MatchResult(int offset, int length) {}
}
