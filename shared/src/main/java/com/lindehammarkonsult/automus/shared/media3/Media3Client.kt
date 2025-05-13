package com.lindehammarkonsult.automus.shared.media3

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.lindehammarkonsult.automus.shared.AppleMusicMediaService
import com.lindehammarkonsult.automus.shared.utils.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "Media3Client"

/**
 * Helper class for interacting with the Media3 library service.
 * This class handles connecting to the media service and provides
 * methods for browsing and controlling playback.
 */
class Media3Client(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private var controllerFuture: ListenableFuture<MediaBrowser>? = null
    private var mediaBrowser: MediaBrowser? = null

    // Expose media items and connection state as flows
    private val _mediaItems = MutableStateFlow<Map<String, List<MediaItem>>>(emptyMap())
    val mediaItems: StateFlow<Map<String, List<MediaItem>>> = _mediaItems.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentMedia = MutableStateFlow<MediaItem?>(null)
    val currentMedia: StateFlow<MediaItem?> = _currentMedia.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentMedia.value = mediaItem
            _duration.value = mediaBrowser?.duration ?: 0L
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            _position.value = newPosition.positionMs
        }
    }

    /**
     * Connect to the media service
     */
    fun connect(lifecycleOwner: LifecycleOwner) {
        if (controllerFuture?.isDone == true && mediaBrowser != null) {
            return // Already connected
        }

        val sessionToken = SessionToken(
            context,
            ComponentName(context, AppleMusicMediaService::class.java)
        )
        
        // Build a new media browser
        controllerFuture = MediaBrowser.Builder(context, sessionToken)
            .buildAsync()
        
        controllerFuture?.addListener({
            try {
                mediaBrowser = controllerFuture?.get()?.apply {
                    addListener(playerListener)
                    
                    // Initialize state
                    _currentMedia.value = currentMediaItem
                    _isPlaying.value = isPlaying
                    _duration.value = duration
                    _position.value = currentPosition
                }
                
                _isConnected.value = true
                
                // Immediately load the root
                lifecycleOwner.lifecycleScope.launch {
                    loadChildren("root")
                }
                
                Log.d(TAG, "Connected to media service")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to media service", e)
                _isConnected.value = false
            }
        }, MoreExecutors.directExecutor())
    }

    /**
     * Disconnect from the media service
     */
    fun disconnect() {
        mediaBrowser?.removeListener(playerListener)
        controllerFuture?.let { future ->
            MediaController.releaseFuture(future)
        }
        mediaBrowser = null
        controllerFuture = null
        _isConnected.value = false
    }

    /**
     * Load media items for a given parent ID
     */
    suspend fun loadChildren(parentId: String): List<MediaItem> {
        val browser = mediaBrowser ?: return emptyList()
        
        return try {
            withTimeout(5000) {
                val children = browser.getChildren(parentId, 0, 50, null).await()
                val items = children.value ?: ImmutableList.of<MediaItem>()
                
                // Update the media items flow
                _mediaItems.update { currentMap ->
                    currentMap + (parentId to items)
                }
                
                items
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading children for $parentId", e)
            emptyList()
        }
    }

    /**
     * Play a media item
     */
    fun playMediaItem(mediaItem: MediaItem) {
        mediaBrowser?.let { browser ->
            // For browsable items, we need to get their children first
            if (mediaItem.mediaMetadata.isBrowsable == true) {
                coroutineScope.launch {
                    val children = loadChildren(mediaItem.mediaId)
                    if (children.isNotEmpty()) {
                        // Play the first item in the browsable container
                        browser.setMediaItems(children)
                        browser.prepare()
                        browser.play()
                    }
                }
            } else {
                // For playable items, play directly
                browser.setMediaItem(mediaItem)
                browser.prepare()
                browser.play()
            }
        }
    }

    /**
     * Search the media library
     */
    suspend fun search(query: String): List<MediaItem> {
        val browser = mediaBrowser ?: return emptyList()
        
        return try {
            withTimeout(5000) {
                browser.search(query, null).await()
                // After the search completes, query for the search results
                loadChildren("search_$query")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching for $query", e)
            emptyList()
        }
    }

    /**
     * Play/Pause toggle
     */
    fun playPause() {
        mediaBrowser?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    /**
     * Skip to next track
     */
    fun skipToNext() {
        mediaBrowser?.seekToNext()
    }

    /**
     * Skip to previous track
     */
    fun skipToPrevious() {
        mediaBrowser?.seekToPrevious()
    }

    /**
     * Seek to a position
     */
    fun seekTo(positionMs: Long) {
        mediaBrowser?.seekTo(positionMs)
    }

    /**
     * Get the current position
     */
    fun getCurrentPosition(): Long {
        return mediaBrowser?.currentPosition ?: 0L
    }

    /**
     * Send a custom command to the service
     */
    suspend fun sendCustomCommand(action: String, args: Bundle = Bundle()): Bundle? {
        val browser = mediaBrowser ?: return null
        
        return try {
            withTimeout(5000) {
                val command = SessionCommand(action, Bundle())
                val result = browser.sendCustomCommand(command, args).await()
                // Return the extras bundle from the SessionResult
                result.extras
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending custom command $action", e)
            null
        }
    }

    /**
     * Check if we're authenticated with Apple Music
     */
    suspend fun isAuthenticated(): Boolean {
        val result = sendCustomCommand("com.lindehammarkonsult.automus.action.CHECK_AUTH")
        return result?.getBoolean("authenticated", false) ?: false
    }
}
