package com.example.eventlotterysystem;

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
    private TextView totalEntrantsTextView;
    private TextView geolocationTextView;
    private TextView descriptionTextView;
    private Button editEventButton;
    private EventRepository repository;
    private String eventId;
    private boolean canEditEvent;

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
        totalEntrantsTextView = findViewById(R.id.viewEventTotalEntrants);
        geolocationTextView = findViewById(R.id.viewEventGeolocation);
        descriptionTextView = findViewById(R.id.viewEventDescription);
        editEventButton = findViewById(R.id.viewEventEditButton);

        repository = new EventRepository();

        findViewById(R.id.viewEventBackButton).setOnClickListener(v -> finish());
        editEventButton.setOnClickListener(v -> openEventEditor());

        eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, R.string.missing_event_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        canEditEvent = getIntent().getBooleanExtra("CAN_EDIT_EVENT", false);
        screenTitleTextView.setVisibility(canEditEvent ? View.VISIBLE : View.GONE);
        editEventButton.setVisibility(canEditEvent ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadEvent();
    }

    private void loadEvent() {
        repository.getEventById(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(EventItem event) {
                try {
                    titleTextView.setText(event.getTitle());
                    showPoster(event.getPosterUrl());
                    showMetadata(event);
                    descriptionTextView.setText(hasText(event.getDescription())
                            ? event.getDescription()
                            : getString(R.string.event_card_missing_description));
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

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to load event details", e);
                Toast.makeText(
                        ViewEventActivity.this,
                        buildLoadErrorMessage(e),
                        Toast.LENGTH_LONG
                ).show();
                renderLoadFailureState();
            }
        });
    }

    private void openEventEditor() {
        Intent intent = new Intent(this, CreateEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        startActivity(intent);
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
        totalEntrantsTextView.setText(getString(R.string.event_total_entrants_label, event.getTotalEntrants()));
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
        totalEntrantsTextView.setText("");
        geolocationTextView.setText("");
        descriptionTextView.setText(R.string.event_details_load_failed_message);
    }

    private String buildLoadErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return getString(R.string.failed_to_load_event) + ": " + exception.getMessage().trim();
        }
        return getString(R.string.failed_to_load_event);
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
}
