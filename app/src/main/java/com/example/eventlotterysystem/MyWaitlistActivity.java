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
 * screen where the entrant can view waitlist and invitation
 *  * statuses
 * This is a class that is the controller of the activity_my_waitlist screen
 */
public class MyWaitlistActivity extends AppCompatActivity {

    private static final String TAG = "MyWaitlistActivity";

    private FirebaseAuth auth;
    private EventRepository repository;
    private ListView myWaitlistListView;
    private TextView emptyState;
    private WaitlistEntryAdapter adapter;
    private final List<WaitlistEntryItem> waitlistEntries = new ArrayList<>();

    /**
     * This connects to all the view on the screen and connects the clickable view to the controller
     * also creates a connection to the database
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_waitlist);

        auth = FirebaseAuth.getInstance();
        repository = new EventRepository();
        myWaitlistListView = findViewById(R.id.myWaitlistListView);
        emptyState = findViewById(R.id.myWaitlistEmptyState);

        adapter = new WaitlistEntryAdapter(this, waitlistEntries);
        myWaitlistListView.setAdapter(adapter);

        findViewById(R.id.myWaitlistBackButton).setOnClickListener(v -> finish());
        myWaitlistListView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, ViewEventActivity.class);
            intent.putExtra("EVENT_ID", waitlistEntries.get(position).getEventId());
            startActivity(intent);
        });
    }

    /**
     * startup of the Activity
     * get user from database and runs a load to get their waitlist
     */
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            navigateToAuthMenu();
            return;
        }
        loadMyWaitlists(currentUser.getUid());
    }

    /**
     * gets all of Users waitlist from the database
     * on success updates display
     * on failure notifies user there was a load failure
     * ID of User to get all Event sign-ups
     * @param uid
     */
    private void loadMyWaitlists(String uid) {
        repository.getMyWaitlists(uid)
                .addOnSuccessListener(entries -> {
                    waitlistEntries.clear();
                    waitlistEntries.addAll(entries);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load my waitlists", e);
                    waitlistEntries.clear();
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    Toast.makeText(
                            MyWaitlistActivity.this,
                            buildLoadErrorMessage(e),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    /**
     * switches screen display whether User has any sign-ups
     * if yes shows the all waitlists
     * if no show text stating user is not signed up to anything
     */
    private void updateEmptyState() {
        boolean hasEntries = !waitlistEntries.isEmpty();
        myWaitlistListView.setVisibility(hasEntries ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(hasEntries ? View.GONE : View.VISIBLE);
    }

    /**
     * this navigates user to AuthMenuActivity
     * should be used when a user could not be found
     * {@link AuthMenuActivity} when no signed-in user is found.
     */
    private void navigateToAuthMenu() {
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * method that coverts a raised exception during an event load into a error message to be displayed
     *  @param exception the exception raised while loading waitlist entries
     *  @return a message describing the error, or a default load-failure message
     */
    private String buildLoadErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return getString(R.string.waitlist_load_failed) + ": " + exception.getMessage().trim();
        }
        return getString(R.string.waitlist_load_failed);
    }
}
