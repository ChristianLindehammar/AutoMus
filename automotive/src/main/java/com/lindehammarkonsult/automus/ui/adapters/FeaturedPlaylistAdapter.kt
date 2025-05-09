package com.lindehammarkonsult.automus.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lindehammarkonsult.automus.R
import com.lindehammarkonsult.automus.databinding.ItemFeaturedPlaylistBinding
import com.lindehammarkonsult.automus.model.FeaturedPlaylist

class FeaturedPlaylistAdapter(
    private val onPlaylistClicked: (FeaturedPlaylist) -> Unit
) : ListAdapter<FeaturedPlaylist, FeaturedPlaylistAdapter.PlaylistViewHolder>(PlaylistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemFeaturedPlaylistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlaylistViewHolder(
        private val binding: ItemFeaturedPlaylistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPlaylistClicked(getItem(position))
                }
            }
        }

        fun bind(playlist: FeaturedPlaylist) {
            binding.playlistTitle.text = playlist.title
            binding.playlistSubtitle.text = playlist.subtitle

            // Load cover art image
            Glide.with(binding.root.context)
                .load(playlist.coverArtUrl)
                .placeholder(R.drawable.album_art_placeholder)
                .into(binding.playlistCoverImage)
        }
    }

    private class PlaylistDiffCallback : DiffUtil.ItemCallback<FeaturedPlaylist>() {
        override fun areItemsTheSame(oldItem: FeaturedPlaylist, newItem: FeaturedPlaylist): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FeaturedPlaylist, newItem: FeaturedPlaylist): Boolean {
            return oldItem == newItem
        }
    }
}
