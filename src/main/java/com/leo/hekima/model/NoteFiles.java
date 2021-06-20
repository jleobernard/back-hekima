package com.leo.hekima.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public record NoteFiles(@JsonProperty("files") List<NoteFile> files) {
    public NoteFiles() {
        this(new ArrayList<>());
    }
}