package com.example.automus

import android.content.Context
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class PlaybackManager(context: Context) {

    private val exoPlayer: SimpleExoPlayer = SimpleExoPlayer.Builder(context).build()

    init {
        exoPlayer.addListener(PlayerEventListener())
    }

    fun play(mediaSource: MediaSource) {
        exoPlayer.prepare(mediaSource)
        exoPlayer.playWhenReady = true
    }

    fun pause() {
        exoPlayer.playWhenReady = false
    }

    fun skipToNext() {
        exoPlayer.next()
    }

    fun skipToPrevious() {
        exoPlayer.previous()
    }

    fun addToQueue(mediaSource: MediaSource) {
        exoPlayer.addMediaSource(mediaSource)
    }

    fun removeFromQueue(index: Int) {
        exoPlayer.removeMediaSource(index)
    }

    private inner class PlayerEventListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            // Handle player state changes
        }
    }
}
