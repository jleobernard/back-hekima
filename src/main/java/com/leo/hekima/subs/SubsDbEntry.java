package com.leo.hekima.subs;

import java.util.List;

public record SubsDbEntry(String videoName,
                          String subs,
                          float fromTs,
                          float toTs,
                          List<SentenceElement> tags) {
    /*public boolean hasEveryWord(SubsDbEntry entry, List<SentenceElement> fixWords) {
        for (SentenceElement fixWord : fixWords) {
            if(fixWord.value().isEmpty()) {
                continue;;
            }
            var found = false;
            for (SentenceElement tag : entry.tags) {
                final var fixedValues = fixWord.value().get();
                for (String fixedValue : fixedValues) {
                    // We only test the first element because db entries surely has no alternatives
                    if(tag.value().get()[0].equals(fixedValue)) {
                        found = true;
                        break;
                    }
                }
            }
            if(!found) {
                return false;
            }
        }
        return true;
    }*/
}
