package com.example.eventlotterysystem;

import java.util.Date;

/**
 * Event class that is a container for all information related to an event
 */
public class EventItem {
    private final String id;
    private final String title;
    private final String description;
    private final String location;
    private final String posterUrl;
    private final int maxEntrants;
    private final int maxParticipants;
    private final int totalEntrants;
    private final Date registrationDeadline;
    private final Date eventDate;
    private final boolean requiresGeolocation;
    private final String hostUid;
    private final String hostDisplayName;
    private final boolean waitlistOpen;
    private final String winningMessage;


    /**
     * creates the Event Object
     * @param id
     * String identification for the database
     * @param title
     * Name of the event
     * @param description
     * This describes what the Event is about
     */
    public EventItem(String id, String title, String description) {
        this(id, title, description, "", "", 0, 0, 0, null, null, false, "", "", true, "");
    }

    /**
     * This creates the Event Object
     * @param id
     * String identification for the database
     * @param title
     * Name of the event
     * @param description
     * This describes what the Event is about
     * @param location
     * the place that the event takes place
     * @param posterUrl
     * link to the event image
     * @param maxEntrants
     * limit to the number of signups
     * @param maxParticipants
     * limit to the number of people going to the event
     * @param totalEntrants
     * current number of Entrants signed up (should always be 0)
     * @param registrationDeadline
     * when entrants will no longer be able to sign up of an event
     * @param eventDate
     * when the event takes place
     * @param requiresGeolocation
     * boolean of if entrant need to be close to the event location to sign up
     * @param hostUid
     * String identification of Author for the database
     * @param hostDisplayName
     * String Name of Author for the database
     */
    public EventItem(
            String id,
            String title,
            String description,
            String location,
            String posterUrl,
            int maxEntrants,
            int maxParticipants,
            int totalEntrants,
            Date registrationDeadline,
            Date eventDate,
            boolean requiresGeolocation,
            String hostUid,
            String hostDisplayName
    ) {
        this(
                id,
                title,
                description,
                location,
                posterUrl,
                maxEntrants,
                maxParticipants,
                totalEntrants,
                registrationDeadline,
                eventDate,
                requiresGeolocation,
                hostUid,
                hostDisplayName,
                true,
                ""
        );
    }

    /**
     * This creates the Event Object
     * @param id
     * String identification for the database
     * @param title
     * Name of the event
     * @param description
     * This describes what the Event is about
     * @param location
     * the place that the event takes place
     * @param posterUrl
     * link to the event image
     * @param maxEntrants
     * limit to the number of signups
     * @param maxParticipants
     * limit to the number of people going to the event
     * @param totalEntrants
     * current number of Entrants signed up (should always be 0)
     * @param registrationDeadline
     * when entrants will no longer be able to sign up of an event
     * @param eventDate
     * when the event takes place
     * @param requiresGeolocation
     * boolean of if entrant need to be close to the event location to sign up
     * @param hostUid
     * String identification of Author for the database
     * @param hostDisplayName
     * String Name of Author for the database
     * @param waitlistOpen
     * whether the waitlist is accepting sign-ups
     * @param winningMessage
     * what to be displayed to a Entrant if they get selected
     */
    public EventItem(
            String id,
            String title,
            String description,
            String location,
            String posterUrl,
            int maxEntrants,
            int maxParticipants,
            int totalEntrants,
            Date registrationDeadline,
            Date eventDate,
            boolean requiresGeolocation,
            String hostUid,
            String hostDisplayName,
            boolean waitlistOpen,
            String winningMessage
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.posterUrl = posterUrl;
        this.maxEntrants = maxEntrants;
        this.maxParticipants = maxParticipants;
        this.totalEntrants = totalEntrants;
        this.registrationDeadline = registrationDeadline;
        this.eventDate = eventDate;
        this.requiresGeolocation = requiresGeolocation;
        this.hostUid = hostUid;
        this.hostDisplayName = hostDisplayName;
        this.waitlistOpen = waitlistOpen;
        this.winningMessage = winningMessage;
    }

    /**
     * getter for ID
     * @return
     * string ID of Event
     */
    public String getId() {
        return id;
    }

    /**
     * getter for Event Title
     * @return
     * string Title of Event
     */
    public String getTitle() {
        return title;
    }

    /**
     * getter for Event Description
     * @return
     * string Description of Event
     */
    public String getDescription() {
        return description;
    }

    /**
     * getter for Event Location
     * @return
     * string representation of event location
     */
    public String getLocation() {
        return location;
    }

    /**
     * getter for link of Event Image
     * @return
     * link to image from database
     */
    public String getPosterUrl() {
        return posterUrl;
    }

    /**
     * getter for Max Entrants
     * @return
     * int limit of how many sign-ups are allowed
     */
    public int getMaxEntrants() {
        return maxEntrants;
    }

    /**
     * getter for Max Participants
     * @return
     * int limits of how many Entrants can be chosen/confirmed
     */
    public int getMaxParticipants() {
        return maxParticipants;
    }

    /**
     * getter for Total Entrants
     * @return
     * int current number of Entrants who have signed up
     */
    public int getTotalEntrants() {
        return totalEntrants;
    }

    /**
     * getter for Registration Deadline
     * @return
     * standard date format of the deadline to sign-up
     */
    public Date getRegistrationDeadline() {
        return registrationDeadline;
    }

    /**
     * getter for the Event Date
     * @return
     * standard date format of the date the event occurs
     */
    public Date getEventDate() {
        return eventDate;
    }

    /***
     * getter for requiresGeolocation boolean
     * @return
     * whether the entrant must be in the same region as the event to sign-up
     */
    public boolean isRequiresGeolocation() {
        return requiresGeolocation;
    }

    /**
     * getter for Author ID
     * @return
     * string ID of Event Creator User
     */
    public String getHostUid() {
        return hostUid;
    }

    /**
     * getter for Author Name
     * @return
     * Name of Event Creator
     */
    public String getHostDisplayName() {
        return hostDisplayName;
    }

    /**
     * getter for waitlistOpen boolean
     * @return
     * whether the any entrant is allowed to sign-up
     */
    public boolean isWaitlistOpen() {
        return waitlistOpen;
    }

    /**
     * getter for winningMessage text
     * @return
     * String to be displayed to a User if they get selected
     */
    public String getWinningMessage() {
        return winningMessage;
    }
}
