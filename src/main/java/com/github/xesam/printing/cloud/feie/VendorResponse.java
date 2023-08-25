package com.github.xesam.printing.cloud.feie;

class VendorResponse<T> {
    private static final int OK = 0;
    public String msg;
    public int ret = -1;
    public int serverExecutedTime;
    public T data;

    public boolean isOK() {
        return ret == OK;
    }
}
