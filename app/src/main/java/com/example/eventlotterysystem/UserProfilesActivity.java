package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * This class deals with the browsing of all users for admins
 */

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
    private ListenerRegistration profilesListener;

    /**
     * This method loads the UI, initializes the firebase and event repository instances,
     * and connects the java variables to the views in the XML
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */

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

    /**
     * Essentially checks whether there is a signed in user, and to navigate
     * to the AuthMenu if not, and check if the current user is an admin.
     */

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

    /**
     * This method deals with the dropdown menu that allows the admin
     * to filter between all users and just organizers.
     */
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

    /**
     * This method checks whether or not the user is an admin, and if so
     * then run the loadProfilesForFilter method, but if not, close the screen.
     * @param uid the id of a user to be verified
     */

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

    /**
     * This method is the heart of this class, it collects the users from the
     * Firestore events collection by using a snapshot listener for live updates,
     * and creates a list by looping through the users collection and calling the
     * renderProfiles method to render the profiles into rows in the UI. Also deals with
     * error handling and profilesListener cleanup.
     */

    private void loadProfilesForFilter() {
        setLoading(true);

        if (profilesListener != null) {
            profilesListener.remove();
            profilesListener = null;
        }

        Query query = firestore.collection("users");
        if (!"all".equals(selectedAccountType)) {
            query = query.whereEqualTo("accountType", "organizer");
        }

        profilesListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                setLoading(false);
                renderProfiles(new ArrayList<>());
                Log.e("Firestore", error.toString());
                return;
            }

            List<UserProfile> profiles = new ArrayList<>();

            if (value != null) {
                for (DocumentSnapshot snapshot : value.getDocuments()) {
                    if (Boolean.TRUE.equals(snapshot.getBoolean("deleted"))) {
                        continue;
                    }
                    profiles.add(mapSnapshotToUserProfile(snapshot));
                }
            }

            profiles.sort((left, right) -> left.getName().compareToIgnoreCase(right.getName()));
            renderProfiles(profiles);
            setLoading(false);
        });
    }

    /**
     * This method turns each profile into a row in the UI, starting by removing any
     * previous views, and notifying the user if there are no profiles. If there are
     * profiles, then it creates a new row for each profiles with the necessary fields
     * that we wanted to display.
     * @param profiles this is the list of profiles from the database
     */

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
            TextView profileTypeValue = row.findViewById(R.id.profileTypeValue);

            profileNameValue.setText(getString(R.string.profile_name_label, safeValue(profile.getName(), R.string.unknown_name)));
            profileTypeValue.setText(getString(R.string.profile_type_label, safeValue(profile.getAccountType(), R.string.unknown_account_type)));
            row.setOnClickListener(v -> openProfileDetails(profile));
            userProfilesContainer.addView(row);
        }
    }

    /**
     * This method deals with converting a Firestore user from the collection into
     * a UserProfile object, matching all necessary fields.
     * @param snapshot a single Firestore user document
     * @return profile
     */

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
        profile.setUid(snapshot.getId());
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

    /**
     * Deals with the loading screen, this method hides data/views
     * while we fetch current data
     * @param loading boolean value
     */

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

    /**
     * Navigates to the AuthMenu when called
     */

    private void navigateToAuthMenu() {
        auth.signOut();
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    /**
     * This method sends the necessary information to the user details page
     * @param profile this is an instance of a single user
     */

    private void openProfileDetails(@NonNull UserProfile profile) {
        Intent intent = new Intent(this, UserProfileDetailsActivity.class);
        intent.putExtra(UserProfileDetailsActivity.NAME, normalize(profile.getName()));
        intent.putExtra(UserProfileDetailsActivity.ACCOUNT_TYPE, normalize(profile.getAccountType()));
        intent.putExtra(UserProfileDetailsActivity.USERNAME, normalize(profile.getUsername()));
        intent.putExtra(UserProfileDetailsActivity.EMAIL, normalize(profile.getEmail()));
        intent.putExtra(UserProfileDetailsActivity.PHONE, normalize(profile.getPhoneNumber()));
        intent.putExtra(UserProfileDetailsActivity.UID, normalize(profile.getUid()));
        long timeMillis = profile.getCreatedAt() == null ? -1L : profile.getCreatedAt().toDate().getTime();
        intent.putExtra(UserProfileDetailsActivity.TIME_MILLIS, timeMillis);
        startActivity(intent);
    }

    /**
     * This method just cleans up a string for us
     * @param value A string that we want to clean
     * @return either an empty string if the value is null or the trimmed string
     */

    @NonNull
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
