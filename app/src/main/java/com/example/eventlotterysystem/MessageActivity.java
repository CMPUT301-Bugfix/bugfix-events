package com.example.eventlotterysystem;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a single direct message conversation.
 */
public class MessageActivity extends AppCompatActivity {
    private static final String TAG = "MessageActivity";

    public static final String THREAD_ID = "THREAD_ID";
    public static final String OTHER_UID = "OTHER_UID";
    public static final String OTHER_NAME = "OTHER_NAME";

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private EventMessageManager messageManager;

    private TextView titleView;
    private TextView emptyStateView;
    private EditText inputView;
    private Button sendButton;
    private ListView listView;
    private ChatMessageAdapter adapter;
    private final List<ChatMessageItem> messages = new ArrayList<>();

    private MessageThreadItem currentThread;
    private ValueEventListener messagesListener;

    /**
     * This is the creation of the Activity
     * This connects to all the view on the screen and connects the clickable view to their controller
     * @param savedInstanceState
     * the saved state of the Activity so that the screen is not reset
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        messageManager = new EventMessageManager();

        titleView = findViewById(R.id.messageTitle);
        emptyStateView = findViewById(R.id.messageEmptyState);
        inputView = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.messageSendButton);
        listView = findViewById(R.id.messageListView);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.comments_sign_in_required, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new ChatMessageAdapter(this, messages, currentUser.getUid());
        listView.setAdapter(adapter);

        findViewById(R.id.messageBackButton).setOnClickListener(v -> finish());
        sendButton.setOnClickListener(v -> sendMessage());
    }

    /**
     * This is the startup of the Activity
     * it loads the current message thread
     */
    @Override
    protected void onStart() {
        super.onStart();
        loadThread();
    }

    /**
     * removes the message listener when leaving the screen
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (currentThread != null && messagesListener != null) {
            database.getReference()
                    .child("messageThreads")
                    .child(currentThread.getThreadId())
                    .child("messages")
                    .removeEventListener(messagesListener);
        }
        messagesListener = null;
    }

    /**
     * loads the message thread for this screen and attaches the listener for its messages
     */
    private void loadThread() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        String threadId = getIntent().getStringExtra(THREAD_ID);
        if (threadId == null) {
            threadId = "";
        } else {
            threadId = threadId.trim();
        }

        String otherUid = getIntent().getStringExtra(OTHER_UID);
        if (otherUid == null) {
            otherUid = "";
        } else {
            otherUid = otherUid.trim();
        }

        String otherName = getIntent().getStringExtra(OTHER_NAME);
        if (otherName == null) {
            otherName = "";
        } else {
            otherName = otherName.trim();
        }

        if (!threadId.isEmpty()) {
            messageManager.getThread(threadId)
                    .addOnSuccessListener(thread -> {
                        boolean isUserOne = currentUser.getUid().equals(thread.getUserOneUid());
                        boolean isUserTwo = currentUser.getUid().equals(thread.getUserTwoUid());
                        if (!isUserOne && !isUserTwo) {
                            Toast.makeText(this, R.string.messages_permission_denied, Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        String oldThreadId = currentThread == null ? "" : currentThread.getThreadId();
                        currentThread = thread;
                        String shownOtherName;
                        if (currentUser.getUid().equals(thread.getUserOneUid())) {
                            shownOtherName = thread.getUserTwoName();
                        } else {
                            shownOtherName = thread.getUserOneName();
                        }

                        titleView.setText(shownOtherName);
                        if (!oldThreadId.isEmpty() && messagesListener != null) {
                            database.getReference()
                                    .child("messageThreads")
                                    .child(oldThreadId)
                                    .child("messages")
                                    .removeEventListener(messagesListener);
                        }

                        messagesListener = new ValueEventListener() {
                            /**
                             * updates the screen when the thread messages change
                             * @param snapshot
                             * the snapshot containing the thread messages
                             */
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                messages.clear();
                                messages.addAll(messageManager.readMessages(snapshot));
                                adapter.notifyDataSetChanged();
                                boolean hasMessages = !messages.isEmpty();
                                listView.setVisibility(hasMessages ? View.VISIBLE : View.GONE);
                                emptyStateView.setVisibility(hasMessages ? View.GONE : View.VISIBLE);
                                if (!messages.isEmpty()) {
                                    listView.post(() -> listView.setSelection(messages.size() - 1));
                                }
                            }

                            /**
                             * handles a failure while loading the thread messages
                             * @param error
                             * the database error returned by Firebase
                             */
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(MessageActivity.this, R.string.messages_load_failed, Toast.LENGTH_SHORT).show();
                            }
                        };
                        database.getReference()
                                .child("messageThreads")
                                .child(thread.getThreadId())
                                .child("messages")
                                .addValueEventListener(messagesListener);
                    })
                    .addOnFailureListener(exception -> {
                        Log.e(TAG, "Failed to load thread", exception);
                        Toast.makeText(this, R.string.messages_load_failed, Toast.LENGTH_SHORT).show();
                        finish();
                    });
            return;
        }

        if (!otherUid.isEmpty()) {
            if (otherUid.equals(currentUser.getUid())) {
                Toast.makeText(this, R.string.messages_permission_denied, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            messageManager.buildThread(currentUser, otherUid, otherName)
                    .addOnSuccessListener(thread -> {
                        String oldThreadId = currentThread == null ? "" : currentThread.getThreadId();
                        currentThread = thread;
                        String shownOtherName;
                        if (currentUser.getUid().equals(thread.getUserOneUid())) {
                            shownOtherName = thread.getUserTwoName();
                        } else {
                            shownOtherName = thread.getUserOneName();
                        }

                        titleView.setText(shownOtherName);
                        if (!oldThreadId.isEmpty() && messagesListener != null) {
                            database.getReference()
                                    .child("messageThreads")
                                    .child(oldThreadId)
                                    .child("messages")
                                    .removeEventListener(messagesListener);
                        }

                        messagesListener = new ValueEventListener() {
                            /**
                             * updates the screen when the thread messages change
                             * @param snapshot
                             * the snapshot containing the thread messages
                             */
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                messages.clear();
                                messages.addAll(messageManager.readMessages(snapshot));
                                adapter.notifyDataSetChanged();
                                boolean hasMessages = !messages.isEmpty();
                                listView.setVisibility(hasMessages ? View.VISIBLE : View.GONE);
                                emptyStateView.setVisibility(hasMessages ? View.GONE : View.VISIBLE);
                                if (!messages.isEmpty()) {
                                    listView.post(() -> listView.setSelection(messages.size() - 1));
                                }
                            }

                            /**
                             * handles a failure while loading the thread messages
                             * @param error
                             * the database error returned by Firebase
                             */
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(MessageActivity.this, R.string.messages_load_failed, Toast.LENGTH_SHORT).show();
                            }
                        };
                        database.getReference()
                                .child("messageThreads")
                                .child(thread.getThreadId())
                                .child("messages")
                                .addValueEventListener(messagesListener);
                    })
                    .addOnFailureListener(exception -> {
                        Log.e(TAG, "Failed to initialize thread", exception);
                        Toast.makeText(this, R.string.messages_load_failed, Toast.LENGTH_SHORT).show();
                        finish();
                    });
            return;
        }

        Toast.makeText(this, R.string.messages_load_failed, Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * sends the text currently entered in the message input field
     */
    private void sendMessage() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || currentThread == null) {
            return;
        }

        String text = inputView.getText() == null ? "" : inputView.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, R.string.messages_empty_error, Toast.LENGTH_SHORT).show();
            return;
        }

        sendButton.setEnabled(false);
        inputView.setEnabled(false);
        messageManager.sendMessage(currentThread, currentUser, text)
                .addOnSuccessListener(unused -> {
                    inputView.setText("");
                    sendButton.setEnabled(true);
                    inputView.setEnabled(true);
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to send message", exception);
                    String errorMessage = exception != null && exception.getMessage() != null
                            ? exception.getMessage().trim()
                            : getString(R.string.messages_send_failed);
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    sendButton.setEnabled(true);
                    inputView.setEnabled(true);
                });
    }
}
