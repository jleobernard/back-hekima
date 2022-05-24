package com.leo.hekima.to;

import com.leo.hekima.subs.SearchableType;

import java.util.Optional;

public record NoAlternativeSubsSearchPatternElement(Optional<String> value, Optional<SearchableType> posTag) {

    @Override
    public String toString() {
        return "NoAlternativeSubsSearchPatternElement{" +
            "value=" + value +
            ", posTag=" + posTag +
            '}';
    }
}
