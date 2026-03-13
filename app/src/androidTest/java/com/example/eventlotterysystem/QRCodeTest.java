package com.example.eventlotterysystem;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.click;

import static org.junit.Assert.*;

import android.content.Intent;
import android.widget.ImageView;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the functionality of QRCode
 * This activity generate a unique QRCode that linked to specific event
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class QRCodeTest {

    /**
     * Helper that launch the activity with a given eventId
     * @param eventId
     *      the Key of database that store all the information of the event
     * @return
     *      The running activity scenario
     */
    private ActivityScenario<QRCode> launchWithEventId(String eventId) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), QRCode.class);
        intent.putExtra("Event_ID", eventId);
        return ActivityScenario.launch(intent);
    }

    /**
     * Test to see if the imageView is visible
     */
    @Test
    public void TestQRCodeImageView(){
        try (ActivityScenario<QRCode> scenario = launchWithEventId("test123")) {
            onView(withId(R.id.qrCode)).check(matches(isDisplayed()));
        }
    }

    /**
     * Test if the QRCode bitmap is generated
     */
    @Test
    public void TestQRCodeBitmap(){
        try (ActivityScenario<QRCode> scenario = launchWithEventId("test123")){
            scenario.onActivity(activity -> {
                ImageView qrcode = activity.findViewById(R.id.qrCode);
                assertNotNull(qrcode.getDrawable());
            });
        }
    }

    /**
     * Test if Back button destroy the intent and finished activity
     */
    @Test
    public void TestQRCodeBackButton(){
        try (ActivityScenario<QRCode> scenario = launchWithEventId("test123")){
            onView(withId(R.id.qrCodeBack)).perform(click());
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

}
