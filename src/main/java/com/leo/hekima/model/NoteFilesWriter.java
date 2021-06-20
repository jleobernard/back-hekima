package com.leo.hekima.model;

import com.leo.hekima.utils.JsonUtils;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.util.ArrayList;

@WritingConverter
public class NoteFilesWriter implements Converter<NoteFiles, Json> {

    @Override
    public Json convert(NoteFiles noteFiles) {
        if(noteFiles == null) {
            return Json.of(JsonUtils.serializeSilentFail(new NoteFiles(new ArrayList<>())));
        }
        return Json.of(JsonUtils.serializeSilentFail(noteFiles));
    }
}