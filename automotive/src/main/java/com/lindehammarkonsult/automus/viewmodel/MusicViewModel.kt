package com.lindehammarkonsult.automus.viewmodel

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MusicViewModel : ViewModel() {
    
    // Connection state
    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected
    
    // Current playback state
    private val _playbackState = MutableLiveData<PlaybackStateCompat>()
    val playbackState: LiveData<PlaybackStateCompat> = _playbackState
    
    // Current media metadata
    private val _metadata = MutableLiveData<MediaMetadataCompat>()
    val metadata: LiveData<MediaMetadataCompat> = _metadata
    
    // Current media items (for lists)
    private val _mediaItems = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
    val mediaItems: LiveData<List<MediaBrowserCompat.MediaItem>> = _mediaItems

    // New LiveData for categorized library content
    private val _playlists = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
    val playlists: LiveData<List<MediaBrowserCompat.MediaItem>> = _playlists

    private val _likedSongs = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
    val likedSongs: LiveData<List<MediaBrowserCompat.MediaItem>> = _likedSongs

    private val _recentlyPlayedItems = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
    val recentlyPlayedItems: LiveData<List<MediaBrowserCompat.MediaItem>> = _recentlyPlayedItems
    
    // Search results
    private val _searchResults = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
    val searchResults: LiveData<List<MediaBrowserCompat.MediaItem>> = _searchResults
    
    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Setters
    fun setIsConnected(connected: Boolean) {
        _isConnected.value = connected
    }
    
    fun setPlaybackState(state: PlaybackStateCompat?) {
        _playbackState.value = state
    }
    
    fun setMetadata(metadata: MediaMetadataCompat?) {
        _metadata.value = metadata
    }
    
    fun setMediaItems(items: List<MediaBrowserCompat.MediaItem>) {
        _mediaItems.value = items
    }

    fun setSearchResults(results: List<MediaBrowserCompat.MediaItem>) {
        _searchResults.value = results
    }
    
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    // Methods to fetch library data (simulated)
    fun fetchPlaylists() {
        // In a real app, this would subscribe to a media ID via MediaBrowser
        _playlists.postValue(emptyList()) // Simulate clearing old data / initial empty state
        // Data would be populated by MediaBrowserCompat.SubscriptionCallback
    }

    fun fetchLikedSongs() {
        _likedSongs.postValue(emptyList())
    }

    fun fetchRecentlyPlayedItems() {
        _recentlyPlayedItems.postValue(emptyList())
    }

    fun fetchAllLibraryData() {
        setLoading(true)
        // Simulate fetching all sections. In a real scenario, these might be
        // individual subscriptions or one larger subscription whose results are categorized.
        fetchPlaylists()
        fetchLikedSongs()
        fetchRecentlyPlayedItems()
        // In a real app, setLoading(false) would be called when all data is loaded
        // or after each section if they load independently and update isLoading accordingly.
        // For this simulation, we'll assume they are quick or managed by individual observers.
        // For simplicity in this step, we'll set loading to false after initiating.
        // A more robust solution would involve callbacks or observing multiple load states.
        setLoading(false) // Simplified for now
    }

    // Updated method to handle liking/unliking songs
    fun toggleLikeStatus(mediaItem: MediaBrowserCompat.MediaItem) {
        val currentLikedSongs = _likedSongs.value?.toMutableList() ?: mutableListOf()
        val mediaId = mediaItem.mediaId
        val existingItem = currentLikedSongs.find { it.mediaId == mediaId }

        if (existingItem != null) {
            // Item exists, so remove it (unlike)
            currentLikedSongs.removeAll { it.mediaId == mediaId }
        } else {
            // Item does not exist, so add it (like)
            // In a real app, you might fetch the full MediaItem or update a database
            // For now, we just add the item. We might need more details on the item
            // if it's not already fully populated.
            currentLikedSongs.add(mediaItem)
        }
        _likedSongs.value = currentLikedSongs

        // Potentially, you might want to persist this change to a backend or local storage here.
        // And also update the original source of this media item if it holds a liked status.
    }
}