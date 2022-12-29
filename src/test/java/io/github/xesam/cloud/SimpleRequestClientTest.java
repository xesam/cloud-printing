package io.github.xesam.cloud;

import io.github.xesam.cloud.simple.SimpleRequestClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SimpleRequestClientTest {
    private static CloseableHttpClient getMockCloseableHttpClient() {
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        try {
            CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            when(httpClient.execute(any(HttpUriRequestBase.class))).thenReturn(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return httpClient;
    }

    private Map<String, String> getMockGetData() {
        Map<String, String> params = new HashMap<>();
        params.put("user", "this is user");
        params.put("key", "this_is_key");
        return params;
    }

    private Map<String, String> getMockHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("header1", "this is header1");
        headers.put("header2", "this_is_header2");
        return headers;
    }

    @Test
    public void when_get_then_encode_params_to_url() throws IOException {
        CloseableHttpClient client = getMockCloseableHttpClient();
        SimpleRequestClient simpleRequestClient = new SimpleRequestClient(client);
        simpleRequestClient.httpGet("https://host", getMockGetData(), getMockHeaders());
        verify(client).execute(argThat(request -> {
            try {
                assertThat(request.getMethod(), equalToIgnoringCase("get"));
                assertThat(request.getUri().toString(), equalTo("https://host/"));
                assertThat(request.getPath(), equalTo("/?user=this%20is%20user&key=this_is_key"));
                assertThat(request.getHeader("header1").getValue(), containsString("this is header1"));
                assertThat(request.getHeader("header2").getValue(), containsString("this_is_header2"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        }));
    }

    @Test
    public void when_delete_then_encode_params_to_url() throws IOException {
        CloseableHttpClient client = getMockCloseableHttpClient();
        SimpleRequestClient simpleRequestClient = new SimpleRequestClient(client);
        simpleRequestClient.httpDelete("https://host", getMockGetData(), getMockHeaders());
        verify(client).execute(argThat(request -> {
            try {
                assertThat(request.getMethod(), equalToIgnoringCase("delete"));
                assertThat(request.getUri().toString(), equalTo("https://host/"));
                assertThat(request.getPath(), equalTo("/?user=this%20is%20user&key=this_is_key"));
                assertThat(request.getHeader("header1").getValue(), containsString("this is header1"));
                assertThat(request.getHeader("header2").getValue(), containsString("this_is_header2"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        }));
    }

    @Test
    public void when_patch_then_encode_data_to_body() throws IOException {
        CloseableHttpClient client = getMockCloseableHttpClient();
        SimpleRequestClient simpleRequestClient = new SimpleRequestClient(client);
        simpleRequestClient.httpPatch("https://host", getMockGetData(), getMockHeaders());
        verify(client).execute(argThat(request -> {
            try {
                assertThat(request.getMethod(), equalToIgnoringCase("patch"));
                assertThat(request.getUri().toString(), equalTo("https://host/"));
                assertThat(request.getPath(), equalTo("/"));
                String body = new BufferedReader(new InputStreamReader(request.getEntity().getContent())).readLine();
                assertThat(body, containsString("this+is+user"));
                assertThat(body, containsString("this_is_key"));
                assertThat(request.getHeader("header1").getValue(), containsString("this is header1"));
                assertThat(request.getHeader("header2").getValue(), containsString("this_is_header2"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        }));
    }

    @Test
    public void when_post_form_then_encode_data_to_body() throws IOException {
        CloseableHttpClient client = getMockCloseableHttpClient();
        SimpleRequestClient simpleRequestClient = new SimpleRequestClient(client);
        simpleRequestClient.httpPost("https://host", getMockGetData(), getMockHeaders());
        verify(client).execute(argThat(request -> {
            try {
                assertThat(request.getMethod(), equalToIgnoringCase("post"));
                assertThat(request.getUri().toString(), equalTo("https://host/"));
                assertThat(request.getPath(), equalTo("/"));
                String body = new BufferedReader(new InputStreamReader(request.getEntity().getContent())).readLine();
                assertThat(body, containsString("this+is+user"));
                assertThat(body, containsString("this_is_key"));
                assertThat(request.getHeader("header1").getValue(), containsString("this is header1"));
                assertThat(request.getHeader("header2").getValue(), containsString("this_is_header2"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        }));
    }

    @Test
    public void when_post_json_then_add_json_to_body() throws IOException {
        CloseableHttpClient client = getMockCloseableHttpClient();
        SimpleRequestClient simpleRequestClient = new SimpleRequestClient(client);
        simpleRequestClient.httpPost("https://host", "{\"a\":100}", getMockHeaders());
        verify(client).execute(argThat(request -> {
            try {
                assertThat(request.getMethod(), equalToIgnoringCase("post"));
                assertThat(request.getUri().toString(), equalTo("https://host/"));
                assertThat(request.getPath(), equalTo("/"));
                String body = new BufferedReader(new InputStreamReader(request.getEntity().getContent())).readLine();
                assertThat(body, containsString("{\"a\":100}"));
                assertThat(request.getHeader("header1").getValue(), containsString("this is header1"));
                assertThat(request.getHeader("header2").getValue(), containsString("this_is_header2"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        }));
    }
}
