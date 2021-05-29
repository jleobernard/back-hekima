package com.leo.hekima.utils.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MyLocalDateSerializer extends JsonSerializer<LocalDate>
{
    private static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter
            .ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.of("UTC"));

    @Override
    public void serialize(LocalDate value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException
    {
        if (value == null) {
            throw new IOException("OffsetDateTime argument is null.");
        }

        jsonGenerator.writeString(ISO_8601_FORMATTER.format(value));
    }
}

