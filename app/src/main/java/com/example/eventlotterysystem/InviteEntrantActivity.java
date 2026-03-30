package com.example.eventlotterysystem;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This is the activity that allows an event organizer to invite an entrant to a private Event
 * This is done by having the organizer entering the Entrants Information
 */
public class InviteEntrantActivity extends AppCompatActivity {
    private TextView backButton;
    private RadioGroup searchTypeView;
    private EditText inputTextView;
    private Button InviteEntrantButton;
    private FirebaseAuth auth;
    private UserRepository repository;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String eventId;

    /**
     * This is the creation of the Activity
     * This connects to all the view on the screen and connects the clickable view to their controller
     * sets screenTitleTextView, editEventButton to be invisible as it should not be displayed initially
     * @param savedInstanceState
     * the saved state of the Activity so that the screen is not reset
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_entrant);

        backButton = findViewById(R.id.InviteEntrantBackButton);
        searchTypeView = findViewById(R.id.InviteEntrantRadioGroup);
        inputTextView = findViewById(R.id.InviteEntrantInputField);
        InviteEntrantButton = findViewById(R.id.InviteEntrantConfirmationButton);

        auth = FirebaseAuth.getInstance();
        repository = new UserRepository();

        eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, R.string.missing_event_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backButton.setOnClickListener(v -> finish());
        InviteEntrantButton.setOnClickListener(v -> findEntrant());
    }


    private void findEntrant() {
        int selectedId = searchTypeView.getCheckedRadioButtonId();

        boolean hasErrors = false;
        if (selectedId == -1) {
            new AlertDialog.Builder(this)
                    .setTitle("Need to select Field")
                    .setMessage("You have not selected which method to Invite Entrant")
                    .setPositiveButton("Ok", null)
                    .show();
            return;
        }
        String inputtedText = readTrimmed(inputTextView);
        if (inputtedText.equals("")) {
            new AlertDialog.Builder(this)
                    .setTitle("Empty Field")
                    .setMessage("You have not entered the Entrant's information")
                    .setPositiveButton("Ok", null)
                    .show();
            return;
        }
        RadioButton selectedRadioButton = findViewById(selectedId);
        String selectedText = selectedRadioButton.getText().toString();

        if (selectedText.equals("Name")) {
            loadMatchingEntrant("username",inputtedText);
        } else if (selectedText.equals("email")){
            loadMatchingEntrant("username",inputtedText);
        } else if (selectedText.equals("Phone Number")) {
            loadMatchingEntrant("phoneNumber",inputtedText);
        }
    }

    /**
     * Loads the Information Of all entrant to find a match
     */
    private void loadMatchingEntrant(String field, String value) {
        repository.loadMatchingEntrant(field, value)
                .addOnSuccessListener(users -> {
                    inviteEntrant(users.get(0));
                })
                .addOnFailureListener(e -> {
                    new AlertDialog.Builder(this)
                            .setTitle("No User Found")
                            .setMessage("We could not find the user you were looking for")
                            .setPositiveButton("Ok", null)
                            .show();
                });
    }

    /**
     * confirms the user to be sent the notification
     * @param entrant
     * the user to be sent the invitation
     */

    private void inviteEntrant(UserProfile entrant) {
        new AlertDialog.Builder(this)
                .setTitle("Found User: "+ entrant.getUsername())
                .setMessage("Would you like to invite this entrant to the event")
                .setPositiveButton("Yes", (dialog, which) -> sendNotification(entrant, eventId))
                .setNegativeButton("No", null)
                .show();
    }

    //TODO - 01.05.06
    /**
     *
     * @param entrant
     * the user to be sent the invitation
     * @param eventId
     * the Id of the event the invitation is for
     */
    private void sendNotification(UserProfile entrant, String eventId) {

    }

    /**
     * converts user text input into string
     * @param input
     * EditText user input
     * @return
     * string of the user input with extraneous spaces removed
     */
    private String readTrimmed(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}