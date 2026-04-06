package com.example.eventlotterysystem;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Instrumented test for testing the expiration logic in {@link EventRepository}
 * Verified that winners past the 3-day window are correctly swept and replaced
 */
@RunWith(AndroidJUnit4.class)
public class EventRepositoryExpirationTest {

    private final EventRepository repository = new EventRepository();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    /**
     * signs in the shared test user before each expiration test
     */
    @Before
    public void setUp() throws Exception {
        TestAuthHelper.ensureSharedTestUser();
    }

    /**
     * Tests that processExpiredWinners correctly identifies a winner past the 3-day threshold
     * and marks them as DECLINED in both the event and user documents.
     */
    @Test
    public void testSweepExpiredWinner() throws Exception {
        String currentUid = auth.getCurrentUser().getUid();
        String eventId = "test-event-" + System.currentTimeMillis();
        String userId = "test-user-expired-" + System.currentTimeMillis();

        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("title", "Expiration Test Event");
            eventData.put("hostUid", currentUid);
            eventData.put("maxParticipants", 1);
            eventData.put("waitlistOpen", true);
            eventData.put("totalEntrants", 1);
            Tasks.await(db.collection("events").document(eventId).set(eventData));

            long fourDaysAgoMillis = System.currentTimeMillis() - (4L * 24 * 60 * 60 * 1000);
            Timestamp expiredTimestamp = new Timestamp(new Date(fourDaysAgoMillis));

            Map<String, Object> waitlistEntry = new HashMap<>();
            waitlistEntry.put("status", EventRepository.WAITLIST_STATUS_CHOSEN);
            waitlistEntry.put("chosenAt", expiredTimestamp);
            waitlistEntry.put("uid", userId);

            Tasks.await(db.collection("events").document(eventId)
                    .collection("waitlist").document(userId).set(waitlistEntry));
            Tasks.await(db.collection("users").document(userId)
                    .collection("waitlists").document(eventId).set(waitlistEntry));

            Tasks.await(repository.processExpiredWinners(eventId, "Replacement win notification"));

            DocumentSnapshot eventDoc = Tasks.await(db.collection("events").document(eventId)
                    .collection("waitlist").document(userId).get());
            assertEquals(EventRepository.WAITLIST_STATUS_DECLINED, eventDoc.getString("status"));

            DocumentSnapshot userDoc = Tasks.await(db.collection("users").document(userId)
                    .collection("waitlists").document(eventId).get());
            assertEquals(EventRepository.WAITLIST_STATUS_DECLINED, userDoc.getString("status"));
        } finally {
            deleteExpirationTestData(eventId, userId);
        }
    }

    /**
     * Verifies that winners within the 3-day window are NOT swept and marked as CHOSEN
     */
    @Test
    public void testKeepRecentWinner() throws Exception {
        String currentUid = auth.getCurrentUser().getUid();
        String eventId = "test-event-recent-" + System.currentTimeMillis();
        String userId = "test-user-recent-" + System.currentTimeMillis();

        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("hostUid", currentUid);
            Tasks.await(db.collection("events").document(eventId).set(eventData));

            long oneDayAgoMillis = System.currentTimeMillis() - (1L * 24 * 60 * 60 * 1000);
            Timestamp recentTimestamp = new Timestamp(new Date(oneDayAgoMillis));

            Map<String, Object> waitlistEntry = new HashMap<>();
            waitlistEntry.put("status", EventRepository.WAITLIST_STATUS_CHOSEN);
            waitlistEntry.put("chosenAt", recentTimestamp);
            waitlistEntry.put("uid", userId);

            Tasks.await(db.collection("events").document(eventId)
                    .collection("waitlist").document(userId).set(waitlistEntry));

            Tasks.await(repository.processExpiredWinners(eventId, "Replacement notification"));

            DocumentSnapshot doc = Tasks.await(db.collection("events").document(eventId)
                    .collection("waitlist").document(userId).get());
            assertEquals(EventRepository.WAITLIST_STATUS_CHOSEN, doc.getString("status"));
        } finally {
            deleteExpirationTestData(eventId, userId);
        }
    }

    /**
     * removes the event and any synthetic waitlist records created by an expiration test
     * @param eventId
     * test event document id
     * @param userId
     * synthetic waitlist user id
     */
    private void deleteExpirationTestData(String eventId, String userId) {
        try {
            Tasks.await(db.collection("events").document(eventId)
                    .collection("waitlist").document(userId).delete());
        } catch (Exception ignored) {
        }
        try {
            Tasks.await(db.collection("users").document(userId)
                    .collection("waitlists").document(eventId).delete());
        } catch (Exception ignored) {
        }
        try {
            Tasks.await(db.collection("events").document(eventId).delete());
        } catch (Exception ignored) {
        }
    }
}
