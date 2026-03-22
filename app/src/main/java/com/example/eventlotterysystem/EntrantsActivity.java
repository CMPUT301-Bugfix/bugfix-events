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
import com.google.firebase.auth.FirebaseUser;

/**
 * This is a class that is the controller of the activity_entrants screen
 * it allows organiser to navigate to view lists of entrants
 * it also can notify entrants and manages acceptance of entrants
 */
public class EntrantsActivity extends AppCompatActivity {
    private static final String TAG = "EntrantsActivity";
    public static final String EVENT_ID = "EVENT_ID";
    public static final String TOTAL_ENTRANTS = "TOTAL_ENTRANTS";
    public static final String MAX_ENTRANTS = "MAX_ENTRANTS";

    private EventRepository repository;
    private NotificationRepository notificationRepository;

    private String eventId;
    private String eventTitle;
    private int totalEntrants;
    private int maxEntrants;
    private int chosenEntrants;
    private int cancelledEntrants;
    private Button allEntrantsButton;
    private Button notifyWaitlistButton;
    private Button performDrawButton;
    private Button processExpiredButton;
    private Button chosenEntrantsButton;
    private Button cancelledEntrantsButton;

    /**
     * This is the creation of the Activity
     * This connects to layout for the screen and connects the clickable view to their controller
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrants);

        repository = new EventRepository();
        notificationRepository = new NotificationRepository();

        eventId = getIntent().getStringExtra(EVENT_ID);
        totalEntrants = getIntent().getIntExtra(TOTAL_ENTRANTS, 0);
        maxEntrants = getIntent().getIntExtra(MAX_ENTRANTS, 0);

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, R.string.missing_event_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        allEntrantsButton = findViewById(R.id.entrantsAllEntrantsButton);
        chosenEntrantsButton = findViewById(R.id.entrantsChosenButton);
        cancelledEntrantsButton = findViewById(R.id.entrantsCancelledButton);
        notifyWaitlistButton = findViewById(R.id.entrantsNotifyWaitlistButton);
        performDrawButton = findViewById(R.id.entrantsPerformDrawButton);
        processExpiredButton = findViewById(R.id.entrantsProcessExpiredButton);

        findViewById(R.id.entrantsBackButton).setOnClickListener(v -> finish());
        allEntrantsButton.setOnClickListener(v -> openEntrantsList(null));
        chosenEntrantsButton.setOnClickListener(v -> openEntrantsList(EventRepository.WAITLIST_STATUS_CHOSEN));
        cancelledEntrantsButton.setOnClickListener(v -> openEntrantsList(EventRepository.WAITLIST_STATUS_DECLINED));
        notifyWaitlistButton.setOnClickListener(v -> showNotifyOptionsDialog());
        performDrawButton.setOnClickListener(v -> showDrawConfirmation());
        processExpiredButton.setOnClickListener(v -> processExpired());

        updateButtons();
    }

    /**
     * This is the startup of the Activity
     * it loads the entrant aggregated information for the Event
     * on a load failure notifies user with a popup
     */
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.event_manage_permission_denied, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        repository.getEventById(eventId)
                .addOnSuccessListener(event -> {
                    if (!EventRepository.canManageEvent(event, currentUser.getUid())) {
                        Toast.makeText(this, R.string.event_manage_permission_denied, Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    eventTitle = event.getTitle();
                    totalEntrants = event.getTotalEntrants();
                    maxEntrants = event.getMaxEntrants();
                    updateButtons();
                    loadEntrantCounts();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to refresh entrant totals", e);
                    Toast.makeText(
                            EntrantsActivity.this,
                            getString(R.string.failed_to_load_entrants),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void loadEntrantCounts() {
        repository.getEntrantCount(eventId, EventRepository.WAITLIST_STATUS_CHOSEN)
                .addOnSuccessListener(count -> {
                    chosenEntrants = count;
                    updateButtons();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load chosen entrant count", e);
                    Toast.makeText(
                            EntrantsActivity.this,
                            getString(R.string.failed_to_load_entrants),
                            Toast.LENGTH_LONG
                    ).show();
                });

        repository.getEntrantCount(eventId, EventRepository.WAITLIST_STATUS_DECLINED)
                .addOnSuccessListener(count -> {
                    cancelledEntrants = count;
                    updateButtons();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load cancelled entrant count", e);
                    Toast.makeText(
                            EntrantsActivity.this,
                            getString(R.string.failed_to_load_entrants),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    /**
     * sets the text of Entrant status label with the important counts related to the status
     */
    private void updateButtons() {
        allEntrantsButton.setText(getString(
                R.string.all_entrants_button_label,
                buildEntrantCountText(totalEntrants, maxEntrants)
        ));
        chosenEntrantsButton.setText(getString(
                R.string.chosen_entrants_button_label,
                chosenEntrants
        ));
        cancelledEntrantsButton.setText(getString(
                R.string.cancelled_entrants_button_label,
                cancelledEntrants
        ));
    }

    /**
     * user chooses which status of Entrant needs to be notified though a popup
     */
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

    /**
     * user inputs notification message body text through a popup
     * once message is complete runs sendBatchNotification to actually send the notification
     * @param statusFilter
     * the status of Entrant that will receive the notification
     */
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

    /**
     * uploads the notification document to the database
     * @param message
     * the text that the notification will send out
     * @param statusFilter
     * the status of Entrant that will receive the notification
     */
    private void sendBatchNotification(String message, String statusFilter) {
        notifyWaitlistButton.setEnabled(false);
        notificationRepository.sendBatchNotification(
                eventId,
                hasEventTitle() ? eventTitle : getString(R.string.unknown_event_title),
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

    /**
     * creates a popup for confirmation to draw Entrants in the waitlist into chosen
     */
    private void showDrawConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Perform Lottery Draw")
                .setMessage("This will randomly select winners from the waitlist and notify them. Continue?")
                .setPositiveButton("Draw", (dialog, which) -> performDraw())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Draws from waitlist document entries with the waitlist status to chosen status
     * the number of waitlist entries selected is from the maxParticipants field of the event document
     * notify user of whether the draw attempt succeeded/failed
     */
    private void performDraw() {
        performDrawButton.setEnabled(false);
        repository.getEventById(eventId).addOnSuccessListener(event -> {
            eventTitle = event.getTitle();
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
        }).addOnFailureListener(e -> {
            performDrawButton.setEnabled(true);
            Toast.makeText(this, "Draw failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    /**
     * converts waitlist document entries with the chosen status to cancel status
     * runs another draw from waitlist document entries automatically
     * notify user of whether the removal attempt succeeded/failed
     */
    private void processExpired() {
        processExpiredButton.setEnabled(false);
        repository.getEventById(eventId).addOnSuccessListener(event -> {
            eventTitle = event.getTitle();
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
        }).addOnFailureListener(e -> {
            processExpiredButton.setEnabled(true);
            Toast.makeText(this, "Clean up failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    /**
     * navigates to AllEntrantsActivity which will display all Entrants of a specific status for an Event
     * @param statusFilter
     * the status that will be displayed in AllEntrantsActivity
     */
    private void openEntrantsList(String statusFilter) {
        Intent intent = new Intent(this, AllEntrantsActivity.class);
        intent.putExtra(EVENT_ID, eventId);
        if (statusFilter != null && !statusFilter.isEmpty()) {
            intent.putExtra(AllEntrantsActivity.STATUS_FILTER, statusFilter);
        }
        startActivity(intent);
    }

    /**
     * merges the totalEntrants and maxEntrants variables into a text to be displayed
     * @param totalEntrants
     * current number of Entrants signed up
     * @param maxEntrants
     * max number of Entrants that are allowed to sign up
     * @return
     * string showing how many slots are filled
     */
    private String buildEntrantCountText(int totalEntrants, int maxEntrants) {
        return totalEntrants + " / "
                + (maxEntrants > 0 ? String.valueOf(maxEntrants) : getString(R.string.unlimited));
    }

    /**
     * checks to see if the event title is not empty
     * @return
     * true if the Event has title
     */
    private boolean hasEventTitle() {
        return eventTitle != null && !eventTitle.trim().isEmpty();
    }
}
