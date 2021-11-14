package com.leo.hekima.to;

import com.fasterxml.jackson.annotation.JsonProperty;

public record QuizzAnswerRequest(@JsonProperty("noteUri") String noteUri,
                                 @JsonProperty("score") float score) {
}

