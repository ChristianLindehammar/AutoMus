package com.lindehammarkonsult.automus.shared.utils

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Utility class to help with converting between legacy MediaBrowserCompat.MediaItem and Media3 MediaItem
 */
object MediaItemConverter {

    /**
     * Convert a legacy MediaBrowserCompat.MediaItem to a Media3 MediaItem
     */
    fun convertToMedia3Item(legacyItem: MediaBrowserCompat.MediaItem): MediaItem {
        val description = legacyItem.description
        
        val isPlayable = legacyItem.isBrowsable.not()
        val isBrowsable = legacyItem.isBrowsable
        
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(description.title)
            .setSubtitle(description.subtitle)
            .setIsPlayable(isPlayable)
            .setIsBrowsable(isBrowsable)
        
        // Add artwork if available
        description.iconUri?.let { metadataBuilder.setArtworkUri(it) }
        
        // Add extras if available
        description.extras?.let { extras ->
            val mediaExtras = Bundle()
            extras.keySet()?.forEach { key ->
                mediaExtras.putString(key, extras.get(key)?.toString())
            }
            metadataBuilder.setExtras(mediaExtras)
        }
        
        return MediaItem.Builder()
            .setMediaId(description.mediaId ?: "")
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }
    
    /**
     * Convert a Media3 MediaItem to a legacy MediaBrowserCompat.MediaItem
     */
    fun convertToLegacyItem(media3Item: MediaItem): MediaBrowserCompat.MediaItem {
        val metadata = media3Item.mediaMetadata
        
        val descriptionBuilder = MediaDescriptionCompat.Builder()
            .setMediaId(media3Item.mediaId)
            .setTitle(metadata.title)
            .setSubtitle(metadata.subtitle)
        
        // Add artwork URI if available
        metadata.artworkUri?.let { descriptionBuilder.setIconUri(it) }
        
        // Add extras if available
        metadata.extras?.let { descriptionBuilder.setExtras(it) }
        
        val flags = if (metadata.isPlayable == true) {
            MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        } else {
            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        }
        
        return MediaBrowserCompat.MediaItem(descriptionBuilder.build(), flags)
    }
    
    /**
     * Convert a list of legacy MediaBrowserCompat.MediaItem to a list of Media3 MediaItem
     */
    fun convertToMedia3Items(legacyItems: List<MediaBrowserCompat.MediaItem>): List<MediaItem> {
        return legacyItems.map { convertToMedia3Item(it) }
    }
    
    /**
     * Convert a list of Media3 MediaItem to a list of legacy MediaBrowserCompat.MediaItem
     */
    fun convertToLegacyItems(media3Items: List<MediaItem>): List<MediaBrowserCompat.MediaItem> {
        return media3Items.map { convertToLegacyItem(it) }
    }
}
