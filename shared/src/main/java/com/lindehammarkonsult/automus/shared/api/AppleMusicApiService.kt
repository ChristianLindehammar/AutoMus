package com.lindehammarkonsult.automus.shared.api

import com.lindehammarkonsult.automus.shared.model.Album
import com.lindehammarkonsult.automus.shared.model.Artist
import com.lindehammarkonsult.automus.shared.model.Genre
import com.lindehammarkonsult.automus.shared.model.Playlist
import com.lindehammarkonsult.automus.shared.model.Track
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service interface for communicating with the Apple Music API
 */
interface AppleMusicApiService {
    
    /**
     * Get the user's playlists
     */
    @GET("me/library/playlists")
    suspend fun getUserPlaylists(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 25,
        @Query("offset") offset: Int = 0
    ): Response<ApiResponse<List<Playlist>>>
    
    /**
     * Get the user's liked/favorite songs
     */
    @GET("me/library/songs")
    suspend fun getLikedSongs(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 25,
        @Query("offset") offset: Int = 0
    ): Response<ApiResponse<List<Track>>>
    
    /**
     * Get the user's recently played tracks
     */
    @GET("me/recent/played")
    suspend fun getRecentlyPlayed(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 25
    ): Response<ApiResponse<List<Track>>>
    
    /**
     * Get tracks in a playlist
     */
    @GET("playlists/{playlistId}/tracks")
    suspend fun getPlaylistTracks(
        @Header("Authorization") token: String,
        @Path("playlistId") playlistId: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Response<ApiResponse<List<Track>>>
    
    /**
     * Get featured playlists
     */
    @GET("catalog/{storefront}/playlists/featured")
    suspend fun getFeaturedPlaylists(
        @Header("Authorization") token: String,
        @Path("storefront") storefront: String = "us",
        @Query("limit") limit: Int = 10
    ): Response<ApiResponse<List<Playlist>>>
    
    /**
     * Get tracks for an album
     */
    @GET("catalog/{storefront}/albums/{albumId}/tracks")
    suspend fun getAlbumTracks(
        @Header("Authorization") token: String,
        @Path("storefront") storefront: String = "us",
        @Path("albumId") albumId: String,
        @Query("limit") limit: Int = 100
    ): Response<ApiResponse<List<Track>>>
    
    /**
     * Get albums for an artist
     */
    @GET("catalog/{storefront}/artists/{artistId}/albums")
    suspend fun getArtistAlbums(
        @Header("Authorization") token: String,
        @Path("storefront") storefront: String = "us",
        @Path("artistId") artistId: String,
        @Query("limit") limit: Int = 25
    ): Response<ApiResponse<List<Album>>>
    
    /**
     * Search for music content
     */
    @GET("catalog/{storefront}/search")
    suspend fun search(
        @Header("Authorization") token: String,
        @Path("storefront") storefront: String = "us",
        @Query("term") query: String,
        @Query("types") types: String = "songs,albums,playlists,artists",
        @Query("limit") limit: Int = 10
    ): Response<SearchResponse>
    
    /**
     * Get music genres/categories
     */
    @GET("catalog/{storefront}/genres")
    suspend fun getGenres(
        @Header("Authorization") token: String,
        @Path("storefront") storefront: String = "us",
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<List<Genre>>>
    
    /**
     * Get recommended playlists for the user
     */
    @GET("me/recommendations")
    suspend fun getRecommendations(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 10
    ): Response<ApiResponse<List<Playlist>>>

    /**
     * Get new releases
     */
    @GET("catalog/{storefront}/charts")
    suspend fun getCharts(
        @Header("Authorization") token: String,
        @Path("storefront") storefront: String = "us",
        @Query("types") types: String = "albums",
        @Query("limit") limit: Int = 10
    ): Response<ChartsResponse>
}

/**
 * Standard API response wrapper
 */
data class ApiResponse<T>(
    val data: T,
    val next: String? = null
)

/**
 * Search response structure
 */
data class SearchResponse(
    val results: SearchResults
)

/**
 * Search results grouped by type
 */
data class SearchResults(
    val songs: ApiResponse<List<Track>>? = null,
    val albums: ApiResponse<List<Album>>? = null,
    val playlists: ApiResponse<List<Playlist>>? = null,
    val artists: ApiResponse<List<Artist>>? = null
)

/**
 * Charts response structure
 */
data class ChartsResponse(
    val results: ChartResults
)

/**
 * Charts results grouped by type
 */
data class ChartResults(
    val albums: List<ChartItem<Album>>? = null,
    val songs: List<ChartItem<Track>>? = null,
    val playlists: List<ChartItem<Playlist>>? = null
)

/**
 * Chart item wrapper
 */
data class ChartItem<T>(
    val name: String,
    val chart: String,
    val data: List<T>
)