package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Tests the functionality of the EntrantActivity
 * it allows organiser to navigate to view lists of entrants
 * it also can notify entrants and manages acceptance of entrants
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntrantsActivityTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * Test to see if performing a draw will move the correct number of entrants to chosen and update the database
     */
    @Test
    public void preformDrawTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String eventId = createManagedEntrantsTestEvent("Entrants draw " + timestamp, 1, 0);
        String email = "draw" + timestamp + "@gmail.com";
        String password = "test123";
        String username = "draw" + timestamp;
        String uid = createTemporaryEntrant(email, password, username, "Draw Test");
        joinWaitlistAsCurrentUser(eventId);
        signInTestUser();

        try {
            try (ActivityScenario<EntrantsActivity> ignored = launchEntrantsActivity(eventId)) {
                onView(withId(R.id.entrantsPerformDrawButton)).perform(click());
                SystemClock.sleep(1000);
                onView(withText("Draw")).perform(click());
                SystemClock.sleep(5000);
            }

            DocumentSnapshot doc = Tasks.await(
                    FirebaseFirestore.getInstance()
                            .collection("events")
                            .document(eventId)
                            .collection("waitlist")
                            .document(uid)
                            .get(),
                    15,
                    TimeUnit.SECONDS
            );
            assertEquals(EventRepository.WAITLIST_STATUS_CHOSEN, doc.getString("status"));
        } finally {
            deleteEntrantsTestData(eventId);
            deleteTemporaryEntrant(uid, email, password, eventId);
        }
    }

    /**
     * Test to see if Clean will remove expired chosen entrants and trigger a redraw
     */
    @Test
    public void cleanExpiredTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String eventId = createManagedEntrantsTestEvent("Entrants clean " + timestamp, 1, 0);
        String firstEmail = "expired" + timestamp + "@gmail.com";
        String secondEmail = "replacement" + timestamp + "@gmail.com";
        String password = "test123";
        String firstUid = createTemporaryEntrant(firstEmail, password, "expired" + timestamp, "Expired Test");
        joinWaitlistAsCurrentUser(eventId);
        String secondUid = createTemporaryEntrant(secondEmail, password, "replacement" + timestamp, "Replacement Test");
        joinWaitlistAsCurrentUser(eventId);
        signInTestUser();

        Tasks.await(new EventRepository().performLotteryDraw(eventId, "Winner"), 15, TimeUnit.SECONDS);
        DocumentSnapshot firstDocAfterDraw = Tasks.await(
                FirebaseFirestore.getInstance().collection("events").document(eventId).collection("waitlist").document(firstUid).get(),
                15,
                TimeUnit.SECONDS
        );
        String expiredUid = EventRepository.WAITLIST_STATUS_CHOSEN.equals(firstDocAfterDraw.getString("status"))
                ? firstUid
                : secondUid;
        String replacementUid = expiredUid.equals(firstUid) ? secondUid : firstUid;
        Date expiredChosenAt = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(4));
        Tasks.await(
                FirebaseFirestore.getInstance()
                        .collection("events")
                        .document(eventId)
                        .collection("waitlist")
                        .document(expiredUid)
                        .update("chosenAt", expiredChosenAt),
                15,
                TimeUnit.SECONDS
        );

        try {
            try (ActivityScenario<EntrantsActivity> ignored = launchEntrantsActivity(eventId)) {
                onView(withId(R.id.entrantsProcessExpiredButton)).perform(click());
                SystemClock.sleep(5000);
            }

            DocumentSnapshot expiredDoc = Tasks.await(
                    FirebaseFirestore.getInstance()
                            .collection("events")
                            .document(eventId)
                            .collection("waitlist")
                            .document(expiredUid)
                            .get(),
                    15,
                    TimeUnit.SECONDS
            );
            DocumentSnapshot replacementDoc = Tasks.await(
                    FirebaseFirestore.getInstance()
                            .collection("events")
                            .document(eventId)
                            .collection("waitlist")
                            .document(replacementUid)
                            .get(),
                    15,
                    TimeUnit.SECONDS
            );
            assertEquals(EventRepository.WAITLIST_STATUS_DECLINED, expiredDoc.getString("status"));
            assertEquals(EventRepository.WAITLIST_STATUS_CHOSEN, replacementDoc.getString("status"));
        } finally {
            deleteEntrantsTestData(eventId);
            deleteTemporaryEntrant(firstUid, firstEmail, password, eventId);
            deleteTemporaryEntrant(secondUid, secondEmail, password, eventId);
        }
    }

    /**
     * Test to see if the navigation to view entrants is correct and displays the correct entrants
     */
    @Test
    public void navigateToAllEntrantsTest() throws Exception {
        signInTestUser();
        String eventId = createManagedEntrantsTestEvent("Entrants nav all " + System.currentTimeMillis(), 1, 0);

        try {
            launchEntrantsActivity(eventId);
            onView(withId(R.id.entrantsAllEntrantsButton)).perform(click());
            SystemClock.sleep(1000);
            intended(hasComponent(AllEntrantsActivity.class.getName()));
        } finally {
            deleteEvent(eventId);
        }
    }

    /**
     * Test to see if the navigation to view entrants of a specific status (chosen) is correct and displays the correct entrants
     */
    @Test
    public void navigateToChosenEntrantsTest() throws Exception {
        signInTestUser();
        String eventId = createManagedEntrantsTestEvent("Entrants nav chosen " + System.currentTimeMillis(), 1, 0);

        try {
            launchEntrantsActivity(eventId);
            onView(withId(R.id.entrantsChosenButton)).perform(click());
            SystemClock.sleep(1000);
            intended(allOf(
                    hasComponent(AllEntrantsActivity.class.getName()),
                    hasExtra(AllEntrantsActivity.STATUS_FILTER, EventRepository.WAITLIST_STATUS_CHOSEN)
            ));
        } finally {
            deleteEvent(eventId);
        }
    }

    /**
     * signs in the shared test account and ensures that remember-me is disabled
     */
    private void signInTestUser() throws Exception {
        TestAuthHelper.ensureSharedTestUser();
    }

    /**
     * creates a managed event that the current user can administer
     */
    private String createManagedEntrantsTestEvent(String title, int maxParticipants, int totalEntrants) throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String eventId = firestore.collection("events").document().getId();

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("title", title);
        eventPayload.put("description", "Entrants test event.");
        eventPayload.put("location", "CAB Edmonton");
        eventPayload.put("posterUrl", "");
        eventPayload.put("maxEntrants", Math.max(totalEntrants, maxParticipants));
        eventPayload.put("maxParticipants", maxParticipants);
        eventPayload.put("totalEntrants", totalEntrants);
        eventPayload.put("registrationDeadline", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(4)));
        eventPayload.put("eventDate", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));
        eventPayload.put("requiresGeolocation", false);
        eventPayload.put("hostUid", currentUser.getUid());
        eventPayload.put("hostDisplayName", "Entrants Test");
        eventPayload.put("waitlistOpen", true);
        eventPayload.put("deleted", false);
        eventPayload.put("createdAt", Timestamp.now());
        eventPayload.put("winningMessage", "Welcome");
        eventPayload.put("coOrganizerUids", new ArrayList<>());
        eventPayload.put("keywords", new ArrayList<>());
        eventPayload.put("isPublic", true);

        Tasks.await(firestore.collection("events").document(eventId).set(eventPayload), 15, TimeUnit.SECONDS);
        return eventId;
    }

    /**
     * creates a temporary entrant account with a matching user profile document
     */
    private String createTemporaryEntrant(String email, String password, String username, String fullName) throws Exception {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        Tasks.await(auth.createUserWithEmailAndPassword(email, password), 15, TimeUnit.SECONDS);
        FirebaseUser currentUser = auth.getCurrentUser();

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("username", username);
        userPayload.put("fullName", fullName);
        userPayload.put("email", email);
        userPayload.put("phoneNumber", "555 555 5555");
        userPayload.put("accountType", "entrant");
        userPayload.put("deleted", false);
        userPayload.put("createdAt", Timestamp.now());
        Tasks.await(
                FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).set(userPayload),
                15,
                TimeUnit.SECONDS
        );
        return currentUser.getUid();
    }

    /**
     * joins the current signed-in user to the event waitlist
     */
    private void joinWaitlistAsCurrentUser(String eventId) throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Tasks.await(new EventRepository().joinWaitlist(eventId, currentUser), 15, TimeUnit.SECONDS);
    }

    /**
     * launches entrants activity for the supplied event
     */
    private ActivityScenario<EntrantsActivity> launchEntrantsActivity(String eventId) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantsActivity.class);
        intent.putExtra(EntrantsActivity.EVENT_ID, eventId);
        return ActivityScenario.launch(intent);
    }

    /**
     * removes the event and its waitlist entries
     */
    private void deleteEntrantsTestData(String eventId) throws Exception {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        for (DocumentSnapshot doc : Tasks.await(
                firestore.collection("events").document(eventId).collection("waitlist").get(),
                15,
                TimeUnit.SECONDS
        ).getDocuments()) {
            Tasks.await(doc.getReference().delete(), 15, TimeUnit.SECONDS);
        }
        deleteEvent(eventId);
    }

    /**
     * removes a temporary entrant account, its profile, and its waitlist membership
     */
    private void deleteTemporaryEntrant(String uid, String email, String password, String eventId) throws Exception {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseAuth.getInstance().signOut();
        Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password), 15, TimeUnit.SECONDS);
        try {
            Tasks.await(firestore.collection("users").document(uid).collection("waitlists").document(eventId).delete(), 15, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
        try {
            Tasks.await(firestore.collection("users").document(uid).delete(), 15, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Tasks.await(currentUser.delete(), 15, TimeUnit.SECONDS);
        }
        signInTestUser();
    }

    /**
     * deletes an event if it still exists
     */
    private void deleteEvent(String eventId) throws Exception {
        Tasks.await(FirebaseFirestore.getInstance().collection("events").document(eventId).delete(), 15, TimeUnit.SECONDS);
    }
}
