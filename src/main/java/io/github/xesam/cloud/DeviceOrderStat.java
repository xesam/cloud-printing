package io.github.xesam.cloud;

public final class DeviceOrderStat {
    private final String deviceSn;
    private final String orderDate;
    private final int printedCount;
    private final int waitingCount;

    public DeviceOrderStat(String deviceSn, String orderDate, int printedCount, int waitingCount) {
        this.deviceSn = deviceSn;
        this.orderDate = orderDate;
        this.printedCount = printedCount;
        this.waitingCount = waitingCount;
    }

    public String getDeviceSn() {
        return deviceSn;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public int getPrintedCount() {
        return printedCount;
    }

    public int getWaitingCount() {
        return waitingCount;
    }

    @Override
    public String toString() {
        return "DeviceOrderStat{" +
                "deviceSn='" + deviceSn + '\'' +
                ", orderDate='" + orderDate + '\'' +
                ", printedCount=" + printedCount +
                ", waitingCount=" + waitingCount +
                '}';
    }
}
