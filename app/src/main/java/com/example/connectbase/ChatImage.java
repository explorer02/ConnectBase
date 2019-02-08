package com.example.connectbase;

public class ChatImage {

    private String sender, messageType, description, imageName, imageUrl, thumbImage, status, seen;
    private long time;

    public ChatImage() {
    }

    public ChatImage(String sender, String messageType, String description, String imageName, String imageUrl, String thumbImage, long time, String status, String seen) {
        this.sender = sender;
        this.messageType = messageType;
        this.description = description;
        this.imageName = imageName;
        this.imageUrl = imageUrl;
        this.thumbImage = thumbImage;
        this.time = time;
        this.status = status;
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

    public String getThumbImage() {
        return thumbImage;
    }

    public void setThumbImage(String thumbImage) {
        this.thumbImage = thumbImage;
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

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSeen() {
        return seen;
    }

    public void setSeen(String seen) {
        this.seen = seen;
    }
}
