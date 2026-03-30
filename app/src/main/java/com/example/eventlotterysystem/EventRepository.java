package com.example.eventlotterysystem;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
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
import java.util.LinkedHashMap;
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
     * Load the a list event from the database for all events creator matching user ID
     * and all events that the user coorganizes and creates a respective event object list
     * @param hostUid
     * ID of the user that manages the event
     * @return
     * task that gets all the events that the user manages as a list of event objects raises an exception of failure
     */
    public Task<List<EventItem>> getHostedEvents(@NonNull String hostUid) {
        Task<List<EventItem>> hostedTask = loadManagedEvents(
                firestore.collection("events").whereEqualTo("hostUid", hostUid),
                "Failed to load hosted events"
        );
        Task<List<EventItem>> coorganizersTask = loadManagedEvents(
                firestore.collection("events").whereArrayContains("coOrganizerUids", hostUid),
                "Failed to load co-organized events"
        );

        return Tasks.whenAllSuccess(hostedTask, coorganizersTask)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("Failed to load hosted events");
                    }

                    @SuppressWarnings("unchecked")
                    List<EventItem> hostedEvents = (List<EventItem>) task.getResult().get(0);
                    @SuppressWarnings("unchecked")
                    List<EventItem> coorganizedEvents = (List<EventItem>) task.getResult().get(1);
                    return mergeManagedEvents(hostedEvents, coorganizedEvents);
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
     * Check to see if there is a waitlist entry for a user signed up to an event and what that status is
     * @param eventId
     * Id of the Event
     * @param uid
     * Id of the user
     * @return
     * Task that tries to obtains a waitlist entry matching the argument id's and returns result if found
     */
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

    /**
     * determines whether a user can access an event detail screen
     * for private events, access is granted to the host and to anyone
     * who already has a waitlist record for the event
     * @param event
     * event being accessed
     * @param eventId
     * id of the event
     * @param uid
     * current user id
     * @return
     * task resolving to whether the user can view the event
     */
    public Task<Boolean> canUserAccessEvent(
            @NonNull EventItem event,
            @NonNull String eventId,
            @Nullable String uid
    ) {
        if (!hasText(uid)) {
            return Tasks.forResult(canUserAccessEvent(event.isPublic(), false, false));
        }

        boolean canManage = canManageEvent(event, uid);
        if (canUserAccessEvent(event.isPublic(), canManage, false)) {
            return Tasks.forResult(true);
        }

        return eventWaitlistEntry(eventId, uid)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("Failed to verify event access");
                    }
                    DocumentSnapshot doc = task.getResult();
                    boolean hasWaitlistEntry = doc != null && doc.exists();
                    return canUserAccessEvent(event.isPublic(), canManage, hasWaitlistEntry);
                });
    }

    /**
     * assigns a user as a coorganizer for an Event and removes any waitlist entry they had for it
     * @param eventId
     * the String id of the Event
     * @param targetUid
     * the String uid of the user being assigned
     * @param actingUid
     * the String uid of the host assigning the coorganizer
     * @return
     * a Task that updates the Event and removes matching waitlist entries
     */
    public Task<Void> assignCoorganizer(
            @NonNull String eventId,
            @NonNull String targetUid,
            @NonNull String actingUid
    ) {
        if (!hasText(eventId) || !hasText(targetUid) || !hasText(actingUid)) {
            return Tasks.forException(new IllegalArgumentException("Missing co-organizer assignment details"));
        }

        DocumentReference eventRef = firestore.collection("events").document(eventId);
        DocumentReference eventWaitlistRef = eventWaitlistEntry(eventId, targetUid);
        DocumentReference userWaitlistRef = userWaitlistEntry(targetUid, eventId);

        return firestore.runTransaction(transaction -> {
            DocumentSnapshot eventDoc = transaction.get(eventRef);
            if (!eventDoc.exists() || Boolean.TRUE.equals(eventDoc.getBoolean("deleted"))) {
                throw new IllegalStateException("Event not found");
            }

            EventItem event = readEventItem(eventDoc);
            if (!isHost(event, actingUid)) {
                throw new IllegalStateException("Only the host can assign co-organizers");
            }
            if (isHost(event, targetUid)) {
                return null;
            }

            List<String> coorganizers = new ArrayList<>(event.getCoorganizers());
            if (!coorganizers.contains(targetUid)) {
                coorganizers.add(targetUid);
            }

            DocumentSnapshot eventMembershipDoc = transaction.get(eventWaitlistRef);
            DocumentSnapshot userMembershipDoc = transaction.get(userWaitlistRef);
            if (eventMembershipDoc.exists()) {
                transaction.delete(eventWaitlistRef);
            }
            if (userMembershipDoc.exists()) {
                transaction.delete(userWaitlistRef);
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("coOrganizerUids", normalizeCoorganizers(coorganizers));
            if (eventMembershipDoc.exists() || userMembershipDoc.exists()) {
                updates.put("totalEntrants", decrementWaitlistCount(event.getTotalEntrants()));
            }
            transaction.update(eventRef, updates);
            return null;
        });
    }

    /**
     * updates a waitlist entry object for a user signed up for an event status' into a new one.
     * @param eventId
     * Id of the Event
     * @param uid
     * Id of the user
     * @param status
     * new status to update the User to
     * @return
     * a Task updates the change in the database
     */
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

        return eventRef.get().continueWithTask(eventTask -> {
            DocumentSnapshot eventDoc = eventTask.getResult();
            if(!eventDoc.exists()){
                throw new IllegalStateException("Event not found");
            }

            Context context  = com.google.firebase.FirebaseApp.getInstance().getApplicationContext();
            boolean hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;
            boolean geoLocationRequired = Boolean.TRUE.equals(eventDoc.getBoolean("requiresGeolocation"));

            if(geoLocationRequired){
                if(hasPermission){
                    FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
                    return fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).
                            continueWithTask(locationTask -> {
                                Location location = locationTask.isSuccessful()? locationTask.getResult() : null;
                                return executeTransaction(eventId, currentUser, location, eventRef, eventWaitlistRef, userWaitlistRef);
                            });
                } else {
                    return Tasks.forException(new SecurityException("Location Permission not Granted"));
                }
            } else {
                return executeTransaction(eventId, currentUser, null, eventRef, eventWaitlistRef, userWaitlistRef);
            }
        });
    }

    /**
     * Helper function of joinWaitlist that execute the transaction to database
     * @param eventId
     *      ID of event that was signed up
     * @param currentUser
     *      Current user document from database
     * @param location
     *      The Geolocation of user when signed up
     * @param eventRef
     *      A reference to the event document in database
     * @param eventWaitlistRef
     *      A reference to the event waitlist document in database
     * @param userWaitlistRef
     *      A reference to the user waitlist document in database
     * @return
     *      A task that represent the asynchronous transaction to database
     */
    public Task<Void> executeTransaction(String eventId, FirebaseUser currentUser, Location location, DocumentReference eventRef, DocumentReference eventWaitlistRef, DocumentReference userWaitlistRef){
        return firestore.runTransaction(transaction -> {
            DocumentSnapshot eventDoc = transaction.get(eventRef);
            DocumentSnapshot eventMembershipDoc = transaction.get(eventWaitlistRef);
            DocumentSnapshot userMembershipDoc = transaction.get(userWaitlistRef);

            if (eventMembershipDoc.exists() || userMembershipDoc.exists()) {
                return null;
            }

            if (Boolean.TRUE.equals(eventDoc.getBoolean("deleted"))) {
                throw new IllegalStateException("Event not found");
            }

            String hostUid = normalize(eventDoc.getString("hostUid"));
            List<String> coorganizers = normalizeCoorganizers(eventDoc.get("coOrganizerUids"));
            if (hostUid.equals(currentUser.getUid()) || coorganizers.contains(currentUser.getUid())) {
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
            if(location != null){
                GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                membershipPayload.put("location", geoPoint);
            }

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

    /**
     * returns the amount of entrants for a given status
     * @param eventId
     * Id of the event
     * @param statusFilter
     * the string name of status (ie chosen)
     * @return
     * a task the queries the data base for waitlist entries matching the status and counts them
     */
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
     * gets all confirmed Entrants for a given Event
     * @param eventId
     * the String id of the Event
     * @return
     * a Task containing the list of confirmed UserProfiles for the Event
     */
    public Task<List<UserProfile>> getConfirmedEntrantsForEvent(@NonNull String eventId) {
        return getEntrantsForEvent(eventId, WAITLIST_STATUS_CONFIRMED);
    }

    /**
     * Performs lottery draw by moving random users from IN_WAITLIST to CHOSEN.
     * Selects up to maxParticipants winners from the pool of users who are IN_WAITLIST.
     * Updates status in both event and user records and notifies the selected users
     * with WINNING notification.
     *
     * @param eventId
     * ID of the event to perform the draw for
     * @param winningMessage
     * message to be sent to users selected in the lottery
     * @return
     *Task that completes when the draw and notifications are finished
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
                                    int drawCount = calculateDrawCount(candidates.size(), finalMax, alreadyChosenCount);

                                    if (drawCount <= 0) return Tasks.forResult(null);

                                    List<String> chosenUids = new ArrayList<>();
                                    List<String> notChosenUids = new ArrayList<>();
                                    WriteBatch batch = firestore.batch();
                                    
                                    for (int i = 0; i < candidates.size(); i++) {
                                        String uid = candidates.get(i).getId();
                                        if (i < drawCount) {
                                            chosenUids.add(uid);
                                            DocumentReference eRef = eventWaitlistEntry(eventId, uid);
                                            DocumentReference uRef = userWaitlistEntry(uid, eventId);
                                            
                                            batch.update(eRef, "status", WAITLIST_STATUS_CHOSEN);
                                            batch.update(eRef, "chosenAt", FieldValue.serverTimestamp());
                                            batch.update(uRef, "status", WAITLIST_STATUS_CHOSEN);
                                            batch.update(uRef, "chosenAt", FieldValue.serverTimestamp());
                                        } else {
                                            notChosenUids.add(uid);
                                        }
                                    }

                                    // set waitlistOpen to false in the event doc when a draw is performed
                                    batch.update(firestore.collection("events").document(eventId), "waitlistOpen", false);

                                    return batch.commit().continueWithTask(ignored -> {
                                        NotificationRepository notifRepo = new NotificationRepository();
                                        List<Task<Void>> tasks = new ArrayList<>();
                                        
                                        // Send Winning Notifications
                                        if (!chosenUids.isEmpty()) {
                                            tasks.add(notifRepo.sendToSpecificUsers(eventId, event.getTitle(), winningMessage, "WIN", chosenUids));
                                        }
                                        
                                        // Send Loser/Waiting Notifications to the rest
                                        if (!notChosenUids.isEmpty()) {
                                            String loseMessage = "The draw for " + event.getTitle() + " has been performed. " +
                                                    "You were not selected this time, but keep an eye on your inbox! " +
                                                    "If someone declines their spot, it will be automatically redrawn.";
                                            tasks.add(notifRepo.sendToSpecificUsers(eventId, event.getTitle(), loseMessage, "GENERAL", notChosenUids));
                                        }
                                        
                                        return Tasks.whenAll(tasks);
                                    });
                                });
                    });
        });
    }

    /**
     * calculates how many users should be drawn from the waitlist pool.
     * Generalized helper function to aid with testing
     *
     * @param candidatesSize
     * number of users currently in the waitlist
     * @param maxParticipants
     * maximum allowed winners for the event
     * @param alreadyChosenCount
     * number of users already in a winning or confirmed state
     * @return The number of slots to fill in this draw
     */
    public static int calculateDrawCount(int candidatesSize, int maxParticipants, int alreadyChosenCount) {
        int spotsAvailable = maxParticipants - alreadyChosenCount;
        return Math.min(candidatesSize, Math.max(0, spotsAvailable));
    }

    /**
     * Finds users who haven't responded within 3 days and replaces them.
     * Triggers a redraw to fill vacated spots.
     * @param eventId
     * The ID of the event to process expired winners from
     * @param winningMessage
     * The message to be sent to users selected in the lottery.
     * @return
     * task that completes after expired winners are processed and any replacement draws finish
     */
    public Task<Void> processExpiredWinners(String eventId, String winningMessage) {
        return processExpiredWinners(eventId, winningMessage, System.currentTimeMillis());
    }

    /**
     * Helper method for processExpiredWinners that allows for a custom current time for testing purposes.
     * @param eventId
     * The ID of the event to process expired winners from
     * @param winningMessage
     * The message to be sent to users selected in the lottery.
     * @param currentTimeMillis
     * The current time in milliseconds.
     * @return
     * task that completes after expired winners are processed and any replacement draws finish
     */
    public Task<Void> processExpiredWinners(String eventId, String winningMessage, long currentTimeMillis) {
        long threeDaysAgo = currentTimeMillis - (3L * 24 * 60 * 60 * 1000);
        Timestamp threshold = new Timestamp(new Date(threeDaysAgo));

        return firestore.collection("events").document(eventId).collection("waitlist")
                .whereIn("status", List.of(WAITLIST_STATUS_CHOSEN, WAITLIST_STATUS_SNOOZED))
                .get().continueWithTask(task -> {
                    List<DocumentSnapshot> allPending = task.getResult().getDocuments();
                    List<String> toRemove = new ArrayList<>();
                    
                    for (DocumentSnapshot doc : allPending) {
                        Timestamp chosenAt = doc.getTimestamp("chosenAt");
                        if (chosenAt != null && chosenAt.compareTo(threshold) < 0) {
                            toRemove.add(doc.getId());
                        }
                    }
                    
                    if (toRemove.isEmpty()) return performLotteryDraw(eventId, winningMessage);

                    WriteBatch batch = firestore.batch();
                    for (String uid : toRemove) {
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
        return eventRef.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException() != null
                        ? task.getException()
                        : new IllegalStateException("Failed to load event");
            }

            DocumentSnapshot eventDoc = task.getResult();
            if (eventDoc == null || !eventDoc.exists()) {
                throw new IllegalStateException("Event not found");
            }

            if (!canManageEvent(readEventItem(eventDoc), currentUser.getUid())) {
                throw new IllegalStateException("You do not have permission to edit this event");
            }

            if (posterUri == null) {
                return eventRef.update(buildUpdatedEventPayload(event, null))
                        .continueWith(updateTask -> {
                            if (!updateTask.isSuccessful()) {
                                throw updateTask.getException() != null
                                        ? updateTask.getException()
                                        : new IllegalStateException("Failed to update event");
                            }
                            return eventId;
                        });
            }

            StorageReference posterRef = storage.getReference()
                    .child("event-posters/" + currentUser.getUid() + "/" + eventId + ".jpg");

            return posterRef.putFile(posterUri)
                    .continueWithTask(uploadTask -> {
                        if (!uploadTask.isSuccessful()) {
                            throw uploadTask.getException() != null
                                    ? uploadTask.getException()
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
        Timestamp eventDateTimestamp = doc.getTimestamp("eventDate");
        Timestamp registrationDeadlineTimestamp = doc.getTimestamp("registrationDeadline");
        return isCurrentEventVisible(
                Boolean.TRUE.equals(doc.getBoolean("waitlistOpen")),
                Boolean.TRUE.equals(doc.getBoolean("deleted")),
                normalizeIsPublic(doc.getBoolean("isPublic")),
                eventDateTimestamp == null ? null : eventDateTimestamp.toDate(),
                registrationDeadlineTimestamp == null ? null : registrationDeadlineTimestamp.toDate(),
                now
        );
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
        List<String> coorganizers = normalizeCoorganizers(doc.get("coOrganizerUids"));
        List<String> keywords = normalizeKeywords(doc.get("keywords"));
        boolean isPublic = normalizeIsPublic(doc.getBoolean("isPublic"));

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
                coorganizers,
                Boolean.TRUE.equals(waitlistOpenValue),
                winningMessage != null ? winningMessage : "",
                keywords,
                isPublic
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
        payload.put("coOrganizerUids", normalizeCoorganizers(event.getCoorganizers()));
        payload.put("waitlistOpen", true);
        payload.put("keywords", normalizeKeywords(event.getKeywords()));
        payload.put("isPublic", event.isPublic());
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
        payload.put("coOrganizerUids", normalizeCoorganizers(event.getCoorganizers()));
        payload.put("keywords", normalizeKeywords(event.getKeywords()));
        payload.put("isPublic", event.isPublic());
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
     * loads a list of Events from the database for a query used in managed Event screens
     * @param query
     * the firestore query used to load the Events
     * @param failureMessage
     * the error message used if loading fails
     * @return
     * a Task containing the list of loaded Event objects
     */
    private Task<List<EventItem>> loadManagedEvents(
            @NonNull Query query,
            @NonNull String failureMessage
    ) {
        return query.get().continueWith(task -> {
            if (!task.isSuccessful()) {
                throw task.getException() != null
                        ? task.getException()
                        : new IllegalStateException(failureMessage);
            }

            List<EventItem> results = new ArrayList<>();
            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                if (Boolean.TRUE.equals(doc.getBoolean("deleted"))) {
                    continue;
                }
                results.add(readEventItem(doc));
            }
            return results;
        });
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
     * whether the waitlist is allowing sign-ups
     * @param registrationDeadline
     * deadline of when sign-ups close
     * @param now
     * time right now
     * @return
     * if is is okay to sign-up for the event
     */
    public static boolean isWaitlistJoinOpen(
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
     * returns whether an event should be visible on the current events screen
     * @param waitlistOpen
     * whether the waitlist is allowing sign-ups
     * @param deleted
     * whether the event is deleted
     * @param isPublic
     * whether the event is public
     * @param eventDate
     * when the event occurs
     * @param registrationDeadline
     * when sign-ups close
     * @param now
     * current time
     * @return
     * whether the event belongs on the current public events list
     */
    public static boolean isCurrentEventVisible(
            boolean waitlistOpen,
            boolean deleted,
            boolean isPublic,
            @Nullable Date eventDate,
            @Nullable Date registrationDeadline,
            @NonNull Date now
    ) {
        if (deleted || !isPublic || !waitlistOpen) {
            return false;
        }

        boolean upcomingByEventDate = eventDate != null && eventDate.after(now);
        boolean beforeDeadline = isWaitlistJoinOpen(waitlistOpen, registrationDeadline, now);
        return upcomingByEventDate || beforeDeadline;
    }

    /**
     * determines event access for the current user
     * @param isPublic
     * whether the event is public
     * @param canManage
     * whether the current user can manage the event
     * @param hasWaitlistEntry
     * whether the current user already has an event waitlist record
     * @return
     * whether the user can view the event
     */
    public static boolean canUserAccessEvent(
            boolean isPublic,
            boolean canManage,
            boolean hasWaitlistEntry
    ) {
        return isPublic || canManage || hasWaitlistEntry;
    }

    /**
     * checks if the given user is the host of a Event
     * @param event
     * the Event being checked
     * @param uid
     * the String uid of the user
     * @return
     * true if the user is the host of the Event
     */
    public static boolean isHost(@NonNull EventItem event, @Nullable String uid) {
        return hasText(uid) && uid.equals(event.getHostUid());
    }

    /**
     * checks if the given user can manage a Event as the host or a coorganizer
     * @param event
     * the Event being checked
     * @param uid
     * the String uid of the user
     * @return
     * true if the user can manage the Event
     */
    public static boolean canManageEvent(@NonNull EventItem event, @Nullable String uid) {
        if (isHost(event, uid)) {
            return true;
        }
        if (!hasText(uid)) {
            return false;
        }
        return event.getCoorganizers().contains(uid);
    }

    /**
     * merges hosted Events and coorganized Events into one list without duplicates
     * @param hostedEvents
     * the list of Events hosted by the user
     * @param coorganizedEvents
     * the list of Events coorganized by the user
     * @return
     * a sorted list of managed Events without duplicates
     */
    @NonNull
    public static List<EventItem> mergeManagedEvents(
            @NonNull List<EventItem> hostedEvents,
            @NonNull List<EventItem> coorganizedEvents
    ) {
        Map<String, EventItem> merged = new LinkedHashMap<>();
        for (EventItem event : hostedEvents) {
            merged.put(event.getId(), event);
        }
        for (EventItem event : coorganizedEvents) {
            merged.put(event.getId(), event);
        }

        List<EventItem> results = new ArrayList<>(merged.values());
        Collections.sort(results, Comparator.comparing(
                EventItem::getEventDate,
                Comparator.nullsLast(Date::compareTo)
        ));
        return results;
    }

    /**
     * return a number 1 more than the argument
     * @param currentCount
     * int to be incremented
     * @return
     * int currentCount incremented by 1
     */
    public static int incrementWaitlistCount(int currentCount) {
        return currentCount + 1;
    }

    /**
     * return a number 1 less than the argument unless negative then 0
     * @param currentCount
     * int to be decremented
     * @return
     * int currentCount after decrementing by 1
     */
    public static int decrementWaitlistCount(int currentCount) {
        return Math.max(0, currentCount - 1);
    }

    /**
     * normalizes keywords read from user input or firestore
     * @param rawKeywords
     * raw keyword list
     * @return
     * trimmed keyword list without duplicates or blanks
     */
    @NonNull
    public static List<String> normalizeKeywords(@Nullable Object rawKeywords) {
        if (!(rawKeywords instanceof List<?>)) {
            return new ArrayList<>();
        }

        List<String> normalized = new ArrayList<>();
        for (Object rawKeyword : (List<?>) rawKeywords) {
            if (!(rawKeyword instanceof String)) {
                continue;
            }
            String keyword = ((String) rawKeyword).trim();
            if (keyword.isEmpty() || containsKeywordIgnoreCase(normalized, keyword)) {
                continue;
            }
            normalized.add(keyword);
        }
        return normalized;
    }

    /**
     * normalizes coorganizer user ids read from user input or firestore
     * @param rawCoorganizers
     * raw coorganizer user id list
     * @return
     * trimmed coorganizer user ids without duplicates or blanks
     */
    @NonNull
    public static List<String> normalizeCoorganizers(@Nullable Object rawCoorganizers) {
        if (!(rawCoorganizers instanceof List<?>)) {
            return new ArrayList<>();
        }

        List<String> normalized = new ArrayList<>();
        for (Object rawUid : (List<?>) rawCoorganizers) {
            if (!(rawUid instanceof String)) {
                continue;
            }
            String uid = normalize((String) rawUid);
            if (uid.isEmpty() || normalized.contains(uid)) {
                continue;
            }
            normalized.add(uid);
        }
        return normalized;
    }

    /**
     * normalizes the stored public flag for legacy events without the field
     * @param isPublicValue
     * stored public flag
     * @return
     * true when the field is missing, otherwise the stored value
     */
    public static boolean normalizeIsPublic(@Nullable Boolean isPublicValue) {
        return isPublicValue == null || Boolean.TRUE.equals(isPublicValue);
    }

    private static boolean containsKeywordIgnoreCase(@NonNull List<String> keywords, @NonNull String candidate) {
        for (String keyword : keywords) {
            if (keyword.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * removes extraneous whitespace and ensures that string is non-null
     * @param value
     * String to be cleaned
     * @return
     * cleaned String
     */
    @NonNull
    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * returns the first non-empty string in the arguments
     * @param candidates
     * string variable(s) to be testing
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
    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Adds a new comment to the specified event.
     *
     * @param eventId the ID of the event receiving the comment
     * @param currentUser the signed-in user posting the comment
     * @param text the comment text to post
     * @return a task representing the result of the comment write operation
     */

    public Task<Void> addComment(
            @NonNull String eventId,
            @NonNull FirebaseUser currentUser,
            @NonNull String text
    ) {
        String trimmedText = text.trim();
        if (trimmedText.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Comment cannot be empty"));
        }

        DocumentReference commentRef = firestore.collection("events")
                .document(eventId)
                .collection("comments")
                .document();

        return getCommentDisplayName(currentUser)
                .continueWithTask(task -> {
                    String displayName = task.isSuccessful() ? normalize(task.getResult()) : "";

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("uid", currentUser.getUid());
                    payload.put("text", trimmedText);
                    payload.put("username", displayName);
                    payload.put("createdAt", FieldValue.serverTimestamp());
                    return commentRef.set(payload);
                });
    }

    /**
     * Resolves the name shown for a comment author.
     *
     * @param currentUser the signed-in user posting the comment
     * @return a task resolving to the preferred display name for comments
     */
    private Task<String> getCommentDisplayName(@NonNull FirebaseUser currentUser) {
        return firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        return "";
                    }

                    DocumentSnapshot userDoc = task.getResult();
                    if (userDoc == null || !userDoc.exists()) {
                        return "";
                    }

                    return normalize(userDoc.getString("fullName"));
                });
    }

    /**
     * Deletes a comment from an event.
     *
     * @param eventId the ID of the event containing the comment
     * @param commentId the ID of the comment to delete
     * @return a task representing the delete operation
     */
    public Task<Void> deleteComment(
            @NonNull String eventId,
            @NonNull String commentId
    ) {
        return firestore.collection("events")
                .document(eventId)
                .collection("comments")
                .document(commentId)
                .delete();
    }

    /**
     * Loads all comments for the specified event.
     *
     * Comments are read from the event's comments subcollection and returned as
     * {@link CommentItem} objects sorted by creation time.
     *
     * @param eventId the ID of the event whose comments should be loaded
     * @return a task containing the list of comments for the event
     */

    public Task<List<CommentItem>> getComments(@NonNull String eventId) {
        return firestore.collection("events")
                .document(eventId)
                .collection("comments")
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("Failed to load comments");
                    }

                    List<CommentItem> comments = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        String uid = normalize(doc.getString("uid"));
                        String username = normalize(doc.getString("username"));
                        String text = normalize(doc.getString("text"));
                        Timestamp createdAtTimestamp = doc.getTimestamp("createdAt");

                        comments.add(new CommentItem(
                                doc.getId(),
                                uid,
                                username,
                                text,
                                createdAtTimestamp == null ? null : createdAtTimestamp.toDate()
                        ));
                    }

                    comments.sort(Comparator.comparing(
                            CommentItem::getCreatedAt,
                            Comparator.nullsLast(Date::compareTo)
                    ));

                    return comments;
                });
    }
}
