package com.leo.hekima.subs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record Sentence(@JsonProperty("elements") List<SentenceElement> elements) {
    @Override
    public String toString() {
        return "Sentence{" +
            "elements=" + elements +
            '}';
    }
}
