package com.capztone.fishfy.ui.activities.ViewModel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.capztone.fishfy.ui.activities.models.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
class FreshFishViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    val menuItemsLiveData: MutableLiveData<List<MenuItem>> = MutableLiveData()

    fun retrieveMenuItems(categoryName: String?) {
        val userId = auth.currentUser?.uid ?: return

        // Fetch user data including language
        database.getReference("user").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnapshot: DataSnapshot) {
                    val userLanguage = userSnapshot.child("language").getValue(String::class.java)?.toLowerCase()
                        ?: "english"

                    // Fetch shopnames string from Locations
                    val locationsRef = database.getReference("Addresses").child(userId)
                    locationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(locationsSnapshot: DataSnapshot) {
                            val shopNamesString = locationsSnapshot.child("Shop Id").getValue(String::class.java)
                            shopNamesString?.let { shopNames ->
                                val shopNamesList = shopNames.split(",").map { it.trim() }
                                val menuItems = mutableListOf<MenuItem>()

                                // Iterate through each shop name to retrieve menu items based on category
                                shopNamesList.forEach { shopName ->
                                    categoryName?.let { category ->
                                        // Fetch items based on the shop and category name
                                        val menuRef = database.getReference("Shops").child(shopName).child(category)

                                        menuRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(menuSnapshot: DataSnapshot) {
                                                if (menuSnapshot.exists()) {
                                                    for (itemSnapshot in menuSnapshot.children) {
                                                        val menuItem = itemSnapshot.getValue(MenuItem::class.java)
                                                        menuItem?.let {
                                                            // Retrieve additional fields and set to menuItem
                                                            it.path = shopName
                                                            it.key = itemSnapshot.key ?: ""


                                                            // Combine English and language-specific food names if needed
                                                            val foodNamesList = it.foodName ?: arrayListOf()
                                                            val englishName = foodNamesList.getOrNull(0) ?: ""
                                                            val languageSpecificName = when (userLanguage) {
                                                                "tamil" -> foodNamesList.getOrNull(1) ?: ""
                                                                "malayalam" -> foodNamesList.getOrNull(2) ?: ""
                                                                "telugu" -> foodNamesList.getOrNull(3) ?: ""
                                                                else -> englishName // Default to English
                                                            }

                                                            // Create a combined name with both English and language-specific names
                                                            val combinedName = if (userLanguage == "english") {
                                                                englishName
                                                            } else {
                                                                "$englishName / $languageSpecificName"
                                                            }

                                                            // Add the combined name to the foodName list
                                                            it.foodName = arrayListOf(combinedName)
                                                            val stockStatus = itemSnapshot.child("stock").getValue(String::class.java)
                                                            it.stock = stockStatus
                                                            // Add the menuItem to the list
                                                            menuItems.add(it)
                                                        }
                                                    }
                                                }

                                                // Post updated menu items to LiveData after processing all menu items
                                                menuItemsLiveData.postValue(menuItems)
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                // Handle onCancelled
                                                Log.e("FreshFishViewModel", "Failed to retrieve menu items for $shopName: ${error.message}")
                                            }
                                        })
                                    }
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle onCancelled
                            Log.e("FreshFishViewModel", "Failed to retrieve shop names: ${error.message}")
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle onCancelled
                    Log.e("FreshFishViewModel", "Failed to retrieve user data: ${error.message}")
                }
            })
    }
}
