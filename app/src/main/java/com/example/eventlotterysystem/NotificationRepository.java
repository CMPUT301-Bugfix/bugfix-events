package com.example.eventlotterysystem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class for managing notifications in Firestore
 * This class handles the creation, retrieval, and status management of notifications,
 * including broadcasting messages to event waitlists and managing private invitations
 */
public class NotificationRepository {

    private final FirebaseFirestore firestore;

    /**
     * Initializes the NotificationRepository with a Firestore instance
     */
    public NotificationRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Sends a notification to a specific sub-group of an event's waitlist based on their status
     *
     * @param eventId       ID of the event
     * @param eventTitle    title of the event
     * @param message       message content
     * @param type          type of notification ("WIN", "GENERAL")
     * @param statusFilter  status to filter recipients by ("IN_WAITLIST", "CHOSEN")
     *                      If null, all users in the waitlist will be notified
     * @return A Task representing the completion of the batch send operation
     */
    public Task<Void> sendBatchNotification(
            @NonNull String eventId,
            @NonNull String eventTitle,
            @NonNull String message,
            @NonNull String type,
            @Nullable String statusFilter
    ) {
        com.google.firebase.firestore.Query query = firestore.collection("events")
                .document(eventId)
                .collection("waitlist");

        if (statusFilter != null) {
            query = query.whereEqualTo("status", statusFilter);
        }

        return query.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException() != null ? task.getException() : new Exception("Query error");
            }

            List<String> recipientUids = new ArrayList<>();
            for (DocumentSnapshot doc : task.getResult()) {
                recipientUids.add(doc.getId());
            }

            if (recipientUids.isEmpty()) {
                return Tasks.forResult(null);
            }

            return sendToSpecificUsers(eventId, eventTitle, message, type, recipientUids);
        });
    }

    /**
     * sends a notification to a specific list of user IDs
     * records the notification in the global log and individual user inbox
     * filters users based on their notification preferences
     *
     * @param eventId       ID of the event associated with the notification
     * @param title         title of the notification
     * @param message       message content
     * @param type          type of notification
     * @param recipientUids A list of user UIDs who should receive the notification
     * @return A Task representing the completion of the write operation
     */
    public Task<Void> sendToSpecificUsers(
            @NonNull String eventId,
            @NonNull String title,
            @NonNull String message,
            @NonNull String type,
            @NonNull List<String> recipientUids
    ) {
        if (recipientUids.isEmpty()) return Tasks.forResult(null);

        List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();
        for (String uid : recipientUids) {
            userTasks.add(firestore.collection("users").document(uid).get());
        }

        return Tasks.whenAllComplete(userTasks).continueWithTask(task -> {
            WriteBatch batch = firestore.batch();
            
            // add to global notification log (always record the attempt)
            DocumentReference logRef = firestore.collection("notifications").document();
            NotificationItem logItem = new NotificationItem(eventId, title, message, type);
            logItem.setId(logRef.getId());
            batch.set(logRef, logItem);

            boolean addedAnyRecipients = false;
            for (Task<DocumentSnapshot> userTask : userTasks) {
                if (userTask.isSuccessful()) {
                    DocumentSnapshot userDoc = userTask.getResult();
                    if (checkPreference(userDoc, type)) {
                        String uid = userDoc.getId();
                        DocumentReference userNotifyRef = firestore.collection("users")
                                .document(uid)
                                .collection("notifications")
                                .document();
                        NotificationItem userItem = new NotificationItem(eventId, title, message, type);
                        userItem.setId(userNotifyRef.getId());
                        batch.set(userNotifyRef, userItem);
                        addedAnyRecipients = true;
                    }
                }
            }

            return batch.commit();
        });
    }

    /**
     * Sends invitations for a private event and creates tracking records in the event's sub-collection
     * filters users based on their private invite opt-in preference
     *
     * @param eventId ID of the event
     * @param title   title of the notification
     * @param message invitation message content
     * @param users   List of UserProfile objects to invite
     * @return A Task representing the completion of the invitation operation
     */
    public Task<Void> sendInvitations(
            @NonNull String eventId,
            @NonNull String title,
            @NonNull String message,
            @NonNull List<UserProfile> users
    ) {
        if (users.isEmpty()) return Tasks.forResult(null);

        List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();
        for (UserProfile user : users) {
            userTasks.add(firestore.collection("users").document(user.getUid()).get());
        }

        return Tasks.whenAllComplete(userTasks).continueWithTask(task -> {
            WriteBatch batch = firestore.batch();
            boolean addedAny = false;

            for (Task<DocumentSnapshot> userTask : userTasks) {
                if (userTask.isSuccessful()) {
                    DocumentSnapshot userDoc = userTask.getResult();
                    if (checkPreference(userDoc, "INVITE")) {
                        String uid = userDoc.getId();
                        
                        // 1. Add to user's inbox
                        DocumentReference userNotifyRef = firestore.collection("users")
                                .document(uid)
                                .collection("notifications")
                                .document();
                        NotificationItem userItem = new NotificationItem(eventId, title, message, "INVITE");
                        userItem.setId(userNotifyRef.getId());
                        batch.set(userNotifyRef, userItem);

                        // 2. Add to event's invitations tracking
                        DocumentReference inviteRef = firestore.collection("events")
                                .document(eventId)
                                .collection("invitations")
                                .document(uid);
                        
                        Map<String, Object> inviteData = new HashMap<>();
                        inviteData.put("uid", uid);
                        inviteData.put("name", userDoc.getString("fullName"));
                        inviteData.put("email", userDoc.getString("email"));
                        inviteData.put("username", userDoc.getString("username"));
                        inviteData.put("status", "PENDING");
                        inviteData.put("timestamp", FieldValue.serverTimestamp());
                        
                        batch.set(inviteRef, inviteData);
                        addedAny = true;
                    }
                }
            }

            return batch.commit();
        });
    }

    /**
     * helper method to check if a user has opted in for a specific notification type
     * defaults to true if the preference field is not found
     *
     * @param userSnapshot Firestore document snapshot of the user
     * @param type         type of notification to check preference for
     * @return true if opted in or preference is unset, false otherwise
     */
    private boolean checkPreference(DocumentSnapshot userSnapshot, String type) {
        if (userSnapshot == null || !userSnapshot.exists()) return false;
        
        String field;
        switch (type) {
            case "WIN": 
                field = "optInWinningNotifications"; 
                break;
            case "INVITE": 
                field = "optInPrivateInvites"; 
                break;
            case "COORGANIZER": 
                field = "optInCoorganizerInvites"; 
                break;
            case "GENERAL": 
                field = "optInOtherNotifications"; 
                break;
            default: 
                return true;
        }
        
        // If the field doesn't exist, default to true
        return !userSnapshot.contains(field) || Boolean.TRUE.equals(userSnapshot.getBoolean(field));
    }

    /**
     * gets all notifications for a specific user, ordered by timestamp (newest first)
     *
     * @param uid UID of the user
     * @return A Task containing a list of NotificationItem objects
     */
    public Task<List<NotificationItem>> getNotificationsForUser(@NonNull String uid) {
        return firestore.collection("users")
                .document(uid)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null ? task.getException() : new Exception("Failed to fetch notifications");
                    }
                    List<NotificationItem> notifications = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult()) {
                        NotificationItem item = doc.toObject(NotificationItem.class);
                        if (item != null) {
                            item.setId(doc.getId());
                            notifications.add(item);
                        }
                    }
                    return notifications;
                });
    }

    /**
     * updates the status of a specific notification in a user's inbox
     *
     * @param uid            UID of the user
     * @param notificationId ID of the notification
     * @param newStatus      new status to set ("READ", "ACCEPTED", "REJECTED")
     * @return A Task representing completion of the update
     */
    public Task<Void> updateNotificationStatus(@NonNull String uid, @NonNull String notificationId, @NonNull String newStatus) {
        return firestore.collection("users")
                .document(uid)
                .collection("notifications")
                .document(notificationId)
                .update("status", newStatus);
    }

    /**
     * Updates the tracking status of an invitation under an event's invitations sub-collection
     *
     * @param eventId   ID of the event
     * @param uid       UID of the user
     * @param newStatus new status to set ("ACCEPTED", "REJECTED")
     * @return A Task representing completion of the update
     */
    public Task<Void> updateInvitationTrackingStatus(@NonNull String eventId, @NonNull String uid, @NonNull String newStatus) {
        return firestore.collection("events")
                .document(eventId)
                .collection("invitations")
                .document(uid)
                .update("status", newStatus);
    }

    /**
     * Deletes a specific notification from a user's inbox
     *
     * @param uid            UID of the user
     * @param notificationId ID of the notification to delete
     * @return A Task representing the complete delete
     */
    public Task<Void> deleteNotification(@NonNull String uid, @NonNull String notificationId) {
        return firestore.collection("users")
                .document(uid)
                .collection("notifications")
                .document(notificationId)
                .delete();
    }

    /**
     * Reads a Notification document from Firestore and converts it to a NotificationItem object
     *
     * @param doc Firestore document snapshot
     * @return A NotificationItem object representing the document data
     */
    @NonNull
    public NotificationItem readNotificationItem(@NonNull DocumentSnapshot doc) {
        String eventId = doc.getString("eventId");
        String title = doc.getString("title");
        String message = doc.getString("message");
        String type = doc.getString("type");

        NotificationItem item = new NotificationItem(
                eventId,
                title,
                message,
                type
        );
        item.setId(doc.getId());
        item.setStatus(doc.getString("status"));
        return item;
    }

    /**
     * Retrieves all notifications from global notifications log, ordered by timestamp (newest first)
     *
     * @return A Task containing a list of all NotificationItem objects in the log
     */
    public Task<List<NotificationItem>> getNotificationLog() {
        return firestore.collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null ? task.getException() : new Exception("Failed to fetch notifications");
                    }
                    List<NotificationItem> notifications = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult()) {
                        NotificationItem item = doc.toObject(NotificationItem.class);
                        if (item != null) {
                            item.setId(doc.getId());
                            notifications.add(item);
                        }
                    }
                    return notifications;
                });
    }

}
