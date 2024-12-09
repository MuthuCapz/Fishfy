package com.capztone.fishfy.ui.activities.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.capztone.fishfy.ui.activities.models.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SearchViewModel : ViewModel() {
    val menuItemsLiveData: MutableLiveData<List<MenuItem>> = MutableLiveData()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> get() = _searchQuery

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getSearchQuery(): String {
        return _searchQuery.value ?: ""
    }

    fun retrieveMenuItems() {
        val userId = auth.currentUser?.uid ?: return

        // Fetch user language first
        database.getReference("user").child(userId).child("language")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(languageSnapshot: DataSnapshot) {
                    val userLanguage =
                        languageSnapshot.getValue(String::class.java)?.toLowerCase() ?: "english"
                    val userLocationRef = database.getReference("Addresses").child(userId)

                    userLocationRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val shopNamesString =
                                snapshot.child("Shop Id").getValue(String::class.java)
                            shopNamesString?.let { shopNames ->
                                val shopNamesList = shopNames.split(",").map { it.trim() }
                                val menuItems = mutableListOf<MenuItem>()
                                val excludedPaths = setOf(
                                    "discount",
                                    "Discount-items",
                                    "Inventory",
                                    "Shop name",
                                    "Products"
                                )

                                shopNamesList.forEach { shopName ->
                                    val shopRef = database.getReference("Shops").child(shopName)

                                    // Retrieve all child paths under the shop, excluding specified paths
                                    shopRef.addListenerForSingleValueEvent(object :
                                        ValueEventListener {
                                        override fun onDataChange(shopSnapshot: DataSnapshot) {
                                            for (categorySnapshot in shopSnapshot.children) {
                                                val categoryName = categorySnapshot.key ?: ""
                                                if (categoryName in excludedPaths) continue

                                                categorySnapshot.children.forEach { itemSnapshot ->
                                                    val menuItem =
                                                        itemSnapshot.getValue(MenuItem::class.java)
                                                    menuItem?.let {
                                                        // Fetch the stock status from the item
                                                        val stockStatus =
                                                            itemSnapshot.child("stock")
                                                                .getValue(String::class.java)

                                                        // Only proceed if the item is "In stock"
                                                        if (stockStatus == "In Stock") {
                                                            val foodNamesList =
                                                                it.foodName ?: arrayListOf()
                                                            val englishName =
                                                                foodNamesList.getOrNull(0) ?: ""
                                                            val languageSpecificName =
                                                                when (userLanguage) {
                                                                    "tamil" -> foodNamesList.getOrNull(
                                                                        1
                                                                    ) ?: ""

                                                                    "malayalam" -> foodNamesList.getOrNull(
                                                                        2
                                                                    ) ?: ""

                                                                    "telugu" -> foodNamesList.getOrNull(
                                                                        3
                                                                    ) ?: ""

                                                                    else -> englishName
                                                                }

                                                            // Create a combined name
                                                            val combinedName =
                                                                if (userLanguage == "english") {
                                                                    englishName
                                                                } else {
                                                                    "$englishName / $languageSpecificName"
                                                                }
                                                            it.foodName = arrayListOf(combinedName)

                                                            // Set path for debugging
                                                            it.path =
                                                                "Shops/$shopName/${categorySnapshot.key}/${itemSnapshot.key}"
                                                            Log.d(
                                                                "SearchViewModel",
                                                                "MenuItem path: ${it.path}"
                                                            )

                                                            // Add to menuItems only if the stock is "In stock"
                                                            menuItems.add(it)
                                                        }
                                                    }
                                                }
                                            }
                                            menuItemsLiveData.postValue(menuItems)
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            // Handle onCancelled
                                        }
                                    })
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle onCancelled
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle onCancelled
                }
            })
    }

}
