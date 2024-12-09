package com.capztone.fishfy.ui.activities.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.ExploreShopBinding
import com.google.firebase.database.*

class ExploreShopAdapter(
    private var shopList: List<String>,
    private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<ExploreShopAdapter.ShopViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(shopName: String)
    }

    inner class ShopViewHolder(val binding: ExploreShopBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(shopName: String, locality: String, shopDisplayName: String, deliveryAmount: String, deliveryType: String) {
            binding.shopTextView.text = shopName
            binding.localityTextView.text = locality
            binding.shopname.text = shopDisplayName
            binding.upto.text = deliveryAmount
            binding.freedelivery.text = deliveryType

            binding.root.setOnClickListener {
                itemClickListener.onItemClick(shopName)
            }

            // Set different images for different positions
            when (absoluteAdapterPosition % 3) {
                0 -> binding.shopImageView.setImageResource(R.drawable.freshfish)
                1 -> binding.shopImageView.setImageResource(R.drawable.fishman)
                2 -> binding.shopImageView.setImageResource(R.drawable.pickles)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val binding = ExploreShopBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShopViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val shopName = shopList[holder.bindingAdapterPosition]

        val databaseReference = FirebaseDatabase.getInstance().reference

        // Fetch shop display name directly from Firebase
        databaseReference.child("ShopNames").child(shopName).child("shopName")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val shopDisplayName = dataSnapshot.getValue(String::class.java) ?: ""

                    if (shopDisplayName.isEmpty()) {
                        // Remove the shop from the list immediately if not found
                        val currentPosition = holder.bindingAdapterPosition
                        if (currentPosition != RecyclerView.NO_POSITION) {
                            removeItem(currentPosition)
                        }
                    } else {
                        // Fetch locality directly from Firebase
                        databaseReference.child("ShopLocations").child(shopName).child("locality")
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    val locality = dataSnapshot.getValue(String::class.java) ?: ""

                                    // Fetch delivery details directly from Firebase
                                    fetchDeliveryDetails(shopName) { deliveryAmount, deliveryType ->
                                        if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                                            holder.bind(
                                                shopName,
                                                locality,
                                                shopDisplayName,
                                                deliveryAmount,
                                                deliveryType
                                            )
                                        }
                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {}
                            })
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    private fun removeItem(position: Int) {
        // Remove the item from the list and notify the adapter immediately
        val mutableShopList = shopList.toMutableList()
        mutableShopList.removeAt(position)
        shopList = mutableShopList
        notifyItemRemoved(position)

        // Optionally, notify the adapter that the list has been changed
        notifyItemRangeChanged(position, shopList.size)
    }

    override fun getItemCount(): Int = shopList.size

    private fun fetchDeliveryDetails(shopName: String, callback: (String, String) -> Unit) {
        val databaseReference = FirebaseDatabase.getInstance().reference

        databaseReference.child("Delivery Details").child(shopName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val deliveryAmount = dataSnapshot.child("Delivery Amount").getValue(String::class.java) ?: ""
                    val deliveryType = dataSnapshot.child("Delivery Type").getValue(String::class.java) ?: ""
                    callback(deliveryAmount, deliveryType)
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    // Use DiffUtil to update only changed items
    fun setShopList(newShopList: List<String>) {
        val diffCallback = ShopDiffCallback(shopList, newShopList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        shopList = newShopList
        diffResult.dispatchUpdatesTo(this)
    }

    class ShopDiffCallback(
        private val oldList: List<String>,
        private val newList: List<String>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
