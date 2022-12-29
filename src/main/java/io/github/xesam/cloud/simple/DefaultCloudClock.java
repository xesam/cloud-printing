package io.github.xesam.cloud.simple;

import io.github.xesam.cloud.CloudClock;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class DefaultCloudClock implements CloudClock {
    @Override
    public long getEpochSecond() {
        return LocalDateTime.now().atZone(ZoneOffset.systemDefault()).toEpochSecond();
    }
}
