package com.lindehammarkonsult.automus.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lindehammarkonsult.automus.R
import com.lindehammarkonsult.automus.databinding.ItemGenreCategoryBinding
import com.lindehammarkonsult.automus.model.GenreCategory

class GenreCategoryAdapter(
    private val onGenreClicked: (GenreCategory) -> Unit
) : ListAdapter<GenreCategory, GenreCategoryAdapter.GenreViewHolder>(GenreDiffCallback()) {

    // Predefined colors for the genre backgrounds
    private val genreColors = listOf(
        Color.parseColor("#8A2BE2"), // Purple
        Color.parseColor("#FF375F"), // Apple Red
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#2196F3"), // Blue
        Color.parseColor("#FF9800"), // Orange
        Color.parseColor("#E91E63")  // Pink
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val binding = ItemGenreCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GenreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        holder.bind(getItem(position), genreColors[position % genreColors.size])
    }

    inner class GenreViewHolder(
        private val binding: ItemGenreCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onGenreClicked(getItem(position))
                }
            }
        }

        fun bind(genre: GenreCategory, backgroundColor: Int) {
            binding.genreName.text = genre.name
            binding.genreName.bringToFront()
            binding.genreBackground.setBackgroundColor(backgroundColor)
        }
    }

    private class GenreDiffCallback : DiffUtil.ItemCallback<GenreCategory>() {
        override fun areItemsTheSame(oldItem: GenreCategory, newItem: GenreCategory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GenreCategory, newItem: GenreCategory): Boolean {
            return oldItem == newItem
        }
    }
}
