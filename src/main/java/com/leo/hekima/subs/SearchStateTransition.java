package com.leo.hekima.subs;

import static com.leo.hekima.subs.SearchPattern.POS_TAG_NOT_WORD;
import static com.leo.hekima.utils.StringUtils.isNotEmpty;

public record SearchStateTransition(
    SearchState newState,
    String content,
    String tag) {
    public boolean canLemmeTransit(PosTag posTag) {
        boolean canDoIt = true;
        if (isNotEmpty(content)) {
            canDoIt = posTag.value().equals(content);
        }
        if(canDoIt && isNotEmpty(tag)) {
            // TODO check if the types for verbs and adjectives could be separated differently
            final String tagType = posTag.type();
            switch (tag) {
                case "VSTEM":
                case "ADJSTEM":
                    canDoIt = tagType.equals("V");
                    break;
                case "NSTEM":
                    canDoIt = tagType.equals("N");
                    break;
                case "VENDING":
                    canDoIt = tagType.equals("E");
                    break;
                case "WORDS":
                case "WORD":
                    canDoIt = !POS_TAG_NOT_WORD.contains(tagType);
                    break;
            }
        }
        return canDoIt;
    }
}
