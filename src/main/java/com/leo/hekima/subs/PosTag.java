package com.leo.hekima.subs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public record PosTag(@JsonProperty("value") String value, @JsonProperty("type") String type) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PosTag posTag = (PosTag) o;
        return typeToKey(this).equals(typeToKey(posTag)) && value.equals(posTag.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeToKey(this), value);
    }

    public static String typeToKey(final PosTag pt) {
        return pt == null ? "" : pt.type.substring(0, Math.min(pt.type.length(), 2));
    }
}
