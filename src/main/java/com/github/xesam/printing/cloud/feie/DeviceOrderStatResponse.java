package com.github.xesam.printing.cloud.feie;

class DeviceOrderStatResponse extends VendorResponse<DeviceOrderStatResponse.Result> {
    static final class Result {
        public int print;
        public int waiting;
    }
}
