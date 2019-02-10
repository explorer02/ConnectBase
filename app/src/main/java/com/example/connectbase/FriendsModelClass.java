package com.example.connectbase;

class FriendsModelClass {

    private long since;

    public FriendsModelClass() {
    }

    FriendsModelClass(long time) {
        this.since = time;
    }

    public long getSince() {
        return since;
    }

    public void setSince(long since) {
        this.since = since;
    }
}
