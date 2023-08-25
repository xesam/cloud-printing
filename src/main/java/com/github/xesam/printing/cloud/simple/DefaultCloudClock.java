package com.github.xesam.printing.cloud.simple;

import com.github.xesam.printing.cloud.CloudClock;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class DefaultCloudClock implements CloudClock {
    @Override
    public long getEpochSecond() {
        return LocalDateTime.now().atZone(ZoneOffset.systemDefault()).toEpochSecond();
    }
}
