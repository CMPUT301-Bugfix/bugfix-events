package com.example.eventlotterysystem;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This class deals with displaying the event details page for the
 * admins, which will allow admins to delete an event
 */

public class AdminEventDetailsActivity extends AppCompatActivity {

    public static final String EVENT_ID = "eventId";
    public static final String EVENT_TITLE = "eventTitle";

    private static final String TAG = "AdminEventDetails";
    private static final long MAX_POSTER_BYTES = 4L * 1024L * 1024L;
    private static final String DATE_PATTERN = "MMM d, yyyy";

    private FirebaseFirestore firestore;
    private EventRepository repository;

    private ImageView posterImageView;
    private TextView titleTextView;
    private TextView hostTextView;
    private TextView locationTextView;
    private TextView dateTextView;
    private TextView deadlineTextView;
    private TextView maxEntrantsTextView;
    private TextView waitlistCountTextView;
    private TextView geolocationTextView;
    private TextView descriptionTextView;
    private Button viewCommentsButton;
    private Button deleteEventButton;

    private String eventId;
    private String eventTitle;
    private boolean isDeleting;
    private EventItem currentEvent;


    /**
     * This method deals with the creation of the event details screen and its component views
     * as well as initializing and getting all the necessary information for that screen,
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_event_details);

        firestore = FirebaseFirestore.getInstance();
        repository = new EventRepository();

        TextView backButton = findViewById(R.id.adminEventDetailsBackButton);
        TextView titleValue = findViewById(R.id.adminEventDetailsTitleValue);
        titleTextView = titleValue;
        posterImageView = findViewById(R.id.adminEventPoster);
        hostTextView = findViewById(R.id.adminEventHost);
        locationTextView = findViewById(R.id.adminEventLocation);
        dateTextView = findViewById(R.id.adminEventDate);
        deadlineTextView = findViewById(R.id.adminEventDeadline);
        maxEntrantsTextView = findViewById(R.id.adminEventMaxEntrants);
        waitlistCountTextView = findViewById(R.id.adminEventWaitlistCount);
        geolocationTextView = findViewById(R.id.adminEventGeolocation);
        descriptionTextView = findViewById(R.id.adminEventDescription);
        viewCommentsButton = findViewById(R.id.adminViewCommentsButton);
        deleteEventButton = findViewById(R.id.adminDeleteEventButton);

        backButton.setOnClickListener(v -> finish());

        eventId = normalize(getIntent().getStringExtra(EVENT_ID));
        eventTitle = normalize(getIntent().getStringExtra(EVENT_TITLE));

        titleValue.setText(TextUtils.isEmpty(eventTitle) ? getString(R.string.unknown_event_title) : eventTitle);

        deleteEventButton.setOnClickListener(v -> confirmDelete());
        viewCommentsButton.setOnClickListener(v -> openCommentsScreen());
    }

    /**
     * This is the startup of the Activity
     * This runs loadEvent to get all the Event information of the event that was selected
     * to get to the activity_admin_event_details screen
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
        if (TextUtils.isEmpty(eventId)) {
            return;
        }

        repository.getEventById(eventId)
                .addOnSuccessListener(event -> {
                    try {
                        currentEvent = event;

                        String title = (event.getTitle() == null || event.getTitle().trim().isEmpty())
                                ? getString(R.string.unknown_event_title)
                                : event.getTitle().trim();
                        titleTextView.setText(title);

                        showPoster(event.getPosterUrl());
                        showMetadata(event);

                        descriptionTextView.setText(hasText(event.getDescription())
                                ? event.getDescription()
                                : getString(R.string.event_card_missing_description));

                        waitlistCountTextView.setVisibility(View.VISIBLE);
                        waitlistCountTextView.setText(getString(
                                R.string.admin_event_entrants_format,
                                event.getTotalEntrants(),
                                event.getMaxEntrants()
                        ));

                    } catch (Exception exception) {
                        Log.e(TAG, "Failed to render admin event details", exception);
                        Toast.makeText(this, R.string.failed_to_render_event, Toast.LENGTH_LONG).show();
                        renderLoadFailureState();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event details", e);
                    Toast.makeText(this, buildLoadErrorMessage(e), Toast.LENGTH_LONG).show();
                    renderLoadFailureState();
                });
    }

    /**
     * This set the image view for the event to be its picture if it has one
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


    /**
     * hides all the views on the screen shows error message of what cause the failure
     * is used when there is a problem in loading the data of the current event
     */
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
        geolocationTextView.setText("");
        descriptionTextView.setText(R.string.event_details_load_failed_message);
    }

    /**
     * opens the comments screen for the current event so an admin can review or moderate comments
     */
    private void openCommentsScreen() {
        if (!hasText(eventId)) {
            Toast.makeText(this, R.string.missing_event_id, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra(CommentsActivity.EVENT_ID, eventId);
        startActivity(intent);
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
     * This method shows the dialog for confirming deletion
     */
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

    /**
     * This method deletes events from the firestore events collection itself
     */
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
                    /**
                     * handles a failure while deleting the Event
                     * @param e
                     * the exception raised while deleting the Event
                     */
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleDeleteFailure(e);
                    }
                });
    }

    /**
     * In the case where we are unable to delete an event, we display a message stating as such
     * @param exception This is the error from deleteEvent()
     */

    private void handleDeleteFailure(@NonNull Exception exception) {
        setDeleting(false);
        if (exception instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) exception).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            Toast.makeText(this, getString(R.string.admin_delete_event_permission_denied), Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, getString(R.string.admin_delete_event_failed), Toast.LENGTH_LONG).show();
    }

    /**
     * Disables the delete button when deleting
     * @param deleting boolean value
     */

    private void setDeleting(boolean deleting) {
        isDeleting = deleting;
        deleteEventButton.setEnabled(!deleting);
    }

    /**
     * This method just cleans up a string for us
     * @param value A string that we want to clean up
     * @return either an empty string if the value is null or the trimmed string
     */

    @NonNull
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
