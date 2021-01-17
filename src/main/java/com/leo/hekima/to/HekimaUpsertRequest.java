package com.leo.hekima.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class HekimaUpsertRequest {
    private final String uri;
    private final String valeur;
    private final String source;
    private final List<String> tags;

    @JsonCreator
    public HekimaUpsertRequest(@JsonProperty("uri") String uri,
                               @JsonProperty("valeur") String valeur,
                               @JsonProperty("source") String source,
                               @JsonProperty("tags") List<String> tags) {
        this.uri = uri;
        this.valeur = valeur;
        this.source = source;
        this.tags = tags;
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

}
