package com.lindehammarkonsult.automus.ui.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lindehammarkonsult.automus.R
import com.lindehammarkonsult.automus.databinding.ItemGenrePillBinding

/**
 * Adapter for displaying genre category pills 
 */
class GenrePillAdapter(
    private val onItemClick: (GenrePill) -> Unit
) : ListAdapter<GenrePillAdapter.GenrePill, GenrePillAdapter.ViewHolder>(DiffCallback) {

    data class GenrePill(
        val id: String,
        val name: String,
        val mediaId: String,
        val color: Int  // Color resource ID
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGenrePillBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val genrePill = getItem(position)
        holder.bind(genrePill)
    }

    class ViewHolder(
        private val binding: ItemGenrePillBinding,
        private val onItemClick: (GenrePill) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(genrePill: GenrePill) {
            binding.genreButton.text = genrePill.name
            binding.genreButton.backgroundTintList = android.content.res.ColorStateList.valueOf(genrePill.color)
            
            binding.genreButton.setOnClickListener {
                onItemClick(genrePill)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<GenrePill>() {
        override fun areItemsTheSame(oldItem: GenrePill, newItem: GenrePill): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GenrePill, newItem: GenrePill): Boolean {
            return oldItem == newItem
        }
    }
}
