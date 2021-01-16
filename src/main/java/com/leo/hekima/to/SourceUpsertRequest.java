package com.leo.hekima.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SourceUpsertRequest {
    private final String type;
    private final String auteur;
    private final String titre;

    @JsonCreator
    public SourceUpsertRequest(@JsonProperty("type") String type,
                               @JsonProperty("auteur") String auteur,
                               @JsonProperty("titre") String titre) {
        this.type = type;
        this.auteur = auteur;
        this.titre = titre;
    }

    public String getType() {
        return type;
    }

    public String getAuteur() {
        return auteur;
    }

    public String getTitre() {
        return titre;
    }
}
