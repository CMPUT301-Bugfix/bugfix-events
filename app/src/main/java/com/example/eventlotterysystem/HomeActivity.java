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

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private EventRepository repository;
    private TextView homeEventsEmptyState;
    private ListView homeEventsListView;
    private EventListAdapter adapter;
    private final List<EventItem> events = new ArrayList<>();

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

    private void scanCode(){
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a QR Code");
        options.setBeepEnabled(true);
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setCameraId(0);
        options.setOrientationLocked(false);
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

    private void loadJoinableEvents() {
        repository.getCurrentEvents(new EventRepository.EventsCallback() {
            @Override
            public void onSuccess(List<EventItem> loadedEvents) {
                events.clear();
                events.addAll(loadedEvents);
                adapter.notifyDataSetChanged();
                updateEmptyState();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to load home events", e);
                events.clear();
                adapter.notifyDataSetChanged();
                updateEmptyState();
                Toast.makeText(
                        HomeActivity.this,
                        buildLoadErrorMessage(e),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void updateEmptyState() {
        boolean hasEvents = !events.isEmpty();
        homeEventsListView.setVisibility(hasEvents ? View.VISIBLE : View.GONE);
        homeEventsEmptyState.setVisibility(hasEvents ? View.GONE : View.VISIBLE);
    }

    private String buildLoadErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return getString(R.string.failed_to_load_events) + ": " + exception.getMessage().trim();
        }
        return getString(R.string.failed_to_load_events);
    }

    private void navigateToAuthMenu() {
        auth.signOut();
        Intent intent = new Intent(this, AuthMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToProfileRemoved() {
        Intent intent = new Intent(this, ProfileRemovedActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
