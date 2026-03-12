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
    static final String WAITLIST_STATUS_CHOSEN = "CHOSEN";
    static final String WAITLIST_STATUS_CONFIRMED = "CONFIRMED";
    static final String WAITLIST_STATUS_DECLINED = "DECLINED";

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
        return userWaitlistEntry(uid, eventId)
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
    public Task<String> getWaitlistStatus(
            @NonNull String eventId,
            @NonNull String uid
    ) {
        return eventWaitlistEntry(eventId, uid)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("Failed to load waitlist status");
                    }

                    DocumentSnapshot doc = task.getResult();
                    if (doc == null || !doc.exists()) {
                        return "";
                    }

                    String status = normalize(doc.getString("status"));
                    return status.isEmpty() ? WAITLIST_STATUS_IN : status;
                });
    }

    public Task<Void> updateWaitlistStatus(
            @NonNull String eventId,
            @NonNull String uid,
            @NonNull String status
    ) {
        DocumentReference eventWaitlistRef = eventWaitlistEntry(eventId, uid);
        DocumentReference userWaitlistRef = userWaitlistEntry(uid, eventId);

        return firestore.runTransaction(transaction -> {
            DocumentSnapshot eventMembershipDoc = transaction.get(eventWaitlistRef);
            DocumentSnapshot userMembershipDoc = transaction.get(userWaitlistRef);
            if (!eventMembershipDoc.exists() && !userMembershipDoc.exists()) {
                throw new IllegalStateException("Waitlist entry not found");
            }

            if (eventMembershipDoc.exists()) {
                transaction.update(eventWaitlistRef, "status", status);
            }
            if (userMembershipDoc.exists()) {
                transaction.update(userWaitlistRef, "status", status);
            }
            return null;
        });
    }

    public Task<Void> joinWaitlist(
            @NonNull String eventId,
            @NonNull FirebaseUser currentUser
    ) {
        DocumentReference eventRef = firestore.collection("events").document(eventId);
        DocumentReference eventWaitlistRef = eventWaitlistEntry(eventId, currentUser.getUid());
        DocumentReference userWaitlistRef = userWaitlistEntry(currentUser.getUid(), eventId);

        return firestore.runTransaction(transaction -> {
            DocumentSnapshot eventDoc = transaction.get(eventRef);
            if (!eventDoc.exists()) {
                throw new IllegalStateException("Event not found");
            }

            DocumentSnapshot eventMembershipDoc = transaction.get(eventWaitlistRef);
            DocumentSnapshot userMembershipDoc = transaction.get(userWaitlistRef);
            if (eventMembershipDoc.exists() || userMembershipDoc.exists()) {
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

            transaction.set(eventWaitlistRef, membershipPayload);
            transaction.set(userWaitlistRef, membershipPayload);
            transaction.update(eventRef, "totalEntrants", incrementWaitlistCount(totalEntrants));
            return null;
        });
    }

    public Task<Void> leaveWaitlist(
            @NonNull String eventId,
            @NonNull String uid
    ) {
        DocumentReference eventRef = firestore.collection("events").document(eventId);
        DocumentReference eventWaitlistRef = eventWaitlistEntry(eventId, uid);
        DocumentReference userWaitlistRef = userWaitlistEntry(uid, eventId);

        return firestore.runTransaction(transaction -> {
            DocumentSnapshot eventMembershipDoc = transaction.get(eventWaitlistRef);
            DocumentSnapshot userMembershipDoc = transaction.get(userWaitlistRef);
            if (!eventMembershipDoc.exists() && !userMembershipDoc.exists()) {
                return null;
            }

            DocumentSnapshot eventDoc = transaction.get(eventRef);
            if (eventMembershipDoc.exists()) {
                transaction.delete(eventWaitlistRef);
            }
            if (userMembershipDoc.exists()) {
                transaction.delete(userWaitlistRef);
            }
            if (eventDoc.exists()) {
                Long totalEntrantsValue = eventDoc.getLong("totalEntrants");
                int totalEntrants = totalEntrantsValue == null ? 0 : totalEntrantsValue.intValue();
                transaction.update(eventRef, "totalEntrants", decrementWaitlistCount(totalEntrants));
            }
            return null;
        });
    }

    public Task<List<WaitlistEntryItem>> getMyWaitlists(@NonNull String uid) {
        return firestore.collection("users")
                .document(uid)
                .collection("waitlists")
                .get()
                .continueWithTask(queryTask -> {
                    if (!queryTask.isSuccessful()) {
                        throw queryTask.getException() != null
                                ? queryTask.getException()
                                : new IllegalStateException("Failed to load waitlists");
                    }

                    List<DocumentSnapshot> waitlistDocs = queryTask.getResult().getDocuments();
                    List<String> eventIds = new ArrayList<>();
                    List<String> statuses = new ArrayList<>();
                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

                    for (DocumentSnapshot doc : waitlistDocs) {
                        String status = normalize(doc.getString("status"));
                        if (status.isEmpty()) {
                            status = WAITLIST_STATUS_IN;
                        }
                        String eventId = firstNonEmpty(
                                normalize(doc.getString("eventId")),
                                doc.getId()
                        );
                        if (eventId.isEmpty()) {
                            continue;
                        }
                        eventIds.add(eventId);
                        statuses.add(status);
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
                                    statuses.get(index)
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

    public Task<Integer> getEntrantCount(
            @NonNull String eventId,
            @Nullable String statusFilter
    ) {
        com.google.firebase.firestore.Query query = firestore.collection("events")
                .document(eventId)
                .collection("waitlist");
        if (hasText(statusFilter)) {
            query = query.whereEqualTo("status", statusFilter);
        }

        return query.get().continueWith(task -> {
            if (!task.isSuccessful()) {
                throw task.getException() != null
                        ? task.getException()
                        : new IllegalStateException("Failed to load entrants");
            }
            return task.getResult().size();
        });
    }

    public Task<List<UserProfile>> getEntrantsForEvent(
            @NonNull String eventId,
            @Nullable String statusFilter
    ) {
        com.google.firebase.firestore.Query query = firestore.collection("events")
                .document(eventId)
                .collection("waitlist");
        if (hasText(statusFilter)) {
            query = query.whereEqualTo("status", statusFilter);
        }

        return query.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException() != null
                        ? task.getException()
                        : new IllegalStateException("Failed to load entrants");
            }

            List<Task<UserProfile>> entrantTasks = new ArrayList<>();
            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                String uid = firstNonEmpty(
                        normalize(doc.getString("uid")),
                        doc.getId()
                );
                if (!hasText(uid)) {
                    continue;
                }
                entrantTasks.add(firestore.collection("users")
                        .document(uid)
                        .get()
                        .continueWith(userTask -> {
                            if (!userTask.isSuccessful()) {
                                throw userTask.getException() != null
                                        ? userTask.getException()
                                        : new IllegalStateException("Failed to load entrant profile");
                            }

                            DocumentSnapshot userDoc = userTask.getResult();
                            if (userDoc == null || !userDoc.exists()) {
                                return null;
                            }
                            if (Boolean.TRUE.equals(userDoc.getBoolean("deleted"))) {
                                return null;
                            }
                            return readUserProfile(userDoc);
                        }));
            }

            if (entrantTasks.isEmpty()) {
                return Tasks.forResult(new ArrayList<>());
            }

            return Tasks.whenAllSuccess(entrantTasks).continueWith(resultsTask -> {
                if (!resultsTask.isSuccessful()) {
                    throw resultsTask.getException() != null
                            ? resultsTask.getException()
                            : new IllegalStateException("Failed to load entrants");
                }

                List<UserProfile> entrants = new ArrayList<>();
                for (Object result : resultsTask.getResult()) {
                    if (result instanceof UserProfile) {
                        entrants.add((UserProfile) result);
                    }
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
    private UserProfile readUserProfile(@NonNull DocumentSnapshot doc) {
        UserProfile userProfile = new UserProfile(
                normalize(doc.getString("fullName")),
                normalize(doc.getString("email")),
                normalize(doc.getString("username")),
                normalize(doc.getString("usernameKey")),
                normalize(doc.getString("phoneNumber")),
                normalize(doc.getString("accountType"))
        );
        userProfile.setUid(doc.getId());

        Timestamp createdAt = doc.getTimestamp("createdAt");
        if (createdAt != null) {
            userProfile.setCreatedAt(createdAt);
        }
        return userProfile;
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

    @NonNull
    private DocumentReference userWaitlistEntry(
            @NonNull String uid,
            @NonNull String eventId
    ) {
        return firestore.collection("users")
                .document(uid)
                .collection("waitlists")
                .document(eventId);
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
