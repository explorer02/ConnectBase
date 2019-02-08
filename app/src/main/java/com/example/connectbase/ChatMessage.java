package com.example.connectbase;

public class ChatMessage {

    private String messageType, message, sender, seen;
    private long time;

    public ChatMessage() {
    }

    public ChatMessage(String message_type, String message, String sender, long time, String seen) {
        this.messageType = message_type;
        this.message = message;
        this.sender = sender;
        this.time = time;
        this.seen = seen;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getSeen() {
        return seen;
    }

    public void setSeen(String seen) {
        this.seen = seen;
    }
}
