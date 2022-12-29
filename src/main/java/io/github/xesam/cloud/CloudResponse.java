package io.github.xesam.cloud;

public class CloudResponse<T> {
    public static <T> CloudResponse<T> ofSuccess(T body) {
        CloudResponse<T> res = new CloudResponse<>(true);
        res.setSuccessEntity(body);
        return res;
    }

    public static <T> CloudResponse<T> ofFail(String message) {
        CloudResponse<T> res = new CloudResponse<>(false);
        res.setFailMessage(message);
        return res;
    }

    private final boolean _success;
    private String message;
    private T entity;

    public CloudResponse(boolean success) {
        this._success = success;
    }

    public boolean isSuccess() {
        return this._success;
    }

    public String getFailMessage() {
        return message;
    }

    CloudResponse<T> setFailMessage(String message) {
        this.message = message;
        return this;
    }

    CloudResponse<T> setSuccessEntity(T entity) {
        this.entity = entity;
        return this;
    }

    public T getSuccessEntity() {
        return this.entity;
    }
}
