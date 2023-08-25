package com.github.xesam.printing.cloud.simple;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xesam.printing.cloud.ResponseParser;

class SimpleResponseParser implements ResponseParser {
    private ObjectMapper objectMapper = new ObjectMapper();

    SimpleResponseParser() {
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public <T> T parse(String responseString, Class<T> klass) {
        try {
            return this.objectMapper.readValue(responseString, klass);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
