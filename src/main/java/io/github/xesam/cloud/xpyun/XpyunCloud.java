package io.github.xesam.cloud.xpyun;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.xesam.cloud.*;
import io.github.xesam.cloud.simple.SimpleCloud;

import java.util.*;

public class XpyunCloud extends SimpleCloud implements CloudApi {

    private static final int DONT_USE_BACK_URL = -1;
    private int backurlFlag;

    public XpyunCloud(CloudAuth cloudAuth) {
        this(cloudAuth, DONT_USE_BACK_URL);
    }

    public XpyunCloud(CloudAuth cloudAuth, int backurlFlag) {
        super(cloudAuth);
        this.setBackUrl(backurlFlag).setUrlRewriter(url -> "https://open.xpyun.net/api/openapi/xprinter/" + url);
    }

    public XpyunCloud setBackUrl(int backurlFlag) {
        this.backurlFlag = backurlFlag;
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
        headers.put("Content-Type", "application/json;charset=UTF-8");
        return headers;
    }

    private CloudResponse<String> request(String apiName, Map<String, Object> data) {
        data.entrySet().removeIf(ele -> Objects.isNull(ele.getValue()) || String.valueOf(ele.getValue()).trim().isEmpty());
        String epochSecond = this.cloudClock.getEpochSecond() + "";
        data.put("user", this.cloudAuth.getAppId());
        data.put("timestamp", epochSecond);
        data.put("sign", MessageDigests.SHA1(this.cloudAuth.getAppId() + this.cloudAuth.getSecret() + epochSecond));
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonData = "";
        try {
            jsonData = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return this.requestClient.httpPost(this.urlRewriter.getUrl(apiName), jsonData, this.createRequestHeaders());
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
        DevicesResponse vendorResponse = responseParser.parse(requestResponse.getSuccessEntity(), DevicesResponse.class);
        if (!this.checkVendorResponse(vendorResponse)) {
            return CloudResponse.ofFail(vendorResponse.msg);
        }
        if (vendorResponse.data.success.size() > 0) {
            return CloudResponse.ofSuccess(true);
        } else {
            return CloudResponse.ofFail(vendorResponse.data.failMsg.get(0));
        }
    }

    @Override
    public CloudResponse<Boolean> addDevice(Device device) {
        Map<String, Object> data = new HashMap<>();
        Map<String, String> deviceObj = new HashMap<>();
        deviceObj.put("sn", device.getSn());
        deviceObj.put("name", device.getName());
        deviceObj.entrySet().removeIf(ele -> Objects.isNull(ele.getValue()) || ele.getValue().trim().isEmpty());
        List<Map<String, String>> items = new ArrayList<>();
        items.add(deviceObj);
        data.put("items", items);
        CloudResponse<String> response = this.request("addPrinters", data);
        return this.createDevicesResponse(response);
    }

    @Override
    public CloudResponse<Boolean> deleteDevice(Device device) {
        Map<String, Object> data = new HashMap<>();
        List<String> snlist = new ArrayList<>();
        snlist.add(getValue(device.getSn()));
        data.put("snlist", snlist);
        CloudResponse<String> response = this.request("delPrinters", data);
        return this.createDevicesResponse(response);
    }

    @Override
    public CloudResponse<Device> queryDevice(Device device) {
        Map<String, Object> data = new HashMap<>();
        data.put("sn", getValue(device.getSn()));
        CloudResponse<String> response = this.request("queryPrinterStatus", data);
        if (!response.isSuccess()) {
            return CloudResponse.ofFail(response.getFailMessage());
        }
        IntResponse vendorResponse = responseParser.parse(response.getSuccessEntity(), IntResponse.class);
        if (!this.checkVendorResponse(vendorResponse)) {
            return CloudResponse.ofFail(vendorResponse.msg);
        }
        if (vendorResponse.data == 1) {
            device.markOnline().setStatus(Device.Status.NORMAL);
            return CloudResponse.ofSuccess(device);
        } else if (vendorResponse.data == 2) {
            device.markOnline().setStatus(Device.Status.ANORMAL);
            return CloudResponse.ofSuccess(device);
        } else {
            device.markOffline().setStatus(Device.Status.ANORMAL);
            return CloudResponse.ofSuccess(device);
        }
    }

    @Override
    public CloudResponse<Boolean> updateDevice(Device device) {
        Map<String, Object> data = new HashMap<>();
        data.put("sn", getValue(device.getSn()));
        data.put("name", getValue(device.getName()));
        data.put("cardno", getValue(device.getCardno()));
        CloudResponse<String> response = this.request("updPrinter", data);
        return this.createBoolResponse(response);
    }

    private CloudResponse<Order> printOrder(String apiName, Device device, Order order) {
        Map<String, Object> data = new HashMap<>();
        data.put("sn", device.getSn());
        data.put("content", order.getContent());
        long expiresIn = order.getExpiredEpochSecond() - this.cloudClock.getEpochSecond();
        if (expiresIn > 0) {
            data.put("expiresIn", expiresIn);
            data.put("mode", 1);
        }
        data.put("copies", order.getCopies());
        if (backurlFlag != DONT_USE_BACK_URL) {
            data.put("backurlFlag", backurlFlag);
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
        return this.printOrder("print", device, order);
    }

    @Override
    public CloudResponse<Order> printLabelOrder(Device device, Order order) {
        return this.printOrder("printLabel", device, order);
    }

    @Override
    public CloudResponse<Order> queryOrder(Order order) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getId());
        CloudResponse<String> response = this.request("queryOrderState", data);
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
        Map<String, Object> data = new HashMap<>();
        data.put("sn", device.getSn());
        data.put("date", queryOption.getDate());
        CloudResponse<String> response = this.request("queryOrderStatis", data);
        if (!response.isSuccess()) {
            return CloudResponse.ofFail(response.getFailMessage());
        }
        DevicesOrderStatResponse vendorResponse = responseParser.parse(response.getSuccessEntity(), DevicesOrderStatResponse.class);
        if (!this.checkVendorResponse(vendorResponse)) {
            return CloudResponse.ofFail(vendorResponse.msg);
        }
        return CloudResponse.ofSuccess(new DeviceOrderStat(device.getSn(), queryOption.getDate(), vendorResponse.data.printed, vendorResponse.data.waiting));
    }

    @Override
    public CloudResponse<Boolean> clearDeviceOrders(Device device) {
        Map<String, Object> data = new HashMap<>();
        data.put("sn", device.getSn());
        CloudResponse<String> response = this.request("delPrinterQueue", data);
        return this.createBoolResponse(response);
    }
}
