package io.github.xesam.cloud;

public interface ResponseParser {
    <T> T parse(String responseString, Class<T> klass);
}
