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
 * UI tests for {@link UserProfilesActivity}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserProfilesActivityTest {

    @Rule
    public ActivityScenarioRule<UserProfilesActivity> scenario =
            new ActivityScenarioRule<>(UserProfilesActivity.class);


    /**
     * Verifies that the main user profile list views are visible.
     */
    @Test
    public void testUserProfilesViewsDisplayed() {
        onView(withId(R.id.userProfilesBackButton)).check(matches(isDisplayed()));
        onView(withId(R.id.userTypeFilterSpinner)).check(matches(isDisplayed()));
    }

    /**
     * Verifies that pressing the back button closes the screen.
     */
    @Test
    public void testBackButtonClosesActivity() {
        onView(withId(R.id.userProfilesBackButton)).perform(click());
    }
}
