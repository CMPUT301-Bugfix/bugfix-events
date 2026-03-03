package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminZoneActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private ProgressBar adminRoleLoading;
    private Button adminUserProfilesButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_zone);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        adminRoleLoading = findViewById(R.id.adminRoleLoading);
        adminUserProfilesButton = findViewById(R.id.adminUserProfilesButton);
        adminUserProfilesButton.setEnabled(false);

        findViewById(R.id.adminZoneBackButton).setOnClickListener(v -> finish());
        adminUserProfilesButton.setOnClickListener(v ->
                startActivity(new Intent(this, UserProfilesActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            navigateToAuthMenu();
            return;
        }

        adminRoleLoading.setVisibility(View.VISIBLE);
        adminUserProfilesButton.setEnabled(false);
        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    adminRoleLoading.setVisibility(View.GONE);
                    String accountType = snapshot.getString("accountType");
                    if (!"admin".equals(accountType)) {
                        finish();
                        return;
                    }
                    adminUserProfilesButton.setEnabled(true);
                })
                .addOnFailureListener(exception -> {
                    adminRoleLoading.setVisibility(View.GONE);
                    finish();
                });
    }

    private void navigateToAuthMenu() {
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
