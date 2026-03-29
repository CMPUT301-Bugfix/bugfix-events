package com.example.eventlotterysystem;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import android.content.Intent;

import android.widget.ProgressBar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * This class deals with the browsing of all notifications for admins
 */
public class AdminNotificationsLogActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private TextView backButton;
    private ProgressBar notificationsLoading;
    private TextView notificationsEmptyState;
    private NotificationRepository notificationRepository;

    private boolean isAdminConfirmed;

    private RecyclerView notificationsRecyclerView;

    private NotificationAdapter notificationAdapter;

    private List<NotificationItem> notificationList = new ArrayList<>();

    /**
     * This method loads the UI, initializes the firebase and notification repository instances,
     * and connects the java variables to the views in the XML
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notification_log);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();

        backButton = findViewById(R.id.NotificationLogBackButton);
        notificationsLoading = findViewById(R.id.adminNotificationsLoading);
        notificationsEmptyState = findViewById(R.id.adminNotificationsEmptyState);
        notificationsRecyclerView = findViewById(R.id.adminNotificationsRecyclerView);

        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter(notificationList, this);
        notificationsRecyclerView.setAdapter(notificationAdapter);

        backButton.setOnClickListener(v -> finish());
    }

    /**
     * The startup for the activity
     * Ensures that the user is logged in and is an admin
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
     * This is the controller that is run when a notification is long clicked by the User (no different from regular click
     * it runs a method that gives the user a popup of the notification
     * @param notification NotificationItem that was clicked.
     */
    @Override
    public void onNotificationLongClick(NotificationItem notification) {
        new AlertDialog.Builder(this)
                .setTitle(notification.getTitle())
                .setMessage(notification.getMessage())
                .show();
    }

    /**
     * This is the controller that is run when a notification is clicked by the User
     * it runs a method that gives the user a popup of notification and its contents
     * @param notification NotificationItem that was clicked.
     */
    @Override
    public void onNotificationClick(NotificationItem notification) {
        new AlertDialog.Builder(this)
                .setTitle(notification.getTitle())
                .setMessage(notification.getMessage())
                .show();
    }

    /**
     * This method checks whether or not the user is an admin, and if so
     * then run the loadNotifications method, but if not, close the screen.
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
                    loadNotifications();
                })
                .addOnFailureListener(exception -> finish());
    }


    /**
     * This method is the heart of this class, it collects the notifications from the
     * Firestore notifications collection by using a snapshot listener for live updates,
     * and creates a list for all notifications to be displayed.
     * Also deals with error handling and notificationListener cleanup.
     */
    private void loadNotifications() {
        setLoading(true);

        notificationRepository.getNotificationLog()
                .addOnSuccessListener(notifications -> {
                    setLoading(false);
                    notificationList.clear();
                    notificationList.addAll(notifications);
                    notificationAdapter.notifyDataSetChanged();
                    Toast.makeText(AdminNotificationsLogActivity.this, "Succeeded to load notifications", Toast.LENGTH_SHORT).show();

                })
                .addOnFailureListener(e -> {
                    notificationsEmptyState.setVisibility(View.VISIBLE);
                    setLoading(false);
                    Toast.makeText(AdminNotificationsLogActivity.this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                });
    }


    /**
     * Deals with the loading screen, this method hides data/views
     * while we fetch current data
     * @param loading boolean value
     */
    private void setLoading(boolean loading) {
        notificationsLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        backButton.setEnabled(!loading);
        if (loading) {
            notificationsEmptyState.setVisibility(View.GONE);
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
}
