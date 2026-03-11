package com.example.eventlotterysystem;

import java.util.Date;

public class NotificationItem {
    private String id;
    private String eventId;
    private String eventTitle;
    private String message;
    private String senderUid;
    private Date timestamp;
    private boolean isRead;

    // Required for Firestore
    public NotificationItem() {}

    public NotificationItem(String id, String eventId, String eventTitle, String message, String senderUid, Date timestamp) {
        this.id = id;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.message = message;
        this.senderUid = senderUid;
        this.timestamp = timestamp;
        this.isRead = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSenderUid() { return senderUid; }
    public void setSenderUid(String senderUid) { this.senderUid = senderUid; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
