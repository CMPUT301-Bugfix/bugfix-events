package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class JoinableEventsActivity extends AppCompatActivity {

    private static final String TAG = "JoinableEventsActivity";

    private EventRepository repository;
    private ListView joinableEventsListView;
    private TextView emptyState;
    private EventListAdapter adapter;

    private final List<EventItem> events = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joinable_events);

        repository = new EventRepository();

        joinableEventsListView = findViewById(R.id.joinableEventsListView);
        emptyState = findViewById(R.id.joinableEventsEmptyState);

        adapter = new EventListAdapter(this, events);
        joinableEventsListView.setAdapter(adapter);

        findViewById(R.id.joinableEventsBackButton).setOnClickListener(v -> finish());

        joinableEventsListView.setOnItemClickListener((parent, view, position, id) -> {
            String eventId = events.get(position).getId();
            Intent intent = new Intent(this, ViewEventActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadJoinableEvents();
    }

    private void loadJoinableEvents() {
        repository.getCurrentEvents(new EventRepository.EventsCallback() {
            @Override
            public void onSuccess(List<EventItem> loadedEvents) {
                events.clear();
                events.addAll(loadedEvents);
                adapter.notifyDataSetChanged();
                updateEmptyState();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to load joinable events", e);
                events.clear();
                adapter.notifyDataSetChanged();
                updateEmptyState();
                Toast.makeText(
                        JoinableEventsActivity.this,
                        buildLoadErrorMessage(e),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void updateEmptyState() {
        boolean hasEvents = !events.isEmpty();
        joinableEventsListView.setVisibility(hasEvents ? ListView.VISIBLE : ListView.GONE);
        emptyState.setVisibility(hasEvents ? TextView.GONE : TextView.VISIBLE);
    }

    private String buildLoadErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return getString(R.string.failed_to_load_events) + ": " + exception.getMessage().trim();
        }
        return getString(R.string.failed_to_load_events);
    }
}
