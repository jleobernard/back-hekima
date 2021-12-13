package com.leo.hekima.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

@Table("word")
public class WordModel {
    @Id
    private Long id;
    private String word;
    private Language language;

    public WordModel() {
    }

    public WordModel(String word, Language language) {
        this.word = word;
        this.language = language;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WordModel wordModel = (WordModel) o;
        return word.equals(wordModel.word) && language == wordModel.language;
    }

    @Override
    public int hashCode() {
        return Objects.hash(word, language);
    }
}
