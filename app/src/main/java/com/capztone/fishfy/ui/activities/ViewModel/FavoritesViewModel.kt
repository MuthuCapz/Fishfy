package com.capztone.fishfy.ui.activities.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.capztone.fishfy.ui.activities.models.MenuItem

class FavoritesViewModel : ViewModel() {
    private val _favoriteItems = MutableLiveData<MutableList<MenuItem>>(mutableListOf())
    val favoriteItems: LiveData<MutableList<MenuItem>> = _favoriteItems

    fun addFavorite(item: MenuItem) {
        _favoriteItems.value?.apply {
            if (!contains(item)) add(item)
        }
        _favoriteItems.notifyObserver()
    }

    fun removeFavorite(item: MenuItem) {
        _favoriteItems.value?.apply {
            remove(item)
        }
        _favoriteItems.notifyObserver()
    }

    private fun <T> MutableLiveData<T>.notifyObserver() {
        this.value = this.value
    }
}
