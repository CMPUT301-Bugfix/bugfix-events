package com.example.eventlotterysystem;

import android.media.Image;

import java.util.ArrayList;
import java.util.List;

public class Event {
    private EntrantList entrantList;
    private List<String> category = new ArrayList<>();
    private Image eventPicture;

    public Event(int signLimit, int confirmedLimit) {
        this.entrantList = new EntrantList(signLimit, confirmedLimit);
        this.category.add("None");
    }


}
