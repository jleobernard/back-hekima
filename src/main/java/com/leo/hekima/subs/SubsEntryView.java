package com.leo.hekima.subs;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SubsEntryView(@JsonProperty("name") String name,
                            @JsonProperty("subs")String subs,
                            @JsonProperty("from")float from,
                            @JsonProperty("to")float to) {
}
