package com.leo.hekima.to;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshRequest(@JsonProperty("refreshToken") String refreshToken) {
}
