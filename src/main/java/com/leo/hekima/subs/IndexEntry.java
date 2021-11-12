package com.leo.hekima.subs;

import java.util.Objects;

public record IndexEntry(int sentenceIndex, int tagIndex) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexEntry that = (IndexEntry) o;
        return sentenceIndex == that.sentenceIndex && tagIndex == that.tagIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sentenceIndex, tagIndex);
    }
}
