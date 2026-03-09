package com.example.eventlotterysystem;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EventDetailsActivity extends AppCompatActivity {

    private TextView titleTextView;
    private TextView descriptionTextView;
    private EventRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        titleTextView = findViewById(R.id.eventDetailsTitle);
        descriptionTextView = findViewById(R.id.eventDetailsDescription);
        Button backButton = findViewById(R.id.eventDetailsBackButton);

        repository = new EventRepository();

        backButton.setOnClickListener(v -> finish());

        String eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Missing event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        repository.getEventById(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(EventItem event) {
                titleTextView.setText(event.getTitle());
                descriptionTextView.setText(event.getDescription());
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(EventDetailsActivity.this, "Failed to load event", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}