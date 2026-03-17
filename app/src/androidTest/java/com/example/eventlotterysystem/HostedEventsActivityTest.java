package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Tests the functionality of the HostedEventsActivity
 * this activity displays all of the events that the current user has created
 * it also allows navigation to create a new event or open an existing one
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class HostedEventsActivityTest {

    /**
     * Test to see if all hosted events are loaded from database and is displayed to the user
     * @throws Exception if authentication or database setup fails
     */
    @Test
    public void viewEventsTest() throws Exception {
        signInTestUser();
        String title = "UofA Hosted List Event " + System.currentTimeMillis();
        String eventId = createHostedTestEvent(title);

        try (ActivityScenario<HostedEventsActivity> ignored = ActivityScenario.launch(HostedEventsActivity.class)) {
            SystemClock.sleep(4000);

            onView(withText(title)).check(matches(isDisplayed()));
            onView(withId(R.id.hostedEventsEmptyState))
                    .check(matches(withEffectiveVisibility(GONE)));
        }

        deleteEvent(eventId);
    }

    /**
     * Test if navigation to ViewEventActivity is correct event and allows edits
     * @throws Exception if authentication or database setup fails
     */
    @Test
    public void NavigateToEvent() throws Exception {
        signInTestUser();
        String title = "Edmonton Hosted Open Event " + System.currentTimeMillis();
        String eventId = createHostedTestEvent(title);

        try (ActivityScenario<HostedEventsActivity> ignored = ActivityScenario.launch(HostedEventsActivity.class)) {
            SystemClock.sleep(4000);

            onData(anything())
                    .inAdapterView(withId(R.id.hostedEventsListView))
                    .atPosition(0)
                    .perform(click());

            SystemClock.sleep(4000);

            onView(withId(R.id.viewEventEditButton)).check(matches(isDisplayed()));
            onView(withId(R.id.viewEventScreenTitle)).check(matches(isDisplayed()));
        }

        deleteEvent(eventId);
    }

    /**
     * Test if navigation to CreateEventActivity is correct
     * @throws Exception if authentication or activity launch fails
     */
    @Test
    public void NavigateToCreateEvent() throws Exception {
        signInTestUser();

        try (ActivityScenario<HostedEventsActivity> ignored = ActivityScenario.launch(HostedEventsActivity.class)) {
            onView(withId(R.id.createEventButton)).perform(click());

            onView(withId(R.id.createEventTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.createEventTitleInput)).perform(scrollTo()).check(matches(isDisplayed()));
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
     * creates a hosted event directly in the database for the signed-in test user
     * @param title
     * title of the event to create
     * @return
     * the document id of the created event
     */
    private String createHostedTestEvent(String title) throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        EventItem event = new EventItem("", title, "Hosted University of Alberta event in Edmonton.", "SUB Edmonton", "", 10, 5, 0, new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)), new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2)), false, currentUser.getUid(), "");
        return Tasks.await(new EventRepository().createEvent(currentUser, event, null), 15, TimeUnit.SECONDS);
    }

    /**
     * deletes a hosted test event after the test run is complete
     * @param eventId
     * document id of the event to delete
     */
    private void deleteEvent(String eventId) throws Exception {
        Tasks.await(FirebaseFirestore.getInstance().collection("events").document(eventId).delete(), 15, TimeUnit.SECONDS);
    }
}
