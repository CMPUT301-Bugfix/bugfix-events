package com.example.eventlotterysystem;

import androidx.annotation.NonNull;

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
     * Sends a notification to all users on the waitlist for a specific event.
     * Updated to match the events/{eventId}/waitlists/{uid} structure.
     */
    public Task<Void> sendNotificationToWaitlist(
            @NonNull String eventId,
            @NonNull String eventTitle,
            @NonNull String message
    ) {
        // get the waitlist using path:  events/{eventId}/waitlists
        return firestore.collection("events")
                .document(eventId)
                .collection("waitlists") // No longer need collectionGroup
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null ? task.getException() : new Exception("Query error");
                    }

                    List<String> recipientUids = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult()) {
                        //  Document ID is the User ID
                        recipientUids.add(doc.getId());
                    }

                    if (recipientUids.isEmpty()) {
                        return Tasks.forResult(null);
                    }

                    return executeBroadcast(eventId, eventTitle, message, "GENERAL", recipientUids);
                });
    }

    private Task<Void> executeBroadcast(
            String eventId,
            String title,
            String message,
            String type,
            List<String> recipientUids
    ) {
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
}
