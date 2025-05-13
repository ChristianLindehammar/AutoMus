package com.lindehammarkonsult.automus.shared.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.lindehammarkonsult.automus.shared.client.AppleMusicClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing media browsing and playback through the Media3 client.
 * UI components can observe the state flows and trigger actions through this ViewModel.
 */
class Media3ViewModel(application: Application) : AndroidViewModel(application) {

    private val client = AppleMusicClient(application)
    
    // Flow for the current media items being displayed
    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()
    
    // Flow for the current browsing path
    private val _browsingPath = MutableStateFlow<List<BrowsingPathItem>>(emptyList())
    val browsingPath: StateFlow<List<BrowsingPathItem>> = _browsingPath.asStateFlow()
    
    // Flow for the current playback state
    val playbackState = client.mediaState
    
    // Flow for whether the client is connected
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    init {
        // Connect to the media service when the ViewModel is created
        connectToService()
        
        // Observe root children
        viewModelScope.launch {
            client.rootChildren.collectLatest { rootItems ->
                if (client.currentParentId.value == _browsingPath.value.lastOrNull()?.id) {
                    _mediaItems.value = rootItems
                }
            }
        }
        
        // Observe the current parent ID
        viewModelScope.launch {
            client.currentParentId.collectLatest { parentId ->
                parentId?.let { id ->
                    val currentPath = _browsingPath.value
                    if (currentPath.isEmpty() || currentPath.last().id != id) {
                        // Update browsing path if not already at this location
                        updateBrowsingPath(id)
                    }
                }
            }
        }
    }
    
    /**
     * Connect to the Media3 service
     */
    fun connectToService() {
        client.connect()
        _isConnected.value = true
    }
    
    /**
     * Disconnect from the Media3 service
     */
    fun disconnectFromService() {
        client.disconnect()
        _isConnected.value = false
    }
    
    /**
     * Browse to a specific media item
     */
    fun browseToItem(item: MediaItem) {
        viewModelScope.launch {
            if (item.mediaMetadata.isPlayable == true) {
                // If the item is playable, play it
                playMediaItem(item)
            } else if (item.mediaMetadata.isBrowsable == true) {
                // If the item is browsable, browse to it
                val children = withContext(Dispatchers.IO) {
                    client.getChildren(item.mediaId)
                }
                _mediaItems.value = children
                
                // Add to browsing path
                addToBrowsingPath(item)
            }
        }
    }
    
    /**
     * Navigate back in the browsing hierarchy
     */
    fun navigateUp(): Boolean {
        val currentPath = _browsingPath.value
        if (currentPath.size <= 1) {
            // Already at root, can't go back further
            return false
        }
        
        // Remove the last item from the path
        val newPath = currentPath.dropLast(1)
        _browsingPath.value = newPath
        
        // Load the items for the new parent
        val parentId = newPath.lastOrNull()?.id ?: "root"
        viewModelScope.launch {
            val children = withContext(Dispatchers.IO) {
                client.getChildren(parentId)
            }
            _mediaItems.value = children
        }
        
        return true
    }
    
    /**
     * Play a media item
     */
    fun playMediaItem(mediaItem: MediaItem) {
        client.play(mediaItem)
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        val isPlaying = playbackState.value.isPlaying
        if (isPlaying) {
            client.pause()
        } else {
            client.resume()
        }
    }
    
    /**
     * Skip to next track
     */
    fun skipToNext() {
        client.skipToNext()
    }
    
    /**
     * Skip to previous track
     */
    fun skipToPrevious() {
        client.skipToPrevious()
    }
    
    /**
     * Search for media items
     */
    fun search(query: String) {
        if (query.isBlank()) {
            return
        }
        
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                client.search(query)
            }
            _mediaItems.value = results
            
            // Update browsing path to show we're in search results
            _browsingPath.value = listOf(
                BrowsingPathItem("root", "Home"),
                BrowsingPathItem("search_$query", "Search: $query")
            )
        }
    }
    
    /**
     * Refresh the current media items
     */
    fun refreshMediaItems() {
        val currentParentId = _browsingPath.value.lastOrNull()?.id ?: "root"
        viewModelScope.launch {
            val children = withContext(Dispatchers.IO) {
                client.getChildren(currentParentId)
            }
            _mediaItems.value = children
        }
    }
    
    /**
     * Update the browsing path based on the current parent ID
     */
    private fun updateBrowsingPath(parentId: String) {
        // This is a simple implementation - in a real app, you might want to look up 
        // the actual title for the parent ID from your repository
        val title = when {
            parentId == "root" -> "Home"
            parentId.startsWith("playlist_") -> "Playlist"
            parentId.startsWith("album_") -> "Album"
            parentId.startsWith("artist_") -> "Artist"
            parentId.startsWith("genre_") -> "Genre"
            else -> parentId
        }
        
        _browsingPath.value = listOf(BrowsingPathItem(parentId, title))
    }
    
    /**
     * Add an item to the browsing path
     */
    private fun addToBrowsingPath(item: MediaItem) {
        _browsingPath.update { currentPath ->
            if (currentPath.any { it.id == item.mediaId }) {
                // If this item is already in the path, truncate the path to this item
                currentPath.takeWhile { it.id != item.mediaId } + BrowsingPathItem(
                    item.mediaId,
                    item.mediaMetadata.title?.toString() ?: "Unknown"
                )
            } else {
                // Otherwise, add it to the current path
                currentPath + BrowsingPathItem(
                    item.mediaId,
                    item.mediaMetadata.title?.toString() ?: "Unknown"
                )
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Disconnect from the media service when the ViewModel is cleared
        disconnectFromService()
    }
    
    /**
     * Data class representing an item in the browsing path
     */
    data class BrowsingPathItem(
        val id: String,
        val title: String
    )
}
