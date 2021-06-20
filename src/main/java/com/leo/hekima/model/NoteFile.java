package com.leo.hekima.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NoteFile(
        @JsonProperty("mime_type") String mimeType,
        @JsonProperty("file_id") String fileId
) {
}
