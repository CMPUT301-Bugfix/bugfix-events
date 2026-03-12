package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminBrowseEventsActivityTest {

    @Rule
    public ActivityScenarioRule<AdminBrowseEventsActivity> scenario =
            new ActivityScenarioRule<>(AdminBrowseEventsActivity.class);

    @Test
    public void testBrowseEventsViewsDisplayed() {
        onView(withId(R.id.adminEventsBackButton)).check(matches(isDisplayed()));
        onView(withId(R.id.adminEventsScrollView)).check(matches(isDisplayed()));    }

    @Test
    public void testBackButtonClosesActivity() {
        onView(withId(R.id.adminEventsBackButton)).perform(click());
    }

    @Test
    public void testClickEventOpensDetails() {
        onView(withText("bean party")).perform(click());

        onView(withId(R.id.adminEventDetailsTitleValue)).check(matches(isDisplayed()));
    }
}
