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

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
}

