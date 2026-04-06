package com.example.eventlotterysystem;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for notification preference delivery filtering.
 */
@RunWith(AndroidJUnit4.class)
public class NotificationPreferenceIntegrationTest {

    /**
     * verifies that a user who opts out does not receive winning, general, private, or coorganizer notifications
     */
    @Test
    public void optedOutUserDoesNotReceiveSuppressedNotificationsTest() throws Exception {
        TestAuthHelper.ensureSharedTestUser();
        NotificationRepository repository = new NotificationRepository();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String eventId = "opt-out-event-" + timestamp;
        String coorganizerEventId = "opt-out-coorg-" + timestamp;
        String email = "optout" + timestamp + "@gmail.com";
        String password = "test123";
        String username = "optout" + timestamp;
        String fullName = "Opt Out User " + timestamp;
        String uid = createTemporaryUserWithPreferences(
                email,
                password,
                username,
                fullName,
                false,
                false,
                false,
                false
        );

        try {
            TestAuthHelper.ensureSharedTestUser();
            FirebaseUser sharedUser = FirebaseAuth.getInstance().getCurrentUser();
            createNotificationTestEvent(eventId, sharedUser.getUid(), "Opt Out Event " + timestamp);
            createNotificationTestEvent(coorganizerEventId, sharedUser.getUid(), "Opt Out Coorganizer Event " + timestamp);

            List<String> recipientUids = java.util.Collections.singletonList(uid);
            UserProfile invitedUser = new UserProfile(fullName, email, username, username.toLowerCase(), "780 555 1122", "user");
            invitedUser.setUid(uid);
            List<UserProfile> invitedUsers = new ArrayList<>();
            invitedUsers.add(invitedUser);

            Tasks.await(repository.sendToSpecificUsers(eventId, "Opt Out Win", "Win message", "WIN", recipientUids), 15, TimeUnit.SECONDS);
            Tasks.await(repository.sendToSpecificUsers(eventId, "Opt Out General", "General message", "GENERAL", recipientUids), 15, TimeUnit.SECONDS);
            Tasks.await(repository.sendInvitations(eventId, "Opt Out Private Invite", "Private invite message", invitedUsers), 15, TimeUnit.SECONDS);
            Tasks.await(repository.sendCoOrganizerInvitation(coorganizerEventId, "Opt Out Coorganizer Invite", "Coorganizer invite message", invitedUsers), 15, TimeUnit.SECONDS);

            signInUser(email, password);
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            QuerySnapshot notifications = Tasks.await(
                    firestore.collection("users").document(uid).collection("notifications").get(),
                    15,
                    TimeUnit.SECONDS
            );
            DocumentSnapshot privateInvite = Tasks.await(
                    firestore.collection("events").document(eventId).collection("invitations").document(uid).get(),
                    15,
                    TimeUnit.SECONDS
            );
            DocumentSnapshot coorganizerInvite = Tasks.await(
                    firestore.collection("events").document(coorganizerEventId).collection("coorganizerinvites").document(uid).get(),
                    15,
                    TimeUnit.SECONDS
            );

            assertTrue(notifications.isEmpty());
            assertFalse(privateInvite.exists());
            assertFalse(coorganizerInvite.exists());
        } finally {
            cleanupNotificationPreferenceTest(eventId, coorganizerEventId, uid, email, password);
        }
    }

    /**
     * creates a temporary user with the supplied opt-in preferences
     * @param email
     * email of the temporary user
     * @param password
     * password of the temporary user
     * @param username
     * username of the temporary user
     * @param fullName
     * full name of the temporary user
     * @param optInCoorganizer
     * coorganizer invite preference for the temporary user
     * @param optInPrivate
     * private invite preference for the temporary user
     * @param optInWinning
     * winning notification preference for the temporary user
     * @param optInOther
     * general notification preference for the temporary user
     * @return
     * uid of the created temporary user
     */
    private String createTemporaryUserWithPreferences(
            String email,
            String password,
            String username,
            String fullName,
            boolean optInCoorganizer,
            boolean optInPrivate,
            boolean optInWinning,
            boolean optInOther
    ) throws Exception {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser created = Tasks.await(
                auth.createUserWithEmailAndPassword(email, password),
                15,
                TimeUnit.SECONDS
        ).getUser();

        Map<String, Object> payload = new HashMap<>();
        payload.put("fullName", fullName);
        payload.put("email", email);
        payload.put("username", username);
        payload.put("usernameKey", username.toLowerCase());
        payload.put("phoneNumber", "780 555 1122");
        payload.put("accountType", "user");
        payload.put("createdAt", Timestamp.now());
        payload.put("pendingEmail", "");
        payload.put("deleted", false);
        payload.put("optInCoorganizerInvites", optInCoorganizer);
        payload.put("optInPrivateInvites", optInPrivate);
        payload.put("optInWinningNotifications", optInWinning);
        payload.put("optInOtherNotifications", optInOther);

        Tasks.await(
                FirebaseFirestore.getInstance().collection("users").document(created.getUid()).set(payload),
                15,
                TimeUnit.SECONDS
        );
        return created.getUid();
    }

    /**
     * creates an event document that can hold invitation tracking records
     * @param eventId
     * id of the event to create
     * @param hostUid
     * uid of the host creating the event
     * @param title
     * title of the event
     */
    private void createNotificationTestEvent(String eventId, String hostUid, String title) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("description", "Notification opt out test event.");
        payload.put("location", "Edmonton");
        payload.put("posterUrl", "");
        payload.put("maxEntrants", 10);
        payload.put("maxParticipants", 5);
        payload.put("totalEntrants", 0);
        payload.put("registrationDeadline", new Timestamp(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2))));
        payload.put("eventDate", new Timestamp(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3))));
        payload.put("requiresGeolocation", false);
        payload.put("hostUid", hostUid);
        payload.put("hostDisplayName", "Notification Test Host");
        payload.put("waitlistOpen", true);
        payload.put("deleted", false);
        payload.put("createdAt", Timestamp.now());
        payload.put("winningMessage", "Welcome");
        payload.put("isPublic", false);

        Tasks.await(
                FirebaseFirestore.getInstance().collection("events").document(eventId).set(payload),
                15,
                TimeUnit.SECONDS
        );
    }

    /**
     * removes all data created by the notification preference integration test
     * @param eventId
     * private event id used in the test
     * @param coorganizerEventId
     * coorganizer event id used in the test
     * @param uid
     * uid of the temporary user
     * @param email
     * email of the temporary user
     * @param password
     * password of the temporary user
     */
    private void cleanupNotificationPreferenceTest(
            String eventId,
            String coorganizerEventId,
            String uid,
            String email,
            String password
    ) throws Exception {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        try {
            signInUser(email, password);
            for (DocumentSnapshot doc : Tasks.await(
                    firestore.collection("users").document(uid).collection("notifications").get(),
                    15,
                    TimeUnit.SECONDS
            ).getDocuments()) {
                Tasks.await(doc.getReference().delete(), 15, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {
        }

        try {
            TestAuthHelper.ensureSharedTestUser();
            Tasks.await(firestore.collection("events").document(eventId).collection("invitations").document(uid).delete(), 15, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }

        try {
            Tasks.await(firestore.collection("events").document(coorganizerEventId).collection("coorganizerinvites").document(uid).delete(), 15, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }

        try {
            for (String title : new String[] {"Opt Out Win", "Opt Out General"}) {
                for (DocumentSnapshot doc : Tasks.await(
                        firestore.collection("notifications").whereEqualTo("title", title).get(),
                        15,
                        TimeUnit.SECONDS
                ).getDocuments()) {
                    Tasks.await(doc.getReference().delete(), 15, TimeUnit.SECONDS);
                }
            }
        } catch (Exception ignored) {
        }

        try {
            Tasks.await(firestore.collection("events").document(eventId).delete(), 15, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }

        try {
            Tasks.await(firestore.collection("events").document(coorganizerEventId).delete(), 15, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }

        try {
            signInUser(email, password);
            try {
                Tasks.await(firestore.collection("users").document(uid).delete(), 15, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                try {
                    Tasks.await(currentUser.delete(), 15, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        } finally {
            FirebaseAuth.getInstance().signOut();
        }
    }

    /**
     * signs in with the supplied credentials
     * @param email
     * email to sign in with
     * @param password
     * password to sign in with
     */
    private void signInUser(String email, String password) throws Exception {
        FirebaseAuth.getInstance().signOut();
        Tasks.await(
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password),
                15,
                TimeUnit.SECONDS
        );
    }
}
