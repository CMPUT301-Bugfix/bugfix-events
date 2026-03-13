package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI tests for {@link AdminZoneActivity}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminZoneActivityTest {

    @Rule
    public ActivityScenarioRule<AdminZoneActivity> scenario =
            new ActivityScenarioRule<>(AdminZoneActivity.class);



    /**
     * Verifies that the main admin action buttons are visible.
     */
    @Test
    public void testAdminZoneButtonsDisplayed() {
        onView(withId(R.id.adminUserProfilesButton)).check(matches(isDisplayed()));
        onView(withId(R.id.adminEventsButton)).check(matches(isDisplayed()));
    }

    /**
     * Verifies that the back button can be tapped.
     */
    @Test
    public void testBackButtonClickable() {
        onView(withId(R.id.adminZoneBackButton)).perform(click());
    }
}
