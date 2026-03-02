package com.example.eventlotterysystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private EditText createNameInput;
    private EditText createEmailInput;
    private EditText createUsernameInput;
    private EditText createPhoneInput;
    private EditText createPasswordInput;
    private EditText createConfirmPasswordInput;
    private ProgressBar loadingIndicator;
    private Button createAccountButton;
    private TextView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        createNameInput = findViewById(R.id.createNameInput);
        createEmailInput = findViewById(R.id.createEmailInput);
        createUsernameInput = findViewById(R.id.createUsernameInput);
        createPhoneInput = findViewById(R.id.createPhoneInput);
        createPasswordInput = findViewById(R.id.createPasswordInput);
        createConfirmPasswordInput = findViewById(R.id.createConfirmPasswordInput);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        createAccountButton = findViewById(R.id.createAccountButton);
        backButton = findViewById(R.id.createBackButton);

        backButton.setOnClickListener(v -> finish());
        createAccountButton.setOnClickListener(v -> onCreateAccountClicked());
    }

    private void onCreateAccountClicked() {
        clearErrors();

        String fullName = readTrimmed(createNameInput);
        String email = readTrimmed(createEmailInput).toLowerCase(Locale.US);
        String username = readTrimmed(createUsernameInput);
        String usernameKey = username.toLowerCase(Locale.US);
        String phone = readTrimmed(createPhoneInput);
        String password = readRaw(createPasswordInput);
        String confirmPassword = readRaw(createConfirmPasswordInput);

        if (TextUtils.isEmpty(fullName)) {
            createNameInput.setError(getString(R.string.field_required));
            createNameInput.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            createEmailInput.setError(getString(R.string.invalid_email));
            createEmailInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(username)) {
            createUsernameInput.setError(getString(R.string.field_required));
            createUsernameInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            createPasswordInput.setError(getString(R.string.field_required));
            createPasswordInput.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            createConfirmPasswordInput.setError(getString(R.string.passwords_do_not_match));
            createConfirmPasswordInput.requestFocus();
            return;
        }

        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                setLoading(false);
                Exception exception = task.getException();
                if (exception instanceof FirebaseAuthUserCollisionException) {
                    showMessage(getString(R.string.email_already_in_use));
                } else {
                    showMessage(getString(R.string.unexpected_error));
                }
                return;
            }

            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                setLoading(false);
                showMessage(getString(R.string.unexpected_error));
                return;
            }
            saveUserProfile(user, fullName, email, username, usernameKey, phone);
        });
    }

    private void saveUserProfile(
            @NonNull FirebaseUser user,
            @NonNull String fullName,
            @NonNull String email,
            @NonNull String username,
            @NonNull String usernameKey,
            @NonNull String phone
    ) {
        DocumentReference usernameRef = firestore.collection("usernames").document(usernameKey);
        DocumentReference userRef = firestore.collection("users").document(user.getUid());

        Map<String, Object> usernamePayload = new HashMap<>();
        usernamePayload.put("uid", user.getUid());
        usernamePayload.put("email", email);
        usernamePayload.put("createdAt", FieldValue.serverTimestamp());

        Map<String, Object> profilePayload = new HashMap<>();
        profilePayload.put("fullName", fullName);
        profilePayload.put("email", email);
        profilePayload.put("username", username);
        profilePayload.put("usernameKey", usernameKey);
        profilePayload.put("accountType", "user");
        profilePayload.put("createdAt", FieldValue.serverTimestamp());
        if (!TextUtils.isEmpty(phone)) {
            profilePayload.put("phoneNumber", phone);
        }

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot existingUsernameDoc = transaction.get(usernameRef);
                    if (existingUsernameDoc.exists()) {
                        throw new FirebaseFirestoreException(
                                "Username already exists",
                                FirebaseFirestoreException.Code.ALREADY_EXISTS
                        );
                    }
                    transaction.set(usernameRef, usernamePayload);
                    transaction.set(userRef, profilePayload);
                    return null;
                })
                .addOnSuccessListener(unused -> {
                    AuthSessionPreference.setRemember(this, true);
                    showMessage(getString(R.string.account_created));
                    navigateAndClearTask(HomeActivity.class);
                })
                .addOnFailureListener(exception -> {
                    if (exception instanceof FirebaseFirestoreException
                            && ((FirebaseFirestoreException) exception).getCode()
                            == FirebaseFirestoreException.Code.ALREADY_EXISTS) {
                        rollbackUserCreation(user, getString(R.string.username_taken));
                    } else {
                        rollbackUserCreation(user, getString(R.string.unexpected_error));
                    }
                });
    }

    private void rollbackUserCreation(@NonNull FirebaseUser user, @NonNull String message) {
        user.delete().addOnCompleteListener(task -> {
            auth.signOut();
            setLoading(false);
            showMessage(message);
        });
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
        createNameInput.setError(null);
        createEmailInput.setError(null);
        createUsernameInput.setError(null);
        createPasswordInput.setError(null);
        createConfirmPasswordInput.setError(null);
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisibility(loading ? ProgressBar.VISIBLE : ProgressBar.GONE);
        createAccountButton.setEnabled(!loading);
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
}
