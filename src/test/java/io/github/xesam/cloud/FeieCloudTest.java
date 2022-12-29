package io.github.xesam.cloud;

import io.github.xesam.cloud.feie.FeieCloud;
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

public class FeieCloudTest {

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

    private static FeieCloud getMockCloud(RequestClient<String> requestClient) {
        CloudAuth cloudAuth = new CloudAuth(TEST_APP_ID, TEST_SECRET);
        FeieCloud cloud = new FeieCloud(cloudAuth);
        cloud.setCloudClock(() -> 1_000_000_000);
        cloud.setRequestClient(requestClient);
        return cloud;
    }

    private static FeieCloud getMockBackUrlCloud(RequestClient<String> requestClient) {
        return getMockCloud(requestClient).setBackUrl("this is backurl");
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

    private static String ensureRequestUrlMatched() {
        return eq("https://api.feieyun.cn/Api/Open/");
    }

    private static Map<String, String> ensureRequestHeadersMatched() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        return eq(headers);
    }

    private static void ensureCommonBodyMatched(Map<String, String> body, int bodyEntriesSize) {
        assertThat(body.entrySet(), hasSize(bodyEntriesSize));
        assertThat(body.get("user"), equalTo(TEST_APP_ID));
        assertThat(body.get("stime"), equalTo("1000000000"));
        assertThat(body.get("sig"), equalTo("c92c63ca5be6d9d31c71a8cc7e6140d59f79a9af"));
    }

    private static void ensureNetworkErrorCatch(Function<CloudApi, CloudResponse<?>> toBeTest) {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        FeieCloud cloud = getMockCloud(requestClient);
        CloudResponse<?> cloudResponse = toBeTest.apply(cloud);
        assertFalse(cloudResponse.isSuccess());
        assertThat(cloudResponse.getFailMessage(), containsString("404"));
    }

    private static void ensureApiErrorCatch(Function<CloudApi, CloudResponse<?>> toBeTest) {
        RequestClient<String> requestClient = getMockRequestClientWithApiFail();
        FeieCloud cloud = getMockCloud(requestClient);
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

    @Test
    public void when_add_device_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        FeieCloud cloud = getMockCloud(requestClient);
        cloud.addDevice(getMockDevice());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched(),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 5);
                    assertThat(body.get("apiname"), equalTo("Open_printerAddlist"));
                    assertThat(body.get("printerContent"), equalTo("01234#abcde#快餐前台#13688889999"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_add_device_but_device_is_incorrect_then_fail_with_device_reason() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"ret\":0,\"data\":{\"no\":[\"01234# abcde# 快餐前台  # 13688889999 （错误：识别码不正确）\"],\"ok\":[]}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        FeieCloud cloud = getMockCloud(requestClient);
        CloudResponse<Boolean> cloudResponse = cloud.addDevice(getMockDevice());
        assertFalse(cloudResponse.isSuccess());
        assertThat(cloudResponse.getFailMessage(), containsString("识别码不正确"));
    }

    @Test
    public void when_add_device_successfully_then_return_true() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"ret\":0,\"data\":{\"ok\":[\"01234# abcde# 快餐前台  # 13688889999 \"],\"no\":[]}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        FeieCloud cloud = getMockCloud(requestClient);
        CloudResponse<Boolean> cloudResponse = cloud.addDevice(getMockDevice());
        assertTrue(cloudResponse.isSuccess());
        assertTrue(cloudResponse.getSuccessEntity());
    }

    @Test
    public void when_delete_device_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        FeieCloud cloud = getMockCloud(requestClient);
        cloud.deleteDevice(getMockDevice());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched(),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 5);
                    assertThat(body.get("apiname"), equalTo("Open_printerDelList"));
                    assertThat(body.get("snlist"), equalTo("01234"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_delete_device_but_device_is_incorrect_then_fail_with_device_reason() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"ret\":0,\"data\":{\"no\":[\"800000777用户UID不匹配\"],\"ok\":[]}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        FeieCloud cloud = getMockCloud(requestClient);
        CloudResponse<Boolean> cloudResponse = cloud.deleteDevice(getMockDevice());
        assertFalse(cloudResponse.isSuccess());
        assertThat(cloudResponse.getFailMessage(), containsString("用户UID不匹配"));
    }

    @Test
    public void when_delete_device_successfully_then_return_true() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"ret\":0,\"data\":{\"ok\":[\"01234成功\"],\"no\":[]}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        FeieCloud cloud = getMockCloud(requestClient);
        CloudResponse<Boolean> cloudResponse = cloud.deleteDevice(getMockDevice());
        assertTrue(cloudResponse.isSuccess());
        assertTrue(cloudResponse.getSuccessEntity());
    }

    @Test
    public void when_query_device_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        FeieCloud cloud = getMockCloud(requestClient);
        cloud.queryDevice(getMockDevice());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched(),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 5);
                    assertThat(body.get("apiname"), equalTo("Open_queryPrinterStatus"));
                    assertThat(body.get("sn"), equalTo("01234"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_query_device_but_device_is_offline_then_mark_device_offline() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"ret\":0,\"data\":\"离线。\"}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        FeieCloud cloud = getMockCloud(requestClient);
        CloudResponse<Device> cloudResponse = cloud.queryDevice(getMockDevice());

        Device queriedDevice = cloudResponse.getSuccessEntity();
        assertTrue(cloudResponse.isSuccess());
        assertFalse(queriedDevice.isOnline());
        assertThat(queriedDevice.getStatus(), equalTo(Device.Status.ANORMAL));
    }

    @Test
    public void when_query_device_and_device_is_online_but_is_not_normal_then_mark_device_online_but_anormal() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"ret\":0,\"data\":\"在线，工作状态不正常。\"}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        FeieCloud cloud = getMockCloud(requestClient);
        CloudResponse<Device> cloudResponse = cloud.queryDevice(getMockDevice());

        Device queriedDevice = cloudResponse.getSuccessEntity();
        assertTrue(cloudResponse.isSuccess());
        assertTrue(queriedDevice.isOnline());
        assertThat(queriedDevice.getStatus(), equalTo(Device.Status.ANORMAL));
    }

    @Test
    public void when_query_device_and_device_is_online_and_normal_then_mark_device_online_and_normal() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"ret\":0,\"data\":\"在线，工作状态正常。\"}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        FeieCloud cloud = getMockCloud(requestClient);
        CloudResponse<Device> cloudResponse = cloud.queryDevice(getMockDevice());

        Device queriedDevice = cloudResponse.getSuccessEntity();
        assertTrue(cloudResponse.isSuccess());
        assertTrue(queriedDevice.isOnline());
        assertThat(queriedDevice.getStatus(), equalTo(Device.Status.NORMAL));
    }

    @Test
    public void when_update_device_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        FeieCloud cloud = getMockCloud(requestClient);
        cloud.updateDevice(getMockDevice());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched(),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 7);
                    assertThat(body.get("apiname"), equalTo("Open_printerEdit"));
                    assertThat(body.get("sn"), equalTo("01234"));
                    assertThat(body.get("name"), equalTo("快餐前台"));
                    assertThat(body.get("phonenum"), equalTo("13688889999"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_update_device_successfully_then_return_true() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"ret\":0,\"data\":true}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        FeieCloud cloud = getMockCloud(requestClient);
        CloudResponse<Boolean> cloudResponse = cloud.updateDevice(getMockDevice());

        assertTrue(cloudResponse.isSuccess());
        assertTrue(cloudResponse.getSuccessEntity());
    }

    @Test
    public void when_print_msg_order_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        FeieCloud cloud = getMockCloud(requestClient);
        cloud.printMsgOrder(getMockDevice(), getMockOrder());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched(),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 7);
                    assertThat(body.get("apiname"), equalTo("Open_printMsg"));
                    assertThat(body.get("sn"), equalTo("01234"));
                    assertThat(body.get("content"), containsString("this is order content"));
                    assertThat(body.get("times"), equalTo("3"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_print_msg_order_with_expired_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        FeieCloud cloud = getMockCloud(requestClient);
        cloud.printMsgOrder(getMockDevice(), getMockOrderWithExpired());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched(),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 8);
                    assertThat(body.get("expired"), equalTo(1_000_000_000 + 86400 + ""));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_print_msg_order_with_expired_and_backurl_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        FeieCloud cloud = getMockBackUrlCloud(requestClient);
        cloud.printMsgOrder(getMockDevice(), getMockOrderWithExpired());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched(),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 9);
                    assertThat(body.get("backurl"), equalTo("this is backurl"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_print_msg_order_successfully_then_set_the_order_id() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"ret\":0,\"data\":\"this_is_order_id\"}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        FeieCloud cloud = getMockCloud(requestClient);
        CloudResponse<Order> cloudResponse = cloud.printMsgOrder(getMockDevice(), getMockOrder());

        assertTrue(cloudResponse.isSuccess());
        assertThat(cloudResponse.getSuccessEntity().getId(), equalTo("this_is_order_id"));
    }

    @Test
    public void when_print_label_order_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        FeieCloud cloud = getMockCloud(requestClient);
        cloud.printLabelOrder(getMockDevice(), getMockOrder());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched(),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 7);
                    assertThat(body.get("apiname"), equalTo("Open_printLabelMsg"));
                    assertThat(body.get("sn"), equalTo("01234"));
                    assertThat(body.get("content"), containsString("this is order content"));
                    assertThat(body.get("times"), equalTo("3"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_print_label_order_with_expired_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        FeieCloud cloud = getMockCloud(requestClient);
        cloud.printLabelOrder(getMockDevice(), getMockOrderWithExpired());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched(),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 8);
                    assertThat(body.get("expired"), equalTo(1_000_000_000 + 86400 + ""));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_print_label_order_with_expired_and_backurl_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        FeieCloud cloud = getMockBackUrlCloud(requestClient);
        cloud.printLabelOrder(getMockDevice(), getMockOrderWithExpired());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched(),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 9);
                    assertThat(body.get("backurl"), equalTo("this is backurl"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_print_label_order_successfully_then_set_the_order_id() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"ret\":0,\"data\":\"this_is_order_id\"}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        FeieCloud cloud = getMockCloud(requestClient);
        CloudResponse<Order> cloudResponse = cloud.printLabelOrder(getMockDevice(), getMockOrder());

        assertTrue(cloudResponse.isSuccess());
        assertThat(cloudResponse.getSuccessEntity().getId(), equalTo("this_is_order_id"));
    }

    @Test
    public void when_query_order_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        FeieCloud cloud = getMockCloud(requestClient);
        cloud.queryOrder(getMockOrder());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched(),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 5);
                    assertThat(body.get("orderid"), equalTo(getMockOrder().getId()));
                    assertThat(body.get("apiname"), equalTo("Open_queryOrderState"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_query_order_has_been_printed_then_mark_the_order_printed() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"ret\":0,\"data\":true}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        FeieCloud cloud = getMockCloud(requestClient);
        CloudResponse<Order> cloudResponse = cloud.queryOrder(getMockOrder());

        assertTrue(cloudResponse.isSuccess());
        assertTrue(cloudResponse.getSuccessEntity().isPrinted());
    }

    @Test
    public void when_query_order_has_not_been_printed_then_mark_the_order_not_printed() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"ret\":0,\"data\":false}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        FeieCloud cloud = getMockCloud(requestClient);
        CloudResponse<Order> cloudResponse = cloud.queryOrder(getMockOrder());

        assertTrue(cloudResponse.isSuccess());
        assertFalse(cloudResponse.getSuccessEntity().isPrinted());
    }

    @Test
    public void when_query_device_order_stat_then_request_params_are_matched() {
        RequestClient<String> requestClient = getMockRequestClientWithNetworkFail();
        FeieCloud cloud = getMockCloud(requestClient);
        cloud.queryDeviceOrders(getMockDevice(), getMockQueryOption());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched(),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 6);
                    assertThat(body.get("sn"), equalTo(getMockDevice().getSn()));
                    assertThat(body.get("date"), equalTo(getMockQueryOption().getDate()));
                    assertThat(body.get("apiname"), equalTo("Open_queryOrderInfoByDate"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_query_device_order_stat_successfully_then_return_the_stat() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"ret\":0,\"data\":{\"print\":6,\"waiting\":1}}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        FeieCloud cloud = getMockCloud(requestClient);
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
        FeieCloud cloud = getMockCloud(requestClient);
        cloud.clearDeviceOrders(getMockDevice());
        verify(requestClient).httpPost(
                ensureRequestUrlMatched(),
                Mockito.<Map<String, String>>argThat(body -> {
                    ensureCommonBodyMatched(body, 5);
                    assertThat(body.get("sn"), equalTo(getMockDevice().getSn()));
                    assertThat(body.get("apiname"), equalTo("Open_delPrinterSqs"));
                    return true;
                }),
                ensureRequestHeadersMatched()
        );
    }

    @Test
    public void when_clear_device_order_successfully_then_return_true() {
        CloudResponse<String> requestResponse = CloudResponse.ofSuccess("{\"ret\":0,\"data\":true}}");
        RequestClient<String> requestClient = getMockRequestClient(requestResponse);
        FeieCloud cloud = getMockCloud(requestClient);
        CloudResponse<Boolean> cloudResponse = cloud.clearDeviceOrders(getMockDevice());

        assertTrue(cloudResponse.isSuccess());
        assertTrue(cloudResponse.getSuccessEntity());
    }
}
