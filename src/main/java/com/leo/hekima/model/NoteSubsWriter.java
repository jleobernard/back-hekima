package com.leo.hekima.model;

import com.leo.hekima.utils.JsonUtils;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.util.ArrayList;

@WritingConverter
public class NoteSubsWriter implements Converter<NoteSubs, Json> {

    @Override
    public Json convert(NoteSubs NoteSubs) {
        if(NoteSubs == null) {
            return Json.of(JsonUtils.serializeSilentFail(new NoteSubs(new ArrayList<>())));
        }
        return Json.of(JsonUtils.serializeSilentFail(NoteSubs));
    }
}