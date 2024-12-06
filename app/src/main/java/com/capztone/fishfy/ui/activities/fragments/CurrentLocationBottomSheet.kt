package com.capztone.fishfy.ui.activities.fragments

import android.content.Intent
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.capztone.admin.utils.FirebaseAuthUtil
import com.capztone.fishfy.databinding.FragmentCurrentLocationBottomSheetBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.IOException
import kotlin.math.*
import com.capztone.fishfy.R
import com.capztone.fishfy.ui.activities.LocationNotAvailable
import com.capztone.fishfy.ui.activities.MainActivity
import com.google.firebase.database.DatabaseException
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CurrentLocationBottomSheet : DialogFragment() {

    private var _binding: FragmentCurrentLocationBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var selectedAddressType: String? = null
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var geocoder: Geocoder

    // Variables for shop locations
    private val adminDestinations = mutableListOf<Pair<Double, Double>>()
    private val shopNames = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCurrentLocationBottomSheetBinding.inflate(inflater, container, false)
        val address = arguments?.getString("address", "No Address")
        binding.etBuildingName.setText(address)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuthUtil.auth
        database = FirebaseDatabase.getInstance().reference

        geocoder = Geocoder(requireContext())

        // Inside onViewCreated method
        binding.etName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) { // When the focus is lost, check the validation
                validateName()
            }
        }

        binding.etMobileNumber.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) { // When the focus is lost, check the validation
                validateMobileNumber()
            }
        }

        binding.etBuildingName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) { // When the focus is lost, check the validation
                validateAddress()
            }
        }
        // Fetch shop locations from Firebase
        fetchShopLocationsFromFirebase()

        binding.btnSaveAsHome.setOnClickListener {
            onAddressTypeSelected("HOME", it)
        }
        binding.detailGoToBackImageButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.btnSaveAsWork.setOnClickListener {
            onAddressTypeSelected("WORK", it)
        }

        binding.btnSaveAsOther.setOnClickListener {
            onAddressTypeSelected("OTHER", it)
        }

        binding.AddressSave.setOnClickListener {
            if (validateName() && validateMobileNumber() && validateAddress()) {
                saveAddressToFirebase()
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT)
                    .show()
            }
        }


    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun fetchShopLocationsFromFirebase() {
        val shopLocationsRef = database.child("ShopLocations")
        shopLocationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (shopSnapshot in dataSnapshot.children) {
                    val shopName = shopSnapshot.key ?: continue
                    val lat =
                        shopSnapshot.child("latitude").getValue(Double::class.java) ?: continue
                    val lng =
                        shopSnapshot.child("longitude").getValue(Double::class.java) ?: continue
                    adminDestinations.add(Pair(lat, lng))
                    shopNames.add(shopName)
                }

                // Calculate distances once shop locations are fetched
                calculateDistances()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Failed to load shop locations",
                    Toast.LENGTH_SHORT
                ).show()
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
                    val databaseReference = FirebaseDatabase.getInstance()
                        .getReference("Delivery Details/User Distance")
                    databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val distanceThresholdString = dataSnapshot.getValue(String::class.java)
                            val distanceThreshold =
                                distanceThresholdString?.toDoubleOrNull() ?: 10.0

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
                                requireContext(),
                                "Error fetching distance threshold: ${databaseError.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                } else {
                    Toast.makeText(
                        requireContext(),
                        "User not authenticated",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Location not found for the entered address",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: IOException) {
            Toast.makeText(
                requireContext(),
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
                Toast.makeText(requireContext(), "Failed to store nearby shops", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Radius of the Earth in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c // Distance in kilometers
    }

    private fun onAddressTypeSelected(type: String, button: View) {
        // Reset all buttons to default color and icon tint
        resetButtonStyle(binding.btnSaveAsHome, R.color.navy)
        resetButtonStyle(binding.btnSaveAsWork, R.color.navy)
        resetButtonStyle(binding.btnSaveAsOther, R.color.navy)

        // Change the background drawable, text color, and icon tint of the selected button
        if (button is AppCompatButton) {
            button.setBackgroundResource(R.drawable.linearbg) // Use drawable resource for background
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            button.compoundDrawablesRelative.forEach {
                it?.setTint(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                    )
                )
            }
        }
        selectedAddressType = type
    }

    private fun resetButtonStyle(button: AppCompatButton, color: Int) {
        button.setBackgroundResource(R.drawable.colorlinear) // Set the default background drawable
        button.setTextColor(ContextCompat.getColor(requireContext(), color))
        button.compoundDrawablesRelative.forEach {
            it?.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    color
                )
            )
        }
    }

    // Validation functions
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
                setMandatoryFieldIndicatorVisible(true, "Mobile Number *", binding.mblnum)
                false
            }

            mobileNumber.length != 10 -> {
                binding.etMobileNumber.error = "Mobile number must be 10 digits"
                setMandatoryFieldIndicatorVisible(true, "Mobile Number *", binding.mblnum)
                false
            }

            else -> {
                setMandatoryFieldIndicatorVisible(false, "Mobile Number *", binding.mblnum)
                true
            }
        }
    }

    private fun validateAddress(): Boolean {
        val address = binding.etBuildingName.text.toString().trim()
        return when {
            TextUtils.isEmpty(address) -> {
                binding.etBuildingName.error = "Address is required"
                setMandatoryFieldIndicatorVisible(true, "Address *", binding.Address)
                false
            }

            address.length > 150 -> {
                binding.etBuildingName.error = "Address cannot exceed 150 characters"
                setMandatoryFieldIndicatorVisible(true, "Address *", binding.Address)
                false
            }

            else -> {
                setMandatoryFieldIndicatorVisible(false, "Address *", binding.Address)
                true
            }
        }
    }


    private fun setMandatoryFieldIndicatorVisible(
        visible: Boolean,
        text: String,
        textView: TextView
    ) {
        if (visible) {
            textView.visibility = View.VISIBLE
            textView.text = text  // Change text dynamically
        } else {
            textView.visibility = View.VISIBLE
        }
    }

    private fun saveAddressToFirebase() {
        val userId = auth.currentUser?.uid ?: return
        val addressType = selectedAddressType ?: return // Ensure addressType is not null

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

            val name = binding.etName.text.toString().trim()
            val mobileNumber = binding.etMobileNumber.text.toString().trim()
            val addressString = "$name,\n" +
                    "${binding.etBuildingName.text.toString().trim()},\n" +
                    "+91 $mobileNumber"

            try {
                // Use Geocoder to get latitude and longitude from address
                val addresses: MutableList<Address>? =
                    geocoder.getFromLocationName(addressString, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val latitude = addresses[0].latitude
                    val longitude = addresses[0].longitude
                    val locality = addresses[0].locality

                    val nearbyShops = mutableListOf<String>()

                    for (i in adminDestinations.indices) {
                        val shopLat = adminDestinations[i].first
                        val shopLng = adminDestinations[i].second

                        val distance = calculateDistance(latitude, longitude, shopLat, shopLng)

                        if (distance < userDistance) { // Use userDistance instead of 10.0
                            nearbyShops.add(shopNames[i])
                        }
                    }

                    if (nearbyShops.isNotEmpty()) {
                        val shopsWithinThreshold = nearbyShops.joinToString(", ")

                        val locationData = HashMap<String, Any>()
                        locationData["address"] = addressString
                        locationData["latitude"] = latitude
                        locationData["longitude"] = longitude
                        locationData["locality"] = locality
                        locationData["Shop Id"] = shopsWithinThreshold // Save shop IDs

                        // Store address in the "PayoutAddress -> userid -> address" path
                        val payoutAddressRef = database.child("PayoutAddress").child(userId)
                        payoutAddressRef.setValue(locationData).addOnSuccessListener {

                        }.addOnFailureListener {
                            Toast.makeText(
                                requireContext(),
                                "Failed to store address in PayoutAddress",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        database.child("Addresses").child(userId).child("Shop Id")
                            .setValue(shopsWithinThreshold)
                            .addOnCompleteListener { shopIdSaveTask ->
                                if (!shopIdSaveTask.isSuccessful) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to save shop ID: ${shopIdSaveTask.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        val locationRef = database.child("Addresses").child(userId)
                        locationRef.child("latitude").setValue(latitude)
                        locationRef.child("longitude").setValue(longitude)
                        locationRef.child("locality").setValue(locality)
                        locationRef.child("type").setValue(addressType)
                        locationRef.child("LocationAddedTime").setValue(
                            SimpleDateFormat(
                                "dd-MM-yyyy hh:mm a",
                                Locale.getDefault()
                            ).format(Date())
                        )

                        selectedAddressType?.let {
                            val addressRef = database.child("Addresses").child(userId).child(it)
                            addressRef.setValue(locationData)
                                .addOnCompleteListener { addressSaveTask ->
                                    if (addressSaveTask.isSuccessful) {
                                        // Address saved successfully, now delete cartItems
                                        val userRef = database.child("user").child(userId)
                                        userRef.child("cartItems").removeValue()
                                            .addOnCompleteListener { cartDeleteTask ->
                                                if (cartDeleteTask.isSuccessful) {
                                                    Toast.makeText(
                                                        requireContext(),
                                                        "Address saved successfully",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        requireContext(),
                                                        "Address saved successfully, but failed to delete cart items: ${cartDeleteTask.exception?.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }

                                                // Redirect to MainActivity
                                                val intent = Intent(
                                                    requireContext(),
                                                    MainActivity::class.java
                                                )
                                                startActivity(intent)
                                            }
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            "Failed to save address: ${addressSaveTask.exception?.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            val userDetailsRef = database.child("Addresses").child(userId).child("User Details")
                            userDetailsRef.child("user name").setValue(name)
                                .addOnCompleteListener { nameSaveTask ->
                                    if (!nameSaveTask.isSuccessful) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Failed to save username: ${nameSaveTask.exception?.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            userDetailsRef.child("mobile number").setValue(mobileNumber)
                                .addOnCompleteListener { mobileNumberSaveTask ->
                                    if (!mobileNumberSaveTask.isSuccessful) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Failed to save mobile number: ${mobileNumberSaveTask.exception?.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                        }

                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Shops are not available in your location",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Location not found for the entered address",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: IOException) {
                Toast.makeText(
                    requireContext(),
                    "Error geocoding address: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.addOnFailureListener {
            Toast.makeText(
                requireContext(),
                "Failed to retrieve User Distance: ${it.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}