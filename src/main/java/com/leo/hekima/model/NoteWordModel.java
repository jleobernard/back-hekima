package com.leo.hekima.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

@Table("note_word")
public class NoteWordModel {
    @Id
    private Long id;
    @Column("note_id")
    private long noteId;
    @Column("word_id")
    private long wordId;

    public NoteWordModel() {
    }

    public NoteWordModel(long noteId, long wordId) {
        this.noteId = noteId;
        this.wordId = wordId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public long getNoteId() {
        return noteId;
    }

    public void setNoteId(long noteId) {
        this.noteId = noteId;
    }

    public long getWordId() {
        return wordId;
    }

    public void setWordId(long wordId) {
        this.wordId = wordId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoteWordModel that = (NoteWordModel) o;
        return noteId == that.noteId && wordId == that.wordId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(noteId, wordId);
    }
}
