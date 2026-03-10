package com.example.eventlotterysystem;

import com.google.firebase.Timestamp;

public class AdminEvent {
    private final String id;
    private final String title;

    private final String hostUid;
    private final Timestamp createdAt;

    public AdminEvent(String id, String title, String hostUid, Timestamp createdAt) {
        this.id = id;
        this.title = title;
        this.hostUid = hostUid;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getHostUid() { return hostUid; }
    public Timestamp getCreatedAt() { return createdAt; }
}
