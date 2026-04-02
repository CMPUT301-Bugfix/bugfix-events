package com.example.eventlotterysystem;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Activity for displaying the status of invitations sent for private event
 * shows list of users and whether they have accepted, rejected, or pending
 */
public class InvitationStatusActivity extends AppCompatActivity {

    private String eventId;
    private FirebaseFirestore firestore;
    private RecyclerView recyclerView;
    private InvitationAdapter adapter;
    private List<Map<String, Object>> invitations = new ArrayList<>();

    /**
     * initializes the activity, sets up the RecyclerView and loads invitation data
     * @param savedInstanceState saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitation_status);

        eventId = getIntent().getStringExtra("EVENT_ID");
        firestore = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.invitationStatusRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InvitationAdapter(invitations);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.invitationStatusBackButton).setOnClickListener(v -> finish());

        loadInvitations();
    }

    /**
     * acquires invitation records from the event's invitations sub-collection in Firestore
     * updates the list and notifies the adapter of data changes
     */
    private void loadInvitations() {
        firestore.collection("events")
                .document(eventId)
                .collection("invitations")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    invitations.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        invitations.add(doc.getData());
                    }
                    adapter.notifyDataSetChanged();
                    if (invitations.isEmpty()) {
                        Toast.makeText(this, "No invitations sent yet", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("InvitationStatus", "Error loading invitations", e);
                    Toast.makeText(this, "Failed to load invitations", Toast.LENGTH_SHORT).show();
                });
    }
}
