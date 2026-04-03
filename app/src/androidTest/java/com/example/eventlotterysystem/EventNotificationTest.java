package com.example.eventlotterysystem;

import static org.junit.Assert.*;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventNotificationTest {
    /**
     * Test if winner receives notification
     * @throws Exception if user did not receive winning notification
     */
    @Test
    public void WinningLotteryNotificationTest() throws Exception {
        signInTestUser();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        EventRepository repository = new EventRepository();
        String eventId = "notif-win-test";

        String entrantUid = currentUser.getUid();
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("title", "UofA Win Notification Event");
        eventPayload.put("description", "Testing winning notification in Edmonton.");
        eventPayload.put("location", "CAB Edmonton");
        eventPayload.put("posterUrl", "");
        eventPayload.put("maxEntrants", 10);
        eventPayload.put("maxParticipants", 1);
        eventPayload.put("totalEntrants", 1);
        eventPayload.put("registrationDeadline", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(5)));
        eventPayload.put("eventDate", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(6)));
        eventPayload.put("requiresGeolocation", false);
        eventPayload.put("hostUid", currentUser.getUid()); // Host is also the only entrant
        eventPayload.put("hostDisplayName", "UofA Organizer");
        eventPayload.put("waitlistOpen", true);
        eventPayload.put("deleted", false);
        eventPayload.put("createdAt", Timestamp.now());
        eventPayload.put("winningMessage", "Congrats! You won the Edmonton lottery!");

        Tasks.await(firestore.collection("events").document(eventId).set(eventPayload), 15, TimeUnit.SECONDS);

        Map<String, Object> waitlistPayload = new HashMap<>();
        waitlistPayload.put("eventId", eventId);
        waitlistPayload.put("uid", entrantUid);
        waitlistPayload.put("status", EventRepository.WAITLIST_STATUS_IN);
        waitlistPayload.put("joinedAt", Timestamp.now());

        Tasks.await(firestore.collection("events").document(eventId)
                .collection("waitlist").document(entrantUid).set(waitlistPayload), 15, TimeUnit.SECONDS);
        Tasks.await(firestore.collection("users").document(entrantUid)
                .collection("waitlists").document(eventId).set(waitlistPayload), 15, TimeUnit.SECONDS);

        Tasks.await(repository.performLotteryDraw(eventId, "Congrats! You won the Edmonton lottery!"), 15, TimeUnit.SECONDS);
        SystemClock.sleep(3000);

        DocumentSnapshot waitlistDoc = Tasks.await(firestore.collection("events").document(eventId)
                .collection("waitlist").document(entrantUid).get(), 15, TimeUnit.SECONDS);
        assertEquals(EventRepository.WAITLIST_STATUS_CHOSEN, waitlistDoc.getString("status"));


        List<DocumentSnapshot> notifications = Tasks.await(firestore.collection("users").document(entrantUid)
                .collection("notifications")
                .whereEqualTo("type", "WIN")
                .whereEqualTo("eventId", eventId)
                .get(), 15, TimeUnit.SECONDS).getDocuments();

        assertFalse("Winner should have received a WIN notification", notifications.isEmpty());
        assertEquals("Congrats! You won the Edmonton lottery!", notifications.get(0).getString("message"));

        for (DocumentSnapshot doc : notifications) {
            Tasks.await(firestore.collection("users").document(entrantUid)
                    .collection("notifications").document(doc.getId()).delete(), 15, TimeUnit.SECONDS);
        }
        Tasks.await(firestore.collection("events").document(eventId)
                .collection("waitlist").document(entrantUid).delete(), 15, TimeUnit.SECONDS);
        Tasks.await(firestore.collection("users").document(entrantUid)
                .collection("waitlists").document(eventId).delete(), 15, TimeUnit.SECONDS);
        Tasks.await(firestore.collection("events").document(eventId).delete(), 15, TimeUnit.SECONDS);
    }

    /**
     * Test loser if receives notification
     * @throws Exception if user did not receive losing notification
     */
    @Test
    public void losingLotteryNotificationFiredTest() throws Exception {
        signInTestUser();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        String eventId = "notif-lose-test";
        String entrantUid = currentUser.getUid();

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("title", "UofA Lose Notification Event");
        eventPayload.put("description", "Testing losing notification.");
        eventPayload.put("hostUid", currentUser.getUid());
        eventPayload.put("waitlistOpen", true);
        eventPayload.put("deleted", false);
        eventPayload.put("createdAt", Timestamp.now());

        Tasks.await(firestore.collection("events").document(eventId).set(eventPayload), 15, TimeUnit.SECONDS);

        NotificationRepository notifRepo = new NotificationRepository();
        List<String> losersList = new ArrayList<>();
        losersList.add(entrantUid);

        Tasks.await(notifRepo.sendToSpecificUsers(
                eventId,
                "UofA Lose Notification Event",
                "Unfortunately, you were not selected for the event.",
                "GENERAL",
                losersList
        ), 15, TimeUnit.SECONDS);
        SystemClock.sleep(3000);

        List<DocumentSnapshot> loserNotifications = Tasks.await(firestore.collection("users").document(entrantUid)
                .collection("notifications")
                .whereEqualTo("type", "GENERAL")
                .whereEqualTo("eventId", eventId)
                .get(), 15, TimeUnit.SECONDS).getDocuments();
        assertFalse("Entrant should have received a GENERAL (lose) notification", loserNotifications.isEmpty());
        assertTrue("Losing notification should mention not being selected",
                loserNotifications.get(0).getString("message").contains("not selected"));

        for (DocumentSnapshot doc : loserNotifications) {
            Tasks.await(firestore.collection("users").document(entrantUid)
                    .collection("notifications").document(doc.getId()).delete(), 15, TimeUnit.SECONDS);
        }


        Tasks.await(firestore.collection("events").document(eventId).delete(), 15, TimeUnit.SECONDS);
    }


    /**
     * signs in the shared test account and ensures that remember-me is disabled
     */
    private void signInTestUser() throws Exception {
        FirebaseAuth.getInstance().signOut();
        Context context = ApplicationProvider.getApplicationContext();
        AuthSessionPreference.setRemember(context, false);
        Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword("test@gmail.com", "test123"), 15, TimeUnit.SECONDS);
    }

}
