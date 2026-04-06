package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * activity for organizers to send invitations for private event
 * allows searching for users and adding them to batch for notification
 */
public class SendPrivateEventActivity extends AppCompatActivity {

    private String eventId;
    private String eventTitle;
    private FirebaseFirestore firestore;
    private NotificationRepository notificationRepository;

    private EditText userSearchInput;
    private Button userSearchButton;
    private RecyclerView userSearchResultsRecyclerView;
    private TextView batchCountText;
    private Button notifyBatchButton;
    private Button viewInvitationStatusButton;

    private UserSearchAdapter adapter;
    private List<UserProfile> searchResults = new ArrayList<>();
    private List<UserProfile> batchUsers = new ArrayList<>();

    /**
     * initializes the activity, sets up UI components and listeners
     * @param savedInstanceState The saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_private_event);

        eventId = getIntent().getStringExtra("EVENT_ID");
        eventTitle = getIntent().getStringExtra("EVENT_TITLE");
        firestore = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();

        userSearchInput = findViewById(R.id.userSearchInput);
        userSearchButton = findViewById(R.id.userSearchButton);
        userSearchResultsRecyclerView = findViewById(R.id.userSearchResultsRecyclerView);
        batchCountText = findViewById(R.id.batchCountText);
        notifyBatchButton = findViewById(R.id.notifyBatchButton);
        viewInvitationStatusButton = findViewById(R.id.viewInvitationStatusButton);

        findViewById(R.id.sendPrivateEventBackButton).setOnClickListener(v -> finish());

        userSearchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserSearchAdapter(searchResults, user -> {
            if (!containsUser(batchUsers, user.getUid())) {
                batchUsers.add(user);
                updateBatchUI();
            } else {
                Toast.makeText(this, "User already in batch", Toast.LENGTH_SHORT).show();
            }
        });
        userSearchResultsRecyclerView.setAdapter(adapter);

        userSearchButton.setOnClickListener(v -> performSearch());
        notifyBatchButton.setOnClickListener(v -> sendNotifications());
        viewInvitationStatusButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, InvitationStatusActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            startActivity(intent);
        });
    }

    /**
     * performs a search for users based on the input text
     * searches by name, email, and username
     */
    private void performSearch() {
        String queryText = userSearchInput.getText().toString().trim();
        if (TextUtils.isEmpty(queryText)) {
            Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }

        // Search by name, email, phone number, or username
        Query nameQuery = firestore.collection("users").whereEqualTo("fullName", queryText);
        Query emailQuery = firestore.collection("users").whereEqualTo("email", queryText.toLowerCase());
        Query phoneQuery = firestore.collection("users").whereEqualTo("phoneNumber", queryText);
        Query usernameQuery = firestore.collection("users").whereEqualTo("username", queryText);

        searchResults.clear();
        adapter.notifyDataSetChanged();

        Tasks.whenAllSuccess(nameQuery.get(), emailQuery.get(), phoneQuery.get(), usernameQuery.get())
                .addOnSuccessListener(results -> {
                    for (Object result : results) {
                        com.google.firebase.firestore.QuerySnapshot snapshot = (com.google.firebase.firestore.QuerySnapshot) result;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            UserProfile profile = mapSnapshotToUserProfile(doc);
                            if (!containsUser(searchResults, profile.getUid())) {
                                searchResults.add(profile);
                            }
                        }
                    }
                    if (searchResults.isEmpty()) {
                        Toast.makeText(this, "No users found", Toast.LENGTH_SHORT).show();
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Checks if a user with the given UID is already in the list
     * @param list The list of UserProfile objects to check
     * @param uid The UID to search for
     * @return true if the user is in the list, false otherwise
     */
    private boolean containsUser(List<UserProfile> list, String uid) {
        for (UserProfile u : list) {
            if (u.getUid().equals(uid)) return true;
        }
        return false;
    }

    /**
     * maps a Firestore document snapshot to UserProfile object
     * @param snapshot document snapshot from Firestore
     * @return populated UserProfile object
     */
    private UserProfile mapSnapshotToUserProfile(DocumentSnapshot snapshot) {
        UserProfile profile = new UserProfile(
                snapshot.getString("fullName"),
                snapshot.getString("email"),
                snapshot.getString("username"),
                "",
                snapshot.getString("phoneNumber"),
                snapshot.getString("accountType")
        );
        profile.setUid(snapshot.getId());
        return profile;
    }

    /**
     * updates the UI to reflect the current number of users in the batch
     */
    private void updateBatchUI() {
        batchCountText.setText("Batch: " + batchUsers.size() + " users");
        notifyBatchButton.setEnabled(!batchUsers.isEmpty());
    }

    /**
     * sends invitation notifications to all users currently in the batch
     */
    private void sendNotifications() {
        String message = "You are invited to join the waitlist for the private event: " + eventTitle;
        notificationRepository.sendInvitations(eventId, "Private Event Invitation", message, batchUsers)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Notifications sent to batch", Toast.LENGTH_SHORT).show();
                    batchUsers.clear();
                    updateBatchUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
