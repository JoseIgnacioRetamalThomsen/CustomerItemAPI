package com.billy.common;

import com.billy.factory.ObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public final class JsonUtils {
    private static final ObjectMapper MAPPER = ObjectMapperFactory.get();

    private JsonUtils() {
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON", e);
        }
    }

    public static <T> T fromJson(InputStream input, Class<T> clazz) {
        try {
            return MAPPER.readValue(input, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON from InputStream into " + clazz.getSimpleName(), e);
        }
    }

    public static <T> T fromJson(byte[] bytes, Class<T> clazz) {
        try {
            return MAPPER.readValue(bytes, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON from byte[] into " + clazz.getSimpleName(), e);
        }
    }

}
