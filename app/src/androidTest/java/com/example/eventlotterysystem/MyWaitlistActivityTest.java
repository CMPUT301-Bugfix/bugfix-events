package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Tests the functionality of the MyWaitlistActivity
 * this activity shows the events that the current user is signed up to
 * it also allows navigation from a waitlist entry into the event details screen
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MyWaitlistActivityTest {

    /**
     * Test to see if all signed-up events are loaded from database and is displayed to the user
     * @throws Exception if authentication or database setup fails
     */
    @Test
    public void viewEventsTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String title = "UofA Waitlist Event " + timestamp;
        String eventId = createWaitlistTestEvent(title, EventRepository.WAITLIST_STATUS_IN);

        try (ActivityScenario<MyWaitlistActivity> ignored = ActivityScenario.launch(MyWaitlistActivity.class)) {
            SystemClock.sleep(4000);

            onView(withText(title)).check(matches(isDisplayed()));
            onView(withId(R.id.myWaitlistEmptyState))
                    .check(matches(withEffectiveVisibility(GONE)));
        }

        deleteWaitlistTestEvent(eventId);
    }

    /**
     * Test if navigation to ViewEventActivity is correct event and is in the view version
     * @throws Exception if authentication or database setup fails
     */
    @Test
    public void NavigateToEvent() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String title = "Edmonton Waitlist Event " + timestamp;
        String eventId = createWaitlistTestEvent(title, EventRepository.WAITLIST_STATUS_IN);

        try (ActivityScenario<MyWaitlistActivity> ignored = ActivityScenario.launch(MyWaitlistActivity.class)) {
            SystemClock.sleep(4000);

            onData(anything())
                    .inAdapterView(withId(R.id.myWaitlistListView))
                    .atPosition(0)
                    .perform(click());

            SystemClock.sleep(4000);

            onView(withId(R.id.viewEventTitle)).check(matches(withText(title)));
            onView(withId(R.id.viewEventEditButton))
                    .check(matches(withEffectiveVisibility(GONE)));
        }

        deleteWaitlistTestEvent(eventId);
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

    /**
     * creates an event and matching waitlist records for the signed-in test user
     * @param title
     * title of the event that should appear in the waitlist
     * @param status
     * waitlist status that should be stored for the user
     * @return
     * the document id of the created event
     */
    private String createWaitlistTestEvent(String title, String status) throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String eventId = firestore.collection("events").document().getId();

        Date registrationDeadline = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        Date eventDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2));

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("title", title);
        eventPayload.put("description", "Waitlist test event for University of Alberta students in Edmonton.");
        eventPayload.put("location", "Education Centre Edmonton");
        eventPayload.put("posterUrl", "");
        eventPayload.put("maxEntrants", 10);
        eventPayload.put("maxParticipants", 5);
        eventPayload.put("totalEntrants", 1);
        eventPayload.put("registrationDeadline", registrationDeadline);
        eventPayload.put("eventDate", eventDate);
        eventPayload.put("requiresGeolocation", false);
        eventPayload.put("hostUid", "waitlist-test-host");
        eventPayload.put("hostDisplayName", "UofA Organizer");
        eventPayload.put("waitlistOpen", true);
        eventPayload.put("deleted", false);
        eventPayload.put("createdAt", Timestamp.now());
        eventPayload.put("winningMessage", "Welcome to the Edmonton waitlist.");

        Map<String, Object> waitlistPayload = new HashMap<>();
        waitlistPayload.put("eventId", eventId);
        waitlistPayload.put("uid", currentUser.getUid());
        waitlistPayload.put("status", status);
        waitlistPayload.put("joinedAt", Timestamp.now());

        Tasks.await(firestore.collection("events").document(eventId).set(eventPayload), 15, TimeUnit.SECONDS);
        Tasks.await(firestore.collection("events").document(eventId).collection("waitlist").document(currentUser.getUid()).set(waitlistPayload), 15, TimeUnit.SECONDS);
        Tasks.await(firestore.collection("users").document(currentUser.getUid()).collection("waitlists").document(eventId).set(waitlistPayload), 15, TimeUnit.SECONDS);

        return eventId;
    }

    /**
     * removes the waitlist records and event document created for a test run
     * @param eventId
     * document id of the event to remove
     */
    private void deleteWaitlistTestEvent(String eventId) throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Tasks.await(firestore.collection("users").document(currentUser.getUid()).collection("waitlists").document(eventId).delete(), 15, TimeUnit.SECONDS);
        Tasks.await(firestore.collection("events").document(eventId).collection("waitlist").document(currentUser.getUid()).delete(), 15, TimeUnit.SECONDS);
        Tasks.await(firestore.collection("events").document(eventId).delete(), 15, TimeUnit.SECONDS);
    }
}
