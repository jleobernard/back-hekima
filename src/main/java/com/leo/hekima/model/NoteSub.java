package com.leo.hekima.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NoteSub(
        @JsonProperty("name") String name,
        @JsonProperty("from") Integer from,
        @JsonProperty("to") Integer to
) {
}
