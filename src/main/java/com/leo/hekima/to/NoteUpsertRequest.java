package com.leo.hekima.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.leo.hekima.model.NoteSub;

import java.util.List;

public class NoteUpsertRequest {
    private final String uri;
    private final String valeur;
    private final String source;
    private final List<String> tags;
    private final List<NoteSub> subs;

    @JsonCreator
    public NoteUpsertRequest(@JsonProperty("uri") String uri,
                             @JsonProperty("valeur") String valeur,
                             @JsonProperty("source") String source,
                             @JsonProperty("tags") List<String> tags,
                             @JsonProperty("subs") List<NoteSub> subs) {
        this.uri = uri;
        this.valeur = valeur;
        this.source = source;
        this.tags = tags;
        this.subs = subs;
    }

    public String getUri() {
        return uri;
    }

    public String getValeur() {
        return valeur;
    }

    public String getSource() {
        return source;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<NoteSub> getSubs() {
        return subs;
    }
}
