package io.github.xesam.cloud.xpyun;

class DevicesOrderStatResponse extends VendorResponse<DevicesOrderStatResponse.Result> {
    static final class Result {
        public int printed;
        public int waiting;
    }
}
