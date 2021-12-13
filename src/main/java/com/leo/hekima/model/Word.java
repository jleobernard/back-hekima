package com.leo.hekima.model;

import java.util.Objects;

public record Word(String word, Language language) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Word word1 = (Word) o;
        return word.equals(word1.word) && language == word1.language;
    }

    @Override
    public int hashCode() {
        return Objects.hash(word, language);
    }
}
