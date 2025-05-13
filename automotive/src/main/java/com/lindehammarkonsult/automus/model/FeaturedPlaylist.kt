package com.lindehammarkonsult.automus.model

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
    }
}
