package io.github.xesam.cloud;

public class Order {
    private String id;
    private String content;
    private long expiredEpochSecond = 0;
    private int copies = 1;
    private boolean printed;
    private String createTime;
    private String printTime;

    public Order() {
    }

    public String getId() {
        return id;
    }

    public Order setId(String id) {
        this.id = id;
        return this;
    }

    public String getContent() {
        return content;
    }

    public Order setContent(String content) {
        this.content = content;
        return this;
    }

    public long getExpiredEpochSecond() {
        return expiredEpochSecond;
    }

    public Order setExpiredEpochSecond(long seconds) {
        this.expiredEpochSecond = seconds;
        return this;
    }

    public int getCopies() {
        return copies;
    }

    public Order setCopies(int copies) {
        this.copies = copies;
        return this;
    }

    public boolean isPrinted() {
        return printed;
    }

    public Order markPrinted() {
        this.printed = true;
        return this;
    }

    public Order markWaiting() {
        this.printed = false;
        return this;
    }

    public String getCreateTime() {
        return createTime;
    }

    public Order setCreateTime(String createTime) {
        this.createTime = createTime;
        return this;
    }

    public String getPrintTime() {
        return printTime;
    }

    public Order setPrintTime(String printTime) {
        this.printTime = printTime;
        return this;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                ", expiredEpochSecond=" + expiredEpochSecond +
                ", copies=" + copies +
                ", printed=" + printed +
                ", createTime='" + createTime + '\'' +
                ", printTime='" + printTime + '\'' +
                '}';
    }
}
