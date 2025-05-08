package com.lindehammarkonsult.automus.ui.adapters

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lindehammarkonsult.automus.R
import com.lindehammarkonsult.automus.databinding.ItemMediaCategoryBinding

/**
 * Adapter for displaying media items in a RecyclerView
 */
class MediaCategoryAdapter(
    private val onItemClick: (MediaBrowserCompat.MediaItem) -> Unit
) : ListAdapter<MediaBrowserCompat.MediaItem, MediaCategoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMediaCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMediaCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: MediaBrowserCompat.MediaItem) {
            val description = item.description

            binding.titleText.text = description.title
            binding.subtitleText.text = description.subtitle

            // Load artwork if available
            val iconUri = description.iconUri
            if (iconUri != null) {
                Glide.with(binding.mediaIcon)
                    .load(iconUri)
                    .placeholder(R.drawable.ic_library)
                    .into(binding.mediaIcon)
            } else {
                // Set default icon based on media type
                val icon = when {
                    item.mediaId?.startsWith("album_") == true -> R.drawable.ic_library
                    item.mediaId?.startsWith("artist_") == true -> R.drawable.ic_library
                    item.mediaId?.startsWith("playlist_") == true -> R.drawable.ic_library
                    else -> R.drawable.ic_library
                }
                binding.mediaIcon.setImageResource(icon)
            }
            
            // Show right chevron only for browsable items
            binding.chevronIcon.visibility = if (item.isBrowsable) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<MediaBrowserCompat.MediaItem>() {
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
            return oldItem.description.title == newItem.description.title &&
                   oldItem.description.subtitle == newItem.description.subtitle &&
                   oldItem.description.iconUri == newItem.description.iconUri
        }
    }
}