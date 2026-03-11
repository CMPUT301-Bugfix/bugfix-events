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
import java.util.Date;
import java.util.List;

public class NotificationRepository {

    public interface NotificationsCallback {
        void onSuccess(List<NotificationItem> notifications);
        void onError(Exception e);
    }

    private final FirebaseFirestore firestore;

    public NotificationRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Sends a notification to all users on the waitlist for a specific event.
     * Uses a collection group query to find all users who have this event in their waitlist.
     */
    public Task<Void> sendNotificationToWaitlist(
            @NonNull String eventId,
            @NonNull String eventTitle,
            @NonNull String message,
            @NonNull String senderUid
    ) {
        // 1. Find all users on the waitlist using a collection group query
        return firestore.collectionGroup("waitlists")
                .whereEqualTo("eventId", eventId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null ? task.getException() : new Exception("Failed to fetch waitlist");
                    }

                    List<String> recipientUids = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult()) {
                        // The path is users/{uid}/waitlists/{eventId}
                        // We can extract the uid from the document reference
                        DocumentReference ref = doc.getReference();
                        DocumentReference userRef = ref.getParent().getParent();
                        if (userRef != null) {
                            recipientUids.add(userRef.getId());
                        }
                    }

                    if (recipientUids.isEmpty()) {
                        return Tasks.forResult(null);
                    }

                    return executeBroadcast(eventId, eventTitle, message, senderUid, recipientUids);
                });
    }

    private Task<Void> executeBroadcast(
            String eventId,
            String eventTitle,
            String message,
            String senderUid,
            List<String> recipientUids
    ) {
        WriteBatch batch = firestore.batch();
        Date now = new Date();

        // 1. Add to global notification log
        DocumentReference logRef = firestore.collection("notifications").document();
        NotificationItem logItem = new NotificationItem(logRef.getId(), eventId, eventTitle, message, senderUid, now);
        batch.set(logRef, logItem);

        // 2. Add to each user's individual inbox
        for (String uid : recipientUids) {
            DocumentReference userNotifyRef = firestore.collection("users")
                    .document(uid)
                    .collection("notifications")
                    .document();
            NotificationItem userItem = new NotificationItem(userNotifyRef.getId(), eventId, eventTitle, message, senderUid, now);
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
