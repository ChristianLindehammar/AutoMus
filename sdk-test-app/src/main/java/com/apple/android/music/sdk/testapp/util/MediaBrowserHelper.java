package com.apple.android.music.sdk.testapp.util;

import android.content.ComponentName;
import android.content.Context;
import androidx.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;

import com.apple.android.music.sdk.testapp.service.MediaPlaybackService;

/**
 * Copyright (C) 2017 Apple, Inc. All rights reserved.
 */

public final class MediaBrowserHelper extends MediaBrowserCompat.ConnectionCallback {

    public interface Listener {

        void onMediaBrowserConnected(@NonNull MediaBrowserCompat mediaBrowser);

    }

    private final Listener listener;
    private final MediaBrowserCompat mediaBrowser;


    public MediaBrowserHelper(@NonNull Context context, @NonNull Listener listener) {
        this.listener = listener;
        mediaBrowser = new MediaBrowserCompat(context, new ComponentName(context, MediaPlaybackService.class), this, null);
    }


    public void connect() {
        mediaBrowser.connect();
    }


    public void disconnect() {
        mediaBrowser.disconnect();
    }


    @Override
    public void onConnected() {
        listener.onMediaBrowserConnected(mediaBrowser);
    }


    @Override
    public void onConnectionSuspended() {
    }


    @Override
    public void onConnectionFailed() {
    }

}
