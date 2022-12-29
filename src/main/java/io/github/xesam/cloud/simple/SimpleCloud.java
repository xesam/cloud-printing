package io.github.xesam.cloud.simple;

import io.github.xesam.cloud.*;

public class SimpleCloud implements CloudApi {
    protected CloudAuth cloudAuth;
    protected CloudClock cloudClock;
    protected UrlRewriter urlRewriter;
    protected RequestClient<String> requestClient;
    protected ResponseParser responseParser;

    public SimpleCloud(CloudAuth cloudAuth) {
        this.cloudAuth = cloudAuth;
        this.setCloudClock(new DefaultCloudClock());
        this.setRequestClient(new SimpleRequestClient());
        this.setResponseParser(new SimpleResponseParser());
    }

    public void setCloudClock(CloudClock cloudClock) {
        this.cloudClock = cloudClock;
    }

    public void setUrlRewriter(UrlRewriter urlRewriter) {
        this.urlRewriter = urlRewriter;
    }

    public void setRequestClient(RequestClient<String> requestClient) {
        this.requestClient = requestClient;
    }

    public void setResponseParser(ResponseParser responseParser) {
        this.responseParser = responseParser;
    }

    private <T> CloudResponse<T> Todo() {
        return CloudResponse.ofFail("not supported");
    }

    @Override
    public CloudResponse<Boolean> addDevice(Device device) {
        return Todo();
    }

    @Override
    public CloudResponse<Boolean> deleteDevice(Device device) {
        return Todo();
    }

    @Override
    public CloudResponse<Device> queryDevice(Device device) {
        return Todo();
    }

    @Override
    public CloudResponse<Boolean> updateDevice(Device device) {
        return Todo();
    }

    @Override
    public CloudResponse<Order> printMsgOrder(Device device, Order order) {
        return Todo();
    }

    @Override
    public CloudResponse<Order> printLabelOrder(Device device, Order order) {
        return Todo();
    }

    @Override
    public CloudResponse<Order> queryOrder(Order order) {
        return Todo();
    }

    @Override
    public CloudResponse<DeviceOrderStat> queryDeviceOrders(Device device, QueryOption queryOption) {
        return Todo();
    }

    @Override
    public CloudResponse<Boolean> clearDeviceOrders(Device device) {
        return Todo();
    }
}
