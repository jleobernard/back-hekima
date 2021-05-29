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
    @Column("mime_type")
    private String mimeType;
    @Column("file_id")
    private String fileId;

    public NoteModel() {
    }

    public NoteModel(String uri) {
        this.uri = uri;
    }

    public NoteModel(Long id, String uri, String valeur, Instant createdAt, Long sourceId, String mimeType, String fileId) {
        this.id = id;
        this.uri = uri;
        this.valeur = valeur;
        this.createdAt = createdAt;
        this.sourceId = sourceId;
        this.mimeType = mimeType;
        this.fileId = fileId;
    }

    public NoteModel(String uri, String valeur, Instant createdAt, List<TagModel> tags, Long source,
                     String mimeType,
                     String fileId) {
        this.uri = uri;
        this.valeur = valeur;
        this.createdAt = createdAt;
        this.sourceId = source;
        this.mimeType = mimeType;
        this.fileId = fileId;
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

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
}
