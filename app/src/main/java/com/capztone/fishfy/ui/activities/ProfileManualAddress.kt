package com.capztone.fishfy.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.capztone.fishfy.R
import java.io.IOException
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.navigation.findNavController
import com.capztone.admin.utils.FirebaseAuthUtil
import com.capztone.fishfy.databinding.ActivityProfileManualAddressBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ProfileManualAddress : AppCompatActivity() {

    private lateinit var binding: ActivityProfileManualAddressBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var selectedAddressType: String? = null
    private lateinit var geocoder: Geocoder
    private val adminDestinations = mutableListOf<Pair<Double, Double>>()
    private val shopNames = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileManualAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

         auth = FirebaseAuthUtil.auth
        database = FirebaseDatabase.getInstance().reference
        geocoder = Geocoder(this)
        fetchShopLocationsFromFirebase()
        binding.etName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateName()
            }
        }
        binding.etMobileNumber.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateMobileNumber()
            }
        }
        binding.etLocality.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                validateCity()

            }
        }
        binding.etPincode.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validatePincode()
            }
        }
        binding.AddressSave.setOnClickListener {
            if (validateName() && validateCity() && validatePincode() && validateMobileNumber()) {
                if (selectedAddressType != null) {
                    CoroutineScope(Dispatchers.Main).launch {
                        saveAddressToFirebase()
                    }
                } else {
                    Toast.makeText(this, "Please select an address type", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
        binding.detailGoToBackImageButton.setOnClickListener {
            onBackPressed()
        }
        binding.btnSaveAsHome.setOnClickListener { onAddressTypeSelected("HOME", it) }
        binding.btnSaveAsWork.setOnClickListener { onAddressTypeSelected("WORK", it) }
        binding.btnSaveAsOther.setOnClickListener { onAddressTypeSelected("OTHER", it) }
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
                calculateDistances()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@ProfileManualAddress, "Failed to load shop locations", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun calculateDistances() {
        val addressString = "${binding.etBuildingName.text}, "

        try {
            // Use Geocoder to get latitude and longitude from address
            val addresses: MutableList<Address>? = geocoder.getFromLocationName(addressString, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val userLat = addresses[0].latitude
                val userLng = addresses[0].longitude

                // Retrieve the distance threshold from Firebase
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val databaseReference = FirebaseDatabase.getInstance().getReference("Delivery Details/User Distance")
                    databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val distanceThresholdString = dataSnapshot.getValue(String::class.java)
                            val distanceThreshold = distanceThresholdString?.toDoubleOrNull() ?: 10.0

                            // Calculate distances between user location and shop locations
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
                                binding.shopnameTextView.text = shopsWithinThreshold
                                storeNearbyShopsInFirebase(shopsWithinThreshold)
                            } else {
                                // Delete the shop name if no shops are within the threshold
                                deleteShopNameFromFirebase(userId)
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Toast.makeText(
                                this@ProfileManualAddress,
                                "Error fetching distance threshold: ${databaseError.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                } else {
                    Toast.makeText(
                        this@ProfileManualAddress,
                        "User not authenticated",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: IOException) {
            Toast.makeText(
                this@ProfileManualAddress,
                "Error geocoding address: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun deleteShopNameFromFirebase(userId: String) {
        val userLocationRef = database.child("Addresses").child(userId).child("Shop Id")
        userLocationRef.removeValue()
            .addOnSuccessListener {
            }
            .addOnFailureListener {
            }
    }

    private fun storeNearbyShopsInFirebase(shops: String) {
        val userId = auth.currentUser?.uid ?: return
        val userLocationRef = database.child("Addresses").child(userId)
        userLocationRef.child("Shop Id").setValue(shops)
            .addOnSuccessListener {

            }
            .addOnFailureListener {
                Toast.makeText(this@ProfileManualAddress, "Failed to store nearby shops", Toast.LENGTH_SHORT)
                    .show()
            }
    }
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    private fun setMandatoryFieldIndicatorVisible(visible: Boolean, text: String, textView: TextView) {
        if (visible) {
            textView.visibility = View.VISIBLE
            textView.text = text
        } else {
            textView.visibility = View.VISIBLE
        }
    }

    private fun validateName(): Boolean {
        val name = binding.etName.text.toString().trim()
        return when {
            TextUtils.isEmpty(name) -> {
                binding.etName.error = "Name is required"
                setMandatoryFieldIndicatorVisible(true, "Name *", binding.name)
                false
            }
            name.length !in 3..20 -> {
                binding.etName.error = "Name must be between 3 and 20 characters"
                setMandatoryFieldIndicatorVisible(true, "Name *", binding.name)
                false
            }
            else -> {
                setMandatoryFieldIndicatorVisible(false, "Name *", binding.name)
                true
            }
        }
    }

    private fun validateMobileNumber(): Boolean {
        val mobileNumber = binding.etMobileNumber.text.toString().trim()
        return when {
            TextUtils.isEmpty(mobileNumber) -> {
                binding.etMobileNumber.error = "Mobile number is required"
                setMandatoryFieldIndicatorVisible(true, "Mobile Number *", binding.PhoneNum)
                false
            }
            mobileNumber.length != 10 -> {
                binding.etMobileNumber.error = "Mobile number must be 10 digits"
                setMandatoryFieldIndicatorVisible(true, "Mobile Number *", binding.PhoneNum)
                false
            }
            else -> {
                setMandatoryFieldIndicatorVisible(false, "Mobile Number *", binding.PhoneNum)
                true
            }
        }
    }

    private fun validateCity(): Boolean {
        val city = binding.etLocality.text.toString().trim()
        val cityPattern = "^[a-zA-Z ]{3,20}$".toRegex()
        return when {
            TextUtils.isEmpty(city) -> {
                binding.etLocality.error = "City is required"
                setMandatoryFieldIndicatorVisible(true, "City *", binding.local)
                false
            }
            !city.matches(cityPattern) -> {
                binding.etLocality.error = "City must be between 3 and 20 characters and contain only letters and spaces"
                setMandatoryFieldIndicatorVisible(true, "City *", binding.local)
                false
            }
            else -> {
                setMandatoryFieldIndicatorVisible(false, "City *", binding.local)
                true
            }
        }
    }

    private fun validatePincode(): Boolean {
        val pincode = binding.etPincode.text.toString().trim()
        return when {
            TextUtils.isEmpty(pincode) -> {
                binding.etPincode.error = "Pincode is required"
                setMandatoryFieldIndicatorVisible(true, "Pincode *", binding.pin)
                false
            }
            pincode.length != 6 -> {
                binding.etPincode.error = "Pincode must be 6 digits"
                setMandatoryFieldIndicatorVisible(true, "Pincode *", binding.pin)
                false
            }
            else -> {
                setMandatoryFieldIndicatorVisible(false, "Pincode *", binding.pin)
                true
            }
        }
    }

    private fun onAddressTypeSelected(type: String, button: View) {
        // Reset all buttons to default color and icon tint
        resetButtonStyle(binding.btnSaveAsHome, R.color.navy)
        resetButtonStyle(binding.btnSaveAsWork, R.color.navy)
        resetButtonStyle(binding.btnSaveAsOther, R.color.navy)

        // Change the background drawable, text color, and icon tint of the selected button
        if (button is AppCompatButton) {
            button.setBackgroundResource(R.drawable.linearbg) // Use drawable resource for background
            button.setTextColor(ContextCompat.getColor(this, R.color.white))
            button.compoundDrawablesRelative.forEach {
                it?.setTint(
                    ContextCompat.getColor(
                        this,
                        R.color.white
                    )
                )
            }
        }
        selectedAddressType = type
    }
    private fun resetButtonStyle(button: AppCompatButton, color: Int) {
        button.setBackgroundResource(R.drawable.colorlinear) // Set the default background drawable
        button.setTextColor(ContextCompat.getColor(this, color))
        button.compoundDrawablesRelative.forEach {
            it?.setTint(
                ContextCompat.getColor(
                    this,
                    color
                )
            )
        }
    }

    private fun saveAddressToFirebase() {
        val userId = auth.currentUser?.uid ?: return


        // Retrieve the "User Distance" value from Firebase
        val adminDistanceRef = database.child("Delivery Details").child("User Distance")
        adminDistanceRef.get().addOnSuccessListener { dataSnapshot ->
            val userDistance = try {
                dataSnapshot.getValue(Double::class.java)
                    ?: 10.0 // Default to 10.0 if not found or conversion fails
            } catch (e: DatabaseException) {
                try {
                    dataSnapshot.getValue(String::class.java)?.toDouble() ?: 10.0
                } catch (e: NumberFormatException) {
                    10.0 // Default to 10.0 if conversion fails
                }
            }

            // Full address string for saving to Firebase
            val name = binding.etName.text.toString().trim()
            val houseFlatNo = binding.etHouseFlatNo.text.toString().trim()
            val buildingName = binding.etBuildingName.text.toString().trim()
            val street = binding.etStreet.text.toString().trim()
            val locality = binding.etLocality.text.toString().trim()
            val pincode = binding.etPincode.text.toString().trim()
            val mobileNumber = binding.etMobileNumber.text.toString().trim()

            // List to store non-empty fields
            val addressParts = mutableListOf<String>()

            if (name.isNotEmpty()) addressParts.add(name)
            if (houseFlatNo.isNotEmpty()) addressParts.add(houseFlatNo)
            if (buildingName.isNotEmpty()) addressParts.add(buildingName)
            if (street.isNotEmpty()) addressParts.add(street)
            if (locality.isNotEmpty() && pincode.isNotEmpty()) {
                addressParts.add("$locality - $pincode")
            }
            if (mobileNumber.isNotEmpty()) addressParts.add("+91 $mobileNumber")

            // Join the list with commas to form the full address string
            val fullAddressString = addressParts.joinToString(", \n ")

            // Address string for geocoding (only locality and pincode)
            val geocodeAddressString = "$locality - $pincode"

            try {
                // Use Geocoder to get latitude and longitude from geocodeAddressString
                val addresses: MutableList<Address>? =
                    geocoder.getFromLocationName(geocodeAddressString, 1)

                if (addresses != null && addresses.isNotEmpty()) {
                    val latitude = addresses[0].latitude
                    val longitude = addresses[0].longitude

                    // Check if the distance to any shop location is greater than 10 kilometers
                    var addressWithinThreshold = false

                    // Calculate distances between user location and shop locations
                    for (i in adminDestinations.indices) {
                        val shopLat = adminDestinations[i].first
                        val shopLng = adminDestinations[i].second

                        val distance = calculateDistance(latitude, longitude, shopLat, shopLng)

                        if (distance > userDistance) { // Only proceed if distance is above 10.0 kilometers
                            addressWithinThreshold = true
                            break
                        }
                    }

                    if (addressWithinThreshold) {
                        // Proceed to save address
                        val nearbyShops = mutableListOf<String>()

                        // Calculate distances and collect nearby shop names within 10.0 kilometers
                        for (i in adminDestinations.indices) {
                            val shopLat = adminDestinations[i].first
                            val shopLng = adminDestinations[i].second

                            val distance = calculateDistance(latitude, longitude, shopLat, shopLng)

                            if (distance < userDistance) { // Collect nearby shop names within 10.0 kilometers
                                nearbyShops.add(shopNames[i])
                            }
                        }

                        // Join the nearby shop names into a single string
                        val shopsWithinThreshold = nearbyShops.joinToString(", ")

                        // Store the address, latitude, longitude, and nearby shops in Firebase
                        val locationData = hashMapOf(
                            "address" to fullAddressString,
                            "latitude" to latitude,
                            "longitude" to longitude,
                            "locality" to locality,
                            "addressType" to selectedAddressType
                        )

                        if (nearbyShops.isNotEmpty()) {
                            locationData["Shop Id"] = shopsWithinThreshold
                        } else {
                            deleteShopNameFromFirebase(userId)
                            database.child("Addresses").child(userId).child("Shop Id")
                                .removeValue()
                                .addOnCompleteListener { shopNameRemoveTask ->
                                    if (shopNameRemoveTask.isSuccessful) {

                                    } else {

                                    }
                                }
                        }


                        // Store the locality directly inside Locations -> userId
                        database.child("Addresses").child(userId).child("locality")
                            .setValue(locality)
                            .addOnCompleteListener { localitySaveTask ->
                                if (!localitySaveTask.isSuccessful) {
                                    Toast.makeText(
                                        this@ProfileManualAddress,
                                        "Failed to save locality: ${localitySaveTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        database.child("PayoutAddress").child(userId).child("address")
                            .setValue(fullAddressString)
                            .addOnCompleteListener { localitySaveTask ->
                                if (!localitySaveTask.isSuccessful) {
                                    Toast.makeText(
                                        this@ProfileManualAddress,
                                        "Failed to save locality: ${localitySaveTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        // Store username and mobile number under Locations -> userId -> User Details
                        val userDetailsRef = database.child("Addresses").child(userId).child("User Details")
                        userDetailsRef.child("user name").setValue(name)
                        userDetailsRef.child("mobile number").setValue(mobileNumber)
                            .addOnCompleteListener { userSaveTask ->
                                if (userSaveTask.isSuccessful) {

                                } else {
                                    Toast.makeText(
                                        this@ProfileManualAddress,
                                        "Failed to save username and mobile number: ${userSaveTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }


                        // Store latitude and longitude under Locations -> userId
                        val locationRef = database.child("Addresses").child(userId)
                        locationRef.child("latitude").setValue(latitude)
                            .addOnCompleteListener { latitudeSaveTask ->
                                if (!latitudeSaveTask.isSuccessful) {
                                    Toast.makeText(
                                        this@ProfileManualAddress,
                                        "Failed to save latitude: ${latitudeSaveTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }


                        locationRef.child("longitude").setValue(longitude)
                            .addOnCompleteListener { longitudeSaveTask ->
                                if (!longitudeSaveTask.isSuccessful) {
                                    Toast.makeText(
                                        this@ProfileManualAddress,
                                        "Failed to save longitude: ${longitudeSaveTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        locationRef.child("type").setValue(selectedAddressType)
                            .addOnCompleteListener { longitudeSaveTask ->
                                if (!longitudeSaveTask.isSuccessful) {
                                    Toast.makeText(
                                        this@ProfileManualAddress,
                                        "Failed to save longitude: ${longitudeSaveTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        val currentDate = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())

                        locationRef.child("LocationAddedTime").setValue(currentDate)
                            .addOnCompleteListener { timeSaveTask ->
                                if (timeSaveTask.isSuccessful) {
                                    Toast.makeText(
                                        this@ProfileManualAddress,
                                        "",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this@ProfileManualAddress,
                                        "Failed to save LocationAddedTime: ${timeSaveTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        // Store shopname under Locations -> userId -> shopname
                        database.child("Addresses").child(userId).child("Shop Id")
                            .setValue(shopsWithinThreshold)
                            .addOnCompleteListener { shopnameSaveTask ->
                                if (!shopnameSaveTask.isSuccessful) {
                                    Toast.makeText(
                                        this@ProfileManualAddress,
                                        "Failed to save shopname: ${shopnameSaveTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        // Store address data under selected address type
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        val currentUserId = currentUser?.uid

                        selectedAddressType?.let {
                            locationRef.child(it).setValue(locationData)
                                .addOnCompleteListener { addressSaveTask ->
                                    if (addressSaveTask.isSuccessful) {
                                        if (currentUserId != null) {
                                            val userCartRef = FirebaseDatabase.getInstance().getReference("user")
                                                .child(currentUserId)
                                                .child("cartItems")

                                            userCartRef.removeValue()
                                                .addOnCompleteListener { cartRemoveTask ->
                                                    if (cartRemoveTask.isSuccessful) {
                                                        Toast.makeText(
                                                            this@ProfileManualAddress,
                                                            "Address saved successfully",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        finish() // Finish the activity after saving
                                                    } else {
                                                        Toast.makeText(
                                                            this@ProfileManualAddress,
                                                            "Failed to clear cart items: ${cartRemoveTask.exception?.message}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                        } else {
                                            Toast.makeText(
                                                this@ProfileManualAddress,
                                                "Failed to get current user ID",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        Toast.makeText(
                                            this@ProfileManualAddress,
                                            "Failed to save address: ${addressSaveTask.exception?.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        }
                    } else {
                        Toast.makeText(
                            this@ProfileManualAddress,
                            "Shops are not available in your location",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@ProfileManualAddress,
                        "Error geocoding address",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: IOException) {
                Toast.makeText(
                    this@ProfileManualAddress,
                    "Error geocoding address: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.addOnFailureListener {
            Toast.makeText(
                this@ProfileManualAddress,
                "Failed to retrieve user distance from Firebase",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}