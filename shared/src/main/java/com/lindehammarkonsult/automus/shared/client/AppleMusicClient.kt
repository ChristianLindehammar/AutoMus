package com.lindehammarkonsult.automus.shared.client

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.SessionError
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.lindehammarkonsult.automus.shared.AppleMusicMediaService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "AppleMusicClient"

/**
 * Client class to interact with the AppleMusicMediaService using Media3 APIs.
 * This class provides an easy-to-use interface for UI components to browse and control
 * music playback using the Media3 service.
 */
class AppleMusicClient(private val context: Context) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    private lateinit var browserFuture: ListenableFuture<MediaBrowser>
    private var mediaBrowser: MediaBrowser? = null
    
    private val _mediaState = MutableStateFlow(MediaState())
    val mediaState: StateFlow<MediaState> = _mediaState.asStateFlow()
    
    private val _rootChildren = MutableStateFlow<List<MediaItem>>(emptyList())
    val rootChildren: StateFlow<List<MediaItem>> = _rootChildren.asStateFlow()
    
    private val _currentParentId = MutableStateFlow<String?>(null)
    val currentParentId: StateFlow<String?> = _currentParentId.asStateFlow()
    
    /**
     * Connect to the media service
     */
    @OptIn(UnstableApi::class)
    fun connect() {
        if (::browserFuture.isInitialized && !browserFuture.isDone) {
            return
        }
        
        val sessionToken = SessionToken(
            context,
            ComponentName(context, AppleMusicMediaService::class.java)
        )
        
        browserFuture = MediaBrowser.Builder(context, sessionToken)
            .buildAsync()
            
        browserFuture.addListener({
            try {
                mediaBrowser = browserFuture.get().also { browser ->
                    // Register callback to receive updates about player state changes
                    browser.addListener(playerListener)
                    
                    // Get the root children immediately after connecting
                    loadRootChildren()
                }
                
                Log.d(TAG, "MediaBrowser connected successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to MediaBrowser", e)
            }
        }, MoreExecutors.directExecutor())
    }
    
    /**
     * Disconnect from the media service
     */
    fun disconnect() {
        MediaController.releaseFuture(browserFuture)
        mediaBrowser = null
    }
    
    /**
     * Load the children of the root node
     */
    fun loadRootChildren() {
        mediaBrowser?.let { browser ->
            coroutineScope.launch {
                val children = withContext(Dispatchers.IO) {
                    val rootId = browser.getLibraryRoot(null).get().value?.mediaId ?: "root"
                    _currentParentId.value = rootId
                    
                    try {
                        getChildren(rootId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading root children", e)
                        emptyList()
                    }
                }
                
                _rootChildren.value = children
            }
        }
    }
    
    /**
     * Get the children of a media item by its ID
     */
    suspend fun getChildren(parentId: String): List<MediaItem> {
        return withContext(Dispatchers.IO) {
            mediaBrowser?.let { browser ->
                try {
                    val childrenFuture = browser.getChildren(
                        parentId,
                        0,  // start index
                        Int.MAX_VALUE,  // page size
                        /* params= */ null
                    )
                    
                    val childrenResult = childrenFuture.get()
                    if (childrenResult.resultCode == LibraryResult.RESULT_SUCCESS) {
                        _currentParentId.value = parentId
                        childrenResult.value ?: emptyList()
                    } else {
                        Log.e(TAG, "Error getting children: ${childrenResult.resultCode}")
                        emptyList()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting children for $parentId", e)
                    emptyList()
                }
            } ?: emptyList()
        }
    }
    
    /**
     * Play a media item
     */
    fun play(mediaItem: MediaItem) {
        mediaBrowser?.let { browser ->
            browser.setMediaItem(mediaItem)
            browser.prepare()
            browser.play()
        }
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        mediaBrowser?.pause()
    }
    
    /**
     * Resume playback
     */
    fun resume() {
        mediaBrowser?.play()
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
     * Seek to position
     */
    fun seekTo(positionMs: Long) {
        mediaBrowser?.seekTo(positionMs)
    }
    
    /**
     * Perform a search
     */
    suspend fun search(query: String): List<MediaItem> {
        return withContext(Dispatchers.IO) {
            mediaBrowser?.let { browser ->
                try {
                    // First search to notify the service
                    val searchFuture = browser.search(query, /* params= */ null)
                    val searchResult = searchFuture.get()
                    
                    if (searchResult.resultCode == LibraryResult.RESULT_SUCCESS) {
                        // Then get the children of the search results
                        val searchMediaId = "search_$query"
                        getChildren(searchMediaId)
                    } else {
                        Log.e(TAG, "Error searching: ${searchResult.resultCode}")
                        emptyList()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error searching for $query", e)
                    emptyList()
                }
            } ?: emptyList()
        }
    }
    
    /**
     * Send a custom command to the media service
     */
    @OptIn(UnstableApi::class)
    fun sendCustomCommand(command: String, args: Bundle = Bundle()): ListenableFuture<androidx.media3.session.SessionResult> {
        return mediaBrowser?.sendCustomCommand(
            androidx.media3.session.SessionCommand(command, Bundle()),
            args
        ) ?: Futures.immediateFuture(
            androidx.media3.session.SessionResult(
                SessionError.ERROR_SESSION_DISCONNECTED
            )
        )
    }
    
    /**
     * Player listener to receive updates about player state changes
     */
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _mediaState.update { it.copy(isPlaying = isPlaying) }
        }
        
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let {
                _mediaState.update { state ->
                    state.copy(currentMediaItem = mediaItem)
                }
            }
        }
        
        override fun onPlaybackStateChanged(playbackState: Int) {
            _mediaState.update { it.copy(playbackState = playbackState) }
        }
    }
    
    /**
     * Data class representing the current state of media playback
     */
    data class MediaState(
        val isPlaying: Boolean = false,
        val playbackState: Int = Player.STATE_IDLE,
        val currentMediaItem: MediaItem? = null
    )
}
