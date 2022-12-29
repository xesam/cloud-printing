package io.github.xesam.cloud.feie;

import java.util.List;

class DeviceOrderStatResponse extends VendorResponse<DeviceOrderStatResponse.Result> {
    static final class Result {
        public int print;
        public int waiting;
    }
}
