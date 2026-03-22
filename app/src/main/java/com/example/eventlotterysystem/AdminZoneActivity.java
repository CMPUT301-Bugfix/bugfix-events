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

/**
 * This class deals with the Admin Zone page
 */

public class AdminZoneActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private ProgressBar adminRoleLoading;
    private Button adminUserProfilesButton;
    private Button adminEventsButton;
    private Button adminPhotosButton;

    /**
     * This method sets up the UI and button navigations
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_zone);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        adminRoleLoading = findViewById(R.id.adminRoleLoading);
        adminUserProfilesButton = findViewById(R.id.adminUserProfilesButton);
        adminUserProfilesButton.setEnabled(false);
        adminEventsButton = findViewById(R.id.adminEventsButton);
        adminEventsButton.setEnabled(false);
        adminPhotosButton = findViewById(R.id.adminPhotosButton);
        adminPhotosButton.setEnabled(false);

        findViewById(R.id.adminZoneBackButton).setOnClickListener(v -> finish());
        adminUserProfilesButton.setOnClickListener(v ->
                startActivity(new Intent(this, UserProfilesActivity.class)));
        adminEventsButton.setOnClickListener(v ->
                startActivity(new Intent(this, AdminBrowseEventsActivity.class)));
        adminPhotosButton.setOnClickListener(v ->
                startActivity(new Intent(this, AdminPhotosActivity.class)));
    }

    /**
     * This screen checks if the current user is an admin and is
     * allowed to access this page
     */

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
        adminEventsButton.setEnabled(false);
        adminPhotosButton.setEnabled(false);
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
                    adminEventsButton.setEnabled(true);
                    adminPhotosButton.setEnabled(true);
                })
                .addOnFailureListener(exception -> {
                    adminRoleLoading.setVisibility(View.GONE);
                    finish();
                });
    }

    /**
     * If in onStart, the user was not signed in, this method
     * forces the user back to the AuthMenu
     */

    private void navigateToAuthMenu() {
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
