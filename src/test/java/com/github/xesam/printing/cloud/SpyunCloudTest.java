package com.github.xesam.printing.cloud;

import com.github.xesam.printing.cloud.*;
import com.github.xesam.printing.cloud.spyun.SpyunCloud;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class SpyunCloudTest {

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
        CloudResponse<String> requestResponse = CloudResponse.ofFail("{\"errormsg\":\"参数错误 : 该帐号未注册.\",\"errorcode\":-1}");
        return getMockRequestClient(requestResponse);
    }

    private static SpyunCloud getMockCloud(RequestClient<String> requestClient) {
        CloudAuth cloudAuth = new CloudAuth(TEST_APP_ID, TEST_SECRET);
        SpyunCloud cloud = new SpyunCloud(cloudAuth);
        cloud.setCloudClock(() -> 1_000_000_000);
        cloud.setRequestClient(requestClient);
        return cloud;
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

    private static QueryOption getMockQueryOption() {
        return new QueryOption().setDate(LocalDate.of(2022, 1, 1));
    }

    private static String ensureRequestUrlMatched(String path) {
        return eq("https://open.spyun.net/v1/printer/" + path);
    }

    private static Map<String, String> ensureRequestHeadersMatched() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        return eq(headers);
    }

    private static void ensureCommonBodyMatched(Map<String, String> body, int bodyEntriesSize) {
        assertThat(body.entrySet(), hasSize(bodyEntriesSize));
        assertThat(body.get("appid"), equalTo(TEST_APP_ID));
        assertThat(body.get("timestamp"), equalTo("1000000000"));
    }

    private static void ensureNetworkErrorCatch(Function<CloudApi, CloudResponse<?>> toBeTest) {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        SpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<?> cloudResponse = toBeTest.apply(cloud);
        assertFalse(cloudResponse.isSuccess());
        assertThat(cloudResponse.getFailMessage(), containsString("404"));
    }

    private static void ensureApiErrorCatch(Function<CloudApi, CloudResponse<?>> toBeTest) {
        RequestClient<String> requestClient = getMockRequestClientWithApiFail();
        SpyunCloud cloud = getMockCloud(requestClient);
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
        ensureApiErrorCatch(cloud -> cloud.queryOrder(getMockOrder()));
        ensureApiErrorCatch(cloud -> cloud.queryDeviceOrders(getMockDevice(), getMockQueryOption()));
        ensureApiErrorCatch(cloud -> cloud.clearDeviceOrders(getMockDevice()));
    }

    @Test
    public void when_add_device_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        SpyunCloud cloud = getMockCloud(requestClient);
        cloud.addDevice(getMockDevice());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched("add"),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 6);
                    assertThat(body.get("sign"), equalTo("3DC01E8396B353552CEB0C98E65E725B"));
                    assertThat(body.get("sn"), equalTo("01234"));
                    assertThat(body.get("pkey"), equalTo("abcde"));
                    assertThat(body.get("name"), equalTo("快餐前台"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_add_device_successfully_then_return_true() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"errorcode\":0}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        SpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Boolean> cloudResponse = cloud.addDevice(getMockDevice());
        assertTrue(cloudResponse.isSuccess());
        assertTrue(cloudResponse.getSuccessEntity());
    }

    @Test
    public void when_delete_device_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        SpyunCloud cloud = getMockCloud(requestClient);
        cloud.deleteDevice(getMockDevice());
        verify(requestClient).httpDelete(
                ensureRequestUrlMatched("delete"),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 4);
                    assertThat(body.get("sn"), equalTo("01234"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_delete_device_successfully_then_return_true() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"errorcode\":0}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        SpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Boolean> cloudResponse = cloud.deleteDevice(getMockDevice());
        assertTrue(cloudResponse.isSuccess());
        assertTrue(cloudResponse.getSuccessEntity());
    }

    @Test
    public void when_query_device_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        SpyunCloud cloud = getMockCloud(requestClient);
        cloud.queryDevice(getMockDevice());
        verify(requestClient).httpGet(
                ensureRequestUrlMatched("info"),
                Mockito.argThat(body -> {
                    ensureCommonBodyMatched(body, 4);
                    assertThat(body.get("sn"), equalTo("01234"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    private CloudResponse<String> getMockDeviceResponse(boolean online, Device.Status status) {
        int onlineCode = online ? 1 : 0;
        int statusCode = status == Device.Status.NORMAL ? 0 : 1;
        String resStr = String.format(
                "{\"errorcode\":0,\"sn\":\"01234\",\"name\":\"name\",\"online\":%d,\"status\":%d,\"sqsnum\":1,\"model\":\"xxx\",\"auto_cut\":1,\"voice\":\"U\",\"imsi\":\"xxx\"}",
                onlineCode,
                statusCode
        );
        return CloudResponse.ofSuccess(resStr);
    }

    @Test
    public void when_query_device_but_device_is_offline_then_mark_device_offline() {
        CloudResponse<String> requestResponse = getMockDeviceResponse(false, Device.Status.ANORMAL);
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        SpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Device> cloudResponse = cloud.queryDevice(getMockDevice());

        Device queriedDevice = cloudResponse.getSuccessEntity();
        assertTrue(cloudResponse.isSuccess());
        assertFalse(queriedDevice.isOnline());
        assertThat(queriedDevice.getStatus(), equalTo(Device.Status.ANORMAL));
    }

    @Test
    public void when_query_device_and_device_is_online_but_is_not_normal_then_mark_device_online_but_anormal() {
        CloudResponse<String> requestResponse = getMockDeviceResponse(true, Device.Status.ANORMAL);
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        SpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Device> cloudResponse = cloud.queryDevice(getMockDevice());

        Device queriedDevice = cloudResponse.getSuccessEntity();
        assertTrue(cloudResponse.isSuccess());
        assertTrue(queriedDevice.isOnline());
        assertThat(queriedDevice.getStatus(), equalTo(Device.Status.ANORMAL));
    }

    @Test
    public void when_query_device_and_device_is_online_and_normal_then_mark_device_online_and_normal() {
        CloudResponse<String> requestResponse = getMockDeviceResponse(true, Device.Status.NORMAL);
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        SpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Device> cloudResponse = cloud.queryDevice(getMockDevice());

        Device queriedDevice = cloudResponse.getSuccessEntity();
        assertTrue(cloudResponse.isSuccess());
        assertTrue(queriedDevice.isOnline());
        assertThat(queriedDevice.getStatus(), equalTo(Device.Status.NORMAL));
    }

    @Test
    public void when_update_device_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        SpyunCloud cloud = getMockCloud(requestClient);
        cloud.updateDevice(getMockDevice());
        verify(requestClient).httpPatch(
                ensureRequestUrlMatched("update"),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 5);
                    assertThat(body.get("sn"), equalTo("01234"));
                    assertThat(body.get("name"), equalTo("快餐前台"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_update_device_successfully_then_return_true() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"errorcode\":0}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        SpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Boolean> cloudResponse = cloud.updateDevice(getMockDevice());

        assertTrue(cloudResponse.isSuccess());
        assertTrue(cloudResponse.getSuccessEntity());
    }

    @Test
    public void when_print_msg_order_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        SpyunCloud cloud = getMockCloud(requestClient);
        cloud.printMsgOrder(getMockDevice(), getMockOrder());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched("print"),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 6);
                    assertThat(body.get("sn"), equalTo("01234"));
                    assertThat(body.get("content"), containsString("this is order content"));
                    assertThat(body.get("times"), equalTo("3"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_print_msg_order_successfully_then_set_the_order_id() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"errorcode\":0,\"id\":\"this_is_order_id\",\"create_time\":\"2019-01-01 00:00:00\"}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        SpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Order> cloudResponse = cloud.printMsgOrder(getMockDevice(), getMockOrder());

        assertTrue(cloudResponse.isSuccess());
        assertThat(cloudResponse.getSuccessEntity().getId(), equalTo("this_is_order_id"));
    }

    @Test
    public void when_query_order_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        SpyunCloud cloud = getMockCloud(requestClient);
        cloud.queryOrder(getMockOrder());
        verify(requestClient).httpGet(
                ensureRequestUrlMatched("order/status"),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 4);
                    assertThat(body.get("id"), equalTo(getMockOrder().getId()));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_query_order_has_been_printed_then_mark_the_order_printed() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"errorcode\":0,\"status\":true,\"print_time\":\"2019-01-01 00:00:00\"}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        SpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Order> cloudResponse = cloud.queryOrder(getMockOrder());

        assertTrue(cloudResponse.isSuccess());
        assertTrue(cloudResponse.getSuccessEntity().isPrinted());
    }

    @Test
    public void when_query_order_has_not_been_printed_then_mark_the_order_not_printed() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"errorcode\":0,\"status\":false}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        SpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Order> cloudResponse = cloud.queryOrder(getMockOrder());

        assertTrue(cloudResponse.isSuccess());
        assertFalse(cloudResponse.getSuccessEntity().isPrinted());
    }

    @Test
    public void when_query_device_order_stat_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        SpyunCloud cloud = getMockCloud(requestClient);
        cloud.queryDeviceOrders(getMockDevice(), getMockQueryOption());
        verify(requestClient).httpGet(
                ensureRequestUrlMatched("order/number"),
                Mockito.<Map<String, String>>argThat(body -> {
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
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"errorcode\":0,\"number\":6}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        SpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<DeviceOrderStat> cloudResponse = cloud.queryDeviceOrders(getMockDevice(), getMockQueryOption());

        assertTrue(cloudResponse.isSuccess());
        assertThat(cloudResponse.getSuccessEntity().getDeviceSn(), equalTo(getMockDevice().getSn()));
        assertThat(cloudResponse.getSuccessEntity().getOrderDate(), equalTo(getMockQueryOption().getDate()));
        assertThat(cloudResponse.getSuccessEntity().getPrintedCount(), equalTo(6));
        assertThat(cloudResponse.getSuccessEntity().getWaitingCount(), equalTo(0));
    }

    @Test
    public void when_clear_device_order_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        SpyunCloud cloud = getMockCloud(requestClient);
        cloud.clearDeviceOrders(getMockDevice());
        verify(requestClient).httpDelete(
                ensureRequestUrlMatched("cleansqs"),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 4);
                    assertThat(body.get("sn"), equalTo(getMockDevice().getSn()));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_clear_device_order_successfully_then_return_true() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"errorcode\":0,\"cleannum\":6}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        SpyunCloud cloud = getMockCloud(requestClient);
        CloudResponse<Boolean> cloudResponse = cloud.clearDeviceOrders(getMockDevice());

        assertTrue(cloudResponse.isSuccess());
        assertTrue(cloudResponse.getSuccessEntity());
    }
}
