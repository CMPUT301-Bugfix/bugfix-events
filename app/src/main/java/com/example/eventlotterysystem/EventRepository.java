package com.example.eventlotterysystem;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventRepository {

    public interface EventsCallback {
        void onSuccess(List<EventItem> events);
        void onError(Exception e);
    }

    public interface EventCallback {
        void onSuccess(EventItem event);
        void onError(Exception e);
    }

    public interface SaveEventCallback {
        void onSuccess(String eventId);
        void onError(Exception e);
    }

    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    public EventRepository() {
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public void getCurrentEvents(@NonNull EventsCallback callback) {
        firestore.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<EventItem> results = new ArrayList<>();
                    Date now = new Date();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (isJoinableEvent(doc, now)) {
                            results.add(readEventItem(doc));
                        }
                    }

                    sortByEventDate(results);
                    callback.onSuccess(results);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getHostedEvents(@NonNull String hostUid, @NonNull EventsCallback callback) {
        firestore.collection("events")
                .whereEqualTo("hostUid", hostUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<EventItem> results = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (Boolean.TRUE.equals(doc.getBoolean("deleted"))) {
                            continue;
                        }
                        results.add(readEventItem(doc));
                    }
                    sortByEventDate(results);
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
                    callback.onSuccess(readEventItem(doc));
                })
                .addOnFailureListener(callback::onError);
    }

    public void createEvent(
            @NonNull FirebaseUser currentUser,
            @NonNull EventItem draftEvent,
            @Nullable Uri posterUri,
            @NonNull SaveEventCallback callback
    ) {
        DocumentReference eventRef = firestore.collection("events").document();
        DocumentReference userRef = firestore.collection("users").document(currentUser.getUid());

        userRef.get()
                .addOnSuccessListener(userSnapshot -> {
                    String accountType = normalize(userSnapshot.getString("accountType"));
                    String hostDisplayName = getHostDisplayName(userSnapshot, currentUser);
                    if (posterUri == null) {
                        createEventRecord(eventRef, userRef, draftEvent, currentUser.getUid(), hostDisplayName, "", accountType, callback, null);
                        return;
                    }

                    StorageReference posterRef = storage.getReference()
                            .child("event-posters/" + currentUser.getUid() + "/" + eventRef.getId() + ".jpg");

                    UploadTask uploadTask = posterRef.putFile(posterUri);
                    uploadTask.continueWithTask(task -> {
                                if (!task.isSuccessful()) {
                                    throw task.getException() != null
                                            ? task.getException()
                                            : new IllegalStateException("Poster upload failed");
                                }
                                return posterRef.getDownloadUrl();
                            })
                            .addOnSuccessListener(downloadUri ->
                                    createEventRecord(
                                            eventRef,
                                            userRef,
                                            draftEvent,
                                            currentUser.getUid(),
                                            hostDisplayName,
                                            downloadUri.toString(),
                                            accountType,
                                            callback,
                                            posterRef
                                    ))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void updateEvent(
            @NonNull String eventId,
            @NonNull FirebaseUser currentUser,
            @NonNull EventItem event,
            @Nullable Uri posterUri,
            @NonNull SaveEventCallback callback
    ) {
        DocumentReference eventRef = firestore.collection("events").document(eventId);
        if (posterUri == null) {
            eventRef.update(buildUpdatedEventPayload(event, null))
                    .addOnSuccessListener(unused -> callback.onSuccess(eventId))
                    .addOnFailureListener(callback::onError);
            return;
        }

        StorageReference posterRef = storage.getReference()
                .child("event-posters/" + currentUser.getUid() + "/" + eventId + ".jpg");

        posterRef.putFile(posterUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("Poster upload failed");
                    }
                    return posterRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri ->
                        eventRef.update(buildUpdatedEventPayload(event, downloadUri.toString()))
                                .addOnSuccessListener(unused -> callback.onSuccess(eventId))
                                .addOnFailureListener(callback::onError))
                .addOnFailureListener(callback::onError);
    }

    private void createEventRecord(
            @NonNull DocumentReference eventRef,
            @NonNull DocumentReference userRef,
            @NonNull EventItem draftEvent,
            @NonNull String hostUid,
            @NonNull String hostDisplayName,
            @NonNull String posterUrl,
            @NonNull String accountType,
            @NonNull SaveEventCallback callback,
            @Nullable StorageReference posterRef
    ) {
        WriteBatch batch = firestore.batch();
        batch.set(eventRef, buildEventPayload(draftEvent, hostUid, hostDisplayName, posterUrl));

        if (accountType.isEmpty() || "user".equals(accountType)) {
            Map<String, Object> accountPayload = new HashMap<>();
            accountPayload.put("accountType", "organizer");
            batch.set(userRef, accountPayload, SetOptions.merge());
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(eventRef.getId()))
                .addOnFailureListener(exception -> {
                    if (posterRef != null) {
                        posterRef.delete();
                    }
                    callback.onError(exception);
                });
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

    @NonNull
    public EventItem readEventItem(@NonNull DocumentSnapshot doc) {
        String title = doc.getString("title");
        String description = doc.getString("description");
        String location = doc.getString("location");
        String posterUrl = doc.getString("posterUrl");
        String hostUid = doc.getString("hostUid");
        String hostDisplayName = doc.getString("hostDisplayName");
        Long maxEntrantsValue = doc.getLong("maxEntrants");
        Long maxParticipantsValue = doc.getLong("maxParticipants");
        Long totalEntrantsValue = doc.getLong("totalEntrants");
        Timestamp eventDateTimestamp = doc.getTimestamp("eventDate");
        Timestamp registrationDeadlineTimestamp = doc.getTimestamp("registrationDeadline");

        if (!hasText(title)) {
            title = "Untitled Event";
        }
        if (description == null) {
            description = "";
        }
        if (location == null) {
            location = "";
        }
        if (posterUrl == null) {
            posterUrl = "";
        }
        if (hostUid == null) {
            hostUid = "";
        }
        if (hostDisplayName == null) {
            hostDisplayName = "";
        }

        return new EventItem(
                doc.getId(),
                title,
                description,
                location,
                posterUrl,
                maxEntrantsValue == null ? 0 : maxEntrantsValue.intValue(),
                maxParticipantsValue == null ? 0 : maxParticipantsValue.intValue(),
                totalEntrantsValue == null ? 0 : totalEntrantsValue.intValue(),
                registrationDeadlineTimestamp == null ? null : registrationDeadlineTimestamp.toDate(),
                eventDateTimestamp == null ? null : eventDateTimestamp.toDate(),
                Boolean.TRUE.equals(doc.getBoolean("requiresGeolocation")),
                hostUid,
                hostDisplayName
        );
    }

    @NonNull
    private Map<String, Object> buildEventPayload(
            @NonNull EventItem event,
            @NonNull String hostUid,
            @NonNull String hostDisplayName,
            @NonNull String posterUrl
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", event.getTitle());
        payload.put("description", event.getDescription());
        payload.put("location", event.getLocation());
        payload.put("posterUrl", posterUrl);
        payload.put("maxEntrants", event.getMaxEntrants());
        payload.put("maxParticipants", event.getMaxParticipants());
        payload.put("totalEntrants", event.getTotalEntrants());
        payload.put("registrationDeadline", event.getRegistrationDeadline());
        payload.put("eventDate", event.getEventDate());
        payload.put("requiresGeolocation", event.isRequiresGeolocation());
        payload.put("hostUid", hostUid);
        payload.put("hostDisplayName", hostDisplayName);
        payload.put("waitlistOpen", true);
        payload.put("deleted", false);
        payload.put("createdAt", Timestamp.now());
        return payload;
    }

    @NonNull
    private Map<String, Object> buildUpdatedEventPayload(
            @NonNull EventItem event,
            @Nullable String posterUrl
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", event.getTitle());
        payload.put("description", event.getDescription());
        payload.put("location", event.getLocation());
        payload.put("maxEntrants", event.getMaxEntrants());
        payload.put("maxParticipants", event.getMaxParticipants());
        payload.put("registrationDeadline", event.getRegistrationDeadline());
        payload.put("eventDate", event.getEventDate());
        payload.put("requiresGeolocation", event.isRequiresGeolocation());
        if (posterUrl != null) {
            payload.put("posterUrl", posterUrl);
        }
        return payload;
    }

    @NonNull
    private String getHostDisplayName(
            @NonNull DocumentSnapshot userSnapshot,
            @NonNull FirebaseUser currentUser
    ) {
        String[] candidates = new String[] {
                userSnapshot.getString("fullName"),
                userSnapshot.getString("username"),
                currentUser.getDisplayName(),
                currentUser.getEmail(),
                currentUser.getUid()
        };
        for (String candidate : candidates) {
            if (hasText(candidate)) {
                return candidate.trim();
            }
        }
        return currentUser.getUid();
    }

    private void sortByEventDate(@NonNull List<EventItem> events) {
        Collections.sort(events, Comparator.comparing(
                EventItem::getEventDate,
                Comparator.nullsLast(Date::compareTo)
        ));
    }

    @NonNull
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
