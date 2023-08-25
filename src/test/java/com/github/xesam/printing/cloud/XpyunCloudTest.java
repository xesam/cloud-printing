package com.github.xesam.printing.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xesam.printing.cloud.*;
import com.github.xesam.printing.cloud.xpyun.XpyunCloud;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class XpyunCloudTest {

    private static final String TEST_APP_ID = "test_id";
    private static final String TEST_SECRET = "test_secret";

    private static RequestClient<String> getMockRequestClient(CloudResponse<String> response) {
        RequestClient<String> requestClient = mock(RequestClient.class);
        when(requestClient.httpPost(anyString(), anyString(), anyMap())).thenReturn(response);
        when(requestClient.httpPost(anyString(), anyMap(), anyMap())).thenReturn(response);
        when(requestClient.httpGet(anyString(), anyMap(), anyMap())).thenReturn(response);
        when(requestClient.httpDelete(anyString(), anyMap(), anyMap())).thenReturn(response);
        when(requestClient.httpPatch(anyString(), anyMap(), anyMap())).thenReturn(response);
        return requestClient;
    }

    private static RequestClient<String> getMockRequestClientWithNetworkFail() {
        CloudResponse<String> requestResponse = CloudResponse.ofFail("404");
        return getMockRequestClient(requestResponse);
    }

    private static RequestClient<String> getMockRequestClientWithApiFail() {
        CloudResponse<String> requestResponse = CloudResponse.ofFail("{\"msg\":\"参数错误 : 该帐号未注册.\",\"ret\":-2,\"data\":null,\"serverExecutedTime\":37}");
        return getMockRequestClient(requestResponse);
    }

    private static XpyunCloud getMockCloud(RequestClient<String> requestClient) {
        CloudAuth cloudAuth = new CloudAuth(TEST_APP_ID, TEST_SECRET);
        XpyunCloud cloud = new XpyunCloud(cloudAuth);
        cloud.setCloudClock(() -> 1_000_000_000);
        cloud.setRequestClient(requestClient);
        return cloud;
    }

    private static XpyunCloud getMockBackUrlCloud(RequestClient<String> requestClient) {
        return getMockCloud(requestClient).setBackUrl(1);
    }

    private static Device getMockDevice() {
        Device device = new Device("01234");
        device.setKey("abcde");
        device.setName("快餐前台");
        device.setCardno("13688889999");
        return device;
    }

    private static Order getMockOrder() {
        Order order = new Order();
        order.setId("this_is_origin_order_id");
        order.setContent("this is order content");
        order.setCopies(3);
        return order;
    }

    private static Order getMockOrderWithExpired() {
        Order order = getMockOrder();
        order.setExpiredEpochSecond(1_000_000_000 + 86400);
        return order;
    }

    private static QueryOption getMockQueryOption() {
        return new QueryOption().setDate(LocalDate.of(2022, 1, 1));
    }

    private static String ensureRequestUrlMatched(String path) {
        return eq("https://open.xpyun.net/api/openapi/xprinter/" + path);
    }

    private static Map<String, String> ensureRequestHeadersMatched() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        return eq(headers);
    }

    private static void ensureCommonBodyMatched(Map<String, Object> body, int bodyEntriesSize) {
        assertThat(body.entrySet(), hasSize(bodyEntriesSize));
        assertThat(body.get("user"), equalTo(TEST_APP_ID));
        assertThat(body.get("timestamp"), equalTo("1000000000"));
        assertThat(body.get("sign"), equalTo("c92c63ca5be6d9d31c71a8cc7e6140d59f79a9af"));
    }

    private static void ensureNetworkErrorCatch(Function<CloudApi, CloudResponse<?>> toBeTest) {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        XpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<?> cloudResponse = toBeTest.apply(cloud);
        assertFalse(cloudResponse.isSuccess());
        assertThat(cloudResponse.getFailMessage(), containsString("404"));
    }

    private static void ensureApiErrorCatch(Function<CloudApi, CloudResponse<?>> toBeTest) {
        RequestClient<String> requestClient = getMockRequestClientWithApiFail();
        XpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<?> cloudResponse = toBeTest.apply(cloud);
        assertFalse(cloudResponse.isSuccess());
        assertThat(cloudResponse.getFailMessage(), containsString("该帐号未注册"));
    }

    @Before
    public void setup_before_each_test() {
    }

    @Test
    public void when_network_error_then_fail_with_network_reason() {
        ensureNetworkErrorCatch(cloud -> cloud.addDevice(getMockDevice()));
        ensureNetworkErrorCatch(cloud -> cloud.deleteDevice(getMockDevice()));
        ensureNetworkErrorCatch(cloud -> cloud.queryDevice(getMockDevice()));
        ensureNetworkErrorCatch(cloud -> cloud.updateDevice(getMockDevice()));
        ensureNetworkErrorCatch(cloud -> cloud.printMsgOrder(getMockDevice(), getMockOrder()));
        ensureNetworkErrorCatch(cloud -> cloud.printLabelOrder(getMockDevice(), getMockOrder()));
        ensureNetworkErrorCatch(cloud -> cloud.queryOrder(getMockOrder()));
        ensureNetworkErrorCatch(cloud -> cloud.queryDeviceOrders(getMockDevice(), getMockQueryOption()));
        ensureNetworkErrorCatch(cloud -> cloud.clearDeviceOrders(getMockDevice()));
    }

    @Test
    public void when_api_error_then_fail_with_api_reason() {
        ensureApiErrorCatch(cloud -> cloud.addDevice(getMockDevice()));
        ensureApiErrorCatch(cloud -> cloud.deleteDevice(getMockDevice()));
        ensureApiErrorCatch(cloud -> cloud.queryDevice(getMockDevice()));
        ensureApiErrorCatch(cloud -> cloud.updateDevice(getMockDevice()));
        ensureApiErrorCatch(cloud -> cloud.printMsgOrder(getMockDevice(), getMockOrder()));
        ensureApiErrorCatch(cloud -> cloud.printLabelOrder(getMockDevice(), getMockOrder()));
        ensureApiErrorCatch(cloud -> cloud.queryOrder(getMockOrder()));
        ensureApiErrorCatch(cloud -> cloud.queryDeviceOrders(getMockDevice(), getMockQueryOption()));
        ensureApiErrorCatch(cloud -> cloud.clearDeviceOrders(getMockDevice()));
    }

    private Map<String, Object> parseMockBody(String bodyString) {
        try {
            return new ObjectMapper().readValue(bodyString, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void when_add_device_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        XpyunCloud cloud = getMockCloud(requestClient);
        cloud.addDevice(getMockDevice());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched("addPrinters"),
                Mockito.<String>argThat(bodyString -> {
                    Map<String, Object> body = parseMockBody(bodyString);
                    ensureCommonBodyMatched(body, 4);
                    Map<String, String> deviceObj = new HashMap<>();
                    deviceObj.put("sn", getMockDevice().getSn());
                    deviceObj.put("name", getMockDevice().getName());
                    List<Map<String, String>> items = new ArrayList<>();
                    items.add(deviceObj);
                    assertThat(body.get("items"), equalTo(items));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_add_device_but_device_is_incorrect_then_fail_with_device_reason() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"code\":0,\"msg\":\"ok\",\"data\":{\"failMsg\":[\"01234 :   识别码不正确\"],\"success\":[]}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        XpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Boolean> cloudResponse = cloud.addDevice(getMockDevice());
        assertFalse(cloudResponse.isSuccess());
        assertThat(cloudResponse.getFailMessage(), containsString("识别码不正确"));
    }

    @Test
    public void when_add_device_successfully_then_return_true() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"code\":0,\"msg\":\"ok\",\"data\":{\"success\":[\"01234\"],\"failMsg\":[]}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        XpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Boolean> cloudResponse = cloud.addDevice(getMockDevice());
        assertTrue(cloudResponse.isSuccess());
        assertTrue(cloudResponse.getSuccessEntity());
    }

    //
    @Test
    public void when_delete_device_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        XpyunCloud cloud = getMockCloud(requestClient);
        cloud.deleteDevice(getMockDevice());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched("delPrinters"),
                Mockito.<String>argThat(bodyString -> {
                    Map<String, Object> body = parseMockBody(bodyString);
                    ensureCommonBodyMatched(body, 4);
                    List<String> snlist = new ArrayList<>();
                    snlist.add(getMockDevice().getSn());
                    assertThat(body.get("snlist"), equalTo(snlist));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_delete_device_but_device_is_incorrect_then_fail_with_device_reason() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"code\":0,\"msg\":\"ok\",\"data\":{\"success\": [], \"fail\": [\"01234\"], \"failMsg\": [\"01234:用户UID不匹配\"]}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        XpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Boolean> cloudResponse = cloud.deleteDevice(getMockDevice());
        assertFalse(cloudResponse.isSuccess());
        assertThat(cloudResponse.getFailMessage(), containsString("用户UID不匹配"));
    }

    @Test
    public void when_delete_device_successfully_then_return_true() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"code\":0,\"msg\":\"ok\",\"data\":{\"success\":[\"01234\"],\"failMsg\":[]}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        XpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Boolean> cloudResponse = cloud.deleteDevice(getMockDevice());
        assertTrue(cloudResponse.isSuccess());
        assertTrue(cloudResponse.getSuccessEntity());
    }

    @Test
    public void when_query_device_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        XpyunCloud cloud = getMockCloud(requestClient);
        cloud.queryDevice(getMockDevice());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched("queryPrinterStatus"),
                Mockito.<String>argThat(bodyString -> {
                    Map<String, Object> body = parseMockBody(bodyString);
                    ensureCommonBodyMatched(body, 4);
                    assertThat(body.get("sn"), equalTo("01234"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_query_device_but_device_is_offline_then_mark_device_offline() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"code\":0,\"msg\":\"ok\",\"data\":0}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        XpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Device> cloudResponse = cloud.queryDevice(getMockDevice());

        Device queriedDevice = cloudResponse.getSuccessEntity();
        assertTrue(cloudResponse.isSuccess());
        assertFalse(queriedDevice.isOnline());
        assertThat(queriedDevice.getStatus(), equalTo(Device.Status.ANORMAL));
    }

    @Test
    public void when_query_device_and_device_is_online_but_is_not_normal_then_mark_device_online_but_anormal() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"code\":0,\"msg\":\"ok\",\"data\":2}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        XpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Device> cloudResponse = cloud.queryDevice(getMockDevice());

        Device queriedDevice = cloudResponse.getSuccessEntity();
        assertTrue(cloudResponse.isSuccess());
        assertTrue(queriedDevice.isOnline());
        assertThat(queriedDevice.getStatus(), equalTo(Device.Status.ANORMAL));
    }

    @Test
    public void when_query_device_and_device_is_online_and_normal_then_mark_device_online_and_normal() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"code\":0,\"msg\":\"ok\",\"data\":1}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        XpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Device> cloudResponse = cloud.queryDevice(getMockDevice());

        Device queriedDevice = cloudResponse.getSuccessEntity();
        assertTrue(cloudResponse.isSuccess());
        assertTrue(queriedDevice.isOnline());
        assertThat(queriedDevice.getStatus(), equalTo(Device.Status.NORMAL));
    }

    @Test
    public void when_update_device_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        XpyunCloud cloud = getMockCloud(requestClient);
        cloud.updateDevice(getMockDevice());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched("updPrinter"),
                Mockito.<String>argThat(bodyString -> {
                    Map<String, Object> body = parseMockBody(bodyString);
                    ensureCommonBodyMatched(body, 6);
                    assertThat(body.get("sn"), equalTo("01234"));
                    assertThat(body.get("name"), equalTo("快餐前台"));
                    assertThat(body.get("cardno"), equalTo("13688889999"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_update_device_successfully_then_return_true() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"code\":0,\"msg\":\"ok\",\"data\":true}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        XpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Boolean> cloudResponse = cloud.updateDevice(getMockDevice());

        assertTrue(cloudResponse.isSuccess());
        assertTrue(cloudResponse.getSuccessEntity());
    }

    @Test
    public void when_print_msg_order_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        XpyunCloud cloud = getMockCloud(requestClient);
        cloud.printMsgOrder(getMockDevice(), getMockOrder());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched("print"),
                Mockito.<String>argThat(bodyString -> {
                    Map<String, Object> body = parseMockBody(bodyString);
                    ensureCommonBodyMatched(body, 6);
                    assertThat(body.get("sn"), equalTo("01234"));
                    assertThat(body.get("content").toString(), containsString("this is order content"));
                    assertThat(body.get("copies"), equalTo(3));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_print_msg_order_with_expired_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        XpyunCloud cloud = getMockCloud(requestClient);
        cloud.printMsgOrder(getMockDevice(), getMockOrderWithExpired());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched("print"),
                Mockito.<String>argThat(bodyString -> {
                    Map<String, Object> body = parseMockBody(bodyString);
                    ensureCommonBodyMatched(body, 8);
                    assertThat(body.get("expiresIn"), equalTo(86400));
                    assertThat(body.get("mode"), equalTo(1));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_print_msg_order_with_expired_and_backurl_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        XpyunCloud cloud = getMockBackUrlCloud(requestClient);
        cloud.printMsgOrder(getMockDevice(), getMockOrderWithExpired());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched("print"),
                Mockito.<String>argThat(bodyString -> {
                    Map<String, Object> body = parseMockBody(bodyString);
                    ensureCommonBodyMatched(body, 9);
                    assertThat(body.get("backurlFlag"), equalTo(1));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_print_msg_order_successfully_then_set_the_order_id() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"code\":0,\"msg\":\"ok\",\"data\":\"this_is_order_id\"}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        XpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Order> cloudResponse = cloud.printMsgOrder(getMockDevice(), getMockOrder());

        assertTrue(cloudResponse.isSuccess());
        assertThat(cloudResponse.getSuccessEntity().getId(), equalTo("this_is_order_id"));
    }

    @Test
    public void when_print_label_order_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        XpyunCloud cloud = getMockCloud(requestClient);
        cloud.printLabelOrder(getMockDevice(), getMockOrder());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched("printLabel"),
                Mockito.<String>argThat(bodyString -> {
                    Map<String, Object> body = parseMockBody(bodyString);
                    ensureCommonBodyMatched(body, 6);
                    assertThat(body.get("sn"), equalTo("01234"));
                    assertThat(body.get("content").toString(), containsString("this is order content"));
                    assertThat(body.get("copies"), equalTo(3));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_print_label_order_with_expired_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        XpyunCloud cloud = getMockCloud(requestClient);
        cloud.printLabelOrder(getMockDevice(), getMockOrderWithExpired());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched("printLabel"),
                Mockito.<String>argThat(bodyString -> {
                    Map<String, Object> body = parseMockBody(bodyString);
                    ensureCommonBodyMatched(body, 8);
                    assertThat(body.get("expiresIn"), equalTo(86400));
                    assertThat(body.get("mode"), equalTo(1));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_print_label_order_with_expired_and_backurl_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        XpyunCloud cloud = getMockBackUrlCloud(requestClient);
        cloud.printLabelOrder(getMockDevice(), getMockOrderWithExpired());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched("printLabel"),
                Mockito.<String>argThat(bodyString -> {
                    Map<String, Object> body = parseMockBody(bodyString);
                    ensureCommonBodyMatched(body, 9);
                    assertThat(body.get("backurlFlag"), equalTo(1));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_print_label_order_successfully_then_set_the_order_id() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"code\":0,\"msg\":\"ok\",\"data\":\"this_is_order_id\"}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        XpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Order> cloudResponse = cloud.printLabelOrder(getMockDevice(), getMockOrder());

        assertTrue(cloudResponse.isSuccess());
        assertThat(cloudResponse.getSuccessEntity().getId(), equalTo("this_is_order_id"));
    }

    @Test
    public void when_query_order_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        XpyunCloud cloud = getMockCloud(requestClient);
        cloud.queryOrder(getMockOrder());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched("queryOrderState"),
                Mockito.<String>argThat(bodyString -> {
                    Map<String, Object> body = parseMockBody(bodyString);
                    ensureCommonBodyMatched(body, 4);
                    assertThat(body.get("orderId"), equalTo(getMockOrder().getId()));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_query_order_has_been_printed_then_mark_the_order_printed() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"code\":0,\"msg\":\"ok\",\"data\":true}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        XpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Order> cloudResponse = cloud.queryOrder(getMockOrder());

        assertTrue(cloudResponse.isSuccess());
        assertTrue(cloudResponse.getSuccessEntity().isPrinted());
    }

    @Test
    public void when_query_order_has_not_been_printed_then_mark_the_order_not_printed() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"code\":0,\"msg\":\"ok\",\"data\":false}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        XpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Order> cloudResponse = cloud.queryOrder(getMockOrder());

        assertTrue(cloudResponse.isSuccess());
        assertFalse(cloudResponse.getSuccessEntity().isPrinted());
    }

    @Test
    public void when_query_device_order_stat_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        XpyunCloud cloud = getMockCloud(requestClient);
        cloud.queryDeviceOrders(getMockDevice(), getMockQueryOption());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched("queryOrderStatis"),
                Mockito.<String>argThat(bodyString -> {
                    Map<String, Object> body = parseMockBody(bodyString);
                    ensureCommonBodyMatched(body, 5);
                    assertThat(body.get("sn"), equalTo(getMockDevice().getSn()));
                    assertThat(body.get("date"), equalTo(getMockQueryOption().getDate()));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_query_device_order_stat_successfully_then_return_the_stat() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"code\":0,\"msg\":\"ok\",\"data\":{\"printed\":6,\"waiting\":1}}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        XpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<DeviceOrderStat> cloudResponse = cloud.queryDeviceOrders(getMockDevice(), getMockQueryOption());

        assertTrue(cloudResponse.isSuccess());
        assertThat(cloudResponse.getSuccessEntity().getDeviceSn(), equalTo(getMockDevice().getSn()));
        assertThat(cloudResponse.getSuccessEntity().getOrderDate(), equalTo(getMockQueryOption().getDate()));
        assertThat(cloudResponse.getSuccessEntity().getPrintedCount(), equalTo(6));
        assertThat(cloudResponse.getSuccessEntity().getWaitingCount(), equalTo(1));
    }

    @Test
    public void when_clear_device_order_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        XpyunCloud cloud = getMockCloud(requestClient);
        cloud.clearDeviceOrders(getMockDevice());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched("delPrinterQueue"),
                Mockito.<String>argThat(bodyString -> {
                    Map<String, Object> body = parseMockBody(bodyString);
                    ensureCommonBodyMatched(body, 4);
                    assertThat(body.get("sn"), equalTo(getMockDevice().getSn()));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_clear_device_order_successfully_then_return_true() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"code\":0,\"msg\":\"ok\",\"data\":true}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        XpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Boolean> cloudResponse = cloud.clearDeviceOrders(getMockDevice());

        assertTrue(cloudResponse.isSuccess());
        assertTrue(cloudResponse.getSuccessEntity());
    }
}
