package com.capztone.fishfy.ui.activities.adapters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.PreviousOrderBinding
import com.capztone.fishfy.ui.activities.models.DiscountItems
import com.capztone.fishfy.ui.activities.models.PreviousItem

// Use a sealed class to handle both PreviousItem and DiscountItem
sealed class OrderItem {
    data class Previous(val item: PreviousItem) : OrderItem()
    data class Discount(val item: DiscountItems) : OrderItem()
}

class PreviousOrderAdapter(
    private val buyHistory: MutableList<OrderItem>,
    private val context: Context
) : RecyclerView.Adapter<PreviousOrderAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PreviousOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = buyHistory[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return buyHistory.size
    }

    fun updateData(newData: MutableList<OrderItem>) {
        buyHistory.clear()
        buyHistory.addAll(filterUniqueItems(newData))
        notifyDataSetChanged()
    }

    private fun filterUniqueItems(data: List<OrderItem>): List<OrderItem> {
        val uniqueItems = mutableListOf<OrderItem>()
        val seenNames = mutableSetOf<String>()

        for (item in data) {
            val foodName = when (item) {
                is OrderItem.Previous -> item.item.foodName?.substringBefore("/")
                is OrderItem.Discount -> item.item.foodNames?.substringBefore("/")
            }
            if (foodName != null && foodName !in seenNames) {
                seenNames.add(foodName)
                uniqueItems.add(item)
            }
        }

        return uniqueItems
    }

    inner class ViewHolder(private val binding: PreviousOrderBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    openDetailsActivity(it, position)
                }
            }
        }

        fun bind(item: OrderItem) {
            with(binding) {
                when (item) {
                    is OrderItem.Previous -> {
                        loadFoodImage(item.item.foodImage)
                        previousFoodName1.text = item.item.foodName?.substringBefore("/") // Extract before slash
                        // Extract integer part of foodPrice and display it
                        val price = item.item.foodPrice?.toDoubleOrNull()?.toInt()
                        previousPrice1.text = "₹${price ?: 0}" // Handle null or invalid price
                        shopname.text = item.item.shopNames.toString()
                    }
                    is OrderItem.Discount -> {
                        loadFoodImage(item.item.foodImages)
                        previousFoodName1.text = item.item.foodNames?.substringBefore("/") // Extract before slash
                        // Extract integer part of foodPrices and display it
                        val price = item.item.foodPrices?.toDoubleOrNull()?.toInt()
                        previousPrice1.text = "₹${price ?: 0}" // Handle null or invalid price
                        shopname.text = "Discounted Item" // Custom label for discounts, adjust as needed
                    }
                }
            }
        }

        private fun openDetailsActivity(view: View, position: Int) {
            val orderItem = buyHistory.getOrNull(position)
            val bundle = Bundle()

            when (orderItem) {
                is OrderItem.Previous -> {
                    // Add PreviousItem details to the bundle
                    bundle.apply {
                        putString("MenuItemName", orderItem.item.foodName)
                        putString("MenuItemPrice", orderItem.item.foodPrice)
                        putString("MenuItemDescription", orderItem.item.foodDescription)
                        putString("MenuItemImage", orderItem.item.foodImage)
                        putString("Shop Id", orderItem.item.shopNames)
                        putString("key", orderItem.item.key)

                        // Process skuUnitQuantities to format as "2 kg"
                        val quantityString = orderItem.item.skuUnitQuantities // This is "2.0 kg" from Firebase
                        val formattedQuantity = if (quantityString?.contains(".0") == true) {
                            quantityString?.replace(".0", "")
                        } else {
                            quantityString
                        }
                        putString("MenuQuantity", formattedQuantity) // Store formatted string
                    }
                }


            is OrderItem.Discount -> {
                    // Add DiscountItem details to the bundle
                    bundle.apply {
                        putString("DiscountItemName", orderItem.item.foodNames)
                        putString("DiscountItemPrice", orderItem.item.foodPrices)
                        putString("DiscountItemDescription", orderItem.item.foodDescriptions)
                        putString("DiscountItemImage", orderItem.item.foodImages)

                    }
                }

                else -> {}
            }

            // Navigate to the details fragment using NavController
            view.findNavController().navigate(R.id.action_homeFragment_to_detailsFragment, bundle)
        }

        private fun loadFoodImage(imageUrl: String?) {
            Glide.with(binding.root)
                .load(imageUrl)
                .into(binding.previousImage)
        }
    }
}