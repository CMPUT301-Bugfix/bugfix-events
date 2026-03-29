package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
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

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Tests the functionality of the HomeActivity
 * it is the starting Activity of normal users and shows a list of current event
 * the user is able to navigate to the other sections of the app through this activity
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class HomeActivityTest {

    /**
     * Test to see if events are loaded from database and is displayed to the user
     */
    @Test
    public void viewEventsTest() {
        FirebaseAuth.getInstance().signOut();
        Context context = ApplicationProvider.getApplicationContext();
        AuthSessionPreference.setRemember(context, false);

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.menuLoginButton)).perform(click());
            onView(withId(R.id.loginIdentifierInput))
                    .perform(replaceText("test"), closeSoftKeyboard());
            onView(withId(R.id.loginPasswordInput))
                    .perform(replaceText("test123"), closeSoftKeyboard());
            onView(withId(R.id.loginButton)).perform(click());

            SystemClock.sleep(6000);

            onData(anything())
                    .inAdapterView(withId(R.id.homeEventsListView))
                    .atPosition(0)
                    .check(matches(isDisplayed()));

            onView(withId(R.id.homeEventsEmptyState))
                    .check(matches(withEffectiveVisibility(GONE)));
        }
    }

    /**
     * Test to navigation to MyThings activity is correct with the correct user information
     */
    @Test
    public void NavigateToMyThingsTest() {
        FirebaseAuth.getInstance().signOut();
        Context context = ApplicationProvider.getApplicationContext();
        AuthSessionPreference.setRemember(context, false);

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.menuLoginButton)).perform(click());
            onView(withId(R.id.loginIdentifierInput))
                    .perform(replaceText("test"), closeSoftKeyboard());
            onView(withId(R.id.loginPasswordInput))
                    .perform(replaceText("test123"), closeSoftKeyboard());
            onView(withId(R.id.loginButton)).perform(click());

            SystemClock.sleep(4000);

            onView(withId(R.id.myThingsButton)).perform(click());

            SystemClock.sleep(3000);

            onView(withId(R.id.settingsButton)).check(matches(isDisplayed()));
            onView(withId(R.id.myThingsSubtitle)).check(matches(isDisplayed()));
        }
    }

    /**
     * Test if filtering by keyword only shows matching events on the Home screen
     */
    @Test
    public void filterEventsByKeywordTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String matchingTitle = "Edmonton AI Event " + timestamp;
        String hiddenTitle = "Edmonton Music Event " + timestamp;
        String matchingEventId = createHomeTestEvent(matchingTitle, "Artificial intelligence event in Edmonton.", "CCIS Edmonton", 5, 4, 12, Arrays.asList("ai" + timestamp, "technology"));
        String hiddenEventId = createHomeTestEvent(hiddenTitle, "Live music event in Edmonton.", "SUB Edmonton", 6, 4, 20, Arrays.asList("music" + timestamp, "concert"));

        try {
            try (ActivityScenario<HomeActivity> ignored = ActivityScenario.launch(HomeActivity.class)) {
                SystemClock.sleep(4000);

                onView(withId(R.id.homeEventsFilterButton)).perform(click());
                onView(withId(R.id.filterKeywordInput)).perform(replaceText("ai" + timestamp), closeSoftKeyboard());
                onView(withText(R.string.filter_apply)).perform(click());

                SystemClock.sleep(1500);

                onData(withEventTitle(matchingTitle)).inAdapterView(withId(R.id.homeEventsListView)).check(matches(isDisplayed()));
                onView(withText(hiddenTitle)).check(doesNotExist());
            }
        } finally {
            deleteHomeTestEvent(matchingEventId);
            deleteHomeTestEvent(hiddenEventId);
        }
    }

    /**
     * Test if filtering by event date range only shows available events inside that date range
     */
    @Test
    public void filterEventsByAvailabilityTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String matchingTitle = "Edmonton Date Event " + timestamp;
        String hiddenTitle = "Edmonton Late Event " + timestamp;
        String matchingEventId = createHomeTestEvent(matchingTitle, "Date filter event in Edmonton.", "CAB Edmonton", 5, 4, 14, Arrays.asList("date-filter"));
        String hiddenEventId = createHomeTestEvent(hiddenTitle, "Later event in Edmonton.", "ECHA Edmonton", 15, 4, 14, Arrays.asList("date-filter"));

        try {
            try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(HomeActivity.class)) {
                SystemClock.sleep(4000);

                applyEventDateFilter(scenario, 4, 6);

                SystemClock.sleep(1500);

                onData(withEventTitle(matchingTitle)).inAdapterView(withId(R.id.homeEventsListView)).check(matches(isDisplayed()));
                onView(withText(hiddenTitle)).check(doesNotExist());
            }
        } finally {
            deleteHomeTestEvent(matchingEventId);
            deleteHomeTestEvent(hiddenEventId);
        }
    }

    /**
     * Test if filtering by max entrants only shows events inside the selected capacity range
     */
    @Test
    public void filterEventsByCapacityTest() throws Exception {
        signInTestUser();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String matchingTitle = "Edmonton Capacity Event " + timestamp;
        String hiddenTitle = "Edmonton Large Event " + timestamp;
        String matchingEventId = createHomeTestEvent(matchingTitle, "Capacity filter event in Edmonton.", "University Station Edmonton", 5, 4, 10, Arrays.asList("capacity-filter"));
        String hiddenEventId = createHomeTestEvent(hiddenTitle, "Large capacity event in Edmonton.", "Lister Edmonton", 6, 4, 25, Arrays.asList("capacity-filter"));

        try {
            try (ActivityScenario<HomeActivity> ignored = ActivityScenario.launch(HomeActivity.class)) {
                SystemClock.sleep(4000);

                onView(withId(R.id.homeEventsFilterButton)).perform(click());
                onView(withId(R.id.filterMinParticipantsInput)).perform(scrollTo(), replaceText("9"), closeSoftKeyboard());
                onView(withId(R.id.filterMaxParticipantsInput)).perform(scrollTo(), replaceText("12"), closeSoftKeyboard());
                onView(withText(R.string.filter_apply)).perform(click());

                SystemClock.sleep(1500);

                onData(withEventTitle(matchingTitle)).inAdapterView(withId(R.id.homeEventsListView)).check(matches(isDisplayed()));
                onView(withText(hiddenTitle)).check(doesNotExist());
            }
        } finally {
            deleteHomeTestEvent(matchingEventId);
            deleteHomeTestEvent(hiddenEventId);
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
     * creates an event document that should appear on the Home screen
     * @param title
     * title of the event that should be created
     * @param description
     * description to store on the event
     * @param location
     * location to store on the event
     * @param eventOffsetDays
     * number of days from now to set the event date
     * @param deadlineOffsetDays
     * number of days from now to set the registration deadline
     * @param maxEntrants
     * maximum entrant count to store on the event
     * @param keywords
     * keyword labels to store on the event
     * @return
     * the document id of the created event
     */
    private String createHomeTestEvent(String title, String description, String location, int eventOffsetDays, int deadlineOffsetDays, int maxEntrants, List<String> keywords) throws Exception {
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
        eventPayload.put("totalEntrants", 0);
        eventPayload.put("registrationDeadline", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(deadlineOffsetDays)));
        eventPayload.put("eventDate", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(eventOffsetDays)));
        eventPayload.put("requiresGeolocation", false);
        eventPayload.put("hostUid", currentUser.getUid());
        eventPayload.put("hostDisplayName", "Edmonton Organizer");
        eventPayload.put("waitlistOpen", true);
        eventPayload.put("deleted", false);
        eventPayload.put("createdAt", Timestamp.now());
        eventPayload.put("winningMessage", "Welcome to the Edmonton event.");
        eventPayload.put("keywords", keywords);
        eventPayload.put("isPublic", true);

        Tasks.await(firestore.collection("events").document(eventId).set(eventPayload), 15, TimeUnit.SECONDS);
        return eventId;
    }

    /**
     * removes the event document created for a Home screen test
     * @param eventId
     * document id of the event to remove
     */
    private void deleteHomeTestEvent(String eventId) throws Exception {
        Tasks.await(FirebaseFirestore.getInstance().collection("events").document(eventId).delete(), 15, TimeUnit.SECONDS);
    }

    /**
     * applies an event date filter directly to the Home screen
     * @param scenario
     * running scenario for the HomeActivity
     * @param fromOffsetDays
     * earliest allowed event date in days from now
     * @param toOffsetDays
     * latest allowed event date in days from now
     */
    private void applyEventDateFilter(ActivityScenario<HomeActivity> scenario, int fromOffsetDays, int toOffsetDays) throws Exception {
        Date fromDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(fromOffsetDays));
        Date toDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(toOffsetDays));

        scenario.onActivity(activity -> {
            try {
                Field fromField = HomeActivity.class.getDeclaredField("activeEventDateFrom");
                Field toField = HomeActivity.class.getDeclaredField("activeEventDateTo");
                Method applyFilters = HomeActivity.class.getDeclaredMethod("applyFilters");

                fromField.setAccessible(true);
                toField.setAccessible(true);
                applyFilters.setAccessible(true);

                fromField.set(activity, fromDate);
                toField.set(activity, toDate);
                applyFilters.invoke(activity);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    /**
     * matches an EventItem in the Home list by title
     * @param title
     * title that should match the EventItem
     * @return
     * matcher for adapter rows backed by the given event title
     */
    private Matcher<Object> withEventTitle(String title) {
        return new TypeSafeMatcher<>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("event title ").appendValue(title);
            }

            @Override
            protected boolean matchesSafely(Object item) {
                return item instanceof EventItem && title.equals(((EventItem) item).getTitle());
            }
        };
    }
}
