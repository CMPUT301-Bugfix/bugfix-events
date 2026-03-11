package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EntrantsActivity extends AppCompatActivity {

    public static final String EVENT_ID = "EVENT_ID";
    public static final String TOTAL_ENTRANTS = "TOTAL_ENTRANTS";
    public static final String MAX_ENTRANTS = "MAX_ENTRANTS";
    public static final String EVENT_TITLE = "EVENT_TITLE";

    private EventRepository repository;
    private String eventId;
    private int totalEntrants;
    private int maxEntrants;
    private String eventTitle;
    private Button allEntrantsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrants);

        repository = new EventRepository();
        eventId = getIntent().getStringExtra(EVENT_ID);
        totalEntrants = getIntent().getIntExtra(TOTAL_ENTRANTS, 0);
        maxEntrants = getIntent().getIntExtra(MAX_ENTRANTS, 0);
        eventTitle = getIntent().getStringExtra(EVENT_TITLE);

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, R.string.missing_event_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        allEntrantsButton = findViewById(R.id.entrantsAllEntrantsButton);
        findViewById(R.id.entrantsBackButton).setOnClickListener(v -> finish());
        allEntrantsButton.setOnClickListener(v -> openAllEntrants());

        updateAllEntrantsButton();
    }

    @Override
    protected void onStart() {
        super.onStart();
        repository.getEventById(eventId)
                .addOnSuccessListener(event -> {
                    totalEntrants = event.getTotalEntrants();
                    maxEntrants = event.getMaxEntrants();
                    eventTitle = event.getTitle();
                    updateAllEntrantsButton();
                })
                .addOnFailureListener(e -> {
                });
    }

    private void updateAllEntrantsButton() {
        allEntrantsButton.setText(getString(
                R.string.all_entrants_button_label,
                buildEntrantCountText(totalEntrants, maxEntrants)
        ));
    }

    private void openAllEntrants() {
        Intent intent = new Intent(this, AllEntrantsActivity.class);
        intent.putExtra(EVENT_ID, eventId);
        intent.putExtra(EVENT_TITLE, eventTitle == null ? "" : eventTitle);
        startActivity(intent);
    }

    private String buildEntrantCountText(int totalEntrants, int maxEntrants) {
        return totalEntrants + " / "
                + (maxEntrants > 0 ? String.valueOf(maxEntrants) : getString(R.string.unlimited));
    }
}
