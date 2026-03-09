package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;

public class JoinableEventsActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private ListView joinableEventsListView;
    private ArrayAdapter<String> adapter;

    private final ArrayList<String> eventTitles = new ArrayList<>();
    private final ArrayList<String> eventIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joinable_events);

        firestore = FirebaseFirestore.getInstance();

        joinableEventsListView = findViewById(R.id.joinableEventsListView);
        Button backButton = findViewById(R.id.joinableEventsBackButton);

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                eventTitles
        );
        joinableEventsListView.setAdapter(adapter);

        backButton.setOnClickListener(v -> finish());

        joinableEventsListView.setOnItemClickListener((parent, view, position, id) -> {
            String eventId = eventIds.get(position);
            Intent intent = new Intent(this, EventDetailsActivity.class);
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
        firestore.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventTitles.clear();
                    eventIds.clear();

                    Date now = new Date();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (isJoinableEvent(doc, now)) {
                            String title = doc.getString("title");
                            if (title == null || title.trim().isEmpty()) {
                                title = "Untitled Event";
                            }

                            eventTitles.add(title);
                            eventIds.add(doc.getId());
                        }
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show());
    }

    private boolean isJoinableEvent(DocumentSnapshot doc, Date now) {
        Boolean waitlistOpen = doc.getBoolean("waitlistOpen");
        Boolean deleted = doc.getBoolean("deleted");
        Timestamp eventDateTimestamp = doc.getTimestamp("eventDate");
        Timestamp registrationDeadlineTimestamp = doc.getTimestamp("registrationDeadline");

        if (Boolean.TRUE.equals(deleted)) {
            return false;
        }

        boolean openFlag = Boolean.TRUE.equals(waitlistOpen);

        boolean upcomingByEventDate = false;
        if (eventDateTimestamp != null) {
            upcomingByEventDate = eventDateTimestamp.toDate().after(now);
        }

        boolean beforeDeadline = false;
        if (registrationDeadlineTimestamp != null) {
            beforeDeadline = registrationDeadlineTimestamp.toDate().after(now);
        }

        return openFlag && (upcomingByEventDate || beforeDeadline);
    }
}