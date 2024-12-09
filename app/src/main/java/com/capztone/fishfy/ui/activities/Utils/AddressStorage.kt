package com.capztone.fishfy.ui.activities.Utils

import android.content.Context
import android.content.SharedPreferences
import com.capztone.fishfy.ui.activities.models.Address
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object AddressStorage {
    private const val PREF_NAME = "AddressPreferences"
    private const val ADDRESSES_KEY = "addresses"

    fun saveAddresses(context: Context, addresses: List<Address>) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(addresses)
        editor.putString(ADDRESSES_KEY, json)
        editor.apply()
    }

    fun getAddresses(context: Context): List<Address> {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(ADDRESSES_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<Address>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }
}
