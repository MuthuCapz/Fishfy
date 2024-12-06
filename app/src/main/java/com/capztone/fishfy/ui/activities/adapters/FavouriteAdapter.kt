package com.capztone.fishfy.ui.activities.adapters

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.FavouritesItemBinding
import com.capztone.fishfy.ui.activities.models.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class FavouriteAdapter(
    private val context: Context,
    private val menuItems: MutableList<MenuItem>,
    private val onItemClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<FavouriteAdapter.FavouriteViewHolder>() {

    private lateinit var database: DatabaseReference

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouriteViewHolder {
        val binding = FavouritesItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavouriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavouriteViewHolder, position: Int) {
        holder.bind(menuItems[position])
    }

    override fun getItemCount(): Int = menuItems.size

    inner class FavouriteViewHolder(private val binding: FavouritesItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(menuItem: MenuItem) {
            binding.apply {
                val foodName = menuItem.foodName?.joinToString(separator = " / ") ?: ""
                val parts = foodName.split("/")
                if (parts.size == 2) {
                    favFoodNameTextView.text = parts[0].trim()
                    favFoodNameTextView1.text = parts[1].trim()
                } else {
                    favFoodNameTextView.text = foodName
                    favFoodNameTextView1.text = ""
                }
                val priceWithPrefix = "₹${menuItem.foodPrice}"
                favPriceTextView.text = priceWithPrefix
                Glide.with(context)
                    .load(Uri.parse(menuItem.foodImage))
                    .into(favImageView)

                // Set the favorite icon based on the stored favorite state
                heart.setOnClickListener {
                    updateFavoriteState(menuItem)
                }

                if (menuItem.stock == "Out Of Stock") {
                    binding.root.alpha = 0.4f // Make item semi-transparent
                    binding.outOfStockLabel.visibility = View.VISIBLE
                    binding.outOfStockLabel.text = "Out Of Stock"
                    binding.root.isClickable = false
                    binding.root.isFocusable = false

                } else {
                    binding.root.alpha = 1.0f // Normal opacity
                    binding.outOfStockLabel.visibility = View.GONE
                    binding.root.isClickable = true
                    binding.root.isFocusable = true
                    binding.root.setOnClickListener {
                        val position = adapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            openDetailsActivity(it, position)
                        }
                    }
                }
                }
        }

        private fun openDetailsActivity(view: View, position: Int) {
            val menuItem = menuItems[position]
            val bundle = Bundle().apply {
                putString("MenuItemName", menuItem.foodName?.getOrNull(0) ?: "")
                putString("MenuItemPrice", menuItem.foodPrice)
                putString("MenuItemDescription", menuItem.foodDescription)
                putString("MenuItemImage", menuItem.foodImage)
                putString("MenuQuantity", menuItem.productQuantity)
                putString("Shop Id",menuItem.path)
                putString("key", menuItem.key)
            }

            // Navigate to the details fragment using NavController
            view.findNavController().navigate(R.id.action_favouritefragment_to_detailsFragment, bundle)
        }

        private fun updateFavoriteState(menuItem: MenuItem) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                database = FirebaseDatabase.getInstance().getReference("Favourite").child(userId)
                menuItem.firebaseKey?.let { key ->
                    // Update Firebase favorite state to false
                    database.child(key).child("favorite").setValue(false).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Remove item from local list only if it matches the clicked item
                            val position = menuItems.indexOf(menuItem)
                            if (position != -1) {
                                removeItem(position)
                            }
                        }
                    }
                }
            }
        }

        private fun removeItem(position: Int) {
            if (position >= 0 && position < menuItems.size) {
                menuItems.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, menuItems.size)
            }
        }
    }
}