package com.leo.hekima.to;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SourceView(@JsonProperty("uri") String uri,
                        @JsonProperty("titre") String titre,
                        @JsonProperty("titreRecherche") String titreRecherche,
                        @JsonProperty("auteur") String auteur,
                        @JsonProperty("type") String type) {
}
