package com.lindehammarkonsult.automus.shared

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem as ExoMediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.lindehammarkonsult.automus.shared.model.*
import com.lindehammarkonsult.automus.shared.repository.AppleMusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "AppleMusicService"
private const val ROOT_ID = "root"
private const val MEDIA_ROOT_ID = "media_root_id"
private const val EMPTY_ROOT_ID = "empty_root_id"
private const val BROWSE_ERROR = "browse_error"

/**
 * Implementation of a MediaBrowserServiceCompat for Apple Music streaming.
 */
class AppleMusicService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var repository: AppleMusicRepository
    
    private lateinit var player: ExoPlayer
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private var isForegroundService = false
    private val handler = Handler(Looper.getMainLooper())
    
    private var currentPlaybackPosition = 0L
    private var updatePositionJob: Runnable = object : Runnable {
        override fun run() {
            if (player.isPlaying) {
                currentPlaybackPosition = player.currentPosition
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "onCreate: initializing service")
        
        // Initialize the repository
        repository = AppleMusicRepository(this)
        
        // Initialize the ExoPlayer
        player = ExoPlayer.Builder(this).build()
        
        // Configure player listeners
        player.addListener(PlayerEventListener())
        
        // Create a MediaSession
        mediaSession = MediaSessionCompat(this, TAG).apply {
            setSessionActivity(buildSessionActivityPendingIntent())
            isActive = true
        }
        
        // Set the session token for the MediaBrowserService
        sessionToken = mediaSession.sessionToken
        
        // Initialize the MediaSessionConnector
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(AppleMusicPlaybackPreparer())
        mediaSessionConnector.setQueueNavigator(QueueNavigator())
        mediaSessionConnector.setPlayer(player)
        
        // Start position update job
        handler.postDelayed(updatePositionJob, 1000)
        
        // Observe changes to playback state
        serviceScope.launch {
            repository.playbackState.collectLatest { state ->
                // Update media session metadata and playback state
                updatePlaybackState(state)
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: releasing resources")
        serviceScope.launch {
            // Cancel any ongoing playback
            player.stop()
            player.release()
        }
        
        // Cancel position updates
        handler.removeCallbacks(updatePositionJob)
        
        // Release MediaSession and cancel ServiceScope
        mediaSession.release()
        serviceJob.cancel()
        
        super.onDestroy()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        val rootExtras = Bundle().apply {
            // Signal to clients whether sign-in is required
            putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
            putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 1)
            putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 0)
        }
        
        return if (allowBrowsing(clientPackageName, clientUid)) {
            // Return root for browsing
            BrowserRoot(ROOT_ID, rootExtras)
        } else {
            // Return an empty root - no browsing allowed
            BrowserRoot(EMPTY_ROOT_ID, rootExtras)
        }
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaItem>>
    ) {
        result.detach()
        
        if (parentId == EMPTY_ROOT_ID) {
            result.sendResult(mutableListOf())
            return
        }

        serviceScope.launch {
            val mediaItems = when (parentId) {
                ROOT_ID -> getRootMediaItems()
                MediaCategory.PLAYLISTS.id -> getPlaylistMediaItems()
                MediaCategory.LIKED_SONGS.id -> getLikedSongsMediaItems()
                MediaCategory.RECENTLY_PLAYED.id -> getRecentlyPlayedMediaItems()
                MediaCategory.BROWSE_CATEGORIES.id -> getCategoriesMediaItems()
                else -> {
                    // Handle media ID patterns for nested browsing
                    when {
                        parentId.startsWith("playlist_") -> {
                            val playlistId = parentId.removePrefix("playlist_")
                            getPlaylistTracks(playlistId)
                        }
                        parentId.startsWith("album_") -> {
                            val albumId = parentId.removePrefix("album_")
                            getAlbumTracks(albumId)
                        }
                        parentId.startsWith("artist_") -> {
                            val artistId = parentId.removePrefix("artist_")
                            getArtistAlbums(artistId)
                        }
                        parentId.startsWith("genre_") -> {
                            val genreId = parentId.removePrefix("genre_")
                            getGenrePlaylists(genreId)
                        }
                        parentId.startsWith("search_") -> {
                            val searchQuery = parentId.removePrefix("search_")
                            performSearch(searchQuery)
                        }
                        else -> mutableListOf()
                    }
                }
            }
            
            result.sendResult(mediaItems)
        }
    }

    override fun onSearch(
        query: String,
        extras: Bundle?,
        result: Result<MutableList<MediaItem>>
    ) {
        result.detach()
        
        if (query.isBlank()) {
            result.sendResult(mutableListOf())
            return
        }
        
        serviceScope.launch {
            val searchResults = performSearch(query)
            result.sendResult(searchResults)
        }
    }
    
    override fun onCustomAction(action: String, extras: Bundle?, result: Result<Bundle>) {
        when (action) {
            ACTION_LOGIN -> {
                // Handle login from activities
                val token = extras?.getString("token")
                val expiresIn = extras?.getLong("expires_in") ?: 0L
                
                if (token != null && expiresIn > 0) {
                    val appleToken = AppleMusicToken(
                        accessToken = token,
                        expiresAt = System.currentTimeMillis() + expiresIn,
                        refreshToken = extras.getString("refresh_token")
                    )
                    
                    repository.setAuthToken(appleToken)
                    
                    val resultBundle = Bundle().apply {
                        putBoolean("success", true)
                    }
                    result.sendResult(resultBundle)
                } else {
                    val resultBundle = Bundle().apply {
                        putBoolean("success", false)
                        putString("error", "Invalid token data")
                    }
                    result.sendResult(resultBundle)
                }
            }
            ACTION_LOGOUT -> {
                repository.clearAuthToken()
                val resultBundle = Bundle().apply {
                    putBoolean("success", true)
                }
                result.sendResult(resultBundle)
            }
            else -> {
                val resultBundle = Bundle().apply {
                    putBoolean("success", false)
                    putString("error", "Unknown action: $action")
                }
                result.sendResult(resultBundle)
            }
        }
    }
    
    /**
     * Check if the client should be allowed to browse the media content
     */
    private fun allowBrowsing(clientPackageName: String, clientUid: Int): Boolean {
        // Allow system UI, our own apps, and other trusted apps
        return true // In production, you may want to implement restrictions
    }
    
    /**
     * Get root level media items
     */
    private suspend fun getRootMediaItems(): MutableList<MediaItem> {
        val isAuthenticated = repository.isAuthenticated()
        
        // If not authenticated, only show some items and a "Login" action
        if (!isAuthenticated) {
            val result = mutableListOf<MediaItem>()
            
            // Add login prompt item
            val loginItem = MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId("login")
                    .setTitle("Login to Apple Music")
                    .setIconUri(Uri.parse("android.resource://${packageName}/drawable/ic_account"))
                    .build(),
                MediaItem.FLAG_BROWSABLE
            )
            result.add(loginItem)
            
            // Add browse categories for public content
            val browseItem = MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(MediaCategory.BROWSE_CATEGORIES.id)
                    .setTitle(MediaCategory.BROWSE_CATEGORIES.title)
                    .build(),
                MediaItem.FLAG_BROWSABLE
            )
            result.add(browseItem)
            
            return result
        }
        
        return repository.getRootCategories().toMutableList()
    }
    
    /**
     * Get user playlists as media items
     */
    private suspend fun getPlaylistMediaItems(): MutableList<MediaItem> {
        // Use mock data for demo or when not authenticated
        val playlists = if (repository.isAuthenticated()) {
            try {
                repository.getUserPlaylists()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching playlists: ${e.message}")
                repository.getMockPlaylists()
            }
        } else {
            repository.getMockPlaylists()
        }
        
        return playlists.map { playlist ->
            // Prefix playlist IDs to distinguish them in the browse hierarchy
            val mediaId = "playlist_${playlist.id}"
            
            MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(mediaId)
                    .setTitle(playlist.title)
                    .setSubtitle("${playlist.trackCount} songs")
                    .setIconUri(playlist.artworkUri)
                    .build(),
                MediaItem.FLAG_BROWSABLE
            )
        }.toMutableList()
    }
    
    /**
     * Get liked songs as media items
     */
    private suspend fun getLikedSongsMediaItems(): MutableList<MediaItem> {
        val tracks = if (repository.isAuthenticated()) {
            try {
                repository.getLikedSongs()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching liked songs: ${e.message}")
                repository.getMockTracks()
            }
        } else {
            repository.getMockTracks()
        }
        
        return tracks.map { track ->
            MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(track.id)
                    .setTitle(track.title)
                    .setSubtitle(track.subtitle)
                    .setIconUri(track.artworkUri)
                    .build(),
                MediaItem.FLAG_PLAYABLE
            )
        }.toMutableList()
    }
    
    /**
     * Get recently played tracks as media items
     */
    private suspend fun getRecentlyPlayedMediaItems(): MutableList<MediaItem> {
        val tracks = if (repository.isAuthenticated()) {
            try {
                repository.getRecentlyPlayed()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching recently played: ${e.message}")
                repository.getMockTracks()
            }
        } else {
            repository.getMockTracks()
        }
        
        return tracks.map { track ->
            MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(track.id)
                    .setTitle(track.title)
                    .setSubtitle(track.subtitle)
                    .setIconUri(track.artworkUri)
                    .build(),
                MediaItem.FLAG_PLAYABLE
            )
        }.toMutableList()
    }
    
    /**
     * Get browse categories as media items
     */
    private suspend fun getCategoriesMediaItems(): MutableList<MediaItem> {
        val genres = repository.getGenres()
        
        val categoriesItems = mutableListOf<MediaItem>()
        
        // Add some predefined categories
        val featuredPlaylists = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId("category_featured_playlists")
                .setTitle("Featured Playlists")
                .build(),
            MediaItem.FLAG_BROWSABLE
        )
        categoriesItems.add(featuredPlaylists)
        
        val newReleases = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId("category_new_releases")
                .setTitle("New Releases")
                .build(),
            MediaItem.FLAG_BROWSABLE
        )
        categoriesItems.add(newReleases)
        
        // Add genres
        genres.forEach { genre ->
            val mediaId = "genre_${genre.id}"
            
            categoriesItems.add(
                MediaItem(
                    MediaDescriptionCompat.Builder()
                        .setMediaId(mediaId)
                        .setTitle(genre.title)
                        .setIconUri(genre.artworkUri)
                        .build(),
                    MediaItem.FLAG_BROWSABLE
                )
            )
        }
        
        return categoriesItems
    }
    
    /**
     * Get tracks for a playlist
     */
    private suspend fun getPlaylistTracks(playlistId: String): MutableList<MediaItem> {
        val tracks = repository.getPlaylistTracks(playlistId)
        
        return tracks.map { track ->
            track.toMediaItem()
        }.toMutableList()
    }
    
    /**
     * Get tracks for an album
     */
    private suspend fun getAlbumTracks(albumId: String): MutableList<MediaItem> {
        val tracks = repository.getAlbumTracks(albumId)
        
        return tracks.map { track ->
            track.toMediaItem()
        }.toMutableList()
    }
    
    /**
     * Get albums for an artist
     */
    private suspend fun getArtistAlbums(artistId: String): MutableList<MediaItem> {
        // This is a placeholder - actual implementation would call repository.getArtistAlbums
        return mutableListOf()
    }
    
    /**
     * Get playlists for a genre
     */
    private suspend fun getGenrePlaylists(genreId: String): MutableList<MediaItem> {
        // This is a placeholder - actual implementation would call repository.getGenrePlaylists
        return mutableListOf()
    }
    
    /**
     * Perform a search
     */
    private suspend fun performSearch(query: String): MutableList<MediaItem> {
        val searchResults = repository.search(query)
        val mediaItems = mutableListOf<MediaItem>()
        
        // Add tracks from search
        searchResults.songs?.data?.forEach { track ->
            mediaItems.add(track.toMediaItem())
        }
        
        // Add albums from search (browsable)
        searchResults.albums?.data?.forEach { album ->
            val mediaId = "album_${album.id}"
            
            mediaItems.add(
                MediaItem(
                    MediaDescriptionCompat.Builder()
                        .setMediaId(mediaId)
                        .setTitle(album.title)
                        .setSubtitle(album.subtitle)
                        .setIconUri(album.artworkUri)
                        .build(),
                    MediaItem.FLAG_BROWSABLE
                )
            )
        }
        
        // Add playlists from search (browsable)
        searchResults.playlists?.data?.forEach { playlist ->
            val mediaId = "playlist_${playlist.id}"
            
            mediaItems.add(
                MediaItem(
                    MediaDescriptionCompat.Builder()
                        .setMediaId(mediaId)
                        .setTitle(playlist.title)
                        .setSubtitle(playlist.subtitle)
                        .setIconUri(playlist.artworkUri)
                        .build(),
                    MediaItem.FLAG_BROWSABLE
                )
            )
        }
        
        // Add artists from search (browsable)
        searchResults.artists?.data?.forEach { artist ->
            val mediaId = "artist_${artist.id}"
            
            mediaItems.add(
                MediaItem(
                    MediaDescriptionCompat.Builder()
                        .setMediaId(mediaId)
                        .setTitle(artist.title)
                        .setSubtitle(artist.subtitle)
                        .setIconUri(artist.artworkUri)
                        .build(),
                    MediaItem.FLAG_BROWSABLE
                )
            )
        }
        
        return mediaItems
    }
    
    /**
     * Build a pending intent for the media session activity
     */
    private fun buildSessionActivityPendingIntent(): android.app.PendingIntent? {
        // In a real app, this should open your player activity
        return null
    }
    
    /**
     * Update the playback state based on the current state from repository
     */
    private fun updatePlaybackState(state: PlaybackState) {
        val track = state.currentTrack ?: return
        
        // Update metadata
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.id)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artistName)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.albumName)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.durationMs)
            .apply {
                track.artworkUri?.let {
                    putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, it.toString())
                    putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, it.toString())
                }
            }
            .build()
            
        mediaSession.setMetadata(metadata)
        
        // Update playback state
        val playbackStateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(
                if (state.isPlaying)
                    PlaybackStateCompat.STATE_PLAYING
                else
                    PlaybackStateCompat.STATE_PAUSED,
                state.position,
                1.0f
            )
            
        mediaSession.setPlaybackState(playbackStateBuilder.build())
    }
    
    /**
     * ExoPlayer event listener
     */
    private inner class PlayerEventListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    // Handle buffering state
                }
                Player.STATE_READY -> {
                    // Handle ready state
                    repository.updatePlaybackState(
                        isPlaying = player.isPlaying,
                        position = player.currentPosition
                    )
                }
                Player.STATE_ENDED -> {
                    // Handle playback ended
                    repository.skipToNext()
                }
                Player.STATE_IDLE -> {
                    // Handle idle state
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            repository.updatePlaybackState(isPlaying = isPlaying)
        }
    }
    
    /**
     * Preparer for playback
     */
    private inner class AppleMusicPlaybackPreparer : MediaSessionConnector.PlaybackPreparer {
        override fun onPrepare(playWhenReady: Boolean) {
            // Just prepare the current media with no specific arguments
            val current = repository.playbackState.value.currentTrack
            if (current != null) {
                prepareMediaForPlaying(current, playWhenReady)
            }
        }

        override fun onPrepareFromMediaId(
            mediaId: String,
            playWhenReady: Boolean,
            extras: Bundle?
        ) {
            // Find the track in the repository
            val tracks = extras?.getParcelableArrayList<MediaMetadataCompat>("tracks")
            val startIndex = extras?.getInt("startIndex") ?: 0
            
            if (tracks != null && tracks.isNotEmpty()) {
                // If we've received a list of tracks (e.g. for an album or playlist)
                val tracksList = tracks.mapNotNull { metadata ->
                    // Convert MediaMetadataCompat to our Track model
                    val id = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                    val title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                    val artistName = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
                    val albumName = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
                    
                    // These values might not be available in the metadata 
                    val albumId = extras.getString("albumId") ?: ""
                    val artistId = extras.getString("artistId") ?: ""
                    
                    val artworkUri = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)?.let {
                        Uri.parse(it)
                    }
                    
                    val durationMs = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                    
                    if (id != null && title != null && artistName != null && albumName != null) {
                        Track(
                            id = id,
                            title = title,
                            albumName = albumName,
                            artistName = artistName,
                            albumId = albumId,
                            artistId = artistId,
                            artworkUri = artworkUri,
                            streamUrl = null, // Will be resolved during playback
                            previewUrl = null,
                            durationMs = durationMs
                        )
                    } else {
                        null
                    }
                }
                
                // Set the queue and current track
                repository.setCurrentQueue(tracksList, startIndex)
                
                // Prepare the specified track
                tracksList.getOrNull(startIndex)?.let { track ->
                    prepareMediaForPlaying(track, playWhenReady)
                }
            } else {
                // Handle single track playback
                serviceScope.launch {
                    // Find track by ID - in a real app, you'd query the repository
                    // For now, just use mock data for demo
                    val tracks = repository.getMockTracks()
                    val track = tracks.find { it.id == mediaId }
                    
                    if (track != null) {
                        // Set current queue to just this track
                        repository.setCurrentQueue(listOf(track))
                        
                        // Prepare the track
                        prepareMediaForPlaying(track, playWhenReady)
                    }
                }
            }
        }

        override fun onPrepareFromSearch(
            query: String,
            playWhenReady: Boolean,
            extras: Bundle?
        ) {
            // For voice search, implement this
            serviceScope.launch {
                val searchResults = repository.search(query)
                val tracks = searchResults.songs?.data ?: emptyList()
                
                if (tracks.isNotEmpty()) {
                    repository.setCurrentQueue(tracks)
                    prepareMediaForPlaying(tracks[0], playWhenReady)
                }
            }
        }

        override fun onPrepareFromUri(
            uri: Uri,
            playWhenReady: Boolean,
            extras: Bundle?
        ) {
            // Not used in this implementation
        }

        override fun getSupportedPrepareActions(): Long {
            return MediaSessionConnector.PlaybackPreparer.ACTIONS
        }
        
        /**
         * Prepare a track for playback
         */
        private fun prepareMediaForPlaying(track: Track, playWhenReady: Boolean) {
            // In a real app, you'd resolve the stream URL here
            val url = track.streamUrl ?: track.previewUrl
            
            if (url != null) {
                val mediaItem = ExoMediaItem.Builder()
                    .setUri(url)
                    .build()
                
                player.setMediaItem(mediaItem)
                player.prepare()
                
                if (playWhenReady) {
                    player.play()
                }
            }
        }
    }
    
    /**
     * Navigator for queue management
     */
    private inner class QueueNavigator : MediaSessionConnector.QueueNavigator {
        override fun onSkipToQueueItem(player: Player, id: Long) {
            repository.skipToPosition(id.toInt())
            
            // Update player with the new current track
            val currentTrack = repository.playbackState.value.currentTrack
            if (currentTrack != null) {
                val url = currentTrack.streamUrl ?: currentTrack.previewUrl
                if (url != null) {
                    val mediaItem = ExoMediaItem.Builder()
                        .setUri(url)
                        .build()
                    
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                }
            }
        }

        override fun onSkipToPrevious(player: Player) {
            repository.skipToPrevious()
            
            // Update player with the previous track
            val currentTrack = repository.playbackState.value.currentTrack
            if (currentTrack != null) {
                val url = currentTrack.streamUrl ?: currentTrack.previewUrl
                if (url != null) {
                    val mediaItem = ExoMediaItem.Builder()
                        .setUri(url)
                        .build()
                    
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                }
            }
        }

        override fun onSkipToNext(player: Player) {
            repository.skipToNext()
            
            // Update player with the next track
            val currentTrack = repository.playbackState.value.currentTrack
            if (currentTrack != null) {
                val url = currentTrack.streamUrl ?: currentTrack.previewUrl
                if (url != null) {
                    val mediaItem = ExoMediaItem.Builder()
                        .setUri(url)
                        .build()
                    
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                }
            }
        }

        override fun getActiveQueueItemId(player: Player): Long {
            val playbackState = repository.playbackState.value
            val currentTrack = playbackState.currentTrack
            
            return if (currentTrack != null) {
                playbackState.queue.indexOf(currentTrack).toLong()
            } else {
                -1
            }
        }

        override fun onCommand(
            player: Player,
            command: String,
            extras: Bundle?,
            cb: ResultReceiver?
        ): Boolean {
            // Handle custom commands
            return false
        }
    }
    
    companion object {
        const val ACTION_LOGIN = "com.lindehammarkonsult.automus.action.LOGIN"
        const val ACTION_LOGOUT = "com.lindehammarkonsult.automus.action.LOGOUT"
    }
}