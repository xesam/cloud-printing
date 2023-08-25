package com.github.xesam.printing.cloud.simple;

import com.github.xesam.printing.cloud.CloudResponse;
import com.github.xesam.printing.cloud.RequestClient;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.TimeValue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SimpleRequestClient implements RequestClient<String> {
    private static final class SimpleConnectionKeepAliveStrategy extends DefaultConnectionKeepAliveStrategy {
        private static final TimeValue TIMEOUT = TimeValue.ofSeconds(15);

        @Override
        public TimeValue getKeepAliveDuration(final HttpResponse response, final HttpContext context) {
            TimeValue timeValue = super.getKeepAliveDuration(response, context);
            return timeValue.min(TIMEOUT);
        }
    }

    private final CloseableHttpClient client;

    private static CloseableHttpClient createDefault() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(5, TimeUnit.SECONDS)
                .setConnectTimeout(5, TimeUnit.SECONDS)
                .setResponseTimeout(5, TimeUnit.SECONDS)
                .build();
        return HttpClients.custom()
                .setKeepAliveStrategy(new SimpleConnectionKeepAliveStrategy())
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    public SimpleRequestClient() {
        this(createDefault());
    }

    public SimpleRequestClient(CloseableHttpClient client) {
        this.client = client;
    }

    private CloudResponse<String> sendRequestWithHeaders(HttpUriRequestBase request, Map<String, String> headers) {
        if (headers != null) {
            headers.forEach(request::addHeader);
        }
        try (CloseableHttpResponse httpResponse = this.client.execute(request)) {
            if (httpResponse.getCode() != 200) {
                return CloudResponse.ofFail("StatusCode is " + httpResponse.getCode());
            }
            String responseString = EntityUtils.toString(httpResponse.getEntity());
            return CloudResponse.ofSuccess(responseString);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            return CloudResponse.ofFail(e.getMessage());
        }
    }

    private List<NameValuePair> buildPairs(Map<String, String> data) {
        return data.entrySet().stream()
                .map(ele -> new BasicNameValuePair(ele.getKey(), ele.getValue()))
                .collect(Collectors.toList());
    }

    private CloudResponse<String> sendUrlParams(HttpUriRequestBase request, Map<String, String> params, Map<String, String> headers) {
        List<NameValuePair> pairs = buildPairs(params);
        URI uri;
        try {
            uri = new URIBuilder(request.getUri())
                    .addParameters(pairs)
                    .build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return CloudResponse.ofFail(e.getMessage());
        }
        request.setUri(uri);
        return sendRequestWithHeaders(request, headers);
    }

    private CloudResponse<String> sendUrlEncodedForm(HttpUriRequestBase request, Map<String, String> formData, Map<String, String> headers) {
        List<NameValuePair> pairs = buildPairs(formData);
        request.setEntity(new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8));
        return sendRequestWithHeaders(request, headers);
    }

    @Override
    public CloudResponse<String> httpGet(String url, Map<String, String> params, Map<String, String> headers) {
        HttpGet request = new HttpGet(url);
        return sendUrlParams(request, params, headers);
    }

    @Override
    public CloudResponse<String> httpPost(String url, String body, Map<String, String> headers) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
        return sendRequestWithHeaders(request, headers);
    }

    @Override
    public CloudResponse<String> httpPost(String url, Map<String, String> formBody, Map<String, String> headers) {
        HttpPost request = new HttpPost(url);
        return sendUrlEncodedForm(request, formBody, headers);
    }

    @Override
    public CloudResponse<String> httpDelete(String url, Map<String, String> params, Map<String, String> headers) {
        HttpDelete request = new HttpDelete(url);
        return sendUrlParams(request, params, headers);
    }

    @Override
    public CloudResponse<String> httpPatch(String url, Map<String, String> formBody, Map<String, String> headers) {
        HttpPatch request = new HttpPatch(url);
        return sendUrlEncodedForm(request, formBody, headers);
    }
}
