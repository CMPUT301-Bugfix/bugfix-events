package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MyThingsActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private NotificationRepository notificationRepository;

    private TextView myThingsSubtitle;
    private Button adminZoneButton;
    private Button myWaitlistButton;
    private RecyclerView notificationsRecyclerView;
    private NotificationAdapter notificationAdapter;
    private List<NotificationItem> notificationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_things);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();

        myThingsSubtitle = findViewById(R.id.myThingsSubtitle);
        adminZoneButton = findViewById(R.id.adminZoneButton);
        myWaitlistButton = findViewById(R.id.myWaitlistButton);
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);

        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter(notificationList);
        notificationsRecyclerView.setAdapter(notificationAdapter);

        findViewById(R.id.myThingsBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.settingsButton).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.hostEventButton).setOnClickListener(v ->
                startActivity(new Intent(this, HostedEventsActivity.class)));
        myWaitlistButton.setOnClickListener(v ->
                startActivity(new Intent(this, MyWaitlistActivity.class)));
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
        loadNotifications(currentUser.getUid());
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

    private void loadNotifications(String uid) {
        notificationRepository.getNotificationsForUser(uid)
                .addOnSuccessListener(notifications -> {
                    notificationList.clear();
                    notificationList.addAll(notifications);
                    notificationAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MyThingsActivity.this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                });
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
