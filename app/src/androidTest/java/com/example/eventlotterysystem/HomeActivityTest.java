package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anything;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.Test;
import org.junit.runner.RunWith;

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
}
