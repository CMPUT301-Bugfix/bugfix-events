package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This is a class that is the controller of the activity_login screen
 * it allows the user to put in credentials to login their account
 */
public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private EditText loginIdentifierInput;
    private EditText loginPasswordInput;
    private CheckBox rememberMeCheckBox;
    private ProgressBar loadingIndicator;
    private Button loginButton;
    private TextView forgotPasswordButton;
    private TextView backButton;

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
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        loginIdentifierInput = findViewById(R.id.loginIdentifierInput);
        loginPasswordInput = findViewById(R.id.loginPasswordInput);
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        loginButton = findViewById(R.id.loginButton);
        forgotPasswordButton = findViewById(R.id.forgotPasswordButton);
        backButton = findViewById(R.id.loginBackButton);

        rememberMeCheckBox.setChecked(AuthSessionPreference.shouldRemember(this));

        backButton.setOnClickListener(v -> finish());
        loginButton.setOnClickListener(v -> onLoginClicked());
        forgotPasswordButton.setOnClickListener(v -> onForgotPasswordClicked());
    }

    /**
     * This is a controller for when loginButton is pressed
     * ensures the user input fields are entered
     * run resolveEmailFromIdentifier to validate login attempt
     */
    private void onLoginClicked() {
        clearErrors();

        String identifier = readTrimmed(loginIdentifierInput);
        String password = readRaw(loginPasswordInput);

        if (TextUtils.isEmpty(identifier)) {
            loginIdentifierInput.setError(getString(R.string.field_required));
            loginIdentifierInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            loginPasswordInput.setError(getString(R.string.field_required));
            loginPasswordInput.requestFocus();
            return;
        }

        setLoading(true);
        resolveEmailFromIdentifier(identifier, new EmailResolverCallback() {

            /**
             * if email is obtain found thorough identifier match in database signs in using that email
             * @param email
             * the email of user
             * @param pendingEmail
             * the new email of user if email changed else an empty string
             */
            @Override
            public void onResolved(@NonNull String email, @NonNull String pendingEmail) {
                signInWithPendingEmail(email, password, pendingEmail);
            }

            /**
             * runs showMessage to show the error message on screen
             * @param message
             * the string describing the error
             */
            @Override
            public void onError(@NonNull String message) {
                setLoading(false);
                showMessage(message);
            }
        });
    }

    /**
     * This is a controller for when forgotPasswordButton is pressed
     * runs resolveEmailFromIdentifier to send a password reset to the matching email in usernames
     */
    private void onForgotPasswordClicked() {
        String identifier = readTrimmed(loginIdentifierInput);
        if (TextUtils.isEmpty(identifier)) {
            loginIdentifierInput.setError(getString(R.string.field_required));
            loginIdentifierInput.requestFocus();
            return;
        }

        setLoading(true);
        resolveEmailFromIdentifier(identifier, new EmailResolverCallback() {
            /**
             * if email is obtain found thorough identifier match in database send password reset to that email
             * @param email
             * the email of user
             * @param pendingEmail
             * the new email of user user changes email
             */
            @Override
            public void onResolved(@NonNull String email, @NonNull String pendingEmail) {
                auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        showMessage(getString(R.string.reset_email_sent));
                    } else {
                        showMessage(getString(R.string.unexpected_error));
                    }
                });
            }

            /**
             * runs showMessage to show the error message on screen
             * @param message
             * the string describing the error
             */
            @Override
            public void onError(@NonNull String message) {
                setLoading(false);
                showMessage(message);
            }
        });
    }

    /**
     * Attempts to match identifier to a stored email in Usernames collection of the Database
     * @param identifier
     * String of either email or Username
     * @param callback
     * EmailResolverCallback how the method should resolve successful and failed attempts
     */
    private void resolveEmailFromIdentifier(
            @NonNull String identifier,
            @NonNull EmailResolverCallback callback
    ) {
        String normalized = identifier.trim().toLowerCase(Locale.US);
        if (normalized.contains("@")) {
            callback.onResolved(normalized, "");
            return;
        }

        firestore.collection("usernames")
                .document(normalized)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String email = normalize(snapshot.getString("email")).toLowerCase(Locale.US);
                    String pendingEmail = normalize(snapshot.getString("pendingEmail"))
                            .toLowerCase(Locale.US);
                    if (TextUtils.isEmpty(email)) {
                        callback.onError(getString(R.string.auth_failed));
                        return;
                    }
                    if (pendingEmail.equals(email)) {
                        pendingEmail = "";
                    }
                    callback.onResolved(email, pendingEmail);
                })
                .addOnFailureListener(exception -> callback.onError(getString(R.string.unexpected_error)));
    }

    /**
     * log user in through pending email and runs syncUsernameEmailMappingForCurrentUser to convert pending email to primary
     * @param primaryEmail
     * the current/past email of user
     * @param password
     * the potential password that need to match with email to log into account
     * @param pendingEmail
     * the new updated email if there is one else an empty string
     */
    private void signInWithPendingEmail(
            @NonNull String primaryEmail,
            @NonNull String password,
            @NonNull String pendingEmail
    ) {
        auth.signInWithEmailAndPassword(primaryEmail, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        syncUsernameEmailMappingForCurrentUser(() -> onLoginSuccess());
                        return;
                    }

                    if (TextUtils.isEmpty(pendingEmail)
                            || primaryEmail.equalsIgnoreCase(pendingEmail)) {
                        setLoading(false);
                        showAuthError();
                        return;
                    }

                    auth.signInWithEmailAndPassword(pendingEmail, password)
                            .addOnCompleteListener(pendingEmailTask -> {
                                if (!pendingEmailTask.isSuccessful()) {
                                    setLoading(false);
                                    showAuthError();
                                    return;
                                }
                                syncUsernameEmailMappingForCurrentUser(() -> onLoginSuccess());
                            });
                });
    }

    /**
     * when credentials are verified get User data from database
     * if user data cannot be gotten runs showAuthError()
     */
    private void onLoginSuccess() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            setLoading(false);
            showAuthError();
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean deleted = !snapshot.exists() || Boolean.TRUE.equals(snapshot.getBoolean("deleted"));
                    if (deleted) {
                        setLoading(false);
                        auth.signOut();
                        AuthSessionPreference.setRemember(LoginActivity.this, false);
                        navigateAndClearTask(ProfileRemovedActivity.class);
                        return;
                    }

                    setLoading(false);
                    AuthSessionPreference.setRemember(LoginActivity.this, rememberMeCheckBox.isChecked());
                    navigateAndClearTask(HomeActivity.class);
                })
                .addOnFailureListener(exception -> {
                    setLoading(false);
                    showMessage(getString(R.string.unexpected_error));
                });
    }

    /**
     * show user error message for failed authentication
     */
    private void showAuthError() {
        showMessage(getString(R.string.auth_failed));
    }

    /**
     * method gets name and email from user collection creates a hashtable in usernames collection
     * this allows finding the userID from name/email with the usernames collection
     * @param onComplete
     * the task to be run once method has been completed
     */
    private void syncUsernameEmailMappingForCurrentUser(@NonNull Runnable onComplete) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            onComplete.run();
            return;
        }

        String currentEmail = currentUser.getEmail();
        if (TextUtils.isEmpty(currentEmail)) {
            onComplete.run();
            return;
        }

        String uid = currentUser.getUid();
        String normalizedEmail = currentEmail.toLowerCase(Locale.US);

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String usernameKey = normalize(snapshot.getString("usernameKey"));
                    String pendingEmail = normalize(snapshot.getString("pendingEmail"))
                            .toLowerCase(Locale.US);
                    boolean shouldClearPending = !TextUtils.isEmpty(pendingEmail)
                            && normalizedEmail.equals(pendingEmail);

                    WriteBatch batch = firestore.batch();

                    Map<String, Object> userUpdates = new HashMap<>();
                    userUpdates.put("email", normalizedEmail);
                    if (shouldClearPending) {
                        userUpdates.put("pendingEmail", FieldValue.delete());
                    } else if (!TextUtils.isEmpty(pendingEmail)) {
                        userUpdates.put("pendingEmail", pendingEmail);
                    }
                    batch.set(
                            firestore.collection("users").document(uid),
                            userUpdates,
                            SetOptions.merge()
                    );

                    if (!TextUtils.isEmpty(usernameKey)) {
                        Map<String, Object> usernameUpdates = new HashMap<>();
                        usernameUpdates.put("uid", uid);
                        usernameUpdates.put("email", normalizedEmail);
                        if (shouldClearPending) {
                            usernameUpdates.put("pendingEmail", FieldValue.delete());
                        } else if (!TextUtils.isEmpty(pendingEmail)) {
                            usernameUpdates.put("pendingEmail", pendingEmail);
                        }

                        batch.set(
                                firestore.collection("usernames")
                                        .document(usernameKey.toLowerCase(Locale.US)),
                                usernameUpdates,
                                SetOptions.merge()
                        );
                    }

                    batch.commit().addOnCompleteListener(task -> onComplete.run());
                })
                .addOnFailureListener(exception -> onComplete.run());
    }

    /**
     * removes extraneous whitespace and ensures that sting is non-null
     * @param value
     * String to be cleaned
     * @return
     * cleaned String
     */
    @NonNull
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * This finishes the current activity and open a new activity based on arguments
     * @param destination
     * the activity class that should be navigated to
     */
    private void navigateAndClearTask(@NonNull Class<?> destination) {
        Intent intent = new Intent(this, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
     * methods clears all errors without managing them
     */
    private void clearErrors() {
        loginIdentifierInput.setError(null);
        loginPasswordInput.setError(null);
    }

    /**
     * method Disables (stops them from being modified) login text fields when attempting a login
     * @param loading
     * whether the app is in the process of read/writing to the database or not
     */
    private void setLoading(boolean loading) {
        loadingIndicator.setVisibility(loading ? ProgressBar.VISIBLE : ProgressBar.GONE);
        loginButton.setEnabled(!loading);
        rememberMeCheckBox.setEnabled(!loading);
        forgotPasswordButton.setEnabled(!loading);
        backButton.setEnabled(!loading);
    }

    /**
     * converts user text input into string
     * @param input
     * EditText user input
     * @return
     * string of the user input with extraneous spaces removed
     */
    @NonNull
    private String readTrimmed(@NonNull EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    /**
     * converts a user input EditText into a string
     * @param input
     * EditText view get text data
     * @return
     * String matching text in view
     */
    @NonNull
    private String readRaw(@NonNull EditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    /**
     * an interface that describes what the program should do when matching email is (not) found
     */
    private interface EmailResolverCallback {
        /**
         * Task to be complete when there is a match
         * @param email
         * the email of user
         * @param pendingEmail
         * the new email of user user changes email
         */
        void onResolved(@NonNull String email, @NonNull String pendingEmail);
        /**
         * display an error message to the user
         * @param message
         * the string describing the error
         */
        void onError(@NonNull String message);
    }
}
