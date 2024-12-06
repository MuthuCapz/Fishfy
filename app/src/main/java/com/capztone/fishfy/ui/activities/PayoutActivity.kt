package com.capztone.fishfy.ui.activities

import android.content.Intent
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.capztone.fishfy.databinding.ActivityPayoutBinding
import com.capztone.fishfy.ui.activities.models.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.capztone.fishfy.R
import android.util.Log

import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.ContextCompat
import android.widget.ImageButton
import android.widget.LinearLayout
import com.capztone.admin.utils.FirebaseAuthUtil
import com.capztone.fishfy.databinding.CustomDialogLayoutBinding
import com.capztone.fishfy.ui.activities.fragments.PayoutAddressFragment
import com.google.android.gms.maps.model.LatLng
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar


class PayoutActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var binding: ActivityPayoutBinding

    private lateinit var address: String
    private var lat: Double? = null
    private var lng: Double? = null

    private lateinit var totalAmount: String


    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var userId: String
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var adjustedTotalAmount: Int = 0

    private lateinit var foodItemName: ArrayList<String>
    private lateinit var foodItemPrice: ArrayList<Double>
    private lateinit var foodItemDescription: ArrayList<String>
    private lateinit var foodItemIngredient: ArrayList<String>
    private lateinit var foodItemImage: ArrayList<String>
    private lateinit var foodItemQuantities: ArrayList<Int>
    private var selectedSlot: String? = null
    private var selectedOptionText: String = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPayoutBinding.inflate(layoutInflater)


        setContentView(binding.root)

        // Initialize Razorpay Checkout
        Checkout.preload(applicationContext)


        selectedSlot = selectedOptionText




        binding.Slot.text = selectedSlot


        auth = FirebaseAuthUtil.auth
        databaseReference = FirebaseDatabase.getInstance().reference
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setUserData()
        setDefaultAddress()
        binding.recentBackButton.setOnClickListener {
            onBackPressed()
        }
        binding.changeAddress.setOnClickListener {
            val addressFragment = PayoutAddressFragment()
            addressFragment.show(supportFragmentManager, addressFragment.tag)
        }
        binding.address.setOnClickListener {
            val addressFragment = PayoutAddressFragment()
            addressFragment.show(supportFragmentManager, addressFragment.tag)
        }

        binding.slotdrop.setOnClickListener { showPopupMenu1() }
        binding.Slot.setOnClickListener { showPopupMenu1() }
        binding.time.setOnClickListener { showPopupMenu1() }

        // Get user details form Firebase
        foodItemName = intent.getStringArrayListExtra("foodItemName") as ArrayList<String>
        foodItemPrice = intent.getStringArrayListExtra("foodItemPrice") as ArrayList<Double>
        foodItemDescription =
            intent.getStringArrayListExtra("foodItemDescription") as ArrayList<String>
        foodItemIngredient =
            intent.getStringArrayListExtra("foodItemIngredient") as ArrayList<String>
        foodItemImage = intent.getStringArrayListExtra("foodItemImage") as ArrayList<String>
        foodItemQuantities = intent.getIntegerArrayListExtra("foodItemQuantities") as ArrayList<Int>



        retrieveFinalTotalFromFirebase()
        binding.placeMyOrderButton.setOnClickListener {
            // Extract the end time from selectedSlot
            address = binding.payoutAddress.text.toString().trim()
            if (address.isBlank()) {
                Toast.makeText(this, "Please enter all the details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedSlot.isNullOrBlank()) {
                Toast.makeText(this, "Please select Time slot", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val endTimeString = selectedSlot?.split(" - ")?.get(1)?.trim() ?: ""
            Log.d("PayoutActivity", "Extracted end time: $endTimeString")

            if (endTimeString.isNotEmpty()) {
                try {
                    // Define a formatter for the input string
                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

                    // Parse the end time into a Calendar object
                    val endTime = Calendar.getInstance()
                    endTime.time = timeFormat.parse(endTimeString)!!

                    // Get the current time as a Calendar object (time only)
                    val currentTime = Calendar.getInstance()
                    currentTime.set(Calendar.YEAR, 0)
                    currentTime.set(Calendar.MONTH, 0)
                    currentTime.set(Calendar.DAY_OF_MONTH, 0)

                    // Reset the endTime date to ignore date differences
                    endTime.set(Calendar.YEAR, 0)
                    endTime.set(Calendar.MONTH, 0)
                    endTime.set(Calendar.DAY_OF_MONTH, 0)

                    Log.d(
                        "PayoutActivity",
                        "Parsed end time: ${timeFormat.format(endTime.time)}"
                    )
                    Log.d(
                        "PayoutActivity",
                        "Current time: ${timeFormat.format(currentTime.time)}"
                    )

                    // Compare the parsed end time with the current time
                    if (endTime.after(currentTime)) {
                        fetchApiKeyAndStartPayment()
                    } else {
                        Toast.makeText(
                            this,
                            "Time Expired, Please choose another time",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    // Handle parsing errors
                    Log.e("PayoutActivity", "Invalid time format: ${e.message}", e)
                    Toast.makeText(this, "Invalid time format", Toast.LENGTH_SHORT).show()
                }
            } else {
                // If no end time was extracted
                Toast.makeText(this, "Invalid selected slot", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        Log.d("PayoutActivity", "onResume called")
        setDefaultAddress()
    }

    private fun setDefaultAddress() {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            val payoutAddressReference = databaseReference.child("PayoutAddress").child(userId).child("address")
            val userReference = databaseReference.child("Addresses").child(userId)

            payoutAddressReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(payoutSnapshot: DataSnapshot) {
                    if (payoutSnapshot.exists()) {
                        val payoutAddress = payoutSnapshot.getValue(String::class.java)
                        payoutAddress?.let {
                            val extractedAddress = extractAddress(it) // Extract the required portion
                            binding.payoutAddress.setText(payoutAddress)

                            // Convert to latitude and longitude
                            getLatLngFromAddress(extractedAddress) { latLng ->
                                latLng?.let {
                                    binding.lat.text = it.latitude.toString()
                                    binding.lng.text = it.longitude.toString()
                                } ?: run {
                                    Log.e("PayoutActivity", "Failed to fetch latitude/longitude")
                                }
                            }
                        }
                    } else {
                        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(locationSnapshot: DataSnapshot) {
                                val addressFound = findFirstAvailableAddress(locationSnapshot)
                                addressFound?.let {
                                    val extractedAddress = extractAddress(it)
                                    binding.payoutAddress.setText(extractedAddress)

                                    // Optionally update PayoutAddress in Firebase
                                    payoutAddressReference.setValue(it)

                                    // Convert to latitude and longitude
                                    getLatLngFromAddress(extractedAddress) { latLng ->
                                        latLng?.let {
                                            binding.lat.text = it.latitude.toString()
                                            binding.lng.text = it.longitude.toString()
                                        } ?: run {
                                            Log.e("PayoutActivity", "Failed to fetch latitude/longitude")
                                        }
                                    }
                                } ?: run {
                                    Log.e("PayoutActivity", "No address found in HOME, WORK, or OTHER")
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("PayoutActivity", "Error fetching saved address", error.toException())
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("PayoutActivity", "Error fetching PayoutAddress", error.toException())
                }
            })
        }
    }
    /**
     * Extracts the portion of the address between the first and last commas.
     */
    private fun extractAddress(fullAddress: String): String {
        val parts = fullAddress.split(",")
        return if (parts.size > 2) {
            parts.subList(1, parts.size - 1).joinToString(",").trim()
        } else {
            fullAddress
        }
    }

    /**
     * Converts an address string to latitude and longitude.
     */
    private fun getLatLngFromAddress(address: String, callback: (LatLng?) -> Unit) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addressList = geocoder.getFromLocationName(address, 1)
            if (!addressList.isNullOrEmpty()) {
                val location = addressList[0]
                callback(LatLng(location.latitude, location.longitude))
            } else {
                callback(null)
            }
        } catch (e: IOException) {
            Log.e("PayoutActivity", "Geocoding failed: ${e.message}")
            callback(null)
        }
    }


    // Helper function to find the first available address from HOME, WORK, or OTHER
    private fun findFirstAvailableAddress(snapshot: DataSnapshot): String? {
        val addressTypes = listOf("HOME", "WORK", "OTHER")
        for (addressType in addressTypes) {
            val addressNode = snapshot.child(addressType).child("address")
            if (addressNode.exists()) {
                addressNode.getValue(String::class.java)?.let {
                    return it
                }
            }
        }
        return null
    }

    // Helper function to clean address string
    // Function to format address lines
    private fun cleanAddressString(address: String): String {
        val lines = address.split("\n") // Split address into lines

        // Trim each line and remove trailing comma if present
        val cleanedLines = lines.map { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.endsWith(',')) {
                trimmedLine.substring(0, trimmedLine.length - 1) // Remove trailing comma
            } else {
                trimmedLine
            }
        }

        return cleanedLines.joinToString("\n") // Join lines back into a single string
    }


    private fun retrieveFinalTotalFromFirebase() {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            val userReference = databaseReference.child("user").child(userId)

            userReference.child("OrderTotalAmount")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val finalTotal = snapshot.getValue(Double::class.java)
                        finalTotal?.let {
                            // Format the finalTotal to two decimal places
                            val formattedTotal = String.format("%.2f", it)
                            // Set the formatted total to the TextView
                            binding.amountTotal.text = "₹ $formattedTotal"
                            setDefaultAddress() // Call setDefaultAddress after setting total amount
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(
                            TAG,
                            "Error retrieving final total from Firebase",
                            error.toException()
                        )
                    }
                })
        }
    }

    // Cache for slot timings
    private var cachedSlotTimings: List<String>? = null

    private fun showPopupMenu1() {
        // Inflate the custom dialog layout using ViewBinding
        val dialogBinding = CustomDialogLayoutBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogBinding.root)
        val dialog = dialogBuilder.create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var selectedLayout: LinearLayout? = null

        // Function to change the color of text and image
        fun setSelectionColor(layout: LinearLayout?, isSelected: Boolean) {
            val textView = layout?.getChildAt(1) as? TextView
            val imageButton = layout?.getChildAt(0) as? ImageButton
            val color = if (isSelected) R.color.navy else R.color.black
            textView?.setTextColor(ContextCompat.getColor(this@PayoutActivity, color))
            imageButton?.setColorFilter(
                ContextCompat.getColor(this@PayoutActivity, color),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        // Get the current user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        // Make sure userId is not null before proceeding
        if (userId != null) {
            // Reference to the current user's cart items to retrieve the "path" value
            val userCartRef =
                FirebaseDatabase.getInstance().getReference("user").child(userId).child("cartItems")

            userCartRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(cartSnapshot: DataSnapshot) {
                    if (cartSnapshot.exists()) {
                        // Retrieve the first cart item (if there are multiple, you can adjust this logic)
                        val cartItemSnapshot = cartSnapshot.children.firstOrNull()
                        val shopPath = cartItemSnapshot?.child("path")?.getValue(String::class.java)

                        if (shopPath != null) {
                            // Now use this "path" to fetch the slot timings from "Delivery Details/path/Slot Timings"
                            val slotTimingsRef = FirebaseDatabase.getInstance()
                                .getReference("Delivery Details").child(shopPath)
                                .child("Slot Timings")

                            // Check if cachedSlotTimings is available
                            if (cachedSlotTimings != null) {
                                // Use the cached slot timings
                                populateSlotOptions(dialogBinding, cachedSlotTimings!!)
                            } else {
                                // Fetch slot timings from Firebase
                                slotTimingsRef.addListenerForSingleValueEvent(object :
                                    ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        val slotTimings = mutableListOf<String>()

                                        // Loop through the Firebase data to get slot timings
                                        for (snapshot in dataSnapshot.children) {
                                            val slotTiming = snapshot.getValue(String::class.java)
                                            slotTiming?.let { slotTimings.add(it) }
                                        }

                                        // Cache the slot timings
                                        cachedSlotTimings = slotTimings

                                        // Now dynamically create options based on slot timings
                                        populateSlotOptions(dialogBinding, slotTimings)
                                    }

                                    override fun onCancelled(databaseError: DatabaseError) {
                                        // Handle error
                                        Log.e(
                                            TAG,
                                            "Failed to fetch slot timings: ${databaseError.message}"
                                        )
                                    }
                                })
                            }
                        } else {
                            Log.e(TAG, "Shop path not found in cart item")
                        }
                    } else {
                        Log.e(TAG, "No cart items found for user: $userId")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "Failed to read user's cart items: ${databaseError.message}")
                }
            })
        } else {
            Log.e(TAG, "User is not logged in.")
        }


        // Function to populate slot options dynamically


        // Set up the OK button click listener using ViewBinding
        dialogBinding.btnOk.setOnClickListener {
            // Update the selected slot value
            selectedSlot = selectedOptionText // Update selectedSlot with the user-selected value
            findViewById<TextView>(R.id.Slot).text =
                selectedSlot // Update the Slot TextView with selected slot
            dialog.dismiss()
        }

        // Set up the Cancel button click listener using ViewBinding
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    // Helper function to populate the slot options in the dialog
    private fun populateSlotOptions(dialogBinding: CustomDialogLayoutBinding, slotTimings: List<String>) {
        val options = listOf(dialogBinding.one, dialogBinding.two, dialogBinding.three)
        var selectedLayout: LinearLayout? = null
        for ((index, option) in options.withIndex()) {
            if (index < slotTimings.size) {
                val textView = option.getChildAt(1) as TextView
                textView.text = slotTimings[index]
            }
        }
        // Function to change the color of text and image
        fun setSelectionColor(layout: LinearLayout?, isSelected: Boolean) {
            val textView = layout?.getChildAt(1) as? TextView
            val imageButton = layout?.getChildAt(0) as? ImageButton
            val color = if (isSelected) R.color.navy else R.color.black
            textView?.setTextColor(ContextCompat.getColor(this, color))
            imageButton?.setColorFilter(
                ContextCompat.getColor(this, color),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
        // Set click listeners for each option
        for (option in options) {
            option.setOnClickListener {
                selectedLayout?.let { setSelectionColor(it, false) }

                selectedLayout = option
                selectedOptionText = (option.getChildAt(1) as TextView).text.toString()

                setSelectionColor(option, true)
            }
        }
    }



    private fun fetchApiKeyAndStartPayment() {
        val database = FirebaseDatabase.getInstance().getReference("config/razorpay/apiKey")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val apiKey = snapshot.getValue(String::class.java)
                if (apiKey != null) {
                    startPayment(apiKey)
                } else {
                    Toast.makeText(this@PayoutActivity, "API Key not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PayoutActivity, "Error fetching API Key: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun startPayment(apiKey: String) {
        val checkout = Checkout()
        checkout.setKeyID(apiKey)

        val amountTotalString = binding.amountTotal.text.toString().replace("₹", "").trim()
        val adjustedTotalAmount = amountTotalString.toDoubleOrNull() ?: 0.0
        val amountInPaise = (adjustedTotalAmount * 100).toInt()
        val options = JSONObject().apply {
            put("name", "Fishfy") // Your business name
            put("description", "Order Payment")
            put("currency", "INR")
            put("amount", amountInPaise)
        }

        try {
            checkout.open(this, options)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error in payment: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun saveOrderDetails(status: String) {
        val usersRef = databaseReference.child("users").child(auth.currentUser?.uid ?: "")
        usersRef.child("userid").get().addOnSuccessListener { userIdSnapshot ->
            userId = userIdSnapshot.getValue(String::class.java) ?: ""

            if (userId.isNotEmpty()) {
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val currentTimeMillis = System.currentTimeMillis()
                val formattedTime = timeFormat.format(currentTimeMillis)

                val amountTotalString = binding.amountTotal.text.toString().replace("₹", "").trim()
                val adjustedTotalAmount = amountTotalString.toDoubleOrNull() ?: 0.0
                val currentDate = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())

                val shopNames = mutableListOf<String>()
                for (i in 0 until binding.pathContainer.childCount) {
                    val textView = binding.pathContainer.getChildAt(i) as TextView
                    shopNames.add(textView.text.toString())
                }

                val skuSet = mutableSetOf<String>()
                val skuUnitQuantities = hashMapOf<String, String>() // Map to store UnitQuantity as String

                val shopsRef = databaseReference.child("Shops")

                // Iterate through each shop name
                shopNames.forEach { shopName ->
                    val shopRef = shopsRef.child(shopName)
                    shopRef.get().addOnSuccessListener { shopSnapshot ->
                        shopSnapshot.children.forEach { categorySnapshot ->
                            categorySnapshot.children.forEach { skuSnapshot ->
                                val skuFoodDescription = skuSnapshot.child("foodDescription").getValue(String::class.java)
                                val skuFoodDescriptions = skuSnapshot.child("foodDescriptions").getValue(String::class.java)

                                if ((skuFoodDescription != null && foodItemDescription.contains(skuFoodDescription)) ||
                                    (skuFoodDescriptions != null && foodItemDescription.contains(skuFoodDescriptions))) {

                                    val skuId = skuSnapshot.key
                                    if (skuId != null && skuId.startsWith("SKU-")) {
                                        skuSet.add(skuId)
                                    }
                                }
                            }
                        }

                        val allUsersRef = databaseReference.child("user")
                        allUsersRef.get().addOnSuccessListener { allUsersSnapshot ->
                            allUsersSnapshot.children.forEach { userSnapshot ->
                                val cartItemsSnapshot = userSnapshot.child("cartItems")
                                skuSet.forEach { skuId ->
                                    val unitQuantity = cartItemsSnapshot.child(skuId).child("UnitQuantity").getValue(String::class.java)
                                    if (!unitQuantity.isNullOrEmpty()) {
                                        // Extract numeric value and unit
                                        val numericValue = unitQuantity.replace("[^\\d.]".toRegex(), "").toDoubleOrNull()
                                        val unit = unitQuantity.replace("[\\d.\\s]".toRegex(), "").lowercase()

                                        if (numericValue != null) {
                                            val formattedValue = when (unit) {
                                                "g" -> if (numericValue >= 1000) {
                                                    "%.1fkg".format(numericValue / 1000) // Convert grams to kilograms if >= 1000g
                                                } else {
                                                    "${numericValue.toInt()}g" // Keep value in grams if < 1000g
                                                }
                                                else -> null // Unknown unit, skip this entry
                                            }

                                            // Store the formatted value in skuUnitQuantities map if valid
                                            if (formattedValue != null) {
                                                skuUnitQuantities[skuId] = formattedValue
                                            }
                                        }
                                    }
                                }
                            }
                            val  delLat = binding.lat.text.toString().trim().toDoubleOrNull() ?: 0.0
                            val  delLng = binding.lng.text.toString().trim().toDoubleOrNull() ?: 0.0


                            // Proceed with saving order details
                            if (shopNames.last() == shopName) {
                                val orderTrackingRef = databaseReference.child("OrderTracking").child("lastOrderNumber")
                                orderTrackingRef.get().addOnSuccessListener { snapshot ->
                                    val lastOrderNumber = snapshot.getValue(Int::class.java) ?: 100000
                                    val newOrderNumber = lastOrderNumber + 1
                                    val itemPushKey = "ORD$newOrderNumber"

                                    orderTrackingRef.setValue(newOrderNumber)

                                    val orderDetails = OrderDetails(
                                        userId,
                                        foodItemName,
                                        foodItemPrice,
                                        foodItemImage,
                                        foodItemQuantities,
                                        address,
                                        adjustedTotalAmount.toInt(),
                                        itemPushKey,
                                        currentDate,
                                        status == "success",
                                        shopNames,
                                        selectedSlot,
                                        foodItemDescription,
                                        skuSet.toList(),
                                        skuUnitQuantities.values.toList(),
                                        delLat,
                                        delLng,// Store only quantities as Strings
                                    )

                                    val orderReference = databaseReference.child("OrderDetails").child(itemPushKey)
                                    orderReference.setValue(orderDetails)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "Order details saved with status: $status")
                                            removeItemFromCart()
                                            removeItemFromCart1()
                                            addOrderToHistory(orderDetails)
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this, "Failed to save order details", Toast.LENGTH_SHORT).show()
                                        }
                                }.addOnFailureListener {
                                    Toast.makeText(this, "Failed to retrieve last order number", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }.addOnFailureListener {
                            Toast.makeText(this, "Failed to retrieve users for UnitQuantity", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this, "Failed to retrieve data for shop: $shopName", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "User ID not found in 'users' path", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to retrieve user ID", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPaymentSuccess(paymentId: String) {
        Toast.makeText(this, "Payment Successful", Toast.LENGTH_SHORT).show()
        saveOrderDetails("success") // Update the status to success after payment
        // Navigate to MainActivity after saving order details
        val intent = Intent(this, MainActivity::class.java)

        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish() // Optionally finish the current activity

    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment Failed or Canceled", Toast.LENGTH_SHORT).show()
    }


    private fun removeItemFromCart() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference
            val cartItemReference = databaseReference.child("user").child(uid).child("cartItems")
            cartItemReference.removeValue().addOnSuccessListener {
                // Handle success
            }.addOnFailureListener { error ->
                // Handle failure
                Toast.makeText(this, "Failed to remove cartitems 😒", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeItemFromCart1() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference
            val cartItemReference = databaseReference.child("Home").child(uid).child("cartItems")
            cartItemReference.removeValue().addOnSuccessListener {
                // Handle success
            }.addOnFailureListener { error ->
                // Handle failure
                Toast.makeText(this, "Failed to remove cartitems 😒", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addOrderToHistory(orderDetails: OrderDetails) {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            // Get current date and time
            val currentDate = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())


            // Store the order details in Firebase
            val orderRef = databaseReference.child("user").child(userId).child("BuyHistory")
                .child(orderDetails.itemPushKey!!)

            // Set the order details
            orderRef.setValue(orderDetails)
                .addOnSuccessListener {
                    // After successfully storing the order details, store the OrderTime separately
                    orderRef.child("OrderedTime").setValue(currentDate)
                        .addOnSuccessListener {
                            Log.d(TAG, "Order added to history with OrderTime")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to store OrderTime", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to add order to history", e)
                }
        }
    }



    private fun setUserData() {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            val userReference = databaseReference.child("user").child(userId)
            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val cartItemsSnapshot = snapshot.child("cartItems")
                        if (cartItemsSnapshot.exists()) {
                            // Clear the existing views from the pathContainer
                            binding.pathContainer.removeAllViews()
                            // Iterate through each itemPushKey
                            cartItemsSnapshot.children.forEach { itemSnapshot ->
                                val path = itemSnapshot.child("path").getValue(String::class.java)
                                // Display the path for each product
                                displayProductPath(path)
                            }
                        }
                        val address = snapshot.child("address").getValue(String::class.java) ?: ""
                        binding.apply {
                            payoutAddress.setText(address)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error reading user data", error.toException())
                }
            })
        }
    }

    private fun displayProductPath(path: String?) {
        val textView = TextView(this)
        textView.text = path
        textView.textSize = 16f
        textView.setPadding(0, 8, 0, 8)
        binding.pathContainer.addView(textView)
    }

    companion object {
        private const val TAG = "PayoutActivity"
    }
}