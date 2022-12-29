package io.github.xesam.cloud.xpyun;

class VendorResponse<T> {
    private static final int OK = 0;
    public String msg;
    public int code = -1;
    public int serverExecutedTime;
    public T data;

    public boolean isOK() {
        return code == OK;
    }
}
