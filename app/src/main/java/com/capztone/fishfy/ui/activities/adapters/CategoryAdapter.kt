package com.capztone.fishfy.ui.activities.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.CategoryItemBinding
import com.capztone.fishfy.ui.activities.models.Category

class CategoryAdapter(
    private val context: Context,
    private val categories: List<Category>,
    private val onItemClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION // Track the clicked item position

    inner class CategoryViewHolder(private val binding: CategoryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category, position: Int) {
            // Set category name
            binding.fishLabel.text = category.name

            // Change text color based on selection
            if (position == selectedPosition) {
                binding.fishLabel.setTextColor(context.getColor(R.color.navy))
            } else {
                binding.fishLabel.setTextColor(context.getColor(R.color.black))
            }

            // Load the image using Glide
            Glide.with(context)
                .load(category.imageUrl)
                .placeholder(R.drawable.fish1)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.fishIcon)

            // Set click listener for the item
            binding.root.setOnClickListener {
                Log.d("CategoryAdapter", "Item clicked: ${category.name}")
                selectedPosition = position
                notifyDataSetChanged() // Notify adapter to update the view
                onItemClick(category)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = CategoryItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position], position)
    }

    override fun getItemCount(): Int {
        return categories.size
    }
}
