package com.example.eventlotterysystem;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
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
    static final String WAITLIST_STATUS_IN = "IN_WAITLIST";

    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    public EventRepository() {
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public Task<List<EventItem>> getCurrentEvents() {
        return firestore.collection("events")
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("Failed to load events");
                    }

                    List<EventItem> results = new ArrayList<>();
                    Date now = new Date();

                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        if (isJoinableEvent(doc, now)) {
                            results.add(readEventItem(doc));
                        }
                    }

                    sortByEventDate(results);
                    return results;
                });
    }

    public Task<List<EventItem>> getHostedEvents(@NonNull String hostUid) {
        return firestore.collection("events")
                .whereEqualTo("hostUid", hostUid)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("Failed to load hosted events");
                    }

                    List<EventItem> results = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        if (Boolean.TRUE.equals(doc.getBoolean("deleted"))) {
                            continue;
                        }
                        results.add(readEventItem(doc));
                    }
                    sortByEventDate(results);
                    return results;
                });
    }

    public Task<EventItem> getEventById(String eventId) {
        return firestore.collection("events")
                .document(eventId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("Failed to load event");
                    }
                    DocumentSnapshot doc = task.getResult();
                    if (!doc.exists()) {
                        throw new IllegalStateException("Event not found");
                    }
                    return readEventItem(doc);
                });
    }

    public Task<String> createEvent(
            @NonNull FirebaseUser currentUser,
            @NonNull EventItem draftEvent,
            @Nullable Uri posterUri
    ) {
        DocumentReference eventRef = firestore.collection("events").document();
        DocumentReference userRef = firestore.collection("users").document(currentUser.getUid());

        return userRef.get().continueWithTask(userTask -> {
                    if (!userTask.isSuccessful()) {
                        throw userTask.getException() != null
                                ? userTask.getException()
                                : new IllegalStateException("Failed to load user profile");
                    }
                    DocumentSnapshot userSnapshot = userTask.getResult();
                    String accountType = normalize(userSnapshot.getString("accountType"));
                    String hostDisplayName = getHostDisplayName(userSnapshot, currentUser);
                    if (posterUri == null) {
                        return createEventRecord(
                                eventRef,
                                userRef,
                                draftEvent,
                                currentUser.getUid(),
                                hostDisplayName,
                                "",
                                accountType,
                                null
                        );
                    }

                    StorageReference posterRef = storage.getReference()
                            .child("event-posters/" + currentUser.getUid() + "/" + eventRef.getId() + ".jpg");

                    UploadTask uploadTask = posterRef.putFile(posterUri);
                    return uploadTask.continueWithTask(task -> {
                                if (!task.isSuccessful()) {
                                    throw task.getException() != null
                                            ? task.getException()
                                            : new IllegalStateException("Poster upload failed");
                                }
                                return posterRef.getDownloadUrl();
                            })
                            .continueWithTask(downloadTask -> {
                                if (!downloadTask.isSuccessful()) {
                                    throw downloadTask.getException() != null
                                            ? downloadTask.getException()
                                            : new IllegalStateException("Poster upload failed");
                                }
                                return createEventRecord(
                                        eventRef,
                                        userRef,
                                        draftEvent,
                                        currentUser.getUid(),
                                        hostDisplayName,
                                        downloadTask.getResult().toString(),
                                        accountType,
                                        posterRef
                                );
                            });
                });
    }

    public Task<Boolean> getWaitlistState(
            @NonNull String eventId,
            @NonNull String uid
    ) {
        return eventWaitlistEntry(eventId, uid)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("Failed to load waitlist state");
                    }
                    DocumentSnapshot doc = task.getResult();
                    return doc != null && doc.exists();
                });
    }

    public Task<Void> joinWaitlist(
            @NonNull String eventId,
            @NonNull FirebaseUser currentUser
    ) {
        DocumentReference eventRef = firestore.collection("events").document(eventId);
        DocumentReference userRef = firestore.collection("users").document(currentUser.getUid());
        DocumentReference waitlistRef = eventWaitlistEntry(eventId, currentUser.getUid());

        return firestore.runTransaction(transaction -> {
            DocumentSnapshot eventDoc = transaction.get(eventRef);
            DocumentSnapshot userDoc = transaction.get(userRef);
            if (!eventDoc.exists()) {
                throw new IllegalStateException("Event not found");
            }

            DocumentSnapshot membershipDoc = transaction.get(waitlistRef);
            if (membershipDoc.exists()) {
                return null;
            }

            if (Boolean.TRUE.equals(eventDoc.getBoolean("deleted"))) {
                throw new IllegalStateException("Event not found");
            }

            String hostUid = normalize(eventDoc.getString("hostUid"));
            if (hostUid.equals(currentUser.getUid())) {
                throw new IllegalStateException("Organizers cannot join their own waitlist");
            }

            boolean waitlistOpen = Boolean.TRUE.equals(eventDoc.getBoolean("waitlistOpen"));
            Timestamp registrationDeadline = eventDoc.getTimestamp("registrationDeadline");
            Date deadlineDate = registrationDeadline == null ? null : registrationDeadline.toDate();
            if (!isWaitlistJoinOpen(waitlistOpen, deadlineDate, new Date())) {
                throw new IllegalStateException("This waitlist is closed");
            }

            Long totalEntrantsValue = eventDoc.getLong("totalEntrants");
            int totalEntrants = totalEntrantsValue == null ? 0 : totalEntrantsValue.intValue();

            Map<String, Object> membershipPayload = new HashMap<>();
            membershipPayload.put("eventId", eventId);
            membershipPayload.put("status", WAITLIST_STATUS_IN);
            membershipPayload.put("joinedAt", FieldValue.serverTimestamp());
            membershipPayload.put("uid", currentUser.getUid());
            membershipPayload.put("name", getWaitlistEntrantName(userDoc, currentUser));
            membershipPayload.put("username", normalize(userDoc.getString("username")));
            membershipPayload.put("email", firstNonEmpty(
                    normalize(userDoc.getString("email")),
                    normalize(currentUser.getEmail())
            ));
            membershipPayload.put("phone", normalize(userDoc.getString("phoneNumber")));
            membershipPayload.put("accountType", firstNonEmpty(
                    normalize(userDoc.getString("accountType")),
                    "user"
            ));
            membershipPayload.put("createdAt", userDoc.getTimestamp("createdAt"));

            transaction.set(waitlistRef, membershipPayload);
            transaction.update(eventRef, "totalEntrants", incrementWaitlistCount(totalEntrants));
            return null;
        });
    }

    public Task<Void> leaveWaitlist(
            @NonNull String eventId,
            @NonNull String uid
    ) {
        DocumentReference eventRef = firestore.collection("events").document(eventId);
        DocumentReference waitlistRef = eventWaitlistEntry(eventId, uid);

        return firestore.runTransaction(transaction -> {
            DocumentSnapshot membershipDoc = transaction.get(waitlistRef);
            if (!membershipDoc.exists()) {
                return null;
            }

            DocumentSnapshot eventDoc = transaction.get(eventRef);
            transaction.delete(waitlistRef);
            if (eventDoc.exists()) {
                Long totalEntrantsValue = eventDoc.getLong("totalEntrants");
                int totalEntrants = totalEntrantsValue == null ? 0 : totalEntrantsValue.intValue();
                transaction.update(eventRef, "totalEntrants", decrementWaitlistCount(totalEntrants));
            }
            return null;
        });
    }

    public Task<List<WaitlistEntryItem>> getMyWaitlists(@NonNull String uid) {
        return firestore.collectionGroup("waitlist")
                .whereEqualTo("uid", uid)
                .get()
                .continueWithTask(queryTask -> {
                    if (!queryTask.isSuccessful()) {
                        throw queryTask.getException() != null
                                ? queryTask.getException()
                                : new IllegalStateException("Failed to load waitlists");
                    }

                    List<DocumentSnapshot> waitlistDocs = queryTask.getResult().getDocuments();
                    List<String> eventIds = new ArrayList<>();
                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

                    for (DocumentSnapshot doc : waitlistDocs) {
                        String status = normalize(doc.getString("status"));
                        if (!WAITLIST_STATUS_IN.equals(status)) {
                            continue;
                        }
                        String eventId = firstNonEmpty(
                                normalize(doc.getString("eventId")),
                                extractWaitlistEventId(doc)
                        );
                        if (eventId.isEmpty()) {
                            continue;
                        }
                        eventIds.add(eventId);
                        tasks.add(firestore.collection("events").document(eventId).get());
                    }

                    if (tasks.isEmpty()) {
                        return Tasks.forResult(new ArrayList<>());
                    }

                    return Tasks.whenAllSuccess(tasks).continueWith(eventsTask -> {
                        if (!eventsTask.isSuccessful()) {
                            throw eventsTask.getException() != null
                                    ? eventsTask.getException()
                                    : new IllegalStateException("Failed to load waitlists");
                        }

                        List<?> results = eventsTask.getResult();
                        List<WaitlistEntryItem> items = new ArrayList<>();
                        for (int index = 0; index < results.size(); index++) {
                            Object result = results.get(index);
                            if (!(result instanceof DocumentSnapshot)) {
                                continue;
                            }
                            DocumentSnapshot eventDoc = (DocumentSnapshot) result;
                            if (!eventDoc.exists()) {
                                continue;
                            }
                            if (Boolean.TRUE.equals(eventDoc.getBoolean("deleted"))) {
                                continue;
                            }
                            EventItem event = readEventItem(eventDoc);
                            items.add(new WaitlistEntryItem(
                                    eventIds.get(index),
                                    event.getTitle(),
                                    event.getEventDate(),
                                    WAITLIST_STATUS_IN
                            ));
                        }

                        Collections.sort(items, Comparator.comparing(
                                WaitlistEntryItem::getEventDate,
                                Comparator.nullsLast(Date::compareTo)
                        ));
                        return items;
                    });
                });
    }

    public Task<List<UserProfile>> getEntrantsForEvent(@NonNull String eventId) {
        return firestore.collection("events")
                .document(eventId)
                .collection("waitlist")
                .whereEqualTo("status", WAITLIST_STATUS_IN)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("Failed to load entrants");
                    }

                    List<UserProfile> entrants = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        UserProfile entrant = readEntrantFromWaitlistDoc(doc);
                        entrants.add(entrant);
                    }

                    entrants.sort(Comparator.comparing(
                            entrant -> firstNonEmpty(
                                    entrant.getName(),
                                    entrant.getUsername(),
                                    entrant.getEmail(),
                                    entrant.getUid()
                            ),
                            String.CASE_INSENSITIVE_ORDER
                    ));
                    return entrants;
                });
    }

    public Task<String> updateEvent(
            @NonNull String eventId,
            @NonNull FirebaseUser currentUser,
            @NonNull EventItem event,
            @Nullable Uri posterUri
    ) {
        DocumentReference eventRef = firestore.collection("events").document(eventId);
        if (posterUri == null) {
            return eventRef.update(buildUpdatedEventPayload(event, null))
                    .continueWith(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException() != null
                                    ? task.getException()
                                    : new IllegalStateException("Failed to update event");
                        }
                        return eventId;
                    });
        }

        StorageReference posterRef = storage.getReference()
                .child("event-posters/" + currentUser.getUid() + "/" + eventId + ".jpg");

        return posterRef.putFile(posterUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("Poster upload failed");
                    }
                    return posterRef.getDownloadUrl();
                })
                .continueWithTask(downloadTask -> {
                    if (!downloadTask.isSuccessful()) {
                        throw downloadTask.getException() != null
                                ? downloadTask.getException()
                                : new IllegalStateException("Poster upload failed");
                    }
                    return eventRef.update(buildUpdatedEventPayload(event, downloadTask.getResult().toString()))
                            .continueWith(updateTask -> {
                                if (!updateTask.isSuccessful()) {
                                    throw updateTask.getException() != null
                                            ? updateTask.getException()
                                            : new IllegalStateException("Failed to update event");
                                }
                                return eventId;
                            });
                });
    }

    private Task<String> createEventRecord(
            @NonNull DocumentReference eventRef,
            @NonNull DocumentReference userRef,
            @NonNull EventItem draftEvent,
            @NonNull String hostUid,
            @NonNull String hostDisplayName,
            @NonNull String posterUrl,
            @NonNull String accountType,
            @Nullable StorageReference posterRef
    ) {
        WriteBatch batch = firestore.batch();
        batch.set(eventRef, buildEventPayload(draftEvent, hostUid, hostDisplayName, posterUrl));

        if (accountType.isEmpty() || "user".equals(accountType)) {
            Map<String, Object> accountPayload = new HashMap<>();
            accountPayload.put("accountType", "organizer");
            batch.set(userRef, accountPayload, SetOptions.merge());
        }

        TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();
        batch.commit()
                .addOnSuccessListener(unused -> taskCompletionSource.setResult(eventRef.getId()))
                .addOnFailureListener(exception -> {
                    if (posterRef != null) {
                        posterRef.delete().addOnCompleteListener(task -> taskCompletionSource.setException(exception));
                        return;
                    }
                    taskCompletionSource.setException(exception);
                });
        return taskCompletionSource.getTask();
    }

    private boolean isJoinableEvent(DocumentSnapshot doc, Date now) {
        Boolean waitlistOpen = doc.getBoolean("waitlistOpen");
        Boolean deleted = doc.getBoolean("deleted");
        Timestamp eventDateTimestamp = doc.getTimestamp("eventDate");
        Timestamp registrationDeadlineTimestamp = doc.getTimestamp("registrationDeadline");

        if (Boolean.TRUE.equals(deleted)) {
            return false;
        }

        boolean upcomingByEventDate = false;
        if (eventDateTimestamp != null) {
            upcomingByEventDate = eventDateTimestamp.toDate().after(now);
        }

        boolean beforeDeadline = isWaitlistJoinOpen(
                Boolean.TRUE.equals(waitlistOpen),
                registrationDeadlineTimestamp == null ? null : registrationDeadlineTimestamp.toDate(),
                now
        );

        return Boolean.TRUE.equals(waitlistOpen) && (upcomingByEventDate || beforeDeadline);
    }

    @NonNull
    private EventItem readEventItem(@NonNull DocumentSnapshot doc) {
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
        Boolean waitlistOpenValue = doc.getBoolean("waitlistOpen");

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
                hostDisplayName,
                Boolean.TRUE.equals(waitlistOpenValue)
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

    @NonNull
    private String getWaitlistEntrantName(
            @NonNull DocumentSnapshot userSnapshot,
            @NonNull FirebaseUser currentUser
    ) {
        return firstNonEmpty(
                normalize(userSnapshot.getString("fullName")),
                normalize(currentUser.getDisplayName()),
                normalize(userSnapshot.getString("username")),
                normalize(currentUser.getEmail()),
                currentUser.getUid()
        );
    }

    @NonNull
    private UserProfile readEntrantFromWaitlistDoc(@NonNull DocumentSnapshot doc) {
        String uid = firstNonEmpty(
                normalize(doc.getString("uid")),
                doc.getId()
        );
        String username = normalize(doc.getString("username"));
        String name = firstNonEmpty(
                normalize(doc.getString("name")),
                username,
                normalize(doc.getString("email"))
        );

        UserProfile entrant = new UserProfile(
                name,
                normalize(doc.getString("email")),
                username,
                "",
                normalize(doc.getString("phone")),
                normalize(doc.getString("accountType"))
        );
        entrant.setUid(uid);

        Timestamp createdAt = doc.getTimestamp("createdAt");
        if (createdAt != null) {
            entrant.setCreatedAt(createdAt);
        }
        return entrant;
    }

    @NonNull
    private String extractWaitlistEventId(@NonNull DocumentSnapshot snapshot) {
        DocumentReference eventRef = snapshot.getReference().getParent().getParent();
        if (eventRef == null) {
            return "";
        }
        return eventRef.getId();
    }

    @NonNull
    private DocumentReference eventWaitlistEntry(
            @NonNull String eventId,
            @NonNull String uid
    ) {
        return firestore.collection("events")
                .document(eventId)
                .collection("waitlist")
                .document(uid);
    }

    private void sortByEventDate(@NonNull List<EventItem> events) {
        Collections.sort(events, Comparator.comparing(
                EventItem::getEventDate,
                Comparator.nullsLast(Date::compareTo)
        ));
    }

    static boolean isWaitlistJoinOpen(
            boolean waitlistOpen,
            @Nullable Date registrationDeadline,
            @NonNull Date now
    ) {
        if (!waitlistOpen) {
            return false;
        }
        return registrationDeadline == null || registrationDeadline.after(now);
    }

    static int incrementWaitlistCount(int currentCount) {
        return currentCount + 1;
    }

    static int decrementWaitlistCount(int currentCount) {
        return Math.max(0, currentCount - 1);
    }

    @NonNull
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    @NonNull
    private String firstNonEmpty(String... candidates) {
        for (String candidate : candidates) {
            if (hasText(candidate)) {
                return candidate.trim();
            }
        }
        return "";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
