package com.example.eventlotterysystem;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;


/**
 * This is a class that is the controller of the activity_home screen
 * it is the starting Activity of normal users and shows a list of current event
 * the user is able to navigate to the other sections of the app through this activity
 */
public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private static final String DATE_PATTERN = "MMM d, yyyy";

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private EventRepository repository;
    private TextView homeEventsEmptyState;
    private ListView homeEventsListView;
    private Button homeEventsFilterButton;
    private EventListAdapter adapter;
    private final List<EventItem> events = new ArrayList<>();
    private final List<EventItem> allEvents = new ArrayList<>();
    private final List<String> activeKeywordQueries = new ArrayList<>();
    private Date activeEventDateFrom;
    private Date activeEventDateTo;
    private Integer activeMinEntrants;
    private Integer activeMaxEntrants;

    /**
     * This is the creation of the Activity
     * This connects to all the view on the screen and connects the clickable view to their controller
     * @param savedInstanceState
     * the saved state of the Activity so that the screen is not reset
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        repository = new EventRepository();
        homeEventsEmptyState = findViewById(R.id.homeEventsEmptyState);
        homeEventsListView = findViewById(R.id.homeEventsListView);
        homeEventsFilterButton = findViewById(R.id.homeEventsFilterButton);

        adapter = new EventListAdapter(this, events);
        homeEventsListView.setAdapter(adapter);

        findViewById(R.id.myThingsButton).setOnClickListener(v ->
                startActivity(new Intent(this, MyThingsActivity.class)));

        findViewById(R.id.qrCodeScan).setOnClickListener(v -> scanCode());
        homeEventsFilterButton.setOnClickListener(v -> showFilterDialog());

        homeEventsListView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, ViewEventActivity.class);
            intent.putExtra("EVENT_ID", events.get(position).getId());
            startActivity(intent);
        });
    }

    /**
     * This is the startup of the Activity
     * This gets the current user from the database and runs verifyActiveProfileAndRender to get user data
     */
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            navigateToAuthMenu();
            return;
        }

        currentUser.reload().addOnCompleteListener(task -> {
            FirebaseUser refreshedUser = auth.getCurrentUser();
            if (!task.isSuccessful() || refreshedUser == null) {
                verifyActiveProfileAndRender(currentUser);
                return;
            }
            verifyActiveProfileAndRender(refreshedUser);
        });
    }

    /**
     * Configures and launches the QR code scanner interface.
     * <p>
     * this method sets the scanner options, including prompt text, beep sound,
     * and camera uses (back and front camera).
     * it use a custom {@link CaptureActivity} to manage the scanning UI
     */
    private void scanCode(){
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a QR Code");
        options.setBeepEnabled(true);
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setCaptureActivity(CaptureActivity.class);
        options.setCameraId(0);
        barLauncher.launch(options);
    }

    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result -> {
       if(result.getContents() != null){
            String scanResults = result.getContents();

            try{
                Uri uri = Uri.parse(scanResults);

                if("myapp".equals(uri.getScheme()) && "event".equals(uri.getHost())){
                    String eventID = uri.getQueryParameter("id");
                    if(eventID != null && !eventID.isEmpty()){
                        Intent intent = new Intent(this, ViewEventActivity.class);
                        intent.putExtra("EVENT_ID", eventID);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Missing Event ID",Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Unregister QR code", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e){
                Toast.makeText(this, "Error reading QR code", Toast.LENGTH_SHORT).show();
            }
       } else {
           Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
       }
    });

    /**
     * method that loads the user from the database and ensure the user profile is not deleted
     * if it is deleted run navigateToProfileRemoved()
     * @param user
     * The user data retried from the database
     */
    private void verifyActiveProfileAndRender(FirebaseUser user) {
        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean deleted = !snapshot.exists() || Boolean.TRUE.equals(snapshot.getBoolean("deleted"));
                    if (deleted) {
                        auth.signOut();
                        AuthSessionPreference.setRemember(this, false);
                        navigateToProfileRemoved();
                        return;
                    }
                    loadJoinableEvents();
                })
                .addOnFailureListener(exception -> loadJoinableEvents());
    }

    /**
     * method that loads all events from the database and updates the adapter to display them
     * on failure runs updateEmptyState and creates an error message through buildLoadErrorMessage()
     */
    private void loadJoinableEvents() {
        repository.getCurrentEvents()
                .addOnSuccessListener(loadedEvents -> {
                    allEvents.clear();
                    allEvents.addAll(loadedEvents);
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load home events", e);
                    allEvents.clear();
                    events.clear();
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    Toast.makeText(
                            HomeActivity.this,
                            buildLoadErrorMessage(e),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void showFilterDialog() {
        View filterDialog = LayoutInflater.from(this).inflate(R.layout.dialog_event_filters, homeEventsListView, false);
        TextInputLayout keywordField = filterDialog.findViewById(R.id.filterKeywordInputLayout);
        AutoCompleteTextView keywordInput = filterDialog.findViewById(R.id.filterKeywordInput);
        ChipGroup keywordChips = filterDialog.findViewById(R.id.filterKeywordChipGroup);
        Button startDateButton = filterDialog.findViewById(R.id.filterDateFromButton);
        TextView startDateText = filterDialog.findViewById(R.id.filterDateFromValue);
        Button endDateButton = filterDialog.findViewById(R.id.filterDateToButton);
        TextView endDateText = filterDialog.findViewById(R.id.filterDateToValue);
        TextInputLayout minEntrantsField = filterDialog.findViewById(R.id.filterMinParticipantsInputLayout);
        EditText minEntrantsInput = filterDialog.findViewById(R.id.filterMinParticipantsInput);
        TextInputLayout maxEntrantsField = filterDialog.findViewById(R.id.filterMaxParticipantsInputLayout);
        EditText maxEntrantsInput = filterDialog.findViewById(R.id.filterMaxParticipantsInput);

        List<String> draftKeywords = new ArrayList<>(activeKeywordQueries);
        List<String> keywordSuggestions = buildKeywordSuggestions();
        ArrayAdapter<String> keywordAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, keywordSuggestions);
        keywordInput.setAdapter(keywordAdapter);
        keywordInput.setThreshold(0);
        keywordInput.setText("");
        renderSelectedKeywordChips(keywordChips, draftKeywords);
        keywordInput.setOnItemClickListener((parent, view, position, id) -> addKeywordSelection(keywordInput, draftKeywords, keywordChips));
        keywordInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                addKeywordSelection(keywordInput, draftKeywords, keywordChips);
                return true;
            }
            return false;
        });
        keywordInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !keywordSuggestions.isEmpty()) {
                keywordInput.post(keywordInput::showDropDown);
            }
        });
        keywordInput.setOnClickListener(v -> {
            if (!keywordSuggestions.isEmpty()) {
                keywordInput.showDropDown();
            }
        });
        minEntrantsInput.setText(activeMinEntrants == null ? "" : String.valueOf(activeMinEntrants));
        maxEntrantsInput.setText(activeMaxEntrants == null ? "" : String.valueOf(activeMaxEntrants));

        final Date[] draftFromDate = new Date[] { activeEventDateFrom };
        final Date[] draftToDate = new Date[] { activeEventDateTo };
        updateDateText(startDateText, draftFromDate[0]);
        updateDateText(endDateText, draftToDate[0]);

        startDateButton.setOnClickListener(v -> showDatePicker(draftFromDate[0], selectedDate -> {
            draftFromDate[0] = selectedDate;
            startDateText.setError(null);
            updateDateText(startDateText, selectedDate);
        }));
        endDateButton.setOnClickListener(v -> showDatePicker(draftToDate[0], selectedDate -> {
            draftToDate[0] = selectedDate;
            endDateText.setError(null);
            updateDateText(endDateText, selectedDate);
        }));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.filter_dialog_title)
                .setView(filterDialog)
                .setPositiveButton(R.string.filter_apply, null)
                .setNegativeButton(R.string.back, null)
                .setNeutralButton(R.string.filter_clear, null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                keywordField.setError(null);
                minEntrantsField.setError(null);
                maxEntrantsField.setError(null);
                startDateText.setError(null);
                endDateText.setError(null);

                Integer minEntrants;
                Integer maxEntrants;
                try {
                    minEntrants = parseBound(minEntrantsInput);
                } catch (IllegalArgumentException exception) {
                    minEntrantsField.setError(getString(R.string.max_entrants_invalid));
                    return;
                }

                try {
                    maxEntrants = parseBound(maxEntrantsInput);
                } catch (IllegalArgumentException exception) {
                    maxEntrantsField.setError(getString(R.string.max_entrants_invalid));
                    return;
                }

                if (draftFromDate[0] != null && draftToDate[0] != null && draftFromDate[0].after(draftToDate[0])) {
                    startDateText.setError(getString(R.string.filter_invalid_date_range));
                    endDateText.setError(getString(R.string.filter_invalid_date_range));
                    return;
                }

                if (minEntrants != null && maxEntrants != null && minEntrants > maxEntrants) {
                    minEntrantsField.setError(getString(R.string.filter_invalid_capacity_range));
                    maxEntrantsField.setError(getString(R.string.filter_invalid_capacity_range));
                    return;
                }

                addKeywordSelection(keywordInput, draftKeywords, keywordChips);
                activeKeywordQueries.clear();
                activeKeywordQueries.addAll(draftKeywords);
                activeEventDateFrom = draftFromDate[0];
                activeEventDateTo = draftToDate[0];
                activeMinEntrants = minEntrants;
                activeMaxEntrants = maxEntrants;
                applyFilters();
                dialog.dismiss();
            });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                keywordField.setError(null);
                minEntrantsField.setError(null);
                maxEntrantsField.setError(null);
                startDateText.setError(null);
                endDateText.setError(null);

                draftKeywords.clear();
                renderSelectedKeywordChips(keywordChips, draftKeywords);
                keywordInput.setText("");
                draftFromDate[0] = null;
                draftToDate[0] = null;
                updateDateText(startDateText, null);
                updateDateText(endDateText, null);
                minEntrantsInput.setText("");
                maxEntrantsInput.setText("");

                activeKeywordQueries.clear();
                activeEventDateFrom = null;
                activeEventDateTo = null;
                activeMinEntrants = null;
                activeMaxEntrants = null;
                applyFilters();
                keywordInput.requestFocus();
            });
        });

        dialog.show();
    }

    private void applyFilters() {
        events.clear();
        for (EventItem event : allEvents) {
            if (matchesFilters(event)) {
                events.add(event);
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    /**
     * method that changes the views of the screen such that there is text stating no events when event list is empty
     */
    private void updateEmptyState() {
        boolean hasEvents = !events.isEmpty();
        homeEventsListView.setVisibility(hasEvents ? View.VISIBLE : View.GONE);
        homeEventsEmptyState.setVisibility(hasEvents ? View.GONE : View.VISIBLE);
    }

    /**
     * method that coverts a raised exception during an user data load from database into a error message to be displayed
     * @return
     * a String message describing what the error was
     */
    private String buildLoadErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return getString(R.string.failed_to_load_events) + ": " + exception.getMessage().trim();
        }
        return getString(R.string.failed_to_load_events);
    }

    /**
     * navigates to AuthMenuActivity
     * method that runs when there is no current user
     */
    private void navigateToAuthMenu() {
        auth.signOut();
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * navigates to ProfileRemovedActivity
     * method that runs when the profile is deleted
     */
    private void navigateToProfileRemoved() {
        Intent intent = new Intent(this, ProfileRemovedActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showDatePicker(Date currentValue, Consumer<Date> listener) {
        Calendar calendar = Calendar.getInstance();
        if (currentValue != null) {
            calendar.setTime(currentValue);
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> listener.accept(
                        Date.from(
                                LocalDate.of(year, month + 1, dayOfMonth)
                                        .atStartOfDay(ZoneId.systemDefault())
                                        .toInstant()
                        )
                ),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void updateDateText(TextView textView, Date date) {
        textView.setText(date == null ? getString(R.string.filter_any_date) : formatDate(date));
    }

    private String readTrimmed(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private Integer parseBound(EditText input) {
        String value = readTrimmed(input);
        if (value.isEmpty()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException("Must be positive");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid number", exception);
        }
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(date);
    }

    private List<String> buildKeywordSuggestions() {
        Set<String> keywords = new LinkedHashSet<>();
        for (EventItem event : allEvents) {
            for (String keyword : event.getKeywords()) {
                if (keyword != null && !keyword.trim().isEmpty()) {
                    keywords.add(keyword.trim());
                }
            }
        }
        List<String> suggestions = new ArrayList<>(keywords);
        suggestions.sort(String.CASE_INSENSITIVE_ORDER);
        return suggestions;
    }

    private static boolean matchesEventDateRange(EventItem event, Date fromDate, Date toDate) {
        if (fromDate == null && toDate == null) {
            return true;
        }
        Date eventDate = event.getEventDate();
        if (eventDate == null) {
            return false;
        }

        LocalDate eventLocalDate = toLocalDate(eventDate);
        if (fromDate != null && eventLocalDate.isBefore(toLocalDate(fromDate))) {
            return false;
        }
        return toDate == null || !eventLocalDate.isAfter(toLocalDate(toDate));
    }

    private static boolean matchesMaxEntrantsRange(EventItem event, Integer minEntrants, Integer maxEntrants) {
        int maxEntrantsValue = event.getMaxEntrants();
        if (minEntrants != null && maxEntrantsValue < minEntrants) {
            return false;
        }
        return maxEntrants == null || maxEntrantsValue <= maxEntrants;
    }

    private boolean matchesFilters(EventItem event) {
        if (!activeKeywordQueries.isEmpty()) {
            boolean keywordMatch = false;
            for (String selectedKeyword : activeKeywordQueries) {
                String normalizedKeyword = selectedKeyword.trim().toLowerCase(Locale.ROOT);
                for (String eventKeyword : event.getKeywords()) {
                    if (eventKeyword != null && eventKeyword.toLowerCase(Locale.ROOT).contains(normalizedKeyword)) {
                        keywordMatch = true;
                        break;
                    }
                }
                if (keywordMatch) {
                    break;
                }
            }
            if (!keywordMatch) {
                return false;
            }
        }

        return matchesEventDateRange(event, activeEventDateFrom, activeEventDateTo)
                && matchesMaxEntrantsRange(event, activeMinEntrants, activeMaxEntrants);
    }

    private void addKeywordSelection(
            AutoCompleteTextView keywordInput,
            List<String> draftKeywords,
            ChipGroup keywordChipGroup
    ) {
        String keyword = readTrimmed(keywordInput);
        if (keyword.isEmpty()) {
            keywordInput.setText("");
            return;
        }

        for (String existingKeyword : draftKeywords) {
            if (existingKeyword.equalsIgnoreCase(keyword)) {
                keywordInput.setText("");
                return;
            }
        }

        draftKeywords.add(keyword);
        draftKeywords.sort(String.CASE_INSENSITIVE_ORDER);
        keywordInput.setText("");
        renderSelectedKeywordChips(keywordChipGroup, draftKeywords);
    }

    private void renderSelectedKeywordChips(ChipGroup keywordChipGroup, List<String> draftKeywords) {
        keywordChipGroup.removeAllViews();
        for (String keyword : draftKeywords) {
            Chip chip = new Chip(this);
            chip.setText(keyword);
            chip.setCloseIconVisible(true);
            chip.setClickable(false);
            chip.setCheckable(false);
            chip.setOnCloseIconClickListener(v -> {
                draftKeywords.remove(keyword);
                renderSelectedKeywordChips(keywordChipGroup, draftKeywords);
            });
            keywordChipGroup.addView(chip);
        }
    }

    private static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

}
