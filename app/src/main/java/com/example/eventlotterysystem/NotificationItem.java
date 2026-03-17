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

    /**
     * Returns the Firestore document identifier for this notification.
     *
     * @return the Firestore document id
     */
    public String getId() { return id; }

    /**
     * Sets the Firestore document identifier for this notification.
     *
     * @param id the Firestore document id to store
     */
    public void setId(String id) { this.id = id; }

    /**
     * Returns the event identifier associated with this notification.
     *
     * @return the related event id
     */
    public String getEventId() { return eventId; }

    /**
     * Sets the event identifier associated with this notification.
     *
     * @param eventId the related event id
     */
    public void setEventId(String eventId) { this.eventId = eventId; }

    /**
     * Returns the title shown for this notification.
     *
     * @return the notification title
     */
    public String getTitle() { return title; }

    /**
     * Sets the title shown for this notification.
     *
     * @param title the notification title
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Returns the main message body for this notification.
     *
     * @return the notification message
     */
    public String getMessage() { return message; }

    /**
     * Sets the main message body for this notification.
     *
     * @param message the notification message
     */
    public void setMessage(String message) { this.message = message; }

    /**
     * Returns the notification type.
     *
     * @return the notification type, such as {@code GENERAL} or {@code WIN}
     */
    public String getType() { return type; }

    /**
     * Sets the notification type.
     *
     * @param type the notification type to store
     */
    public void setType(String type) { this.type = type; }

    /**
     * Returns the current response status for this notification.
     *
     * @return the response status, such as {@code PENDING}
     */
    public String getStatus() { return status; }

    /**
     * Sets the current response status for this notification.
     *
     * @param status the response status to store
     */
    public void setStatus(String status) { this.status = status; }

    /**
     * Returns the creation timestamp for this notification.
     *
     * @return the creation time in milliseconds
     */
    public long getTimestamp() { return timestamp; }

    /**
     * Sets the creation timestamp for this notification.
     *
     * @param timestamp the creation time in milliseconds
     */
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
