package com.lindehammarkonsult.automus.shared.model

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import android.net.Uri

/**
 * Extension function to convert a Track to a Media3 MediaItem
 */
fun Track.toMedia3Item(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artistName)
        .setAlbumTitle(albumName)
        .setSubtitle(subtitle)
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .setArtworkUri(artworkUri)
        .build()

    return MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(metadata)
        .setUri(streamUrl?.let { Uri.parse(it) } ?: artworkUri)
        .build()
}

/**
 * Extension functions for MediaCategory class
 */
fun MediaCategory.toMedia3Item(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .build()

    return MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(metadata)
        .build()
}

/**
 * Extension function for Playlist
 */
fun Playlist.toMedia3Item(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setArtworkUri(artworkUri)
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .build()

    return MediaItem.Builder()
        .setMediaId("playlist_$id")
        .setMediaMetadata(metadata)
        .build()
}

/**
 * Extension function for Album
 */
fun Album.toMedia3Item(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artistName)
        .setSubtitle(subtitle)
        .setArtworkUri(artworkUri)
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .build()

    return MediaItem.Builder()
        .setMediaId("album_$id")
        .setMediaMetadata(metadata)
        .build()
}

/**
 * Extension function for Artist
 */
fun Artist.toMedia3Item(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setArtworkUri(artworkUri)
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .build()

    return MediaItem.Builder()
        .setMediaId("artist_$id")
        .setMediaMetadata(metadata)
        .build()
}

/**
 * Extension function for Genre
 */
fun Genre.toMedia3Item(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtworkUri(artworkUri)
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .build()

    return MediaItem.Builder()
        .setMediaId("genre_$id")
        .setMediaMetadata(metadata)
        .build()
}
