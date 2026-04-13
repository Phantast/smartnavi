package com.ilm.sandwich.tools;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility for writing SharedPreferences without AsyncTask.
 * Uses apply() which is already asynchronous.
 */
public class PreferencesHelper {

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    public static void putBoolean(Context context, String key, boolean value) {
        getPrefs(context).edit().putBoolean(key, value).apply();
    }

    public static void putString(Context context, String key, String value) {
        getPrefs(context).edit().putString(key, value).apply();
    }

    public static void putInt(Context context, String key, int value) {
        getPrefs(context).edit().putInt(key, value).apply();
    }

    public static String getString(Context context, String key, String defaultValue) {
        return getPrefs(context).getString(key, defaultValue);
    }

    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        return getPrefs(context).getBoolean(key, defaultValue);
    }

    public static int getInt(Context context, String key, int defaultValue) {
        return getPrefs(context).getInt(key, defaultValue);
    }
}
