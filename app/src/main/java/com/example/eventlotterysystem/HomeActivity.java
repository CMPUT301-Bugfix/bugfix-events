package com.example.eventlotterysystem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.List;


/**
 * This is a class that is the controller of the activity_home screen
 * it is the starting Activity of normal users and shows a list of current event
 * the user is able to navigate to the other sections of the app through this activity
 */
public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private EventRepository repository;
    private TextView homeEventsEmptyState;
    private ListView homeEventsListView;
    private EventListAdapter adapter;
    private final List<EventItem> events = new ArrayList<>();

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

        adapter = new EventListAdapter(this, events);
        homeEventsListView.setAdapter(adapter);

        findViewById(R.id.myThingsButton).setOnClickListener(v ->
                startActivity(new Intent(this, MyThingsActivity.class)));

        findViewById(R.id.qrCodeScan).setOnClickListener(v -> scanCode());

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
                    events.clear();
                    events.addAll(loadedEvents);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load home events", e);
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
}
