package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
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
 * UI tests for {@link AdminZoneActivity}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminZoneActivityTest {

    /**
     * Verifies that the main admin action buttons are visible.
     */
    @Test
    public void testAdminZoneButtonsDisplayed() throws Exception {
        signInTestUser();

        try (ActivityScenario<AdminZoneActivity> ignored = ActivityScenario.launch(AdminZoneActivity.class)) {
            SystemClock.sleep(4000);
            onView(withId(R.id.adminUserProfilesButton)).check(matches(isDisplayed()));
            onView(withId(R.id.adminEventsButton)).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the back button can be tapped.
     */
    @Test
    public void testBackButtonClickable() throws Exception {
        signInTestUser();

        try (ActivityScenario<AdminZoneActivity> ignored = ActivityScenario.launch(AdminZoneActivity.class)) {
            SystemClock.sleep(4000);
            onView(withId(R.id.adminZoneBackButton)).perform(click());
        }
    }

    /**
     * signs in the shared admin-capable test account and ensures that remember-me is disabled
     */
    private void signInTestUser() throws Exception {
        TestAuthHelper.ensureSharedTestUser();
    }
}
