package io.github.xesam.cloud;

import java.util.Map;

public interface RequestClient<T> {
    CloudResponse<T> httpGet(String url, Map<String, String> params, Map<String, String> headers);
    CloudResponse<T> httpPost(String url, String body, Map<String, String> headers);
    CloudResponse<T> httpPost(String url, Map<String, String> formBody, Map<String, String> headers);
    CloudResponse<T> httpDelete(String url, Map<String, String> params, Map<String, String> headers);
    CloudResponse<T> httpPatch(String url, Map<String, String> formBody, Map<String, String> headers);
}
