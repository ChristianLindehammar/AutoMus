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
     * Get the root media categories
     */
    suspend fun getRootCategories(): List<MediaBrowserCompat.MediaItem> = withContext(Dispatchers.IO) {
        MediaCategory.values().map { category ->
            val description = MediaDescriptionCompat.Builder()
                .setMediaId(category.id)
                .setTitle(category.title)
                .build()
                
            MediaBrowserCompat.MediaItem(
                description, 
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            )
        }
    }
    
    /**
     * Set authentication token from service (compatibility method)
     */
    fun setAuthToken(token: AppleMusicToken) {
        // This method remains for backward compatibility with AppleMusicService
        authManager.tokenProvider.setUserToken(token.accessToken)
    }
    
    /**
     * Clear authentication token (logout)
     */
    fun clearAuthToken() {
        authManager.logout()
    }
    
    /**
     * Get the auth header for API calls
     */
    private fun getAuthHeader(): String {
        return "Bearer ${authManager.tokenProvider.getDeveloperToken()}"
    }
    
    /**
     * Get user playlists
     */
    suspend fun getUserPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        if (!isAuthenticated()) return@withContext emptyList<Playlist>()
        
        try {
            // Use the user's authentication token automatically managed by the SDK
            val response = apiService.getUserPlaylists("Bearer ${authManager.tokenProvider.getMusicUserToken()}")
            if (response.isSuccessful && response.body() != null) {
                return@withContext response.body()!!.data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user playlists: ${e.message}")
        }
        
        return@withContext emptyList<Playlist>()
    }
    
    /**
     * Get liked songs
     */
    suspend fun getLikedSongs(): List<Track> = withContext(Dispatchers.IO) {
        if (!isAuthenticated()) return@withContext emptyList<Track>()
        
        try {
            val response = apiService.getLikedSongs("Bearer ${authManager.tokenProvider.getMusicUserToken()}")
            if (response.isSuccessful && response.body() != null) {
                return@withContext response.body()!!.data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching liked songs: ${e.message}")
        }
        
        return@withContext emptyList<Track>()
    }
    
    /**
     * Get recently played tracks
     */
    suspend fun getRecentlyPlayed(): List<Track> = withContext(Dispatchers.IO) {
        if (!isAuthenticated()) return@withContext emptyList<Track>()
        
        try {
            val response = apiService.getRecentlyPlayed("Bearer ${authManager.tokenProvider.getMusicUserToken()}")
            if (response.isSuccessful && response.body() != null) {
                return@withContext response.body()!!.data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching recently played: ${e.message}")
        }
        
        return@withContext emptyList<Track>()
    }
    
    // Playback control methods that delegate to native SDK
    
    /**
     * Play a track by ID
     */
    fun playTrack(trackId: String) {
        playbackManager.playTrack(trackId)
    }
    
    /**
     * Play an album by ID
     */
    fun playAlbum(albumId: String) {
        playbackManager.playAlbum(albumId)
    }
    
    /**
     * Play a playlist by ID
     */
    fun playPlaylist(playlistId: String) {
        playbackManager.playPlaylist(playlistId)
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }
    
    /**
     * Skip to next track
     */
    fun skipToNext() {
        playbackManager.skipToNext()
    }
    
    /**
     * Skip to previous track
     */
    fun skipToPrevious() {
        playbackManager.skipToPrevious()
    }
    
    /**
     * Set repeat mode
     * @param mode 0: off, 1: repeat one, 2: repeat all
     */
    fun setRepeatMode(mode: RepeatMode) {
        playbackManager.setRepeatMode(mode)
    }
    
    /**
     * Set shuffle mode
     */
    fun setShuffleMode(shuffleEnabled: Boolean) {
        playbackManager.setShuffleMode(shuffleEnabled)
    }
    
    /**
     * Set current track and queue (compatible with old implementation)
     */
    fun setCurrentQueue(tracks: List<Track>, startPosition: Int = 0) {
        if (tracks.isEmpty()) return
        
        val track = tracks[startPosition]
        playTrack(track.id)
    }
    
    /**
     * Update playback state (compatible with old implementation)
     */
    fun updatePlaybackState(
        isPlaying: Boolean? = null,
        position: Long? = null,
        shuffleMode: Boolean? = null,
        repeatMode: RepeatMode? = null
    ) {
        // These operations are now handled by the SDK
        if (isPlaying != null) {
            if (isPlaying) {
                playbackManager.togglePlayPause()
            } else {
                playbackManager.togglePlayPause()
            }
        }
        
        if (position != null) {
            playbackManager.seekTo(position)
        }
        
        if (shuffleMode != null) {
            playbackManager.setShuffleMode(shuffleMode)
        }
        
        if (repeatMode != null) {
            playbackManager.setRepeatMode(repeatMode)
        }
    }
    
    /**
     * Skip to a specific position in the queue
     */
    fun skipToPosition(position: Int) {
        // This functionality is now handled internally by the SDK
    }
    
    // Continue with existing API methods
    
    /**
     * Get featured playlists
     */
    suspend fun getFeaturedPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        if (!isAuthenticated()) return@withContext emptyList<Playlist>()
        
        try {
            val response = apiService.getFeaturedPlaylists("Bearer ${authManager.tokenProvider.getMusicUserToken()}")
            if (response.isSuccessful && response.body() != null) {
                return@withContext response.body()!!.data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching featured playlists: ${e.message}")
        }
        
        return@withContext emptyList<Playlist>()
    }
    
    /**
     * Get tracks for a playlist
     */
    suspend fun getPlaylistTracks(playlistId: String): List<Track> = withContext(Dispatchers.IO) {
        if (!isAuthenticated()) return@withContext emptyList<Track>()
        
        try {
            val response = apiService.getPlaylistTracks("Bearer ${authManager.tokenProvider.getMusicUserToken()}", playlistId)
            if (response.isSuccessful && response.body() != null) {
                return@withContext response.body()!!.data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching playlist tracks: ${e.message}")
        }
        
        return@withContext emptyList<Track>()
    }
    
    /**
     * Get tracks for an album
     */
    suspend fun getAlbumTracks(albumId: String): List<Track> = withContext(Dispatchers.IO) {
        if (!isAuthenticated()) return@withContext emptyList<Track>()
        
        try {
            val response = apiService.getAlbumTracks("Bearer ${authManager.tokenProvider.getMusicUserToken()}", albumId = albumId)
            if (response.isSuccessful && response.body() != null) {
                return@withContext response.body()!!.data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching album tracks: ${e.message}")
        }
        
        return@withContext emptyList<Track>()
    }
    
    /**
     * Search for music content
     */
    suspend fun search(query: String): SearchResults = withContext(Dispatchers.IO) {
        if (!isAuthenticated()) {
            return@withContext SearchResults()
        }
        
        try {
            val response = apiService.search("Bearer ${authManager.tokenProvider.getMusicUserToken()}", query = query)
            if (response.isSuccessful && response.body() != null) {
                return@withContext response.body()!!.results
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing search: ${e.message}")
        }
        
        return@withContext SearchResults()
    }
    
    /**
     * Get available music categories/genres
     */
    suspend fun getGenres(): List<Genre> = withContext(Dispatchers.IO) {
        if (!isAuthenticated()) return@withContext emptyList<Genre>()
        
        try {
            val response = apiService.getGenres("Bearer ${authManager.tokenProvider.getMusicUserToken()}")
            if (response.isSuccessful && response.body() != null) {
                return@withContext response.body()!!.data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching genres: ${e.message}")
        }
        
        return@withContext emptyList<Genre>()
    }
    
    // Mock data for development and testing - still useful when not authenticated
    //region Mock Data Methods
    fun getMockPlaylists(): List<Playlist> {
        // ... existing mock data ...
        return listOf(
            Playlist(
                id = "playlist-001",
                title = "My Favorites",
                curatorName = "User",
                description = "Collection of favorite tracks",
                artworkUri = Uri.parse("https://is3-ssl.mzstatic.com/image/thumb/Features124/v4/6a/cf/64/6acf6440-13b3-3bce-0c01-7032fb4d626c/source/450x450bb.jpeg"),
                trackCount = 324
            ),
            Playlist(
                id = "playlist-002",
                title = "Driving Mix",
                curatorName = "User",
                description = "Great songs for the road",
                artworkUri = Uri.parse("https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/c8/59/3e/c8593ebb-6eb0-f3d9-cfe9-021840718d70/source/450x450bb.jpeg"),
                trackCount = 156
            ),
            Playlist(
                id = "playlist-003",
                title = "Workout Beats",
                curatorName = "User",
                description = "High energy tracks for exercise",
                artworkUri = Uri.parse("https://is5-ssl.mzstatic.com/image/thumb/Music114/v4/c4/5e/44/c45e444a-3c00-9a81-9cf9-825373563328/source/450x450bb.jpeg"),
                trackCount = 89
            )
        )
    }
    
    fun getMockTracks(): List<Track> {
        // ... existing mock data ...
        return listOf(
            Track(
                id = "song-001",
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
                subtitle = TODO()
            ),
            Track(
                id = "song-002",
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
                subtitle = TODO()
            ),
            Track(
                id = "song-003",
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
                subtitle = TODO()
            )
        )
    }
    //endregion
    
    /**
     * Release resources when no longer needed
     */
    fun release() {
        if (::playbackManager.isInitialized) {
            playbackManager.release()
        }
    }
}