package com.leo.hekima.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public record NoteSubs(@JsonProperty("subs") List<NoteSub> subs) {
    public NoteSubs() {
        this(new ArrayList<>());
    }
}