package com.lindehammarkonsult.automus.shared.model

/**
 * Data class representing the current state of playback
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentTrack: Track? = null,
    val position: Long = 0L,
    val buffering: Boolean = false,
    val error: PlaybackError? = null,
    val repeatMode: Int = RepeatMode.NONE,
    val shuffleEnabled: Boolean = false,
    val queue: List<Track> = emptyList(),
    val shuffleMode: Boolean = false  // Added this property
)

/**
 * Enum representing repeat modes for playback
 */
enum class RepeatMode {
    NONE,
    ONE,
    ALL
}

/**
 * Data class representing playback errors
 */
data class PlaybackError(
    val code: Int,
    val message: String
)