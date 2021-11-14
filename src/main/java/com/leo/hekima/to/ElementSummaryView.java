package com.leo.hekima.to;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ElementSummaryView(
        @JsonProperty("uri")String uri,
        @JsonProperty("name")String name) { }
