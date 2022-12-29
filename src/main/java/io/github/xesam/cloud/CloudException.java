package io.github.xesam.cloud;

public class CloudException extends RuntimeException {
    public CloudException() {
    }

    public CloudException(String message) {
        super(message);
    }

    public CloudException(Throwable cause) {
        super(cause);
    }

    public CloudException(String message, Throwable cause) {
        super(message, cause);
    }
}
