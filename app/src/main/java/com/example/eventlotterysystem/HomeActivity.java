package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private TextView signedInSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        signedInSubtitle = findViewById(R.id.signedInSubtitle);

        findViewById(R.id.myThingsButton).setOnClickListener(v ->
                startActivity(new Intent(this, MyThingsActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            navigateToAuthMenu();
            return;
        }

        currentUser.reload().addOnCompleteListener(task -> {
            FirebaseUser refreshedUser = auth.getCurrentUser();
            if (!task.isSuccessful() || refreshedUser == null) {
                verifyActiveProfileAndRender(currentUser);
                return;
            }
            verifyActiveProfileAndRender(refreshedUser);
        });
    }

    private void verifyActiveProfileAndRender(FirebaseUser user) {
        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean deleted = !snapshot.exists() || Boolean.TRUE.equals(snapshot.getBoolean("deleted"));
                    if (deleted) {
                        auth.signOut();
                        AuthSessionPreference.setRemember(this, false);
                        navigateToProfileRemoved();
                        return;
                    }
                    refreshIdentity(user);
                })
                .addOnFailureListener(exception -> refreshIdentity(user));
    }

    private void refreshIdentity(FirebaseUser user) {
        String identity = user.getEmail();
        if (identity == null || identity.isEmpty()) {
            identity = user.getUid();
        }
        signedInSubtitle.setText(getString(R.string.signed_in_as, identity));
    }

    private void navigateToAuthMenu() {
        auth.signOut();
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToProfileRemoved() {
        Intent intent = new Intent(this, ProfileRemovedActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
