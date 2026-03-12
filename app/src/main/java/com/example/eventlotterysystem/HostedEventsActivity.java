package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a class that is the controller of the activity_hosted_events screen
 * this displays all the event the user has created
 * allows navigation to create a new event and to events the user has hosted
 */
public class HostedEventsActivity extends AppCompatActivity {

    private static final String TAG = "HostedEventsActivity";

    private FirebaseAuth auth;
    private EventRepository repository;

    private ListView hostedEventsListView;
    private TextView emptyState;
    private EventListAdapter adapter;
    private final List<EventItem> hostedEvents = new ArrayList<>();

    /**
     * This is the creation of the Activity
     * This connects to all the view on the screen and connects the clickable view to their controller
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hosted_events);

        auth = FirebaseAuth.getInstance();
        repository = new EventRepository();

        hostedEventsListView = findViewById(R.id.hostedEventsListView);
        emptyState = findViewById(R.id.hostedEventsEmptyState);
        Button createEventButton = findViewById(R.id.createEventButton);

        adapter = new EventListAdapter(this, hostedEvents);
        hostedEventsListView.setAdapter(adapter);

        findViewById(R.id.hostedEventsBackButton).setOnClickListener(v -> finish());
        createEventButton.setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class)));
        hostedEventsListView.setOnItemClickListener((parent, view, position, id) -> openHostedEvent(hostedEvents.get(position)));
    }

    /**
     * This is the startup of the Activity
     * get user from database and runs a load to get their created events
     */
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            navigateToAuthMenu();
            return;
        }
        loadHostedEvents(currentUser.getUid());
    }

    /**
     * gets all of Creator's Events from the database
     * on success updates display
     * on failure notifies user there was a load failure
     * @param uid
     * ID of Event Creator to get all Events of
     */
    private void loadHostedEvents(String uid) {
        repository.getHostedEvents(uid)
                .addOnSuccessListener(events -> {
                    hostedEvents.clear();
                    hostedEvents.addAll(events);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load hosted events", e);
                    Toast.makeText(
                            HostedEventsActivity.this,
                            buildLoadErrorMessage(e),
                            Toast.LENGTH_LONG
                    ).show();
                    hostedEvents.clear();
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    /**
     * switches screen display whether User has created events
     * if yes shows the events created
     * if no show text staing user has no events
     */
    private void updateEmptyState() {
        boolean hasEvents = !hostedEvents.isEmpty();
        hostedEventsListView.setVisibility(hasEvents ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(hasEvents ? View.GONE : View.VISIBLE);
    }

    /**
     * This is a controller for when an event in hostedEventsListView is pressed
     * navigates user to event they clicked
     * @param event
     * the Event that was clicked and to be opened in ViewEventActivity
     */
    private void openHostedEvent(EventItem event) {
        Intent intent = new Intent(this, ViewEventActivity.class);
        intent.putExtra("EVENT_ID", event.getId());
        intent.putExtra("CAN_EDIT_EVENT", true);
        startActivity(intent);
    }

    /**
     * this navigates user to AuthMenuActivity
     * should be used when a user could not be found
     */
    private void navigateToAuthMenu() {
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * method that coverts a raised exception during an event load into a error message to be displayed
     * @param exception
     * the exception that was created
     * @return
     * a String message describing what the error was
     */
    private String buildLoadErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return getString(R.string.failed_to_load_events) + ": " + exception.getMessage().trim();
        }
        return getString(R.string.failed_to_load_events);
    }
}
