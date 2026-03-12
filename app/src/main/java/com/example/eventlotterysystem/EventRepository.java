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

/**
 * The Class the manages the interactions between Event as a object in the program and as a document in the database
 */
public class EventRepository {
    public static final String WAITLIST_STATUS_IN = "IN_WAITLIST";
    public static final String WAITLIST_STATUS_CHOSEN = "CHOSEN";
    public static final String WAITLIST_STATUS_CONFIRMED = "CONFIRMED";
    public static final String WAITLIST_STATUS_DECLINED = "DECLINED";
    public static final String WAITLIST_STATUS_SNOOZED = "SNOOZED";

    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    /**
     * creates the EventRepository object which is just references to the database
     * the methods allows for access to Event documents in the database through Ids and event objects
     */
    public EventRepository() {
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    /**
     * Loads all active events in the database into a list of event objects
     * @return
     * task that reads the database for all match events and creates event object for them
     */
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

    /**
     * Load the a list event from the database for all events creator matching user ID and creates a respective event object list
     * @param hostUid
     * ID of the user that created the event
     * @return
     * task that gets all the events that the user created as a list of event objects raises an exception of failure
     */
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

    /**
     * Load the event from the database by its id and creates a respective event object
     * @param eventId
     * id of the Event
     * @return
     * a task that loads the event by Id into an event object and raises an exception if the task fails
     */
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

    /**
     * Manages task of creating an event document in the database
     * may also add a posterUri if there is one which it then also uploads the image file to the database
     * @param currentUser
     * current User document from database
     * @param draftEvent
     * Event object to be added as a event document to the database
     * @param posterUri
     * link the the event image
     * @return
     * appropriate task to create a new event document to the database depending on the situation that raises an exception it the task fails
     */
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

    /**
     * Check to see if there is a waitlist entry for a user signed up to an event
     * @param eventId
     * Id of the Event
     * @param uid
     * Id of the user
     * @return
     * Task that tries to obtains a waitlist entry matching the argument id's and returns true if found
     */
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

            if (eventMembershipDoc.exists()) {
                transaction.update(eventWaitlistRef, "status", status);
                if (WAITLIST_STATUS_CHOSEN.equals(status) || WAITLIST_STATUS_SNOOZED.equals(status)) {
                    transaction.update(eventWaitlistRef, "chosenAt", FieldValue.serverTimestamp());
                }
            }
            if (userMembershipDoc.exists()) {
                transaction.update(userWaitlistRef, "status", status);
                if (WAITLIST_STATUS_CHOSEN.equals(status) || WAITLIST_STATUS_SNOOZED.equals(status)) {
                    transaction.update(userWaitlistRef, "chosenAt", FieldValue.serverTimestamp());
                }
            }
            return null;
        });
    }

    /**
     * Updates the database to remove a user from an event waitlist
     * @param eventId
     * Id of event that was signed up
     * @param currentUser
     * current User document from database
     * @return
     * Task that removes the waitlist document from the user and event documents
     */
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

    /**
     * Updates the database to remove a user from an event waitlist
     * @param eventId
     * Id of event that was signed up
     * @param uid
     * Id of the signed up user
     * @return
     * Task that removes the waitlist document from the user and event
     */
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

    /**
     * get the all sign-up's a user has done for event
     * Using user ID to access the database loads waitlist entries and creating WaitlistEntryItem objects
     * @param uid
     * Id of the User
     * @return
     * Task that obtains the all WaitlistEntryItem corresponding to the user
     */
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
                        return Tasks.forResult(new ArrayList<WaitlistEntryItem>());
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
  
    /**
     * gets the users who have signed up for the event
     * Using event ID to access the database loads waitlist entries
     * then the user data matching the entry to create userprofile objects
     * @param eventId
     * ID of the event
     * @param statusFilter
     * also filers for only users of a matching status of acceptance
     * @return
     * Task that obtains the user profiles of user who have signed-up to the waitlist (or other stage of acceptance to event)
     */
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

    /**
     * Performs the lottery draw by moving random users from IN_WAITLIST to CHOSEN.
     */
    public Task<Void> performLotteryDraw(String eventId, String winningMessage) {
        return firestore.collection("events").document(eventId).get().continueWithTask(task -> {
            DocumentSnapshot eventDoc = task.getResult();
            if (!eventDoc.exists()) throw new Exception("Event not found");
            
            EventItem event = readEventItem(eventDoc);
            int maxParticipants = event.getMaxParticipants();
            if (maxParticipants <= 0) maxParticipants = event.getTotalEntrants();

            final int finalMax = maxParticipants;

            return firestore.collection("events").document(eventId).collection("waitlist")
                    .whereEqualTo("status", WAITLIST_STATUS_IN)
                    .get().continueWithTask(waitlistTask -> {
                        List<DocumentSnapshot> candidates = waitlistTask.getResult().getDocuments();
                        if (candidates.isEmpty()) return Tasks.forResult(null);

                        Collections.shuffle(candidates);
                        
                        return firestore.collection("events").document(eventId).collection("waitlist")
                                .whereIn("status", List.of(WAITLIST_STATUS_CHOSEN, WAITLIST_STATUS_CONFIRMED, WAITLIST_STATUS_SNOOZED))
                                .get().continueWithTask(chosenTask -> {
                                    int alreadyChosenCount = chosenTask.getResult().size();
                                    int spotsAvailable = finalMax - alreadyChosenCount;
                                    int drawCount = Math.min(candidates.size(), Math.max(0, spotsAvailable));

                                    if (drawCount <= 0) return Tasks.forResult(null);

                                    List<String> chosenUids = new ArrayList<>();
                                    WriteBatch batch = firestore.batch();
                                    for (int i = 0; i < drawCount; i++) {
                                        String uid = candidates.get(i).getId();
                                        chosenUids.add(uid);
                                        DocumentReference eRef = eventWaitlistEntry(eventId, uid);
                                        DocumentReference uRef = userWaitlistEntry(uid, eventId);
                                        
                                        batch.update(eRef, "status", WAITLIST_STATUS_CHOSEN);
                                        batch.update(eRef, "chosenAt", FieldValue.serverTimestamp());
                                        batch.update(uRef, "status", WAITLIST_STATUS_CHOSEN);
                                        batch.update(uRef, "chosenAt", FieldValue.serverTimestamp());
                                    }

                                    return batch.commit().continueWithTask(ignored -> {
                                        NotificationRepository notifRepo = new NotificationRepository();
                                        // Pass the chosenUids directly to avoid re-querying!
                                        return notifRepo.sendToSpecificUsers(eventId, event.getTitle(), winningMessage, "WIN", chosenUids);
                                    });
                                });
                    });
        });
    }

    /**
     * Finds users who haven't responded within 3 days and replaces them.
     */
    public Task<Void> processExpiredWinners(String eventId, String winningMessage) {
        long threeDaysAgo = System.currentTimeMillis() - (3L * 24 * 60 * 60 * 1000);
        Timestamp threshold = new Timestamp(new Date(threeDaysAgo));

        return firestore.collection("events").document(eventId).collection("waitlist")
                .whereIn("status", List.of(WAITLIST_STATUS_CHOSEN, WAITLIST_STATUS_SNOOZED))
                .whereLessThan("chosenAt", threshold)
                .get().continueWithTask(task -> {
                    List<DocumentSnapshot> expired = task.getResult().getDocuments();
                    if (expired.isEmpty()) return performLotteryDraw(eventId, winningMessage);

                    WriteBatch batch = firestore.batch();
                    for (DocumentSnapshot doc : expired) {
                        String uid = doc.getId();
                        batch.update(eventWaitlistEntry(eventId, uid), "status", WAITLIST_STATUS_DECLINED);
                        batch.update(userWaitlistEntry(uid, eventId), "status", WAITLIST_STATUS_DECLINED);
                    }

                    return batch.commit().continueWithTask(ignored -> performLotteryDraw(eventId, winningMessage));
                });
    }

    /**
     * Manages task of updating an Event into the database
     * update the Event document, and posterUri if there is one which also uploads the image file to the database
     * @param eventId
     * ID of the Event to be updated
     * @param currentUser
     * Event Creator (current User) document from database
     * @param event
     * Event object with new data
     * @param posterUri
     * link to the Event picture
     * @return
     * appropriate task to update the database depending on the situation that raises an exception it the task fails
     */
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

    /**
     * Updates the database event document to the new data with the batch commit being a task
     * @param eventRef
     * a reference to a Event document in the database
     * @param userRef
     * a reference to a User document in the database
     * @param draftEvent
     * a Event object that needs to be updated to the database
     * @param hostUid
     * ID of the Event Creator
     * @param hostDisplayName
     * Name of the Event Creator
     * @param posterUrl
     * Link to the event image
     * @param accountType
     * what type of User the User is (Entrant(User), Organizer, Admin)
     * @param posterRef
     * reference to the event Image file in the database
     * @return
     * a task with the result of Event id on success and an exception on failure
     */
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

    /**
     * returns whether an event is able to have sign-ups
     * @param doc
     * a reference to an event document
     * @param now
     * time right now
     * @return
     * whether an event is able to have sign-ups
     */
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

    /**
     * reads in a Event document in the database into an Event object
     * @param doc
     * a reference to Event document in the database
     * @return
     * a created Event object with the data from the document
     */
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
        String winningMessage = doc.getString("winningMessage");

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
                Boolean.TRUE.equals(waitlistOpenValue),
                winningMessage != null ? winningMessage : ""
        );
    }

    /**
     * creates a map matches data with label for database writing
     * @param event
     * a Event object in the program
     * @param hostUid
     * ID of the Event host User (Current user)
     * @param hostDisplayName
     * name of Event host
     * @param posterUrl
     * a link to the Event image
     * @return
     * map of field names and associated data into a format writable to the database
     */
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
        payload.put("winningMessage", event.getWinningMessage());
        return payload;
    }

    /**
     * creates a map matches data with label for database writing
     * @param event
     * a Event object in the program
     * @param posterUrl
     * a link to the Event image
     * @return
     * map of field names and associated data into a format writable to the database
     */
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
        payload.put("winningMessage", event.getWinningMessage());
        if (posterUrl != null) {
            payload.put("posterUrl", posterUrl);
        }
        return payload;
    }

    /**
     * returns string text that designates who the event organizer is
     * @param userSnapshot
     * reference to the current user in the database
     * @param currentUser
     * current User document from database
     * @return
     * string name (or other refence label if name is missing) of the Event organizer (User)
     */
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

    /**
     * reads in a user from the database into a UserProfile
     * @param doc
     * a reference to the user document to read data of
     * @return
     * the User profile of matching the document in the database
     */
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

    /**
     * gets the event ID of the event that the snapshot is contained in
     * @param snapshot
     * reference to a waitlist entry in an Event document
     * @return
     * string event ID of that the waitlist entry is for
     */
    @NonNull
    private String extractWaitlistEventId(@NonNull DocumentSnapshot snapshot) {
        DocumentReference eventRef = snapshot.getReference().getParent().getParent();
        if (eventRef == null) {
            return "";
        }
        return eventRef.getId();
    }

    /**
     * returns the waitlist entry of a event for a specific user from the database
     * @param eventId
     * Event ID
     * @param uid
     * User ID
     * @return
     * the pointer to the waitlist item in the database (under events collection)
     */
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

    /**
     * returns the waitlist entry of a user for a specific event from the database
     * @param uid
     * User ID
     * @param eventId
     * event ID
     * @return
     * the pointer to the waitlist item in the database (under users collection)
     */
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

    /**
     * sorts a list of events by the time of occurrence of the Event
     * @param events
     * list of events to be sorted
     */
    private void sortByEventDate(@NonNull List<EventItem> events) {
        Collections.sort(events, Comparator.comparing(
                EventItem::getEventDate,
                Comparator.nullsLast(Date::compareTo)
        ));
    }

    /**
     * returns whether the (is waitlist open and it is before the deadline)
     * @param waitlistOpen
     * whether the wailist is allowing sign-ups
     * @param registrationDeadline
     * deadline of when sign-ups close
     * @param now
     * time right now
     * @return
     * if is is okay to sign-up for the event
     */
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

    /**
     * return a number 1 more than the argument
     * @param currentCount
     * int to be incremented
     * @return
     * int currentCount incremented by 1
     */
    static int incrementWaitlistCount(int currentCount) {
        return currentCount + 1;
    }

    /**
     * return a number 1 less than the argument unless negative then 0
     * @param currentCount
     * int to be decremented
     * @return
     * int currentCount after decrementing by 1
     */
    static int decrementWaitlistCount(int currentCount) {
        return Math.max(0, currentCount - 1);
    }

    /**
     * removes extraneous whitespace and ensures that sting is non-null
     * @param value
     * String to be cleaned
     * @return
     * cleaned String
     */
    @NonNull
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * returns the first non-empty string in the arguments
     * @param candidates
     * string varable(s) to be testing
     * @return
     * first string that is not empty, or empty string if all are empty
     */
    @NonNull
    private String firstNonEmpty(String... candidates) {
        for (String candidate : candidates) {
            if (hasText(candidate)) {
                return candidate.trim();
            }
        }
        return "";
    }

    /**
     * method that check is there is content in the string
     * @param value
     * the string to be testing
     * @return
     * true if there was actual text in the string
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
