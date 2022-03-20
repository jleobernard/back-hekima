package com.leo.hekima.to;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthenticationRequest(@JsonProperty("username") String username, @JsonProperty("password") String password) {
}
