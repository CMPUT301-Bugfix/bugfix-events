package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.example.eventlotterysystem.EntrantsActivity.EVENT_ID;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * Tests the functionality of the AllEntrantsActivity
 * this is the activity that shows the Organizer all entrants of a given Status
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AllEntrantsActivityTest {

    private final String EventId = "WrGnWl3sZcFMzFhEmgln";
    //private String EventId = "tEJkEpXh3FOGPePn3W3k";
    private final String waitlistUser = "Username: testU4";
    private final String chosenUser = "Username: testU1";
    private final String confirmedUser = "Username: testU2";
    private final String declinedUser = "Username: testU3";

    /**
     * Test to see if all entrants are displayed for an event
     */
    @Test
    public void viewAllEntrantsTest() throws Exception {
        signInTestUser();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                AllEntrantsActivity.class
        );
        intent.putExtra(EVENT_ID, EventId);
        try (ActivityScenario<AllEntrantsActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(10000); // had to add sys clock as my laptop could not load users in time
            /* don't know why this doesn't work
            onData(allOf(is(instanceOf(String.class)), is(waitlistUser)))
                    .inAdapterView(withId(R.id.allEntrantsListView))
                    .check(matches(isDisplayed()));
            */
            onView(withText(waitlistUser)).check(matches(isDisplayed()));
        }
    }

    /**
     * Test to see if activity shows entrants when the Activity is navigated to
     */
    @Test
    public void navigatedToAllEntrantsActivityTest() throws Exception {
        signInTestUser();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantsActivity.class
        );
        intent.putExtra(EVENT_ID, EventId);
        try (ActivityScenario<EntrantsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.entrantsAllEntrantsButton)).perform(click());
            SystemClock.sleep(10000); // had to add sys clock as my laptop could not load users in time
            onView(withText(waitlistUser)).check(matches(isDisplayed()));
        }
    }

    /**
     * Test to see if all entrants in the waiting list are displayed for an event
     */
    @Test
    public void viewWaitingListEntrantsTest() throws Exception {
        signInTestUser();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                AllEntrantsActivity.class
        );
        intent.putExtra(EVENT_ID, EventId);
        intent.putExtra(AllEntrantsActivity.STATUS_FILTER, "IN_WAITLIST");
        try (ActivityScenario<AllEntrantsActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(10000); // had to add sys clock as my laptop could not load users in time
            /* don't know why this doesn't work
            onData(allOf(is(instanceOf(String.class)), is(waitlistUser)))
                    .inAdapterView(withId(R.id.allEntrantsListView))
                    .check(matches(isDisplayed()));
            */
            onView(withText(waitlistUser)).check(matches(isDisplayed()));

        }
    }

    /**
     * Test to see if all entrants in the chosen list are displayed for an event
     */
    @Test
    public void viewChosenEntrantsTest() throws Exception  {
        signInTestUser();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                AllEntrantsActivity.class
        );
        intent.putExtra(EVENT_ID, EventId);
        intent.putExtra(AllEntrantsActivity.STATUS_FILTER, "CHOSEN");
        try (ActivityScenario<AllEntrantsActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(10000); // had to add sys clock as my laptop could not load users in time
            /* don't know why this doesn't work
            onData(allOf(is(instanceOf(String.class)), is(chosenUser)))
                    .inAdapterView(withId(R.id.allEntrantsListView))
                    .check(matches(isDisplayed()));
            */
            onView(withText(chosenUser)).check(matches(isDisplayed()));
        }
    }

    /**
     * Test to see if all entrants in the confirmed list are displayed for an event
     */
    @Test
    public void viewConfirmedEntrantsTest() throws Exception  {
        signInTestUser();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                AllEntrantsActivity.class
        );
        intent.putExtra(EVENT_ID, EventId);
        intent.putExtra(AllEntrantsActivity.STATUS_FILTER, "CONFIRMED");
        try (ActivityScenario<AllEntrantsActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(10000); // had to add sys clock as my laptop could not load users in time
            /* don't know why this doesn't work
            onData(allOf(is(instanceOf(String.class)), is(confirmedUser)))
                    .inAdapterView(withId(R.id.allEntrantsListView))
                    .check(matches(isDisplayed()));
            */
            onView(withText(confirmedUser)).check(matches(isDisplayed()));

        }
    }

    /**
     * Test to see if all entrants iin the declined list are displayed for an event
     */
    @Test
    public void viewDeclinedEntrantsTest() throws Exception  {
        signInTestUser();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                AllEntrantsActivity.class
        );
        intent.putExtra(EVENT_ID, EventId);
        intent.putExtra(AllEntrantsActivity.STATUS_FILTER, "DECLINED");
        try (ActivityScenario<AllEntrantsActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(10000); // had to add sys clock as my laptop could not load users in time
            /* don't know why this doesn't work
            onData(allOf(is(instanceOf(String.class)), is(declinedUser)))
                    .inAdapterView(withId(R.id.allEntrantsListView))
                    .check(matches(isDisplayed()));
            */
            onView(withText(declinedUser)).check(matches(isDisplayed()));

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
