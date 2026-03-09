package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;


public class HostedEventsActivty extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hosted_events);

        backButton = findViewById(R.id.buttonBack);
        createButton = findViewById(R.id.buttonCreateEvent);
        EventList =  findViewById(R.id.ListAuthorEvents);


        createButton.setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventsActivity.class)));

        EventList.setOnClickListener(v ->
                startActivity(new Intent(this, EditEventsActivity.class)));
    }
}
