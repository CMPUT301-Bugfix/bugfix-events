package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
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
     * Test that clicking the Show Map button on a geolocation-required event
     * with entrant locations displays the map dialog
     */
    @Test
    public void showMapDisplaysEntrantLocationsTest() throws Exception {
        signInTestUser();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        String eventId = firestore.collection("events").document().getId();
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("title", "UofA Map Test " + System.currentTimeMillis());
        eventPayload.put("description", "Map display test in Edmonton.");
        eventPayload.put("location", "SUB Edmonton");
        eventPayload.put("posterUrl", "");
        eventPayload.put("maxEntrants", 10);
        eventPayload.put("maxParticipants", 5);
        eventPayload.put("totalEntrants", 1);
        eventPayload.put("registrationDeadline", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(5)));
        eventPayload.put("eventDate", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(6)));
        eventPayload.put("requiresGeolocation", true);
        eventPayload.put("hostUid", currentUser.getUid());
        eventPayload.put("hostDisplayName", "UofA Organizer");
        eventPayload.put("waitlistOpen", true);
        eventPayload.put("deleted", false);
        eventPayload.put("createdAt", Timestamp.now());
        eventPayload.put("winningMessage", "Welcome to the Edmonton event.");

        Tasks.await(firestore.collection("events").document(eventId).set(eventPayload), 15, TimeUnit.SECONDS);

        Map<String, Object> waitlistPayload = new HashMap<>();
        waitlistPayload.put("eventId", eventId);
        waitlistPayload.put("uid", "fake-entrant-uid");
        waitlistPayload.put("status", EventRepository.WAITLIST_STATUS_IN);
        waitlistPayload.put("joinedAt", Timestamp.now());
        waitlistPayload.put("location", new com.google.firebase.firestore.GeoPoint(53.5461, -113.4938));

        Tasks.await(firestore.collection("events").document(eventId)
                .collection("waitlist").document("fake-entrant-uid")
                .set(waitlistPayload), 15, TimeUnit.SECONDS);

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ViewEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        intent.putExtra("CAN_EDIT_EVENT", true);

        try (ActivityScenario<ViewEventActivity> ignored = ActivityScenario.launch(intent)) {
            SystemClock.sleep(4000);

            onView(withId(R.id.viewEventShowMapButton)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.viewEventShowMapButton)).perform(scrollTo(), click());
            SystemClock.sleep(3000);

            onView(withText("Entrant Locations")).check(matches(isDisplayed()));
            onView(withText("Close")).check(matches(isDisplayed()));

            onView(withText("Close")).perform(click());
        }

        Tasks.await(firestore.collection("events").document(eventId)
                .collection("waitlist").document("fake-entrant-uid").delete(), 15, TimeUnit.SECONDS);
        Tasks.await(firestore.collection("events").document(eventId).delete(), 15, TimeUnit.SECONDS);
    }


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
     * Test if the event screen shows that geolocation is required when the event requires it
     */
    @Test
    public void geolocationRequiredDisplayedTest() throws Exception {
        signInTestUser();
        String eventId = createViewEventTestEvent("UofA Geolocation Display Event " + System.currentTimeMillis(), "Geolocation display test in Edmonton.", "CAB Edmonton", true, 5, 6, 0, 10, "geolocation-display-host", "", true);

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ViewEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);

        try (ActivityScenario<ViewEventActivity> ignored = ActivityScenario.launch(intent)) {
            SystemClock.sleep(4000);
            onView(withId(R.id.viewEventGeolocation)).check(matches(withText(R.string.event_geolocation_enabled)));
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

            onView(withId(R.id.viewEventJoinWaitlistButton)).perform(scrollTo(), click());
            onView(withText(R.string.join)).perform(click());
            SystemClock.sleep(4000);

            onView(withId(R.id.viewEventWaitlistJoinedLabel)).check(matches(isDisplayed()));
            onView(withId(R.id.viewEventLeaveWaitlistButton)).check(matches(isDisplayed()));
        }

        deleteViewEventTestData(eventId);
    }

    /**
     * Test if joining the waitlist shows the lottery information popup
     */
    @Test
    public void joinWaitlistShowsLotteryPopupTest() throws Exception {
        signInTestUser();
        String title = "UofA Lottery Popup Event " + System.currentTimeMillis();
        String eventId = createViewEventTestEvent(title, "Lottery popup test for Edmonton students.", "CAB Edmonton", true, 5, 6, 0, 10, "lottery-popup-host", "");

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ViewEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);

        try (ActivityScenario<ViewEventActivity> ignored = ActivityScenario.launch(intent)) {
            SystemClock.sleep(4000);

            onView(withId(R.id.viewEventJoinWaitlistButton)).perform(scrollTo(), click());

            onView(withText(R.string.join_waitlist)).check(matches(isDisplayed()));
            onView(withText(containsString("terms of the lottery"))).check(matches(isDisplayed()));
            onView(withText(R.string.join)).check(matches(isDisplayed()));
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

            onView(withId(R.id.viewEventJoinWaitlistButton)).perform(scrollTo(), click());
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

            onView(withId(R.id.viewEventLeaveWaitlistButton)).perform(scrollTo(), click());
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

            onView(withId(R.id.viewEventLeaveWaitlistButton)).perform(scrollTo(), click());
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

            onView(withId(R.id.viewEventJoinWaitlistButton)).perform(scrollTo()).check(matches(isDisplayed()));
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

            onView(withId(R.id.viewEventJoinWaitlistButton)).perform(scrollTo()).check(matches(isDisplayed()));
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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String title = "UofA Organizer Event " + System.currentTimeMillis();
        String eventId = createViewEventTestEvent(
                title,
                "Organizer view test in Edmonton.",
                "SUB Edmonton",
                true,
                5,
                6,
                0,
                10,
                currentUser.getUid(),
                ""
        );

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ViewEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        intent.putExtra("CAN_EDIT_EVENT", true);

        try (ActivityScenario<ViewEventActivity> ignored = ActivityScenario.launch(intent)) {
            SystemClock.sleep(4000);

            onView(withId(R.id.viewEventEditButton)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.viewEventScreenTitle)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.viewEventEditButton)).perform(scrollTo(), click());
            SystemClock.sleep(3000);

            onView(withId(R.id.createEventTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.createEventSubmitButton)).check(matches(withText(R.string.edit_event_submit)));
        }

        deleteViewEventTestData(eventId);
    }

    /**
     * tests that a removed organizer can no longer open an old hosted event in edit mode
     */
    @Test
    public void removedOrganizerCannotEditHostedEventTest() throws Exception {
        FirebaseAuth.getInstance().signOut();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String email = "removedorganizer" + timestamp + "@gmail.com";
        String password = "test123";
        String username = "removedorganizer" + timestamp;
        String uid = createTemporaryUser(email, password, username, "Removed Organizer", "organizer");
        String eventId = createViewEventTestEvent(
                "Removed Organizer Event " + timestamp,
                "Removed organizer regression test.",
                "SUB Edmonton",
                true,
                5,
                6,
                0,
                10,
                uid,
                ""
        );

        try {
            markUserSuspended(uid);
            signInUser(email, password);

            Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ViewEventActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            intent.putExtra("CAN_EDIT_EVENT", true);

            try (ActivityScenario<ViewEventActivity> ignored = ActivityScenario.launch(intent)) {
                SystemClock.sleep(4000);

                onView(withId(R.id.viewEventEditButton))
                        .check(matches(withEffectiveVisibility(GONE)));
                onView(withId(R.id.viewEventScreenTitle))
                        .check(matches(withEffectiveVisibility(GONE)));
            }
        } finally {
            deleteEventOnly(eventId);
            deleteTemporaryUser(uid, email, password);
        }
    }

    /**
     * signs in the shared test account and ensures that remember-me is disabled
     */
    private void signInTestUser() throws Exception {
        TestAuthHelper.ensureSharedTestUser();
    }

    /**
     * signs in with the supplied credentials
     * @param email
     * email of the account to sign in
     * @param password
     * password of the account to sign in
     */
    private void signInUser(String email, String password) throws Exception {
        FirebaseAuth.getInstance().signOut();
        Tasks.await(
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password),
                15,
                TimeUnit.SECONDS
        );
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
        return createViewEventTestEvent(title, description, location, waitlistOpen, deadlineOffsetDays, eventOffsetDays, totalEntrants, maxEntrants, hostUid, waitlistStatus, false);
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
     * @param requiresGeolocation
     * whether the event should require device geolocation on join
     * @return
     * the document id of the created event
     */
    private String createViewEventTestEvent(String title, String description, String location, boolean waitlistOpen, int deadlineOffsetDays, int eventOffsetDays, int totalEntrants, int maxEntrants, String hostUid, String waitlistStatus, boolean requiresGeolocation) throws Exception {
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
        eventPayload.put("requiresGeolocation", requiresGeolocation);
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
     * creates a temporary auth user and profile for the suspended organizer test
     * @param email
     * email of the temporary user
     * @param password
     * password of the temporary user
     * @param username
     * username of the temporary user
     * @param fullName
     * full name of the temporary user
     * @param accountType
     * account type stored on the temporary profile
     * @return
     * uid of the created temporary user
     */
    private String createTemporaryUser(
            String email,
            String password,
            String username,
            String fullName,
            String accountType
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
        payload.put("phoneNumber", "888 888 8888");
        payload.put("accountType", accountType);
        payload.put("createdAt", Timestamp.now());
        payload.put("deleted", false);
        payload.put("pendingEmail", "");

        Tasks.await(
                FirebaseFirestore.getInstance().collection("users").document(created.getUid()).set(payload),
                15,
                TimeUnit.SECONDS
        );
        return created.getUid();
    }

    /**
     * marks a user profile as suspended and removes organizer status
     * @param uid
     * uid of the user to update
     */
    private void markUserSuspended(String uid) throws Exception {
        Map<String, Object> updates = new HashMap<>();
        updates.put("accountType", "user");
        updates.put("suspended", true);
        Tasks.await(
                FirebaseFirestore.getInstance().collection("users").document(uid).update(updates),
                15,
                TimeUnit.SECONDS
        );
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

    /**
     * deletes only the event document for tests that do not create waitlist entries
     * @param eventId
     * id of the event to delete
     */
    private void deleteEventOnly(String eventId) throws Exception {
        Tasks.await(
                FirebaseFirestore.getInstance().collection("events").document(eventId).delete(),
                15,
                TimeUnit.SECONDS
        );
    }

    /**
     * deletes a temporary auth user and profile document
     * @param uid
     * uid of the temporary user
     * @param email
     * email of the temporary user
     * @param password
     * password of the temporary user
     */
    private void deleteTemporaryUser(String uid, String email, String password) throws Exception {
        FirebaseAuth.getInstance().signOut();
        try {
            Tasks.await(
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password),
                    15,
                    TimeUnit.SECONDS
            );
            try {
                Tasks.await(
                        FirebaseFirestore.getInstance().collection("users").document(uid).delete(),
                        15,
                        TimeUnit.SECONDS
                );
            } catch (Exception ignored) {
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                try {
                    Tasks.await(currentUser.delete(), 15, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                }
            }
        } finally {
            FirebaseAuth.getInstance().signOut();
        }
    }
}
