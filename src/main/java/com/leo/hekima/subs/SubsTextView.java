package com.leo.hekima.subs;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SubsTextView(@JsonProperty("text")String text,
                           @JsonProperty("from")float from,
                           @JsonProperty("to")float to) {
}
