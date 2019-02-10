package com.example.connectbase;

public class UserId {
    private String star;
    private long time;

    public UserId() {
    }

    public UserId(String star, long time) {
        this.star = star;
        this.time = time;
    }

    public String getStar() {
        return star;
    }

    public void setStar(String star) {
        this.star = star;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
