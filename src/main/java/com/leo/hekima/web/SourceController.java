package com.leo.hekima.web;

import com.leo.hekima.service.SourceService;

public class SourceController {
    private final SourceService sourceService;

    public SourceController(SourceService sourceService) {
        this.sourceService = sourceService;
    }
}
