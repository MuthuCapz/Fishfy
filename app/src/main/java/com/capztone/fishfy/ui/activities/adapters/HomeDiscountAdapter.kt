package com.capztone.fishfy.ui.activities.adapters

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.Global.putString
import android.util.Log
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
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.HomeDiscountBinding
import com.capztone.fishfy.ui.activities.Utils.ToastHelper
import com.capztone.fishfy.ui.activities.models.CartItems
import com.capztone.fishfy.ui.activities.models.DiscountItem
import com.capztone.fishfy.ui.activities.models.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeDiscountAdapter(
    private val context: Context
) : RecyclerView.Adapter<HomeDiscountAdapter.HomeDiscountViewHolder>() {

    private val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val locationsRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Addresses").child(userId)
    private var shopNames: List<String> = emptyList()
    private var discountItems: MutableList<DiscountItem> = mutableListOf()
    private val sharedPreferences = context.getSharedPreferences("CartPrefs", Context.MODE_PRIVATE)

    init {
        locationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val shopNameString = snapshot.child("Shop Id").value?.toString() ?: ""
                shopNames = shopNameString.split(",").map { it.trim() }
                fetchDiscountItems()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeDiscountAdapter", "Failed to read shop names: ${error.message}")
            }
        })
    }

    private fun fetchDiscountItems() {
        for (shopName in shopNames) {
            val shopRef = FirebaseDatabase.getInstance().getReference("Shops").child(shopName)
            shopRef.child("discount").addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    try {
                        // Fetch DiscountItem
                        val discountItem = snapshot.getValue(DiscountItem::class.java)

                        // Get the stock information from the snapshot
                        val stockStatus = snapshot.child("stocks").getValue(String::class.java)

                        discountItem?.let {
                            // Set stock status to DiscountItem
                            it.stocks = stockStatus

                            // Add the item to the list
                            discountItems.add(it)

                            // Assign path to the DiscountItem
                            it.path = shopName

                            // Notify the adapter that data has changed
                            notifyDataSetChanged()

                            Log.d("HomeDiscountAdapter", "Added item: $it with stock: $stockStatus")
                        }
                    } catch (e: Exception) {
                        Log.e("HomeDiscountAdapter", "Error parsing discount item: ${e.message}")
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    // Handle child changed if needed
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    // Handle child removed if needed
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    // Handle child moved if needed
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("HomeDiscountAdapter", "Fetch discount items cancelled: ${error.message}")
                }
            })
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeDiscountViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = HomeDiscountBinding.inflate(inflater, parent, false)
        return HomeDiscountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeDiscountViewHolder, position: Int) {
        if (position < discountItems.size) {
            val discountItem = discountItems[position]
            holder.bind(discountItem)
        } else {
            Log.e("HomeDiscountAdapter", "Index out of bounds: position $position, size ${discountItems.size}")
        }
    }

    fun updateData(newDiscountItems: List<DiscountItem>) {
        discountItems = newDiscountItems.toMutableList()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return discountItems.size
    }

    inner class HomeDiscountViewHolder(private val binding: HomeDiscountBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val quantityLiveData = MutableLiveData<Int>()

        init {
            val defaultQuantity = 0
            quantityLiveData.value = defaultQuantity

            binding.quantityy.setOnClickListener {
                val currentQuantity = quantityLiveData.value ?: defaultQuantity
                if (currentQuantity == 0) {
                    quantityLiveData.value = 1

                    binding.plusImageButton.visibility = View.VISIBLE
                    binding.minusImageButton.visibility = View.VISIBLE
                    addItemToCart()
                }
            }

            binding.plusImageButton.setOnClickListener {
                val currentQuantity = quantityLiveData.value ?: defaultQuantity
                val newQuantity = currentQuantity + 1
                quantityLiveData.value = newQuantity
                saveQuantity(binding.name.text.toString(), newQuantity)
                addItemToCart()
            }

            binding.minusImageButton.setOnClickListener {
                val currentQuantity = quantityLiveData.value ?: 0
                if (currentQuantity > 0) {
                    val newQuantity = currentQuantity - 0
                    quantityLiveData.value = newQuantity
                    saveQuantity(binding.name.text.toString(), newQuantity)
                    if (currentQuantity == 0) {
                        removeItemFromCart()
                    } else {
                        updateCartItemQuantity(newQuantity)
                        addItemToCart1()
                    }
                }
            }


            (context as? LifecycleOwner)?.let { lifecycleOwner ->
                quantityLiveData.observe(lifecycleOwner, Observer { quantity ->
                    updateQuantityText(quantity)
                })
            }

            updateQuantityText(defaultQuantity)
        }
        private fun updateCartItemQuantity(newQuantity: Int) {
            val database = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val foodName = binding.name.text.toString()

            if (userId != null && foodName.isNotEmpty()) {
                val cartItemsRef = database.child("user").child(userId).child("cartItems")
                val productQuantity = discountItems[adapterPosition].productQuantity

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
                                    val foodPricePerUnit =
                                        cartSnapshot.child("foodPrice").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0

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

        fun bind(discountItem: DiscountItem) {
            binding.apply {
                // Retrieve user's language from Firebase
                val languageRef = FirebaseDatabase.getInstance().reference
                    .child("user")
                    .child(userId)
                    .child("language")

                languageRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val language = snapshot.getValue(String::class.java) ?: "english"
                        val foodNames = discountItem.foodNames ?: ArrayList()

                        // Get English name
                        val englishName = foodNames.getOrNull(0) ?: ""

                        // Set food name based on language
                        val selectedLanguageName = when (language.toLowerCase()) {
                            "tamil" -> foodNames.getOrNull(1)
                            "malayalam" -> foodNames.getOrNull(2)
                            "telugu" -> foodNames.getOrNull(3)
                            else -> null
                        }

                        // Combine English name with the selected language name
                        val displayName = if (selectedLanguageName.isNullOrBlank()) {
                            englishName
                        } else {
                            "$englishName / $selectedLanguageName"
                        }

                        // Update the discountItem's foodNames with the displayName
                        discountItem.foodNames = arrayListOf(displayName)

                        // Bind other data
                        name.text = displayName
                        menuPrice1.text = "₹${discountItem.foodPrices}"
                        shopname.text = discountItem.path ?: ""
                        discount.text = "${discountItem.discounts} Off"
                        Glide.with(context).load(Uri.parse(discountItem.foodImages)).into(freshfishImage)
                        freshfishImage.tag = discountItem.foodImages

                        // Set saved quantity if any
                        val foodName = discountItem.foodNames?.getOrNull(0) ?: ""
                        val savedQuantity = getSavedQuantity(foodName)
                        quantityLiveData.value = savedQuantity
                        updateQuantityText(savedQuantity)

                        // Listen for quantity changes
                        val quantityRef = FirebaseDatabase.getInstance().reference
                            .child("user")
                            .child(userId!!)
                            .child("cartItems")
                            .orderByChild("foodName")
                            .equalTo(foodName)

                        quantityRef.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    for (itemSnapshot in snapshot.children) {
                                        val foodQuantity = itemSnapshot.child("foodQuantity").getValue(Int::class.java) ?: 0
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

                        if (discountItem.stocks == "Out Of Stock") {
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
                                    openDetailsActivity(discountItem,it)
                                }
                            }

                        }


                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("HomeDiscountAdapter", "Failed to read language: ${error.message}")
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
            val cartItemsRef = FirebaseAuth.getInstance().currentUser?.uid?.let {
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
                            showShopChangeDialog(cartItemsRef)
                        } else {
                            addItemToCartWithoutCheck()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("HomeDiscountAdapter", "Add to cart cancelled: ${databaseError.message}")
                    }
                })
            }
        }
        private fun addItemToCart1() {
            val currentShopName = binding.shopname.text.toString()
            val cartItemsRef = FirebaseAuth.getInstance().currentUser?.uid?.let {
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
                            showShopChangeDialog(cartItemsRef)
                        } else {
                            addItemToCartWithoutCheck1()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("HomeDiscountAdapter", "Add to cart cancelled: ${databaseError.message}")
                    }
                })
            }
        }


        private fun showShopChangeDialog(cartItemsRef: DatabaseReference) {
            val context = binding.root.context
            val layoutInflater = LayoutInflater.from(context)
            val customLayout = layoutInflater.inflate(R.layout.shop_dialog, null)

            val dialog = AlertDialog.Builder(context)
                .setView(customLayout)
                .create()

            val positiveButton = customLayout.findViewById<AppCompatButton>(R.id.dialog_positive_button)
            val negativeButton = customLayout.findViewById<AppCompatButton>(R.id.dialog_negative_button)

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

            dialog.show()
        }

        private fun addItemToCartWithoutCheck() {
            val database = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val shopName = binding.shopname.text.toString()
            val discountItem = discountItems.getOrNull(adapterPosition) ?: return
            val foodName = discountItem.foodNames?.getOrNull(0) ?: ""
            val foodPricePerUnit = discountItem.foodPrices?.toDoubleOrNull() ?: 0.0 // Extract unit price
            val foodImage = discountItem.foodImages ?: ""
            val quantity = quantityLiveData.value ?: 1
            val productQuantity = discountItem.productQuantity ?: "0g"
            val key = discountItem.key

            if (userId != null && foodName.isNotEmpty() && foodPricePerUnit > 0 && foodImage.isNotEmpty()) {
                val cartItemsRef = database.child("user").child(userId).child("cartItems")
                val CartItemAddTime = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())

                // Extract numeric value and unit from productQuantity
                val numericValue = productQuantity.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                val unit = productQuantity.filter { it.isLetter() }.lowercase()

                // Convert product quantity to grams if necessary
                val productQuantityInGrams = when (unit) {
                    "kg" -> numericValue * 1000
                    "g" -> numericValue
                    else -> numericValue // Default to grams if no unit is found
                }

                // Calculate the UnitQuantity based on the current quantity
                val unitQuantityInGrams = (quantity * productQuantityInGrams).toInt()
                val unitQuantityString = "${unitQuantityInGrams}g"

                // Calculate total price based on quantity
                val totalPrice = quantity * foodPricePerUnit

                cartItemsRef.orderByChild("foodName").equalTo(foodName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // Update existing item in the cart
                                for (cartSnapshot in dataSnapshot.children) {
                                    val currentQuantity = cartSnapshot.child("foodQuantity").getValue(Int::class.java) ?: 1
                                    val newQuantity = currentQuantity + 1
                                    val newUnitQuantityInGrams = (newQuantity * productQuantityInGrams).toInt()
                                    val newUnitQuantityString = "${newUnitQuantityInGrams}g"
                                    val newTotalPrice = newQuantity * foodPricePerUnit

                                    cartSnapshot.ref.child("foodQuantity").setValue(newQuantity)
                                    cartSnapshot.ref.child("UnitQuantity").setValue(newUnitQuantityString)
                                    cartSnapshot.ref.child("foodPrice").setValue(newTotalPrice.toString())
                                        .addOnSuccessListener {
                                        }
                                        .addOnFailureListener {
                                            ToastHelper.showCustomToast(context, "Failed to update item details")
                                        }
                                }
                            } else {
                                // Add new item to the cart
                                val cartItem = CartItems(
                                    shopName,
                                    foodName,
                                    totalPrice.toString(), // Save total price here
                                    discountItem.foodDescriptions,
                                    foodImage,
                                    quantity,
                                    CartItemAddTime,
                                    key,
                                    unitQuantityString // Include UnitQuantity in the CartItems object
                                )

                                if (key != null) {
                                    cartItemsRef.child(key).setValue(cartItem)
                                        .addOnSuccessListener {
                                        }
                                        .addOnFailureListener {
                                            ToastHelper.showCustomToast(context, "Failed to add item")
                                        }
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("HomeDiscountAdapter", "Add to cart cancelled: ${databaseError.message}")
                        }
                    })
            }
        }
        private fun addItemToCartWithoutCheck1() {
            val database = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val shopName = binding.shopname.text.toString()
            val discountItem = discountItems.getOrNull(adapterPosition) ?: return
            val foodName = discountItem.foodNames?.getOrNull(0) ?: ""
            val foodPrice = discountItem.foodPrices?.toDoubleOrNull() ?: 0.0
            val foodImage = discountItem.foodImages ?: ""
            val productQuantity = discountItem.productQuantity ?: "0g"
            val quantity = quantityLiveData.value ?: 1

            if (userId != null && foodName.isNotEmpty() && foodPrice > 0 && foodImage.isNotEmpty()) {
                val cartItemsRef = database.child("user").child(userId).child("cartItems")
                val cartItemAddTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                // Extract numeric value and unit from productQuantity
                val numericValue = productQuantity.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                val unit = productQuantity.filter { it.isLetter() }.lowercase()

                // Convert product quantity to grams if necessary
                val productQuantityInGrams = when (unit) {
                    "kg" -> numericValue * 1000
                    "g" -> numericValue
                    else -> numericValue // Default to grams if no unit is found
                }

                // Calculate UnitQuantity based on the current quantity
                val unitQuantityInGrams = (quantity * productQuantityInGrams).toInt()
                val unitQuantityString = "${unitQuantityInGrams}g"

                cartItemsRef.orderByChild("foodName").equalTo(foodName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                for (cartSnapshot in dataSnapshot.children) {
                                    val currentQuantity = cartSnapshot.child("foodQuantity").getValue(Int::class.java) ?: 1
                                    val newQuantity = currentQuantity - 1

                                    if (newQuantity <= 0) {
                                        // Remove the item if quantity is 0 or less
                                        cartSnapshot.ref.removeValue()
                                            .addOnSuccessListener {
                                            }
                                            .addOnFailureListener {
                                                ToastHelper.showCustomToast(context, "Failed to remove item")
                                            }
                                    } else {
                                        // Update the quantity, UnitQuantity, and price
                                        val newUnitQuantityInGrams = (newQuantity * productQuantityInGrams).toInt()
                                        val newUnitQuantityString = "${newUnitQuantityInGrams}g"
                                        val newTotalPrice = newQuantity * foodPrice

                                        cartSnapshot.ref.child("foodQuantity").setValue(newQuantity)
                                        cartSnapshot.ref.child("UnitQuantity").setValue(newUnitQuantityString)
                                        cartSnapshot.ref.child("foodPrice").setValue(newTotalPrice.toString())
                                            .addOnSuccessListener {
                                            }
                                            .addOnFailureListener {
                                                ToastHelper.showCustomToast(context, "Failed to update item details")
                                            }
                                    }
                                }
                            } else {
                                // Add new item to the cart
                                val totalPrice = quantity * foodPrice
                                val cartItem = CartItems(
                                    shopName,
                                    foodName,
                                    totalPrice.toString(),
                                    discountItem.foodDescriptions,
                                    foodImage,
                                    quantity,
                                    cartItemAddTime,
                                    unitQuantityString // Include calculated UnitQuantity
                                )

                                cartItemsRef.push().setValue(cartItem)
                                    .addOnSuccessListener {
                                    }
                                    .addOnFailureListener {
                                        ToastHelper.showCustomToast(context, "Failed to add item")
                                    }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("HomeDiscountAdapter", "Add to cart cancelled: ${databaseError.message}")
                        }
                    })
            }
        }


        private fun removeItemFromCart() {
            val database = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val shopName = shopNames.getOrNull(adapterPosition) ?: ""
            val discountItem = discountItems.getOrNull(adapterPosition) ?: return
            val foodName = discountItem.foodNames?.getOrNull(0) ?: ""
            val cartItemsRef = userId?.let { database.child("user").child(it).child("cartItems") }

            if (cartItemsRef != null) {
                cartItemsRef.orderByChild("foodName").equalTo(foodName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                for (cartSnapshot in dataSnapshot.children) {
                                    val currentQuantity = cartSnapshot.child("foodQuantity").getValue(Int::class.java)
                                        ?: 1
                                    if (currentQuantity > 1) {
                                        cartSnapshot.ref.child("foodQuantity").setValue(currentQuantity - 1)
                                            .addOnSuccessListener {
                                            }.addOnFailureListener {
                                                ToastHelper.showCustomToast(context, "Failed to update item quantity")
                                            }
                                    } else {
                                        cartSnapshot.ref.removeValue()
                                            .addOnSuccessListener {
                                            }.addOnFailureListener {
                                                ToastHelper.showCustomToast(context, "Failed to remove item")
                                            }
                                    }
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("HomeDiscountAdapter", "Remove from cart cancelled: ${databaseError.message}")
                        }
                    })
            }
        }
    }

    private fun openDetailsActivity(discountItem: DiscountItem, view: View) {
        val bundle = Bundle().apply {
            putString("DiscountItemName", discountItem.foodNames?.getOrNull(0) ?: "")
            putString("DiscountItemPrice", discountItem.foodPrices)
            putString("DiscountItemDescription", discountItem.foodDescriptions)
            putString("DiscountItemImage", discountItem.foodImages)
            putString("DiscountAmount", discountItem.discounts)
            putString("DiscountQuantity", discountItem.productQuantity)
            putString("Shop Id",discountItem.path)
            putString("key", discountItem.key)

        }

        view.findNavController().navigate(R.id.action_homeFragment_to_detailsFragment, bundle)
    }
}