package com.github.xesam.printing.cloud;

public interface ResponseParser {
    <T> T parse(String responseString, Class<T> klass);
}
