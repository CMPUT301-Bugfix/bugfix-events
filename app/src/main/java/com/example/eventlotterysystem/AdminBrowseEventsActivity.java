package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * This class deals with the browsing of all events for admins
 */

public class AdminBrowseEventsActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private TextView backButton;
    private ProgressBar eventsLoading;
    private TextView eventsEmptyState;
    private ScrollView eventsScrollView;
    private LinearLayout eventsContainer;
    private EventRepository eventRepository;

    private boolean isAdminConfirmed;
    private ListenerRegistration eventsListener;

    /**
     * This method loads the UI, initializes the firebase and event repository instances,
     * and connects the java variables to the views in the XML
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_events);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        eventRepository = new EventRepository();

        backButton = findViewById(R.id.adminEventsBackButton);
        eventsLoading = findViewById(R.id.adminEventsLoading);
        eventsEmptyState = findViewById(R.id.adminEventsEmptyState);
        eventsScrollView = findViewById(R.id.adminEventsScrollView);
        eventsContainer = findViewById(R.id.adminEventsContainer);

        backButton.setOnClickListener(v -> finish());
    }

    /**
     * Essentially checks whether there is a signed in user, and to navigate
     * to the AuthMenu if not, and check if the current user is an admin.
     */

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            navigateToAuthMenu();
            return;
        }

        verifyAdminAndLoad(currentUser.getUid());
    }

    /**
     * Removes event listeners in the event this function is called.
     */

    @Override
    protected void onStop() {
        super.onStop();
        if (eventsListener != null) {
            eventsListener.remove();
            eventsListener = null;
        }
    }

    /**
     * This method checks whether or not the user is an admin, and if so
     * then run the loadEvents method, but if not, close the screen.
     * @param uid the id of a user to be verified
     */

    private void verifyAdminAndLoad(@NonNull String uid) {
        setLoading(true);
        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String accountType = snapshot.getString("accountType");
                    if (!"admin".equals(accountType)) {
                        finish();
                        return;
                    }
                    isAdminConfirmed = true;
                    loadEvents();
                })
                .addOnFailureListener(exception -> finish());
    }


    /**
     * This method is the heart of this class, it collects the events from the
     * Firestore events collection by using a snapshot listener for live updates,
     * and creates a list by looping through the events collection and calling the
     * renderEvents method to render the events into rows in the UI. Also deals with
     * error handling and eventListener cleanup.
     */
    private void loadEvents() {
        setLoading(true);

        if (eventsListener != null) {
            eventsListener.remove();
            eventsListener = null;
        }


        Query query = firestore.collection("events");

        eventsListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                setLoading(false);
                renderEvents(new ArrayList<>());
                Log.e("Firestore", error.toString());
                return;
            }

            List<EventItem> events = new ArrayList<>();

            if (value != null) {
                for (DocumentSnapshot snapshot : value.getDocuments()) {
                    if (Boolean.TRUE.equals(snapshot.getBoolean("deleted"))) {
                        continue;
                    }

                    events.add(eventRepository.readEventItem(snapshot));
                }
            }


            events.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));

            renderEvents(events);
            setLoading(false);
        });
    }


    /**
     * This method turns each event into a row in the UI, starting by removing any
     * previous views, and notifying the user if there are no events. If there are
     * events, then it creates a new row for each event with the necessary fields
     * that we wanted to display.
     * @param events this is the list of events from the database
     */

    private void renderEvents(@NonNull List<EventItem> events) {
        eventsContainer.removeAllViews();

        if (events.isEmpty()) {
            eventsEmptyState.setText(getString(R.string.no_events));
            eventsEmptyState.setVisibility(View.VISIBLE);
            eventsScrollView.setVisibility(View.GONE);
            return;
        }

        eventsEmptyState.setVisibility(View.GONE);
        eventsScrollView.setVisibility(View.VISIBLE);

        for (EventItem event : events) {
            View row = getLayoutInflater().inflate(R.layout.item_admin_event, eventsContainer, false);

            TextView eventTitleValue = row.findViewById(R.id.eventTitleValue);
            TextView eventDateValue = row.findViewById(R.id.eventDateValue);
            TextView eventOrganizerValue = row.findViewById(R.id.eventOrganizerValue);
            TextView eventEntrantsValue = row.findViewById(R.id.eventEntrantsValue);


            String title = event.getTitle();
            eventTitleValue.setText(TextUtils.isEmpty(title) ? getString(R.string.unknown_event_title) : title);


            String dateText = getString(R.string.unknown_event_date);
            if (event.getEventDate() != null) {
                java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault());
                dateText = fmt.format(event.getEventDate());
            }
            eventDateValue.setText(dateText);

            String organizer = event.getHostDisplayName();
            eventOrganizerValue.setText(TextUtils.isEmpty(organizer) ? getString(R.string.unknown_organizer) : organizer);

            eventEntrantsValue.setText(getString(
                    R.string.admin_event_entrants_format,
                    event.getTotalEntrants(),
                    event.getMaxEntrants()
            ));

            row.setOnClickListener(v -> openEventDetails(event));
            eventsContainer.addView(row);
        }
    }

    /**
     * This method sends the event id and event title to the event details page
     * @param event this is an instance of a single event
     */

    private void openEventDetails(@NonNull EventItem event) {
        Intent intent = new Intent(this, AdminEventDetailsActivity.class);
        intent.putExtra(AdminEventDetailsActivity.EVENT_ID, event.getId());
        intent.putExtra(AdminEventDetailsActivity.EVENT_TITLE, normalize(event.getTitle()));
        startActivity(intent);
    }


    /**
     * Deals with the loading screen, this method hides data/views
     * while we fetch current data
     * @param loading boolean value
     */

    private void setLoading(boolean loading) {
        eventsLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        backButton.setEnabled(!loading);
        if (loading) {
            eventsEmptyState.setVisibility(View.GONE);
            eventsScrollView.setVisibility(View.GONE);
        }
    }

    /**
     * Navigates to the AuthMenu when called
     */

    private void navigateToAuthMenu() {
        auth.signOut();
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    /**
     * This method just cleans up a string for us
     * @param value A string that we want to clean
     * @return either an empty string if the value is null or the trimmed string
     */

    @NonNull
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
