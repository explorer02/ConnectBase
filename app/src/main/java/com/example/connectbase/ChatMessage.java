package com.example.connectbase;

public class ChatMessage {

    private String messageType, message, sender, time;

    public ChatMessage(String message_type, String message, String sender, String time) {
        this.messageType = message_type;
        this.message = message;
        this.sender = sender;
        this.time = time;
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

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
