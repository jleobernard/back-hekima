package com.leo.hekima.web;

import com.leo.hekima.service.TagService;

public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }
}
