package com.example.securedrive.service.util;

import lombok.Getter;


public class SuffixAutomaton {


    private static class State {
        int len;
        int link;
        int firstPos;


        private byte[] transKeys;
        private int[] transValues;
        private int transSize;
        State(int len, int link, int firstPos) {
            this.len = len;
            this.link = link;
            this.firstPos = firstPos;
            this.transKeys = null;
            this.transValues = null;
            this.transSize = 0;
        }


        int getNext(int c) {
            if (transKeys == null) return -1;
            for (int i = 0; i < transSize; i++) {
                if ((transKeys[i] & 0xFF) == c) {
                    return transValues[i];
                }
            }
            return -1;
        }

        void putTransition(int c, int state) {
            if (transKeys == null) {
                transKeys = new byte[2];
                transValues = new int[2];
            }
            for (int i = 0; i < transSize; i++) {
                if ((transKeys[i] & 0xFF) == c) {
                    transValues[i] = state;
                    return;
                }
            }
            if (transSize == transKeys.length) {
                byte[] newKeys = new byte[transKeys.length * 2];
                int[] newValues = new int[transValues.length * 2];
                System.arraycopy(transKeys, 0, newKeys, 0, transKeys.length);
                System.arraycopy(transValues, 0, newValues, 0, transValues.length);
                transKeys = newKeys;
                transValues = newValues;
            }
            transKeys[transSize] = (byte) c;
            transValues[transSize] = state;
            transSize++;
        }


        void copyTransitionsFrom(State other) {
            if (other.transKeys == null) {
                this.transKeys = null;
                this.transValues = null;
                this.transSize = 0;
            } else {
                this.transKeys = new byte[other.transSize];
                this.transValues = new int[other.transSize];
                System.arraycopy(other.transKeys, 0, this.transKeys, 0, other.transSize);
                System.arraycopy(other.transValues, 0, this.transValues, 0, other.transSize);
                this.transSize = other.transSize;
            }
        }
    }

    private final State[] st;
    private int size;
    private int last;


    public SuffixAutomaton(byte[] data) {
        st = new State[2 * data.length + 2];
        st[0] = new State(0, -1, -1);
        size = 1;
        last = 0;

        for (int i = 0; i < data.length; i++) {
            extend(data[i], i);
        }
    }


    private void extend(byte c, int pos) {
        int uc = c & 0xFF;
        int curr = size++;
        st[curr] = new State(st[last].len + 1, 0, pos);

        int p = last;
        while (p != -1 && st[p].getNext(uc) == -1) {
            st[p].putTransition(uc, curr);
            p = st[p].link;
        }
        if (p == -1) {
            st[curr].link = 0;
        } else {
            int q = st[p].getNext(uc);
            if (st[p].len + 1 == st[q].len) {
                st[curr].link = q;
            } else {
                int clone = size++;
                st[clone] = new State(st[p].len + 1, st[q].link, st[q].firstPos);
                st[clone].copyTransitionsFrom(st[q]);
                while (p != -1 && st[p].getNext(uc) == q) {
                    st[p].putTransition(uc, clone);
                    p = st[p].link;
                }
                st[q].link = clone;
                st[curr].link = clone;
            }
        }
        last = curr;
    }


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
            int nextState = st[currentState].getNext(c);
            if (nextState != -1) {
                currentState = nextState;
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
