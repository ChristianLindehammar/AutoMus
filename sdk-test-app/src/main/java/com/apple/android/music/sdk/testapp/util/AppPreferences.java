package com.apple.android.music.sdk.testapp.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Copyright (C) 2018 Apple, Inc. All rights reserved.
 */
public class AppPreferences {

    private static final String PREFERENCES_FILE_NAME = "app_preferences";
    private static final String KEY_APPLE_MUSIC_USER_TOKEN = "apple-music-user-token";
    private static volatile AppPreferences instance;

    private SharedPreferences preferences;

    public static AppPreferences with(Context context) {
        if (instance == null) {
            synchronized (AppPreferences.class) {
                instance = new AppPreferences(context);
            }
        }
        return instance;
    }

    public String getAppleMusicUserToken() {
        return preferences.getString(KEY_APPLE_MUSIC_USER_TOKEN, null);
    }

    public void setAppleMusicUserToken(String userToken) {
        preferences.edit().putString(KEY_APPLE_MUSIC_USER_TOKEN, userToken).apply();
    }

    private AppPreferences(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
    }

}
