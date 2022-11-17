package com.github.lc.oss.commons.web.services;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.lc.oss.commons.serialization.Jsonable;

public class JsonService implements com.github.lc.oss.commons.api.services.JsonService {
    private static final ObjectWriter WRITER = new ObjectMapper().writer();
    private static final ObjectReader READER = new ObjectMapper().reader();

    public <T extends Jsonable> T from(String json, Class<T> clazz) {
        if (json == null || json.trim().equals("")) {
            return null;
        }

        try {
            return JsonService.READER.readValue(json, clazz);
        } catch (IOException ex) {
            throw new RuntimeException("Error deseralizing from JSON", ex);
        }
    }

    public String to(Jsonable object) {
        try {
            return JsonService.WRITER.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Error seralizing to JSON", ex);
        }
    }
}
