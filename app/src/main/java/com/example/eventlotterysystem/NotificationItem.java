package com.example.eventlotterysystem;

/**
 * Model class representing a notification item in the system.
 * Notifications can be related to winning a lottery or general event updates.
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
     * Default constructor required for Firestore data mapping.
     */
    public NotificationItem() {}

    /**
     * Constructs a new NotificationItem with the specified details.
     *
     * @param eventId The ID of the associated event.
     * @param title   The title of the notification.
     * @param message The message body.
     * @param type    The type of notification ("WIN" or "GENERAL").
     */
    public NotificationItem(String eventId, String title, String message, String type) {
        this.eventId = eventId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.status = "PENDING";
        this.timestamp = System.currentTimeMillis();
    }

    /** @return The Firestore document ID. */
    public String getId() { return id; }
    /** @param id The Firestore document ID to set. */
    public void setId(String id) { this.id = id; }

    /** @return The ID of the associated event. */
    public String getEventId() { return eventId; }
    /** @param eventId The event ID to set. */
    public void setEventId(String eventId) { this.eventId = eventId; }

    /** @return The title of the notification. */
    public String getTitle() { return title; }
    /** @param title The title to set. */
    public void setTitle(String title) { this.title = title; }

    /** @return The message body of the notification. */
    public String getMessage() { return message; }
    /** @param message The message body to set. */
    public void setMessage(String message) { this.message = message; }

    /** @return The type of notification ("WIN" or "GENERAL"). */
    public String getType() { return type; }
    /** @param type The type to set. */
    public void setType(String type) { this.type = type; }

    /** @return The status of the notification ("PENDING", "ACCEPTED", etc.). */
    public String getStatus() { return status; }
    /** @param status The status to set. */
    public void setStatus(String status) { this.status = status; }

    /** @return The timestamp when the notification was created. */
    public long getTimestamp() { return timestamp; }
    /** @param timestamp The creation timestamp to set. */
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
