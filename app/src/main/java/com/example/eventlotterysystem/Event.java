package com.example.eventlotterysystem;

import android.media.Image;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This is a class for an Event
 */
public class Event {
    private final UUID eventId;
    private String title;
    private String description;
    //private List<EventCategory> category = new ArrayList<>(); //not implemented yet
    private String location; //maybe have location as a class
    //private RegistrationPeriod registrationPeriod; //not implemented yet
    private LocalDateTime eventDate;
    private EventStatus status;
    private final EntrantList entrantList;
    private Image eventPicture;
    private Boolean RestrictByGeolocation;

    //private QRCode Code; //not implemented yet

    /**
     * This creates the Event
     * @param signLimit
     * This is number of Entrants able to sign up for the Event
     * @param confirmedLimit
     * This is number of slots available to Entrants
     * @param title
     * This is name of the event
     * @param description
     * This describes what the Event is about
     * @param EventDate
     * This is when the Event takes place
     * @param lotteryDeadline
     * This is when lottery occurs
     * @param needGeolocation
     * This will restrict signups if Entrants are not Nearby
     //* @param photo
     //* This is a photo that represents the Event
     */
    public Event(
            int signLimit,
            int confirmedLimit,
            String title,
            String description,
            LocalDateTime EventDate,
            LocalDateTime lotteryDeadline,
            Boolean needGeolocation
            //Image photo) {
            ) {
        this.eventId = UUID.randomUUID();
        this.title = title;
        this.description = description;
        this.eventDate = EventDate;
        this.status = EventStatus.PUBLISHED;
        this.entrantList = new EntrantList(signLimit, confirmedLimit);
        this.RestrictByGeolocation = needGeolocation;
        //this.eventPicture = photo;
    }

    //TODO
    //public QRCode generateQRCode() {}
    //public void setRegistrationPeriod(DateTime start,DateTime end) {}
    /**
     * This set the eventPicture for the event
     * @param image
     * This is the new image to be displayed for the event
     */
    public void setPoster(Image image) {
        eventPicture = image;
    }
    //public Event getDetails() {} //is this just meant to return itself
    /**
     * This signs up a Entrant into the waitingList
     * @param Entrant
     * This is the Entrant to be signed up
     */
    public void signUp(UserProfile Entrant) {
       entrantList.addEntrantToWaitingList(Entrant);
    }
    public EntrantList getEntrantList() {
        return entrantList;
    }
}
