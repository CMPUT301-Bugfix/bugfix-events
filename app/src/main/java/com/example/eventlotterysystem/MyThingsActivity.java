package com.example.eventlotterysystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a class that is the controller of the activity_my_things screen
 * this is the activity that allows navigation to modify profile, events, waitlist
 * also shows notifications that have been received
 */
public class MyThingsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {

    private static final String TAG = "MyThingsActivity";
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private NotificationRepository notificationRepository;
    private EventRepository eventRepository;

    private TextView myThingsSubtitle;
    private Button adminZoneButton;
    private Button myWaitlistButton;
    private Button messagesButton;
    private RecyclerView notificationsRecyclerView;
    private Button hostEventButton;
    private NotificationAdapter notificationAdapter;
    private List<NotificationItem> notificationList = new ArrayList<>();

    /**
     * This is the creation of the Activity
     * This connects to layout for the screen and connects the clickable view to their controller
     * It also sets up notifications to be received
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_things);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();
        eventRepository = new EventRepository();

        myThingsSubtitle = findViewById(R.id.myThingsSubtitle);
        adminZoneButton = findViewById(R.id.adminZoneButton);
        myWaitlistButton = findViewById(R.id.myWaitlistButton);
        messagesButton = findViewById(R.id.messagesButton);
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        hostEventButton = findViewById(R.id.hostEventButton);

        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter(notificationList, this);
        notificationsRecyclerView.setAdapter(notificationAdapter);

        findViewById(R.id.myThingsBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.settingsButton).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.hostEventButton).setOnClickListener(v ->
                startActivity(new Intent(this, HostedEventsActivity.class)));
        myWaitlistButton.setOnClickListener(v ->
                startActivity(new Intent(this, MyWaitlistActivity.class)));
        messagesButton.setOnClickListener(v ->
                startActivity(new Intent(this, InboxActivity.class)));
        findViewById(R.id.myThingsLogoutButton).setOnClickListener(v -> onLogOutClicked());
        adminZoneButton.setOnClickListener(v ->
                startActivity(new Intent(this, AdminZoneActivity.class)));
    }

    /**
     * This is the startup of the Activity
     * it runs loads for account type and notifications for the user
     */
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            navigateToAuthMenu();
            return;
        }

        String identity = currentUser.getEmail();
        if (identity == null || identity.isEmpty()) {
            identity = currentUser.getUid();
        }
        myThingsSubtitle.setText(getString(R.string.signed_in_as, identity));
        loadAccountType(currentUser.getUid());
        loadNotifications(currentUser.getUid());
    }

    /**
     * allows access to the adminZoneButton if the user is an admin and
     * hides the host an event button if the user has organizer privileges
     * suspended
     * @param uid
     * Id of the current user
     */
    private void loadAccountType(String uid) {
        adminZoneButton.setVisibility(View.GONE);
        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String accountType = snapshot.getString("accountType");
                    if ("admin".equals(accountType)) {
                        adminZoneButton.setVisibility(View.VISIBLE);
                    }

                    Boolean suspended = snapshot.getBoolean("suspended");
                    if (Boolean.TRUE.equals(suspended)) {
                        hostEventButton.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(exception -> adminZoneButton.setVisibility(View.GONE));
    }

    /**
     * loads all the notifications that the user should receive and add it to notificationList to be displayed
     * on failure updates the display to notify user of the error
     * @param uid
     * Id of the current user
     */
    private void loadNotifications(String uid) {
        notificationRepository.getNotificationsForUser(uid)
                .addOnSuccessListener(notifications -> {
                    notificationList.clear();
                    notificationList.addAll(notifications);
                    notificationAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MyThingsActivity.this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * This is the controller that is run when a notification is clicked by the User
     * it runs a method that gives the user a popup of notification content depending on type of notification
     * @param notification NotificationItem that was clicked.
     */
    @Override
    public void onNotificationClick(NotificationItem notification) {
        if ("WIN".equals(notification.getType())) {
            showWinningDialog(notification);
        } else {
            showGeneralDialog(notification);
        }
    }

    /**
     * This is the controller that is run when a notification click-held by the User
     * creates a popup that allows the user to delete the notification
     * @param notification NotificationItem that was long-clicked.
     */
    @Override
    public void onNotificationLongClick(NotificationItem notification) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Notification")
                .setMessage("Are you sure you want to delete this notification?")
                .setPositiveButton("Delete", (dialog, which) -> deleteNotification(notification))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * creates a popup that show the notification message and allows the user to mange being chosen for the event the notification is for
     * should be run on a selection win notification
     * @param notification
     * NotificationItem that was clicked
     */
    private void showWinningDialog(NotificationItem notification) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(notification.getTitle())
                .setMessage(notification.getMessage());

        if ("PENDING".equals(notification.getStatus())) {
            builder.setPositiveButton("Accept", (dialog, which) -> handleInvite(notification, EventRepository.WAITLIST_STATUS_CONFIRMED))
                   .setNeutralButton("Snooze", (dialog, which) -> handleInvite(notification, EventRepository.WAITLIST_STATUS_SNOOZED))
                   .setNegativeButton("Reject", (dialog, which) -> handleInvite(notification, EventRepository.WAITLIST_STATUS_DECLINED));
        } else {
            String statusText = "ACCEPTED".equals(notification.getStatus()) ? "Accepted" : "Declined";
            builder.setMessage(notification.getMessage() + "\n\nStatus: " + statusText);
            builder.setPositiveButton("OK", null);
        }

        builder.show();
    }

    /**
     * creates a popup that show the notification message
     * @param notification
     * NotificationItem that was clicked
     */
    private void showGeneralDialog(NotificationItem notification) {
        new AlertDialog.Builder(this)
                .setTitle(notification.getTitle())
                .setMessage(notification.getMessage())
                .setPositiveButton("OK", (dialog, which) -> {
                    notificationRepository.updateNotificationStatus(auth.getUid(), notification.getId(), "READ");
                })
                .show();
    }

    /**
     * method that updates the waitlist status of the user for an event
     * accepted/rejected if the user accepts/rejects
     * notifies user if there was a failure in updating the database
     * @param notification
     * NotificationItem that was clicked
     * @param newStatus
     * what option the user selected for the selection win notification
     */
    private void handleInvite(NotificationItem notification, String newStatus) {
        String uid = auth.getUid();
        String eventId = notification.getEventId();
        
        Toast.makeText(this, "Updating status to " + newStatus + "...", Toast.LENGTH_SHORT).show();
        
        eventRepository.updateWaitlistStatus(eventId, uid, newStatus)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Waitlist updated successfully", Toast.LENGTH_SHORT).show();
                    
                    String notifStatus = EventRepository.WAITLIST_STATUS_SNOOZED.equals(newStatus) ? "PENDING" : 
                                        (EventRepository.WAITLIST_STATUS_CONFIRMED.equals(newStatus) ? "ACCEPTED" : "REJECTED");
                    
                    notificationRepository.updateNotificationStatus(uid, notification.getId(), notifStatus)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Notification status updated", Toast.LENGTH_SHORT).show();
                            loadNotifications(uid);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to update notification: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Notification update failed", e);
                        });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Waitlist update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Waitlist update failed", e);
                });
    }

    /**
     * deletes a notification for the user and updates it to the database
     * notifies user if there was a failure in updating the database
     * @param notification
     * NotificationItem that was clicked
     */
    private void deleteNotification(NotificationItem notification) {
        notificationRepository.deleteNotification(auth.getUid(), notification.getId())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();
                    loadNotifications(auth.getUid());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * This is a controller for when myThingsLogoutButton is pressed
     * logs the user out of their user profile and navigates AuthMenuActivity
     */
    private void onLogOutClicked() {
        auth.signOut();
        AuthSessionPreference.setRemember(this, false);
        navigateToAuthMenu();
    }

    /**
     * navigates user to AuthMenuActivity and finishes this one
     */
    private void navigateToAuthMenu() {
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
