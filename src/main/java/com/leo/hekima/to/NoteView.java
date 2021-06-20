package com.leo.hekima.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.leo.hekima.model.NoteFile;

import java.util.List;

public record NoteView(@JsonProperty("uri") String uri,
                       @JsonProperty("valeur") String valeur,
                       @JsonProperty("tags") List<TagView> tags,
                       @JsonProperty("source") SourceView source,
                       @JsonProperty("files") List<NoteFile> files) {
}
