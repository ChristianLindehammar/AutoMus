package com.lindehammarkonsult.automus.shared.model

import android.net.Uri
import android.os.Parcelable
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import kotlinx.parcelize.Parcelize

/**
 * Data class representing a music track
 */
@Parcelize
data class Track(
    val id: String,
    val title: String,
    val subtitle: String,
    val artistName: String,
    val albumName: String,
    val durationMs: Long,
    val artworkUri: Uri? = null,
    val albumId: String = "",
    val artistId: String = "",
    val streamUrl: String? = null,
    val previewUrl: String? = null,
    val isExplicit: Boolean = false
) : Parcelable

/**
 * Extension function to convert a Track to a MediaBrowserCompat.MediaItem
 */
fun Track.toMediaItem(): MediaBrowserCompat.MediaItem {
    val description = MediaDescriptionCompat.Builder()
        .setMediaId(id)
        .setTitle(title)
        .setSubtitle(subtitle)
        .setIconUri(artworkUri)
        .build()
    
    return MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
}