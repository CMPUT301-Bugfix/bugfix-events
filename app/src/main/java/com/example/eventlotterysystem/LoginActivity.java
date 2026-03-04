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
            @Override
            public void onResolved(@NonNull String email, @NonNull String pendingEmail) {
                signInWithPendingEmail(email, password, pendingEmail);
            }

            @Override
            public void onError(@NonNull String message) {
                setLoading(false);
                showMessage(message);
            }
        });
    }

    private void onForgotPasswordClicked() {
        String identifier = readTrimmed(loginIdentifierInput);
        if (TextUtils.isEmpty(identifier)) {
            loginIdentifierInput.setError(getString(R.string.field_required));
            loginIdentifierInput.requestFocus();
            return;
        }

        setLoading(true);
        resolveEmailFromIdentifier(identifier, new EmailResolverCallback() {
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

            @Override
            public void onError(@NonNull String message) {
                setLoading(false);
                showMessage(message);
            }
        });
    }

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

    private void showAuthError() {
        showMessage(getString(R.string.auth_failed));
    }

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

    @NonNull
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void navigateAndClearTask(@NonNull Class<?> destination) {
        Intent intent = new Intent(this, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showMessage(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void clearErrors() {
        loginIdentifierInput.setError(null);
        loginPasswordInput.setError(null);
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisibility(loading ? ProgressBar.VISIBLE : ProgressBar.GONE);
        loginButton.setEnabled(!loading);
        rememberMeCheckBox.setEnabled(!loading);
        forgotPasswordButton.setEnabled(!loading);
        backButton.setEnabled(!loading);
    }

    @NonNull
    private String readTrimmed(@NonNull EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    @NonNull
    private String readRaw(@NonNull EditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private interface EmailResolverCallback {
        void onResolved(@NonNull String email, @NonNull String pendingEmail);

        void onError(@NonNull String message);
    }
}
