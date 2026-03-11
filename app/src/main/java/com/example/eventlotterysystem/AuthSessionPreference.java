package com.example.eventlotterysystem;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * This is a class stores authentication preference on the device (if user should be remembered)
 */
public final class AuthSessionPreference {

    private static final String PREFERENCES_NAME = "event_lottery_preferences";
    private static final String REMEMBER_ME_KEY = "remember_me";

    /**
     * This method checks if the device should be remembered for subsequent application startups
     * @param context
     * Context the instance of the application running
     * @return
     * boolean of if device REMEMBER_ME_KEY
     */
    public static boolean shouldRemember(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(REMEMBER_ME_KEY, true);
    }

    /**
     * This method sets whether the device should contain REMEMBER_ME_KEY
     * @param context
     * Context the instance of the application running
     * @param remember
     * boolean of to set if the device should have the REMEMBER_ME_KEY
     */
    public static void setRemember(Context context, boolean remember) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(REMEMBER_ME_KEY, remember).apply();
    }
}
