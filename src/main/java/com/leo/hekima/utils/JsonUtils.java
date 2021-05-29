package com.leo.hekima.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.leo.hekima.exception.UnrecoverableServiceException;
import com.leo.hekima.utils.json.MyLocalDateSerializer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class JsonUtils {
    private JsonUtils() {}

    public static ObjectMapper mapper = new ObjectMapper();

    static {
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new MyLocalDateSerializer());
        //module.addSerializer(new LocalDateSerializer(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        module.addSerializer(new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("dd/MM/yyyy'T'HH:mm:ss")));
        module.addDeserializer(LocalDate.class, new MyLocalDateDeserializer());
        mapper.registerModule(module);
        mapper.registerModule(new Jdk8Module());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    }

    /**
     * Convert an object to JSON byte array.
     *
     * @param object
     *            the object to convert
     * @return the JSON byte array
     * @throws IOException
     */
    public static byte[] convertObjectToJsonBytes(Object object)
            throws IOException {
        return mapper.writeValueAsBytes(object);
    }

    public static <T> T deserialize(final String json, final Class<T> clazz) throws IOException {
        return mapper.readValue(json, clazz);
    }

    public static JsonNode deserializeSilentFail(final String json) {
        try {
            return mapper.readTree(json);
        } catch (IOException e) {
            throw new UnrecoverableServiceException(e);
        }
    }

    public static <T> T deserializeSilentFail(final String json, final Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new UnrecoverableServiceException(e);
        }
    }

    public static <T> T deserialize(final String json, final TypeReference<T> typeReference) throws IOException {
        if(json == null)  {
            return null;
        }
        return mapper.readValue(json, typeReference);
    }

    public static <T> T deserializeSilentFail(final String json, final TypeReference<T> typeReference) {
        if(json == null)  {
            return null;
        }
        try {
            return mapper.readValue(json, typeReference);
        } catch (IOException e) {
            throw new UnrecoverableServiceException(e);
        }
    }

    public static <T> T parseJson(final String json, final Class<T> clazz) throws IOException {
        return mapper.readValue(new File(json), clazz);
    }

    public static <T> JsonNode parseJson(final String json) throws IOException {
        return mapper.readTree(json);
    }

    public static <T> T parseJson(final File f, final Class<T> clazz) throws IOException {
        return mapper.readValue(f, clazz);
    }

    public static <T> T parseJson(InputStream resourceAsStream, Class<T> clazz) throws IOException {
        return mapper.readValue(resourceAsStream, clazz);
    }

    public static <T> T parseJson(InputStream resourceAsStream, final TypeReference<T> typeReference) throws IOException {
        return mapper.readValue(resourceAsStream, typeReference);
    }

    public static <T> T parseJson(final File file, final TypeReference<T> typeReference) throws IOException {
        return mapper.readValue(file, typeReference);
    }

    public static <T> T parseJson(final String data, final TypeReference<T> typeReference) throws IOException {
        return mapper.readValue(data, typeReference);
    }

    public static <T> String serialize(final Object source) throws IOException {
        return mapper.writeValueAsString(source);
    }

    public static <T> String serializeSilentFail(final Object source) {
        try {
            return mapper.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            throw new UnrecoverableServiceException(e);
        }
    }

    public static <T> String serialize(T source, boolean throwException) throws UncheckedIOException {
        try {
            return mapper.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            if (throwException) {
                throw new UncheckedIOException(e);
            } else {
                return "{}";
            }
        }
    }

    public static int getIntOrDefaultValue(final String fieldName, final JsonNode node, final int defaultValue) {
        JsonNode fieldValue = node.get(fieldName);
        return fieldValue == null ? defaultValue : fieldValue.asInt(defaultValue);
    }

    public static boolean getBoolOrDefaultValue(final String fieldName, final JsonNode node, final boolean defaultValue) {
        JsonNode fieldValue = node.get(fieldName);
        return fieldValue == null ? defaultValue : fieldValue.asBoolean(defaultValue);
    }

    public static void serializeToFile(final File f, final Object data, final boolean override) throws IOException {
        if(f.exists()) {
            if(override) {
                f.delete();
                f.createNewFile();
            } else {
                throw new FileAlreadyExistsException("file.exists");
            }
        } else {
            f.createNewFile();
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(f, data);
    }
    public static void serializeToFileSilentFail(final File f, final Object data, final boolean override) {
        try {
            serializeToFile(f, data, override);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
