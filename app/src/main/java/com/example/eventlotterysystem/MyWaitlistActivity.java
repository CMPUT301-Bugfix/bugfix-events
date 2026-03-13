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
 * This is a class that is the controller of the activity_my_waitlist screen
 * It is the activity that allows users to see the list of events they have signed up for
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
     * This is the creation of the Activity
     * This connects to all the view on the screen and connects the clickable view to the controller
     * also creates a connection to the database
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
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
     * This is the startup of the Activity
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
     * @param uid
     * ID of User to get all Event sign-ups of
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
            return getString(R.string.waitlist_load_failed) + ": " + exception.getMessage().trim();
        }
        return getString(R.string.waitlist_load_failed);
    }
}
