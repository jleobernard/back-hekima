package com.leo.hekima.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("note_quizz_histo")
public class NoteQuizzHistoryModel {
    @Id
    private Long id;
    @Column("note_id")
    private Long noteId;
    @Column("created_at")
    private Instant createdAt;
    private float score;

    public NoteQuizzHistoryModel(Long noteId, final float score) {
        this.noteId = noteId;
        this.score = score;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }
}
