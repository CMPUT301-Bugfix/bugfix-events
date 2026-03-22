package com.example.eventlotterysystem;

import android.app.DatePickerDialog;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
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

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This is a class that is the controller of the activity_create_event screen
 * this activity allows users to create events that will be added to the database
 * is also used to edit an event by populating the input view with Event data
 */
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
    private TextInputLayout keywordInputLayout;
    private TextInputEditText keywordInput;
    private EditText maxEntrantsInput;
    private EditText maxParticipantsInput;
    private EditText winningMessageInput;
    private ChipGroup keywordsChipGroup;
    private ImageView posterPreview;
    private TextView posterStatus;
    private TextView deadlineValue;
    private TextView eventDateValue;
    private TextView visibilityStatus;
    private Button addKeywordButton;
    private Button posterButton;
    private Button deadlineButton;
    private Button eventDateButton;
    private Button submitButton;
    private SwitchCompat geolocationSwitch;
    private SwitchCompat publicSwitch;
    private final List<String> keywords = new ArrayList<>();
    private final List<String> currentCoorganizers = new ArrayList<>();

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

    /**
     * This is the creation of the Activity
     * This connects to views for the screen and connects the clickable view to their controller
     * Has an additional boolean edit mode that immediately loadEventForEditing() if there already is a event creation in progress
     * @param savedInstanceState
     * the saved state of the Activity so that the screen is not reset
     */
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
        keywordInputLayout = findViewById(R.id.createEventKeywordInputLayout);
        keywordInput = findViewById(R.id.createEventKeywordInput);
        maxEntrantsInput = findViewById(R.id.createEventMaxEntrantsInput);
        maxParticipantsInput = findViewById(R.id.createEventMaxParticipantsInput);
        winningMessageInput = findViewById(R.id.createEventWinningMessageInput);
        keywordsChipGroup = findViewById(R.id.createEventKeywordsChipGroup);
        posterPreview = findViewById(R.id.createEventPosterPreview);
        posterStatus = findViewById(R.id.createEventPosterStatus);
        deadlineValue = findViewById(R.id.createEventDeadlineValue);
        eventDateValue = findViewById(R.id.createEventDateValue);
        visibilityStatus = findViewById(R.id.createEventVisibilityStatus);
        addKeywordButton = findViewById(R.id.createEventAddKeywordButton);
        posterButton = findViewById(R.id.createEventPosterButton);
        deadlineButton = findViewById(R.id.createEventDeadlineButton);
        eventDateButton = findViewById(R.id.createEventDateButton);
        submitButton = findViewById(R.id.createEventSubmitButton);
        geolocationSwitch = findViewById(R.id.createEventGeolocationSwitch);
        publicSwitch = findViewById(R.id.createEventPublicSwitch);

        findViewById(R.id.createEventBackButton).setOnClickListener(v -> finish());
        addKeywordButton.setOnClickListener(v -> addKeywordFromInput());
        posterButton.setOnClickListener(v -> posterPickerLauncher.launch("image/*"));
        deadlineButton.setOnClickListener(v -> showDatePicker(true));
        eventDateButton.setOnClickListener(v -> showDatePicker(false));
        submitButton.setOnClickListener(v -> submitEvent());
        keywordInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean enterPressed = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (!enterPressed && actionId == android.view.inputmethod.EditorInfo.IME_NULL) {
                return false;
            }
            addKeywordFromInput();
            return true;
        });
        publicSwitch.setChecked(true);
        publicSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> updateVisibilityStatus(isChecked));

        editingEventId = getIntent().getStringExtra("EVENT_ID");
        editMode = hasText(editingEventId);

        showPosterOptionalState();
        updateVisibilityStatus(publicSwitch.isChecked());
        if (editMode) {
            screenTitle.setText(R.string.edit_event_title);
            screenSubtitle.setText(R.string.edit_event_subtitle);
            submitButton.setText(R.string.edit_event_submit);
            loadEventForEditing(editingEventId);
        }
    }

    /**
     * This opens a popup to select the date from a calender display
     * this updates the date variable based on the boolean argument
     * @param forDeadline
     * if the method is run fo event date or registration deadline date
     */
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

    /**
     * This method creates the Event object from the user inputs
     * it has error checking to ensure all mandatory fields are filled
     * non-mandatory fields that are not filled are set to their defaults
     * the created event is uploaded to the database in Events collection
     * notifies user if creation was successful or not
     * if in edit mode modifies the event in database instead of creating a new document
     */
    private void submitEvent() {
        clearErrors();

        String title = readTrimmed(titleInput);
        String description = readTrimmed(descriptionInput);
        String location = readTrimmed(locationInput);
        String winningMessage = readTrimmed(winningMessageInput);
        addKeywordFromInput();

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

        int maxParticipants = 0;
        try {
            maxParticipants = parseRequiredPositiveInt(readTrimmed(maxParticipantsInput));
        } catch (IllegalArgumentException exception) {
            maxParticipantsInput.setError(getString(R.string.max_participants_invalid));
            hasErrors = true;
        }

        if (maxEntrants > 0 && maxParticipants > maxEntrants) {
            maxParticipantsInput.setError(getString(R.string.max_participants_exceeds_entrants));
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
                maxParticipants,
                0,
                toRegistrationDeadline(selectedDeadlineDate),
                toEventDate(selectedEventDate),
                geolocationSwitch.isChecked(),
                currentUser.getUid(),
                "",
                currentCoorganizers,
                true,
                winningMessage,
                keywords,
                publicSwitch.isChecked()
        );

        if (editMode) {
            repository.updateEvent(editingEventId, currentUser, draftEvent, selectedPosterUri)
                    .addOnSuccessListener(eventId -> {
                        setLoading(false);
                        Toast.makeText(
                                CreateEventActivity.this,
                                R.string.event_updated,
                                Toast.LENGTH_SHORT
                        ).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(
                                CreateEventActivity.this,
                                R.string.failed_to_update_event,
                                Toast.LENGTH_SHORT
                        ).show();
                    });
        } else {
            repository.createEvent(currentUser, draftEvent, selectedPosterUri)
                    .addOnSuccessListener(eventId -> {
                        setLoading(false);
                        Toast.makeText(
                                CreateEventActivity.this,
                                R.string.event_created,
                                Toast.LENGTH_SHORT
                        ).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(
                                CreateEventActivity.this,
                                R.string.failed_to_create_event,
                                Toast.LENGTH_SHORT
                        ).show();
                    });
        }
    }

    /**
     * methods clears all errors without managing them
     */
    private void clearErrors() {
        titleInput.setError(null);
        descriptionInput.setError(null);
        locationInput.setError(null);
        keywordInputLayout.setError(null);
        maxEntrantsInput.setError(null);
        maxParticipantsInput.setError(null);
        posterStatus.setError(null);
        deadlineValue.setError(null);
        eventDateValue.setError(null);
    }

    /**
     * method Disables(stops them from being modified) interactive views when creating the event
     * @param loading
     * whether the app is in the process of read/writing to the database or not
     */
    private void setLoading(boolean loading) {
        submitButton.setEnabled(!loading);
        posterButton.setEnabled(!loading);
        deadlineButton.setEnabled(!loading);
        eventDateButton.setEnabled(!loading);
        titleInput.setEnabled(!loading);
        descriptionInput.setEnabled(!loading);
        locationInput.setEnabled(!loading);
        keywordInputLayout.setEnabled(!loading);
        keywordInput.setEnabled(!loading);
        addKeywordButton.setEnabled(!loading);
        maxEntrantsInput.setEnabled(!loading);
        maxParticipantsInput.setEnabled(!loading);
        winningMessageInput.setEnabled(!loading);
        geolocationSwitch.setEnabled(!loading);
        publicSwitch.setEnabled(!loading);
        findViewById(R.id.createEventBackButton).setEnabled(!loading);
    }

    /**
     * loads the current draft event data
     * if event data is loaded runs populateForm()
     * if load fails notifies user though buildLoadErrorMessage()
     * @param eventId
     * the Event ID to get the event from the database
     */
    private void loadEventForEditing(String eventId) {
        setLoading(true);
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            setLoading(false);
            Toast.makeText(this, R.string.unexpected_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        repository.getEventById(eventId)
                .addOnSuccessListener(event -> {
                    if (!EventRepository.canManageEvent(event, currentUser.getUid())) {
                        setLoading(false);
                        Toast.makeText(
                                CreateEventActivity.this,
                                R.string.event_manage_permission_denied,
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                        return;
                    }
                    setLoading(false);
                    populateForm(event);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event for editing", e);
                    setLoading(false);
                    Toast.makeText(
                            CreateEventActivity.this,
                            buildLoadErrorMessage(e),
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                });
    }

    /**
     * loads the event information into the Input Views
     * @param event
     * event whose data will be used to update the views
     */
    private void populateForm(EventItem event) {
        titleInput.setText(event.getTitle());
        descriptionInput.setText(event.getDescription());
        locationInput.setText(event.getLocation());
        keywords.clear();
        keywords.addAll(event.getKeywords());
        currentCoorganizers.clear();
        currentCoorganizers.addAll(event.getCoorganizers());
        renderKeywordChips();
        maxEntrantsInput.setText(event.getMaxEntrants() > 0 ? String.valueOf(event.getMaxEntrants()) : "");
        maxParticipantsInput.setText(event.getMaxParticipants() > 0 ? String.valueOf(event.getMaxParticipants()) : "");
        winningMessageInput.setText(event.getWinningMessage());

        selectedDeadlineDate = toLocalDate(event.getRegistrationDeadline());
        selectedEventDate = toLocalDate(event.getEventDate());
        if (selectedDeadlineDate != null) {
            deadlineValue.setText(formatDate(toRegistrationDeadline(selectedDeadlineDate)));
        }
        if (selectedEventDate != null) {
            eventDateValue.setText(formatDate(toEventDate(selectedEventDate)));
        }

        geolocationSwitch.setChecked(event.isRequiresGeolocation());
        publicSwitch.setChecked(event.isPublic());
        updateVisibilityStatus(event.isPublic());
        currentPosterUrl = event.getPosterUrl();
        showExistingPoster(currentPosterUrl);
    }

    private void addKeywordFromInput() {
        if (keywordInput == null) {
            return;
        }

        String keyword = keywordInput.getText() == null ? "" : keywordInput.getText().toString().trim();
        keywordInputLayout.setError(null);
        if (!hasText(keyword)) {
            keywordInput.setText("");
            return;
        }
        if (containsKeywordIgnoreCase(keyword)) {
            keywordInputLayout.setError(getString(R.string.event_keyword_duplicate));
            return;
        }

        keywords.add(keyword);
        keywordInput.setText("");
        renderKeywordChips();
    }

    private boolean containsKeywordIgnoreCase(String candidate) {
        for (String keyword : keywords) {
            if (keyword.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private void renderKeywordChips() {
        keywordsChipGroup.removeAllViews();
        for (String keyword : keywords) {
            Chip chip = new Chip(this);
            chip.setText(keyword);
            chip.setCloseIconVisible(true);
            chip.setClickable(false);
            chip.setCheckable(false);
            chip.setOnCloseIconClickListener(v -> {
                keywords.remove(keyword);
                renderKeywordChips();
            });
            keywordsChipGroup.addView(chip);
        }
    }

    private void updateVisibilityStatus(boolean isPublic) {
        visibilityStatus.setText(isPublic
                ? R.string.event_visibility_public_state
                : R.string.event_visibility_private_state);
    }

    /**
     * if there is a posterUrl sets the posterPreview to the Event Image
     * tries to load image from database to be displayed
     * on failure show no poster mode and notify user that the load failed
     * @param posterUrl
     * the link to the event image
     */
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

    /**
     * method to manage posterPreview when no event image
     * sets views such that the region that would show the event image ask for image input
     */
    private void showPosterOptionalState() {
        if (selectedPosterUri == null && !hasText(currentPosterUrl)) {
            posterPreview.setVisibility(View.GONE);
            posterPreview.setImageDrawable(null);
        }
        posterStatus.setText(R.string.poster_optional);
        posterStatus.setTextColor(ContextCompat.getColor(this, android.R.color.secondary_text_light));
    }

    /**
     * This changes the locality date to standard form so that
     * @param date
     * Date in Users timezone
     * @return
     * LocalDate the conversion to standard form to manage different timezones
     */
    private LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * method that coverts a raised exception during an event load into a error message to be displayed
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
     * converts user text input into string
     * @param input
     * EditText user input
     * @return
     * string of the user input with extraneous spaces removed
     */
    private String readTrimmed(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    /**
     * check to see if a string is empty
     * @param value
     * the string to be tested
     * @return
     * boolean true if string contains non-space characters
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * converts string into integer
     * @param value
     * the string to be converted
     * @return
     * int of the converted string
     * @exception  IllegalArgumentException
     * if the string is converted to a negative int or cannot be converted
     */
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

    /**
     * converts string into integer with check to see if string is non-empty
     * @param value
     * string to be converted
     * @return
     * converted string into int
     * @exception IllegalArgumentException
     * if the string is empty
     */
    private int parseRequiredPositiveInt(String value) {
        if (!hasText(value)) {
            throw new IllegalArgumentException("Missing number");
        }
        return parseOptionalPositiveInt(value);
    }

    /**
     * converts a user timezone day into standard form
     * @param localDate
     * day in current timezone form
     * @return
     * date in standard from assuming a time just before midnight
     */
    private Date toRegistrationDeadline(LocalDate localDate) {
        return toDate(localDate.atTime(LocalTime.MAX));
    }

    /**
     * converts a user timezone day into standard form
     * @param localDate
     * day in current timezone form
     * @return
     * date in standard from assuming a time of Noon
     */
    private Date toEventDate(LocalDate localDate) {
        return toDate(localDate.atTime(LocalTime.NOON));
    }

    /**
     * converts a user timezone date into standard form
     * @param localDateTime
     * date in current timezone form
     * @return
     * date in standard from
     */
    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * formats date into string form
     * @param date
     * date to be converted
     * @return
     * String form of the date
     */
    private String formatDate(Date date) {
        return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(date);
    }
}
