package com.example.eventlotterysystem;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles user-to-user messaging backed by Firebase Realtime Database.
 */
public class EventMessageManager {
    private static final String THREADS = "messageThreads";
    private static final String USER_THREADS = "userThreads";

    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public EventMessageManager() {
    }

    @NonNull
    public static String buildThreadId(@NonNull String firstUid, @NonNull String secondUid) {
        String first = firstUid.trim();
        String second = secondUid.trim();
        if (first.compareTo(second) <= 0) {
            return first + "_" + second;
        }
        return second + "_" + first;
    }

    @NonNull
    public Task<MessageThreadItem> buildThread(
            @NonNull FirebaseUser currentUser,
            @NonNull String otherUid,
            String otherName
    ) {
        String currentUid = currentUser.getUid().trim();
        return getUserName(currentUid)
                .continueWithTask(currentNameTask -> getUserName(otherUid)
                        .continueWith(otherNameTask -> {
                            String finalCurrentName = "";
                            if (currentNameTask.isSuccessful() && currentNameTask.getResult() != null) {
                                finalCurrentName = currentNameTask.getResult();
                            }

                            String finalOtherName = "";
                            if (otherNameTask.isSuccessful() && otherNameTask.getResult() != null) {
                                finalOtherName = otherNameTask.getResult();
                            }

                            String threadId = buildThreadId(currentUid, otherUid);
                            if (currentUid.compareTo(otherUid) <= 0) {
                                return new MessageThreadItem(
                                        threadId,
                                        currentUid,
                                        finalCurrentName,
                                        otherUid,
                                        finalOtherName,
                                        "",
                                        0L,
                                        ""
                                );
                            }

                            return new MessageThreadItem(
                                    threadId,
                                    otherUid,
                                    finalOtherName,
                                    currentUid,
                                    finalCurrentName,
                                    "",
                                    0L,
                                    ""
                            );
                        }));
    }

    @NonNull
    public Task<MessageThreadItem> getThread(@NonNull String threadId) {
        return database.getReference()
                .child(THREADS)
                .child(threadId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("Failed to load message thread");
                    }

                    DataSnapshot snapshot = task.getResult();
                    if (snapshot == null || !snapshot.exists()) {
                        throw new IllegalStateException("Message thread not found");
                    }

                    MessageThreadItem item = snapshot.getValue(MessageThreadItem.class);
                    if (item == null || item.getThreadId() == null || item.getThreadId().trim().isEmpty()) {
                        throw new IllegalStateException("Message thread not found");
                    }

                    return refreshThreadNames(item);
                });
    }

    @NonNull
    public Task<Void> sendMessage(
            @NonNull MessageThreadItem thread,
            @NonNull FirebaseUser currentUser,
            @NonNull String text
    ) {
        String trimmedText = text.trim();
        if (trimmedText.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Message cannot be empty"));
        }

        String currentUid = currentUser.getUid();
        if (!currentUid.equals(thread.getUserOneUid()) && !currentUid.equals(thread.getUserTwoUid())) {
            return Tasks.forException(new IllegalStateException("User cannot access this message thread"));
        }

        return refreshThreadNames(thread).continueWithTask(refreshTask -> {
            MessageThreadItem currentThread = refreshTask.getResult();

            String senderName = currentUid.equals(currentThread.getUserOneUid())
                    ? currentThread.getUserOneName()
                    : currentThread.getUserTwoName();
            if (senderName == null || senderName.trim().isEmpty()) {
                senderName = "";
            }

            long sentAt = System.currentTimeMillis();
            DatabaseReference messageReference = database.getReference()
                    .child(THREADS)
                    .child(currentThread.getThreadId())
                    .child("messages")
                    .push();
            String messageId = messageReference.getKey();
            if (messageId == null || messageId.trim().isEmpty()) {
                throw new IllegalStateException("Failed to allocate message ID");
            }

            ChatMessageItem message = new ChatMessageItem(
                    messageId.trim(),
                    currentUid,
                    senderName,
                    trimmedText,
                    sentAt
            );

            currentThread.setLastMessageText(trimmedText);
            currentThread.setLastMessageAt(sentAt);
            currentThread.setLastSenderUid(currentUid);

            Task<Void> saveThread = saveThreadInfo(currentThread);
            Task<Void> saveMessage = messageReference.setValue(message);
            return Tasks.whenAll(saveThread, saveMessage);
        });
    }

    @NonNull
    public List<MessageThreadItem> readThreads(@NonNull DataSnapshot snapshot) {
        List<MessageThreadItem> threads = new ArrayList<>();
        for (DataSnapshot child : snapshot.getChildren()) {
            MessageThreadItem item = child.getValue(MessageThreadItem.class);
            if (item != null && item.getThreadId() != null && !item.getThreadId().trim().isEmpty()) {
                threads.add(item);
            }
        }
        threads.sort((left, right) -> Long.compare(right.getLastMessageAt(), left.getLastMessageAt()));
        return threads;
    }

    @NonNull
    public List<ChatMessageItem> readMessages(@NonNull DataSnapshot snapshot) {
        List<ChatMessageItem> messages = new ArrayList<>();
        for (DataSnapshot child : snapshot.getChildren()) {
            ChatMessageItem item = child.getValue(ChatMessageItem.class);
            if (item != null && item.getMessageId() != null && !item.getMessageId().trim().isEmpty()) {
                messages.add(item);
            }
        }
        messages.sort(Comparator.comparingLong(ChatMessageItem::getSentAt));
        return messages;
    }
    @NonNull
    private Task<String> getUserName(@NonNull String uid) {
        return firestore.collection("users")
                .document(uid)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if (snapshot != null && snapshot.exists()) {
                            String fullName = snapshot.getString("fullName");
                            if (fullName != null && !fullName.trim().isEmpty()) {
                                return fullName.trim();
                            }
                        }
                    }
                    return "";
                });
    }

    @NonNull
    private Task<MessageThreadItem> refreshThreadNames(@NonNull MessageThreadItem thread) {
        return getUserName(thread.getUserOneUid())
                .continueWithTask(firstNameTask -> getUserName(thread.getUserTwoUid())
                        .continueWithTask(secondNameTask -> {
                            String firstName = "";
                            if (firstNameTask.isSuccessful() && firstNameTask.getResult() != null) {
                                firstName = firstNameTask.getResult();
                            }

                            String secondName = "";
                            if (secondNameTask.isSuccessful() && secondNameTask.getResult() != null) {
                                secondName = secondNameTask.getResult();
                            }

                            thread.setUserOneName(firstName);
                            thread.setUserTwoName(secondName);

                            return saveThreadInfo(thread).continueWith(saveTask -> thread);
                        }));
    }

    @NonNull
    private Task<Void> saveThreadInfo(@NonNull MessageThreadItem thread) {
        DatabaseReference threadReference = database.getReference()
                .child(THREADS)
                .child(thread.getThreadId());
        Map<String, Object> threadValues = new HashMap<>();
        threadValues.put("threadId", thread.getThreadId());
        threadValues.put("userOneUid", thread.getUserOneUid());
        threadValues.put("userOneName", thread.getUserOneName());
        threadValues.put("userTwoUid", thread.getUserTwoUid());
        threadValues.put("userTwoName", thread.getUserTwoName());
        threadValues.put("lastMessageText", thread.getLastMessageText());
        threadValues.put("lastMessageAt", thread.getLastMessageAt());
        threadValues.put("lastSenderUid", thread.getLastSenderUid());

        Task<Void> saveThread = threadReference.updateChildren(threadValues);
        Task<Void> saveFirstUserThread = database.getReference()
                .child(USER_THREADS)
                .child(thread.getUserOneUid())
                .child(thread.getThreadId())
                .setValue(thread);
        Task<Void> saveSecondUserThread = database.getReference()
                .child(USER_THREADS)
                .child(thread.getUserTwoUid())
                .child(thread.getThreadId())
                .setValue(thread);

        return Tasks.whenAll(saveThread, saveFirstUserThread, saveSecondUserThread);
    }
}
