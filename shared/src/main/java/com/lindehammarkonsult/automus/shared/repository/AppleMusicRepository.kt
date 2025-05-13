package com.lindehammarkonsult.automus.shared.repository

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import com.lindehammarkonsult.automus.shared.api.AppleMusicApiService
import com.lindehammarkonsult.automus.shared.auth.AppleMusicAuthManager
import com.lindehammarkonsult.automus.shared.auth.AppleMusicPlaybackManager
import com.lindehammarkonsult.automus.shared.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import com.lindehammarkonsult.automus.shared.api.ApiResponse
import com.lindehammarkonsult.automus.shared.api.SearchResults
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

private const val TAG = "AppleMusicRepository"

/**
 * Repository for Apple Music data
 */
class AppleMusicRepository(
    private val context: Context,
    private val developerToken: String,
    private val clientId: String,
    private val clientSecret: String
) {
    // API Service
    private val apiService: AppleMusicApiService
    
    // MusicKit SDK managers
    private val authManager: AppleMusicAuthManager = AppleMusicAuthManager(
        context,
        developerToken,
        clientId,
        clientSecret
    )
    private lateinit var playbackManager: AppleMusicPlaybackManager
    
    // Expose playback state from the playback manager
    val playbackState: StateFlow<PlaybackState>
        get() = playbackManager.playbackState
    
    init {
        // Set up OkHttp client for API requests
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
            
        // Set up Moshi for JSON parsing
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            
        // Create Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.music.apple.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            
        // Create API service
        apiService = retrofit.create(AppleMusicApiService::class.java)
    }
    
    /**
     * Initialize the MusicKit SDK with developer token
     */
    fun initialize() {
        // Check if token is a placeholder or empty
        if (developerToken == "YOUR_APPLE_MUSIC_DEVELOPER_TOKEN_HERE" || developerToken.isBlank()) {
            Log.w(TAG, "Developer token is missing or using placeholder. Running in limited mode.")
            // We'll still initialize with the placeholder, but auth will fail
            // This allows the app to start without crashing
        } else {
            Log.d(TAG, "Initializing Apple Music repository with developer token")
        }
        
        try {
            // Initialize auth manager
            authManager.initialize()
            
            // Initialize the playback manager with the token provider
            playbackManager = AppleMusicPlaybackManager(context, authManager.tokenProvider)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Apple Music components: ${e.message}", e)
            // Create a default playback manager to avoid null references
            playbackManager = AppleMusicPlaybackManager(context, authManager.tokenProvider)
        }
    }
    
    /**
     * Check if the user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return authManager.isAuthenticated()
    }
    
    /**
     * Start authentication process (should be called from Activity)
     */
    fun authenticate(activity: Activity) {
        authManager.authenticate(activity)
    }
    
    /**
     * Clear the authentication token and log out the user
     */
    fun clearAuthToken() {
        try {
            authManager.clearToken()
            Log.d(TAG, "User authentication token cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing authentication token: ${e.message}", e)
        }
    }
    
    // Playback control methods
    
    /**
     * Play a track by its ID
     */
    fun playTrack(trackId: String) {
        playbackManager.playTrack(trackId)
    }
    
    /**
     * Play an album by its ID
     */
    fun playAlbum(albumId: String) {
        playbackManager.playAlbum(albumId)
    }
    
    /**
     * Play a playlist by its ID
     */
    fun playPlaylist(playlistId: String) {
        playbackManager.playPlaylist(playlistId)
    }
    
    /**
     * Toggle between play and pause
     */
    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }
    
    /**
     * Resume playback if paused
     */
    fun resumePlayback() {
        playbackManager.togglePlayPause()
    }
    
    /**
     * Pause playback if playing
     */
    fun pausePlayback() {
        playbackManager.togglePlayPause()
    }
    
    /**
     * Stop playback completely
     */
    fun stopPlayback() {
        // Apple Music SDK doesn't have a direct "stop" method, so we pause and reset
        pausePlayback()
    }
    
    /**
     * Skip to the next track in queue
     */
    fun skipToNext() {
        playbackManager.skipToNext()
    }
    
    /**
     * Skip to the previous track in queue
     */
    fun skipToPrevious() {
        playbackManager.skipToPrevious() 
    }
    
    /**
     * Seek to a specific position in the current track
     */
    fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }
    
    /**
     * Set the repeat mode
     */
    fun setRepeatMode(repeatMode: RepeatMode) {
        playbackManager.setRepeatMode(repeatMode)
    }
    
    /**
     * Set shuffle mode on or off
     */
    fun setShuffleMode(shuffleEnabled: Boolean) {
        playbackManager.setShuffleMode(shuffleEnabled)
    }

    // Existing methods below...
    
    /**
     * Get the root media categories
     */
    suspend fun getRootMenuItems(): List<MediaBrowserCompat.MediaItem> = withContext(Dispatchers.IO) {
        // ... existing implementation ...
        emptyList()
    }
    
    /**
     * Get the root media categories as MediaBrowserCompat.MediaItem objects
     */
    suspend fun getRootCategories(): List<MediaBrowserCompat.MediaItem> = withContext(Dispatchers.IO) {
        val categories = ArrayList<MediaBrowserCompat.MediaItem>()
        
        // Add standard categories
        MediaCategory.values().forEach { category ->
            val mediaItem = MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(category.id)
                    .setTitle(category.title)
                    .build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            )
            categories.add(mediaItem)
        }
        
        return@withContext categories
    }
    
    /**
     * Get user playlists
     */
    suspend fun getUserPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        // In a real implementation, you would fetch from the API
        // For now, return mock data
        return@withContext getMockPlaylists()
    }
    
    /**
     * Get liked songs from the user's library
     */
    suspend fun getLikedSongs(): List<Track> = withContext(Dispatchers.IO) {
        // In a real implementation, you would fetch from the API
        // For now, return mock data
        return@withContext getMockTracks()
    }
    
    /**
     * Get recently played tracks
     */
    suspend fun getRecentlyPlayed(): List<Track> = withContext(Dispatchers.IO) {
        // In a real implementation, you would fetch from the API
        // For now, return mock data
        return@withContext getMockTracks()
    }
    
    /**
     * Get mock playlists for development and testing
     */
    fun getMockPlaylists(): List<Playlist> {
        return listOf(
            Playlist(
                id = "playlist-001",
                title = "Top Hits",
                description = "The most popular songs right now",
                artworkUri = Uri.parse("https://is1-ssl.mzstatic.com/image/thumb/Features115/v4/77/1c/de/771cdedb-4c59-c2c9-4c48-549d0d227075/source/200x200bb.jpeg"),
                trackCount = 50,
                curatorName = "Apple Music"
            ),
            Playlist(
                id = "playlist-002",
                title = "Chill Mix",
                description = "A personalized mix of relaxing music",
                artworkUri = Uri.parse("https://is2-ssl.mzstatic.com/image/thumb/Features125/v4/f5/c3/98/f5c398a1-2136-3165-1bed-22aab3f0828f/source/200x200bb.jpeg"),
                trackCount = 25,
                curatorName = "Apple Music for You"
            ),
            Playlist(
                id = "playlist-003",
                title = "Favorites",
                description = "Your favorite tracks",
                artworkUri = Uri.parse("https://is3-ssl.mzstatic.com/image/thumb/Features115/v4/15/a1/52/15a1527e-b31a-abdb-0fc9-36f23c4f7389/source/200x200bb.jpeg"),
                trackCount = 100,
                curatorName = "Your Library"
            )
        )
    }
    
    /**
     * Get playlist tracks by playlist ID
     */
    suspend fun getPlaylistTracks(playlistId: String): List<Track> = withContext(Dispatchers.IO) {
        // In a real implementation, you would fetch from the API based on playlistId
        // For now, return mock data
        return@withContext getMockTracks()
    }
    
    /**
     * Get album tracks by album ID
     */
    suspend fun getAlbumTracks(albumId: String): List<Track> = withContext(Dispatchers.IO) {
        // In a real implementation, you would fetch from the API based on albumId
        // For now, return mock data
        return@withContext getMockTracks()
    }
    
    /**
     * Get mock tracks for development and testing
     */
    fun getMockTracks(): List<Track> {
        return listOf(
            Track(
                id = "track-001",
                title = "Blinding Lights",
                albumName = "After Hours",
                artistName = "The Weeknd",
                albumId = "album-001",
                artistId = "artist-001",
                artworkUri = Uri.parse("https://is4-ssl.mzstatic.com/image/thumb/Music125/v4/32/75/b8/3275b838-5bc8-781d-5324-21f1b1829251/source/450x450bb.jpeg"),
                streamUrl = null,
                previewUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview115/v4/77/17/99/771799f8-acb3-ae06-873c-9d4474eac29d/mzaf_10332520797539877415.plus.aac.p.m4a",
                durationMs = 200000,
                isExplicit = false,
                subtitle = "The Weeknd • After Hours"
            ),
            Track(
                id = "track-002",
                title = "Stay",
                albumName = "Stay - Single",
                artistName = "Kid Laroi & Justin Bieber",
                albumId = "album-002",
                artistId = "artist-002",
                artworkUri = Uri.parse("https://is3-ssl.mzstatic.com/image/thumb/Music115/v4/a5/a0/90/a5a0903a-9fcb-b3e2-2639-76e27e3a6c0a/source/450x450bb.jpeg"),
                streamUrl = null,
                previewUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview122/v4/cb/e5/cd/cbe5cd65-d4fb-c44f-d80b-6712e82855c9/mzaf_18382862308326827799.plus.aac.p.m4a",
                durationMs = 141000,
                isExplicit = false,
                subtitle = "Kid Laroi & Justin Bieber • Stay - Single"
            ),
            Track(
                id = "track-003",
                title = "As It Was",
                albumName = "Harry's House",
                artistName = "Harry Styles",
                albumId = "album-003",
                artistId = "artist-003",
                artworkUri = Uri.parse("https://is4-ssl.mzstatic.com/image/thumb/Music126/v4/2a/19/fb/2a19fb85-2f70-9e44-f2a9-82abe679b88e/source/450x450bb.jpeg"),
                streamUrl = null,
                previewUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview122/v4/96/7e/ea/967eea9f-839a-ba30-3e36-8d97c0ff412a/mzaf_8991881941319874779.plus.aac.p.m4a",
                durationMs = 167000,
                isExplicit = false,
                subtitle = "Harry Styles • Harry's House"
            )
        )
    }
    
    /**
     * Get music genres/categories
     */
    suspend fun getGenres(): List<Genre> = withContext(Dispatchers.IO) {
        // In a real implementation, you would fetch from the API
        // For now, return mock data
        return@withContext getMockGenres()
    }
    
    /**
     * Get mock genres for development and testing
     */
    fun getMockGenres(): List<Genre> {
        return listOf(
            Genre(
                id = "genre_pop",
                title = "Pop",
                artworkUri = Uri.parse("https://is5-ssl.mzstatic.com/image/thumb/Features115/v4/cc/62/0c/cc620ccb-c9d7-3d0d-7b9e-cca5202bef5a/source/200x200bb.jpeg")
            ),
            Genre(
                id = "genre_rock",
                title = "Rock",
                artworkUri = Uri.parse("https://is2-ssl.mzstatic.com/image/thumb/Features125/v4/f5/c5/17/f5c51723-3aa5-96db-0c93-e5f489c79063/source/200x200bb.jpeg")
            ),
            Genre(
                id = "genre_hiphop",
                title = "Hip-Hop/Rap",
                artworkUri = Uri.parse("https://is1-ssl.mzstatic.com/image/thumb/Features115/v4/be/18/65/be1865f4-e81c-8609-7415-6ec583faccc1/source/200x200bb.jpeg")
            ),
            Genre(
                id = "genre_electronic",
                title = "Electronic",
                artworkUri = Uri.parse("https://is4-ssl.mzstatic.com/image/thumb/Features115/v4/97/77/1d/97771d00-c9e0-302f-890e-174f9b869760/source/200x200bb.jpeg")
            ),
            Genre(
                id = "genre_jazz",
                title = "Jazz",
                artworkUri = Uri.parse("https://is2-ssl.mzstatic.com/image/thumb/Features115/v4/33/64/ea/3364ea1b-30c5-1cdd-77cd-9771edb4c242/source/200x200bb.jpeg")
            )
        )
    }
    
    /**
     * Search the Apple Music catalog
     */
    suspend fun search(query: String): SearchResults = withContext(Dispatchers.IO) {
        // In a real implementation, you would fetch from the API
        // For now, return mock search results
        return@withContext getMockSearchResults(query)
    }
    
    /**
     * Get mock search results for development and testing
     */
    private fun getMockSearchResults(query: String): SearchResults {
        val lowerCaseQuery = query.lowercase()
        
        // Filter mock data based on the query
        val matchingTracks = getMockTracks()
            .filter { 
                it.title.lowercase().contains(lowerCaseQuery) || 
                it.artistName.lowercase().contains(lowerCaseQuery) ||
                it.albumName.lowercase().contains(lowerCaseQuery)
            }
        
        // Create mock playlists matching query
        val matchingPlaylists = getMockPlaylists()
            .filter {
                it.title.lowercase().contains(lowerCaseQuery) ||
                it.description?.lowercase()?.contains(lowerCaseQuery) == true
            }
        
        // Create mock albums matching query
        val matchingAlbums = listOf(
            Album(
                id = "album-001",
                title = "After Hours",
                artistName = "The Weeknd",
                artistId = "artist-001",
                releaseYear = 2020,
                artworkUri = Uri.parse("https://is4-ssl.mzstatic.com/image/thumb/Music125/v4/32/75/b8/3275b838-5bc8-781d-5324-21f1b1829251/source/450x450bb.jpeg"),
                trackCount = 14
            ),
            Album(
                id = "album-002",
                title = "Stay - Single",
                artistName = "Kid Laroi & Justin Bieber",
                artistId = "artist-002",
                releaseYear = 2021,
                artworkUri = Uri.parse("https://is3-ssl.mzstatic.com/image/thumb/Music115/v4/a5/a0/90/a5a0903a-9fcb-b3e2-2639-76e27e3a6c0a/source/450x450bb.jpeg"),
                trackCount = 1
            ),
            Album(
                id = "album-003", 
                title = "Harry's House",
                artistName = "Harry Styles",
                artistId = "artist-003",
                releaseYear = 2022,
                artworkUri = Uri.parse("https://is4-ssl.mzstatic.com/image/thumb/Music126/v4/2a/19/fb/2a19fb85-2f70-9e44-f2a9-82abe679b88e/source/450x450bb.jpeg"),
                trackCount = 13
            )
        ).filter {
            it.title.lowercase().contains(lowerCaseQuery) ||
            it.artistName.lowercase().contains(lowerCaseQuery)
        }
        
        // Create mock artists matching query
        val matchingArtists = listOf(
            Artist(
                id = "artist-001",
                title = "The Weeknd",
                artworkUri = Uri.parse("https://is1-ssl.mzstatic.com/image/thumb/Features115/v4/e5/5c/49/e55c496c-864f-e46e-8218-2c5e6053b5b6/source/450x450bb.jpeg"),
                genreNames = listOf("Pop", "R&B")
            ),
            Artist(
                id = "artist-002",
                title = "Kid Laroi",
                artworkUri = Uri.parse("https://is2-ssl.mzstatic.com/image/thumb/Music125/v4/b4/7d/d4/b47dd495-86e7-3933-6a2a-1d913ddf39c5/source/450x450bb.jpeg"),
                genreNames = listOf("Hip-Hop", "Pop")
            ),
            Artist(
                id = "artist-003",
                title = "Harry Styles",
                artworkUri = Uri.parse("https://is4-ssl.mzstatic.com/image/thumb/Features115/v4/73/94/62/73946233-287f-5c48-e5a0-b5a7d105865d/source/450x450bb.jpeg"),
                genreNames = listOf("Pop", "Rock")
            )
        ).filter {
            it.title.lowercase().contains(lowerCaseQuery) ||
            it.genreNames.any { genre -> genre.lowercase().contains(lowerCaseQuery) }
        }
        
        return SearchResults(
            songs = if (matchingTracks.isNotEmpty()) ApiResponse(matchingTracks) else null,
            albums = if (matchingAlbums.isNotEmpty()) ApiResponse(matchingAlbums) else null,
            playlists = if (matchingPlaylists.isNotEmpty()) ApiResponse(matchingPlaylists) else null,
            artists = if (matchingArtists.isNotEmpty()) ApiResponse(matchingArtists) else null
        )
    }
    
    /**
     * Release resources when no longer needed
     */
    fun release() {
        if (::playbackManager.isInitialized) {
            playbackManager.release()
        }
    }
}
