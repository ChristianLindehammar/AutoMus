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
}