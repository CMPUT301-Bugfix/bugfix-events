package com.example.eventlotterysystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.location.Location;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Tests the geolocation verification flow in EventRepository
 */
@RunWith(AndroidJUnit4.class)
public class EventRepositoryGeolocationTest {

    private final EventRepository repository = new EventRepository();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    /**
     * signs in the shared test account and ensures that remember me is disabled
     */
    @Before
    public void setUp() throws Exception {
        FirebaseAuth.getInstance().signOut();
        Context context = ApplicationProvider.getApplicationContext();
        AuthSessionPreference.setRemember(context, false);
        Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword("test@gmail.com", "test123"), 15, TimeUnit.SECONDS);
    }

    /**
     * tests that joining a geolocation-required event stores the captured location in both waitlist records
     */
    @Test
    public void joinRequiredGeolocationEventWithLocationTest() throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String eventId = createGeolocationTestEvent("Required Geolocation Event " + System.currentTimeMillis(), true);
        Location location = new Location("test");
        location.setLatitude(53.5232);
        location.setLongitude(-113.5263);

        try {
            Tasks.await(repository.joinWaitlist(eventId, currentUser, location), 15, TimeUnit.SECONDS);

            DocumentSnapshot eventWaitlist = Tasks.await(firestore.collection("events").document(eventId).collection("waitlist").document(currentUser.getUid()).get(), 15, TimeUnit.SECONDS);
            DocumentSnapshot userWaitlist = Tasks.await(firestore.collection("users").document(currentUser.getUid()).collection("waitlists").document(eventId).get(), 15, TimeUnit.SECONDS);
            GeoPoint eventLocation = eventWaitlist.getGeoPoint("location");
            GeoPoint userLocation = userWaitlist.getGeoPoint("location");

            assertTrue(eventWaitlist.exists());
            assertTrue(userWaitlist.exists());
            assertNotNull(eventLocation);
            assertNotNull(userLocation);
            assertEquals(53.5232, eventLocation.getLatitude(), 0.0001);
            assertEquals(-113.5263, eventLocation.getLongitude(), 0.0001);
            assertEquals(53.5232, userLocation.getLatitude(), 0.0001);
            assertEquals(-113.5263, userLocation.getLongitude(), 0.0001);
        } finally {
            deleteGeolocationTestData(eventId, currentUser.getUid());
        }
    }

    /**
     * tests that joining a geolocation-required event without a captured location is blocked
     */
    @Test
    public void joinRequiredGeolocationEventWithoutLocationFailsTest() throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String eventId = createGeolocationTestEvent("Required Geolocation Failure Event " + System.currentTimeMillis(), true);

        try {
            boolean failed = false;
            try {
                Tasks.await(repository.joinWaitlist(eventId, currentUser, null), 15, TimeUnit.SECONDS);
            } catch (Exception exception) {
                failed = true;
            }

            DocumentSnapshot eventWaitlist = Tasks.await(firestore.collection("events").document(eventId).collection("waitlist").document(currentUser.getUid()).get(), 15, TimeUnit.SECONDS);
            DocumentSnapshot userWaitlist = Tasks.await(firestore.collection("users").document(currentUser.getUid()).collection("waitlists").document(eventId).get(), 15, TimeUnit.SECONDS);
            DocumentSnapshot event = Tasks.await(firestore.collection("events").document(eventId).get(), 15, TimeUnit.SECONDS);

            assertTrue(failed);
            assertFalse(eventWaitlist.exists());
            assertFalse(userWaitlist.exists());
            assertEquals(0L, event.getLong("totalEntrants").longValue());
        } finally {
            deleteGeolocationTestData(eventId, currentUser.getUid());
        }
    }

    /**
     * tests that joining an event without a geolocation requirement still succeeds without a stored location
     */
    @Test
    public void joinOptionalGeolocationEventWithoutLocationTest() throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String eventId = createGeolocationTestEvent("Optional Geolocation Event " + System.currentTimeMillis(), false);

        try {
            Tasks.await(repository.joinWaitlist(eventId, currentUser, null), 15, TimeUnit.SECONDS);

            DocumentSnapshot eventWaitlist = Tasks.await(firestore.collection("events").document(eventId).collection("waitlist").document(currentUser.getUid()).get(), 15, TimeUnit.SECONDS);
            DocumentSnapshot userWaitlist = Tasks.await(firestore.collection("users").document(currentUser.getUid()).collection("waitlists").document(eventId).get(), 15, TimeUnit.SECONDS);

            assertTrue(eventWaitlist.exists());
            assertTrue(userWaitlist.exists());
            assertNull(eventWaitlist.getGeoPoint("location"));
            assertNull(userWaitlist.getGeoPoint("location"));
        } finally {
            deleteGeolocationTestData(eventId, currentUser.getUid());
        }
    }

    /**
     * creates an event document that can be used to test joining the waitlist
     * @param title
     * title of the event to create
     * @param requiresGeolocation
     * whether the event should require device geolocation to join
     * @return
     * the document id of the created event
     */
    private String createGeolocationTestEvent(String title, boolean requiresGeolocation) throws Exception {
        String eventId = firestore.collection("events").document().getId();
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("title", title);
        eventPayload.put("description", "Geolocation repository test in Edmonton.");
        eventPayload.put("location", "SUB Edmonton");
        eventPayload.put("posterUrl", "");
        eventPayload.put("maxEntrants", 10);
        eventPayload.put("maxParticipants", 5);
        eventPayload.put("totalEntrants", 0);
        eventPayload.put("registrationDeadline", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2)));
        eventPayload.put("eventDate", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)));
        eventPayload.put("requiresGeolocation", requiresGeolocation);
        eventPayload.put("hostUid", "geolocation-test-host");
        eventPayload.put("hostDisplayName", "Geolocation Host");
        eventPayload.put("waitlistOpen", true);
        eventPayload.put("deleted", false);
        eventPayload.put("createdAt", Timestamp.now());
        eventPayload.put("winningMessage", "Welcome to the geolocation test event.");

        Tasks.await(firestore.collection("events").document(eventId).set(eventPayload), 15, TimeUnit.SECONDS);
        return eventId;
    }

    /**
     * removes all waitlist records and the event document created for a geolocation test
     * @param eventId
     * document id of the event to remove
     * @param uid
     * document id of the signed-in user to remove from the waitlist
     */
    private void deleteGeolocationTestData(String eventId, String uid) throws Exception {
        Tasks.await(firestore.collection("users").document(uid).collection("waitlists").document(eventId).delete(), 15, TimeUnit.SECONDS);
        Tasks.await(firestore.collection("events").document(eventId).collection("waitlist").document(uid).delete(), 15, TimeUnit.SECONDS);
        Tasks.await(firestore.collection("events").document(eventId).delete(), 15, TimeUnit.SECONDS);
    }
}
