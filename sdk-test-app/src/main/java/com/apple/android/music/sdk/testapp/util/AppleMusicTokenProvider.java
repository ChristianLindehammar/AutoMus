package com.apple.android.music.sdk.testapp.util;

import android.content.Context;

import com.apple.android.music.sdk.testapp.R;
import com.apple.android.sdk.authentication.TokenProvider;

/**
 * Copyright (C) 2018 Apple, Inc. All rights reserved.
 */
public class AppleMusicTokenProvider implements TokenProvider {

    private final Context context;
    private final AppPreferences appPreferences;


    public AppleMusicTokenProvider(Context context) {
        this.context = context.getApplicationContext();
        appPreferences = AppPreferences.with(this.context);
    }

    @Override
    public String getDeveloperToken() {
        return context.getString(R.string.developer_token);
    }

    @Override
    public String getUserToken() {
        return appPreferences.getAppleMusicUserToken();
    }

}
