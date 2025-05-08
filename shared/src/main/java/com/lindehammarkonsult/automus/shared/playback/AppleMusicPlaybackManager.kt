package com.lindehammarkonsult.automus.shared.playback

import android.content.Context
import android.net.Uri
import android.util.Log
import com.apple.android.music.MusicKit
import com.apple.android.music.playback.*
import com.lindehammarkonsult.automus.shared.auth.AppleMusicTokenProvider
import com.lindehammarkonsult.automus.shared.model.PlaybackState
import com.lindehammarkonsult.automus.shared.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "AppleMusicPlaybackMgr"

/**
 * Manager class for Apple Music playback using the MusicKit SDK
 * Handles native playback operations through the SDK's MediaPlayback module
 */
class AppleMusicPlaybackManager(private val context: Context) {
    
    // MusicKit player reference
    private var musicPlayer: MusicPlayer? = null
    
    // State flow for observing playback state
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    // Token provider for authentication
    private val tokenProvider = AppleMusicTokenProvider(context)
    
    // Flag to track initialization status
    private var isInitialized = false
    
    /**
     * Initialize the playback manager
     */
    fun initialize() {
        if (isInitialized) {
            Log.d(TAG, "Already initialized")
            return
        }
        
        try {
            // Create a music player instance
            musicPlayer = MusicKit.getInstance().createMusicPlayer().apply {
                // Set up player callbacks
                setPlayerStateCallback(object : PlayerStateCallback {
                    override fun onPlayerStateRestored(playerState: PlayerState) {
                        Log.d(TAG, "Player state restored: $playerState")
                    }
                    
                    override fun onPlaybackStateChanged(state: PlaybackState) {
                        when (state) {
                            PlaybackState.PLAYING -> {
                                Log.d(TAG, "Playback state: PLAYING")
                                updatePlaybackState(isPlaying = true)
                            }
                            PlaybackState.PAUSED -> {
                                Log.d(TAG, "Playback state: PAUSED")
                                updatePlaybackState(isPlaying = false)
                            }
                            PlaybackState.STOPPED -> {
                                Log.d(TAG, "Playback state: STOPPED")
                                updatePlaybackState(isPlaying = false)
                            }
                            PlaybackState.INTERRUPTED -> {
                                Log.d(TAG, "Playback state: INTERRUPTED")
                                updatePlaybackState(isPlaying = false)
                            }
                            PlaybackState.SEEKING -> {
                                Log.d(TAG, "Playback state: SEEKING")
                                // Keep current state
                            }
                            PlaybackState.WAITING -> {
                                Log.d(TAG, "Playback state: WAITING")
                                // Keep current state, update buffering if needed
                            }
                            PlaybackState.COMPLETED -> {
                                Log.d(TAG, "Playback state: COMPLETED")
                                updatePlaybackState(isPlaying = false)
                            }
                            else -> {
                                Log.d(TAG, "Playback state: UNKNOWN")
                            }
                        }
                    }
                })
                
                // Set up item transition callback
                setQueueItemTransitionCallback(object : QueueItemTransitionCallback {
                    override fun onItemTransition(item: QueueItem?) {
                        if (item != null) {
                            Log.d(TAG, "Queue item transition: ${item.title}")
                            
                            // Update current track in state
                            val track = Track(
                                id = item.id,
                                title = item.title ?: "Unknown",
                                subtitle = item.artistName ?: "Unknown artist",
                                artistName = item.artistName ?: "Unknown artist",
                                albumName = item.albumName ?: "Unknown album",
                                durationMs = (item.duration * 1000).toLong(),
                                artworkUri = item.artwork?.let { Uri.parse(it.url) }
                            )
                            
                            updatePlaybackState(currentTrack = track)
                        } else {
                            Log.d(TAG, "Queue item transition: null")
                            updatePlaybackState(currentTrack = null)
                        }
                    }
                })
                
                // Set up playback progress callback
                setPlaybackProgressCallback(object : PlaybackProgressCallback {
                    override fun onUpdate(progress: Float) {
                        val currentPosition = (progress * getCurrentTrackDuration()).toLong()
                        updatePlaybackState(position = currentPosition)
                    }
                })
                
                // Set up general error callback
                setErrorCallback(object : ErrorCallback {
                    override fun onError(error: MusicPlayerError) {
                        Log.e(TAG, "Music player error: ${error.localizedMessage}")
                    }
                })
            }
            
            Log.d(TAG, "PlaybackManager initialized")
            isInitialized = true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing PlaybackManager: ${e.message}", e)
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        try {
            musicPlayer?.release()
            musicPlayer = null
            isInitialized = false
            Log.d(TAG, "PlaybackManager released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing PlaybackManager: ${e.message}", e)
        }
    }
    
    /**
     * Play a specific track by ID
     */
    fun playTrack(trackId: String) {
        ensureInitialized()
        
        try {
            val request = MusicPlayerRequest.createPlayRequest(trackId)
            musicPlayer?.play(request, object : MusicPlayerCallback {
                override fun onCompletion(result: MusicPlayerResult) {
                    if (result.isSuccess) {
                        Log.d(TAG, "Play track request successful: $trackId")
                    } else {
                        Log.e(TAG, "Play track request failed: ${result.error?.localizedMessage}")
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error playing track: ${e.message}", e)
        }
    }
    
    /**
     * Play a specific album by ID
     */
    fun playAlbum(albumId: String) {
        ensureInitialized()
        
        try {
            val request = MusicPlayerRequest.createPlayRequest(albumId, type = "albums")
            musicPlayer?.play(request, createDefaultCallback("album", albumId))
        } catch (e: Exception) {
            Log.e(TAG, "Error playing album: ${e.message}", e)
        }
    }
    
    /**
     * Play a specific playlist by ID
     */
    fun playPlaylist(playlistId: String) {
        ensureInitialized()
        
        try {
            val request = MusicPlayerRequest.createPlayRequest(playlistId, type = "playlists")
            musicPlayer?.play(request, createDefaultCallback("playlist", playlistId))
        } catch (e: Exception) {
            Log.e(TAG, "Error playing playlist: ${e.message}", e)
        }
    }
    
    /**
     * Resume playback
     */
    fun resume() {
        ensureInitialized()
        
        try {
            musicPlayer?.resume()
            Log.d(TAG, "Resume playback requested")
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming playback: ${e.message}", e)
        }
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        ensureInitialized()
        
        try {
            musicPlayer?.pause()
            Log.d(TAG, "Pause playback requested")
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing playback: ${e.message}", e)
        }
    }
    
    /**
     * Skip to the next track
     */
    fun skipToNext() {
        ensureInitialized()
        
        try {
            musicPlayer?.skipToNextItem(createDefaultCallback("skip-next", ""))
        } catch (e: Exception) {
            Log.e(TAG, "Error skipping to next track: ${e.message}", e)
        }
    }
    
    /**
     * Skip to the previous track
     */
    fun skipToPrevious() {
        ensureInitialized()
        
        try {
            musicPlayer?.skipToPreviousItem(createDefaultCallback("skip-previous", ""))
        } catch (e: Exception) {
            Log.e(TAG, "Error skipping to previous track: ${e.message}", e)
        }
    }
    
    /**
     * Seek to a specific position in the current track
     */
    fun seekTo(position: Long) {
        ensureInitialized()
        
        try {
            val durationMs = getCurrentTrackDuration()
            if (durationMs > 0) {
                val progress = position.toFloat() / durationMs
                musicPlayer?.seekToPosition(progress)
                Log.d(TAG, "Seek to position: $position ms (progress: $progress)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking to position: ${e.message}", e)
        }
    }
    
    /**
     * Set repeat mode
     */
    fun setRepeatMode(repeatMode: RepeatMode) {
        ensureInitialized()
        
        try {
            val nativeMode = when (repeatMode) {
                RepeatMode.NONE -> com.apple.android.music.playback.RepeatMode.NONE
                RepeatMode.ONE -> com.apple.android.music.playback.RepeatMode.ONE
                RepeatMode.ALL -> com.apple.android.music.playback.RepeatMode.ALL
            }
            musicPlayer?.setRepeatMode(nativeMode)
            Log.d(TAG, "Set repeat mode: $repeatMode")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting repeat mode: ${e.message}", e)
        }
    }
    
    /**
     * Set shuffle mode
     */
    fun setShuffleMode(shuffleEnabled: Boolean) {
        ensureInitialized()
        
        try {
            musicPlayer?.setShuffleMode(
                if (shuffleEnabled) 
                    ShuffleMode.SONGS 
                else 
                    ShuffleMode.OFF
            )
            Log.d(TAG, "Set shuffle mode: $shuffleEnabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting shuffle mode: ${e.message}", e)
        }
    }
    
    /**
     * Get the current queue of tracks
     */
    fun getQueue(): List<QueueItem> {
        ensureInitialized()
        
        return try {
            val queue = musicPlayer?.queue ?: emptyList()
            Log.d(TAG, "Queue contains ${queue.size} items")
            queue
        } catch (e: Exception) {
            Log.e(TAG, "Error getting queue: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get the current track's duration in milliseconds
     */
    private fun getCurrentTrackDuration(): Float {
        val currentItem = musicPlayer?.currentItem
        return currentItem?.duration ?: 0f
    }
    
    /**
     * Create a default callback for player operations
     */
    private fun createDefaultCallback(operation: String, id: String): MusicPlayerCallback {
        return object : MusicPlayerCallback {
            override fun onCompletion(result: MusicPlayerResult) {
                if (result.isSuccess) {
                    Log.d(TAG, "$operation operation successful: $id")
                } else {
                    Log.e(TAG, "$operation operation failed: ${result.error?.localizedMessage}")
                }
            }
        }
    }
    
    /**
     * Update the playback state with new values, maintaining existing ones if not specified
     */
    private fun updatePlaybackState(
        isPlaying: Boolean? = null,
        currentTrack: Track? = null,
        position: Long? = null
    ) {
        val currentState = _playbackState.value
        val newState = currentState.copy(
            isPlaying = isPlaying ?: currentState.isPlaying,
            currentTrack = currentTrack ?: currentState.currentTrack,
            position = position ?: currentState.position
        )
        _playbackState.value = newState
    }
    
    /**
     * Ensure the manager is initialized
     */
    private fun ensureInitialized() {
        if (!isInitialized || musicPlayer == null) {
            initialize()
        }
    }
    
    /**
     * Repeat mode for playback
     */
    enum class RepeatMode {
        NONE, ONE, ALL
    }
}