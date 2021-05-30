package com.leo.hekima.to;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AckResponse(@JsonProperty("ok") boolean ok, @JsonProperty("message") String message){
    public static final AckResponse OK = new AckResponse(true, null);
}
