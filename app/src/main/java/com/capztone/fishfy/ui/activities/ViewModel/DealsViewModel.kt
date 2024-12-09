package com.capztone.fishfy.ui.activities.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.capztone.fishfy.ui.activities.models.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DealsViewModel : ViewModel() {

    private val _menuItems = MutableLiveData<List<MenuItem>>()
    val menuItems: LiveData<List<MenuItem>> get() = _menuItems

    private val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
    private val database = FirebaseDatabase.getInstance().getReference("Favourite").child(currentUserID ?: "")

    init {
        loadMenuItems()
    }

    private fun loadMenuItems() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<MenuItem>()
                for (favoriteSnapshot in snapshot.children) {
                    val menuItem = favoriteSnapshot.getValue(MenuItem::class.java)
                    menuItem?.let { items.add(it) }
                }
                _menuItems.value = items
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle the error
            }
        })
    }

    fun toggleFavorite(menuItem: MenuItem) {
        val newFavoriteState = !menuItem.favorite
        menuItem.favorite = newFavoriteState

        if (newFavoriteState) {
            updateFavoriteStateInFirebase(menuItem)
        } else {
            removeFavoriteFromFirebase(menuItem)
        }

        // Update the list in LiveData
        _menuItems.value = _menuItems.value?.map {
            if (it.foodId == menuItem.foodId) menuItem else it
        }
    }

    private fun updateFavoriteStateInFirebase(menuItem: MenuItem) {
        currentUserID?.let { userId ->
            val database = FirebaseDatabase.getInstance().getReference("Favourite").child(userId)
            val favoriteRef = database.push()
            menuItem.firebaseKey = favoriteRef.key // Store the key in the MenuItem
            favoriteRef.setValue(menuItem).addOnSuccessListener {
                // Handle success
            }.addOnFailureListener { e ->
                // Handle failure
            }
        }
    }

    private fun removeFavoriteFromFirebase(menuItem: MenuItem) {
        currentUserID?.let { userId ->
            val database = FirebaseDatabase.getInstance().getReference("Favourite").child(userId)
            menuItem.firebaseKey?.let { key ->
                database.child(key).removeValue().addOnSuccessListener {
                    // Handle success
                }.addOnFailureListener { e ->
                    // Handle failure
                }
            }
        }
    }
}
