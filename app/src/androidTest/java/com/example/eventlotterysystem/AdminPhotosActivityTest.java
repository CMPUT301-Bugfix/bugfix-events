package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Tests the functionality of the AdminPhotosActivity
 * this is the activity that lets admins browse uploaded Event posters
 * and remove posters when needed
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminPhotosActivityTest {

    /**
     * Test if the admin can browse uploaded Event posters
     */
    @Test
    public void browseImagesTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String title = "Edmonton Admin Photo Event " + timestamp;
        String posterUrl = uploadPosterForTest("athabasca_hall.jpg", "admin-photo-" + timestamp + ".jpg");
        String eventId = createAdminPhotoTestEvent(title, posterUrl);

        try {
            try (ActivityScenario<AdminPhotosActivity> ignored = ActivityScenario.launch(AdminPhotosActivity.class)) {
                SystemClock.sleep(5000);

                onView(withText(title)).check(matches(isDisplayed()));
                onView(withContentDescription(getRemovePosterDescription(title))).check(matches(isDisplayed()));
            }
        } finally {
            deleteAdminPhotoTestData(eventId, posterUrl);
        }
    }

    /**
     * Test if the admin can remove an uploaded Event poster
     */
    @Test
    public void removeImagesTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String title = "Edmonton Remove Photo Event " + timestamp;
        String posterUrl = uploadPosterForTest("ccis.jpg", "admin-remove-photo-" + timestamp + ".jpg");
        String eventId = createAdminPhotoTestEvent(title, posterUrl);

        try {
            try (ActivityScenario<AdminPhotosActivity> ignored = ActivityScenario.launch(AdminPhotosActivity.class)) {
                SystemClock.sleep(5000);

                onView(withContentDescription(getRemovePosterDescription(title))).perform(click());
                onView(withText(R.string.admin_remove_photo_confirm_action)).perform(click());

                SystemClock.sleep(5000);

                onView(withText(title)).check(doesNotExist());
            }

            DocumentSnapshot eventSnapshot = Tasks.await(FirebaseFirestore.getInstance().collection("events").document(eventId).get(), 15, TimeUnit.SECONDS);
            assertEquals("", eventSnapshot.getString("posterUrl"));
        } finally {
            deleteAdminPhotoTestData(eventId, posterUrl);
        }
    }

    /**
     * signs in the shared test account and ensures that remember-me is disabled
     */
    private void signInTestUser() throws Exception {
        TestAuthHelper.ensureSharedTestUser();
    }

    /**
     * uploads a poster asset to Firebase Storage for an admin photo test
     * @param assetName
     * asset file name inside the androidTest assets directory
     * @param storageName
     * storage file name for the uploaded poster
     * @return
     * download url of the uploaded poster
     */
    private String uploadPosterForTest(String assetName, String storageName) throws Exception {
        byte[] posterBytes = readAssetBytes(assetName);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("event-posters/" + currentUser.getUid() + "/" + storageName);
        Tasks.await(storageReference.putBytes(posterBytes), 15, TimeUnit.SECONDS);
        Uri downloadUri = Tasks.await(storageReference.getDownloadUrl(), 15, TimeUnit.SECONDS);
        return downloadUri.toString();
    }

    /**
     * reads an asset file into bytes for poster upload
     * @param assetName
     * asset file name inside the androidTest assets directory
     * @return
     * file bytes for the given asset
     */
    private byte[] readAssetBytes(String assetName) throws Exception {
        try (InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(assetName);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }

    /**
     * creates an Event document with a poster for an admin photo test
     * @param title
     * title of the Event that should be created
     * @param posterUrl
     * poster url that should be stored on the Event
     * @return
     * the document id of the created Event
     */
    private String createAdminPhotoTestEvent(String title, String posterUrl) throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String eventId = firestore.collection("events").document().getId();

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("title", title);
        eventPayload.put("description", "Admin photo test event in Edmonton.");
        eventPayload.put("location", "SUB Edmonton");
        eventPayload.put("posterUrl", posterUrl);
        eventPayload.put("maxEntrants", 10);
        eventPayload.put("maxParticipants", 5);
        eventPayload.put("totalEntrants", 0);
        eventPayload.put("registrationDeadline", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(4)));
        eventPayload.put("eventDate", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));
        eventPayload.put("requiresGeolocation", false);
        eventPayload.put("hostUid", currentUser.getUid());
        eventPayload.put("hostDisplayName", "Update Test");
        eventPayload.put("waitlistOpen", true);
        eventPayload.put("deleted", false);
        eventPayload.put("createdAt", Timestamp.now());
        eventPayload.put("winningMessage", "Welcome to the Edmonton event.");
        eventPayload.put("coOrganizerUids", new ArrayList<>());
        eventPayload.put("keywords", new ArrayList<>());
        eventPayload.put("isPublic", true);

        Tasks.await(firestore.collection("events").document(eventId).set(eventPayload), 15, TimeUnit.SECONDS);
        return eventId;
    }

    /**
     * removes the Event document and poster file created for an admin photo test
     * @param eventId
     * id of the Event to remove
     * @param posterUrl
     * poster url that should be deleted if it still exists
     */
    private void deleteAdminPhotoTestData(String eventId, String posterUrl) throws Exception {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Tasks.await(firestore.collection("events").document(eventId).delete(), 15, TimeUnit.SECONDS);

        try {
            Tasks.await(FirebaseStorage.getInstance().getReferenceFromUrl(posterUrl).delete(), 15, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    /**
     * builds the remove button content description for a specific Event title
     * @param title
     * title shown on the admin photo card
     * @return
     * content description used by the remove poster button
     */
    private String getRemovePosterDescription(String title) {
        return ApplicationProvider.getApplicationContext().getString(R.string.admin_remove_photo_content_description, title);
    }
}
