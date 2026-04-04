package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

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

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * UI tests for {@link AdminBrowseEventsActivity}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminBrowseEventsActivityTest {

    /**
     * Verifies that the main browse-events views are visible.
     */
    @Test
    public void testBrowseEventsViewsDisplayed() throws Exception {
        signInTestUser();
        String title = "bean party " + System.currentTimeMillis();
        String eventId = createAdminBrowseTestEvent(title);

        try {
            try (ActivityScenario<AdminBrowseEventsActivity> ignored = ActivityScenario.launch(AdminBrowseEventsActivity.class)) {
                SystemClock.sleep(4000);
                onView(withId(R.id.adminEventsBackButton)).check(matches(isDisplayed()));
                onView(withId(R.id.adminEventsScrollView)).check(matches(isDisplayed()));
                onView(Matchers.allOf(withId(R.id.eventTitleValue), withText(title))).check(matches(isDisplayed()));
            }
        } finally {
            deleteEvent(eventId);
        }
    }

    /**
     * Verifies that pressing the back button closes the screen.
     */
    @Test
    public void testBackButtonClosesActivity() throws Exception {
        signInTestUser();

        try (ActivityScenario<AdminBrowseEventsActivity> ignored = ActivityScenario.launch(AdminBrowseEventsActivity.class)) {
            SystemClock.sleep(4000);
            onView(withId(R.id.adminEventsBackButton)).perform(click());
        }
    }

    /**
     * Verifies that selecting an event opens the details screen.
     */
    @Test
    public void testClickEventOpensDetails() throws Exception {
        signInTestUser();
        String title = "bean party " + System.currentTimeMillis();
        String eventId = createAdminBrowseTestEvent(title);

        try {
            try (ActivityScenario<AdminBrowseEventsActivity> ignored = ActivityScenario.launch(AdminBrowseEventsActivity.class)) {
                SystemClock.sleep(4000);
                onView(Matchers.allOf(withId(R.id.eventTitleValue), withText(title))).perform(click());
                SystemClock.sleep(2000);
                onView(withId(R.id.adminEventDetailsTitleValue)).check(matches(isDisplayed()));
            }
        } finally {
            deleteEvent(eventId);
        }
    }

    /**
     * signs in the shared admin-capable test account and ensures that remember-me is disabled
     */
    private void signInTestUser() throws Exception {
        TestAuthHelper.ensureSharedTestUser();
    }

    /**
     * creates an event visible to the admin browse screen
     */
    private String createAdminBrowseTestEvent(String title) throws Exception {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String eventId = firestore.collection("events").document().getId();

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("title", title);
        eventPayload.put("description", "Admin browse test event.");
        eventPayload.put("location", "CAB Edmonton");
        eventPayload.put("posterUrl", "");
        eventPayload.put("maxEntrants", 10);
        eventPayload.put("maxParticipants", 5);
        eventPayload.put("totalEntrants", 0);
        eventPayload.put("registrationDeadline", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(4)));
        eventPayload.put("eventDate", new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));
        eventPayload.put("requiresGeolocation", false);
        eventPayload.put("hostUid", currentUser.getUid());
        eventPayload.put("hostDisplayName", "Admin Test");
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
     * deletes a test event
     */
    private void deleteEvent(String eventId) throws Exception {
        Tasks.await(FirebaseFirestore.getInstance().collection("events").document(eventId).delete(), 15, TimeUnit.SECONDS);
    }
}
