package io.github.xesam.cloud.spyun;

class OrderResponse extends VendorResponse {
    public boolean status;
    public String print_time;

    public boolean isPrinted() {
        return status;
    }
}
