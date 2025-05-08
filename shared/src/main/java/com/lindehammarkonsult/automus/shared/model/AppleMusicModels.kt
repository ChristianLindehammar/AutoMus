package com.lindehammarkonsult.automus.shared.model

import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.os.Bundle

/**
 * Root nodes of our music library
 */
enum class MediaCategory(val id: String, val title: String) {
    PLAYLISTS("playlists", "Playlists"),
    LIKED_SONGS("liked_songs", "Liked Songs"),
    RECENTLY_PLAYED("recently_played", "Recently Played"),
    BROWSE_CATEGORIES("browse_categories", "Browse by Category"),
    SEARCH("search", "Search")
}

/**
 * Represents an Apple Music authentication token
 */
data class AppleMusicToken(
    val accessToken: String,
    val expiresAt: Long, // Unix timestamp in milliseconds
    val tokenType: String = "Bearer",
    val refreshToken: String? = null
)

/**
 * Base class for music items
 */
sealed class MusicItem {
    abstract val id: String
    abstract val title: String
    abstract val subtitle: String?
    abstract val artworkUri: Uri?
    abstract val playable: Boolean
    abstract val browsable: Boolean
    
    fun toMediaItem(): MediaBrowserCompat.MediaItem {
        val extras = Bundle().apply {
            putString("ITEM_TYPE", this@MusicItem.javaClass.simpleName)
            if (this@MusicItem is Track) {
                putString("MEDIA_ID", id)
                putString("ALBUM_ID", albumId)
                putString("ARTIST_ID", artistId)
            }
        }
        
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(id)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setIconUri(artworkUri)
            .setExtras(extras)
            .build()
            
        return MediaBrowserCompat.MediaItem(
            description,
            if (playable) 
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE 
            else
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        )
    }
    
    fun toMetadata(): MediaMetadataCompat {
        val builder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
            
        subtitle?.let {
            builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, it)
        }
        
        artworkUri?.let {
            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, it.toString())
            builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, it.toString())
        }
        
        if (this is Track) {
            builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artistName)
            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, albumName)
            builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)
        }
        
        return builder.build()
    }
}

/**
 * Represents a music track
 */
data class Track(
    override val id: String,
    override val title: String,
    val albumName: String,
    val artistName: String,
    val albumId: String,
    val artistId: String,
    override val artworkUri: Uri?,
    val streamUrl: String?,
    val previewUrl: String?,
    val durationMs: Long,
    val isExplicit: Boolean = false
) : MusicItem() {
    override val subtitle: String = "$artistName • $albumName"
    override val playable: Boolean = true
    override val browsable: Boolean = false
}

/**
 * Represents a playlist
 */
data class Playlist(
    override val id: String,
    override val title: String,
    val curatorName: String,
    val description: String?,
    override val artworkUri: Uri?,
    val trackCount: Int
) : MusicItem() {
    override val subtitle: String? = "$trackCount songs"
    override val playable: Boolean = true
    override val browsable: Boolean = true
}

/**
 * Represents an album
 */
data class Album(
    override val id: String,
    override val title: String,
    val artistName: String,
    val artistId: String,
    val releaseYear: Int?,
    override val artworkUri: Uri?,
    val trackCount: Int,
    val isCompilation: Boolean = false
) : MusicItem() {
    override val subtitle: String = artistName
    override val playable: Boolean = true
    override val browsable: Boolean = true
}

/**
 * Represents an artist
 */
data class Artist(
    override val id: String,
    override val title: String,
    override val artworkUri: Uri?,
    val genreNames: List<String>
) : MusicItem() {
    override val subtitle: String? = genreNames.joinToString(", ")
    override val playable: Boolean = false
    override val browsable: Boolean = true
}

/**
 * Represents a genre or category
 */
data class Genre(
    override val id: String,
    override val title: String,
    override val artworkUri: Uri?
) : MusicItem() {
    override val subtitle: String? = null
    override val playable: Boolean = false
    override val browsable: Boolean = true
}

/**
 * Represents the state of the player and current queue
 */
data class PlaybackState(
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val isPlaying: Boolean = false,
    val position: Long = 0,
    val shuffleMode: Boolean = false,
    val repeatMode: Int = 0, // 0: OFF, 1: REPEAT_ONE, 2: REPEAT_ALL
    val isOffline: Boolean = false
)