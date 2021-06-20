package com.leo.hekima.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.List;

@Table("note")
public class NoteModel {
    @Id
    private Long id;
    private String uri;
    private String valeur;
    @Column("created_at")
    private Instant createdAt;
    @Column("source_id")
    private Long sourceId;
    private NoteFiles files;

    public NoteModel() {
        this.files = new NoteFiles();
    }

    public NoteModel(String uri) {
        this.uri = uri;
        this.files = new NoteFiles();
    }

    public NoteModel(Long id, String uri, String valeur, Instant createdAt, Long sourceId, NoteFiles files) {
        this.id = id;
        this.uri = uri;
        this.valeur = valeur;
        this.createdAt = createdAt;
        this.sourceId = sourceId;
        this.files = files;
    }

    public NoteModel(String uri, String valeur, Instant createdAt, List<TagModel> tags, Long source,
                     NoteFiles files) {
        this.uri = uri;
        this.valeur = valeur;
        this.createdAt = createdAt;
        this.sourceId = source;
        this.files = files;
    }

    public String getUri() {
        return uri;
    }

    public String getValeur() {
        return valeur;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setValeur(String valeur) {
        this.valeur = valeur;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public NoteFiles getFiles() {
        return files;
    }

    public void setFiles(NoteFiles files) {
        this.files = files;
    }
}
