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

/**
 * This is a class that is the controller of the activity_signup screen
 * allows a user to create an account and then naviagtes them to home activity
 */
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

    /**
     * This is the  controller for when createAccountButton is pressed
     * it has error checking to ensure all mandatory fields are filled
     * the created user is uploaded to the database in Users collection
     */
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

    /**
     * signs up the user by storing them to the database
     * edits the user in users so that it contains the user information
     * creates the user in usernames collections so that the user can log in
     * if successful notifies the user that the account was created
     * if failure notifies the user and removes the user from database (if there was a problem/ username already taken)
     * @param user
     * the user account in the database
     * @param fullName
     * the name of the user
     * @param email
     * the email of the user
     * @param username
     * the username of the user
     * @param usernameKey
     * the username of the user but all characters are lowercase
     * @param phone
     * the phone number of the user
     */
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

    /**
     * undoes the user creation by deleting the user on the database
     * also signs user out an displays a message pop-up to user
     * @param user
     * the user account in the database
     * @param message
     * a text message to be displayed
     */
    private void rollbackUserCreation(@NonNull FirebaseUser user, @NonNull String message) {
        user.delete().addOnCompleteListener(task -> {
            auth.signOut();
            setLoading(false);
            showMessage(message);
        });
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
        createNameInput.setError(null);
        createEmailInput.setError(null);
        createUsernameInput.setError(null);
        createPasswordInput.setError(null);
        createConfirmPasswordInput.setError(null);
    }

    /**
     * method Disables (stops them from being modified) login text fields when attempting a login
     * @param loading
     * whether the app is in the process of read/writing to the database or not
     */
    private void setLoading(boolean loading) {
        loadingIndicator.setVisibility(loading ? ProgressBar.VISIBLE : ProgressBar.GONE);
        createAccountButton.setEnabled(!loading);
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
}
