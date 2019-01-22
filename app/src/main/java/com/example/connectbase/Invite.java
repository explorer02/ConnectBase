package com.example.connectbase;

public class Invite {
    private String request_type;

    public Invite(String request_type) {
        this.request_type = request_type;
    }

    public Invite() {
    }

    public String getRequest_type() {
        return request_type;
    }

    public void setRequest_type(String request_type) {
        this.request_type = request_type;
    }
}
