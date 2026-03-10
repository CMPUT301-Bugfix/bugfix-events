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

public class AdminBrowseEventsActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private TextView backButton;
    private ProgressBar eventsLoading;
    private TextView eventsEmptyState;
    private ScrollView eventsScrollView;
    private LinearLayout eventsContainer;

    private boolean isAdminConfirmed;
    private ListenerRegistration eventsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_events);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        backButton = findViewById(R.id.adminEventsBackButton);
        eventsLoading = findViewById(R.id.adminEventsLoading);
        eventsEmptyState = findViewById(R.id.adminEventsEmptyState);
        eventsScrollView = findViewById(R.id.adminEventsScrollView);
        eventsContainer = findViewById(R.id.adminEventsContainer);

        backButton.setOnClickListener(v -> finish());
    }

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

    @Override
    protected void onStop() {
        super.onStop();
        if (eventsListener != null) {
            eventsListener.remove();
            eventsListener = null;
        }
    }

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

            List<AdminEvent> events = new ArrayList<>();

            if (value != null) {
                for (DocumentSnapshot snapshot : value.getDocuments()) {
                    if (Boolean.TRUE.equals(snapshot.getBoolean("deleted"))) {
                        continue;
                    }

                    String id = snapshot.getId();
                    String title = normalize(snapshot.getString("title"));
                    String hostUid = normalize(snapshot.getString("hostUid"));

                    events.add(new AdminEvent(
                            id,
                            title,
                            hostUid,
                            snapshot.getTimestamp("createdAt")
                    ));
                }
            }

            
            events.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));

            renderEvents(events);
            setLoading(false);
        });
    }

    private void renderEvents(@NonNull List<AdminEvent> events) {
        eventsContainer.removeAllViews();

        if (events.isEmpty()) {
            eventsEmptyState.setText(getString(R.string.no_events));
            eventsEmptyState.setVisibility(View.VISIBLE);
            eventsScrollView.setVisibility(View.GONE);
            return;
        }

        eventsEmptyState.setVisibility(View.GONE);
        eventsScrollView.setVisibility(View.VISIBLE);

        for (AdminEvent event : events) {
            View row = getLayoutInflater().inflate(R.layout.item_admin_event, eventsContainer, false);
            TextView eventTitleValue = row.findViewById(R.id.eventTitleValue);

            String title = event.getTitle();
            eventTitleValue.setText(TextUtils.isEmpty(title) ? getString(R.string.unknown_event_title) : title);

            row.setOnClickListener(v -> openEventDetails(event));
            eventsContainer.addView(row);
        }
    }

    private void openEventDetails(@NonNull AdminEvent event) {
        Intent intent = new Intent(this, AdminEventDetailsActivity.class);
        intent.putExtra(AdminEventDetailsActivity.EVENT_ID, event.getId());
        intent.putExtra(AdminEventDetailsActivity.EVENT_TITLE, normalize(event.getTitle()));
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        eventsLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        backButton.setEnabled(!loading);
        if (loading) {
            eventsEmptyState.setVisibility(View.GONE);
            eventsScrollView.setVisibility(View.GONE);
        }
    }

    private void navigateToAuthMenu() {
        auth.signOut();
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @NonNull
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
