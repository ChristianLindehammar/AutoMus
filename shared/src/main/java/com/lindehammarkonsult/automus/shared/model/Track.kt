package com.lindehammarkonsult.automus.shared.model

import android.net.Uri
import android.os.Parcelable
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
    val artworkUri: Uri? = null
) : Parcelable