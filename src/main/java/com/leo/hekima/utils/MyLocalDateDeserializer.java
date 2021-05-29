package com.leo.hekima.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MyLocalDateDeserializer extends JsonDeserializer<LocalDate>
{
    private static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter
            .ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.of("UTC"));

    public static LocalDate deserialize(final String str) {
        return LocalDate.from(ISO_8601_FORMATTER.parse(str));
    }

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        if (parser.hasToken(JsonToken.VALUE_STRING)) {
            String string = parser.getText().trim();
            return LocalDate.from(ISO_8601_FORMATTER.parse(string));
        }
        if (parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return LocalDate.ofEpochDay(parser.getLongValue());
        }
        return null;
    }
}

