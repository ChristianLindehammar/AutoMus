package com.lindehammarkonsult.automus.ui.adapters

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lindehammarkonsult.automus.R
import com.lindehammarkonsult.automus.databinding.ItemPlaylistHorizontalBinding // Changed binding

/**
 * Adapter for displaying playlists in a horizontal RecyclerView
 */
class PlaylistAdapter(
    private val onItemClick: (MediaBrowserCompat.MediaItem) -> Unit
) : ListAdapter<MediaBrowserCompat.MediaItem, PlaylistAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlaylistHorizontalBinding.inflate( // Changed binding
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mediaItem = getItem(position)
        holder.bind(mediaItem)
    }

    class ViewHolder(
        private val binding: ItemPlaylistHorizontalBinding, // Changed binding
        private val onItemClick: (MediaBrowserCompat.MediaItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mediaItem: MediaBrowserCompat.MediaItem) {
            val description = mediaItem.description

            // Set title and subtitle (song count)
            binding.playlistTitle.text = description.title
            binding.playlistSongCount.text = description.subtitle ?: "" // Assuming subtitle is song count

            // Load artwork if available
            description.iconUri?.let { uri ->
                Glide.with(binding.playlistCoverImage.context)
                    .load(uri)
                    .placeholder(R.drawable.album_art_placeholder) // Ensure you have this placeholder
                    .into(binding.playlistCoverImage)
            } ?: run {
                // Use default icon for categories without artwork
                binding.playlistCoverImage.setImageResource(R.drawable.album_art_placeholder)
            }

            // Set click listener
            binding.root.setOnClickListener {
                onItemClick(mediaItem)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<MediaBrowserCompat.MediaItem>() {
        override fun areItemsTheSame(
            oldItem: MediaBrowserCompat.MediaItem,
            newItem: MediaBrowserCompat.MediaItem
        ): Boolean {
            return oldItem.mediaId == newItem.mediaId
        }

        override fun areContentsTheSame(
            oldItem: MediaBrowserCompat.MediaItem,
            newItem: MediaBrowserCompat.MediaItem
        ): Boolean {
            // Consider if more fields need to be checked for content equality
            return oldItem.mediaId == newItem.mediaId &&
                   oldItem.description.title == newItem.description.title &&
                   oldItem.description.subtitle == newItem.description.subtitle &&
                   oldItem.description.iconUri == newItem.description.iconUri
        }
    }
}
