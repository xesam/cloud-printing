package com.github.xesam.printing.cloud.spyun;

class VendorResponse {
    private static final int OK = 0;
    public int errorcode = -1;
    public String errormsg;

    public boolean isOK() {
        return errorcode == OK;
    }
}
