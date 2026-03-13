package com.example.eventlotterysystem;

import java.util.Date;

/**
 * This is a class that represents and event that a User is on a Waitlist for
 */
public class WaitlistEntryItem {
    private final String eventId;
    private final String title;
    private final Date eventDate;
    private final String status;

    /**
     * This creates the waitlist entry Object
     * @param eventId
     * This is Id of the Event
     * @param title
     * This is string name the Event
     * @param eventDate
     * This is the time the Event takes place
     * @param status
     * This is the stage that the User is on (waitlist, chosen, confirmed)
     */
    public WaitlistEntryItem(String eventId, String title, Date eventDate, String status) {
        this.eventId = eventId;
        this.title = title;
        this.eventDate = eventDate;
        this.status = status;
    }

    /**
     * returns the Id of the corresponding Event that the waitlist is for
     * @return
     * String event's ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * returns the Name of the corresponding Event that the waitlist is for
     * @return
     * String Event Name
     */
    public String getTitle() {
        return title;
    }

    /**
     * returns the date of the corresponding Event's Occurrence
     * @return
     * Date time the Event takes place
     */
    public Date getEventDate() {
        return eventDate;
    }

    /**
     * returns the state the user is in for the corresponding Event
     * @return
     * String status condition (waitlist, chosen, confirmed)
     */
    public String getStatus() {
        return status;
    }
}
