package com.leo.hekima.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MyPage(@JsonProperty("lines") List<String> blocks) {
}

