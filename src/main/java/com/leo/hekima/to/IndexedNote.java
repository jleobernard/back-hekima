package com.leo.hekima.to;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IndexedNote(@JsonProperty("uri") String uri) {

}
