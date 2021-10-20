package com.leo.hekima.model;

import com.leo.hekima.utils.JsonUtils;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.util.ArrayList;

@ReadingConverter
public class NoteSubsReader implements Converter<Json, NoteSubs> {

    @Override
    public NoteSubs convert(Json raw) {
        final var files = JsonUtils.deserializeSilentFail(raw.asString(), NoteSubs.class);
        if(files == null) {
            return new NoteSubs(new ArrayList<>());
        }
        return files;
    }
}