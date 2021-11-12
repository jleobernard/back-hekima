package com.leo.hekima.subs;

import java.util.Objects;

public record PosTag(String value, String type) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PosTag posTag = (PosTag) o;
        return value.equals(posTag.value) && type.equals(posTag.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type);
    }
}
