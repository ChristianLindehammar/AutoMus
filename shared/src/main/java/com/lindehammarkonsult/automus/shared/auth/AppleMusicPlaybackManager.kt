package com.lindehammarkonsult.automus.shared.auth

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import com.apple.android.music.playback.controller.MediaPlayerController
import com.apple.android.music.playback.controller.MediaPlayerControllerFactory
import com.apple.android.music.playback.model.MediaPlayerException
import com.apple.android.music.playback.model.PlaybackRepeatMode
import com.apple.android.music.playback.model.PlaybackShuffleMode
import com.apple.android.music.playback.model.PlaybackState
import com.apple.android.music.playback.model.PlayerQueueItem
import com.lindehammarkonsult.automus.shared.model.RepeatMode
import com.lindehammarkonsult.automus.shared.model.PlaybackState as AppPlaybackState
import com.lindehammarkonsult.automus.shared.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

private const val TAG = "AppleMusicPlaybackMgr"

/**
 * Manager class for handling Apple Music playback using the MusicKit SDK
 */
class AppleMusicPlaybackManager(
    context: Context,
    private val tokenProvider: AppleMusicTokenProvider
) : MediaPlayerController.Listener {

    // Background thread for handling playback operations
    private val handlerThread: HandlerThread
    private val handler: Handler
    
    // MusicKit playback components
    private val playerController: MediaPlayerController
    
    // Playback State Flow
    private val _playbackState = MutableStateFlow(AppPlaybackState())
    val playbackState: StateFlow<AppPlaybackState> = _playbackState
    
    // Flag to track if we're running with valid authentication
    private var hasValidAuthentication = false
    
    // Native libraries required by the SDK
    companion object {
        init {
            try {
                // Adding these lines prevents OOM false alarms
                System.setProperty("org.bytedeco.javacpp.maxphysicalbytes", "0")
                System.setProperty("org.bytedeco.javacpp.maxbytes", "0")
                
                System.loadLibrary("c++_shared")
                System.loadLibrary("appleMusicSDK")
                Log.d(TAG, "Loaded native libraries successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load native libraries: ${e.message}", e)
                throw e
            }
        }
    }
    
    init {
        // Create background thread for SDK operations
        handlerThread = HandlerThread("AppleMusicPlaybackManager", Process.THREAD_PRIORITY_BACKGROUND)
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        
        // Check if we have valid tokens
        // Use the nullable version of the developer token getter
        val devToken = tokenProvider.getDeveloperTokenOrNull()
        hasValidAuthentication = devToken != null && 
            devToken != "YOUR_APPLE_MUSIC_DEVELOPER_TOKEN_HERE" && 
            devToken.isNotBlank()
        
        // Create the player controller using the factory
        playerController = try {
            MediaPlayerControllerFactory.createLocalController(
                context,
                handler,
                tokenProvider
            ).also {
                // Register this class as a listener
                it.addListener(this)
                Log.d(TAG, "Initialized AppleMusicPlaybackManager with valid token")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating MediaPlayerController: ${e.message}")
            // Create a dummy controller for safety - in a real app, you might handle this differently
            // Here we're just making sure the app doesn't crash
            MediaPlayerControllerFactory.createLocalController(
                context,
                handler,
                EmergencyTokenProvider()
            ).also {
                hasValidAuthentication = false
                Log.w(TAG, "Created fallback MediaPlayerController due to initialization error")
            }
        }
    }
    
    /**
     * Play a track by ID
     */
    fun playTrack(trackId: String) {
        // Cast to avoid overload resolution ambiguity
        val userToken: String? = tokenProvider.userToken
        if (!hasValidAuthentication || userToken == null) {
            Log.e(TAG, "Cannot play track: User not authenticated or missing valid token")
            return
        }
        
        try {
            // Create catalog playback queue item provider
            val songUrl = "music:/song/$trackId"
            val queueProvider = com.apple.android.music.playback.queue.CatalogPlaybackQueueItemProvider.Builder()
                .items(com.apple.android.music.playback.model.MediaItemType.SONG, trackId)
                .build()
            
            // Prepare and play
            playerController.prepare(queueProvider, true)  // true means play when ready
            Log.d(TAG, "Playing track: $trackId")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing track: ${e.message}")
        }
    }
    
    // Simple emergency token provider that returns empty tokens to prevent crashes
    private inner class EmergencyTokenProvider : com.apple.android.sdk.authentication.TokenProvider {
        override fun getDeveloperToken(): String = ""
        
        override fun getUserToken(): String? = null  // Changed from getMusicUserToken to getUserToken
    }
    
    /**
     * Play an album by ID
     */
    fun playAlbum(albumId: String) {
        // Cast to avoid overload resolution ambiguity
        val userToken: String? = tokenProvider.getUserToken()
        if (userToken == null) return
        
        try {
            // Create catalog playback queue item provider for album
            val queueProvider = com.apple.android.music.playback.queue.CatalogPlaybackQueueItemProvider.Builder()
                .containers(com.apple.android.music.playback.model.MediaContainerType.ALBUM, albumId)
                .build()
            
            // Prepare and play
            playerController.prepare(queueProvider, true)
            Log.d(TAG, "Playing album: $albumId")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing album: ${e.message}")
        }
    }
    
    /**
     * Play a playlist by ID
     */
    fun playPlaylist(playlistId: String) {
        // Cast to avoid overload resolution ambiguity
        val userToken: String? = tokenProvider.getUserToken()
        if (userToken == null) return
        
        try {
            // Create catalog playback queue item provider for playlist
            val queueProvider = com.apple.android.music.playback.queue.CatalogPlaybackQueueItemProvider.Builder()
                .containers(com.apple.android.music.playback.model.MediaContainerType.PLAYLIST, playlistId)
                .build()
            
            // Prepare and play
            playerController.prepare(queueProvider, true)
            Log.d(TAG, "Playing playlist: $playlistId")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing playlist: ${e.message}")
        }
    }
    
    /**
     * Play or pause the current playback
     */
    fun togglePlayPause() {
        try {
            if (playerController.getPlaybackState() == PlaybackState.PLAYING) {
                playerController.pause()
            } else {
                playerController.play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling play/pause: ${e.message}")
        }
    }
    
    /**
     * Skip to the next track
     */
    fun skipToNext() {
        try {
            playerController.skipToNextItem()
            
            // Update playback state in our repository
            updatePlaybackState()
        } catch (e: Exception) {
            Log.e(TAG, "Error skipping to next: ${e.message}")
        }
    }
    
    /**
     * Skip to the previous track
     */
    fun skipToPrevious() {
        try {
            playerController.skipToPreviousItem()
            
            // Update playback state in our repository
            updatePlaybackState()
        } catch (e: Exception) {
            Log.e(TAG, "Error skipping to previous: ${e.message}")
        }
    }
    
    /**
     * Seek to a position in the current track
     */
    fun seekTo(positionMs: Long) {
        try {
            playerController.seekToPosition(positionMs) // SDK expects milliseconds
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking to position: ${e.message}")
        }
    }
    
    /**
     * Set repeat mode
     * @param mode 0: off, 1: repeat one, 2: repeat all
     */
    fun setRepeatMode(mode: RepeatMode) {
        try {
            when (mode) {
                RepeatMode.NONE -> playerController.setRepeatMode(PlaybackRepeatMode.REPEAT_MODE_OFF) // NONE
                RepeatMode.ONE -> playerController.setRepeatMode(PlaybackRepeatMode.REPEAT_MODE_ONE) // ONE
                RepeatMode.ALL -> playerController.setRepeatMode(PlaybackRepeatMode.REPEAT_MODE_ALL) // ALL
                else -> playerController.setRepeatMode(PlaybackRepeatMode.REPEAT_MODE_OFF)
            }
            
            // Update playback state
            _playbackState.value = _playbackState.value.copy(
                repeatMode = mode
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting repeat mode: ${e.message}")
        }
    }
    
    /**
     * Set shuffle mode
     */
    fun setShuffleMode(shuffleEnabled: Boolean) {
        try {
            playerController.setShuffleMode(
                if (shuffleEnabled) PlaybackShuffleMode.SHUFFLE_MODE_SONGS // SONGS mode
                else PlaybackShuffleMode.SHUFFLE_MODE_OFF // OFF mode
            )
            
            // Update playback state
            _playbackState.value = _playbackState.value.copy(
                shuffleMode = shuffleEnabled
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting shuffle mode: ${e.message}")
        }
    }
    
    /**
     * Update the playback state with the latest information
     */
    private fun updatePlaybackState() {
        try {
            val playbackPosition = playerController.getCurrentPosition()
            val isPlaying = playerController.getPlaybackState() == PlaybackState.PLAYING
            
            _playbackState.value = _playbackState.value.copy(
                isPlaying = isPlaying,
                position = playbackPosition
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error updating playback state: ${e.message}")
        }
    }
    
    /**
     * Release resources when no longer needed
     */
    fun release() {
        try {
            playerController.removeListener(this)
            playerController.release()
            handlerThread.quitSafely()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources: ${e.message}")
        }
    }

    //
    // MediaPlayerController.Listener Implementation
    //
    
    override fun onPlayerStateRestored(playerController: MediaPlayerController) {
        Log.d(TAG, "Player state restored")
    }

    override fun onPlaybackStateChanged(
        playerController: MediaPlayerController,
        previousState: Int,
        currentState: Int
    ) {
        Log.d(TAG, "Playback state changed: $previousState -> $currentState")
        updatePlaybackState()
        
        // No need for notification handling here as it's handled in the service
    }

    override fun onPlaybackStateUpdated(playerController: MediaPlayerController) {
        updatePlaybackState()
    }

    override fun onBufferingStateChanged(
        playerController: MediaPlayerController,
        isBuffering: Boolean
    ) {
        Log.d(TAG, "Buffering state changed: $isBuffering")
        // We could update a buffering indicator in the UI
    }

    override fun onCurrentItemChanged(
        playerController: MediaPlayerController,
        previousItem: PlayerQueueItem?,
        currentItem: PlayerQueueItem?
    ) {
        Log.d(TAG, "Current item changed: ${currentItem?.getItem()?.getTitle() ?: "null"}")
        
        // Update our playback state with the new track info
        currentItem?.let { item ->
            val mediaItem = item.item // Get the PlayerMediaItem from PlayerQueueItem
            val currentTrack = Track(
                id = mediaItem.subscriptionStoreId ?: UUID.randomUUID().toString(),
                title = mediaItem.title ?: "Unknown",
                albumName = mediaItem.albumTitle ?: "Unknown",
                artistName = mediaItem.artistName ?: "Unknown",
                albumId = mediaItem.albumSubscriptionStoreId ?: "",
                artistId = mediaItem.artistSubscriptionStoreId ?: "",
                artworkUri = mediaItem.getArtworkUrl(300, 300)?.let { android.net.Uri.parse(it) },
                streamUrl = null, // Handled by SDK
                previewUrl = null,
                durationMs = mediaItem.duration,
                isExplicit = mediaItem.isExplicitContent,
                subtitle = "${mediaItem.artistName ?: ""} • ${mediaItem.albumTitle ?: ""}" // Construct subtitle from artist and album
            )
            
            _playbackState.value = _playbackState.value.copy(
                currentTrack = currentTrack
            )
        }
    }

    override fun onItemEnded(
        playerController: MediaPlayerController,
        queueItem: PlayerQueueItem,
        endPosition: Long
    ) {
        Log.d(TAG, "Item ended: ${queueItem.item.title}")
    }

    override fun onMetadataUpdated(
        playerController: MediaPlayerController,
        currentItem: PlayerQueueItem
    ) {
        Log.d(TAG, "Metadata updated: ${currentItem.item.title}")
        
        // If needed, update our track information with the fresh metadata
    }

    override fun onPlaybackQueueChanged(
        playerController: MediaPlayerController,
        playbackQueueItems: MutableList<PlayerQueueItem>
    ) {
        Log.d(TAG, "Queue changed: ${playbackQueueItems.size} items")
        
        // Update our queue in the app state
        val tracks = playbackQueueItems.map { item ->
            val mediaItem = item.getItem() // Get the PlayerMediaItem from PlayerQueueItem
            Track(
                id = mediaItem.subscriptionStoreId ?: UUID.randomUUID().toString(),
                title = mediaItem.title ?: "Unknown",
                albumName = mediaItem.albumTitle ?: "Unknown",
                artistName = mediaItem.artistName ?: "Unknown",
                albumId = mediaItem.albumSubscriptionStoreId ?: "",
                artistId = mediaItem.artistSubscriptionStoreId ?: "",
                artworkUri = mediaItem.getArtworkUrl(300, 300)?.let { android.net.Uri.parse(it) },
                streamUrl = null,
                previewUrl = null,
                durationMs = mediaItem.duration,
                isExplicit = mediaItem.isExplicitContent,
                subtitle = "${mediaItem.artistName ?: ""} • ${mediaItem.albumTitle ?: ""}" // Construct subtitle from artist and album
            )
        }
        
        _playbackState.value = _playbackState.value.copy(
            queue = tracks
        )
    }

    override fun onPlaybackQueueItemsAdded(
        playerController: MediaPlayerController, 
        queueInsertionType: Int,
        containerType: Int, 
        itemType: Int
    ) {
        Log.d(TAG, "Queue items added")
    }

    override fun onPlaybackError(
        playerController: MediaPlayerController,
        error: MediaPlayerException
    ) {
        Log.e(TAG, "Playback error: ${error.message}")
        // We could show an error message to the user here
    }

    override fun onPlaybackRepeatModeChanged(
        playerController: MediaPlayerController,
        currentRepeatMode: Int
    ) {
        Log.d(TAG, "Repeat mode changed: $currentRepeatMode")
        
        // Map the SDK repeat mode to our app's repeat mode
        val appRepeatMode = when (currentRepeatMode) {
            PlaybackRepeatMode.REPEAT_MODE_OFF -> RepeatMode.NONE
            PlaybackRepeatMode.REPEAT_MODE_ONE -> RepeatMode.ONE
            PlaybackRepeatMode.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.NONE
        }
        
        _playbackState.value = _playbackState.value.copy(
            repeatMode = appRepeatMode
        )
    }

    override fun onPlaybackShuffleModeChanged(
        playerController: MediaPlayerController,
        currentShuffleMode: Int
    ) {
        Log.d(TAG, "Shuffle mode changed: $currentShuffleMode")
        
        val isShuffleOn = currentShuffleMode != PlaybackShuffleMode.SHUFFLE_MODE_OFF
        
        _playbackState.value = _playbackState.value.copy(
            shuffleMode = isShuffleOn
        )
    }
}