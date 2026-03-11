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

public class MyWaitlistActivity extends AppCompatActivity {

    private static final String TAG = "MyWaitlistActivity";

    private FirebaseAuth auth;
    private EventRepository repository;
    private ListView myWaitlistListView;
    private TextView emptyState;
    private WaitlistEntryAdapter adapter;
    private final List<WaitlistEntryItem> waitlistEntries = new ArrayList<>();

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

    private void updateEmptyState() {
        boolean hasEntries = !waitlistEntries.isEmpty();
        myWaitlistListView.setVisibility(hasEntries ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(hasEntries ? View.GONE : View.VISIBLE);
    }

    private void navigateToAuthMenu() {
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String buildLoadErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return getString(R.string.waitlist_load_failed) + ": " + exception.getMessage().trim();
        }
        return getString(R.string.waitlist_load_failed);
    }
}
