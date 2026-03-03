package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserProfilesActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private TextView backButton;
    private Spinner userTypeFilterSpinner;
    private ProgressBar userProfilesLoading;
    private TextView userProfilesEmptyState;
    private ScrollView userProfilesScrollView;
    private LinearLayout userProfilesContainer;

    private boolean isAdminConfirmed;
    private String selectedAccountType = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profiles);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        backButton = findViewById(R.id.userProfilesBackButton);
        userTypeFilterSpinner = findViewById(R.id.userTypeFilterSpinner);
        userProfilesLoading = findViewById(R.id.userProfilesLoading);
        userProfilesEmptyState = findViewById(R.id.userProfilesEmptyState);
        userProfilesScrollView = findViewById(R.id.userProfilesScrollView);
        userProfilesContainer = findViewById(R.id.userProfilesContainer);

        backButton.setOnClickListener(v -> finish());
        setupFilterSpinner();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            navigateToAuthMenu();
            return;
        }

        verifyAdminAndLoad(currentUser.getUid());
    }

    private void setupFilterSpinner() {
        List<String> filterOptions = new ArrayList<>();
        filterOptions.add(getString(R.string.filter_users));
        filterOptions.add(getString(R.string.filter_organizers));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                filterOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        userTypeFilterSpinner.setAdapter(adapter);

        userTypeFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String nextAccountType = position == 1 ? "organizer" : "all";
                if (nextAccountType.equals(selectedAccountType) && isAdminConfirmed) {
                    return;
                }
                selectedAccountType = nextAccountType;
                if (isAdminConfirmed) {
                    loadProfilesForFilter();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void verifyAdminAndLoad(@NonNull String uid) {
        setLoading(true);
        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String accountType = snapshot.getString("accountType");
                    if (!"admin".equals(accountType)) {
                        finish();
                        return;
                    }
                    isAdminConfirmed = true;
                    loadProfilesForFilter();
                })
                .addOnFailureListener(exception -> finish());
    }

    private void loadProfilesForFilter() {
        setLoading(true);
        if ("all".equals(selectedAccountType)) {
            firestore.collection("users")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<UserProfile> profiles = new ArrayList<>();
                        for (DocumentSnapshot snapshot : querySnapshot.getDocuments()) {
                            profiles.add(mapSnapshotToUserProfile(snapshot));
                        }
                        profiles.sort((left, right) -> left.getName().compareToIgnoreCase(right.getName()));
                        renderProfiles(profiles);
                        setLoading(false);
                    })
                    .addOnFailureListener(exception -> {
                        setLoading(false);
                        renderProfiles(new ArrayList<>());
                        showMessage(getString(R.string.unexpected_error));
                    });
            return;
        }

        firestore.collection("users")
                .whereEqualTo("accountType", "organizer")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<UserProfile> profiles = new ArrayList<>();
                    for (DocumentSnapshot snapshot : querySnapshot.getDocuments()) {
                        profiles.add(mapSnapshotToUserProfile(snapshot));
                    }
                    profiles.sort((left, right) -> left.getName().compareToIgnoreCase(right.getName()));
                    renderProfiles(profiles);
                    setLoading(false);
                })
                .addOnFailureListener(exception -> {
                    setLoading(false);
                    renderProfiles(new ArrayList<>());
                    showMessage(getString(R.string.unexpected_error));
                });
    }

    private void renderProfiles(@NonNull List<UserProfile> profiles) {
        userProfilesContainer.removeAllViews();

        if (profiles.isEmpty()) {
            userProfilesEmptyState.setText(getString(R.string.no_profiles_for_filter));
            userProfilesEmptyState.setVisibility(View.VISIBLE);
            userProfilesScrollView.setVisibility(View.GONE);
            return;
        }

        userProfilesEmptyState.setVisibility(View.GONE);
        userProfilesScrollView.setVisibility(View.VISIBLE);

        for (UserProfile profile : profiles) {
            View row = getLayoutInflater().inflate(R.layout.item_admin_user_profile, userProfilesContainer, false);
            TextView profileNameValue = row.findViewById(R.id.profileNameValue);
            TextView profileUsernameValue = row.findViewById(R.id.profileUsernameValue);
            TextView profileEmailValue = row.findViewById(R.id.profileEmailValue);
            TextView profileTypeValue = row.findViewById(R.id.profileTypeValue);
            TextView profileJoinDateValue = row.findViewById(R.id.profileJoinDateValue);

            profileNameValue.setText(getString(R.string.profile_name_label, safeValue(profile.getName(), R.string.unknown_name)));
            profileUsernameValue.setText(getString(R.string.profile_username_label, safeValue(profile.getUsername(), R.string.unknown_username)));
            profileEmailValue.setText(getString(R.string.profile_email_label, safeValue(profile.getEmail(), R.string.unknown_email)));
            profileTypeValue.setText(getString(R.string.profile_type_label, safeValue(profile.getAccountType(), R.string.unknown_account_type)));
            profileJoinDateValue.setText(getString(R.string.profile_join_date_label, formatJoinDate(profile.getCreatedAt())));

            userProfilesContainer.addView(row);
        }
    }

    @NonNull
    private UserProfile mapSnapshotToUserProfile(@NonNull DocumentSnapshot snapshot) {
        UserProfile profile = new UserProfile(
                normalize(snapshot.getString("fullName")),
                normalize(snapshot.getString("email")),
                normalize(snapshot.getString("username")),
                "",
                normalize(snapshot.getString("phoneNumber")),
                normalize(snapshot.getString("accountType"))
        );
        profile.setCreatedAt(snapshot.getTimestamp("createdAt"));
        return profile;
    }

    @NonNull
    private String safeValue(@NonNull String value, int fallbackResId) {
        if (TextUtils.isEmpty(value)) {
            return getString(fallbackResId);
        }
        return value;
    }

    @NonNull
    private String formatJoinDate(Timestamp createdAt) {
        if (createdAt == null) {
            return getString(R.string.unknown_join_date);
        }
        Date createdAtDate = createdAt.toDate();
        return new SimpleDateFormat("MMM d, yyyy", Locale.US).format(createdAtDate);
    }

    private void setLoading(boolean loading) {
        userProfilesLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        backButton.setEnabled(!loading);
        userTypeFilterSpinner.setEnabled(!loading);
        if (loading) {
            userProfilesEmptyState.setVisibility(View.GONE);
            userProfilesScrollView.setVisibility(View.GONE);
        }
    }

    private void showMessage(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void navigateToAuthMenu() {
        auth.signOut();
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @NonNull
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
