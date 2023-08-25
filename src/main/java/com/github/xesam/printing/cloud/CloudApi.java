package com.github.xesam.printing.cloud;

public interface CloudApi {
    /**
     * 新增设备
     */
    CloudResponse<Boolean> addDevice(Device device);

    /**
     * 删除设备
     */
    CloudResponse<Boolean> deleteDevice(Device device);

    /**
     * 查询设备信息
     */
    CloudResponse<Device> queryDevice(Device device);

    /**
     * 部分更新设备信息，设备不支持的属性会直接忽略
     */
    CloudResponse<Boolean> updateDevice(Device device);

    /**
     * 打印小票订单，如果设备不支持，会直接返回失败
     */
    CloudResponse<Order> printMsgOrder(Device device, Order order);

    /**
     * 打印标签订单，如果设备不支持，会直接返回失败
     */
    CloudResponse<Order> printLabelOrder(Device device, Order order);

    /**
     * 查询指定订单的打印情况，Order 必须要指定 id
     */
    CloudResponse<Order> queryOrder(Order order);

    /**
     * 查询打印机的订单信息
     */
    CloudResponse<DeviceOrderStat> queryDeviceOrders(Device device, QueryOption queryOption);

    /**
     * 清空指定设备的打印队列
     */
    CloudResponse<Boolean> clearDeviceOrders(Device device);
}
