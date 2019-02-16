package com.example.connectbase;

class ChatFile {

    public String sender, messageType, description, fileUrl, fileName, seen;
    public long time, size;

    ChatFile(String sender, String messageType, String description, String fileUrl, long time, String filename, String seen, long size) {
        this.sender = sender;
        this.messageType = messageType;
        this.description = description;
        this.fileUrl = fileUrl;
        this.time = time;
        this.fileName = filename;
        this.seen = seen;
        this.size = size;
    }

    ChatFile() {
    }

    String getSender() {
        return sender;
    }

    void setSender(String sender) {
        this.sender = sender;
    }

    String getMessageType() {
        return messageType;
    }

    void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    String getFileUrl() {
        return fileUrl;
    }

    void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    long getTime() {
        return time;
    }

    void setTime(long time) {
        this.time = time;
    }

    String getFileName() {
        return fileName;
    }

    void setFileName(String fileName) {
        this.fileName = fileName;
    }

    String getSeen() {
        return seen;
    }

    void setSeen(String seen) {
        this.seen = seen;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
