package com.lindehammarkonsult.automus.ui.adapters

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lindehammarkonsult.automus.R
import com.lindehammarkonsult.automus.databinding.ItemSongLikedBinding // Changed binding

/**
 * Adapter for displaying liked songs in a RecyclerView
 */
class LikedSongsAdapter(
    private val onItemClick: (MediaBrowserCompat.MediaItem) -> Unit,
    private val onLikeClick: (MediaBrowserCompat.MediaItem) -> Unit // Changed: Only MediaItem
) : ListAdapter<MediaBrowserCompat.MediaItem, LikedSongsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSongLikedBinding.inflate( // Changed binding
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick, onLikeClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mediaItem = getItem(position)
        holder.bind(mediaItem)
    }

    class ViewHolder(
        private val binding: ItemSongLikedBinding, // Changed binding
        private val onItemClick: (MediaBrowserCompat.MediaItem) -> Unit,
        private val onLikeClick: (MediaBrowserCompat.MediaItem) -> Unit // Changed: Only MediaItem
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mediaItem: MediaBrowserCompat.MediaItem) {
            val description = mediaItem.description

            binding.songTitle.text = description.title
            binding.songArtist.text = description.subtitle ?: "" // Assuming subtitle is artist

            description.iconUri?.let {
                Glide.with(binding.songAlbumArt.context)
                    .load(it)
                    .placeholder(R.drawable.album_art_placeholder)
                    .into(binding.songAlbumArt)
            } ?: run {
                binding.songAlbumArt.setImageResource(R.drawable.album_art_placeholder)
            }

            // Handle liked state - This is a placeholder.
            // You'll need a way to determine if a song is liked, perhaps from the MediaItem itself or a ViewModel.
            // For now, it's always shown as liked as per the static layout.
            binding.songLikedButton.setImageResource(R.drawable.ic_heart_filled)
            binding.songLikedButton.setColorFilter(ContextCompat.getColor(itemView.context, R.color.apple_red))

            binding.songLikedButton.setOnClickListener {
                // Implement like/unlike logic here. For now, it just calls the callback.
                // You might want to toggle the icon and update the liked state in your ViewModel.
                onLikeClick(mediaItem) // Changed: Call with only mediaItem
            }

            binding.root.setOnClickListener {
                onItemClick(mediaItem)
            }
            
            // It's good practice to also make the play arrow clickable if it's intended to play the song directly
            binding.songPlayArrow.setOnClickListener {
                 onItemClick(mediaItem) // Or a more specific play action
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
            return oldItem.mediaId == newItem.mediaId &&
                   oldItem.description.title == newItem.description.title &&
                   oldItem.description.subtitle == newItem.description.subtitle &&
                   oldItem.description.iconUri == newItem.description.iconUri
            // Add a check for liked state if it becomes part of MediaItem or a separate data source
        }
    }
}
