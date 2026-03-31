package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the current user's list of message threads.
 */
public class InboxActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private EventMessageManager messageManager;
    private ListView inboxListView;
    private TextView emptyStateView;
    private MessageThreadAdapter adapter;
    private final List<MessageThreadItem> threads = new ArrayList<>();

    private ValueEventListener threadsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        messageManager = new EventMessageManager();

        inboxListView = findViewById(R.id.inboxListView);
        emptyStateView = findViewById(R.id.inboxEmptyState);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.comments_sign_in_required, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new MessageThreadAdapter(this, threads, currentUser.getUid());
        inboxListView.setAdapter(adapter);
        inboxListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < threads.size()) {
                Intent intent = new Intent(this, MessageActivity.class);
                intent.putExtra(MessageActivity.THREAD_ID, threads.get(position).getThreadId());
                startActivity(intent);
            }
        });

        findViewById(R.id.inboxBackButton).setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        attachThreadsListener(currentUser.getUid());
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (threadsListener != null && currentUser != null) {
            database.getReference()
                    .child("userThreads")
                    .child(currentUser.getUid())
                    .removeEventListener(threadsListener);
        }
        threadsListener = null;
    }

    private void attachThreadsListener(@NonNull String uid) {
        if (threadsListener != null) {
            database.getReference()
                    .child("userThreads")
                    .child(uid)
                    .removeEventListener(threadsListener);
        }

        threadsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                threads.clear();
                threads.addAll(messageManager.readThreads(snapshot));
                adapter.notifyDataSetChanged();
                boolean hasThreads = !threads.isEmpty();
                inboxListView.setVisibility(hasThreads ? View.VISIBLE : View.GONE);
                emptyStateView.setVisibility(hasThreads ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(
                        InboxActivity.this,
                        R.string.messages_load_failed,
                        Toast.LENGTH_SHORT
                ).show();
            }
        };
        database.getReference()
                .child("userThreads")
                .child(uid)
                .addValueEventListener(threadsListener);
    }
}
