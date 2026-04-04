package com.example.eventlotterysystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * integrated tests for the private event invitation system
 * verifies that invitations are correctly sent, tracked, and responded to
 */
@RunWith(AndroidJUnit4.class)
public class InvitationIntegrationTest {

    private final NotificationRepository notificationRepository = new NotificationRepository();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    @Before
    public void setUp() throws Exception {
        TestAuthHelper.ensureSharedTestUser();
    }

    /**
     * creates a map containing all required fields for a valid event document
     * @param hostUid the uid of the host
     * @return a map of event data
     */
    private Map<String, Object> createValidEventData(String hostUid) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Integration Test Event");
        data.put("description", "Test Description");
        data.put("location", "Test Location");
        data.put("posterUrl", "");
        data.put("maxEntrants", 100);
        data.put("totalEntrants", 0);
        data.put("maxParticipants", 10);
        data.put("registrationDeadline", new Timestamp(new Date(System.currentTimeMillis() + 86400000)));
        data.put("eventDate", new Timestamp(new Date(System.currentTimeMillis() + 172800000)));
        data.put("requiresGeolocation", false);
        data.put("hostUid", hostUid);
        data.put("hostDisplayName", "Test Host");
        data.put("waitlistOpen", true);
        data.put("deleted", false);
        data.put("createdAt", FieldValue.serverTimestamp());
        return data;
    }


    /**
     * tests that updating an invitation status correctly updates tracking record
     */
    @Test
    public void testRespondToInvitation() throws Exception {
        String currentUid = auth.getCurrentUser().getUid();
        String eventId = "test-respond-event-" + System.currentTimeMillis();

        try {
            Tasks.await(db.collection("events").document(eventId).set(createValidEventData(currentUid)));

            Map<String, Object> inviteData = new HashMap<>();
            inviteData.put("uid", currentUid);
            inviteData.put("name", "Test User");
            inviteData.put("email", "test@user.com");
            inviteData.put("username", "testuser");
            inviteData.put("status", "PENDING");
            inviteData.put("timestamp", FieldValue.serverTimestamp());

            Tasks.await(db.collection("events").document(eventId)
                    .collection("invitations").document(currentUid).set(inviteData));

            Tasks.await(notificationRepository.updateInvitationTrackingStatus(eventId, currentUid, "ACCEPTED"));

            DocumentSnapshot inviteDoc = Tasks.await(db.collection("events").document(eventId)
                    .collection("invitations").document(currentUid).get());

            assertEquals("ACCEPTED", inviteDoc.getString("status"));
        } finally {
            deleteInvitationTestData(eventId, currentUid);
        }
    }

    /**
     * removes the event and invitation tracking record created by an integration test
     * @param eventId
     * test event document id
     * @param uid
     * invited user id
     */
    private void deleteInvitationTestData(String eventId, String uid) {
        try {
            Tasks.await(db.collection("events").document(eventId)
                    .collection("invitations").document(uid).delete());
        } catch (Exception ignored) {
        }
        try {
            Tasks.await(db.collection("events").document(eventId).delete());
        } catch (Exception ignored) {
        }
    }
}
