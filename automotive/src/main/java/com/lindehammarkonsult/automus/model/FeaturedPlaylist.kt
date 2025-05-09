package com.lindehammarkonsult.automus.model

import android.support.v4.media.MediaBrowserCompat

/**
 * Represents a featured playlist in Apple Music
 */
data class FeaturedPlaylist(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverArtUrl: String?,
    val mediaId: String // ID used for playback
) {
    companion object {
        // Helper method to convert from MediaBrowser MediaItem
        fun fromMediaItem(mediaItem: MediaBrowserCompat.MediaItem): FeaturedPlaylist {
            val description = mediaItem.description
            return FeaturedPlaylist(
                id = description.mediaId ?: "",
                title = description.title.toString(),
                subtitle = description.subtitle.toString(),
                coverArtUrl = description.iconUri?.toString(),
                mediaId = description.mediaId ?: ""
            )
        }
    }
}
