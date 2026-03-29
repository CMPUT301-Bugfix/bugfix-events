package com.example.eventlotterysystem;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

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
    private EventRepository repository;

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
        repository = new EventRepository();

        eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, R.string.missing_event_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backButton.setOnClickListener(v -> finish());
        InviteEntrantButton.setOnClickListener(v -> InviteEntrant());
    }


    private void InviteEntrant() {
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