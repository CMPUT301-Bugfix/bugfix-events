package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class AllEntrantsActivity extends AppCompatActivity {

    private static final String TAG = "AllEntrantsActivity";
    public static final String STATUS_FILTER = "STATUS_FILTER";

    private EventRepository repository;
    private String eventId;
    private String statusFilter;
    private ListView entrantsListView;
    private TextView titleView;
    private TextView emptyState;
    private EntrantAdapter adapter;
    private final List<UserProfile> entrants = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_entrants);

        repository = new EventRepository();
        eventId = getIntent().getStringExtra(EntrantsActivity.EVENT_ID);
        statusFilter = normalizeStatusFilter(getIntent().getStringExtra(STATUS_FILTER));
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, R.string.missing_event_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        titleView = findViewById(R.id.allEntrantsTitle);
        entrantsListView = findViewById(R.id.allEntrantsListView);
        emptyState = findViewById(R.id.allEntrantsEmptyState);
        adapter = new EntrantAdapter(this, entrants);
        entrantsListView.setAdapter(adapter);

        applyFilterUi();
        findViewById(R.id.allEntrantsBackButton).setOnClickListener(v -> finish());
        entrantsListView.setOnItemClickListener((parent, view, position, id) -> openEntrantDetails(entrants.get(position)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadEntrants();
    }

    private void loadEntrants() {
        repository.getEntrantsForEvent(eventId, statusFilter)
                .addOnSuccessListener(items -> {
                    entrants.clear();
                    entrants.addAll(items);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load entrants", e);
                    entrants.clear();
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    Toast.makeText(
                            AllEntrantsActivity.this,
                            buildLoadErrorMessage(e),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void updateEmptyState() {
        boolean hasEntrants = !entrants.isEmpty();
        entrantsListView.setVisibility(hasEntrants ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(hasEntrants ? View.GONE : View.VISIBLE);
    }

    private void openEntrantDetails(UserProfile entrant) {
        Intent intent = new Intent(this, UserProfileDetailsActivity.class);
        intent.putExtra(UserProfileDetailsActivity.NAME, entrant.getName());
        intent.putExtra(UserProfileDetailsActivity.ACCOUNT_TYPE, entrant.getAccountType());
        intent.putExtra(UserProfileDetailsActivity.USERNAME, entrant.getUsername());
        intent.putExtra(UserProfileDetailsActivity.EMAIL, entrant.getEmail());
        intent.putExtra(UserProfileDetailsActivity.PHONE, entrant.getPhoneNumber());
        intent.putExtra(UserProfileDetailsActivity.UID, entrant.getUid());
        long createdAtMillis = entrant.getCreatedAt() == null ? -1L : entrant.getCreatedAt().toDate().getTime();
        intent.putExtra(UserProfileDetailsActivity.TIME_MILLIS, createdAtMillis);
        intent.putExtra(UserProfileDetailsActivity.ALLOW_DELETE, false);
        startActivity(intent);
    }

    private String buildLoadErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return getString(R.string.failed_to_load_entrants) + ": " + exception.getMessage().trim();
        }
        return getString(R.string.failed_to_load_entrants);
    }

    private String normalizeStatusFilter(String value) {
        if (EventRepository.WAITLIST_STATUS_CHOSEN.equals(value)
                || EventRepository.WAITLIST_STATUS_DECLINED.equals(value)) {
            return value;
        }
        return null;
    }

    private void applyFilterUi() {
        if (EventRepository.WAITLIST_STATUS_CHOSEN.equals(statusFilter)) {
            titleView.setText(R.string.chosen_entrants);
            emptyState.setText(R.string.no_chosen_entrants);
            return;
        }
        if (EventRepository.WAITLIST_STATUS_DECLINED.equals(statusFilter)) {
            titleView.setText(R.string.cancelled_entrants);
            emptyState.setText(R.string.no_cancelled_entrants);
            return;
        }
        titleView.setText(R.string.all_entrants);
        emptyState.setText(R.string.no_entrants);
    }
}
