package com.lindehammarkonsult.automus.shared

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.lindehammarkonsult.automus.shared.model.*
import com.lindehammarkonsult.automus.shared.playback.AppleMusicPlayer
import com.lindehammarkonsult.automus.shared.repository.AppleMusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "AppleMusicMediaService"
private const val ROOT_ID = "root"
private const val EMPTY_ROOT_ID = "empty_root_id"

/**
 * Implementation of a MediaLibraryService for Apple Music streaming
 * using Apple's MusicKit SDK for Android with Media3.
 */
@UnstableApi
class AppleMusicMediaService : MediaLibraryService() {

    private lateinit var player: AppleMusicPlayer
    private lateinit var mediaSession: MediaLibrarySession
    private lateinit var repository: AppleMusicRepository
    
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private val handler = Handler(Looper.getMainLooper())
    
    // Authentication properties from BuildConfig
    private val developerToken = BuildConfig.APPLE_MUSIC_DEVELOPER_TOKEN
    private val clientId = BuildConfig.APPLE_MUSIC_CLIENT_ID
    private val clientSecret = BuildConfig.APPLE_MUSIC_CLIENT_SECRET

    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "onCreate: initializing service")
        
        // Static initializer for native libraries
        try {
            // Prevent OOM false alarms
            System.setProperty("org.bytedeco.javacpp.maxphysicalbytes", "0")
            System.setProperty("org.bytedeco.javacpp.maxbytes", "0")
            
            // Load required native libraries
            System.loadLibrary("c++_shared")
            System.loadLibrary("appleMusicSDK")
            
            Log.d(TAG, "Successfully loaded native libraries")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load native libraries: ${e.message}", e)
        }
        
        // Initialize the repository with auth parameters
        repository = AppleMusicRepository(this, developerToken, clientId, clientSecret)
        
        // Initialize the MusicKit SDK
        repository.initialize()
        
        // Create the bridge player for Apple Music SDK
        player = AppleMusicPlayer(repository, serviceScope)
        
        // Set audio attributes for the Media3 player
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
            
        // Apply the audio attributes
        player.setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)

        // Create a MediaSession
        val sessionCallback = AppleMusicLibrarySessionCallback()
        mediaSession = MediaLibrarySession.Builder(this, player, sessionCallback)
            .setSessionActivity(buildSessionActivityPendingIntent())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: releasing resources")
        
        // Release Apple Music resources
        repository.release()
        
        // Release MediaSession and player
        mediaSession.release()
        player.release()
        
        // Cancel all coroutine jobs
        serviceJob.cancel()
        
        super.onDestroy()
    }

    /**
     * Build a pending intent for the media session activity
     */
    private fun buildSessionActivityPendingIntent(): PendingIntent {
        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let { intent ->
            PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } ?: PendingIntent.getActivity(
            this, 0, Intent(), PendingIntent.FLAG_IMMUTABLE
        )
        
        return activityIntent
    }
    
    /**
     * Media Library Session Callback implementation
     */
    private inner class AppleMusicLibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            
            // Add custom commands if needed
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .add(SessionCommand(ACTION_LOGIN, Bundle()))
                .add(SessionCommand(ACTION_LOGOUT, Bundle()))
                .build()
                
            return MediaSession.ConnectionResult.accept(
                availableSessionCommands,
                connectionResult.availablePlayerCommands
            )
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            // Check if client is allowed to browse
            if (!allowBrowsing(browser.packageName, 0)) {
                val emptyRoot = MediaItem.Builder()
                    .setMediaId(EMPTY_ROOT_ID)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()
                
                return Futures.immediateFuture(LibraryResult.ofItem(emptyRoot, params))
            }
            
            // Return root item
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
            
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return serviceScope.future {
                if (parentId == EMPTY_ROOT_ID) {
                    return@future LibraryResult.ofItemList(
                        ImmutableList.of(), params
                    )
                }

                // All suspend functions are called within the future block,
                // which creates a proper coroutine context
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
                            else -> ImmutableList.of()
                        }
                    }
                }
                
                LibraryResult.ofItemList(mediaItems, params)
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return serviceScope.future {
                // Fetch the individual item
                when {
                    mediaId.startsWith("playlist_") -> {
                        val playlistId = mediaId.removePrefix("playlist_")
                        val playlists = repository.getUserPlaylists()
                        val playlist = playlists.find { playlist: Playlist -> playlist.id == playlistId }
                        
                        playlist?.let { foundPlaylist: Playlist ->
                            LibraryResult.ofItem(foundPlaylist.toMedia3Item(), null)
                        } ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                    }
                    mediaId.startsWith("album_") -> {
                        // Similar handling for albums
                        LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                    }
                    else -> {
                        // Assume it's a track
                        val tracks = repository.getMockTracks()  // Should be from a real source
                        val track = tracks.find { track: Track -> track.id == mediaId }
                        
                        track?.let { foundTrack: Track ->
                            LibraryResult.ofItem(foundTrack.toMedia3Item(), null)
                        } ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                    }
                }
            }
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            return serviceScope.future {
                if (query.isBlank()) {
                    return@future LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                }
                
                val mediaId = "search_$query"
                
                // The actual search will be performed when client calls getChildren with this ID
                session.notifySearchResultChanged(browser, query, 1, params)
                
                LibraryResult.ofVoid()
            }
        }
        
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            return when (customCommand.customAction) {
                ACTION_LOGIN -> {
                    // Authentication must be initiated from an Activity
                    val resultBundle = Bundle().apply {
                        putBoolean("success", false)
                        putString("message", "Authentication must be initiated from an Activity")
                    }
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED, resultBundle))
                }
                ACTION_LOGOUT -> {
                    // Clear authentication token
                    repository.clearAuthToken()
                    val resultBundle = Bundle().apply {
                        putBoolean("success", true)
                    }
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, resultBundle))
                }
                else -> {
                    val resultBundle = Bundle().apply {
                        putBoolean("success", false)
                        putString("error", "Unknown action: ${customCommand.customAction}")
                    }
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE, resultBundle))
                }
            }
        }
        
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // Handle playback resumption
            return serviceScope.future {
                val queue = repository.playbackState.value.queue
                if (queue.isNotEmpty()) {
                    val mediaItems = queue.map { track -> track.toMedia3Item() }
                    val startItem = repository.playbackState.value.currentTrack?.let { currentTrack -> 
                        queue.indexOf(currentTrack).takeIf { index -> index >= 0 } ?: 0 
                    } ?: 0
                    val startPositionMs = repository.playbackState.value.position
                    
                    MediaSession.MediaItemsWithStartPosition(
                        mediaItems, 
                        startItem,
                        startPositionMs
                    )
                } else {
                    // No previous queue, return empty list
                    MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0)
                }
            }
        }
    }
    
    /**
     * Check if the client should be allowed to browse the media content
     */
    private fun allowBrowsing(clientPackageName: String?, clientUid: Int): Boolean {
        // Allow system UI, our own apps, and other trusted apps
        return true // In production, you may want to implement restrictions
    }
    
    /**
     * Get root level media items
     */
    private suspend fun getRootMediaItems(): ImmutableList<MediaItem> {
        val isAuthenticated = repository.isAuthenticated()
        val result = mutableListOf<MediaItem>()
        
        if (!isAuthenticated) {
            // Add login prompt item
            val loginItem = MediaItem.Builder()
                .setMediaId("login")
                .setMediaMetadata(MediaMetadata.Builder()
                    .setTitle("Login to Apple Music")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build())
                .build()
            result.add(loginItem)
            
            // Add browse categories for public content
            val browseItem = MediaItem.Builder()
                .setMediaId(MediaCategory.BROWSE_CATEGORIES.id)
                .setMediaMetadata(MediaMetadata.Builder()
                    .setTitle(MediaCategory.BROWSE_CATEGORIES.title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build())
                .build()
            result.add(browseItem)
            
            return ImmutableList.copyOf(result)
        }
        
        // Convert all categories to Media3 MediaItems
        return ImmutableList.copyOf(
            MediaCategory.values().map { category ->
                category.toMedia3Item()
            }
        )
    }
    
    /**
     * Get user playlists as media items
     */
    private suspend fun getPlaylistMediaItems(): ImmutableList<MediaItem> {
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
        
        return ImmutableList.copyOf(
            playlists.map { playlist ->
                playlist.toMedia3Item()
            }
        )
    }
    
    /**
     * Get liked songs as media items
     */
    private suspend fun getLikedSongsMediaItems(): ImmutableList<MediaItem> {
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
        
        return ImmutableList.copyOf(
            tracks.map { track ->
                track.toMedia3Item()
            }
        )
    }
    
    /**
     * Get recently played tracks as media items
     */
    private suspend fun getRecentlyPlayedMediaItems(): ImmutableList<MediaItem> {
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
        
        return ImmutableList.copyOf(
            tracks.map { track ->
                track.toMedia3Item()
            }
        )
    }
    
    /**
     * Get browse categories as media items
     */
    private suspend fun getCategoriesMediaItems(): ImmutableList<MediaItem> {
        val genres = repository.getGenres()
        val categoriesItems = mutableListOf<MediaItem>()
        
        // Add some predefined categories
        val featuredPlaylists = MediaItem.Builder()
            .setMediaId("category_featured_playlists")
            .setMediaMetadata(MediaMetadata.Builder()
                .setTitle("Featured Playlists")
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build())
            .build()
        categoriesItems.add(featuredPlaylists)
        
        val newReleases = MediaItem.Builder()
            .setMediaId("category_new_releases")
            .setMediaMetadata(MediaMetadata.Builder()
                .setTitle("New Releases")
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build())
            .build()
        categoriesItems.add(newReleases)
        
        // Add genres
        genres.forEach { genre: Genre ->
            categoriesItems.add(genre.toMedia3Item())
        }
        
        return ImmutableList.copyOf(categoriesItems)
    }
    
    /**
     * Get tracks for a playlist
     */
    private suspend fun getPlaylistTracks(playlistId: String): ImmutableList<MediaItem> {
        val tracks = repository.getPlaylistTracks(playlistId)
        
        return ImmutableList.copyOf(
            tracks.map { track ->
                track.toMedia3Item()
            }
        )
    }
    
    /**
     * Get tracks for an album
     */
    private suspend fun getAlbumTracks(albumId: String): ImmutableList<MediaItem> {
        val tracks = repository.getAlbumTracks(albumId)
        
        return ImmutableList.copyOf(
            tracks.map { track ->
                track.toMedia3Item()
            }
        )
    }
    
    /**
     * Get albums for an artist
     */
    private suspend fun getArtistAlbums(artistId: String): ImmutableList<MediaItem> {
        // This is a placeholder - actual implementation would call repository.getArtistAlbums
        return ImmutableList.of()
    }
    
    /**
     * Get playlists for a genre
     */
    private suspend fun getGenrePlaylists(genreId: String): ImmutableList<MediaItem> {
        // This is a placeholder - actual implementation would call repository.getGenrePlaylists
        return ImmutableList.of()
    }
    
    /**
     * Perform a search
     */
    private suspend fun performSearch(query: String): ImmutableList<MediaItem> {
        val searchResults = repository.search(query)
        val mediaItems = mutableListOf<MediaItem>()
        
        // Add tracks from search
        searchResults.songs?.data?.let { trackList: List<Track> ->
            for (track in trackList) {
                mediaItems.add(track.toMedia3Item())
            }
        }
        
        // Add albums from search
        searchResults.albums?.data?.let { albumList: List<Album> ->
            for (album in albumList) {
                mediaItems.add(album.toMedia3Item())
            }
        }
        
        // Add playlists from search
        searchResults.playlists?.data?.let { playlistList: List<Playlist> ->
            for (playlist in playlistList) {
                mediaItems.add(playlist.toMedia3Item())
            }
        }
        
        // Add artists from search
        searchResults.artists?.data?.let { artistList: List<Artist> ->
            for (artist in artistList) {
                mediaItems.add(artist.toMedia3Item())
            }
        }
        
        return ImmutableList.copyOf(mediaItems)
    }
    
    companion object {
        const val ACTION_LOGIN = "com.lindehammarkonsult.automus.action.LOGIN"
        const val ACTION_LOGOUT = "com.lindehammarkonsult.automus.action.LOGOUT"
    }
}

/**
 * Extension function to convert a coroutine to a ListenableFuture
 */
fun <T> CoroutineScope.future(block: suspend () -> T): ListenableFuture<T> {
    val future = SettableFuture.create<T>()
    launch {
        try {
            future.set(block())
        } catch (e: Exception) {
            future.setException(e)
        }
    }
    return future
}
