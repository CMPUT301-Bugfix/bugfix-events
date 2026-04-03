package com.example.eventlotterysystem;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class deals with displaying the user details page for the
 * admins, which will allow admins to delete a user profile
 */

public class UserProfileDetailsActivity extends AppCompatActivity {

    public static final String NAME = "name";
    public static final String ACCOUNT_TYPE = "accountType";
    public static final String USERNAME = "username";
    public static final String EMAIL = "email";
    public static final String PHONE = "phone";
    public static final String UID = "uid";
    public static final String TIME_MILLIS = "timeMillis";
    public static final String ALLOW_DELETE = "allowDelete";
    public static final String EVENT_ID = "eventId";

    private FirebaseFirestore firestore;
    private Button deleteProfileButton;
    private Button assignCoorganizerButton;
    private Button removeOrganizerButton;
    private EventRepository repository;
    private NotificationRepository notificationRepository;
    private boolean isDeleting;
    private boolean isAssigningCoorganizer;
    private String viewedUid;
    private List<UserProfile> batchUsers = new ArrayList<>();
    private String viewedAccountType;
    private String viewedName;
    private String sourceEventId;

    /**
     * This method deals with the creation of the user profile details screen and its component views
     * as well as initializing and getting all the necessary information for that screen,
     * @param savedInstanceState
     * the Bundle data from a previous state of the activity
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile_details);
        firestore = FirebaseFirestore.getInstance();
        repository = new EventRepository();
        notificationRepository = new NotificationRepository();

        TextView detailsBackButton = findViewById(R.id.userProfileDetailsBackButton);
        TextView detailsNameValue = findViewById(R.id.userProfileDetailsNameValue);
        TextView detailsTypeValue = findViewById(R.id.userProfileDetailsTypeValue);
        TextView detailsUsernameValue = findViewById(R.id.userProfileDetailsUsernameValue);
        TextView detailsEmailValue = findViewById(R.id.userProfileDetailsEmailValue);
        TextView detailsPhoneValue = findViewById(R.id.userProfileDetailsPhoneValue);
        TextView detailsJoinDateValue = findViewById(R.id.userProfileDetailsJoinDateValue);
        deleteProfileButton = findViewById(R.id.userProfileDeleteButton);
        assignCoorganizerButton = findViewById(R.id.userProfileAssignCoorganizerButton);
        removeOrganizerButton = findViewById(R.id.userProfileRemoveOrganizerButton);

        detailsBackButton.setOnClickListener(v -> finish());

        viewedName = normalize(getIntent().getStringExtra(NAME));
        viewedAccountType = normalize(getIntent().getStringExtra(ACCOUNT_TYPE));
        viewedUid = normalize(getIntent().getStringExtra(UID));

        String username = normalize(getIntent().getStringExtra(USERNAME));
        String email = normalize(getIntent().getStringExtra(EMAIL));
        String phone = normalize(getIntent().getStringExtra(PHONE));
        long timeMillis = getIntent().getLongExtra(TIME_MILLIS, -1L);
        boolean allowDelete = getIntent().getBooleanExtra(ALLOW_DELETE, true);
        sourceEventId = normalize(getIntent().getStringExtra(EVENT_ID));

        detailsNameValue.setText(getString(R.string.profile_name_label, safeValue(viewedName, R.string.unknown_name)));
        detailsTypeValue.setText(getString(R.string.profile_type_label, safeValue(viewedAccountType, R.string.unknown_account_type)));
        detailsUsernameValue.setText(getString(R.string.profile_username_label, safeValue(username, R.string.unknown_username)));
        detailsEmailValue.setText(getString(R.string.profile_email_label, safeValue(email, R.string.unknown_email)));
        detailsPhoneValue.setText(getString(R.string.profile_phone_label, safeValue(phone, R.string.unknown_phone)));
        detailsJoinDateValue.setText(getString(R.string.profile_join_date_label, formatJoinDate(timeMillis)));

        boolean canDeleteViewedProfile = allowDelete
                && !TextUtils.isEmpty(viewedUid)
                && !"admin".equalsIgnoreCase(viewedAccountType);
        if (!canDeleteViewedProfile) {
            deleteProfileButton.setVisibility(View.GONE);
        } else {
            deleteProfileButton.setOnClickListener(v -> onDeleteProfileClicked());
        }

        boolean canRemoveOrganizer = !TextUtils.isEmpty(viewedUid)
                && "organizer".equalsIgnoreCase(viewedAccountType);

        if (!canRemoveOrganizer) {
            removeOrganizerButton.setVisibility(View.GONE);
        } else {
            removeOrganizerButton.setVisibility(View.VISIBLE);
            removeOrganizerButton.setOnClickListener(v -> onRemoveOrganizerClicked());
        }

        assignCoorganizerButton.setVisibility(View.GONE);
        if (!TextUtils.isEmpty(sourceEventId) && !TextUtils.isEmpty(viewedUid)) {
            configureAssignCoorganizerButton();
        }
    }

    /**
     * This method shows the dialog for confirming deletion
     */

    private void onDeleteProfileClicked() {
        if (isDeleting) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_delete_profile_confirm_title)
                .setMessage(getString(
                        R.string.admin_delete_profile_confirm_message,
                        safeValue(viewedName, R.string.unknown_name)
                ))
                .setNegativeButton(R.string.admin_delete_profile_cancel_action, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.admin_delete_profile_confirm_action, (dialog, which) -> deleteViewedProfile())
                .show();
    }

    /**
     * This method shows the dialog for confirming the removing of an organizer
     */

    private void onRemoveOrganizerClicked() {
        if (isDeleting || TextUtils.isEmpty(viewedUid)) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_remove_organizer_confirm_title)
                .setMessage(getString(
                        R.string.admin_remove_organizer_confirm_message,
                        safeValue(viewedName, R.string.unknown_name)
                ))
                .setNegativeButton(R.string.admin_delete_profile_cancel_action, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.admin_remove_organizer_confirm_action, (d, w) -> removeOrganizerPrivileges())
                .show();
    }

    /**
     * This method deletes the user document from the firestore user collection
     */

    private void deleteViewedProfile() {
        if (TextUtils.isEmpty(viewedUid)) {
            showMessage(getString(R.string.admin_delete_profile_failed));
            return;
        }

        setDeleting(true);
        firestore.collection("users")
                .document(viewedUid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    setDeleting(false);
                    showMessage(getString(R.string.admin_delete_profile_success));
                    finish();
                })
                .addOnFailureListener(new OnFailureListener() {
                    /**
                     * handles a failure while deleting the UserProfile
                     * @param e
                     * the exception raised while deleting the UserProfile
                     */
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleDeleteFailure(e);
                    }
                });
    }

    /**
     * This method removes an organizers privileges and changes them from
     * an organizer to a user in the firestore collections and also adds a suspended
     * field set to true for the user
     */

    private void removeOrganizerPrivileges() {
        if (TextUtils.isEmpty(viewedUid)) {
            showMessage(getString(R.string.admin_remove_organizer_failed));
            return;
        }

        setDeleting(true);
        firestore.collection("users")
                .document(viewedUid)
                .update("suspended", true, "accountType", "user")
                .addOnSuccessListener(aVoid -> {
                    setDeleting(false);
                    showMessage(getString(R.string.admin_remove_organizer_success));
                    viewedAccountType = "user";
                    removeOrganizerButton.setVisibility(View.GONE);
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showMessage(getString(R.string.admin_remove_organizer_failed));
                    }
                });
    }

    /**
     * In the case where we are unable to delete an event, we display a message stating as such
     * @param exception This is the error from deleteEvent()
     */

    private void handleDeleteFailure(@NonNull Exception exception) {
        setDeleting(false);
        if (exception instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) exception).getCode()
                == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            showMessage(getString(R.string.admin_delete_profile_permission_denied));
            return;
        }
        showMessage(getString(R.string.admin_delete_profile_failed));
    }

    /**
     * Disables the delete button when deleting
     * @param deleting boolean value
     */

    private void setDeleting(boolean deleting) {
        isDeleting = deleting;
        deleteProfileButton.setEnabled(!deleting);
    }

    /**
     * checks if the viewed user can be promoted to a coorganizer for the source Event
     * and shows the assign button if they can be assigned
     */
    private void configureAssignCoorganizerButton() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            assignCoorganizerButton.setVisibility(View.GONE);
            return;
        }

        repository.getEventById(sourceEventId)
                .addOnSuccessListener(event -> {
                    boolean canAssign = EventRepository.isHost(event, currentUser.getUid())
                            && !EventRepository.isHost(event, viewedUid)
                            && !event.getCoorganizers().contains(viewedUid);
                    if (!canAssign) {
                        assignCoorganizerButton.setVisibility(View.GONE);
                        return;
                    }
                    assignCoorganizerButton.setVisibility(View.VISIBLE);
                    assignCoorganizerButton.setOnClickListener(v -> showAssignCoorganizerDialog());
                })
                .addOnFailureListener(exception -> assignCoorganizerButton.setVisibility(View.GONE));
    }

    /**
     * shows a popup asking the host to confirm assigning the viewed user as a coorganizer
     */
    private void showAssignCoorganizerDialog() {
        if (isAssigningCoorganizer) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.assign_coorganizer_title)
                .setMessage(getString(
                        R.string.assign_coorganizer_confirm_message,
                        safeValue(viewedName, R.string.unknown_name)
                ))
                .setNegativeButton(R.string.back, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.assign_coorganizer_action, (dialog, which) -> assignCoorganizer())
                .show();
    }

    /**
     * assigns the viewed user as a coorganizer for the source Event
     */
    private void assignCoorganizer() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || TextUtils.isEmpty(sourceEventId) || TextUtils.isEmpty(viewedUid)) {
            showMessage(getString(R.string.assign_coorganizer_failed));
            return;
        }

        isAssigningCoorganizer = true;
        assignCoorganizerButton.setEnabled(false);
        UserProfile user = createUserProfile(viewedUid);
        batchUsers.add(user);
        repository.assignCoorganizer(sourceEventId, viewedUid, currentUser.getUid())
                .addOnSuccessListener(unused -> {
                    isAssigningCoorganizer = false;
                    assignCoorganizerButton.setEnabled(true);
                    sendNotifications();
                    showMessage(getString(R.string.assign_coorganizer_success));
                    finish();
                })
                .addOnFailureListener(exception -> {
                    isAssigningCoorganizer = false;
                    assignCoorganizerButton.setEnabled(true);
                    showMessage(getString(R.string.assign_coorganizer_failed));
                });

    }

    /**
     * sends invitation notifications to user currently in the batch
     */
    private void sendNotifications() {
        repository.getEventById(sourceEventId)
                .addOnSuccessListener(event -> {
                    String message = "You are invited to be a Coorganizer for the event: " + event.getTitle();
                    notificationRepository.sendCoOrganizerInvitation(sourceEventId, "Coorganizer Invitation", message, batchUsers)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Notification sent", Toast.LENGTH_SHORT).show();
                                batchUsers.clear();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to send notification:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    isAssigningCoorganizer = false;
                    assignCoorganizerButton.setEnabled(true);
                    showMessage(getString(R.string.assign_coorganizer_failed));
                });

    }

    /**
     * Creates a user profile object from a user ID
     *
     * @param UID ID of user's profile to be create
     * @return the profile object of the user
     */
    private UserProfile createUserProfile(String UID) {
        Task<DocumentSnapshot> loadingDocument = firestore.collection("users").document(UID).get();
        while (!loadingDocument.isComplete()) {
        }
        return readUserProfile(loadingDocument.getResult());
    }

    /**
     * reads in a user from the database into a UserProfile
     * @param doc
     * a reference to the user document to read data of
     * @return
     * the User profile of matching the document in the database
     */
    @NonNull
    private UserProfile readUserProfile(@NonNull DocumentSnapshot doc) {
        UserProfile userProfile = new UserProfile(
                normalize(doc.getString("fullName")),
                normalize(doc.getString("email")),
                normalize(doc.getString("username")),
                normalize(doc.getString("usernameKey")),
                normalize(doc.getString("phoneNumber")),
                normalize(doc.getString("accountType"))
        );
        userProfile.setUid(doc.getId());

        Timestamp createdAt = doc.getTimestamp("createdAt");
        if (createdAt != null) {
            userProfile.setCreatedAt(createdAt);
        }
        return userProfile;
    }

    /**
     * shows a popup message to the user
     * @param message
     * the message that will be displayed
     */
    private void showMessage(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * returns a fallback string if the given value is empty
     * @param value
     * the String value being checked
     * @param fallbackResId
     * the string resource used if the value is empty
     * @return
     * the given value, or the fallback string if it is empty
     */
    @NonNull
    private String safeValue(@NonNull String value, int fallbackResId) {
        if (TextUtils.isEmpty(value)) {
            return getString(fallbackResId);
        }
        return value;
    }

    /**
     * This method formats a long value into a readable date
     * @param createdAtMillis the long value that we want to format into the date
     * @return The formatted date
     */

    @NonNull
    private String formatJoinDate(long createdAtMillis) {
        if (createdAtMillis < 0L) {
            return getString(R.string.unknown_join_date);
        }
        return new SimpleDateFormat("MMM d, yyyy", Locale.US).format(new Date(createdAtMillis));
    }

    /**
     * This method just cleans up a string for us
     * @param value A string that we want to clean up
     * @return either an empty string if the value is null or the trimmed string
     */

    @NonNull
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
