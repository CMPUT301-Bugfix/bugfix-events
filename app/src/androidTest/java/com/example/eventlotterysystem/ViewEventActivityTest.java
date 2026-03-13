package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Tests the functionality of the ViewEventActivity
 * this is the activity that shows event Information to Entrants
 * It has the controllers that let the Entrant signup/leave the waitlist
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ViewEventActivityTest {

    /**
     * Test if the display is correct when given an event
     */
    @Test
    public void viewEventScreenDisplayTest() throws Exception {
        signInTestUser();
        String title = "UofA View Event " + System.currentTimeMillis();
        String eventId = createViewEventTestEvent(title, "Open house for University of Alberta students in Edmonton.", "SUB Edmonton", true, 5, 6, 0, 10, "view-event-host", "");

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ViewEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);

        try (ActivityScenario<ViewEventActivity> ignored = ActivityScenario.launch(intent)) {
            SystemClock.sleep(4000);

            onView(withId(R.id.viewEventTitle)).check(matches(withText(title)));
            onView(withId(R.id.viewEventLocation)).check(matches(withText("Location: SUB Edmonton")));
            onView(withId(R.id.viewEventDescription))
                    .check(matches(withText("Open house for University of Alberta students in Edmonton.")));
        }

        deleteViewEventTestData(eventId);
    }

    /**
     * Test if the event screen shows the total number of entrants for the waitlist
     */
    @Test
    public void totalEntrantsShownTest() throws Exception {
        signInTestUser();
        String eventId = createViewEventTestEvent("UofA Waitlist Count Event " + System.currentTimeMillis(), "Waitlist count test in Edmonton.", "CCIS Edmonton", true, 5, 6, 7, 10, "waitlist-count-host", "");

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ViewEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);

        try (ActivityScenario<ViewEventActivity> ignored = ActivityScenario.launch(intent)) {
            SystemClock.sleep(4000);

            onView(withId(R.id.viewEventWaitlistCount)).check(matches(withText("Total Entrants: 7")));
        }

        deleteViewEventTestData(eventId);
    }

    /**
     * Test if clicking on the sign up button signs up a user to the waitlist
     */
    @Test
    public void signUpButtonTest() throws Exception {
        signInTestUser();
        String title = "UofA Sign Up Event " + System.currentTimeMillis();
        String eventId = createViewEventTestEvent(title, "Join test for Edmonton students.", "CAB Edmonton", true, 5, 6, 0, 10, "join-test-host", "");

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ViewEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);

        try (ActivityScenario<ViewEventActivity> ignored = ActivityScenario.launch(intent)) {
            SystemClock.sleep(4000);

            onView(withId(R.id.viewEventJoinWaitlistButton)).perform(click());
            onView(withText(R.string.join)).perform(click());
            SystemClock.sleep(4000);

            onView(withId(R.id.viewEventWaitlistJoinedLabel)).check(matches(isDisplayed()));
            onView(withId(R.id.viewEventLeaveWaitlistButton)).check(matches(isDisplayed()));
        }

        deleteViewEventTestData(eventId);
    }

    /**
     * Test if clicking on the sign up button updates the waitlist on the database
     */
    @Test
    public void signUpToDatabaseTest() throws Exception {
        signInTestUser();
        String eventId = createViewEventTestEvent("UofA Database Join " + System.currentTimeMillis(), "Database join test in Edmonton.", "Tory Building Edmonton", true, 5, 6, 0, 10, "join-db-host", "");

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ViewEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);

        try (ActivityScenario<ViewEventActivity> ignored = ActivityScenario.launch(intent)) {
            SystemClock.sleep(4000);

            onView(withId(R.id.viewEventJoinWaitlistButton)).perform(click());
            onView(withText(R.string.join)).perform(click());
            SystemClock.sleep(4000);
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentSnapshot eventWaitlist = Tasks.await(firestore.collection("events").document(eventId).collection("waitlist").document(currentUser.getUid()).get(), 15, TimeUnit.SECONDS);
        DocumentSnapshot userWaitlist = Tasks.await(firestore.collection("users").document(currentUser.getUid()).collection("waitlists").document(eventId).get(), 15, TimeUnit.SECONDS);
        assertTrue(eventWaitlist.exists());
        assertTrue(userWaitlist.exists());

        deleteViewEventTestData(eventId);
    }

    /**
     * Test if clicking on the leave button removes user from waitlist in Event class
     */
    @Test
    public void leaveWaitlistButtonTest() throws Exception {
        signInTestUser();
        String eventId = createViewEventTestEvent("UofA Leave Event " + System.currentTimeMillis(), "Leave waitlist test in Edmonton.", "Education Centre Edmonton", true, 5, 6, 1, 10, "leave-test-host", EventRepository.WAITLIST_STATUS_IN);

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ViewEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);

        try (ActivityScenario<ViewEventActivity> ignored = ActivityScenario.launch(intent)) {
            SystemClock.sleep(4000);

            onView(withId(R.id.viewEventLeaveWaitlistButton)).perform(click());
            SystemClock.sleep(4000);

            onView(withId(R.id.viewEventJoinWaitlistButton)).check(matches(isDisplayed()));
            onView(withId(R.id.viewEventLeaveWaitlistButton))
                    .check(matches(withEffectiveVisibility(GONE)));
        }

        deleteViewEventTestData(eventId);
    }

    /**
     * Test if clicking on the leave button updates the waitlist on the database
     */
    @Test
    public void leaveWaitlistToDatabaseTest() throws Exception {
        signInTestUser();
        String eventId = createViewEventTestEvent("Edmonton Leave Database " + System.currentTimeMillis(), "Leave database test for UofA event.", "CCIS Edmonton", true, 5, 6, 1, 10, "leave-db-host", EventRepository.WAITLIST_STATUS_IN);

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ViewEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);

        try (ActivityScenario<ViewEventActivity> ignored = ActivityScenario.launch(intent)) {
            SystemClock.sleep(4000);

            onView(withId(R.id.viewEventLeaveWaitlistButton)).perform(click());
            SystemClock.sleep(4000);
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentSnapshot eventWaitlist = Tasks.await(firestore.collection("events").document(eventId).collection("waitlist").document(currentUser.getUid()).get(), 15, TimeUnit.SECONDS);
        DocumentSnapshot userWaitlist = Tasks.await(firestore.collection("users").document(currentUser.getUid()).collection("waitlists").document(eventId).get(), 15, TimeUnit.SECONDS);
        assertFalse(eventWaitlist.exists());
        assertFalse(userWaitlist.exists());

        deleteViewEventTestData(eventId);
    }

    /**
     * Ensures user cannot sign up to an event that has passed its deadline
     */
    @Test
    public void signUpAfterDeadlineTest() throws Exception {
        signInTestUser();
        String eventId = createViewEventTestEvent("UofA Deadline Event " + System.currentTimeMillis(), "Past deadline event in Edmonton.", "ECHA Edmonton", true, -2, 2, 0, 10, "deadline-host", "");

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ViewEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);

        try (ActivityScenario<ViewEventActivity> ignored = ActivityScenario.launch(intent)) {
            SystemClock.sleep(4000);

            onView(withId(R.id.viewEventJoinWaitlistButton)).check(matches(isDisplayed()));
            onView(withId(R.id.viewEventJoinWaitlistButton)).check(matches(not(isEnabled())));
        }

        deleteViewEventTestData(eventId);
    }

    /**
     * Ensures user cannot sign up to an event that is at max sign-ups
     */
    @Test
    public void signUpWhenFullTest() throws Exception {
        signInTestUser();
        String eventId = createViewEventTestEvent("Edmonton Full Event " + System.currentTimeMillis(), "Closed full event for UofA students.", "Lister Edmonton", false, 5, 6, 10, 10, "full-host", "");

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ViewEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);

        try (ActivityScenario<ViewEventActivity> ignored = ActivityScenario.launch(intent)) {
            SystemClock.sleep(4000);

            onView(withId(R.id.viewEventJoinWaitlistButton)).check(matches(isDisplayed()));
            onView(withId(R.id.viewEventJoinWaitlistButton)).check(matches(not(isEnabled())));
        }

        deleteViewEventTestData(eventId);
    }

    /**
     * tests to see that when opened through the organiser view allow navigation to edit the Event
     */
    @Test
    public void openedByAuthorTest() throws Exception {
        signInTestUser();
        String title = "UofA Organizer Event " + System.currentTimeMillis();
        String eventId = createViewEventTestEvent(title, "Organizer view test in Edmonton.", "SUB Edmonton", true, 5, 6, 0, 10, "author-host", "");

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ViewEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        intent.putExtra("CAN_EDIT_EVENT", true);

        try (ActivityScenario<ViewEventActivity> ignored = ActivityScenario.launch(intent)) {
            SystemClock.sleep(4000);

            onView(withId(R.id.viewEventEditButton)).check(matches(isDisplayed()));
            onView(withId(R.id.viewEventScreenTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.viewEventEditButton)).perform(click());
            SystemClock.sleep(3000);

            onView(withId(R.id.createEventTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.createEventSubmitButton)).check(matches(withText(R.string.edit_event_submit)));
        }

        deleteViewEventTestData(eventId);
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
     * creates an event document and optional waitlist records for the view event tests
     * @param title
     * title of the event that should be created
     * @param description
     * description to store on the event
     * @param location
     * location to store on the event
     * @param waitlistOpen
     * whether the event should accept new waitlist sign-ups
     * @param deadlineOffsetDays
     * number of days from now to set the registration deadline
     * @param eventOffsetDays
     * number of days from now to set the event date
     * @param totalEntrants
     * total entrant count to store on the event
     * @param maxEntrants
     * maximum entrant count to store on the event
     * @param hostUid
     * uid to store as the organiser of the event
     * @param waitlistStatus
     * optional waitlist status to store for the signed-in user
     * @return
     * the document id of the created event
     */
    private String createViewEventTestEvent(String title, String description, String location, boolean waitlistOpen, int deadlineOffsetDays, int eventOffsetDays, int totalEntrants, int maxEntrants, String hostUid, String waitlistStatus) throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String eventId = firestore.collection("events").document().getId();

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("title", title);
        eventPayload.put("description", description);
        eventPayload.put("location", location);
        eventPayload.put("posterUrl", "");
        eventPayload.put("maxEntrants", maxEntrants);
        eventPayload.put("maxParticipants", 5);
        eventPayload.put("totalEntrants", totalEntrants);
        eventPayload.put("registrationDeadline", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(deadlineOffsetDays)));
        eventPayload.put("eventDate", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(eventOffsetDays)));
        eventPayload.put("requiresGeolocation", false);
        eventPayload.put("hostUid", hostUid);
        eventPayload.put("hostDisplayName", "UofA Organizer");
        eventPayload.put("waitlistOpen", waitlistOpen);
        eventPayload.put("deleted", false);
        eventPayload.put("createdAt", Timestamp.now());
        eventPayload.put("winningMessage", "Welcome to the Edmonton event.");

        Tasks.await(firestore.collection("events").document(eventId).set(eventPayload), 15, TimeUnit.SECONDS);

        if (!waitlistStatus.isEmpty()) {
            Map<String, Object> waitlistPayload = new HashMap<>();
            waitlistPayload.put("eventId", eventId);
            waitlistPayload.put("uid", currentUser.getUid());
            waitlistPayload.put("status", waitlistStatus);
            waitlistPayload.put("joinedAt", Timestamp.now());

            Tasks.await(firestore.collection("events").document(eventId).collection("waitlist").document(currentUser.getUid()).set(waitlistPayload), 15, TimeUnit.SECONDS);
            Tasks.await(firestore.collection("users").document(currentUser.getUid()).collection("waitlists").document(eventId).set(waitlistPayload), 15, TimeUnit.SECONDS);
        }

        return eventId;
    }

    /**
     * removes all waitlist records and the event document created for a view event test
     * @param eventId
     * document id of the event to remove
     */
    private void deleteViewEventTestData(String eventId) throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Tasks.await(firestore.collection("users").document(currentUser.getUid()).collection("waitlists").document(eventId).delete(), 15, TimeUnit.SECONDS);
        Tasks.await(firestore.collection("events").document(eventId).collection("waitlist").document(currentUser.getUid()).delete(), 15, TimeUnit.SECONDS);
        Tasks.await(firestore.collection("events").document(eventId).delete(), 15, TimeUnit.SECONDS);
    }
}
