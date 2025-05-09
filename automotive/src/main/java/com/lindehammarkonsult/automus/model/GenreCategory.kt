package com.lindehammarkonsult.automus.model

import android.support.v4.media.MediaBrowserCompat

/**
 * Represents a music genre category
 */
data class GenreCategory(
    val id: String,
    val name: String,
    val mediaId: String // ID used for browsing genre content
) {
    companion object {
        // Helper method to convert from MediaBrowser MediaItem
        fun fromMediaItem(mediaItem: MediaBrowserCompat.MediaItem): GenreCategory {
            val description = mediaItem.description
            return GenreCategory(
                id = description.mediaId ?: "",
                name = description.title.toString(),
                mediaId = description.mediaId ?: ""
            )
        }
    }
}
