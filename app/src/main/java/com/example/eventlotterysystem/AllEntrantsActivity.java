package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a class that is the controller of the activity_all_entrants screen
 * this is the activity that shows the Organizer all entrants of a given Status
 */
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

    /**
     * This is the creation of the Activity
     * This connects to layout for the screen and connects the clickable view to their controller
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
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

    /**
     * This is the startup of the Activity
     * runs loadEntrants
     */
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.event_manage_permission_denied, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        repository.getEventById(eventId)
                .addOnSuccessListener(event -> {
                    if (!EventRepository.canManageEvent(event, currentUser.getUid())) {
                        Toast.makeText(this, R.string.event_manage_permission_denied, Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    loadEntrants();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to verify entrant access", e);
                    Toast.makeText(
                            AllEntrantsActivity.this,
                            buildLoadErrorMessage(e),
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                });
    }

    /**
     * loads all Entrant information of Entrants of a given status(ie INWAITLIST,CONFIRMED) into the display views
     */
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

    /**
     * hides the listview of entrants and replaces it with the text stating there are no Entrants
     */
    private void updateEmptyState() {
        boolean hasEntrants = !entrants.isEmpty();
        entrantsListView.setVisibility(hasEntrants ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(hasEntrants ? View.GONE : View.VISIBLE);
    }

    /**
     * This is a controller for when an Entrant in entrantsListView is pressed
     * Opens the UserProfileDetailsActivity and supplies it with the Entrant Information
     * @param entrant
     * the User object that was clicked on
     */
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
        intent.putExtra(UserProfileDetailsActivity.EVENT_ID, eventId);
        startActivity(intent);
    }

    /**
     * method that coverts a raised exception during the loading of entrants into a error message to be displayed
     * @param exception
     * the exception that was created
     * @return
     * a String message describing what the error was
     */
    private String buildLoadErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return getString(R.string.failed_to_load_entrants) + ": " + exception.getMessage().trim();
        }
        return getString(R.string.failed_to_load_entrants);
    }

    /**
     * Checks the status text of a Entrant to make sure it is valid
     * @param value
     * the String of text of what status an Entrant is
     * @return
     * same status if the status matches a allowable one, non if there was no match
     */
    private String normalizeStatusFilter(String value) {
        if (EventRepository.WAITLIST_STATUS_CHOSEN.equals(value)
                || EventRepository.WAITLIST_STATUS_DECLINED.equals(value)) {
            return value;
        }
        return null;
    }

    /**
     * Sets the Title of the activity stating which kind of Entrants for the Event are listed
     */
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
