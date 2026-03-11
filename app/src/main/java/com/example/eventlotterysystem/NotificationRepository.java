package com.example.eventlotterysystem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {

    private final FirebaseFirestore firestore;

    public NotificationRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Sends a targeted notification to a specific sub-group of the waitlist.
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
     * Directly sends a notification to a provided list of user UIDs.
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

        // 1. Add to global notification log
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

    public Task<Void> updateNotificationStatus(@NonNull String uid, @NonNull String notificationId, @NonNull String newStatus) {
        return firestore.collection("users")
                .document(uid)
                .collection("notifications")
                .document(notificationId)
                .update("status", newStatus);
    }

    public Task<Void> deleteNotification(@NonNull String uid, @NonNull String notificationId) {
        return firestore.collection("users")
                .document(uid)
                .collection("notifications")
                .document(notificationId)
                .delete();
    }
}
