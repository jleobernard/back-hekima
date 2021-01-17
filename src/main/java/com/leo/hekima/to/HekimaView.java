package com.leo.hekima.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class HekimaView {

    private final String uri;
    private final String valeur;
    private final long createdAt;
    private final List<TagView> tags;
    private final SourceView source;
    private final boolean hasFile;

    @JsonCreator
    public HekimaView(@JsonProperty("uri") String uri,
                      @JsonProperty("valeur") String valeur,
                      @JsonProperty("createdAt") long createdAt,
                      @JsonProperty("tags") List<TagView> tags,
                      @JsonProperty("source") SourceView source,
                      @JsonProperty("hasFile") boolean hasFile) {
        this.uri = uri;
        this.valeur = valeur;
        this.createdAt = createdAt;
        this.tags = tags;
        this.source = source;
        this.hasFile = hasFile;
    }

    public String getUri() {
        return uri;
    }

    public String getValeur() {
        return valeur;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public List<TagView> getTags() {
        return tags;
    }

    public SourceView getSource() {
        return source;
    }

    public boolean isHasFile() {
        return hasFile;
    }
}
