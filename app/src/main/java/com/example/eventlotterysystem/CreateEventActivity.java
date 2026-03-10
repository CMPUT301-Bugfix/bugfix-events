package com.example.eventlotterysystem;

import android.app.DatePickerDialog;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CreateEventActivity extends AppCompatActivity {

    private static final String TAG = "CreateEventActivity";
    private static final String DATE_PATTERN = "MMM d, yyyy";
    private static final long MAX_POSTER_BYTES = 4L * 1024L * 1024L;

    private FirebaseAuth auth;
    private EventRepository repository;

    private TextView screenTitle;
    private TextView screenSubtitle;
    private EditText titleInput;
    private EditText descriptionInput;
    private EditText locationInput;
    private EditText maxEntrantsInput;
    private ImageView posterPreview;
    private TextView posterStatus;
    private TextView deadlineValue;
    private TextView eventDateValue;
    private Button posterButton;
    private Button deadlineButton;
    private Button eventDateButton;
    private Button submitButton;
    private SwitchCompat geolocationSwitch;

    private Uri selectedPosterUri;
    private LocalDate selectedDeadlineDate;
    private LocalDate selectedEventDate;
    private String editingEventId;
    private String currentPosterUrl = "";
    private boolean editMode;

    private final ActivityResultLauncher<String> posterPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    return;
                }
                selectedPosterUri = uri;
                posterPreview.setVisibility(View.VISIBLE);
                posterPreview.setImageURI(uri);
                posterStatus.setText(R.string.poster_selected);
                posterStatus.setError(null);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        auth = FirebaseAuth.getInstance();
        repository = new EventRepository();

        screenTitle = findViewById(R.id.createEventTitle);
        screenSubtitle = findViewById(R.id.createEventSubtitle);
        titleInput = findViewById(R.id.createEventTitleInput);
        descriptionInput = findViewById(R.id.createEventDescriptionInput);
        locationInput = findViewById(R.id.createEventLocationInput);
        maxEntrantsInput = findViewById(R.id.createEventMaxEntrantsInput);
        posterPreview = findViewById(R.id.createEventPosterPreview);
        posterStatus = findViewById(R.id.createEventPosterStatus);
        deadlineValue = findViewById(R.id.createEventDeadlineValue);
        eventDateValue = findViewById(R.id.createEventDateValue);
        posterButton = findViewById(R.id.createEventPosterButton);
        deadlineButton = findViewById(R.id.createEventDeadlineButton);
        eventDateButton = findViewById(R.id.createEventDateButton);
        submitButton = findViewById(R.id.createEventSubmitButton);
        geolocationSwitch = findViewById(R.id.createEventGeolocationSwitch);

        findViewById(R.id.createEventBackButton).setOnClickListener(v -> finish());
        posterButton.setOnClickListener(v -> posterPickerLauncher.launch("image/*"));
        deadlineButton.setOnClickListener(v -> showDatePicker(true));
        eventDateButton.setOnClickListener(v -> showDatePicker(false));
        submitButton.setOnClickListener(v -> submitEvent());

        editingEventId = getIntent().getStringExtra("EVENT_ID");
        editMode = hasText(editingEventId);

        showPosterOptionalState();
        if (editMode) {
            screenTitle.setText(R.string.edit_event_title);
            screenSubtitle.setText(R.string.edit_event_subtitle);
            submitButton.setText(R.string.edit_event_submit);
            loadEventForEditing(editingEventId);
        }
    }

    private void showDatePicker(boolean forDeadline) {
        LocalDate currentValue = forDeadline ? selectedDeadlineDate : selectedEventDate;
        Calendar calendar = Calendar.getInstance();
        if (currentValue != null) {
            calendar.set(currentValue.getYear(), currentValue.getMonthValue() - 1, currentValue.getDayOfMonth());
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    LocalDate selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    if (forDeadline) {
                        selectedDeadlineDate = selectedDate;
                        deadlineValue.setText(formatDate(toRegistrationDeadline(selectedDate)));
                        deadlineValue.setError(null);
                    } else {
                        selectedEventDate = selectedDate;
                        eventDateValue.setText(formatDate(toEventDate(selectedDate)));
                        eventDateValue.setError(null);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void submitEvent() {
        clearErrors();

        String title = readTrimmed(titleInput);
        String description = readTrimmed(descriptionInput);
        String location = readTrimmed(locationInput);

        boolean hasErrors = false;
        if (!hasText(title)) {
            titleInput.setError(getString(R.string.field_required));
            hasErrors = true;
        }
        if (!hasText(description)) {
            descriptionInput.setError(getString(R.string.field_required));
            hasErrors = true;
        }
        if (!hasText(location)) {
            locationInput.setError(getString(R.string.field_required));
            hasErrors = true;
        }
        if (selectedDeadlineDate == null) {
            deadlineValue.setError(getString(R.string.registration_deadline_required));
            deadlineValue.setText(R.string.registration_deadline_required);
            hasErrors = true;
        }
        if (selectedEventDate == null) {
            eventDateValue.setError(getString(R.string.event_date_required));
            eventDateValue.setText(R.string.event_date_required);
            hasErrors = true;
        }

        int maxEntrants = 0;
        try {
            maxEntrants = parseOptionalPositiveInt(readTrimmed(maxEntrantsInput));
        } catch (IllegalArgumentException exception) {
            maxEntrantsInput.setError(getString(R.string.max_entrants_invalid));
            hasErrors = true;
        }

        if (selectedDeadlineDate != null
                && selectedEventDate != null
                && selectedDeadlineDate.isAfter(selectedEventDate)) {
            deadlineValue.setError(getString(R.string.registration_deadline_invalid));
            deadlineValue.setText(R.string.registration_deadline_invalid);
            hasErrors = true;
        }

        if (hasErrors) {
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || selectedDeadlineDate == null || selectedEventDate == null) {
            Toast.makeText(this, R.string.unexpected_error, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        EventItem draftEvent = new EventItem(
                editMode ? editingEventId : "",
                title,
                description,
                location,
                currentPosterUrl,
                maxEntrants,
                0,
                toRegistrationDeadline(selectedDeadlineDate),
                toEventDate(selectedEventDate),
                geolocationSwitch.isChecked(),
                currentUser.getUid(),
                ""
        );

        EventRepository.SaveEventCallback callback = new EventRepository.SaveEventCallback() {
            @Override
            public void onSuccess(String eventId) {
                setLoading(false);
                Toast.makeText(
                        CreateEventActivity.this,
                        editMode ? R.string.event_updated : R.string.event_created,
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Toast.makeText(
                        CreateEventActivity.this,
                        editMode ? R.string.failed_to_update_event : R.string.failed_to_create_event,
                        Toast.LENGTH_SHORT
                ).show();
            }
        };

        if (editMode) {
            repository.updateEvent(editingEventId, currentUser, draftEvent, selectedPosterUri, callback);
        } else {
            repository.createEvent(currentUser, draftEvent, selectedPosterUri, callback);
        }
    }

    private void clearErrors() {
        titleInput.setError(null);
        descriptionInput.setError(null);
        locationInput.setError(null);
        maxEntrantsInput.setError(null);
        posterStatus.setError(null);
        deadlineValue.setError(null);
        eventDateValue.setError(null);
    }

    private void setLoading(boolean loading) {
        submitButton.setEnabled(!loading);
        posterButton.setEnabled(!loading);
        deadlineButton.setEnabled(!loading);
        eventDateButton.setEnabled(!loading);
        titleInput.setEnabled(!loading);
        descriptionInput.setEnabled(!loading);
        locationInput.setEnabled(!loading);
        maxEntrantsInput.setEnabled(!loading);
        geolocationSwitch.setEnabled(!loading);
        findViewById(R.id.createEventBackButton).setEnabled(!loading);
    }

    private void loadEventForEditing(String eventId) {
        setLoading(true);
        repository.getEventById(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(EventItem event) {
                setLoading(false);
                populateForm(event);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to load event for editing", e);
                setLoading(false);
                Toast.makeText(
                        CreateEventActivity.this,
                        buildLoadErrorMessage(e),
                        Toast.LENGTH_LONG
                ).show();
                finish();
            }
        });
    }

    private void populateForm(EventItem event) {
        titleInput.setText(event.getTitle());
        descriptionInput.setText(event.getDescription());
        locationInput.setText(event.getLocation());
        maxEntrantsInput.setText(event.getMaxEntrants() > 0 ? String.valueOf(event.getMaxEntrants()) : "");

        selectedDeadlineDate = toLocalDate(event.getRegistrationDeadline());
        selectedEventDate = toLocalDate(event.getEventDate());
        if (selectedDeadlineDate != null) {
            deadlineValue.setText(formatDate(toRegistrationDeadline(selectedDeadlineDate)));
        }
        if (selectedEventDate != null) {
            eventDateValue.setText(formatDate(toEventDate(selectedEventDate)));
        }

        geolocationSwitch.setChecked(event.isRequiresGeolocation());
        currentPosterUrl = event.getPosterUrl();
        showExistingPoster(currentPosterUrl);
    }

    private void showExistingPoster(String posterUrl) {
        if (!hasText(posterUrl)) {
            posterPreview.setImageDrawable(null);
            posterPreview.setVisibility(View.GONE);
            showPosterOptionalState();
            return;
        }

        posterPreview.setVisibility(View.VISIBLE);
        posterPreview.setImageResource(android.R.drawable.ic_menu_gallery);
        posterStatus.setText(R.string.poster_selected);
        posterStatus.setError(null);

        try {
            FirebaseStorage.getInstance()
                    .getReferenceFromUrl(posterUrl)
                    .getBytes(MAX_POSTER_BYTES)
                    .addOnSuccessListener(bytes -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        posterPreview.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                    })
                    .addOnFailureListener(exception -> {
                        Log.e(TAG, "Failed to load current poster", exception);
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        posterPreview.setVisibility(View.GONE);
                        posterPreview.setImageDrawable(null);
                        showPosterOptionalState();
                    });
        } catch (IllegalArgumentException exception) {
            Log.e(TAG, "Invalid poster URL", exception);
            posterPreview.setVisibility(View.GONE);
            posterPreview.setImageDrawable(null);
            showPosterOptionalState();
        }
    }

    private void showPosterOptionalState() {
        if (selectedPosterUri == null && !hasText(currentPosterUrl)) {
            posterPreview.setVisibility(View.GONE);
            posterPreview.setImageDrawable(null);
        }
        posterStatus.setText(R.string.poster_optional);
        posterStatus.setTextColor(ContextCompat.getColor(this, android.R.color.secondary_text_light));
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private String buildLoadErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return getString(R.string.failed_to_load_event) + ": " + exception.getMessage().trim();
        }
        return getString(R.string.failed_to_load_event);
    }

    private String readTrimmed(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int parseOptionalPositiveInt(String value) {
        if (!hasText(value)) {
            return 0;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException("Must be positive");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid number", exception);
        }
    }

    private Date toRegistrationDeadline(LocalDate localDate) {
        return toDate(localDate.atTime(LocalTime.MAX));
    }

    private Date toEventDate(LocalDate localDate) {
        return toDate(localDate.atTime(LocalTime.NOON));
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(date);
    }
}
