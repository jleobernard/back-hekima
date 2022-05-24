package com.leo.hekima.to;

import com.leo.hekima.subs.SearchableType;

import java.util.Optional;

public record SubsSearchPatternElement(Optional<String[]> alternatives, Optional<SearchableType> posTag) { }
