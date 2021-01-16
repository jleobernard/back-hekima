package com.leo.hekima.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TagUpserRequest {
    private final String valeur;

    @JsonCreator
    public TagUpserRequest(@JsonProperty("valeur") String valeur) {
        this.valeur = valeur;
    }

    public String getValeur() {
        return valeur;
    }
}
