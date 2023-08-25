package com.github.xesam.printing.cloud;

public final class CloudAuth {
    private final String appId;
    private final String secret;

    public CloudAuth(String appId, String secret) {
        this.appId = appId;
        this.secret = secret;
    }

    public String getAppId() {
        return appId;
    }

    public String getSecret() {
        return secret;
    }
}
