package com.example.eventlotterysystem;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EventRepository {

    public interface EventsCallback {
        void onSuccess(List<EventItem> events);
        void onError(Exception e);
    }

    public interface EventCallback {
        void onSuccess(EventItem event);
        void onError(Exception e);
    }

    private final FirebaseFirestore firestore;

    public EventRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void getCurrentEvents(@NonNull EventsCallback callback) {
        firestore.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<EventItem> results = new ArrayList<>();
                    Date now = new Date();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (isJoinableEvent(doc, now)) {
                            String title = doc.getString("title");
                            String description = doc.getString("description");

                            if (title == null || title.trim().isEmpty()) {
                                title = "Untitled Event";
                            }
                            if (description == null) {
                                description = "";
                            }

                            results.add(new EventItem(doc.getId(), title, description));
                        }
                    }

                    callback.onSuccess(results);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getEventById(String eventId, @NonNull EventCallback callback) {
        firestore.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onError(new Exception("Event not found"));
                        return;
                    }

                    String title = doc.getString("title");
                    String description = doc.getString("description");

                    if (title == null || title.trim().isEmpty()) {
                        title = "Untitled Event";
                    }
                    if (description == null) {
                        description = "";
                    }

                    callback.onSuccess(new EventItem(doc.getId(), title, description));
                })
                .addOnFailureListener(callback::onError);
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