package com.leo.hekima.subs;

import java.util.List;

public record SubsDbEntry(String videoName,
                          String subs,
                          float fromTs,
                          float toTs,
                          List<PosTag> tags) {
    public boolean hasEveryWord(SubsDbEntry entry, List<PosTag> fixWords) {
        for (PosTag fixWord : fixWords) {
            var found = false;
            for (PosTag tag : entry.tags) {
                if(tag.value().equals(fixWord.value())) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                return false;
            }
        }
        return true;
    }
}
