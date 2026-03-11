package com.example.eventlotterysystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class EntrantsActivity extends AppCompatActivity {

    private static final String TAG = "EntrantsActivity";
    public static final String EVENT_ID = "EVENT_ID";
    public static final String TOTAL_ENTRANTS = "TOTAL_ENTRANTS";
    public static final String MAX_ENTRANTS = "MAX_ENTRANTS";
    public static final String EVENT_TITLE = "EVENT_TITLE";

    private EventRepository repository;
    private NotificationRepository notificationRepository;

    private String eventId;
    private int totalEntrants;
    private int maxEntrants;
    private String eventTitle;
    private Button allEntrantsButton;
    private Button notifyWaitlistButton;
    private Button performDrawButton;
    private Button processExpiredButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrants);

        repository = new EventRepository();
        notificationRepository = new NotificationRepository();

        eventId = getIntent().getStringExtra(EVENT_ID);
        totalEntrants = getIntent().getIntExtra(TOTAL_ENTRANTS, 0);
        maxEntrants = getIntent().getIntExtra(MAX_ENTRANTS, 0);
        eventTitle = getIntent().getStringExtra(EVENT_TITLE);

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, R.string.missing_event_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        allEntrantsButton = findViewById(R.id.entrantsAllEntrantsButton);
        notifyWaitlistButton = findViewById(R.id.entrantsNotifyWaitlistButton);
        performDrawButton = findViewById(R.id.entrantsPerformDrawButton);
        processExpiredButton = findViewById(R.id.entrantsProcessExpiredButton);

        findViewById(R.id.entrantsBackButton).setOnClickListener(v -> finish());
        allEntrantsButton.setOnClickListener(v -> openAllEntrants());
        notifyWaitlistButton.setOnClickListener(v -> showNotifyOptionsDialog());
        performDrawButton.setOnClickListener(v -> showDrawConfirmation());
        processExpiredButton.setOnClickListener(v -> processExpired());

        updateAllEntrantsButton();
    }

    @Override
    protected void onStart() {
        super.onStart();
        repository.getEventById(eventId)
                .addOnSuccessListener(event -> {
                    totalEntrants = event.getTotalEntrants();
                    maxEntrants = event.getMaxEntrants();
                    eventTitle = event.getTitle();
                    updateAllEntrantsButton();
                })
                .addOnFailureListener(e -> {
                });
    }

    private void updateAllEntrantsButton() {
        allEntrantsButton.setText(getString(
                R.string.all_entrants_button_label,
                buildEntrantCountText(totalEntrants, maxEntrants)
        ));
    }

    private void showNotifyOptionsDialog() {
        String[] options = {"Everyone", "Waiting", "Chosen (Winners)", "Confirmed", "Declined"};
        new AlertDialog.Builder(this)
                .setTitle("Who would you like to notify?")
                .setItems(options, (dialog, which) -> {
                    String filter = null;
                    switch (which) {
                        case 1: filter = EventRepository.WAITLIST_STATUS_IN; break;
                        case 2: filter = EventRepository.WAITLIST_STATUS_CHOSEN; break;
                        case 3: filter = EventRepository.WAITLIST_STATUS_CONFIRMED; break;
                        case 4: filter = EventRepository.WAITLIST_STATUS_DECLINED; break;
                    }
                    showNotificationInputDialog(filter);
                })
                .show();
    }

    private void showNotificationInputDialog(String statusFilter) {
        EditText input = new EditText(this);
        input.setHint(R.string.notification_message_hint);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle(R.string.send_notification)
                .setView(input)
                .setPositiveButton(R.string.notification_send_action, (dialog, which) -> {
                    String message = input.getText().toString().trim();
                    if (message.isEmpty()) {
                        Toast.makeText(this, R.string.field_required, Toast.LENGTH_SHORT).show();
                    } else {
                        sendBatchNotification(message, statusFilter);
                    }
                })
                .setNegativeButton(R.string.back, null)
                .show();
    }

    private void sendBatchNotification(String message, String statusFilter) {
        notifyWaitlistButton.setEnabled(false);
        notificationRepository.sendBatchNotification(
                eventId,
                eventTitle != null ? eventTitle : "Event Update",
                message,
                "GENERAL",
                statusFilter
        ).addOnSuccessListener(unused -> {
            notifyWaitlistButton.setEnabled(true);
            Toast.makeText(this, R.string.notification_sent, Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            notifyWaitlistButton.setEnabled(true);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Notification failed", e);
        });
    }

    private void showDrawConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Perform Lottery Draw")
                .setMessage("This will randomly select winners from the waitlist and notify them. Continue?")
                .setPositiveButton("Draw", (dialog, which) -> performDraw())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDraw() {
        performDrawButton.setEnabled(false);
        repository.getEventById(eventId).addOnSuccessListener(event -> {
            String winningMsg = event.getWinningMessage();
            if (winningMsg == null || winningMsg.trim().isEmpty()) {
                winningMsg = "Congratulations! You have been selected for the event.";
            }
            
            repository.performLotteryDraw(eventId, winningMsg)
                    .addOnSuccessListener(unused -> {
                        performDrawButton.setEnabled(true);
                        Toast.makeText(this, R.string.draw_success, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        performDrawButton.setEnabled(true);
                        Toast.makeText(this, "Draw failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }

    private void processExpired() {
        processExpiredButton.setEnabled(false);
        repository.getEventById(eventId).addOnSuccessListener(event -> {
            String winningMsg = event.getWinningMessage();
            if (winningMsg == null || winningMsg.trim().isEmpty()) {
                winningMsg = "Congratulations! You have been selected for the event.";
            }
            
            repository.processExpiredWinners(eventId, winningMsg)
                    .addOnSuccessListener(unused -> {
                        processExpiredButton.setEnabled(true);
                        Toast.makeText(this, R.string.expired_cleaned, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        processExpiredButton.setEnabled(true);
                        Toast.makeText(this, "Clean up failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }

    private void openAllEntrants() {
        Intent intent = new Intent(this, AllEntrantsActivity.class);
        intent.putExtra(EVENT_ID, eventId);
        intent.putExtra(EVENT_TITLE, eventTitle == null ? "" : eventTitle);
        startActivity(intent);
    }

    private String buildEntrantCountText(int totalEntrants, int maxEntrants) {
        return totalEntrants + " / "
                + (maxEntrants > 0 ? String.valueOf(maxEntrants) : getString(R.string.unlimited));
    }
}
