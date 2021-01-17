package com.leo.hekima.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Node("Hekima")
public class HekimaModel {
    @Id
    private String uri;
    private String valeur;
    private long createdAt;
    @Relationship(type = "TAG", direction = Relationship.Direction.OUTGOING)
    private List<HekimaTagModel> tags;
    @Relationship(type = "SOURCE", direction = Relationship.Direction.OUTGOING)
    private HekimaSourceModel source;
    private String mimeType;
    private String fileId;

    public HekimaModel() {
    }

    public HekimaModel(String uri) {
        this.uri = uri;
    }
    public HekimaModel(String uri, String valeur, long createdAt, List<HekimaTagModel> tags, HekimaSourceModel source,
                       String mimeType,
                       String fileId) {
        this.uri = uri;
        this.valeur = valeur;
        this.createdAt = createdAt;
        this.tags = tags;
        this.source = source;
        this.mimeType = mimeType;
        this.fileId = fileId;
    }

    public String getUri() {
        return uri;
    }

    public String getValeur() {
        return valeur;
    }

    public void setValeur(String valeur) {
        this.valeur = valeur;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public List<HekimaTagModel> getTags() {
        return tags;
    }

    public void setTags(List<HekimaTagModel> tags) {
        this.tags = tags;
    }

    public HekimaSourceModel getSource() {
        return source;
    }

    public void setSource(HekimaSourceModel source) {
        this.source = source;
    }

    public void setUri(String uri) {
        this.uri = uri;
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
