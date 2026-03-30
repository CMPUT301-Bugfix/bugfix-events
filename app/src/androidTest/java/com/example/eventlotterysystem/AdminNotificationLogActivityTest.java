package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.example.eventlotterysystem.EntrantsActivity.EVENT_ID;

import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * Tests the functionality of the AdminNotificationLogActivity
 * this is the activity that shows all Notifications to Admins
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminNotificationLogActivityTest {

    private final String NotificationTitle = "AdminTest";
    private final String NotificationContent = "For testing Admin Viewing Capability";

    /**
     * Test to see if notifications are displayed for an admin
     * @throws Exception if authentication or asynchronous setup fails
     */
    @Test
    public void viewNotificationsTest() throws Exception {
        signInTestUser();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                AdminNotificationsLogActivity.class
        );

        try (ActivityScenario<AdminNotificationsLogActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(10000); // had to add sys clock as my laptop could not load notifications in time
            onView(withText(NotificationTitle)).check(matches(isDisplayed()));
            onView(withText(NotificationContent)).check(matches(isDisplayed()));
        }
    }

    /**
     * Test to see if activity shows notifications when the Activity is navigated to
     * @throws Exception if authentication or asynchronous setup fails
     */
    @Test
    public void navigatedToAdminNotificationLogActivityTest() throws Exception {
        signInTestUser();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                AdminZoneActivity.class
        );
        try (ActivityScenario<AdminZoneActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(10000); // had to add sys clock as my laptop could not load notifications in time
            onView(withId(R.id.adminNotificationLogButton)).perform(click());
            SystemClock.sleep(10000); // had to add sys clock as my laptop could not load notifications in time
            onView(withText(NotificationTitle)).check(matches(isDisplayed()));
            onView(withText(NotificationContent)).check(matches(isDisplayed()));
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
}
