package com.lindehammarkonsult.automus.shared.auth

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import com.apple.android.music.MediaItem
import com.apple.android.music.playback.controller.MediaPlayerController
import com.apple.android.music.playback.controller.MediaPlayerControllerFactory
import com.apple.android.music.playback.model.MediaPlayerException
import com.apple.android.music.playback.model.PlaybackRepeatMode
import com.apple.android.music.playback.model.PlaybackShuffleMode
import com.apple.android.music.playback.model.PlaybackState
import com.apple.android.music.playback.model.PlayerQueueItem
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
        
        // Create the player controller using the factory
        playerController = MediaPlayerControllerFactory.createLocalController(
            context,
            handler,
            tokenProvider
        )
        
        // Register this class as a listener
        playerController.addListener(this)
        
        Log.d(TAG, "Initialized AppleMusicPlaybackManager")
    }
    
    /**
     * Play a track by ID
     */
    fun playTrack(trackId: String) {
        if (tokenProvider.getMusicUserToken() == null) {
            Log.e(TAG, "Cannot play track: User not authenticated")
            return
        }
        
        try {
            val songUrl = "music:/song/$trackId"
            playerController.prepareToPlay(songUrl)
            playerController.play()
            Log.d(TAG, "Playing track: $trackId")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing track: ${e.message}")
        }
    }
    
    /**
     * Play an album by ID
     */
    fun playAlbum(albumId: String) {
        if (tokenProvider.getMusicUserToken() == null) return
        
        try {
            val albumUrl = "music:/album/$albumId"
            playerController.prepareToPlay(albumUrl)
            playerController.play()
            Log.d(TAG, "Playing album: $albumId")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing album: ${e.message}")
        }
    }
    
    /**
     * Play a playlist by ID
     */
    fun playPlaylist(playlistId: String) {
        if (tokenProvider.getMusicUserToken() == null) return
        
        try {
            val playlistUrl = "music:/playlist/$playlistId"
            playerController.prepareToPlay(playlistUrl)
            playerController.play()
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
            if (playerController.playbackState == PlaybackState.PLAYING) {
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
            playerController.seekToPosition(positionMs / 1000.0) // Convert ms to seconds
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking to position: ${e.message}")
        }
    }
    
    /**
     * Set repeat mode
     * @param mode 0: off, 1: repeat one, 2: repeat all
     */
    fun setRepeatMode(mode: Int) {
        try {
            when (mode) {
                0 -> playerController.setRepeatMode(PlaybackRepeatMode.NONE)
                1 -> playerController.setRepeatMode(PlaybackRepeatMode.ONE)
                2 -> playerController.setRepeatMode(PlaybackRepeatMode.ALL)
                else -> playerController.setRepeatMode(PlaybackRepeatMode.NONE)
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
                if (shuffleEnabled) PlaybackShuffleMode.SONGS
                else PlaybackShuffleMode.OFF
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
            val playbackPosition = (playerController.playbackPosition * 1000).toLong()
            val isPlaying = playerController.playbackState == PlaybackState.PLAYING
            
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
        Log.d(TAG, "Current item changed: ${currentItem?.title ?: "null"}")
        
        // Update our playback state with the new track info
        currentItem?.let { item ->
            val currentTrack = Track(
                id = item.id ?: UUID.randomUUID().toString(),
                title = item.title ?: "Unknown",
                albumName = item.albumName ?: "Unknown",
                artistName = item.artistName ?: "Unknown",
                albumId = item.albumId ?: "",
                artistId = item.artistId ?: "",
                artworkUri = item.artworkUrl?.let { android.net.Uri.parse(it) },
                streamUrl = null, // Handled by SDK
                previewUrl = null,
                durationMs = (item.duration * 1000).toLong(),
                isExplicit = item.isExplicit ?: false
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
        Log.d(TAG, "Item ended: ${queueItem.title}")
    }

    override fun onMetadataUpdated(
        playerController: MediaPlayerController,
        currentItem: PlayerQueueItem
    ) {
        Log.d(TAG, "Metadata updated: ${currentItem.title}")
        
        // If needed, update our track information with the fresh metadata
    }

    override fun onPlaybackQueueChanged(
        playerController: MediaPlayerController,
        playbackQueueItems: MutableList<PlayerQueueItem>
    ) {
        Log.d(TAG, "Queue changed: ${playbackQueueItems.size} items")
        
        // Update our queue in the app state
        val tracks = playbackQueueItems.map { item ->
            Track(
                id = item.id ?: UUID.randomUUID().toString(),
                title = item.title ?: "Unknown",
                albumName = item.albumName ?: "Unknown",
                artistName = item.artistName ?: "Unknown",
                albumId = item.albumId ?: "",
                artistId = item.artistId ?: "",
                artworkUri = item.artworkUrl?.let { android.net.Uri.parse(it) },
                streamUrl = null,
                previewUrl = null,
                durationMs = (item.duration * 1000).toLong(),
                isExplicit = item.isExplicit ?: false
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
            PlaybackRepeatMode.NONE -> 0
            PlaybackRepeatMode.ONE -> 1
            PlaybackRepeatMode.ALL -> 2
            else -> 0
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
        
        val isShuffleOn = currentShuffleMode != PlaybackShuffleMode.OFF
        
        _playbackState.value = _playbackState.value.copy(
            shuffleMode = isShuffleOn
        )
    }
}