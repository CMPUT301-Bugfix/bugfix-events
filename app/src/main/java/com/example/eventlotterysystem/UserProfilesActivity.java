package com.example.eventlotterysystem;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class UserProfilesActivity extends AppCompatActivity {

    private ImageButton backButton;
    private ListView profilesListView;

    private final ArrayList<UserProfile> profiles = new ArrayList<>();
    private UserProfileArrayAdapter adapter;

    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private ListenerRegistration registration;


    private static final String FIELD_FULL_NAME = "fullName";
    private static final String FIELD_ACCOUNT_TYPE = "accountType";
    private static final String FIELD_SUMMARY = "summary";
    private static final String FIELD_DELETED = "deleted";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profiles);

        backButton = findViewById(R.id.btn_back);
        profilesListView = findViewById(R.id.list_profiles);

        backButton.setOnClickListener(v -> finish());

        adapter = new UserProfileArrayAdapter(this, profiles);
        profilesListView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");


        profilesListView.setOnItemLongClickListener((parent, view, position, id) -> {
            UserProfile selected = profiles.get(position);
            showDeleteConfirmDialog(selected);
            return true;
        });

        startListeningToUsers();
    }

    private void startListeningToUsers() {
        registration = usersRef.addSnapshotListener((QuerySnapshot snapshots, com.google.firebase.firestore.FirebaseFirestoreException error) -> {
            if (error != null) {
                Toast.makeText(this, "Load failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            if (snapshots == null) return;

            profiles.clear();

            for (DocumentSnapshot doc : snapshots.getDocuments()) {

                Boolean deleted = doc.getBoolean(FIELD_DELETED);
                if (deleted != null && deleted) {
                    continue;
                }

                String id = doc.getId();

                String fullName = safeString(doc.getString(FIELD_FULL_NAME), "Unnamed user");
                String typeRaw = safeString(doc.getString(FIELD_ACCOUNT_TYPE), "user");
                String summary = safeString(doc.getString(FIELD_SUMMARY), "");

                UserProfile.AccountType accountType = UserProfile.parseAccountType(typeRaw);

                profiles.add(new UserProfile(id, fullName, accountType, summary));
            }

            adapter.notifyDataSetChanged();
        });
    }

    private void showDeleteConfirmDialog(UserProfile user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete user?")
                .setMessage("This will permanently delete:\n\n" + user.getFullName() + "\n(" + user.getAccountType().name() + ")")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> hardDeleteUser(user.getId()))
                .show();
    }

    private void hardDeleteUser(String userId) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .delete()
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );

    }

    private String safeString(String value, String fallback) {
        if (value == null) return fallback;
        String v = value.trim();
        return v.isEmpty() ? fallback : v;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
