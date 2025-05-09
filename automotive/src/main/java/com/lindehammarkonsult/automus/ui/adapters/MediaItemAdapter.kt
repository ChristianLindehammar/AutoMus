package com.lindehammarkonsult.automus.ui.adapters

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lindehammarkonsult.automus.R
import com.lindehammarkonsult.automus.databinding.ItemMediaBinding

/**
 * Adapter for displaying media items in a RecyclerView
 */
class MediaItemAdapter(
    private val onItemClick: (MediaBrowserCompat.MediaItem) -> Unit
) : ListAdapter<MediaBrowserCompat.MediaItem, MediaItemAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMediaBinding.inflate(
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
        private val binding: ItemMediaBinding,
        private val onItemClick: (MediaBrowserCompat.MediaItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mediaItem: MediaBrowserCompat.MediaItem) {
            val description: MediaDescriptionCompat = mediaItem.description

            // Set title and subtitle
            binding.itemTitle.text = description.title
            binding.itemSubtitle.text = description.subtitle

            // Load artwork if available
            description.iconUri?.let { uri ->
                Glide.with(binding.itemImage.context)
                    .load(uri)
                    .placeholder(R.drawable.album_art_placeholder)
                    .into(binding.itemImage)
            } ?: run {
                // Use default artwork if no URI is available
                binding.itemImage.setImageResource(R.drawable.album_art_placeholder)
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
            return oldItem.mediaId == newItem.mediaId &&
                   oldItem.description.title == newItem.description.title &&
                   oldItem.description.subtitle == newItem.description.subtitle
        }
    }
}