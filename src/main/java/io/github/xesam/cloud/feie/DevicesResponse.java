package io.github.xesam.cloud.feie;

import java.util.List;

class DevicesResponse extends VendorResponse<DevicesResponse.Result> {
    static final class Result {
        public List<String> ok;
        public List<String> no;
    }
}
