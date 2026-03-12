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

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserProfileDetailsActivityTest {

    @Rule
    public ActivityScenarioRule<UserProfileDetailsActivity> scenario =
            new ActivityScenarioRule<>(UserProfileDetailsActivity.class);

    @Test
    public void testUserProfileDetailsViewsDisplayed() {
        onView(withId(R.id.userProfileDetailsBackButton)).check(matches(isDisplayed()));
        onView(withId(R.id.userProfileDetailsNameValue)).check(matches(isDisplayed()));
        onView(withId(R.id.userProfileDetailsTypeValue)).check(matches(isDisplayed()));
        onView(withId(R.id.userProfileDetailsUsernameValue)).check(matches(isDisplayed()));
        onView(withId(R.id.userProfileDetailsEmailValue)).check(matches(isDisplayed()));
        onView(withId(R.id.userProfileDetailsPhoneValue)).check(matches(isDisplayed()));
        onView(withId(R.id.userProfileDetailsJoinDateValue)).check(matches(isDisplayed()));
    }

    @Test
    public void testBackButtonClosesActivity() {
        onView(withId(R.id.userProfileDetailsBackButton)).perform(click());
    }
}
