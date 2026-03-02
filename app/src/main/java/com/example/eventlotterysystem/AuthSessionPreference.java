package com.example.eventlotterysystem;

import android.content.Context;
import android.content.SharedPreferences;

public final class AuthSessionPreference {

    private static final String PREFERENCES_NAME = "event_lottery_preferences";
    private static final String REMEMBER_ME_KEY = "remember_me";

    public static boolean shouldRemember(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(REMEMBER_ME_KEY, true);
    }

    public static void setRemember(Context context, boolean remember) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(REMEMBER_ME_KEY, remember).apply();
    }
}
