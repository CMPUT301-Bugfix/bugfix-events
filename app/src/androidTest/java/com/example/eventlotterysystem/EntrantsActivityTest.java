package com.example.eventlotterysystem;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.Intents;
import static com.example.eventlotterysystem.AllEntrantsActivity.STATUS_FILTER;
import static com.example.eventlotterysystem.EntrantsActivity.EVENT_ID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Tests the functionality of the EntrantActivity
 * it allows organiser to navigate to view lists of entrants
 * it also can notify entrants and manages acceptance of entrants
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntrantsActivityTest {
    private final String EventId = "Ihxujm0X8KeGpeT39n6E";

    /**
     * Test to see if Preforming a draw will move the correct number of entrants to chosen and update the database
     */
    @Test
    public void preformDrawTest() throws Exception {
        Intents.init();
        signInTestUser();
        CreateWaitlistEntry("waitlist2","IN_WAITLIST","testU6", EventId);

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantsActivity.class
        );
        intent.putExtra(EVENT_ID, EventId);
        try (ActivityScenario<EntrantsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.entrantsPerformDrawButton)).perform(click());
            SystemClock.sleep(10000);
            Espresso.onView(ViewMatchers.withText("Draw")).perform(ViewActions.click());
            SystemClock.sleep(10000);
            Tasks.await(FirebaseFirestore.getInstance().collection("events").document(EventId).collection("waitlist").document("waitlist2").get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc != null && doc.exists()) {
                        String value = doc.getString("status");
                        assertEquals("CHOSEN",value);
                    }
                }
            }));
        }
        deleteWaitlistEntry("waitlist2",EventId);
        Intents.release();
    }

    /**
     * Test to see if Clean will remove chosen Entrants who failed to accept in time
     */
    @Test
    public void cleanExpiredTest() throws Exception {
        signInTestUser();
        Intents.init();
        CreateWaitlistEntry("waitlist1","CHOSEN","testU5", EventId);


        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantsActivity.class
        );

        try (ActivityScenario<EntrantsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.entrantsProcessExpiredButton)).perform(click());
            SystemClock.sleep(10000);
            Espresso.onView(ViewMatchers.withText("Confirm")).perform(ViewActions.click());
            SystemClock.sleep(10000);
            Tasks.await(FirebaseFirestore.getInstance().collection("events").document(EventId).collection("waitlist").document("waitlist2").get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc != null && doc.exists()) {
                        String value = doc.getString("status");
                        assertEquals("DECLINED",value);
                    }
                }
            }));
        }
        deleteWaitlistEntry("waitlist1",EventId);
        Intents.release();
    }

    /**
     * Test to see if the navigation to view entrants is correct and displays the correct entrants
     */
    @Test
    public void navigateToAllEntrantsTest() throws Exception {
        Intents.init();
        signInTestUser();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantsActivity.class
        );
        intent.putExtra(EVENT_ID, EventId);
        try (ActivityScenario<EntrantsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.entrantsAllEntrantsButton)).perform(click());
            SystemClock.sleep(10000); // had to add sys clock as my laptop could not load users in time
            intended(hasComponent(AllEntrantsActivity.class.getName()));
        }
        Intents.release();
    }

    /**
     * Test to see if the navigation to view entrants of a specific status (chosen) is correct and displays the correct entrants
     * do not need to test other statuses as they are implemented together
     */
    @Test
    public void navigateToChosenEntrantsTest() throws Exception {
        Intents.init();
        signInTestUser();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantsActivity.class
        );
        intent.putExtra(EVENT_ID, EventId);
        try (ActivityScenario<EntrantsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.entrantsChosenButton)).perform(click());
            SystemClock.sleep(10000); // had to add sys clock as my laptop could not load users in time
            intended(allOf(
                    hasComponent(AllEntrantsActivity.class.getName()),
                    hasExtra(STATUS_FILTER, "CHOSEN")
            ));
        }
        Intents.release();
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

    /**
     * deletes a waitlist document from the database
     * @param waitListId
     * document id of the waitlist entry that should be removed
     * @param eventId
     * document id of the event that contains the waitlist entry
     */
    private void deleteWaitlistEntry(String waitListId, String eventId) throws Exception {
        Tasks.await(FirebaseFirestore.getInstance().collection("events").document(eventId).collection("waitlist").document(waitListId).delete(), 15, TimeUnit.SECONDS);
    }

    /**
     * creates a waitlist document from the database
     * @param waitListId
     * document id of the waitlist entry to be created
     * @param status
     * the status the waitlist should be
     * @param uid
     * the user that the waitlist entry belongs to
     * @param eventId
     * document id of the event that contains the waitlist entry
     */
    private void CreateWaitlistEntry(String waitListId, String status, String uid, String eventId) throws Exception {
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("eventId", eventId);
        hashMap.put("status", status);
        hashMap.put("uid", uid);
        Tasks.await(FirebaseFirestore.getInstance().collection("events").document(eventId).collection("waitlist").document(waitListId).set(hashMap), 15, TimeUnit.SECONDS);
    }
}
