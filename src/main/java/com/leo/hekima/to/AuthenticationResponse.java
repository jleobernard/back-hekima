package com.leo.hekima.to;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthenticationResponse(
        @JsonProperty("user") String user,
        @JsonProperty("accessToken") String accessToken) {
}
