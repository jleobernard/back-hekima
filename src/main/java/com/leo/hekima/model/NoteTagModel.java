package com.leo.hekima.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("note_tag")
public class NoteTagModel {
    @Id
    private Long id;
    @Column("note_id")
    private long noteId;
    @Column("tag_id")
    private long tagId;

    public NoteTagModel() {
    }

    public NoteTagModel(long noteId, long tagId) {
        this.noteId = noteId;
        this.tagId = tagId;
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

    public long getTagId() {
        return tagId;
    }

    public void setTagId(long tagId) {
        this.tagId = tagId;
    }
}
