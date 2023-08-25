package com.github.xesam.printing.cloud.feie;


import com.github.xesam.printing.cloud.*;
import com.github.xesam.printing.cloud.simple.SimpleCloud;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FeieCloud extends SimpleCloud implements CloudApi {

    private String backurl;

    public FeieCloud(CloudAuth cloudAuth) {
        this(cloudAuth, null);
    }

    public FeieCloud(CloudAuth cloudAuth, String backurl) {
        super(cloudAuth);
        this.setBackUrl(backurl).setUrlRewriter(url -> "https://api.feieyun.cn/Api/Open/" + url);
    }

    public FeieCloud setBackUrl(String backurl) {
        this.backurl = backurl;
        return this;
    }

    private String getValue(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private Map<String, String> createRequestHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        return headers;
    }

    private CloudResponse<String> request(String apiName, Map<String, String> data) {
        String epochSecond = this.cloudClock.getEpochSecond() + "";
        data.put("user", this.cloudAuth.getAppId());
        data.put("stime", epochSecond);
        data.put("sig", MessageDigests.SHA1(this.cloudAuth.getAppId() + this.cloudAuth.getSecret() + epochSecond));
        data.put("apiname", apiName);
        data.entrySet().removeIf(ele -> Objects.isNull(ele.getValue()) || ele.getValue().trim().isEmpty());
        return this.requestClient.httpPost(this.urlRewriter.getUrl(""), data, this.createRequestHeaders());
    }

    private boolean checkVendorResponse(VendorResponse<?> vendorResponse) {
        return vendorResponse != null && vendorResponse.isOK();
    }

    private CloudResponse<Boolean> createBoolResponse(CloudResponse<String> requestResponse) {
        if (!requestResponse.isSuccess()) {
            return CloudResponse.ofFail(requestResponse.getFailMessage());
        }
        BoolResponse vendorResponse = responseParser.parse(requestResponse.getSuccessEntity(), BoolResponse.class);
        if (this.checkVendorResponse(vendorResponse)) {
            return CloudResponse.ofSuccess(true);
        }
        return CloudResponse.ofFail(vendorResponse.msg);
    }

    private CloudResponse<Boolean> createDevicesResponse(CloudResponse<String> requestResponse) {
        if (!requestResponse.isSuccess()) {
            return CloudResponse.ofFail(requestResponse.getFailMessage());
        }
        DevicesResponse devicesResponse = responseParser.parse(requestResponse.getSuccessEntity(), DevicesResponse.class);
        if (!this.checkVendorResponse(devicesResponse)) {
            return CloudResponse.ofFail(devicesResponse.msg);
        }
        if (devicesResponse.data.ok.size() > 0) {
            return CloudResponse.ofSuccess(true);
        } else {
            return CloudResponse.ofFail(devicesResponse.data.no.get(0));
        }
    }

    private String getDevicePrinterContent(Device device) {
        return String.format("%s#%s#%s#%s",
                getValue(device.getSn()),
                getValue(device.getKey()),
                getValue(device.getName()),
                getValue(device.getCardno())
        );
    }

    @Override
    public CloudResponse<Boolean> addDevice(Device device) {
        Map<String, String> data = new HashMap<>();
        String printerContent = this.getDevicePrinterContent(device);
        data.put("printerContent", printerContent);
        CloudResponse<String> response = this.request("Open_printerAddlist", data);
        return this.createDevicesResponse(response);
    }

    private String getDeviceSn(Device device) {
        return getValue(device.getSn());
    }

    @Override
    public CloudResponse<Boolean> deleteDevice(Device device) {
        Map<String, String> data = new HashMap<>();
        String snlist = this.getDeviceSn(device);
        data.put("snlist", snlist);
        CloudResponse<String> response = this.request("Open_printerDelList", data);
        return this.createDevicesResponse(response);
    }

    @Override
    public CloudResponse<Device> queryDevice(Device device) {
        Map<String, String> data = new HashMap<>();
        data.put("sn", getValue(device.getSn()));
        CloudResponse<String> response = this.request("Open_queryPrinterStatus", data);
        if (!response.isSuccess()) {
            return CloudResponse.ofFail(response.getFailMessage());
        }
        StringResponse vendorResponse = responseParser.parse(response.getSuccessEntity(), StringResponse.class);
        if (!vendorResponse.isOK()) {
            return CloudResponse.ofFail(vendorResponse.msg);
        }
        if (vendorResponse.data.startsWith("在线，工作状态正常")) {
            device.markOnline().setStatus(Device.Status.NORMAL);
        } else if (vendorResponse.data.startsWith("在线，工作状态不正常")) {
            device.markOnline().setStatus(Device.Status.ANORMAL);
        } else {
            device.markOffline().setStatus(Device.Status.ANORMAL);
        }
        return CloudResponse.ofSuccess(device);
    }

    @Override
    public CloudResponse<Boolean> updateDevice(Device device) {
        Map<String, String> data = new HashMap<>();
        data.put("sn", getValue(device.getSn()));
        data.put("name", getValue(device.getName()));
        data.put("phonenum", getValue(device.getCardno()));
        CloudResponse<String> response = this.request("Open_printerEdit", data);
        return this.createBoolResponse(response);
    }

    private CloudResponse<Order> printOrder(String apiName, Device device, Order order) {
        Map<String, String> data = new HashMap<>();
        data.put("sn", device.getSn());
        data.put("content", order.getContent());
        data.put("times", String.valueOf(order.getCopies()));
        if (order.getExpiredEpochSecond() > 0) {
            data.put("expired", String.valueOf(order.getExpiredEpochSecond()));
        }
        if (backurl != null) {
            data.put("backurl", backurl);
        }
        CloudResponse<String> response = this.request(apiName, data);
        if (!response.isSuccess()) {
            return CloudResponse.ofFail(response.getFailMessage());
        }
        StringResponse vendorResponse = responseParser.parse(response.getSuccessEntity(), StringResponse.class);
        if (!this.checkVendorResponse(vendorResponse)) {
            return CloudResponse.ofFail(vendorResponse.msg);
        }
        order.setId(vendorResponse.data);
        return CloudResponse.ofSuccess(order);
    }

    @Override
    public CloudResponse<Order> printMsgOrder(Device device, Order order) {
        return this.printOrder("Open_printMsg", device, order);
    }

    @Override
    public CloudResponse<Order> printLabelOrder(Device device, Order order) {
        return this.printOrder("Open_printLabelMsg", device, order);
    }

    @Override
    public CloudResponse<Order> queryOrder(Order order) {
        Map<String, String> data = new HashMap<>();
        data.put("orderid", order.getId());
        CloudResponse<String> response = this.request("Open_queryOrderState", data);
        if (!response.isSuccess()) {
            return CloudResponse.ofFail(response.getFailMessage());
        }
        BoolResponse vendorResponse = responseParser.parse(response.getSuccessEntity(), BoolResponse.class);
        if (!this.checkVendorResponse(vendorResponse)) {
            return CloudResponse.ofFail(vendorResponse.msg);
        }
        if (vendorResponse.data) {
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
        CloudResponse<String> response = this.request("Open_queryOrderInfoByDate", data);
        if (!response.isSuccess()) {
            return CloudResponse.ofFail(response.getFailMessage());
        }
        DeviceOrderStatResponse vendorResponse = responseParser.parse(response.getSuccessEntity(), DeviceOrderStatResponse.class);
        if (!this.checkVendorResponse(vendorResponse)) {
            return CloudResponse.ofFail(vendorResponse.msg);
        }
        return CloudResponse.ofSuccess(new DeviceOrderStat(device.getSn(), queryOption.getDate(), vendorResponse.data.print, vendorResponse.data.waiting));
    }

    @Override
    public CloudResponse<Boolean> clearDeviceOrders(Device device) {
        Map<String, String> data = new HashMap<>();
        data.put("sn", device.getSn());
        CloudResponse<String> vendorResponse = this.request("Open_delPrinterSqs", data);
        return this.createBoolResponse(vendorResponse);
    }
}
