package io.github.xesam.cloud;

public class Device {
    public enum Status {
        NORMAL, ANORMAL
    }

    public enum CutMode {
        AutoCut, ManualCut
    }

    public enum Voice {
        UNKNOWN, OFF, DIDI, HUMAN
    }

    private final String sn;
    private String key;
    private String name;
    private String cardno;
    private boolean online;
    private Status status = Status.NORMAL;
    private CutMode cutMode;
    private Voice voice = Voice.UNKNOWN;
    private int volume = 0;

    public Device(String sn) {
        this.sn = sn;
    }

    public String getSn() {
        return sn;
    }

    public String getKey() {
        return key;
    }

    public Device setKey(String key) {
        this.key = key;
        return this;
    }

    public String getName() {
        return name;
    }

    public Device setName(String name) {
        this.name = name;
        return this;
    }

    public String getCardno() {
        return cardno;
    }

    public Device setCardno(String cardno) {
        this.cardno = cardno;
        return this;
    }

    public CutMode getCutMode() {
        return cutMode;
    }

    public void setCutMode(CutMode cutMode) {
        this.cutMode = cutMode;
    }

    public boolean isOnline() {
        return online;
    }

    public Device markOnline() {
        this.online = true;
        return this;
    }

    public Device markOffline() {
        this.online = false;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public Device setStatus(Status status) {
        this.status = status;
        return this;
    }

    public Voice getVoice() {
        return voice;
    }

    public Device setVoice(Voice voice) {
        this.voice = voice;
        return this;
    }

    public int getVolume() {
        return volume;
    }

    public Device setVolume(int volume) {
        this.volume = volume;
        return this;
    }

    @Override
    public String toString() {
        return "Device{" +
                "sn='" + sn + '\'' +
                ", key='" + key + '\'' +
                ", name='" + name + '\'' +
                ", cardno='" + cardno + '\'' +
                ", online=" + online +
                ", status=" + status +
                ", cutMode=" + cutMode +
                ", voice=" + voice +
                ", volume=" + volume +
                '}';
    }
}
