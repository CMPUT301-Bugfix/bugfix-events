package com.example.eventlotterysystem;

/**
 * Model class representing a notification item in the system
 * Notifications can be related to winning a lottery ("WIN") or general event updates ("GENERAL")
 */
public class NotificationItem {
    private String id; // Firestore Doc ID
    private String eventId;
    private String title;
    private String message;
    private String type; // "WIN", "GENERAL"
    private String status; // "PENDING", "ACCEPTED", "REJECTED", "READ"
    private long timestamp;

    /**
     * Default constructor required for Firestore
     */
    public NotificationItem() {} // Required for Firestore

    /**
     * Constructs a new NotificationItem with the specified details
     * Initial status is set to "PENDING" and timestamp to current time
     *
     * @param eventId ID of the associated event
     * @param title   title of the notification
     * @param message message content
     * @param type    category of notification ("WIN" or "GENERAL")
     */
    public NotificationItem(String eventId, String title, String message, String type) {
        this.eventId = eventId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.status = "PENDING";
        this.timestamp = System.currentTimeMillis();
    }

    /** @return Firestore doc ID */
    public String getId() { return id; }

    /** @param id Firestore document ID to set */
    public void setId(String id) { this.id = id; }

    /** @return ID of the event this notification is about. */
    public String getEventId() { return eventId; }

    /** @param eventId event ID to set */
    public void setEventId(String eventId) { this.eventId = eventId; }

    /** @return headline or title of the notification. */
    public String getTitle() { return title; }

    /** @param title title to set */
    public void setTitle(String title) { this.title = title; }

    /** @return actual message */
    public String getMessage() { return message; }

    /** @param message message to set */
    public void setMessage(String message) { this.message = message; }

    /** @return type of notification ("GENERAL" or "WIN") */
    public String getType() { return type; }

    /** @param type type to set */
    public void setType(String type) { this.type = type; }

    /** @return user's response status (e.g., "PENDING") */
    public String getStatus() { return status; }

    /** @param status response status to set */
    public void setStatus(String status) { this.status = status; }

    /** @return creation time in milliseconds */
    public long getTimestamp() { return timestamp; }

    /** @param timestamp creation time to set*/
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
