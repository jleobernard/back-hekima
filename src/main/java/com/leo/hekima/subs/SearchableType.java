package com.leo.hekima.subs;

import java.util.Optional;

public enum SearchableType {
    VERB_STEM("V"),
    NOUN("N");

    private final String type;

    SearchableType(String type) {
        this.type = type;
    }

    public static Optional<SearchableType> fromTag(String posTag) {
        return switch (posTag) {
            case "V" -> Optional.of(SearchableType.VERB_STEM);
            case "N" -> Optional.of(SearchableType.NOUN);
            default -> Optional.empty();
        };
    }

    public String getType() {
        return type;
    }
}
