package com.example.eventlotterysystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This is a class that is the controller of the activity_view_event screen
 * this is the activity that shows event Information to Entrants
 * It has the controllers that let the Entrant signup/leave the waitlist
 */
public class ViewEventActivity extends AppCompatActivity {
    private static final String TAG = "ViewEventActivity";
    private static final long MAX_POSTER_BYTES = 4L * 1024L * 1024L;
    private static final String DATE_PATTERN = "MMM d, yyyy";
    private ImageView posterImageView;
    private TextView screenTitleTextView;
    private TextView titleTextView;
    private ChipGroup keywordsChipGroup;
    private TextView hostTextView;
    private TextView locationTextView;
    private TextView dateTextView;
    private TextView deadlineTextView;
    private TextView maxEntrantsTextView;
    private TextView waitlistCountTextView;
    private TextView geolocationTextView;
    private TextView descriptionTextView;
    private TextView waitlistJoinedTextView;
    private TextView qrCodeButton;
    private Button entrantsButton;
    private Button editEventButton;
    private Button joinWaitlistButton;
    private Button leaveWaitlistButton;
    private Button acceptInvitationButton;
    private Button rejectInvitationButton;
    private String currentWaitlistStatus = "";
    private FirebaseAuth auth;
    private EventRepository repository;

    private String eventId;
    private boolean canEditEvent;
    private EventItem currentEvent;
    private boolean showJoinButton;
    private boolean joinEnabled;
    private boolean showJoinedLabel;
    private boolean showLeaveButton;
    private boolean waitlistActionLoading;

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
        setContentView(R.layout.activity_view_event);

        posterImageView = findViewById(R.id.viewEventPoster);
        screenTitleTextView = findViewById(R.id.viewEventScreenTitle);
        titleTextView = findViewById(R.id.viewEventTitle);
        keywordsChipGroup = findViewById(R.id.viewEventKeywords);
        hostTextView = findViewById(R.id.viewEventHost);
        locationTextView = findViewById(R.id.viewEventLocation);
        dateTextView = findViewById(R.id.viewEventDate);
        deadlineTextView = findViewById(R.id.viewEventDeadline);
        maxEntrantsTextView = findViewById(R.id.viewEventMaxEntrants);
        waitlistCountTextView = findViewById(R.id.viewEventWaitlistCount);
        geolocationTextView = findViewById(R.id.viewEventGeolocation);
        descriptionTextView = findViewById(R.id.viewEventDescription);
        waitlistJoinedTextView = findViewById(R.id.viewEventWaitlistJoinedLabel);
        entrantsButton = findViewById(R.id.viewEventEntrantsButton);
        editEventButton = findViewById(R.id.viewEventEditButton);
        joinWaitlistButton = findViewById(R.id.viewEventJoinWaitlistButton);
        leaveWaitlistButton = findViewById(R.id.viewEventLeaveWaitlistButton);
        qrCodeButton = findViewById(R.id.createQRCode);
        acceptInvitationButton = findViewById(R.id.viewEventAcceptInvitationButton);
        rejectInvitationButton = findViewById(R.id.viewEventRejectInvitationButton);

        auth = FirebaseAuth.getInstance();
        repository = new EventRepository();

        findViewById(R.id.viewEventBackButton).setOnClickListener(v -> finish());
        entrantsButton.setOnClickListener(v -> openEntrantsScreen());
        editEventButton.setOnClickListener(v -> openEventEditor());
        joinWaitlistButton.setOnClickListener(v -> showJoinWaitlistDialog());
        leaveWaitlistButton.setOnClickListener(v -> leaveWaitlist());
        acceptInvitationButton.setOnClickListener(v -> acceptInvitation());
        rejectInvitationButton.setOnClickListener(v -> showRejectInvitationDialog());

        eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, R.string.missing_event_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        canEditEvent = getIntent().getBooleanExtra("CAN_EDIT_EVENT", false);
        screenTitleTextView.setVisibility(canEditEvent ? View.VISIBLE : View.GONE);
        editEventButton.setVisibility(canEditEvent ? View.VISIBLE : View.GONE);
        qrCodeButton.setVisibility(View.GONE);

        qrCodeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRCode.class);
            intent.putExtra("Event_ID", eventId);
            startActivity(intent);
        });
    }

    /**
     * This is the startup of the Activity
     * This runs loadEvent to get all the Event information of the event that was selected to get to the activity_view_event screen
     */
    @Override
    protected void onStart() {
        super.onStart();
        loadEvent();
    }

    /**
     * This loads the information of the selected event into this activity
     * This gets the event from the repository
     * if the event is unable to be loaded renderLoadFailureState() is run
     * if the event info was loaded but unable to be displayed renderLoadFailureState() is run
     * otherwise it  edits the text of the view to display event information
     */
    private void loadEvent() {
        repository.getEventById(eventId)
                .addOnSuccessListener(event -> {
                    FirebaseUser currentUser = auth.getCurrentUser();
                    repository.canUserAccessEvent(
                                    event,
                                    eventId,
                                    currentUser == null ? null : currentUser.getUid()
                            )
                            .addOnSuccessListener(canAccess -> {
                                if (!canAccess) {
                                    Toast.makeText(
                                            ViewEventActivity.this,
                                            R.string.private_event_access_denied,
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    finish();
                                    return;
                                }
                                renderEvent(event);
                            })
                            .addOnFailureListener(exception -> {
                                Log.e(TAG, "Failed to verify event access", exception);
                                Toast.makeText(
                                        ViewEventActivity.this,
                                        buildLoadErrorMessage(exception),
                                        Toast.LENGTH_LONG
                                ).show();
                                renderLoadFailureState();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event details", e);
                    Toast.makeText(
                            ViewEventActivity.this,
                            buildLoadErrorMessage(e),
                            Toast.LENGTH_LONG
                    ).show();
                    renderLoadFailureState();
                });
    }

    private void renderEvent(EventItem event) {
        try {
            currentEvent = event;
            qrCodeButton.setVisibility(event.isPublic() ? View.VISIBLE : View.GONE);
            titleTextView.setText(event.getTitle());
            renderKeywordChips(event.getKeywords());
            showPoster(event.getPosterUrl());
            showMetadata(event);
            descriptionTextView.setText(hasText(event.getDescription())
                    ? event.getDescription()
                    : getString(R.string.event_card_missing_description));
            loadWaitlistState(event);
        } catch (Exception exception) {
            Log.e(TAG, "Failed to render event details", exception);
            Toast.makeText(
                    ViewEventActivity.this,
                    getString(R.string.failed_to_render_event),
                    Toast.LENGTH_LONG
            ).show();
            renderLoadFailureState();
        }
    }

    /**
     * This is a controller for when editEventButton is press
     * it starts the CreateEventActivity Activity with the EVENT_ID as an argument
     */
    private void openEventEditor() {
        Intent intent = new Intent(this, CreateEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        startActivity(intent);
    }

    private void openEntrantsScreen() {
        if (currentEvent == null) {
            return;
        }
        Intent intent = new Intent(this, EntrantsActivity.class);
        intent.putExtra(EntrantsActivity.EVENT_ID, currentEvent.getId());
        intent.putExtra(EntrantsActivity.TOTAL_ENTRANTS, currentEvent.getTotalEntrants());
        intent.putExtra(EntrantsActivity.MAX_ENTRANTS, currentEvent.getMaxEntrants());
        startActivity(intent);
    }

    /**
     * This is a controller for when joinWaitlistButton is pressed
     * it opens a popup to confirm the signup of to the Event
     * if join is press runs joinWaitlist()
     */
    private void showJoinWaitlistDialog() {
        if (currentEvent == null) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.join_waitlist)
                .setMessage(getString(
                        R.string.waitlist_join_dialog_message,
                        formatMaxEntrants(currentEvent.getMaxEntrants()),
                        currentEvent.getTitle(),
                        formatDate(currentEvent.getEventDate()),
                        formatDate(currentEvent.getRegistrationDeadline())
                ))
                .setNegativeButton(R.string.back, null)
                .setPositiveButton(R.string.join, (dialog, which) -> joinWaitlist())
                .show();
    }

    /**
     * This is method that get the user to join the waitlist of the event
     * if successful it saves the waitlist item to the database
     * on failure notifies the user that it was unable to do so
     */
    private void joinWaitlist() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || currentEvent == null) {
            return;
        }
        setWaitlistActionLoading(true);
        repository.joinWaitlist(eventId, currentUser)
                .addOnSuccessListener(ignored -> {
                    Toast.makeText(
                            ViewEventActivity.this,
                            R.string.waitlist_join_success,
                            Toast.LENGTH_SHORT
                    ).show();
                    setWaitlistActionLoading(false);
                    loadEvent();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to join waitlist", e);
                    Toast.makeText(
                            ViewEventActivity.this,
                            buildWaitlistErrorMessage(e),
                            Toast.LENGTH_LONG
                    ).show();
                    setWaitlistActionLoading(false);
                    applyWaitlistViewState();
                });
    }

    /**
     * This is method that get the user to leave the waitlist of the event
     * if successful it removes the waitlist item from the database
     * on failure notifies the user that it was unable to do so
     */
    private void leaveWaitlist() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || currentEvent == null) {
            return;
        }
        setWaitlistActionLoading(true);
        repository.leaveWaitlist(eventId, currentUser.getUid())
                .addOnSuccessListener(ignored -> {
                    Toast.makeText(
                            ViewEventActivity.this,
                            R.string.waitlist_leave_success,
                            Toast.LENGTH_SHORT
                    ).show();
                    setWaitlistActionLoading(false);
                    loadEvent();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to leave waitlist", e);
                    Toast.makeText(
                            ViewEventActivity.this,
                            buildWaitlistErrorMessage(e),
                            Toast.LENGTH_LONG
                    ).show();
                    setWaitlistActionLoading(false);
                    applyWaitlistViewState();
                });
    }

    /**
     * Accepts an invitation for the current user by updating the waitlist status
     * to {@code CONFIRMED}.
     *
     * On success, the event is reloaded so the confirmed state is reflected in the UI.
     */
    private void acceptInvitation() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || currentEvent == null) {
            return;
        }

        setWaitlistActionLoading(true);
        repository.updateWaitlistStatus(
                        eventId,
                        currentUser.getUid(),
                        EventRepository.WAITLIST_STATUS_CONFIRMED
                )
                .addOnSuccessListener(ignored -> {
                    Toast.makeText(
                            ViewEventActivity.this,
                            "Invitation accepted",
                            Toast.LENGTH_SHORT
                    ).show();
                    currentWaitlistStatus = EventRepository.WAITLIST_STATUS_CONFIRMED;
                    setWaitlistActionLoading(false);
                    loadEvent();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to accept invitation", e);
                    Toast.makeText(
                            ViewEventActivity.this,
                            "Failed to accept invitation",
                            Toast.LENGTH_LONG
                    ).show();
                    setWaitlistActionLoading(false);
                    applyWaitlistViewState();
                });
    }

    private void showRejectInvitationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reject invitation")
                .setMessage("Are you sure you want to reject this invitation?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reject", (dialog, which) -> rejectInvitation())
                .show();
    }

    /**
     * Rejects an invitation for the current user by updating the waitlist status
     * to {@code DECLINED}.
     *
     * On success, the event is reloaded so the declined state is reflected in the UI.
     */

    private void rejectInvitation() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || currentEvent == null) {
            return;
        }

        setWaitlistActionLoading(true);
        repository.updateWaitlistStatus(
                        eventId,
                        currentUser.getUid(),
                        EventRepository.WAITLIST_STATUS_DECLINED
                )
                .addOnSuccessListener(ignored -> {
                    Toast.makeText(
                            ViewEventActivity.this,
                            "Invitation rejected",
                            Toast.LENGTH_SHORT
                    ).show();
                    currentWaitlistStatus = EventRepository.WAITLIST_STATUS_DECLINED;
                    setWaitlistActionLoading(false);
                    loadEvent();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to reject invitation", e);
                    Toast.makeText(
                            ViewEventActivity.this,
                            "Failed to reject invitation",
                            Toast.LENGTH_LONG
                    ).show();
                    setWaitlistActionLoading(false);
                    applyWaitlistViewState();
                });
    }

    /**
     * This is the method that allows the user to join the waitlist
     * on success it updates how the screen should be displayed and then updates the screen -> joined
     * on failure it updates how the screen should be displayed and then updates the screen -> error message
     * @param event
     * the current event for this activity
     */
    private void loadWaitlistState(EventItem event) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            currentWaitlistStatus = "";
            updateWaitlistFlags(false, false, false, false);
            applyWaitlistViewState();
            return;
        }

        repository.getWaitlistStatus(eventId, currentUser.getUid())
                .addOnSuccessListener(status -> {
                    currentWaitlistStatus = status;
                    updateWaitlistControls(event, currentUser.getUid(), status);
                    applyWaitlistViewState();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load waitlist state", e);
                    Toast.makeText(
                            ViewEventActivity.this,
                            buildWaitlistErrorMessage(e),
                            Toast.LENGTH_LONG
                    ).show();
                    currentWaitlistStatus = "";
                    updateWaitlistControls(event, currentUser.getUid(), "");
                    applyWaitlistViewState();
                });
    }
    /**
     * This is method modifies the screen views such that the User has joined
     * For US 01.05.03:
     *      * - CHOSEN shows Accept and Reject buttons
     *      * - CONFIRMED shows confirmed state
     *      * - DECLINED shows declined state
     *      * - IN_WAITLIST keeps the regular waitlist state
     */
    private void applyWaitlistViewState() {
        joinWaitlistButton.setVisibility(showJoinButton ? View.VISIBLE : View.GONE);
        joinWaitlistButton.setEnabled(joinEnabled && !waitlistActionLoading);

        waitlistCountTextView.setVisibility(currentEvent == null ? View.GONE : View.VISIBLE);

        boolean isChosen = EventRepository.WAITLIST_STATUS_CHOSEN.equals(currentWaitlistStatus);
        boolean isConfirmed = EventRepository.WAITLIST_STATUS_CONFIRMED.equals(currentWaitlistStatus);
        boolean isInWaitlist = EventRepository.WAITLIST_STATUS_IN.equals(currentWaitlistStatus);

        acceptInvitationButton.setVisibility(isChosen ? View.VISIBLE : View.GONE);
        acceptInvitationButton.setEnabled(!waitlistActionLoading);

        rejectInvitationButton.setVisibility(isChosen ? View.VISIBLE : View.GONE);
        rejectInvitationButton.setEnabled(!waitlistActionLoading);

        leaveWaitlistButton.setVisibility(showLeaveButton ? View.VISIBLE : View.GONE);
        leaveWaitlistButton.setEnabled(!waitlistActionLoading);

        waitlistJoinedTextView.setVisibility((showJoinedLabel || isConfirmed || isInWaitlist) ? View.VISIBLE : View.GONE);

        if (isConfirmed) {
            waitlistJoinedTextView.setText("Confirmed");
        } else if (isChosen) {
            waitlistJoinedTextView.setText("Chosen");
        } else if (isInWaitlist) {
            waitlistJoinedTextView.setText(R.string.waitlist_joined_label);
        } else if (EventRepository.WAITLIST_STATUS_DECLINED.equals(currentWaitlistStatus)) {
            waitlistJoinedTextView.setText("Declined");
        }

        if (currentEvent != null) {
            waitlistCountTextView.setText(
                    getString(R.string.event_total_entrants_label, currentEvent.getTotalEntrants())
            );
            entrantsButton.setText(getString(
                    R.string.entrants_button_label,
                    buildEntrantCountText(currentEvent)
            ));
        }

        entrantsButton.setVisibility(shouldShowEntrantsButton() ? View.VISIBLE : View.GONE);
    }
    /**
     * This is sets the whether there is a change of waitlist in the process of loading and runs update views
     * @param loading true if a waitlist action is in progress, otherwise false
     */
    private void setWaitlistActionLoading(boolean loading) {
        waitlistActionLoading = loading;
        applyWaitlistViewState();
    }

    /**
     * Updates the internal waitlist UI flags based on the user's current status
     * for the selected event.
     *
     * @param event the current event being displayed
     * @param currentUserUid the ID of the signed-in user
     * @param status the current waitlist status for this user and event
     */

    private void updateWaitlistControls(EventItem event, String currentUserUid, String status) {
        boolean organizer = currentUserUid != null && currentUserUid.equals(event.getHostUid());
        if (organizer) {
            updateWaitlistFlags(false, false, false, false);
            return;
        }

        if (EventRepository.WAITLIST_STATUS_CHOSEN.equals(status)) {
            updateWaitlistFlags(false, false, false, false);
            return;
        }

        if (EventRepository.WAITLIST_STATUS_CONFIRMED.equals(status)) {
            updateWaitlistFlags(false, false, true, false);
            return;
        }

        if (EventRepository.WAITLIST_STATUS_DECLINED.equals(status)) {
            updateWaitlistFlags(false, false, false, false);
            return;
        }

        if (EventRepository.WAITLIST_STATUS_IN.equals(status)) {
            updateWaitlistFlags(false, false, true, true);
            return;
        }

        boolean isOpen = EventRepository.isWaitlistJoinOpen(
                event.isWaitlistOpen(),
                event.getRegistrationDeadline(),
                new Date()
        );
        updateWaitlistFlags(true, isOpen, false, false);
    }
    /**
     * This toggles the states of booleans so that applyWaitlistViewState can update the display appropriately
     * @param showJoinButton
     * whether the join button should be visible
     * @param joinEnabled
     * whether the user should be able to sign up for the event
     * @param showJoinedLabel
     * whether the prompt text to join be visible
     * @param showLeaveButton
     * whether the leave button should be visible
     */
    private void updateWaitlistFlags(
            boolean showJoinButton,
            boolean joinEnabled,
            boolean showJoinedLabel,
            boolean showLeaveButton
    ) {
        this.showJoinButton = showJoinButton;
        this.joinEnabled = joinEnabled;
        this.showJoinedLabel = showJoinedLabel;
        this.showLeaveButton = showLeaveButton;
    }

    /**
     * This set the image view for the event to be it's picture if it has one
     * will load image from the database
     * on load failure will notify the user that there is a failure ant its cause
     * @param posterUrl
     * the link to the image
     */
    private void showPoster(String posterUrl) {
        if (!hasText(posterUrl)) {
            posterImageView.setVisibility(View.GONE);
            posterImageView.setImageDrawable(null);
            return;
        }

        posterImageView.setVisibility(View.VISIBLE);
        posterImageView.setImageResource(android.R.drawable.ic_menu_gallery);

        try {
            FirebaseStorage.getInstance()
                    .getReferenceFromUrl(posterUrl)
                    .getBytes(MAX_POSTER_BYTES)
                    .addOnSuccessListener(bytes -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        posterImageView.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                    })
                    .addOnFailureListener(exception -> {
                        Log.e(TAG, "Failed to load event poster", exception);
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        posterImageView.setVisibility(View.GONE);
                        posterImageView.setImageDrawable(null);
                    });
        } catch (IllegalArgumentException exception) {
            Log.e(TAG, "Invalid event poster URL", exception);
            posterImageView.setVisibility(View.GONE);
            posterImageView.setImageDrawable(null);
        }
    }

    /**
     * This method populates the events metadata fields and displays a
     * generic message in the event that the field's value is missing
     * @param event
     * the event that was used to open this activity
     */
    private void showMetadata(EventItem event) {
        String host = hasText(event.getHostDisplayName())
                ? event.getHostDisplayName()
                : getString(R.string.event_card_missing_host);
        String location = hasText(event.getLocation())
                ? event.getLocation()
                : getString(R.string.event_card_missing_location);
        String eventDate = hasText(formatDate(event.getEventDate()))
                ? formatDate(event.getEventDate())
                : getString(R.string.event_card_missing_date);
        String deadlineDate = hasText(formatDate(event.getRegistrationDeadline()))
                ? formatDate(event.getRegistrationDeadline())
                : getString(R.string.event_card_missing_date);

        hostTextView.setText(getString(R.string.event_hosted_by, host));
        locationTextView.setText(getString(R.string.event_location_label, location));
        dateTextView.setText(getString(R.string.event_date_label, eventDate));
        deadlineTextView.setText(getString(R.string.event_deadline_label, deadlineDate));
        if (event.getMaxEntrants() > 0) {
            maxEntrantsTextView.setText(getString(R.string.event_max_entrants_label, event.getMaxEntrants()));
        } else {
            maxEntrantsTextView.setText(R.string.event_max_entrants_unlimited);
        }
        geolocationTextView.setText(event.isRequiresGeolocation()
                ? R.string.event_geolocation_enabled
                : R.string.event_geolocation_disabled);
    }

    private void renderKeywordChips(List<String> keywords) {
        keywordsChipGroup.removeAllViews();
        if (keywords == null || keywords.isEmpty()) {
            keywordsChipGroup.setVisibility(View.GONE);
            return;
        }

        keywordsChipGroup.setVisibility(View.VISIBLE);
        for (String keyword : keywords) {
            Chip chip = new Chip(this);
            chip.setText(keyword);
            chip.setClickable(false);
            chip.setCheckable(false);
            keywordsChipGroup.addView(chip);
        }
    }

    /**
     * hides all the views on the screen shows error message of what cause the failure
     * is used when there is a problem in loading the data of the current event
     */
    private void renderLoadFailureState() {
        posterImageView.setVisibility(View.GONE);
        posterImageView.setImageDrawable(null);
        titleTextView.setText(R.string.failed_to_load_event);
        keywordsChipGroup.removeAllViews();
        keywordsChipGroup.setVisibility(View.GONE);
        hostTextView.setText("");
        locationTextView.setText("");
        dateTextView.setText("");
        deadlineTextView.setText("");
        maxEntrantsTextView.setText("");
        waitlistCountTextView.setText("");
        waitlistCountTextView.setVisibility(View.GONE);
        waitlistJoinedTextView.setVisibility(View.GONE);
        joinWaitlistButton.setVisibility(View.GONE);
        leaveWaitlistButton.setVisibility(View.GONE);
        qrCodeButton.setVisibility(View.GONE);
        geolocationTextView.setText("");
        descriptionTextView.setText(R.string.event_details_load_failed_message);
    }

    /**
     * method that converts a raised exception during an event load into a error message to be displayed
     * @param exception
     * the exception that was created
     * @return
     * a String message describing what the error was
     */
    private String buildLoadErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return getString(R.string.failed_to_load_event) + ": " + exception.getMessage().trim();
        }
        return getString(R.string.failed_to_load_event);
    }

    /**
     *  @param exception the exception raised during a waitlist action
     * method that coverts a raised exception during a change in waitlist (join/leave) into a error message to be displayed
     * @return
     * a String message describing what the error was
     */
    private String buildWaitlistErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return getString(R.string.waitlist_action_failed) + ": " + exception.getMessage().trim();
        }
        return getString(R.string.waitlist_action_failed);
    }

    /**
     * Determines whether the entrants button should be shown for the current user.
     *
     * @return true if the signed-in user is the host of the current event, otherwise false
     */

    private boolean shouldShowEntrantsButton() {
        FirebaseUser currentUser = auth.getCurrentUser();
        return currentEvent != null
                && currentUser != null
                && currentUser.getUid().equals(currentEvent.getHostUid());
    }

    /**
     * method that check is there is content in the string
     * @param value
     * the string to be testing
     * @return
     * true if there was actual text in the string
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * This changes the date to match locality
     * @param date
     * Date the time of the Event
     * @return
     * String representation of the date that matches the timezone of the user
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(date);
    }

    /**
     * This format the maxEntrants int into a string to be displayed
     * @param maxEntrants
     * max signups allowed for this event
     * @return
     * String representation limit (number if limit / "no limit" otherwise"
     */
    private String formatMaxEntrants(int maxEntrants) {
        return maxEntrants > 0 ? String.valueOf(maxEntrants) : "no limit";
    }

    /**
     * Builds the entrant count text shown for the current event.
     *
     * @param event the event whose entrant count should be displayed
     * @return a formatted entrant count string
     */

    private String buildEntrantCountText(EventItem event) {
        String limit = event.getMaxEntrants() > 0
                ? String.valueOf(event.getMaxEntrants())
                : getString(R.string.unlimited);
        return event.getTotalEntrants() + " / " + limit;
    }
}
