package com.example.eventlotterysystem;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class AdminEventDetailsActivity extends AppCompatActivity {

    public static final String EVENT_ID = "eventId";
    public static final String EVENT_TITLE = "eventTitle";

    private FirebaseFirestore firestore;
    private Button deleteEventButton;

    private String eventId;
    private String eventTitle;

    private boolean isDeleting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_event_details);

        firestore = FirebaseFirestore.getInstance();

        TextView backButton = findViewById(R.id.adminEventDetailsBackButton);
        TextView titleValue = findViewById(R.id.adminEventDetailsTitleValue);
        deleteEventButton = findViewById(R.id.adminDeleteEventButton);

        backButton.setOnClickListener(v -> finish());

        eventId = normalize(getIntent().getStringExtra(EVENT_ID));
        eventTitle = normalize(getIntent().getStringExtra(EVENT_TITLE));

        titleValue.setText(TextUtils.isEmpty(eventTitle) ? getString(R.string.unknown_event_title) : eventTitle);

        deleteEventButton.setOnClickListener(v -> confirmDelete());
    }

    private void confirmDelete() {
        if (isDeleting || TextUtils.isEmpty(eventId)) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_delete_event_confirm_title)
                .setMessage(getString(R.string.admin_delete_event_confirm_message,
                        TextUtils.isEmpty(eventTitle) ? getString(R.string.unknown_event_title) : eventTitle))
                .setNegativeButton(R.string.admin_delete_event_cancel_action, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.admin_delete_event_confirm_action, (d, w) -> deleteEvent())
                .show();
    }

    private void deleteEvent() {
        if (TextUtils.isEmpty(eventId)) return;

        setDeleting(true);

        firestore.collection("events")
                .document(eventId)
                .delete()
                .addOnSuccessListener(unused -> {
                    setDeleting(false);
                    Toast.makeText(this, getString(R.string.admin_delete_event_success), Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleDeleteFailure(e);
                    }
                });
    }

    private void handleDeleteFailure(@NonNull Exception exception) {
        setDeleting(false);
        if (exception instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) exception).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            Toast.makeText(this, getString(R.string.admin_delete_event_permission_denied), Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, getString(R.string.admin_delete_event_failed), Toast.LENGTH_LONG).show();
    }

    private void setDeleting(boolean deleting) {
        isDeleting = deleting;
        deleteEventButton.setEnabled(!deleting);
    }

    @NonNull
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
