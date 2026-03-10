package com.example.eventlotterysystem;

import java.util.Date;

public class WaitlistEntryItem {
    private final String eventId;
    private final String title;
    private final Date eventDate;
    private final String status;

    public WaitlistEntryItem(String eventId, String title, Date eventDate, String status) {
        this.eventId = eventId;
        this.title = title;
        this.eventDate = eventDate;
        this.status = status;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public String getStatus() {
        return status;
    }
}
