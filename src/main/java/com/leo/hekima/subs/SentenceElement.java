package com.leo.hekima.subs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Optional;

public record SentenceElement(@JsonProperty("value") Optional<String> value,
                              @JsonProperty("type") Optional<String> type) {

    public SentenceElement(String value, String type) {
        this(Optional.ofNullable(value), Optional.ofNullable(type));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SentenceElement that = (SentenceElement) o;
        return value.equals(that.value) && typeToKey(this).equals(typeToKey(that));
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.orElse(""), typeToKey(this));
    }

    public static String typeToKey(final SentenceElement pt) {
        return pt == null ? "" : pt.type.map(type -> type.substring(0, Math.min(type.length(), 1))).orElse("");
    }

    @Override
    public String toString() {
        return "SentenceElement{" +
            "value=" + value +
            ", type=" + type +
            '}';
    }
}
