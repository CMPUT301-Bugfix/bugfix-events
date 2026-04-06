package com.example.eventlotterysystem;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class deals with the admin page for browsing Event posters
 * and removing posters from Events
 */
public class AdminPhotosActivity extends AppCompatActivity {

    private static final String TAG = "AdminPhotosActivity";
    private static final long MAX_POSTER_BYTES = 4L * 1024L * 1024L;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private EventRepository eventRepository;

    private TextView backButton;
    private ProgressBar photosLoading;
    private TextView photosEmptyState;
    private ScrollView photosScrollView;
    private LinearLayout photosContainer;

    private ListenerRegistration photosListener;
    private final Set<String> pendingPosterRemovals = new HashSet<>();

    /**
     * This method sets up the admin photos screen and all the views on it
     * @param savedInstanceState
     * the Bundle data from a previous state of the activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_photos);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        eventRepository = new EventRepository();

        backButton = findViewById(R.id.adminPhotosBackButton);
        photosLoading = findViewById(R.id.adminPhotosLoading);
        photosEmptyState = findViewById(R.id.adminPhotosEmptyState);
        photosScrollView = findViewById(R.id.adminPhotosScrollView);
        photosContainer = findViewById(R.id.adminPhotosContainer);

        backButton.setOnClickListener(v -> finish());
    }

    /**
     * This screen checks if the current user is an admin and
     * loads the Event posters if they are allowed to access this page
     */
    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            navigateToAuthMenu();
            return;
        }

        verifyAdminAndLoad(currentUser.getUid());
    }

    /**
     * removes the firestore listener when leaving the screen
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (photosListener != null) {
            photosListener.remove();
            photosListener = null;
        }
    }

    /**
     * checks if the given user is an admin before loading the poster list
     * @param uid
     * the String uid of the current user
     */
    private void verifyAdminAndLoad(@NonNull String uid) {
        setLoading(true);
        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String accountType = snapshot.getString("accountType");
                    if (!"admin".equals(accountType)) {
                        finish();
                        return;
                    }
                    loadPhotos();
                })
                .addOnFailureListener(exception -> finish());
    }

    /**
     * loads all Events with posters from the database and listens for changes
     */
    private void loadPhotos() {
        setLoading(true);

        if (photosListener != null) {
            photosListener.remove();
            photosListener = null;
        }

        Query query = firestore.collection("events");
        photosListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                setLoading(false);
                renderPhotos(new ArrayList<>());
                Log.e(TAG, "Failed to load admin photos", error);
                Toast.makeText(this, R.string.admin_photos_load_failed, Toast.LENGTH_LONG).show();
                return;
            }

            List<EventItem> eventsWithPhotos = new ArrayList<>();
            if (value != null) {
                for (DocumentSnapshot snapshot : value.getDocuments()) {
                    if (Boolean.TRUE.equals(snapshot.getBoolean("deleted"))) {
                        continue;
                    }

                    EventItem event = eventRepository.readEventItem(snapshot);
                    if (!hasText(event.getPosterUrl())) {
                        continue;
                    }

                    eventsWithPhotos.add(event);
                }
            }

            eventsWithPhotos.sort((a, b) -> normalize(a.getTitle()).compareToIgnoreCase(normalize(b.getTitle())));
            renderPhotos(eventsWithPhotos);
            setLoading(false);
        });
    }

    /**
     * shows the Event posters on the screen in cards for the admin
     * @param eventsWithPhotos
     * the list of Events that have posters
     */
    private void renderPhotos(@NonNull List<EventItem> eventsWithPhotos) {
        photosContainer.removeAllViews();

        if (eventsWithPhotos.isEmpty()) {
            photosEmptyState.setVisibility(View.VISIBLE);
            photosScrollView.setVisibility(View.GONE);
            return;
        }

        photosEmptyState.setVisibility(View.GONE);
        photosScrollView.setVisibility(View.VISIBLE);

        for (EventItem event : eventsWithPhotos) {
            View card = getLayoutInflater().inflate(R.layout.item_admin_photo, photosContainer, false);
            TextView titleView = card.findViewById(R.id.adminPhotoEventTitle);
            ImageView posterView = card.findViewById(R.id.adminPhotoImage);
            ImageButton removeButton = card.findViewById(R.id.adminPhotoRemoveButton);

            String eventTitle = hasText(event.getTitle())
                    ? event.getTitle().trim()
                    : getString(R.string.unknown_event_title);
            titleView.setText(eventTitle);
            removeButton.setContentDescription(getString(R.string.admin_remove_photo_content_description, eventTitle));

            bindPoster(posterView, event.getPosterUrl());

            boolean isPendingRemoval = pendingPosterRemovals.contains(event.getId());
            removeButton.setEnabled(!isPendingRemoval);
            removeButton.setAlpha(isPendingRemoval ? 0.5f : 1f);
            removeButton.setOnClickListener(v -> confirmRemovePoster(event, removeButton));

            photosContainer.addView(card);
        }
    }

    /**
     * loads the poster image into the ImageView for a given poster url
     * @param posterView
     * the ImageView that will display the poster
     * @param posterUrl
     * the String url of the poster in Firebase Storage
     */
    private void bindPoster(@NonNull ImageView posterView, String posterUrl) {
        posterView.setImageResource(android.R.drawable.ic_menu_gallery);

        try {
            FirebaseStorage.getInstance()
                    .getReferenceFromUrl(posterUrl)
                    .getBytes(MAX_POSTER_BYTES)
                    .addOnSuccessListener(bytes -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        posterView.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                    })
                    .addOnFailureListener(exception -> {
                        Log.e(TAG, "Failed to load event poster", exception);
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        posterView.setImageDrawable(null);
                    });
        } catch (IllegalArgumentException exception) {
            Log.e(TAG, "Invalid event poster URL", exception);
            posterView.setImageDrawable(null);
        }
    }

    /**
     * shows the confirmation popup before the admin removes a Event poster
     * @param event
     * the EventItem whose poster is being removed
     * @param removeButton
     * the ImageButton that triggered the remove action
     */
    private void confirmRemovePoster(@NonNull EventItem event, @NonNull ImageButton removeButton) {
        if (pendingPosterRemovals.contains(event.getId())) {
            return;
        }

        String title = hasText(event.getTitle()) ? event.getTitle().trim() : getString(R.string.unknown_event_title);
        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_remove_photo_confirm_title)
                .setMessage(getString(R.string.admin_remove_photo_confirm_message, title))
                .setNegativeButton(R.string.admin_remove_photo_cancel_action, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.admin_remove_photo_confirm_action,
                        (dialog, which) -> removePoster(event, removeButton))
                .show();
    }

    /**
     * removes the poster url from the Event document and starts deleting the poster file
     * @param event
     * the EventItem whose poster is being removed
     * @param removeButton
     * the ImageButton used to remove the poster
     */
    private void removePoster(@NonNull EventItem event, @NonNull ImageButton removeButton) {
        String eventId = normalize(event.getId());
        String posterUrl = normalize(event.getPosterUrl());
        if (eventId.isEmpty() || posterUrl.isEmpty()) {
            return;
        }

        pendingPosterRemovals.add(eventId);
        removeButton.setEnabled(false);
        removeButton.setAlpha(0.5f);

        firestore.collection("events")
                .document(eventId)
                .update("posterUrl", "")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, R.string.admin_remove_photo_success, Toast.LENGTH_LONG).show();
                    deletePosterFromStorage(eventId, posterUrl);
                })
                .addOnFailureListener(exception -> {
                    pendingPosterRemovals.remove(eventId);
                    removeButton.setEnabled(true);
                    removeButton.setAlpha(1f);
                    Log.e(TAG, "Failed to remove event poster", exception);
                    Toast.makeText(this, R.string.admin_remove_photo_failed, Toast.LENGTH_LONG).show();
                });
    }

    /**
     * deletes the poster file from Firebase Storage after it has been removed from the Event
     * @param eventId
     * the String id of the Event
     * @param posterUrl
     * the String url of the poster in Firebase Storage
     */
    private void deletePosterFromStorage(@NonNull String eventId, @NonNull String posterUrl) {
        try {
            FirebaseStorage.getInstance()
                    .getReferenceFromUrl(posterUrl)
                    .delete()
                    .addOnSuccessListener(unused -> pendingPosterRemovals.remove(eventId))
                    .addOnFailureListener(exception -> {
                        pendingPosterRemovals.remove(eventId);
                        Log.w(TAG, "Poster removed from event but storage cleanup failed", exception);
                        Toast.makeText(this, R.string.admin_remove_photo_cleanup_failed, Toast.LENGTH_LONG).show();
                    });
        } catch (IllegalArgumentException exception) {
            pendingPosterRemovals.remove(eventId);
            Log.w(TAG, "Poster removed from event but storage URL was invalid", exception);
            Toast.makeText(this, R.string.admin_remove_photo_cleanup_failed, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * updates the loading state of the admin photos screen
     * @param loading
     * true if the screen is loading, false otherwise
     */
    private void setLoading(boolean loading) {
        photosLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        backButton.setEnabled(!loading);
        if (loading) {
            photosEmptyState.setVisibility(View.GONE);
            photosScrollView.setVisibility(View.GONE);
        }
    }

    /**
     * signs the user out and sends them back to the Auth Menu
     */
    private void navigateToAuthMenu() {
        auth.signOut();
        startActivity(new Intent(this, AuthMenuActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    /**
     * checks if a String has text in it
     * @param value
     * the String being checked
     * @return
     * true if the String is not null or blank, false otherwise
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * removes surrounding spaces from a String and returns an empty String if it is null
     * @param value
     * the String being normalized
     * @return
     * the trimmed String, or an empty String if the value is null
     */
    @NonNull
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
