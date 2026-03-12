package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EntrantsActivity extends AppCompatActivity {
    private static final String TAG = "EntrantsActivity";

    public static final String EVENT_ID = "EVENT_ID";
    public static final String TOTAL_ENTRANTS = "TOTAL_ENTRANTS";
    public static final String MAX_ENTRANTS = "MAX_ENTRANTS";

    private EventRepository repository;
    private String eventId;
    private int totalEntrants;
    private int maxEntrants;
    private int chosenEntrants;
    private int cancelledEntrants;
    private Button allEntrantsButton;
    private Button chosenEntrantsButton;
    private Button cancelledEntrantsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrants);

        repository = new EventRepository();
        eventId = getIntent().getStringExtra(EVENT_ID);
        totalEntrants = getIntent().getIntExtra(TOTAL_ENTRANTS, 0);
        maxEntrants = getIntent().getIntExtra(MAX_ENTRANTS, 0);

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, R.string.missing_event_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        allEntrantsButton = findViewById(R.id.entrantsAllEntrantsButton);
        chosenEntrantsButton = findViewById(R.id.entrantsChosenButton);
        cancelledEntrantsButton = findViewById(R.id.entrantsCancelledButton);
        findViewById(R.id.entrantsBackButton).setOnClickListener(v -> finish());
        allEntrantsButton.setOnClickListener(v -> openEntrantsList(null));
        chosenEntrantsButton.setOnClickListener(v -> openEntrantsList(EventRepository.WAITLIST_STATUS_CHOSEN));
        cancelledEntrantsButton.setOnClickListener(v -> openEntrantsList(EventRepository.WAITLIST_STATUS_DECLINED));

        updateButtons();
    }

    @Override
    protected void onStart() {
        super.onStart();
        repository.getEventById(eventId)
                .addOnSuccessListener(event -> {
                    totalEntrants = event.getTotalEntrants();
                    maxEntrants = event.getMaxEntrants();
                    updateButtons();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to refresh entrant totals", e);
                });

        repository.getEntrantCount(eventId, EventRepository.WAITLIST_STATUS_CHOSEN)
                .addOnSuccessListener(count -> {
                    chosenEntrants = count;
                    updateButtons();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load chosen entrant count", e);
                    Toast.makeText(
                            EntrantsActivity.this,
                            getString(R.string.failed_to_load_entrants),
                            Toast.LENGTH_LONG
                    ).show();
                });

        repository.getEntrantCount(eventId, EventRepository.WAITLIST_STATUS_DECLINED)
                .addOnSuccessListener(count -> {
                    cancelledEntrants = count;
                    updateButtons();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load cancelled entrant count", e);
                    Toast.makeText(
                            EntrantsActivity.this,
                            getString(R.string.failed_to_load_entrants),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void updateButtons() {
        allEntrantsButton.setText(getString(
                R.string.all_entrants_button_label,
                buildEntrantCountText(totalEntrants, maxEntrants)
        ));
        chosenEntrantsButton.setText(getString(
                R.string.chosen_entrants_button_label,
                chosenEntrants
        ));
        cancelledEntrantsButton.setText(getString(
                R.string.cancelled_entrants_button_label,
                cancelledEntrants
        ));
    }

    private void openEntrantsList(String statusFilter) {
        Intent intent = new Intent(this, AllEntrantsActivity.class);
        intent.putExtra(EVENT_ID, eventId);
        if (statusFilter != null && !statusFilter.isEmpty()) {
            intent.putExtra(AllEntrantsActivity.STATUS_FILTER, statusFilter);
        }
        startActivity(intent);
    }

    private String buildEntrantCountText(int totalEntrants, int maxEntrants) {
        return totalEntrants + " / "
                + (maxEntrants > 0 ? String.valueOf(maxEntrants) : getString(R.string.unlimited));
    }
}
