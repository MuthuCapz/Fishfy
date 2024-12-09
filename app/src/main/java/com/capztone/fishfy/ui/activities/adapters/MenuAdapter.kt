package com.capztone.fishfy.ui.activities.adapters

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import com.capztone.fishfy.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.fishfy.databinding.MenuItemBinding
import com.capztone.fishfy.ui.activities.Utils.ToastHelper
import com.capztone.fishfy.ui.activities.models.CartItems
import com.capztone.fishfy.ui.activities.models.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class  MenuAdapter(
    private var menuItems: MutableList<MenuItem>,
     private val context: Context
) : RecyclerView.Adapter<MenuAdapter.ViewHolder>() {
    private val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
    private val sharedPreferences = context.getSharedPreferences("CartPrefs", Context.MODE_PRIVATE)
    private val seenFoodNames = mutableSetOf<String>()
    private val favoriteItemsKey = "favoriteItems"
    private var foodDescription: String? = null

    fun updateMenuItems(newMenuItems: List<MenuItem>) {
        menuItems.clear()
        seenFoodNames.clear()
        newMenuItems.forEach { menuItem ->
            if (!seenFoodNames.contains(menuItem.foodName?.getOrNull(0) ?: "")) {
                menuItems.add(menuItem)
                seenFoodNames.add(menuItem.foodName?.getOrNull(0) ?: "")
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val menuItem = menuItems[position]
         holder.bind(menuItem)
    }

    fun updateData(newItems: List<MenuItem>) {
        menuItems = newItems.toMutableList()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return menuItems.size
    }

    inner class ViewHolder(private val binding: MenuItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val quantityLiveData = MutableLiveData<Int>()

        init {
            val defaultQuantity = 0
            quantityLiveData.value = defaultQuantity

            binding.quantityy.setOnClickListener {
                val currentQuantity = quantityLiveData.value ?: defaultQuantity
                if (currentQuantity == 0) {
                    quantityLiveData.value = 1 // Set to 1 when clicking "Add" for the first time
                    binding.plusImageButton.visibility = View.VISIBLE
                    binding.minusImageButton.visibility = View.VISIBLE
                    addItemToCart()
                }
            }

            binding.plusImageButton.setOnClickListener {
                val currentQuantity = quantityLiveData.value ?: defaultQuantity
                val newQuantity = currentQuantity + 1
                quantityLiveData.value = newQuantity
                saveQuantity(binding.menuFoodName1.text.toString(), newQuantity)
                addItemToCart()
            }



            binding.minusImageButton.setOnClickListener {
                val currentQuantity = quantityLiveData.value ?: 0
                if (currentQuantity > 0) {
                    val newQuantity = currentQuantity - 1
                    quantityLiveData.value = newQuantity
                    saveQuantity(binding.menuFoodName1.text.toString(), newQuantity)
                    if (newQuantity == 0) {
                        removeItemFromCart()
                    } else {
                        updateCartItemQuantity(newQuantity)
                        addItemToCart1()
                    }
                }
            }


            // Ensure context is a LifecycleOwner before observing
            (context as? LifecycleOwner)?.let { lifecycleOwner ->
                quantityLiveData.observe(lifecycleOwner, Observer { quantity ->
                    updateQuantityText(quantity)
                })
            }

            // Initialize visibility based on initial quantity
            updateQuantityText(defaultQuantity)
        }

        private fun updateCartItemQuantity(newQuantity: Int) {
            val database = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val foodPriceString = binding.menuPrice1.text.toString().removePrefix("₹")
            val foodPricePerUnit = foodPriceString.toDoubleOrNull() ?: 0.0
            val foodName = binding.menuFoodName1.text.toString() + binding.menuFoodName2.text.toString()

            if (userId != null && foodName.isNotEmpty()) {
                val cartItemsRef = database.child("user").child(userId).child("cartItems")
                val productQuantity = menuItems[adapterPosition].productQuantity

                // Extract numeric value and unit from productQuantity
                val numericValue = productQuantity?.filter { it.isDigit() }?.toDoubleOrNull() ?: 0.0
                val unit = productQuantity?.filter { it.isLetter() }?.lowercase()

                // Convert product quantity to grams if necessary
                val productQuantityInGrams = when (unit) {
                    "kg" -> numericValue * 1000
                    "g" -> numericValue
                    else -> numericValue // Default to grams if no unit is found
                }

                // Calculate the new UnitQuantity
                val newUnitQuantity = (newQuantity * productQuantityInGrams).toInt()
                val unitString = "g"
                val unitQuantityString = "${newUnitQuantity}${unitString}"

                cartItemsRef.orderByChild("foodName").equalTo(foodName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                for (cartSnapshot in dataSnapshot.children) {
                                    // Fetch the current food price per unit

                                    // Calculate the new total price
                                    val newTotalPrice = newQuantity * foodPricePerUnit

                                    // Update foodQuantity, UnitQuantity, and foodPrice
                                    cartSnapshot.ref.child("foodQuantity").setValue(newQuantity)
                                    cartSnapshot.ref.child("UnitQuantity").setValue(unitQuantityString)
                                    cartSnapshot.ref.child("foodPrice").setValue(newTotalPrice.toString())
                                        .addOnSuccessListener {

                                        }
                                        .addOnFailureListener {
                                            ToastHelper.showCustomToast(
                                                context,
                                                "Failed to update item details"
                                            )
                                        }
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("NearItemAdapter", "Failed to update cart item", databaseError.toException())
                        }
                    })
            }
        }


        private fun removeItemFromCart() {
            val database = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val foodName =
                binding.menuFoodName1.text.toString() + binding.menuFoodName2.text.toString()

            if (userId != null && foodName.isNotEmpty()) {
                val cartItemsRef = database.child("user").child(userId).child("cartItems")

                cartItemsRef.orderByChild("foodName").equalTo(foodName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // Item in cart, remove it
                                for (cartSnapshot in dataSnapshot.children) {
                                    cartSnapshot.ref.removeValue()
                                        .addOnSuccessListener {

                                        }.addOnFailureListener {
                                            ToastHelper.showCustomToast(
                                                context,
                                                "Failed to remove item"
                                            )
                                        }
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            // Handle error
                        }
                    })
            }
        }

        private fun updateQuantityText(quantity: Int) {
            binding.quantityy.text = if (quantity > 0) quantity.toString() else "Add"
            binding.plusImageButton.visibility = if (quantity > 0) View.VISIBLE else View.GONE
            binding.minusImageButton.visibility = if (quantity > 0) View.VISIBLE else View.GONE
        }

        private fun saveQuantity(foodName: String, quantity: Int) {
            val editor = sharedPreferences.edit()
            editor.putInt("quantity_$foodName", quantity)
            editor.apply()
        }

        private fun getSavedQuantity(foodName: String): Int {
            return sharedPreferences.getInt("quantity_$foodName", 0)
        }

        private fun addItemToCart() {
            val currentShopName = binding.shopname.text.toString()
            val cartItemsRef = currentUserID?.let {
                FirebaseDatabase.getInstance().reference.child("user").child(it).child("cartItems")
            }

            if (cartItemsRef != null) {
                cartItemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        var differentShopFound = false

                        for (itemSnapshot in dataSnapshot.children) {
                            val shopName = itemSnapshot.child("path").value as String
                            if (shopName != currentShopName) {
                                differentShopFound = true
                                break
                            }
                        }

                        if (differentShopFound) {
                            val context = binding.root.context
                            val layoutInflater = LayoutInflater.from(context)
                            val customLayout = layoutInflater.inflate(R.layout.shop_dialog, null)

                            val dialog = AlertDialog.Builder(context, R.style.CustomDialogg)
                                .setView(customLayout)
                                .setCancelable(false)
                                .create()

                            // Set background to transparent
                            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                            val positiveButton =
                                customLayout.findViewById<AppCompatButton>(R.id.dialog_positive_button)
                            val negativeButton =
                                customLayout.findViewById<AppCompatButton>(R.id.dialog_negative_button)

                            positiveButton.setOnClickListener {
                                cartItemsRef.removeValue().addOnSuccessListener {
                                    addItemToCartWithoutCheck()
                                    dialog.dismiss()
                                }
                            }

                            negativeButton.setOnClickListener {
                                quantityLiveData.value = 0
                                updateQuantityText(0)
                                dialog.dismiss()
                            }
                            dialog.setOnKeyListener { _, keyCode, event ->
                                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                                    ToastHelper.showCustomToast(context, "Please select Yes or No")
                                    true
                                } else {
                                    false
                                }
                            }
                            dialog.show()
                        } else {
                            addItemToCartWithoutCheck()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle error
                    }
                })
            }
        }


        private fun addItemToCartWithoutCheck() {
            val database = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val shopName = binding.shopname.text.toString()
            val menuItem = menuItems.getOrNull(adapterPosition) ?: return
            val foodName = binding.menuFoodName1.text.toString() + binding.menuFoodName2.text.toString()
            val foodPriceString = binding.menuPrice1.text.toString().removePrefix("₹")
            val foodPricePerUnit = foodPriceString.toDoubleOrNull() ?: 0.0
            val foodImage = binding.nearImage.tag.toString()
            val quantity = quantityLiveData.value ?: 1
            val key = menuItem.key
            val productQuantity = menuItem.productQuantity

            // Extract numeric value from productQuantity (e.g., "500g", "1kg")
            val numericValue = productQuantity?.filter { it.isDigit() }?.toDoubleOrNull() ?: 0.0
            val unit = productQuantity?.filter { it.isLetter() }?.lowercase()

            // Convert to grams if necessary and prepare the unit string
            val (productQuantityInGrams, unitString) = when (unit) {
                "kg" -> numericValue * 1000 to "g"
                "g" -> numericValue to "g"
                else -> numericValue to "g" // Default to grams if the unit is not recognized
            }

            // Calculate UnitQuantity (foodQuantity * productQuantityInGrams)
            val unitQuantity = (quantity * productQuantityInGrams).toInt()
            val unitQuantityString = "${unitQuantity}${unitString}" // e.g., "500g"

            // Calculate the total price
            val totalPrice = quantity * foodPricePerUnit

            if (userId != null && foodName.isNotEmpty() && foodPriceString.isNotEmpty() && foodImage.isNotEmpty()) {
                val cartItemsRef = database.child("user").child(userId).child("cartItems")
                val CartItemAddTime = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())

                cartItemsRef.orderByChild("foodName").equalTo(foodName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // Item already in cart, update quantity and total price
                                for (cartSnapshot in dataSnapshot.children) {
                                    val quantityValue = cartSnapshot.child("foodQuantity").getValue(Any::class.java)
                                    val currentQuantity = when (quantityValue) {
                                        is Long -> quantityValue.toInt()
                                        is String -> quantityValue.toIntOrNull() ?: 0
                                        else -> 0
                                    }
                                    val newQuantity = currentQuantity + 1
                                    val newUnitQuantity = (newQuantity * productQuantityInGrams).toInt()
                                    val newUnitQuantityString = "${newUnitQuantity}${unitString}"
                                    val newTotalPrice = newQuantity * foodPricePerUnit

                                    cartSnapshot.ref.child("foodQuantity").setValue(newQuantity)
                                    cartSnapshot.ref.child("UnitQuantity").setValue(newUnitQuantityString)
                                    cartSnapshot.ref.child("foodPrice").setValue(newTotalPrice.toString())
                                        .addOnSuccessListener {

                                        }.addOnFailureListener {
                                            ToastHelper.showCustomToast(
                                                context,
                                                "Failed to update item quantity and price"
                                            )
                                        }
                                }
                            } else {
                                // Item not in cart, add new item
                                val cartItem = CartItems(
                                    shopName,
                                    foodName,
                                    totalPrice.toString(),
                                    menuItem.foodDescription,
                                    foodImage,
                                    quantity,
                                    CartItemAddTime,
                                    menuItem.key,
                                )
                                if (key != null) {
                                    cartItemsRef.child(key).setValue(cartItem)
                                        .addOnSuccessListener {
                                            // Store UnitQuantity as well
                                            cartItemsRef.child(key).child("UnitQuantity").setValue(unitQuantityString)
                                                .addOnSuccessListener {

                                                }
                                                .addOnFailureListener {
                                                    ToastHelper.showCustomToast(
                                                        context,
                                                        "Failed to add UnitQuantity to cart"
                                                    )
                                                }
                                        }
                                        .addOnFailureListener {
                                            ToastHelper.showCustomToast(
                                                context,
                                                "Failed to add item to cart"
                                            )
                                        }
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            // Handle error
                            ToastHelper.showCustomToast(
                                context,
                                "Database error: ${databaseError.message}"
                            )
                        }
                    })
            }
        }


        private fun addItemToCart1() {
            val database = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val shopName = binding.shopname.text.toString()
            val foodName =
                binding.menuFoodName1.text.toString() + binding.menuFoodName2.text.toString()
            val foodPrice = binding.menuPrice1.text.toString().removePrefix("₹")
            val foodImage = binding.nearImage.tag.toString()
            val quantity = quantityLiveData.value ?: 1

            if (userId != null && foodName.isNotEmpty() && foodPrice.isNotEmpty() && foodImage.isNotEmpty()) {
                val cartItemsRef = database.child("user").child(userId).child("cartItems")

                cartItemsRef.orderByChild("foodName").equalTo(foodName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // Item already in cart, update quantity
                                for (cartSnapshot in dataSnapshot.children) {
                                    val currentQuantity =
                                        cartSnapshot.child("foodQuantity").getValue(Int::class.java)
                                            ?: 1
                                    val newQuantity = currentQuantity - 1
                                    cartSnapshot.ref.child("foodQuantity").setValue(newQuantity)
                                        .addOnSuccessListener {

                                        }.addOnFailureListener {
                                            ToastHelper.showCustomToast(
                                                context,
                                                "Failed to update item quantity"
                                            )
                                        }
                                }
                            }

                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            // Handle error
                        }
                    })
            }
        }


        fun bind(menuItem: MenuItem) {
            if (menuItem != null) {
                binding.apply {
                    menuFoodName1.text = menuItem.foodName?.getOrNull(0) ?: ""
                    menuFoodName2.text = menuItem.foodName?.getOrNull(1) ?: ""

                    // Check if foodPrice is non-null and not empty
                    menuPrice1.text = "₹${menuItem.foodPrice}"
                    Qty.text = menuItem.productQuantity


                    shopname.text = menuItem.path ?: ""
                    foodDescription = menuItem.foodDescription

                    // Check if foodImage is non-null and not empty
                    Glide.with(context).load(Uri.parse(menuItem.foodImage)).into(nearImage)
                    nearImage.tag = menuItem.foodImage

                    val foodName = menuItem.foodName?.getOrNull(0) ?: ""
                    val savedQuantity = getSavedQuantity(foodName)

                    quantityLiveData.value = savedQuantity
                    updateQuantityText(savedQuantity)
                }
                binding.root.setOnClickListener {
                    // Handle item click
                }


                val quantityRef = FirebaseDatabase.getInstance().reference
                    .child("user")
                    .child(currentUserID!!)
                    .child("cartItems")
                    .orderByChild("foodName")
                    .equalTo(menuItem.foodName?.getOrNull(0) ?: "")

                quantityRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (itemSnapshot in snapshot.children) {
                                val foodQuantity =
                                    itemSnapshot.child("foodQuantity").getValue(Int::class.java)
                                        ?: 0
                                quantityLiveData.value = foodQuantity
                            }
                        } else {
                            quantityLiveData.value = 0
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("NearItemAdapter", "Database error: ${error.message}")
                    }
                })



                if (menuItem.stock == "Out Of Stock") {
                    binding.root.alpha = 0.4f // Make item semi-transparent
                    binding.outOfStockLabel.visibility = View.VISIBLE
                    binding.outOfStockLabel.text = "Out Of Stock"
                    binding.root.isClickable = false
                    binding.root.isFocusable = false
                    binding.minusImageButton.isClickable=false
                    binding.plusImageButton.isClickable=false
                    binding.quantityy.isClickable=false
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



        private fun saveFavoriteState(itemId: String, isFavorite: Boolean) {
            val favoritesSet =
                sharedPreferences.getStringSet(favoriteItemsKey, mutableSetOf()) ?: mutableSetOf()
            if (isFavorite) {
                favoritesSet.add(itemId)
            } else {
                favoritesSet.remove(itemId)
            }
            sharedPreferences.edit().putStringSet(favoriteItemsKey, favoritesSet).apply()
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
            view.findNavController().navigate(R.id.action_homeFragment_to_detailsFragment, bundle)
        }

    }
}