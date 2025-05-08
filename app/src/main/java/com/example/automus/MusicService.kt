package com.example.automus

import android.content.Intent
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class MusicService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var playbackStateBuilder: PlaybackStateCompat.Builder
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var exoPlayer: SimpleExoPlayer

    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSessionCompat(this, "MusicService")
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setCallback(MediaSessionCallback())
        sessionToken = mediaSession.sessionToken

        playbackStateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP
            )
        mediaSession.setPlaybackState(playbackStateBuilder.build())

        exoPlayer = SimpleExoPlayer.Builder(this).build()
        exoPlayer.addListener(PlayerEventListener())
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        // Load media items here
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot("root", null)
    }

    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            mediaSession.isActive = true
            exoPlayer.playWhenReady = true
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        }

        override fun onPause() {
            exoPlayer.playWhenReady = false
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        }

        override fun onSkipToNext() {
            exoPlayer.next()
            updatePlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT)
        }

        override fun onSkipToPrevious() {
            exoPlayer.previous()
            updatePlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS)
        }

        override fun onStop() {
            exoPlayer.stop()
            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
            mediaSession.isActive = false
        }

        private fun updatePlaybackState(state: Int) {
            playbackStateBuilder.setState(state, exoPlayer.currentPosition, 1.0f)
            mediaSession.setPlaybackState(playbackStateBuilder.build())
        }
    }

    private inner class PlayerEventListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    if (playWhenReady) {
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    } else {
                        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                    }
                }
                Player.STATE_ENDED -> {
                    updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                }
            }
        }

        private fun updatePlaybackState(state: Int) {
            playbackStateBuilder.setState(state, exoPlayer.currentPosition, 1.0f)
            mediaSession.setPlaybackState(playbackStateBuilder.build())
        }
    }
}
