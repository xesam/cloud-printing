package io.github.xesam.cloud.spyun;

import io.github.xesam.cloud.*;
import io.github.xesam.cloud.simple.SimpleCloud;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SpyunCloud extends SimpleCloud implements CloudApi {

    public SpyunCloud(CloudAuth cloudAuth) {
        super(cloudAuth);
        this.setUrlRewriter(url -> "https://open.spyun.net/v1/printer/" + url);
    }

    private Map<String, String> createRequestHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        return headers;
    }

    private Map<String, String> fullfill(Map<String, String> data) {
        data.put("appid", this.cloudAuth.getAppId());
        data.put("timestamp", String.valueOf(this.cloudClock.getEpochSecond()));
        data.entrySet().removeIf(ele -> Objects.isNull(ele.getValue()) || ele.getValue().trim().isEmpty());

        String paramStr = data.entrySet()
                .stream()
                .map(ele -> ele.getKey() + "=" + ele.getValue())
                .collect(Collectors.joining("&"));

        data.put("sign", MessageDigests.MD5(paramStr + "&appsecret=" + this.cloudAuth.getSecret()));
        return data;
    }

    private CloudResponse<String> request(String method, String apiName, Map<String, String> data) {
        Map<String, String> reqData = this.fullfill(data);
        switch (method.toUpperCase()) {
            case "POST":
                return this.requestClient.httpPost(this.urlRewriter.getUrl(apiName), reqData, this.createRequestHeaders());
            case "DELETE":
                return this.requestClient.httpDelete(this.urlRewriter.getUrl(apiName), reqData, this.createRequestHeaders());
            case "PATCH":
                return this.requestClient.httpPatch(this.urlRewriter.getUrl(apiName), reqData, this.createRequestHeaders());
            default:
                return this.requestClient.httpGet(this.urlRewriter.getUrl(apiName), reqData, this.createRequestHeaders());
        }
    }

    private boolean checkVendorResponse(VendorResponse vendorResponse) {
        return vendorResponse != null && vendorResponse.isOK();
    }

    private CloudResponse<Boolean> createBoolResponse(CloudResponse<String> requestResponse) {
        if (!requestResponse.isSuccess()) {
            return CloudResponse.ofFail(requestResponse.getFailMessage());
        }
        VendorResponse vendorResponse = responseParser.parse(requestResponse.getSuccessEntity(), VendorResponse.class);
        if (this.checkVendorResponse(vendorResponse)) {
            return CloudResponse.ofSuccess(true);
        } else {
            return CloudResponse.ofFail(vendorResponse.errormsg);
        }
    }

    @Override
    public CloudResponse<Boolean> addDevice(Device device) {
        Map<String, String> data = new TreeMap<>();
        data.put("sn", device.getSn());
        data.put("pkey", device.getKey());
        data.put("name", device.getName());
        CloudResponse<String> response = this.request("post", "add", data);
        return this.createBoolResponse(response);
    }

    @Override
    public CloudResponse<Boolean> deleteDevice(Device device) {
        Map<String, String> data = new TreeMap<>();
        data.put("sn", device.getSn());
        CloudResponse<String> response = this.request("delete", "delete", data);
        return this.createBoolResponse(response);
    }

    @Override
    public CloudResponse<Device> queryDevice(Device device) {
        Map<String, String> data = new TreeMap<>();
        data.put("sn", device.getSn());
        CloudResponse<String> response = this.request("get", "info", data);
        if (!response.isSuccess()) {
            return CloudResponse.ofFail(response.getFailMessage());
        }
        DeviceResponse vendorResponse = responseParser.parse(response.getSuccessEntity(), DeviceResponse.class);
        if (!this.checkVendorResponse(vendorResponse)) {
            return CloudResponse.ofFail(response.getFailMessage());
        }
        device.setName(vendorResponse.name)
                .setCardno(vendorResponse.imsi)
                .setStatus(vendorResponse.isNormal() ? Device.Status.NORMAL : Device.Status.ANORMAL);
        if (vendorResponse.isOnline()) {
            device.markOnline();
        } else {
            device.markOffline();
        }
        if (vendorResponse.isAutoCut()) {
            device.setCutMode(Device.CutMode.AutoCut);
        } else {
            device.setCutMode(Device.CutMode.ManualCut);
        }
        return CloudResponse.ofSuccess(device);
    }

    @Override
    public CloudResponse<Boolean> updateDevice(Device device) {
        Map<String, String> data = new TreeMap<>();
        data.put("sn", device.getSn());
        data.put("name", device.getName());
        CloudResponse<String> response = this.request("patch", "update", data);
        return this.createBoolResponse(response);
    }

    @Override
    public CloudResponse<Order> printMsgOrder(Device device, Order order) {
        Map<String, String> data = new TreeMap<>();
        data.put("sn", device.getSn());
        data.put("content", order.getContent());
        data.put("times", String.valueOf(order.getCopies()));
        CloudResponse<String> response = this.request("post", "print", data);
        if (!response.isSuccess()) {
            return CloudResponse.ofFail(response.getFailMessage());
        }
        PrintResponse vendorResponse = responseParser.parse(response.getSuccessEntity(), PrintResponse.class);
        if (!this.checkVendorResponse(vendorResponse)) {
            return CloudResponse.ofFail(response.getFailMessage());
        }
        order.setId(vendorResponse.id);
        return CloudResponse.ofSuccess(order);
    }

    @Override
    public CloudResponse<Order> queryOrder(Order order) {
        Map<String, String> data = new TreeMap<>();
        data.put("id", order.getId());
        CloudResponse<String> response = this.request("get", "order/status", data);
        if (!response.isSuccess()) {
            return CloudResponse.ofFail(response.getFailMessage());
        }
        OrderResponse vendorResponse = responseParser.parse(response.getSuccessEntity(), OrderResponse.class);
        if (!this.checkVendorResponse(vendorResponse)) {
            return CloudResponse.ofFail(vendorResponse.errormsg);
        }
        if (vendorResponse.status) {
            order.markPrinted();
        } else {
            order.markWaiting();
        }
        return CloudResponse.ofSuccess(order);
    }

    @Override
    public CloudResponse<DeviceOrderStat> queryDeviceOrders(Device device, QueryOption queryOption) {
        Map<String, String> data = new HashMap<>();
        data.put("sn", device.getSn());
        data.put("date", queryOption.getDate());
        CloudResponse<String> response = this.request("get", "order/number", data);
        if (!response.isSuccess()) {
            return CloudResponse.ofFail(response.getFailMessage());
        }
        DeviceOrderStatResponse vendorResponse = responseParser.parse(response.getSuccessEntity(), DeviceOrderStatResponse.class);
        if (!this.checkVendorResponse(vendorResponse)) {
            return CloudResponse.ofFail(vendorResponse.errormsg);
        }
        return CloudResponse.ofSuccess(new DeviceOrderStat(device.getSn(), queryOption.getDate(), vendorResponse.number, 0));
    }

    @Override
    public CloudResponse<Boolean> clearDeviceOrders(Device device) {
        Map<String, String> data = new TreeMap<>();
        data.put("sn", device.getSn());
        CloudResponse<String> response = this.request("delete", "cleansqs", data);
        return this.createBoolResponse(response);
    }
}
