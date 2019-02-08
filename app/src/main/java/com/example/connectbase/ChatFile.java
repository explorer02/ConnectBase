package com.example.connectbase;

public class ChatFile {

    private String sender, messageType, description, fileUrl, status, fileName, seen;
    private long time;

    public ChatFile() {
    }


    public ChatFile(String sender, String messageType, String description, String file, long time, String status, String filename, String seen) {
        this.sender = sender;
        this.messageType = messageType;
        this.description = description;
        this.fileUrl = file;
        this.time = time;
        this.status = status;
        this.fileName = filename;
        this.seen = seen;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSeen() {
        return seen;
    }

    public void setSeen(String seen) {
        this.seen = seen;
    }
}
