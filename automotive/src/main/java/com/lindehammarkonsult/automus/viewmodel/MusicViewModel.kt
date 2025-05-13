package com.lindehammarkonsult.automus.viewmodel

import android.app.Application
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.lindehammarkonsult.automus.shared.client.AppleMusicClient
import com.lindehammarkonsult.automus.shared.utils.MediaItemConverter
import com.lindehammarkonsult.automus.shared.viewmodel.Media3ViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    
    // Media3 ViewModel for handling Media3 interactions
    private val media3ViewModel = Media3ViewModel(application)
    
    // Connection state
    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected
    
    // Current playback state (legacy and Media3)
    private val _playbackState = MutableLiveData<PlaybackStateCompat>()
    val playbackState: LiveData<PlaybackStateCompat> = _playbackState
    
    // Current media metadata
    private val _metadata = MutableLiveData<MediaMetadataCompat>()
    val metadata: LiveData<MediaMetadataCompat> = _metadata
    
    // Current media items (for lists) - legacy MediaBrowserCompat items
    private val _mediaItems = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
    val mediaItems: LiveData<List<MediaBrowserCompat.MediaItem>> = _mediaItems
    
    // Current media items using Media3
    private val _media3Items = MutableLiveData<List<MediaItem>>()
    val media3Items: LiveData<List<MediaItem>> = _media3Items

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

    // Methods to fetch library data (simulated with mock data)
    fun fetchPlaylists() {
        // Create mock playlist data to match the design
        val playlists = mutableListOf<MediaBrowserCompat.MediaItem>()
        
        // My Favorites playlist
        val favoritesDescription = MediaDescriptionCompat.Builder()
            .setMediaId("playlist_favorites")
            .setTitle("My Favorites")
            .setSubtitle("324 songs")
            .build()
        playlists.add(MediaBrowserCompat.MediaItem(favoritesDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE))
        
        // Driving Mix playlist
        val drivingDescription = MediaDescriptionCompat.Builder()
            .setMediaId("playlist_driving")
            .setTitle("Driving Mix")
            .setSubtitle("156 songs")
            .build()
        playlists.add(MediaBrowserCompat.MediaItem(drivingDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE))
        
        // Workout Beats playlist
        val workoutDescription = MediaDescriptionCompat.Builder()
            .setMediaId("playlist_workout")
            .setTitle("Workout Beats")
            .setSubtitle("89 songs")
            .build()
        playlists.add(MediaBrowserCompat.MediaItem(workoutDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE))
        
        _playlists.postValue(playlists)
    }

    fun fetchLikedSongs() {
        // Create mock liked songs data to match the design
        val likedSongs = mutableListOf<MediaBrowserCompat.MediaItem>()
        
        // Blinding Lights by The Weeknd
        val blindingLightsDescription = MediaDescriptionCompat.Builder()
            .setMediaId("song_blinding_lights")
            .setTitle("Blinding Lights")
            .setSubtitle("The Weeknd")
            .build()
        likedSongs.add(MediaBrowserCompat.MediaItem(blindingLightsDescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
        
        // Stay by Kid Laroi & Justin Bieber
        val stayDescription = MediaDescriptionCompat.Builder()
            .setMediaId("song_stay")
            .setTitle("Stay")
            .setSubtitle("Kid Laroi & Justin Bieber")
            .build()
        likedSongs.add(MediaBrowserCompat.MediaItem(stayDescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
        
        // As It Was by Harry Styles
        val asItWasDescription = MediaDescriptionCompat.Builder()
            .setMediaId("song_as_it_was")
            .setTitle("As It Was")
            .setSubtitle("Harry Styles")
            .build()
        likedSongs.add(MediaBrowserCompat.MediaItem(asItWasDescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
        
        _likedSongs.postValue(likedSongs)
    }

    fun fetchRecentlyPlayedItems() {
        // Create mock recently played items to match the design
        val recentlyPlayed = mutableListOf<MediaBrowserCompat.MediaItem>()
        
        // Starboy by The Weeknd
        val starboyDescription = MediaDescriptionCompat.Builder()
            .setMediaId("album_starboy")
            .setTitle("Starboy")
            .setSubtitle("Album • The Weeknd")
            .build()
        recentlyPlayed.add(MediaBrowserCompat.MediaItem(starboyDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE))
        
        // Future Nostalgia by Dua Lipa
        val futureNostalgiaDescription = MediaDescriptionCompat.Builder()
            .setMediaId("album_future_nostalgia")
            .setTitle("Future Nostalgia")
            .setSubtitle("Album • Dua Lipa")
            .build()
        recentlyPlayed.add(MediaBrowserCompat.MediaItem(futureNostalgiaDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE))
        
        // Positions by Ariana Grande
        val positionsDescription = MediaDescriptionCompat.Builder()
            .setMediaId("album_positions")
            .setTitle("Positions")
            .setSubtitle("Album • Ariana Grande")
            .build()
        recentlyPlayed.add(MediaBrowserCompat.MediaItem(positionsDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE))
        
        _recentlyPlayedItems.postValue(recentlyPlayed)
    }

    fun fetchAllLibraryData() {
        setLoading(true)
        // Load mock data for all sections
        fetchPlaylists()
        fetchLikedSongs()
        fetchRecentlyPlayedItems()
        setLoading(false)
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
    
    // Media3 related methods
    
    /**
     * Browse to a specific Media3 media item
     */
    fun browseToMedia3Item(mediaItem: MediaItem) {
        media3ViewModel.browseToItem(mediaItem)
    }
    
    /**
     * Play a Media3 media item
     */
    fun playMedia3Item(mediaItem: MediaItem) {
        media3ViewModel.playMediaItem(mediaItem)
    }
    
    /**
     * Navigate up in the Media3 browse hierarchy
     */
    fun navigateUp(): Boolean {
        return media3ViewModel.navigateUp()
    }
    
    /**
     * Toggle play/pause on the Media3 player
     */
    fun togglePlayPause() {
        media3ViewModel.togglePlayPause()
    }
    
    /**
     * Skip to the next track
     */
    fun skipToNext() {
        media3ViewModel.skipToNext()
    }
    
    /**
     * Skip to the previous track
     */
    fun skipToPrevious() {
        media3ViewModel.skipToPrevious()
    }
    
    /**
     * Search for content using Media3
     */
    fun searchMedia3(query: String) {
        media3ViewModel.search(query)
    }
    
    /**
     * Refresh the current Media3 media items
     */
    fun refreshMedia3Items() {
        media3ViewModel.refreshMediaItems()
    }
    
    override fun onCleared() {
        super.onCleared()
        // Disconnect from the Media3 service when the ViewModel is cleared
        media3ViewModel.disconnectFromService()
    }

    init {
        // Connect to the Media3 service
        media3ViewModel.connectToService()
        
        // Observe Media3 connection state
        viewModelScope.launch {
            media3ViewModel.isConnected.collectLatest { connected ->
                _isConnected.postValue(connected)
            }
        }
        
        // Observe Media3 media items
        viewModelScope.launch {
            media3ViewModel.mediaItems.collectLatest { items ->
                _media3Items.postValue(items)
                
                // Convert Media3 items to legacy MediaBrowserCompat.MediaItem for backwards compatibility
                _mediaItems.postValue(MediaItemConverter.convertToLegacyItems(items))
            }
        }
        
        // Observe Media3 playback state
        viewModelScope.launch {
            media3ViewModel.playbackState.collectLatest { state ->
                // Update metadata and playback state from Media3
                updateFromMedia3PlaybackState(state)
            }
        }
    }
    
    // Using MediaItemConverter utility instead of a local method
    
    /**
     * Update legacy metadata and playback state from Media3 playback state
     */
    private fun updateFromMedia3PlaybackState(state: AppleMusicClient.MediaState) {
        // Update metadata
        state.currentMediaItem?.let { mediaItem ->
            val metadata = mediaItem.mediaMetadata
            val mediaMetadataCompat = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaItem.mediaId)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, metadata.title?.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, metadata.title?.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, metadata.artist?.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, metadata.albumTitle?.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, metadata.subtitle?.toString())
                .apply {
                    metadata.artworkUri?.let {
                        putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, it.toString())
                        putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, it.toString())
                    }
                    metadata.extras?.getLong("duration")?.let {
                        putLong(MediaMetadataCompat.METADATA_KEY_DURATION, it)
                    }
                }
                .build()
                
            _metadata.postValue(mediaMetadataCompat)
        }
        
        // Update playback state
        val legacyState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(
                if (state.isPlaying) 
                    PlaybackStateCompat.STATE_PLAYING
                else
                    PlaybackStateCompat.STATE_PAUSED,
                0, // position - would need to get from Media3
                1.0f // playback speed
            )
            .build()
            
        _playbackState.postValue(legacyState)
    }
}