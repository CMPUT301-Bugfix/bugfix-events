package com.example.eventlotterysystem;


/**
 * This is a class that Contains Entrant Name and ID stored in an event
 */
public class EntrantInfo {
    private final String name;
    private final String id;

    EntrantInfo(String name, String id) {
        this.name = name;
        this.id = id;
    }

    /**
     * Returns the entrant display name.
     *
     * @return the entrant name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the entrant identifier.
     *
     * @return the entrant id
     */
    public String getId() {
        return id;
    }
}
