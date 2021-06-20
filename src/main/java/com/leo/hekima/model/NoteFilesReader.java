package com.leo.hekima.model;

import com.leo.hekima.utils.JsonUtils;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.util.ArrayList;

@ReadingConverter
public class NoteFilesReader implements Converter<Json, NoteFiles> {

    @Override
    public NoteFiles convert(Json raw) {
        final var files = JsonUtils.deserializeSilentFail(raw.asString(), NoteFiles.class);
        if(files == null) {
            return new NoteFiles(new ArrayList<>());
        }
        return files;
    }
}