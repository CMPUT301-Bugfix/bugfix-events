package com.example.eventlotterysystem;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CreateEventActivity extends AppCompatActivity {

    private static final String DATE_PATTERN = "MMM d, yyyy";

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private EventRepository repository;
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

    private final ActivityResultLauncher<String> posterPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    return;
                }
                selectedPosterUri = uri;
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

        posterStatus.setText(R.string.poster_optional);
        posterStatus.setTextColor(ContextCompat.getColor(this, android.R.color.secondary_text_light));
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
            maxEntrants = parsePositiveInt(readTrimmed(maxEntrantsInput));
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
                "",
                title,
                description,
                location,
                "",
                maxEntrants,
                0,
                toRegistrationDeadline(selectedDeadlineDate),
                toEventDate(selectedEventDate),
                geolocationSwitch.isChecked(),
                currentUser.getUid(),
                ""
        );

        repository.createEvent(currentUser, draftEvent, selectedPosterUri, new EventRepository.CreateEventCallback() {
            @Override
            public void onSuccess(String eventId) {
                setLoading(false);
                Toast.makeText(CreateEventActivity.this, R.string.event_created, Toast.LENGTH_SHORT).show();

                // adding the entrant lists
                firestore = FirebaseFirestore.getInstance();
                DocumentReference EventDocRef = firestore.collection("Event").document(eventId);
                EventDocRef.collection("WaitingList").document();
                EventDocRef.collection("ChosenList").document();
                EventDocRef.collection("ConfirmedList").document();
                finish();
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Toast.makeText(CreateEventActivity.this, R.string.failed_to_create_event, Toast.LENGTH_SHORT).show();
            }
        });
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

    private String readTrimmed(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int parsePositiveInt(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing number");
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
