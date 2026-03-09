package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MyThingsActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private TextView myThingsSubtitle;
    private Button adminZoneButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_things);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        myThingsSubtitle = findViewById(R.id.myThingsSubtitle);
        adminZoneButton = findViewById(R.id.adminZoneButton);

        findViewById(R.id.myThingsBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.settingsButton).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.hostEventButton).setOnClickListener(v ->
                startActivity(new Intent(this, HostedEventsActivity.class)));
        findViewById(R.id.myThingsLogoutButton).setOnClickListener(v -> onLogOutClicked());
        adminZoneButton.setOnClickListener(v ->
                startActivity(new Intent(this, AdminZoneActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            navigateToAuthMenu();
            return;
        }

        String identity = currentUser.getEmail();
        if (identity == null || identity.isEmpty()) {
            identity = currentUser.getUid();
        }
        myThingsSubtitle.setText(getString(R.string.signed_in_as, identity));
        loadAccountType(currentUser.getUid());
    }

    private void loadAccountType(String uid) {
        adminZoneButton.setVisibility(View.GONE);
        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String accountType = snapshot.getString("accountType");
                    if ("admin".equals(accountType)) {
                        adminZoneButton.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(exception -> adminZoneButton.setVisibility(View.GONE));
    }

    private void onLogOutClicked() {
        auth.signOut();
        AuthSessionPreference.setRemember(this, false);
        navigateToAuthMenu();
    }

    private void navigateToAuthMenu() {
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
