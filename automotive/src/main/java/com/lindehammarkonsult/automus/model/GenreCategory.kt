package com.lindehammarkonsult.automus.model

/**
 * Represents a music genre category
 */
data class GenreCategory(
    val id: String,
    val name: String,
    val mediaId: String // ID used for browsing genre content
) {
    companion object {
    }
}
