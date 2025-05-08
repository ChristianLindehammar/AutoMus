package com.lindehammarkonsult.automus.shared.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.lindehammarkonsult.automus.shared.api.AppleMusicApiService
import com.lindehammarkonsult.automus.shared.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

private const val TAG = "AppleMusicRepository"
private const val TOKEN_PREF_NAME = "apple_music_auth"
private const val TOKEN_KEY = "access_token"
private const val TOKEN_EXPIRY_KEY = "expiry_time"
private const val REFRESH_TOKEN_KEY = "refresh_token"

/**
 * Repository for Apple Music data
 */
class AppleMusicRepository(private val context: Context) {
    // API Service
    private val apiService: AppleMusicApiService
    
    // Auth token
    private var authToken: AppleMusicToken? = null
    
    // Playback state
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState

    init {
        // Set up OkHttp client
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
        
        // Try to load saved token
        loadAuthToken()
    }
    
    /**
     * Load the stored authentication token from secure storage
     */
    private fun loadAuthToken() {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
                
            val sharedPreferences = EncryptedSharedPreferences.create(
                context,
                TOKEN_PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            val token = sharedPreferences.getString(TOKEN_KEY, null)
            val expiryTime = sharedPreferences.getLong(TOKEN_EXPIRY_KEY, 0)
            val refreshToken = sharedPreferences.getString(REFRESH_TOKEN_KEY, null)
            
            if (token != null && expiryTime > System.currentTimeMillis()) {
                authToken = AppleMusicToken(
                    accessToken = token,
                    expiresAt = expiryTime,
                    refreshToken = refreshToken
                )
                Log.d(TAG, "Loaded auth token from storage")
            } else {
                Log.d(TAG, "No valid auth token found in storage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading auth token: ${e.message}")
        }
    }
    
    /**
     * Save authentication token to secure storage
     */
    private fun saveAuthToken(token: AppleMusicToken) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
                
            val sharedPreferences = EncryptedSharedPreferences.create(
                context,
                TOKEN_PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            sharedPreferences.edit()
                .putString(TOKEN_KEY, token.accessToken)
                .putLong(TOKEN_EXPIRY_KEY, token.expiresAt)
                .putString(REFRESH_TOKEN_KEY, token.refreshToken)
                .apply()
            
            authToken = token
            Log.d(TAG, "Saved auth token to storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving auth token: ${e.message}")
        }
    }
    
    /**
     * Check if the user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return authToken != null && authToken!!.expiresAt > System.currentTimeMillis()
    }
    
    /**
     * Set a new authentication token
     */
    fun setAuthToken(token: AppleMusicToken) {
        saveAuthToken(token)
    }
    
    /**
     * Clear the authentication token (logout)
     */
    fun clearAuthToken() {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
                
            val sharedPreferences = EncryptedSharedPreferences.create(
                context,
                TOKEN_PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            sharedPreferences.edit().clear().apply()
            authToken = null
            Log.d(TAG, "Cleared auth token")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing auth token: ${e.message}")
        }
    }
    
    /**
     * Get the authentication header string
     */
    private fun getAuthHeader(): String {
        return "Bearer ${authToken?.accessToken ?: ""}"
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
     * Get user playlists
     */
    suspend fun getUserPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        if (!isAuthenticated()) return@withContext emptyList<Playlist>()
        
        try {
            val response = apiService.getUserPlaylists(getAuthHeader())
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
            val response = apiService.getLikedSongs(getAuthHeader())
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
            val response = apiService.getRecentlyPlayed(getAuthHeader())
            if (response.isSuccessful && response.body() != null) {
                return@withContext response.body()!!.data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching recently played: ${e.message}")
        }
        
        return@withContext emptyList<Track>()
    }
    
    /**
     * Get featured playlists
     */
    suspend fun getFeaturedPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        if (!isAuthenticated()) return@withContext emptyList<Playlist>()
        
        try {
            val response = apiService.getFeaturedPlaylists(getAuthHeader())
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
            val response = apiService.getPlaylistTracks(getAuthHeader(), playlistId)
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
            val response = apiService.getAlbumTracks(getAuthHeader(), albumId = albumId)
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
            val response = apiService.search(getAuthHeader(), query = query)
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
            val response = apiService.getGenres(getAuthHeader())
            if (response.isSuccessful && response.body() != null) {
                return@withContext response.body()!!.data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching genres: ${e.message}")
        }
        
        return@withContext emptyList<Genre>()
    }
    
    /**
     * Set current track and queue
     */
    fun setCurrentQueue(tracks: List<Track>, startPosition: Int = 0) {
        val currentTrack = if (tracks.isNotEmpty() && startPosition < tracks.size) {
            tracks[startPosition]
        } else {
            null
        }
        
        _playbackState.value = _playbackState.value.copy(
            currentTrack = currentTrack,
            queue = tracks,
            position = 0
        )
    }
    
    /**
     * Update playback state
     */
    fun updatePlaybackState(
        isPlaying: Boolean? = null,
        position: Long? = null,
        shuffleMode: Boolean? = null,
        repeatMode: Int? = null
    ) {
        val current = _playbackState.value
        _playbackState.value = current.copy(
            isPlaying = isPlaying ?: current.isPlaying,
            position = position ?: current.position,
            shuffleMode = shuffleMode ?: current.shuffleMode,
            repeatMode = repeatMode ?: current.repeatMode
        )
    }
    
    /**
     * Skip to the next track in the queue
     */
    fun skipToNext() {
        val current = _playbackState.value
        val queue = current.queue
        
        if (queue.isEmpty() || current.currentTrack == null) return
        
        val currentIndex = queue.indexOf(current.currentTrack)
        if (currentIndex < 0) return
        
        val nextIndex = if (currentIndex >= queue.size - 1) 0 else currentIndex + 1
        _playbackState.value = current.copy(
            currentTrack = queue[nextIndex],
            position = 0
        )
    }
    
    /**
     * Skip to the previous track in the queue
     */
    fun skipToPrevious() {
        val current = _playbackState.value
        val queue = current.queue
        
        if (queue.isEmpty() || current.currentTrack == null) return
        
        val currentIndex = queue.indexOf(current.currentTrack)
        if (currentIndex < 0) return
        
        val previousIndex = if (currentIndex <= 0) queue.size - 1 else currentIndex - 1
        _playbackState.value = current.copy(
            currentTrack = queue[previousIndex],
            position = 0
        )
    }
    
    /**
     * Skip to a specific position in the queue
     */
    fun skipToPosition(position: Int) {
        val current = _playbackState.value
        val queue = current.queue
        
        if (queue.isEmpty() || position < 0 || position >= queue.size) return
        
        _playbackState.value = current.copy(
            currentTrack = queue[position],
            position = 0
        )
    }
    
    // Mock data for development and testing
    //region Mock Data Methods
    fun getMockPlaylists(): List<Playlist> {
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
                isExplicit = false
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
                isExplicit = false
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
                isExplicit = false
            )
        )
    }
    //endregion
}