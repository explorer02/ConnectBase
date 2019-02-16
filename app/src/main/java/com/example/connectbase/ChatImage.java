package com.example.connectbase;

class ChatImage {

    public String sender, messageType, description, imageName, imageUrl, thumbImage, seen;
    public long time;

    ChatImage() {
    }

    ChatImage(String sender, String messageType, String description, String imageName, String imageUrl, String thumbImage, long time, String seen) {
        this.sender = sender;
        this.messageType = messageType;
        this.description = description;
        this.imageName = imageName;
        this.imageUrl = imageUrl;
        this.thumbImage = thumbImage;
        this.time = time;
        this.seen = seen;
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

    String getThumbImage() {
        return thumbImage;
    }

    void setThumbImage(String thumbImage) {
        this.thumbImage = thumbImage;
    }

    long getTime() {
        return time;
    }

    void setTime(long time) {
        this.time = time;
    }

    String getImageName() {
        return imageName;
    }

    void setImageName(String imageName) {
        this.imageName = imageName;
    }

    String getImageUrl() {
        return imageUrl;
    }

    void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    String getSeen() {
        return seen;
    }

    void setSeen(String seen) {
        this.seen = seen;
    }
}
