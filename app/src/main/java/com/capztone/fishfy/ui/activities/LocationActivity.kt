package com.capztone.fishfy.ui.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.capztone.admin.utils.FirebaseAuthUtil
import com.capztone.fishfy.databinding.ActivityLocationBinding
import com.capztone.fishfy.ui.activities.Utils.ToastHelper
import com.capztone.fishfy.ui.activities.adapters.AddressAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlin.math.*

class LocationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var mainBinding: ActivityLocationBinding
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    private var savedAddresses = mutableListOf<String>()
    private lateinit var adapter: AddressAdapter
    private val adminDestinations = mutableListOf<Pair<Double, Double>>()
    private val shopNames = mutableListOf<String>()

    private lateinit var userLocationListener: ValueEventListener
    private lateinit var userLocationRef: DatabaseReference

    private var distanceThreshold: Double = 10.0  // Default value, will be overwritten

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        auth = FirebaseAuthUtil.auth
        database = FirebaseDatabase.getInstance().reference
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)


        mainBinding.Locationbutton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_MAP)
        }
        mainBinding.manualLocation.setOnClickListener {
            val intent = Intent(this, ManualLocation::class.java)
            startActivity(intent)
        }



        fetchAdminDistanceThreshold()
        fetchShopLocationsFromFirebase()
    }
    private fun fetchAdminDistanceThreshold() {

        val adminRef = database.child("Delivery Details").child("User Distance")

        adminRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val distanceString = dataSnapshot.getValue(String::class.java)
                if (distanceString != null) {
                    try {
                        distanceThreshold = distanceString.toDouble()
                    } catch (e: NumberFormatException) {
                        ToastHelper.showCustomToast(this@LocationActivity, "Invalid distance format")

                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                ToastHelper.showCustomToast(this@LocationActivity, "Failed to load distance threshold")
            }
        })
    }




    override fun onDestroy() {
        super.onDestroy()
        userLocationRef.removeEventListener(userLocationListener)
    }

    private fun updateLocationTextView(latitude: Double?, longitude: Double?) {
        val textView = mainBinding.tvAddress
        if (latitude != null && longitude != null) {
            textView.text = "Latitude: $latitude\nLongitude: $longitude"
        } else {
            textView.text = "Location not available"
        }
    }

    private fun fetchShopLocationsFromFirebase() {
        val shopLocationsRef = database.child("ShopLocations")
        shopLocationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (shopSnapshot in dataSnapshot.children) {
                    val shopName = shopSnapshot.key ?: continue
                    val lat = shopSnapshot.child("latitude").getValue(Double::class.java) ?: continue
                    val lng = shopSnapshot.child("longitude").getValue(Double::class.java) ?: continue
                    adminDestinations.add(Pair(lat, lng))
                    shopNames.add(shopName)
                }
                fetchUserLocationFromFirebase()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                ToastHelper.showCustomToast(this@LocationActivity, "Failed to load shop locations")
            }
        })
    }
    private fun fetchUserLocationFromFirebase() {
        val userId = auth.currentUser?.uid ?: return

        userLocationRef = database.child("Addresses").child(userId)
        userLocationListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val latitude = dataSnapshot.child("latitude").getValue(Double::class.java)
                val longitude = dataSnapshot.child("longitude").getValue(Double::class.java)

                updateLocationTextView(latitude, longitude)
                calculateDistances(latitude, longitude)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                ToastHelper.showCustomToast(this@LocationActivity, "Failed to load user location")
            }
        }
        userLocationRef.addValueEventListener(userLocationListener)
    }
    private fun calculateDistances(userLat: Double?, userLng: Double?) {
        if (userLat == null || userLng == null) return

        val nearbyShops = mutableListOf<String>()

        for (i in adminDestinations.indices) {
            val shopLat = adminDestinations[i].first
            val shopLng = adminDestinations[i].second

            val distance = calculateDistance(userLat, userLng, shopLat, shopLng)

            if (distance < distanceThreshold) {
                nearbyShops.add(shopNames[i])
            }
        }

        if (nearbyShops.isNotEmpty()) {
            val shopsWithinThreshold = nearbyShops.joinToString(", ")
            mainBinding.shoptextview.text = shopsWithinThreshold
            storeNearbyShopsInFirebase(shopsWithinThreshold)
        }
    }

    private fun storeNearbyShopsInFirebase(shops: String) {
        val userId = auth.currentUser?.uid ?: return
        val userLocationRef = database.child("Addresses").child(userId)
        userLocationRef.child("Shop Id").setValue(shops)
            .addOnSuccessListener {

            }
            .addOnFailureListener {
            }
    }
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = deg2rad(lat2 - lat1)
        val dLon = deg2rad(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(deg2rad(lat1)) * cos(deg2rad(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    private fun deg2rad(deg: Double): Double {
        return deg * (Math.PI / 180)
    }
    private fun getSavedAddressesFromSharedPreferences(): MutableList<String> {
        val savedAddressesSet = sharedPreferences.getStringSet("SAVED_ADDRESSES", HashSet<String>()) ?: HashSet()
        return savedAddressesSet.toMutableList()
    }



    companion object {
        const val REQUEST_CODE_MAP = 1001
    }
}
