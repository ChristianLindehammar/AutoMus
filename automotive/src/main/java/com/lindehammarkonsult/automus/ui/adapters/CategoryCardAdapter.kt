package com.lindehammarkonsult.automus.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lindehammarkonsult.automus.databinding.ItemCategoryCardBinding

/**
 * Adapter for displaying top-level category cards (Liked Songs, Recently Played, Downloads)
 */
class CategoryCardAdapter(
    private val onItemClick: (CategoryCard) -> Unit
) : ListAdapter<CategoryCardAdapter.CategoryCard, CategoryCardAdapter.ViewHolder>(DiffCallback) {

    data class CategoryCard(
        val id: String,
        val title: String,
        val subtitle: String,
        val iconResId: Int,
        val mediaId: String? = null
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val categoryCard = getItem(position)
        holder.bind(categoryCard)
    }

    class ViewHolder(
        private val binding: ItemCategoryCardBinding,
        private val onItemClick: (CategoryCard) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(categoryCard: CategoryCard) {
            binding.title.text = categoryCard.title
            binding.subtitle.text = categoryCard.subtitle
            binding.icon.setImageResource(categoryCard.iconResId)
            
            binding.root.setOnClickListener {
                onItemClick(categoryCard)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<CategoryCard>() {
        override fun areItemsTheSame(oldItem: CategoryCard, newItem: CategoryCard): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CategoryCard, newItem: CategoryCard): Boolean {
            return oldItem == newItem
        }
    }
}
