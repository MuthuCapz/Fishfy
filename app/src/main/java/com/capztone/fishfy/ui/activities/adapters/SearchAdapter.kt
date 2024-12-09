package com.capztone.fishfy.ui.activities.adapters



import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.SearchItemBinding
import com.capztone.fishfy.ui.activities.models.MenuItem

class SearchAdapter(
    private var menuItems: List<MenuItem>,
    private val context: Context,
    private val noResultsTextView: TextView // Add this parameter
) : RecyclerView.Adapter<SearchAdapter.MenuViewHolder>() {

    private var filteredMenuItems: List<MenuItem> = menuItems

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = SearchItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(filteredMenuItems[position])
    }

    override fun getItemCount(): Int = filteredMenuItems.size

    inner class MenuViewHolder(private val binding: SearchItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    openDetailsFragment(it, position)
                }
            }
        }

        private fun openDetailsFragment(view: View, position: Int) {
            val menuItem = filteredMenuItems[position]
            val bundle = Bundle().apply {
                putString("MenuItemName", menuItem.foodName?.getOrNull(0) ?: "")
                putString("MenuItemPrice", menuItem.foodPrice)
                putString("MenuItemDescription", menuItem.foodDescription)
                putString("MenuItemImage", menuItem.foodImage)
                putString("MenuQuantity", menuItem.productQuantity)
                putString("key", menuItem.key)



                // Extracting the Shop ID from the path
                val pathSegments = menuItem.path!!.split("/")
                val shopId = if (pathSegments.size > 1 && pathSegments[0] == "Shops") {
                    pathSegments[1] // This will give you "SHOP1001"
                } else {
                    "Unknown Shop" // Default value if parsing fails
                }
                putString("Shop Id", shopId) // Pass the shop ID here
            }

            // Navigate to the details fragment using NavController
            view.findNavController().navigate(R.id.action_searchFragment_to_detailsFragment, bundle)
        }


        fun bind(menuItem: MenuItem) {
            binding.apply {
                val foodName =
                    menuItem.foodName?.toString()?.replace("[", "")?.replace("]", "") ?: ""
                val slashIndex = foodName.indexOf("/")
                if (slashIndex != -1 && slashIndex < foodName.length - 1) {
                    menuFoodName.text = foodName.substring(0, slashIndex + 1).trim()
                    menuFoodName1.text = foodName.substring(slashIndex + 1).trim()
                } else {
                    menuFoodName.text = foodName
                    menuFoodName1.text = ""
                }

                val priceWithPrefix = "₹${menuItem.foodPrice}"
                menuPrice.text = priceWithPrefix

                val path = menuItem.path
                if (!path.isNullOrEmpty()) {
                    Log.d("SearchAdapter", "Path: $path")
                    val pathSegments = path.split("/")

                    val shopName = if (pathSegments.size > 1 && pathSegments[0] == "Shops") {
                        pathSegments[1]
                    } else {
                        "Unknown Shop"
                    }
                    shopname.text = shopName
                } else {
                    Log.d("SearchAdapter", "Path is null or empty")
                    shopname.text = "Unknown Shop"
                }

                // Load the image if the URL is valid
                if (!menuItem.foodImage.isNullOrEmpty()) {
                    val url = Uri.parse(menuItem.foodImage)
                    Glide.with(context).load(url).into(menuImage)
                } else {
                    // Set a placeholder image if `foodImage` is null or empty
                    menuImage.setImageResource(R.drawable.fish1) // Replace with your placeholder image resource
                }
            }
        }
    }
        fun filter(query: String) {
            filteredMenuItems = if (query.isEmpty()) {
                menuItems
            } else {
                menuItems.filter { menuItem ->
                    // Check if the English name contains the query
                    menuItem.foodName?.getOrNull(0)?.contains(query, ignoreCase = true) ?: false
                }
            }
            notifyDataSetChanged()

            // Show/hide the noResultsTextView based on the filtered list size
            if (filteredMenuItems.isEmpty()) {
                noResultsTextView.visibility = View.VISIBLE
            } else {
                noResultsTextView.visibility = View.GONE
            }
        }
    }
