package com.lindehammarkonsult.automus.ui.adapters

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lindehammarkonsult.automus.R
import com.lindehammarkonsult.automus.databinding.ItemAlbumHorizontalBinding
// import com.bumptech.glide.Glide // Assuming Glide for image loading

class RecentlyPlayedAdapter(
    private val onItemClick: (MediaBrowserCompat.MediaItem) -> Unit
) : ListAdapter<MediaBrowserCompat.MediaItem, RecentlyPlayedAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlbumHorizontalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mediaItem = getItem(position)
        holder.bind(mediaItem)
    }

    inner class ViewHolder(private val binding: ItemAlbumHorizontalBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(mediaItem: MediaBrowserCompat.MediaItem) {
            binding.albumTitle.text = mediaItem.description.title
            binding.albumArtist.text = mediaItem.description.subtitle 
            // Assuming you have a placeholder, replace R.drawable.album_art_placeholder
            // Glide.with(binding.albumCoverImage.context)
            //     .load(mediaItem.description.iconUri) 
            //     .placeholder(R.drawable.album_art_placeholder) // Replace with your placeholder
            //     .into(binding.albumCoverImage)
            
            // For now, using a static placeholder if Glide is not set up
            // or if iconUri is null
            if (mediaItem.description.iconUri != null) {
                 // Glide.with(binding.albumCoverImage.context)
                 //    .load(mediaItem.description.iconUri)
                 //    .placeholder(R.drawable.album_art_placeholder)
                 //    .into(binding.albumCoverImage)
                 binding.albumCoverImage.setImageResource(R.drawable.album_art_placeholder) // Placeholder
            } else {
                binding.albumCoverImage.setImageResource(R.drawable.album_art_placeholder) // Placeholder
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MediaBrowserCompat.MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaBrowserCompat.MediaItem, newItem: MediaBrowserCompat.MediaItem): Boolean {
            return oldItem.mediaId == newItem.mediaId
        }

        override fun areContentsTheSame(oldItem: MediaBrowserCompat.MediaItem, newItem: MediaBrowserCompat.MediaItem): Boolean {
            return oldItem.description.title == newItem.description.title &&
                   oldItem.description.subtitle == newItem.description.subtitle &&
                   oldItem.description.iconUri == newItem.description.iconUri
        }
    }
}
