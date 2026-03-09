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

public class HostedEventsActivity extends AppCompatActivity {

    private static final String TAG = "HostedEventsActivity";

    private FirebaseAuth auth;
    private EventRepository repository;

    private ListView hostedEventsListView;
    private TextView emptyState;
    private EventListAdapter adapter;
    private final List<EventItem> hostedEvents = new ArrayList<>();

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
        hostedEventsListView.setOnItemClickListener((parent, view, position, id) -> openEventDetails(hostedEvents.get(position)));
    }

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

    private void loadHostedEvents(String uid) {
        repository.getHostedEvents(uid, new EventRepository.EventsCallback() {
            @Override
            public void onSuccess(List<EventItem> events) {
                hostedEvents.clear();
                hostedEvents.addAll(events);
                adapter.notifyDataSetChanged();
                updateEmptyState();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to load hosted events", e);
                Toast.makeText(
                        HostedEventsActivity.this,
                        buildLoadErrorMessage(e),
                        Toast.LENGTH_LONG
                ).show();
                hostedEvents.clear();
                adapter.notifyDataSetChanged();
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        boolean hasEvents = !hostedEvents.isEmpty();
        hostedEventsListView.setVisibility(hasEvents ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(hasEvents ? View.GONE : View.VISIBLE);
    }

    private void openEventDetails(EventItem event) {
        Intent intent = new Intent(this, ViewEventActivity.class);
        intent.putExtra("EVENT_ID", event.getId());
        startActivity(intent);
    }

    private void navigateToAuthMenu() {
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String buildLoadErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return getString(R.string.failed_to_load_events) + ": " + exception.getMessage().trim();
        }
        return getString(R.string.failed_to_load_events);
    }
}
