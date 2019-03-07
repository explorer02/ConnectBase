package com.example.connectbase;

class FriendsModelClass {

    private long since;
    private String chatId;

    public FriendsModelClass() {
    }

    FriendsModelClass(long time, String chatId) {
        this.since = time;
        this.chatId = chatId;
    }

    public long getSince() {
        return since;
    }

    public void setSince(long since) {
        this.since = since;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }
}
