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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

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
            public void onResolved(@NonNull String email) {
                auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            setLoading(false);
                            if (!task.isSuccessful()) {
                                showAuthError();
                                return;
                            }
                            AuthSessionPreference.setRemember(LoginActivity.this, rememberMeCheckBox.isChecked());
                            navigateAndClearTask(HomeActivity.class);
                        });
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
            public void onResolved(@NonNull String email) {
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
            callback.onResolved(normalized);
            return;
        }

        firestore.collection("usernames")
                .document(normalized)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String email = snapshot.getString("email");
                    if (TextUtils.isEmpty(email)) {
                        callback.onError(getString(R.string.auth_failed));
                        return;
                    }
                    callback.onResolved(email.toLowerCase(Locale.US));
                })
                .addOnFailureListener(exception -> callback.onError(getString(R.string.unexpected_error)));
    }

    private void showAuthError() {
        showMessage(getString(R.string.auth_failed));
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
        void onResolved(@NonNull String email);

        void onError(@NonNull String message);
    }
}
