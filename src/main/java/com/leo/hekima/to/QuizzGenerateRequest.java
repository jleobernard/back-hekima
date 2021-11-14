package com.leo.hekima.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public record QuizzGenerateRequest(@JsonProperty("sources") Set<String> sources,
                                   @JsonProperty("tags") Set<String> tags) {
}

