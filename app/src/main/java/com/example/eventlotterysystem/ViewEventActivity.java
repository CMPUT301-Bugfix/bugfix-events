package com.example.eventlotterysystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ViewEventActivity extends AppCompatActivity {

    private static final String TAG = "ViewEventActivity";
    private static final long MAX_POSTER_BYTES = 4L * 1024L * 1024L;
    private static final String DATE_PATTERN = "MMM d, yyyy";

    private ImageView posterImageView;
    private TextView screenTitleTextView;
    private TextView titleTextView;
    private TextView hostTextView;
    private TextView locationTextView;
    private TextView dateTextView;
    private TextView deadlineTextView;
    private TextView maxEntrantsTextView;
    private TextView waitlistCountTextView;
    private TextView geolocationTextView;
    private TextView descriptionTextView;
    private TextView waitlistJoinedTextView;
    private Button editEventButton;
    private Button joinWaitlistButton;
    private Button leaveWaitlistButton;
    private Button notifyWaitlistButton;

    private FirebaseAuth auth;
    private EventRepository repository;
    private NotificationRepository notificationRepository;

    private String eventId;
    private boolean canEditEvent;
    private EventItem currentEvent;
    private boolean showJoinButton;
    private boolean joinEnabled;
    private boolean showJoinedLabel;
    private boolean showLeaveButton;
    private boolean waitlistActionLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_event);

        posterImageView = findViewById(R.id.viewEventPoster);
        screenTitleTextView = findViewById(R.id.viewEventScreenTitle);
        titleTextView = findViewById(R.id.viewEventTitle);
        hostTextView = findViewById(R.id.viewEventHost);
        locationTextView = findViewById(R.id.viewEventLocation);
        dateTextView = findViewById(R.id.viewEventDate);
        deadlineTextView = findViewById(R.id.viewEventDeadline);
        maxEntrantsTextView = findViewById(R.id.viewEventMaxEntrants);
        waitlistCountTextView = findViewById(R.id.viewEventWaitlistCount);
        geolocationTextView = findViewById(R.id.viewEventGeolocation);
        descriptionTextView = findViewById(R.id.viewEventDescription);
        waitlistJoinedTextView = findViewById(R.id.viewEventWaitlistJoinedLabel);
        editEventButton = findViewById(R.id.viewEventEditButton);
        joinWaitlistButton = findViewById(R.id.viewEventJoinWaitlistButton);
        leaveWaitlistButton = findViewById(R.id.viewEventLeaveWaitlistButton);
        notifyWaitlistButton = findViewById(R.id.viewEventNotifyButton);

        auth = FirebaseAuth.getInstance();
        repository = new EventRepository();
        notificationRepository = new NotificationRepository();

        findViewById(R.id.viewEventBackButton).setOnClickListener(v -> finish());
        editEventButton.setOnClickListener(v -> openEventEditor());
        joinWaitlistButton.setOnClickListener(v -> showJoinWaitlistDialog());
        leaveWaitlistButton.setOnClickListener(v -> leaveWaitlist());
        notifyWaitlistButton.setOnClickListener(v -> showNotifyDialog());

        eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, R.string.missing_event_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        canEditEvent = getIntent().getBooleanExtra("CAN_EDIT_EVENT", false);
        screenTitleTextView.setVisibility(canEditEvent ? View.VISIBLE : View.GONE);
        editEventButton.setVisibility(canEditEvent ? View.VISIBLE : View.GONE);
        notifyWaitlistButton.setVisibility(canEditEvent ? View.VISIBLE : View.GONE);

        findViewById(R.id.createQRCode).setOnClickListener(v -> {
            Intent intent = new Intent(this, QRCode.class);
            intent.putExtra("Event_ID", eventId);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadEvent();
    }

    private void loadEvent() {
        repository.getEventById(eventId)
                .addOnSuccessListener(event -> {
                    try {
                        currentEvent = event;
                        titleTextView.setText(event.getTitle());
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

    private void showNotifyDialog() {
        if (currentEvent == null) return;

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
                        sendNotification(message);
                    }
                })
                .setNegativeButton(R.string.back, null)
                .show();
    }

    private void sendNotification(String message) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || currentEvent == null) return;

        notifyWaitlistButton.setEnabled(false);
        notificationRepository.sendNotificationToWaitlist(
                currentEvent.getId(),
                currentEvent.getTitle(),
                message,
                user.getUid()
        ).addOnSuccessListener(unused -> {
            notifyWaitlistButton.setEnabled(true);
            Toast.makeText(this, R.string.notification_sent, Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            notifyWaitlistButton.setEnabled(true);
            Toast.makeText(this, R.string.unexpected_error, Toast.LENGTH_SHORT).show();
        });
    }

    private void openEventEditor() {
        Intent intent = new Intent(this, CreateEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        startActivity(intent);
    }

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

    private void loadWaitlistState(EventItem event) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            updateWaitlistFlags(false, false, false, false);
            applyWaitlistViewState();
            return;
        }

        repository.getWaitlistState(eventId, currentUser.getUid())
                .addOnSuccessListener(joined -> {
                    updateWaitlistControls(event, currentUser.getUid(), Boolean.TRUE.equals(joined));
                    applyWaitlistViewState();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load waitlist state", e);
                    Toast.makeText(
                            ViewEventActivity.this,
                            buildWaitlistErrorMessage(e),
                            Toast.LENGTH_LONG
                    ).show();
                    updateWaitlistControls(event, currentUser.getUid(), false);
                    applyWaitlistViewState();
                });
    }

    private void applyWaitlistViewState() {
        joinWaitlistButton.setVisibility(showJoinButton ? View.VISIBLE : View.GONE);
        joinWaitlistButton.setEnabled(joinEnabled && !waitlistActionLoading);
        waitlistCountTextView.setVisibility(currentEvent == null ? View.GONE : View.VISIBLE);
        waitlistJoinedTextView.setVisibility(showJoinedLabel ? View.VISIBLE : View.GONE);
        leaveWaitlistButton.setVisibility(showLeaveButton ? View.VISIBLE : View.GONE);
        leaveWaitlistButton.setEnabled(!waitlistActionLoading);

        if (currentEvent != null) {
            waitlistCountTextView.setText(
                    getString(R.string.event_total_entrants_label, currentEvent.getTotalEntrants())
            );
        }
        waitlistJoinedTextView.setText(R.string.waitlist_joined_label);
    }

    private void setWaitlistActionLoading(boolean loading) {
        waitlistActionLoading = loading;
        applyWaitlistViewState();
    }

    private void updateWaitlistControls(EventItem event, String currentUserUid, boolean joined) {
        boolean organizer = currentUserUid != null && currentUserUid.equals(event.getHostUid());
        if (organizer) {
            updateWaitlistFlags(false, false, false, false);
            return;
        }
        if (joined) {
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

    private void renderLoadFailureState() {
        posterImageView.setVisibility(View.GONE);
        posterImageView.setImageDrawable(null);
        titleTextView.setText(R.string.failed_to_load_event);
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
        notifyWaitlistButton.setVisibility(View.GONE);
        geolocationTextView.setText("");
        descriptionTextView.setText(R.string.event_details_load_failed_message);
    }

    private String buildLoadErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return getString(R.string.failed_to_load_event) + ": " + exception.getMessage().trim();
        }
        return getString(R.string.failed_to_load_event);
    }

    private String buildWaitlistErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return getString(R.string.waitlist_action_failed) + ": " + exception.getMessage().trim();
        }
        return getString(R.string.waitlist_action_failed);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(date);
    }

    private String formatMaxEntrants(int maxEntrants) {
        return maxEntrants > 0 ? String.valueOf(maxEntrants) : "no limit";
    }
}
