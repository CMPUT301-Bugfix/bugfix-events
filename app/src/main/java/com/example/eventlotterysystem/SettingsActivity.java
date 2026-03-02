package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private TextView backButton;
    private LinearLayout updateInformationHeader;
    private TextView updateInformationIndicator;
    private LinearLayout updateInformationContent;
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

    private boolean isAccordionExpanded;
    private boolean isSaving;
    private boolean pendingSignOutAfterSave;
    private boolean profileLoaded;

    private UserProfile originalProfile = new UserProfile("", "", "", "", "");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        backButton = findViewById(R.id.settingsBackButton);
        updateInformationHeader = findViewById(R.id.updateInformationHeader);
        updateInformationIndicator = findViewById(R.id.updateInformationIndicator);
        updateInformationContent = findViewById(R.id.updateInformationContent);
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
        setAccordionExpanded(false);

        backButton.setOnClickListener(v -> finish());
        updateInformationHeader.setOnClickListener(v -> {
            if (!isSaving) {
                setAccordionExpanded(!isAccordionExpanded);
            }
        });
        saveProfileChangesButton.setOnClickListener(v -> onSaveClicked());
        deleteAccountButton.setOnClickListener(v -> onDeleteAccountClicked());

        loadCurrentProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshVerifiedEmailIfNeeded();
    }

    private void setAccordionExpanded(boolean expanded) {
        isAccordionExpanded = expanded;
        updateInformationContent.setVisibility(expanded ? View.VISIBLE : View.GONE);
        updateInformationIndicator.setText(getString(expanded ? R.string.hide : R.string.show));
    }

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
        profileLoaded = true;

        updateNameInput.setText(loadedName);
        updateEmailInput.setText(loadedEmail);
        updateUsernameInput.setText(loadedUsername);
        updatePhoneInput.setText(loadedPhone);
    }

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

    private void onDeleteAccountSuccess() {
        setSaving(false);
        showMessage(getString(R.string.account_deleted));
        auth.signOut();
        AuthSessionPreference.setRemember(this, false);
        navigateToAuthMenu();
    }

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

    private void handleDeleteAccountDataFailure(@NonNull Exception exception) {
        setSaving(false);
        showMessage(getString(R.string.account_delete_failed));
    }

    private void handleDeleteAccountAuthFailure(@NonNull Exception exception) {
        setSaving(false);
        showMessage(getString(R.string.account_delete_partial_retry));
    }

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

    private void clearErrors() {
        updateNameInput.setError(null);
        updateEmailInput.setError(null);
        updateUsernameInput.setError(null);
        currentPasswordInput.setError(null);
        newPasswordInput.setError(null);
        confirmNewPasswordInput.setError(null);
    }

    private void clearPasswordInputs() {
        currentPasswordInput.setText("");
        newPasswordInput.setText("");
        confirmNewPasswordInput.setText("");
    }

    private void setSaving(boolean saving) {
        isSaving = saving;
        loadingIndicator.setVisibility(saving ? ProgressBar.VISIBLE : ProgressBar.GONE);
        backButton.setEnabled(!saving);
        updateInformationHeader.setEnabled(!saving);
        updateNameInput.setEnabled(!saving);
        updateEmailInput.setEnabled(!saving);
        updateUsernameInput.setEnabled(!saving);
        updatePhoneInput.setEnabled(!saving);
        currentPasswordInput.setEnabled(!saving);
        newPasswordInput.setEnabled(!saving);
        confirmNewPasswordInput.setEnabled(!saving);
        saveProfileChangesButton.setEnabled(!saving);
        deleteAccountButton.setEnabled(!saving);
    }

    private void showMessage(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void navigateToAuthMenu() {
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
