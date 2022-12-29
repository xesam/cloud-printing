package io.github.xesam.cloud.xpyun;

import java.util.List;

class DevicesResponse extends VendorResponse<DevicesResponse.Result> {
    static final class Result {
        public List<String> success;
        public List<String> fail;
        public List<String> failMsg;
    }
}
