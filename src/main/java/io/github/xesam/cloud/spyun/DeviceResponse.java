package io.github.xesam.cloud.spyun;

class DeviceResponse extends VendorResponse {
    public String sn;
    public String name;
    public int online;
    public int status;
    public String imsi;
    public int sqsnum;
    public String model;
    public int auto_cut;
    public String voice;

    public boolean isOnline() {
        return online == 1;
    }

    public boolean isNormal() {
        return status == 0;
    }

    public boolean isAutoCut() {
        return auto_cut == 1;
    }
}
