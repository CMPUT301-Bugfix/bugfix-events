package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This is a class that is the controller of the activity_settings screen
 * this is the activity that allows a user to change their preference settings(when implemented) and account information
 * allows a user to create an account and then navigates them to home activity
 * also is where the user can delete their account
 */
public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private TextView backButton;
    private View updateInformationHeader;
    private TextView updateInformationIndicator;
    private ScrollView updateInformationContent;
    private EditText updateNameInput;
    private EditText updateEmailInput;
    private EditText updateUsernameInput;
    private EditText updatePhoneInput;
    private EditText currentPasswordInput;
    private EditText newPasswordInput;
    private EditText confirmNewPasswordInput;
    private Button saveProfileChangesButton;
    private Button deleteAccountButton;
    private ProgressBar loadingIndicator;

    // Notification Preferences
    private LinearLayout notificationPreferencesHeader;
    private TextView notificationPreferencesIndicator;
    private LinearLayout notificationPreferencesContent;
    private SwitchCompat optInCoorganizerInvitesSwitch;
    private SwitchCompat optInPrivateInvitesSwitch;
    private SwitchCompat optInWinningNotificationsSwitch;
    private SwitchCompat optInOtherNotificationsSwitch;
    private Button saveNotificationPreferencesButton;

    private boolean isAccordionExpanded;
    private boolean isNotifAccordionExpanded;
    private boolean isSaving;
    private boolean pendingSignOutAfterSave;
    private boolean profileLoaded;

    private UserProfile originalProfile = new UserProfile("", "", "", "", "");

    /**
     * This is the creation of the Activity
     * This connects to all the view on the screen and connects the clickable views to their controller
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        backButton = findViewById(R.id.settingsBackButton);
        updateInformationHeader = findViewById(R.id.updateInformationHeader);
        updateInformationIndicator = findViewById(R.id.updateInformationIndicator);
        updateInformationContent = findViewById(R.id.updateInformationContentScroll);
        updateNameInput = findViewById(R.id.updateNameInput);
        updateEmailInput = findViewById(R.id.updateEmailInput);
        updateUsernameInput = findViewById(R.id.updateUsernameInput);
        updatePhoneInput = findViewById(R.id.updatePhoneInput);
        currentPasswordInput = findViewById(R.id.currentPasswordInput);
        newPasswordInput = findViewById(R.id.newPasswordInput);
        confirmNewPasswordInput = findViewById(R.id.confirmNewPasswordInput);
        saveProfileChangesButton = findViewById(R.id.saveProfileChangesButton);
        deleteAccountButton = findViewById(R.id.deleteAccountButton);
        loadingIndicator = findViewById(R.id.settingsLoadingIndicator);

        // Notification Preference Views
        notificationPreferencesHeader = findViewById(R.id.notificationPreferencesHeader);
        notificationPreferencesIndicator = findViewById(R.id.notificationPreferencesIndicator);
        notificationPreferencesContent = findViewById(R.id.notificationPreferencesContent);
        optInCoorganizerInvitesSwitch = findViewById(R.id.optInCoorganizerInvitesSwitch);
        optInPrivateInvitesSwitch = findViewById(R.id.optInPrivateInvitesSwitch);
        optInWinningNotificationsSwitch = findViewById(R.id.optInWinningNotificationsSwitch);
        optInOtherNotificationsSwitch = findViewById(R.id.optInOtherNotificationsSwitch);
        saveNotificationPreferencesButton = findViewById(R.id.saveNotificationPreferencesButton);

        setAccordionExpanded(false);
        setNotifAccordionExpanded(false);

        backButton.setOnClickListener(v -> finish());
        updateInformationHeader.setOnClickListener(v -> {
            if (!isSaving) {
                setAccordionExpanded(!isAccordionExpanded);
            }
        });
        notificationPreferencesHeader.setOnClickListener(v -> {
            if (!isSaving) {
                setNotifAccordionExpanded(!isNotifAccordionExpanded);
            }
        });

        saveProfileChangesButton.setOnClickListener(v -> onSaveClicked());
        saveNotificationPreferencesButton.setOnClickListener(v -> onSaveNotifPreferencesClicked());
        deleteAccountButton.setOnClickListener(v -> onDeleteAccountClicked());

        loadCurrentProfile();
    }

    /**
     * This when the Activity is resumed (is returned to)
     * runs refreshVerifiedEmailIfNeeded to finish the process of changing emails
     */
    @Override
    protected void onResume() {
        super.onResume();
        refreshVerifiedEmailIfNeeded();
    }

    /**
     * managed the screen views to either show or hide the field that allow changing user information
     * @param expanded
     * whether the form should be expanded to user information fields and allowing editing
     */
    private void setAccordionExpanded(boolean expanded) {
        isAccordionExpanded = expanded;
        updateInformationContent.setVisibility(expanded ? View.VISIBLE : View.GONE);
        updateInformationIndicator.setText(getString(expanded ? R.string.hide : R.string.show));
    }

    /**
     * manages the visibility of the notification preferences accordion
     * @param expanded true to show, false to hide
     */
    private void setNotifAccordionExpanded(boolean expanded) {
        isNotifAccordionExpanded = expanded;
        notificationPreferencesContent.setVisibility(expanded ? View.VISIBLE : View.GONE);
        notificationPreferencesIndicator.setText(getString(expanded ? R.string.hide : R.string.show));
    }

    /**
     * get a pointer to the user profile document and runs applyLoadedProfile if successful
     * notifies user if the was an error in the process
     */
    private void loadCurrentProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            showMessage(getString(R.string.session_expired_sign_in_again));
            finish();
            return;
        }

        String authEmail = normalize(currentUser.getEmail()).toLowerCase(Locale.US);
        if (!TextUtils.isEmpty(authEmail)) {
            updateEmailInput.setText(authEmail);
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> applyLoadedProfile(snapshot, authEmail))
                .addOnFailureListener(exception -> showMessage(getString(R.string.unexpected_error)));
    }

    /**
     * loads the user profile information from the database to populate the input form with the users current information
     * also crate the UserProfile object matching the database data
     * @param snapshot
     * a reference to the user profile document in users
     * @param authEmail
     * the email that is being used to authenticate the changes
     */
    private void applyLoadedProfile(@NonNull DocumentSnapshot snapshot, @NonNull String authEmail) {
        String loadedName = normalize(snapshot.getString("fullName"));
        String loadedEmail = normalize(snapshot.getString("email")).toLowerCase(Locale.US);
        String loadedUsername = normalize(snapshot.getString("username"));
        String loadedUsernameKey = normalize(snapshot.getString("usernameKey")).toLowerCase(Locale.US);
        String loadedPhone = normalize(snapshot.getString("phoneNumber"));
        String loadedAccountType = normalize(snapshot.getString("accountType"));

        if (TextUtils.isEmpty(loadedEmail)) {
            loadedEmail = authEmail;
        }
        if (!TextUtils.isEmpty(authEmail)) {
            loadedEmail = authEmail;
        }
        if (TextUtils.isEmpty(loadedUsername) && !TextUtils.isEmpty(loadedUsernameKey)) {
            loadedUsername = loadedUsernameKey;
        }
        if (TextUtils.isEmpty(loadedUsernameKey) && !TextUtils.isEmpty(loadedUsername)) {
            loadedUsernameKey = loadedUsername.toLowerCase(Locale.US);
        }

        originalProfile = new UserProfile(loadedName, loadedEmail, loadedUsername, loadedUsernameKey, loadedPhone, loadedAccountType);
        
        // Load notification preferences
        originalProfile.setOptInCoorganizerInvites(snapshot.contains("optInCoorganizerInvites") ? Boolean.TRUE.equals(snapshot.getBoolean("optInCoorganizerInvites")) : true);
        originalProfile.setOptInPrivateInvites(snapshot.contains("optInPrivateInvites") ? Boolean.TRUE.equals(snapshot.getBoolean("optInPrivateInvites")) : true);
        originalProfile.setOptInWinningNotifications(snapshot.contains("optInWinningNotifications") ? Boolean.TRUE.equals(snapshot.getBoolean("optInWinningNotifications")) : true);
        originalProfile.setOptInOtherNotifications(snapshot.contains("optInOtherNotifications") ? Boolean.TRUE.equals(snapshot.getBoolean("optInOtherNotifications")) : true);

        profileLoaded = true;

        updateNameInput.setText(loadedName);
        updateEmailInput.setText(loadedEmail);
        updateUsernameInput.setText(loadedUsername);
        updatePhoneInput.setText(loadedPhone);

        // Apply notification preference switches
        optInCoorganizerInvitesSwitch.setChecked(originalProfile.isOptInCoorganizerInvites());
        optInPrivateInvitesSwitch.setChecked(originalProfile.isOptInPrivateInvites());
        optInWinningNotificationsSwitch.setChecked(originalProfile.isOptInWinningNotifications());
        optInOtherNotificationsSwitch.setChecked(originalProfile.isOptInOtherNotifications());
    }

    /**
     * saves the user's notification opt-in preferences to Firestore
     */
    private void onSaveNotifPreferencesClicked() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        setSaving(true);
        Map<String, Object> updates = new HashMap<>();
        updates.put("optInCoorganizerInvites", optInCoorganizerInvitesSwitch.isChecked());
        updates.put("optInPrivateInvites", optInPrivateInvitesSwitch.isChecked());
        updates.put("optInWinningNotifications", optInWinningNotificationsSwitch.isChecked());
        updates.put("optInOtherNotifications", optInOtherNotificationsSwitch.isChecked());

        firestore.collection("users").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    setSaving(false);
                    originalProfile.setOptInCoorganizerInvites(optInCoorganizerInvitesSwitch.isChecked());
                    originalProfile.setOptInPrivateInvites(optInPrivateInvitesSwitch.isChecked());
                    originalProfile.setOptInWinningNotifications(optInWinningNotificationsSwitch.isChecked());
                    originalProfile.setOptInOtherNotifications(optInOtherNotificationsSwitch.isChecked());
                    showMessage("Notification preferences updated");
                })
                .addOnFailureListener(e -> {
                    setSaving(false);
                    showMessage("Failed to update preferences");
                });
    }

    /**
     * This removes the extra field of pendingEmail for user & usernames collection
     * this is run once verification to change the email has occurred
     * give the user a popup once the change has been completed notifying the completion
     */
    private void refreshVerifiedEmailIfNeeded() {
        if (!profileLoaded || isSaving) {
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        currentUser.reload().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                return;
            }

            String refreshedEmail = normalize(currentUser.getEmail()).toLowerCase(Locale.US);
            if (TextUtils.isEmpty(refreshedEmail) || refreshedEmail.equals(originalProfile.getEmail())) {
                return;
            }

            WriteBatch batch = firestore.batch();
            DocumentReference userRef = firestore.collection("users").document(currentUser.getUid());
            Map<String, Object> userUpdates = new HashMap<>();
            userUpdates.put("email", refreshedEmail);
            userUpdates.put("pendingEmail", FieldValue.delete());
            batch.set(userRef, userUpdates, SetOptions.merge());

            if (!TextUtils.isEmpty(originalProfile.getUsernameKey())) {
                DocumentReference usernameRef = firestore.collection("usernames")
                        .document(originalProfile.getUsernameKey());
                Map<String, Object> usernameUpdates = new HashMap<>();
                usernameUpdates.put("uid", currentUser.getUid());
                usernameUpdates.put("email", refreshedEmail);
                usernameUpdates.put("pendingEmail", FieldValue.delete());
                batch.set(usernameRef, usernameUpdates, SetOptions.merge());
            }

            batch.commit().addOnSuccessListener(unused -> {
                originalProfile.setEmail(refreshedEmail);
                updateEmailInput.setText(refreshedEmail);
                showMessage(getString(R.string.email_change_completed));
            });
        });
    }

    /**
     * This is a controller for when saveProfileChangesButton is pressed
     * get the user input for all the input fields
     * validates that all necessary fields are filled
     * creates new user profile account object with the changed information
     * manages what methods need to be called for proper level of verification and which writes to the database need to be done
     */
    private void onSaveClicked() {
        clearErrors();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            showMessage(getString(R.string.session_expired_sign_in_again));
            finish();
            return;
        }

        String updatedName = updateNameInput.getText().toString().trim();
        String updatedEmail = updateEmailInput.getText().toString().trim().toLowerCase(Locale.US);
        String updatedUsername = updateUsernameInput.getText().toString().trim();
        String updatedUsernameKey = updatedUsername.toLowerCase(Locale.US);
        String updatedPhone = updatePhoneInput.getText().toString().trim();
        String currentPassword = currentPasswordInput.getText().toString();
        String newPassword = newPasswordInput.getText().toString();
        String confirmNewPassword = confirmNewPasswordInput.getText().toString();

        if (TextUtils.isEmpty(updatedName)) {
            updateNameInput.setError(getString(R.string.field_required));
            updateNameInput.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(updatedEmail).matches()) {
            updateEmailInput.setError(getString(R.string.invalid_email));
            updateEmailInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(updatedUsername)) {
            updateUsernameInput.setError(getString(R.string.field_required));
            updateUsernameInput.requestFocus();
            return;
        }

        boolean wantsPasswordChange = !TextUtils.isEmpty(newPassword) || !TextUtils.isEmpty(confirmNewPassword);
        if (wantsPasswordChange) {
            if (TextUtils.isEmpty(newPassword)) {
                newPasswordInput.setError(getString(R.string.field_required));
                newPasswordInput.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(confirmNewPassword)) {
                confirmNewPasswordInput.setError(getString(R.string.field_required));
                confirmNewPasswordInput.requestFocus();
                return;
            }
            if (!newPassword.equals(confirmNewPassword)) {
                confirmNewPasswordInput.setError(getString(R.string.passwords_do_not_match));
                confirmNewPasswordInput.requestFocus();
                return;
            }
            if (newPassword.equals(currentPassword)) {
                newPasswordInput.setError(getString(R.string.new_password_must_differ));
                newPasswordInput.requestFocus();
                return;
            }
        }

        boolean emailChanged = !updatedEmail.equals(originalProfile.getEmail());
        boolean usernameChanged = !updatedUsernameKey.equals(originalProfile.getUsernameKey());
        boolean requiresReauth = emailChanged || wantsPasswordChange;
        if (requiresReauth && TextUtils.isEmpty(currentPassword)) {
            currentPasswordInput.setError(getString(R.string.current_password_required_for_sensitive_changes));
            currentPasswordInput.requestFocus();
            return;
        }

        if (updatedName.equals(originalProfile.getName())
                && updatedPhone.equals(originalProfile.getPhoneNumber())
                && !emailChanged
                && !usernameChanged
                && updatedUsername.equals(originalProfile.getUsername())
                && !wantsPasswordChange) {
            showMessage(getString(R.string.no_changes_to_save));
            return;
        }

        UserProfile editedProfile = new UserProfile(updatedName, updatedEmail, updatedUsername, updatedUsernameKey, updatedPhone, originalProfile.getAccountType());

        setSaving(true);
        pendingSignOutAfterSave = false;
        if (requiresReauth) {
            reauthenticateCurrentUser(currentUser, currentPassword, () -> updateAuthAndSave(currentUser, editedProfile, emailChanged, wantsPasswordChange, newPassword, true));
            return;
        }

        updateAuthAndSave(currentUser, editedProfile, emailChanged, wantsPasswordChange, newPassword, false);
    }

    /**
     * This is a controller for when deleteAccountButton is pressed
     * crates confirmation popup requiring password to proceed
     * if user confirms process runs deleteAccount to start the deletion process
     */
    private void onDeleteAccountClicked() {
        if (isSaving) {
            return;
        }

        EditText passwordInput = new EditText(this);
        passwordInput.setHint(getString(R.string.current_password));
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account_confirm_title)
                .setMessage(R.string.delete_account_confirm_message)
                .setView(passwordInput)
                .setNegativeButton(R.string.delete_account_cancel_action, (d, which) -> d.dismiss())
                .setPositiveButton(R.string.delete_account_confirm_action, null)
                .create();

        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString();
            if (TextUtils.isEmpty(password)) {
                passwordInput.setError(getString(R.string.delete_account_requires_password));
                passwordInput.requestFocus();
                return;
            }
            dialog.dismiss();
            deleteAccount(password);
        }));
        dialog.show();
    }

    /**
     * start management of account deletion
     * ensure that the pointer to the user is still connected and manages confirmation of deletion
     * @param currentPassword
     * the original password before any user profile changes
     */
    private void deleteAccount(@NonNull String currentPassword) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            showMessage(getString(R.string.session_expired_sign_in_again));
            navigateToAuthMenu();
            return;
        }

        String authEmail = normalize(currentUser.getEmail()).toLowerCase(Locale.US);
        if (TextUtils.isEmpty(authEmail)) {
            showMessage(getString(R.string.unexpected_error));
            return;
        }

        setSaving(true);
        pendingSignOutAfterSave = false;
        AuthCredential credential = EmailAuthProvider.getCredential(authEmail, currentPassword);
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(unused -> deleteAccountProfileAndAuth(currentUser))
                .addOnFailureListener(this::handleDeleteAccountReauthFailure);
    }

    /**
     * writes to the database to delete the user profile and account information
     * does not actually delete the account just tags it as a deleted one so that it cant be used
     * @param currentUser
     * pointer to the user in the database
     */
    private void deleteAccountProfileAndAuth(@NonNull FirebaseUser currentUser) {
        WriteBatch batch = firestore.batch();
        String uid = currentUser.getUid();
        batch.delete(firestore.collection("users").document(uid));

        String usernameKey = normalize(originalProfile.getUsernameKey()).toLowerCase(Locale.US);
        if (TextUtils.isEmpty(usernameKey)) {
            usernameKey = normalize(updateUsernameInput.getText() == null
                    ? ""
                    : updateUsernameInput.getText().toString()).toLowerCase(Locale.US);
        }
        if (!TextUtils.isEmpty(usernameKey)) {
            batch.delete(firestore.collection("usernames").document(usernameKey));
        }

        batch.commit()
                .addOnSuccessListener(unused -> currentUser.delete()
                        .addOnSuccessListener(deleteUnused -> onDeleteAccountSuccess())
                        .addOnFailureListener(this::handleDeleteAccountAuthFailure))
                .addOnFailureListener(this::handleDeleteAccountDataFailure);
    }

    /**
     * runs showMessage to inform user that the deletion was a success
     * runs navigateToAuthMenu to exit user from account
     * also turns of saving mode for the screen
     */
    private void onDeleteAccountSuccess() {
        setSaving(false);
        showMessage(getString(R.string.account_deleted));
        auth.signOut();
        AuthSessionPreference.setRemember(this, false);
        navigateToAuthMenu();
    }

    /** NOT
     * handle an exception when a user fails to validate confirmation of profile deletion
     * runs showMessage to inform user of the cause for the failure
     * @param exception
     * what the cause for the failure was
     */
    private void handleDeleteAccountReauthFailure(@NonNull Exception exception) {
        setSaving(false);
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            showMessage(getString(R.string.current_password_incorrect));
            return;
        }
        if (exception instanceof FirebaseAuthRecentLoginRequiredException) {
            showMessage(getString(R.string.session_expired_sign_in_again));
            return;
        }
        showMessage(getString(R.string.unexpected_error));
    }

    /**
     * handle an exception when a an attempt to delete a profile fails
     * runs showMessage to inform user of the cause for the failure
     * @param exception
     * what the cause for the failure was
     */
    private void handleDeleteAccountDataFailure(@NonNull Exception exception) {
        setSaving(false);
        showMessage(getString(R.string.account_delete_failed));
    }

    /**
     * handle an exception when account is only partially deleted instead of full
     * runs showMessage to inform user of the cause for the failure
     * @param exception
     * what the cause for the failure was
     */
    private void handleDeleteAccountAuthFailure(@NonNull Exception exception) {
        setSaving(false);
        showMessage(getString(R.string.account_delete_partial_retry));
    }

    /**
     * prompt user for credentials for authentication for changes to key login fields
     * @param currentUser
     * pointer to the user in the database
     * @param currentPassword
     * the original password before any user profile changes
     * @param onSuccess
     * that task that is to be run if the changes are authenticated
     */
    private void reauthenticateCurrentUser(
            @NonNull FirebaseUser currentUser,
            @NonNull String currentPassword,
            @NonNull Runnable onSuccess
    ) {
        String authEmail = normalize(currentUser.getEmail()).toLowerCase(Locale.US);
        if (TextUtils.isEmpty(authEmail)) {
            handleReauthFailure(new IllegalStateException("Missing authenticated email"));
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(authEmail, currentPassword);
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(this::handleReauthFailure);
    }

    /**
     * if the email is also being changed runs email verification for changed email
     * afterwards runs updatePasswordAndSave to continue the preparation before changing the data base
     * @param currentUser
     * pointer to the user in the database
     * @param editedProfile
     * userprofile account object with user inputted changes
     * @param emailChanged
     * if the email is being changed
     * @param wantsPasswordChange
     * if the password is being changed
     * @param newPassword
     * the new password that the user changed to will be "" if password is not being changed
     * @param hadAuthSensitiveChange
     * whether one of the changes to the user profile is used for signing in
     */
    private void updateAuthAndSave(
            @NonNull FirebaseUser currentUser,
            @NonNull UserProfile editedProfile,
            boolean emailChanged,
            boolean wantsPasswordChange,
            @NonNull String newPassword,
            boolean hadAuthSensitiveChange
    ) {
        if (emailChanged) {
            currentUser.verifyBeforeUpdateEmail(editedProfile.getEmail())
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Exception exception = task.getException();
                            if (exception != null) {
                                handleAuthFailure(exception);
                            } else {
                                showMessage(getString(R.string.unexpected_error));
                            }
                            return;
                        }
                        showMessage(getString(R.string.verify_email_change_sent));
                        pendingSignOutAfterSave = true;
                        updatePasswordAndSave(currentUser, editedProfile, wantsPasswordChange, newPassword, hadAuthSensitiveChange);
                    });
            return;
        }

        updatePasswordAndSave(currentUser, editedProfile, wantsPasswordChange, newPassword, hadAuthSensitiveChange);
    }

    /**
     * manages whether updatePassword password need to be run
     * or if can go straight manging writing to database with syncProfileToFirestore
     * @param currentUser
     * pointer to the user in the database
     * @param editedProfile
     * userprofile account object with user inputted changes
     * @param wantsPasswordChange
     * if the password is being changed
     * @param newPassword
     * the string of the new password
     * @param hadAuthSensitiveChange
     * whether one of the changes to the user profile is used for signing in
     */
    private void updatePasswordAndSave(
            @NonNull FirebaseUser currentUser,
            @NonNull UserProfile editedProfile,
            boolean wantsPasswordChange,
            @NonNull String newPassword,
            boolean hadAuthSensitiveChange
    ) {
        if (!wantsPasswordChange) {
            syncProfileToFirestore(currentUser, editedProfile, hadAuthSensitiveChange);
            return;
        }

        currentUser.updatePassword(newPassword)
                .addOnSuccessListener(unused -> syncProfileToFirestore(currentUser, editedProfile, hadAuthSensitiveChange))
                .addOnFailureListener(this::handleAuthFailure);
    }

    /**
     * creates the the userUpdates hash that will be used to update fields in the database
     * depending on which fields were changed manges whether  usernames collection also need to be updated
     * @param currentUser
     * pointer to the user in the database
     * @param editedProfile
     * userprofile account object with user inputted changes
     * @param hadAuthSensitiveChange
     * whether one of the changes to the user profile is used for signing in
     */
    private void syncProfileToFirestore(
            @NonNull FirebaseUser currentUser,
            @NonNull UserProfile editedProfile,
            boolean hadAuthSensitiveChange
    ) {
        boolean emailVerificationPending = pendingSignOutAfterSave
                && !editedProfile.getEmail().equals(originalProfile.getEmail());

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("fullName", editedProfile.getName());
        userUpdates.put("username", editedProfile.getUsername());
        userUpdates.put("usernameKey", editedProfile.getUsernameKey());
        if (emailVerificationPending) {
            userUpdates.put("email", originalProfile.getEmail());
            userUpdates.put("pendingEmail", editedProfile.getEmail());
        } else {
            userUpdates.put("email", editedProfile.getEmail());
            userUpdates.put("pendingEmail", FieldValue.delete());
        }
        boolean usernameChanged = !editedProfile.getUsernameKey().equals(originalProfile.getUsernameKey());
        if (TextUtils.isEmpty(editedProfile.getPhoneNumber())) {
            userUpdates.put("phoneNumber", FieldValue.delete());
        } else {
            userUpdates.put("phoneNumber", editedProfile.getPhoneNumber());
        }

        if (usernameChanged) {
            moveUsernameMapping(currentUser, editedProfile, userUpdates, emailVerificationPending, hadAuthSensitiveChange);
            return;
        }
        saveWithoutUsernameMove(currentUser, editedProfile, userUpdates, emailVerificationPending, hadAuthSensitiveChange);
    }

    /**
     * updates the usernames database to create new mapping of username to user id
     * if there is a user already with the name stops writing and runs handleProfileSyncFailure
     * otherwise continues writing with through onProfileSaved
     * @param currentUser
     * pointer to the user in the database
     * @param editedProfile
     * userprofile account object with user inputted changes
     * @param userUpdates
     * a Map to organize the write to update fields in the database
     * @param emailVerificationPending
     * whether there was an email send out to verify the changes
     * @param hadAuthSensitiveChange
     * whether one of the changes to the user profile is used for signing in
     */
    private void moveUsernameMapping(
            @NonNull FirebaseUser currentUser,
            @NonNull UserProfile editedProfile,
            @NonNull Map<String, Object> userUpdates,
            boolean emailVerificationPending,
            boolean hadAuthSensitiveChange
    ) {
        DocumentReference userRef = firestore.collection("users").document(currentUser.getUid());
        DocumentReference newUsernameRef = firestore.collection("usernames").document(editedProfile.getUsernameKey());
        DocumentReference oldUsernameRef = TextUtils.isEmpty(originalProfile.getUsernameKey())
                ? null
                : firestore.collection("usernames").document(originalProfile.getUsernameKey());

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot newUsernameSnapshot = transaction.get(newUsernameRef);
                    if (newUsernameSnapshot.exists()) {
                        String existingUid = normalize(newUsernameSnapshot.getString("uid"));
                        if (!currentUser.getUid().equals(existingUid)) {
                            throw new FirebaseFirestoreException(
                                    "Username already exists",
                                    FirebaseFirestoreException.Code.ALREADY_EXISTS
                            );
                        }
                    }

                    Map<String, Object> usernamePayload = new HashMap<>();
                    usernamePayload.put("uid", currentUser.getUid());
                    if (emailVerificationPending) {
                        usernamePayload.put("email", originalProfile.getEmail());
                        usernamePayload.put("pendingEmail", editedProfile.getEmail());
                    } else {
                        usernamePayload.put("email", editedProfile.getEmail());
                        usernamePayload.put("pendingEmail", FieldValue.delete());
                    }
                    if (!newUsernameSnapshot.exists()) {
                        usernamePayload.put("createdAt", FieldValue.serverTimestamp());
                    }
                    transaction.set(newUsernameRef, usernamePayload, SetOptions.merge());

                    if (oldUsernameRef != null
                            && !originalProfile.getUsernameKey().equals(editedProfile.getUsernameKey())) {
                        transaction.delete(oldUsernameRef);
                    }
                    transaction.set(userRef, userUpdates, SetOptions.merge());
                    return null;
                })
                .addOnSuccessListener(unused -> onProfileSaved(editedProfile))
                .addOnFailureListener(exception -> handleProfileSyncFailure(exception, hadAuthSensitiveChange));
    }

    /**
     * writes to the changes to user in users profile
     * if hadAuthSensitiveChange is true also adds the new pending email the usernames collection (which manages authorization)
     * @param currentUser
     * pointer to the user in the database
     * @param editedProfile
     * userprofile account object with user inputted changes
     * @param userUpdates
     * a Map to organize the write to update fields in the database
     * @param emailVerificationPending
     * whether there was an email send out to verify the changes
     * @param hadAuthSensitiveChange
     * whether one of the changes to the user profile is used for signing in
     */
    private void saveWithoutUsernameMove(
            @NonNull FirebaseUser currentUser,
            @NonNull UserProfile editedProfile,
            @NonNull Map<String, Object> userUpdates,
            boolean emailVerificationPending,
            boolean hadAuthSensitiveChange
    ) {
        WriteBatch batch = firestore.batch();
        DocumentReference userRef = firestore.collection("users").document(currentUser.getUid());
        batch.set(userRef, userUpdates, SetOptions.merge());

        if (emailVerificationPending && !TextUtils.isEmpty(editedProfile.getUsernameKey())) {
            DocumentReference usernameRef = firestore.collection("usernames").document(editedProfile.getUsernameKey());
            Map<String, Object> usernameUpdates = new HashMap<>();
            usernameUpdates.put("uid", currentUser.getUid());
            usernameUpdates.put("email", originalProfile.getEmail());
            usernameUpdates.put("pendingEmail", editedProfile.getEmail());
            batch.set(usernameRef, usernameUpdates, SetOptions.merge());
        }

        batch.commit()
                .addOnSuccessListener(unused -> onProfileSaved(editedProfile))
                .addOnFailureListener(exception -> handleProfileSyncFailure(exception, hadAuthSensitiveChange));
    }

    /**
     * updates the views to their original state(but with changed account information)
     * notifies user of the success of the change
     * signs the user out(by navigating to AuthMenu if a sign-out was pending
     * @param editedProfile
     * user Profile object that has the changed profile information
     */
    private void onProfileSaved(@NonNull UserProfile editedProfile) {
        setSaving(false);
        originalProfile = editedProfile;

        updateNameInput.setText(editedProfile.getName());
        updateEmailInput.setText(editedProfile.getEmail());
        updateUsernameInput.setText(editedProfile.getUsername());
        updatePhoneInput.setText(editedProfile.getPhoneNumber());
        clearPasswordInputs();

        if (pendingSignOutAfterSave) {
            pendingSignOutAfterSave = false;
            auth.signOut();
            AuthSessionPreference.setRemember(this, false);
            navigateToAuthMenu();
            return;
        }

        showMessage(getString(R.string.profile_updated));
    }

    /**
     * for when the authentication for a when a password/email change fails notifies user of the failure
     * @param exception
     * the exception that was created by a user no longer being authenticated for the profile they are not
     */
    private void handleReauthFailure(@NonNull Exception exception) {
        setSaving(false);
        pendingSignOutAfterSave = false;
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            currentPasswordInput.setError(getString(R.string.current_password_incorrect));
            currentPasswordInput.requestFocus();
            return;
        }
        if (exception instanceof FirebaseAuthRecentLoginRequiredException) {
            showMessage(getString(R.string.session_expired_sign_in_again));
            return;
        }
        showMessage(getString(R.string.unexpected_error));
    }

    /**
     * handles exceptions that led to write failure to database and notifies the user of the cause
     * special popup text for matching email, password issues
     * @param exception
     * the exception that was created from the write attempt
     */
    private void handleAuthFailure(@NonNull Exception exception) {
        setSaving(false);
        pendingSignOutAfterSave = false;
        if (exception instanceof FirebaseAuthUserCollisionException) {
            updateEmailInput.setError(getString(R.string.email_already_in_use));
            updateEmailInput.requestFocus();
            return;
        }
        if (exception instanceof FirebaseAuthWeakPasswordException) {
            newPasswordInput.setError(getString(R.string.weak_password));
            newPasswordInput.requestFocus();
            return;
        }
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            currentPasswordInput.setError(getString(R.string.current_password_incorrect));
            currentPasswordInput.requestFocus();
            return;
        }
        if (exception instanceof FirebaseAuthRecentLoginRequiredException) {
            currentPasswordInput.setError(getString(R.string.current_password_required_for_sensitive_changes));
            currentPasswordInput.requestFocus();
            return;
        }
        showMessage(getString(R.string.unexpected_error));
    }

    /**
     * notifies user that some (or all) of the changed account information failed to sync and there was not updated
     * @param exception
     * The exception that caused the failure (ie a user with that username already exists)
     * @param hadAuthSensitiveChange
     * Whether the write to the database changed authentication data (first stage or write)
     */
    private void handleProfileSyncFailure(@NonNull Exception exception, boolean hadAuthSensitiveChange) {
        setSaving(false);
        pendingSignOutAfterSave = false;
        if (exception instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) exception).getCode()
                == FirebaseFirestoreException.Code.ALREADY_EXISTS) {
            updateUsernameInput.setError(getString(R.string.username_taken));
            updateUsernameInput.requestFocus();
            return;
        }

        showMessage(getString(hadAuthSensitiveChange
                ? R.string.profile_sync_failed_retry
                : R.string.profile_update_failed_retry));
    }

    /**
     * methods clears all errors from input fields without managing them
     */
    private void clearErrors() {
        updateNameInput.setError(null);
        updateEmailInput.setError(null);
        updateUsernameInput.setError(null);
        currentPasswordInput.setError(null);
        newPasswordInput.setError(null);
        confirmNewPasswordInput.setError(null);
    }

    /**
     * sets the password input views to be blank (back to initial state)
     */
    private void clearPasswordInputs() {
        currentPasswordInput.setText("");
        newPasswordInput.setText("");
        confirmNewPasswordInput.setText("");
    }

    /**
     * method removes the interactivity of interactive views when saves changes so the fields data is not modified
     * @param saving
     * whether the app is in the process of writing to the database or not
     */
    private void setSaving(boolean saving) {
        isSaving = saving;
        loadingIndicator.setVisibility(saving ? ProgressBar.VISIBLE : ProgressBar.GONE);
        backButton.setEnabled(!saving);
        updateInformationHeader.setEnabled(!saving);
        
        // Ensure header is not null before using
        if (notificationPreferencesHeader != null) {
            notificationPreferencesHeader.setEnabled(!saving);
        }
        
        updateNameInput.setEnabled(!saving);
        updateEmailInput.setEnabled(!saving);
        updateUsernameInput.setEnabled(!saving);
        updatePhoneInput.setEnabled(!saving);
        currentPasswordInput.setEnabled(!saving);
        newPasswordInput.setEnabled(!saving);
        confirmNewPasswordInput.setEnabled(!saving);
        saveProfileChangesButton.setEnabled(!saving);
        
        // Ensure save button is not null before using
        if (saveNotificationPreferencesButton != null) {
            saveNotificationPreferencesButton.setEnabled(!saving);
        }

        deleteAccountButton.setEnabled(!saving);
    }

    /**
     * display message to user through toast popup
     * @param message
     * the message to be displayed
     */
    private void showMessage(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * navigates user to AuthMenuActivity (signup/login prompt) and finishes this activity
     */
    private void navigateToAuthMenu() {
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * removes extraneous whitespace and ensures that string is non-null
     * @param value
     * String to be cleaned
     * @return
     * the cleaned up String
     */
    @NonNull
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
