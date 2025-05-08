package com.apple.android.music.sdk.testapp.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.apple.android.music.sdk.testapp.R;

/**
 * Copyright (C) 2017 Apple, Inc. All rights reserved.
 */

public final class NowPlayingFragment extends BaseFragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, Handler.Callback {

    public static final String TAG = "NowPlayingFragment";
    private static final int MESSAGE_UPDATE_PROGRESS = 1;

    private MediaControllerCompat mediaController;
    private MediaControllerCallback mediaControllerCallback;
    private ImageView artworkImageView;
    private TextView titleTextView;
    private TextView subtitleTextView;
    private ImageView playButtonImageView;
    private ImageView previousButtonImageView;
    private ImageView nextButtonImageView;
    private SeekBar seekBar;
    private TextView elapsedTimeTextView;
    private TextView remainingTimeTextView;
    private StringBuilder timeStringBuilder;
    private boolean userSeeking;
    private Handler handler;
    private PlaybackStateCompat currentPlaybackState;

    public static NowPlayingFragment newInstance() {
        return new NowPlayingFragment();
    }


    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_UPDATE_PROGRESS:
                updatePosition();
                handler.sendEmptyMessageDelayed(MESSAGE_UPDATE_PROGRESS, 1_000);
                return true;
        }
        return false;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.player_play_button:
                handlePlayClick();
                break;
            case R.id.player_previous_button:
                mediaController.getTransportControls().skipToPrevious();
                break;
            case R.id.player_next_button:
                mediaController.getTransportControls().skipToNext();
                break;
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaControllerCallback = new MediaControllerCallback();
        userSeeking = false;
        timeStringBuilder = new StringBuilder();
        handler = new Handler(this);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_now_playing, container, false);
        artworkImageView = (ImageView) view.findViewById(R.id.player_artwork);
        titleTextView = (TextView) view.findViewById(R.id.player_title);
        subtitleTextView = (TextView) view.findViewById(R.id.player_subtitle);
        playButtonImageView = (ImageView) view.findViewById(R.id.player_play_button);
        playButtonImageView.setOnClickListener(this);
        previousButtonImageView = (ImageView) view.findViewById(R.id.player_previous_button);
        previousButtonImageView.setOnClickListener(this);
        nextButtonImageView = (ImageView) view.findViewById(R.id.player_next_button);
        nextButtonImageView.setOnClickListener(this);
        seekBar = (SeekBar) view.findViewById(R.id.player_seek_bar);
        seekBar.setOnSeekBarChangeListener(this);
        elapsedTimeTextView = (TextView) view.findViewById(R.id.player_time_elapsed);
        remainingTimeTextView = (TextView) view.findViewById(R.id.player_time_remaining);
        return view;
    }


    @Override
    public void onMediaBrowserConnected(MediaBrowserCompat mediaBrowser) {
        super.onMediaBrowserConnected(mediaBrowser);
        if (mediaController == null && getActivity() != null) {
            mediaController = MediaControllerCompat.getMediaController(getActivity());
            mediaController.registerCallback(mediaControllerCallback);
            mediaControllerCallback.onPlaybackStateChanged(mediaController.getPlaybackState());
            mediaControllerCallback.onMetadataChanged(mediaController.getMetadata());
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(getString(R.string.title_now_playing));
        mediaController = MediaControllerCompat.getMediaController(getActivity());
        if (mediaController != null) {
            mediaController.registerCallback(mediaControllerCallback);
            mediaControllerCallback.onPlaybackStateChanged(mediaController.getPlaybackState());
            mediaControllerCallback.onMetadataChanged(mediaController.getMetadata());
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        if (mediaController != null) {
            mediaController.unregisterCallback(mediaControllerCallback);
        }
        handler.removeCallbacksAndMessages(null);
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        DateUtils.formatElapsedTime(timeStringBuilder, progress / 1_000);
        elapsedTimeTextView.setText(timeStringBuilder.toString());
        DateUtils.formatElapsedTime(timeStringBuilder, (seekBar.getMax() - progress) / 1_000);
        timeStringBuilder.insert(0, '-');
        remainingTimeTextView.setText(timeStringBuilder.toString());
    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        userSeeking = true;
    }


    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        userSeeking = false;
        mediaController.getTransportControls().seekTo(seekBar.getProgress());
    }


    private void handlePlayClick() {
        if (mediaController != null) {
            switch (mediaController.getPlaybackState().getState()) {
                case PlaybackStateCompat.STATE_BUFFERING:
                case PlaybackStateCompat.STATE_CONNECTING:
                case PlaybackStateCompat.STATE_PLAYING:
                    mediaController.getTransportControls().pause();
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                case PlaybackStateCompat.STATE_PAUSED:
                    mediaController.getTransportControls().play();
                    break;
            }
        }
    }


    private void updatePosition() {
        if (currentPlaybackState == null || userSeeking) {
            return;
        }

        long currentPosition = currentPlaybackState.getPosition();
        if (currentPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            long timeDelta = SystemClock.elapsedRealtime() - currentPlaybackState.getLastPositionUpdateTime();
            currentPosition += timeDelta * currentPlaybackState.getPlaybackSpeed();
        }
        seekBar.setProgress((int)currentPosition);
    }


    private class MediaControllerCallback extends MediaControllerCompat.Callback {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (state == null) {
                return;
            }
            currentPlaybackState = state;
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PAUSED:
                case PlaybackStateCompat.STATE_STOPPED:
                    playButtonImageView.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_play));
                    break;
                default:
                    handler.sendEmptyMessageDelayed(MESSAGE_UPDATE_PROGRESS, 100);
                    playButtonImageView.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause));
                    break;
            }
        }


        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata != null) {
                final long duration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
                seekBar.setMax((int)duration);

                artworkImageView.setImageBitmap(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART));

                titleTextView.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
                subtitleTextView.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));

            }
        }


    }

}
