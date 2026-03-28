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
import static com.example.eventlotterysystem.EntrantsActivity.EVENT_ID;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Tests the functionality of the AllEntrantsActivity
 * this is the activity that shows the Organizer all entrants of a given Status
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AllEntrantsActivityTest {

    private final String EventId = "WrGnWl3sZcFMzFhEmgln";
    //private String EventId = "tEJkEpXh3FOGPePn3W3k";
    private final String waitlistUser = "Username: testU4";
    private final String chosenUser = "Username: testU1";
    private final String confirmedUser = "Username: testU2";
    private final String declinedUser = "Username: testU3";


    /**
     * Test to see if all entrants are displayed for an event
     * @throws Exception if authentication or asynchronous setup fails
     */
    @Test
    public void viewAllEntrantsTest() throws Exception {
        signInTestUser();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                AllEntrantsActivity.class
        );
        intent.putExtra(EVENT_ID, EventId);
        try (ActivityScenario<AllEntrantsActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(10000); // had to add sys clock as my laptop could not load users in time
            /* don't know why this doesn't work
            onData(allOf(is(instanceOf(String.class)), is(waitlistUser)))
                    .inAdapterView(withId(R.id.allEntrantsListView))
                    .check(matches(isDisplayed()));
            */
            onView(withText(waitlistUser)).check(matches(isDisplayed()));
        }
    }

    /**
     * Test to see if activity shows entrants when the Activity is navigated to
     * @throws Exception if authentication or asynchronous setup fails
     */
    @Test
    public void navigatedToAllEntrantsActivityTest() throws Exception {
        signInTestUser();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantsActivity.class
        );
        intent.putExtra(EVENT_ID, EventId);
        try (ActivityScenario<EntrantsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.entrantsAllEntrantsButton)).perform(click());
            SystemClock.sleep(10000); // had to add sys clock as my laptop could not load users in time
            onView(withText(waitlistUser)).check(matches(isDisplayed()));
        }
    }

    /**
     * Test to see if all entrants in the waiting list are displayed for an event
     * @throws Exception if authentication or asynchronous setup fails
     */
    @Test
    public void viewWaitingListEntrantsTest() throws Exception {
        signInTestUser();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                AllEntrantsActivity.class
        );
        intent.putExtra(EVENT_ID, EventId);
        intent.putExtra(AllEntrantsActivity.STATUS_FILTER, "IN_WAITLIST");
        try (ActivityScenario<AllEntrantsActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(10000); // had to add sys clock as my laptop could not load users in time
            /* don't know why this doesn't work
            onData(allOf(is(instanceOf(String.class)), is(waitlistUser)))
                    .inAdapterView(withId(R.id.allEntrantsListView))
                    .check(matches(isDisplayed()));
            */
            onView(withText(waitlistUser)).check(matches(isDisplayed()));

        }
    }

    /**
     * Test to see if all entrants in the chosen list are displayed for an event
     * @throws Exception if authentication or asynchronous setup fails
     */
    @Test
    public void viewChosenEntrantsTest() throws Exception  {
        signInTestUser();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                AllEntrantsActivity.class
        );
        intent.putExtra(EVENT_ID, EventId);
        intent.putExtra(AllEntrantsActivity.STATUS_FILTER, "CHOSEN");
        try (ActivityScenario<AllEntrantsActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(10000); // had to add sys clock as my laptop could not load users in time
            /* don't know why this doesn't work
            onData(allOf(is(instanceOf(String.class)), is(chosenUser)))
                    .inAdapterView(withId(R.id.allEntrantsListView))
                    .check(matches(isDisplayed()));
            */
            onView(withText(chosenUser)).check(matches(isDisplayed()));
        }
    }

    /**
     * Test to see if all entrants in the confirmed list are displayed for an event
     * @throws Exception if authentication or asynchronous setup fails
     */
    @Test
    public void viewConfirmedEntrantsTest() throws Exception  {
        signInTestUser();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                AllEntrantsActivity.class
        );
        intent.putExtra(EVENT_ID, EventId);
        intent.putExtra(AllEntrantsActivity.STATUS_FILTER, "CONFIRMED");
        try (ActivityScenario<AllEntrantsActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(10000); // had to add sys clock as my laptop could not load users in time
            /* don't know why this doesn't work
            onData(allOf(is(instanceOf(String.class)), is(confirmedUser)))
                    .inAdapterView(withId(R.id.allEntrantsListView))
                    .check(matches(isDisplayed()));
            */
            onView(withText(confirmedUser)).check(matches(isDisplayed()));

        }
    }

    /**
     * Test to see if all entrants iin the declined list are displayed for an event
     * @throws Exception if authentication or asynchronous setup fails
     */
    @Test
    public void viewDeclinedEntrantsTest() throws Exception  {
        signInTestUser();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                AllEntrantsActivity.class
        );
        intent.putExtra(EVENT_ID, EventId);
        intent.putExtra(AllEntrantsActivity.STATUS_FILTER, "DECLINED");
        try (ActivityScenario<AllEntrantsActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(10000); // had to add sys clock as my laptop could not load users in time
            /* don't know why this doesn't work
            onData(allOf(is(instanceOf(String.class)), is(declinedUser)))
                    .inAdapterView(withId(R.id.allEntrantsListView))
                    .check(matches(isDisplayed()));
            */
            onView(withText(declinedUser)).check(matches(isDisplayed()));

        }
    }

    /**
     * Test if the organizer can view the list of cancelled entrants for an Event
     */
    @Test
    public void viewCancelledEntrantsStoryTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String eventId = createManagedEntrantsTestEvent("Edmonton Cancelled Entrants Event " + timestamp);
        String email = "test" + timestamp + "@gmail.com";
        String password = "test123";
        String username = "cancelled" + timestamp;
        String uid = createTemporaryEntrant(email, password, username, "Cancelled Test");
        joinWaitlistAsCurrentUser(eventId);
        signInTestUser();
        Tasks.await(new EventRepository().updateWaitlistStatus(eventId, uid, EventRepository.WAITLIST_STATUS_DECLINED), 15, TimeUnit.SECONDS);

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AllEntrantsActivity.class);
        intent.putExtra(EVENT_ID, eventId);
        intent.putExtra(AllEntrantsActivity.STATUS_FILTER, EventRepository.WAITLIST_STATUS_DECLINED);

        try {
            try (ActivityScenario<AllEntrantsActivity> scenario = ActivityScenario.launch(intent)) {
                SystemClock.sleep(5000);
                onView(withText("Username: " + username)).check(matches(isDisplayed()));
            }
        } finally {
            deleteEntrantsStoryTestData(eventId, uid, email, password);
        }
    }

    /**
     * Test if the organizer can view the final list of confirmed entrants for an Event
     */
    @Test
    public void viewFinalEnrolledEntrantsStoryTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String eventId = createManagedEntrantsTestEvent("Edmonton Confirmed Entrants Event " + timestamp);
        String email = "testconfirmed" + timestamp + "@gmail.com";
        String password = "test123";
        String username = "confirmed" + timestamp;
        String uid = createTemporaryEntrant(email, password, username, "Confirmed Test");
        joinWaitlistAsCurrentUser(eventId);
        signInTestUser();
        Tasks.await(new EventRepository().updateWaitlistStatus(eventId, uid, EventRepository.WAITLIST_STATUS_CONFIRMED), 15, TimeUnit.SECONDS);

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AllEntrantsActivity.class);
        intent.putExtra(EVENT_ID, eventId);
        intent.putExtra(AllEntrantsActivity.STATUS_FILTER, EventRepository.WAITLIST_STATUS_CONFIRMED);

        try {
            try (ActivityScenario<AllEntrantsActivity> scenario = ActivityScenario.launch(intent)) {
                SystemClock.sleep(5000);
                onView(withText("Username: " + username)).check(matches(isDisplayed()));
            }
        } finally {
            deleteEntrantsStoryTestData(eventId, uid, email, password);
        }
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
     * creates an Event document managed by the shared test account
     * @param title
     * title of the Event that should be created
     * @return
     * the document id of the created Event
     */
    private String createManagedEntrantsTestEvent(String title) throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String eventId = firestore.collection("events").document().getId();

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("title", title);
        eventPayload.put("description", "Entrants story test in Edmonton.");
        eventPayload.put("location", "SUB Edmonton");
        eventPayload.put("posterUrl", "");
        eventPayload.put("maxEntrants", 10);
        eventPayload.put("maxParticipants", 5);
        eventPayload.put("totalEntrants", 1);
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
     * creates a temporary entrant account for an entrants story test
     * @param email
     * email stored on the profile
     * @param password
     * password used for the temporary auth account
     * @param username
     * username stored on the profile
     * @param fullName
     * name stored on the profile
     * @return
     * uid of the created temporary user
     */
    private String createTemporaryEntrant(String email, String password, String username, String fullName) throws Exception {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        Tasks.await(auth.createUserWithEmailAndPassword(email, password), 15, TimeUnit.SECONDS);
        FirebaseUser currentUser = auth.getCurrentUser();

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("username", username);
        userPayload.put("fullName", fullName);
        userPayload.put("email", email);
        userPayload.put("phoneNumber", "888 888 8888");
        userPayload.put("accountType", "entrant");
        userPayload.put("createdAt", Timestamp.now());
        userPayload.put("deleted", false);

        Tasks.await(FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).set(userPayload), 15, TimeUnit.SECONDS);
        return currentUser.getUid();
    }

    /**
     * joins the current signed-in user to the Event waitlist
     * @param eventId
     * id of the Event being joined
     */
    private void joinWaitlistAsCurrentUser(String eventId) throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Tasks.await(new EventRepository().joinWaitlist(eventId, currentUser), 15, TimeUnit.SECONDS);
    }

    /**
     * removes the Event and temporary entrant account created for an entrants story test
     * @param eventId
     * id of the Event to remove
     * @param uid
     * uid of the temporary entrant
     * @param email
     * email of the temporary entrant
     * @param password
     * password of the temporary entrant
     */
    private void deleteEntrantsStoryTestData(String eventId, String uid, String email, String password) throws Exception {
        signInTestUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Tasks.await(firestore.collection("events").document(eventId).collection("waitlist").document(uid).delete(), 15, TimeUnit.SECONDS);
        Tasks.await(firestore.collection("events").document(eventId).delete(), 15, TimeUnit.SECONDS);

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
        Tasks.await(FirebaseAuth.getInstance().getCurrentUser().delete(), 15, TimeUnit.SECONDS);
    }
}
