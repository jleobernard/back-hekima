package com.leo.hekima.to;

public record SubsSearchRequest(String q, boolean exact, SubsSearchPatternElement[] pattern) {
}
