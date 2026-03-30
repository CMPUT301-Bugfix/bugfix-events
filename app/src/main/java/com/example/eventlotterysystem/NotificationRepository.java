package com.example.eventlotterysystem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository class for managing notifications in Firestore.
 * This class handles the creation, retrieval, and status management of notifications,
 * including broadcasting messages to event waitlists.
 */
public class NotificationRepository {

    private final FirebaseFirestore firestore;

    /**
     * Initializes the NotificationRepository with a Firestore instance.
     */
    public NotificationRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Sends a notification to a specific sub-group of an event's waitlist based on their status.
     *
     * @param eventId       ID of the event
     * @param eventTitle   title of the event
     * @param message      message content
     * @param type          type of notification (e.g., "WIN", "GENERAL").
     * @param statusFilter status to filter recipients by (e.g., "IN_WAITLIST", "CHOSEN")
     *                     If null, all users in the waitlist will be notified
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
     * Sends a notification to a specific list of user IDs.
     * Records the notification in the global log and individual user inbox
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

        WriteBatch batch = firestore.batch();

        // 1. Add to global notification log (root notifications collection)
        DocumentReference logRef = firestore.collection("notifications").document();
        NotificationItem logItem = new NotificationItem(eventId, title, message, type);
        logItem.setId(logRef.getId());
        batch.set(logRef, logItem);

        // 2. Add to each user's individual inbox
        for (String uid : recipientUids) {
            DocumentReference userNotifyRef = firestore.collection("users")
                    .document(uid)
                    .collection("notifications")
                    .document();
            NotificationItem userItem = new NotificationItem(eventId, title, message, type);
            userItem.setId(userNotifyRef.getId());
            batch.set(userNotifyRef, userItem);
        }

        return batch.commit();
    }

    /**
     * Retrieves all notifications for a specific user, ordered by timestamp (newest first)
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
     * Updates the status of a specific notification in a user inbox
     *
     * @param uid            UID of the user
     * @param notificationId ID of the notification
     * @param newStatus      new status to set (e.g., "READ", "ACCEPTED")
     * @return A Task representing completion of upddte
     */
    public Task<Void> updateNotificationStatus(@NonNull String uid, @NonNull String notificationId, @NonNull String newStatus) {
        return firestore.collection("users")
                .document(uid)
                .collection("notifications")
                .document(notificationId)
                .update("status", newStatus);
    }

    /**
     * Deletes a specific notification from a user's inbox.
     *
     * @param uid            UID of the user
     * @param notificationId ID of the notif to delete
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
     * reads in a Notification document in the database into an Notification object
     * @param doc
     * a reference to Notification document in the database
     * @return
     * a created Notification object with the data from the document
     */
    @NonNull
    public NotificationItem readNotificationItem(@NonNull DocumentSnapshot doc) {
        String eventId = doc.getString("eventId");
        String title = doc.getString("title");
        String message = doc.getString("message");
        String type = doc.getString("type");

        return new NotificationItem(
                eventId,
                title,
                message,
                type
        );
    }

    /**
     * Retrieves all notifications from the notifications collection, ordered by timestamp (newest first)
     * @return A Task containing a list of NotificationItem objects
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
