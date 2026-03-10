package com.example.eventlotterysystem;

import java.util.Date;

public class EventItem {
    private final String id;
    private final String title;
    private final String description;
    private final String location;
    private final String posterUrl;
    private final int maxEntrants;
    private final int maxParticipants; //added this
    private final int totalEntrants;
    private final Date registrationDeadline;
    private final Date eventDate;
    private final boolean requiresGeolocation;
    private final String hostUid;
    private final String hostDisplayName;

    public EventItem(String id, String title, String description) {
        this(id, title, description, "", "", 0, 0, 0, null, null, false, "", "");
    }

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
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public int getMaxEntrants() {
        return maxEntrants;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public int getTotalEntrants() {
        return totalEntrants;
    }

    public Date getRegistrationDeadline() {
        return registrationDeadline;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public boolean isRequiresGeolocation() {
        return requiresGeolocation;
    }

    public String getHostUid() {
        return hostUid;
    }

    public String getHostDisplayName() {
        return hostDisplayName;
    }
}
