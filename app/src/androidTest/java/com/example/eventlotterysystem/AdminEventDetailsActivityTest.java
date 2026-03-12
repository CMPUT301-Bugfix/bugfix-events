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
public class AdminEventDetailsActivityTest {
    @Rule
    public ActivityScenarioRule<AdminEventDetailsActivity> scenario =
            new ActivityScenarioRule<>(AdminEventDetailsActivity.class);

    @Test
    public void testAdminEventDetailsViewsDisplayed() {
        onView(withId(R.id.adminEventDetailsBackButton)).check(matches(isDisplayed()));
        onView(withId(R.id.adminEventDetailsTitleValue)).check(matches(isDisplayed()));
        onView(withId(R.id.adminDeleteEventButton)).check(matches(isDisplayed()));
    }

    @Test
    public void testBackButtonClosesActivity() {
        onView(withId(R.id.adminEventDetailsBackButton)).perform(click());
    }
}
