package com.capztone.fishfy.ui.activities.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capztone.fishfy.ui.activities.models.Category
import com.capztone.fishfy.ui.activities.models.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ShopOneViewModel : ViewModel() {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    private val _menuItems = MutableLiveData<List<MenuItem>>()
    val menuItems: LiveData<List<MenuItem>>
        get() = _menuItems
    private val _categoryName = MutableLiveData<String>()
    val categoryName: LiveData<String> get() = _categoryName


    fun selectCategory(categoryName: Category) {
        _categoryName.value = categoryName.toString()
    }


    fun retrieveData(shopName: String, textValue: String) {
        viewModelScope.launch {
            currentUser?.let { user ->
                val userRef = database.getReference("user").child(user.uid).child("language")

                val languageSnapshot = userRef.get().await()
                val userLanguage =
                    languageSnapshot.getValue(String::class.java)?.toLowerCase() ?: "english"

                retrieveShopData(shopName, userLanguage,textValue)
            }
        }
    }

    private suspend fun retrieveShopData(shopName: String, userLanguage: String, textValue: String?) {
        try {
            val shopRef = database.getReference("Shops").child(shopName)
            val firstChildPath: String

            // Determine the initial path
            if (textValue.isNullOrEmpty()) {
                // Get the first child key as the default path
                val snapshot = shopRef.get().await()
                firstChildPath = snapshot.children.firstOrNull()?.key ?: return
            } else {
                // Use the provided textValue as the path
                firstChildPath = textValue
            }

            // Retrieve menu items from the determined path
            val menuRef = shopRef.child(firstChildPath)
            val menuSnapshot = menuRef.get().await()
            if (menuSnapshot.exists()) {
                retrieveMenuItems(menuRef, _menuItems, userLanguage, shopName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle error (e.g., show a toast message or log the error)
        }
    }

    private suspend fun retrieveMenuItems(
        menuRef: DatabaseReference,
        menuLiveData: MutableLiveData<List<MenuItem>>,
        userLanguage: String,shopName: String
    ) {
        try {
            val snapshot = menuRef.get().await()
            val menuItems = mutableListOf<MenuItem>()
            for (itemSnapshot in snapshot.children) {
                val menuItem = itemSnapshot.getValue(MenuItem::class.java)
                menuItem?.let {

                    it.path = shopName
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
                    menuItems.add(it)
                }
            }
            menuLiveData.postValue(menuItems)
        } catch (e: Exception) {
            // Handle error
        }
    }


}