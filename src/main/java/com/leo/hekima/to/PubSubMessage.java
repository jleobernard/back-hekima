package com.leo.hekima.to;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PubSubMessage(@JsonProperty("uri") String uri, @JsonProperty("type") String type){
}
