package com.capztone.fishfy.ui.activities.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import com.capztone.fishfy.R
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView

import com.capztone.fishfy.ui.activities.models.Address
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.text.InputFilter
import android.text.TextWatcher
import com.capztone.fishfy.databinding.DialogDeleteConfirmationBinding
import com.capztone.fishfy.databinding.PayoutSavedAddressBinding
import com.capztone.fishfy.ui.activities.Utils.AddressStorage
import com.capztone.fishfy.ui.activities.Utils.ToastHelper
import com.capztone.fishfy.ui.activities.fragments.PayoutAddressFragment

class PayoutAddressAdapter(
    private val context: Context,
    private val listener: OnAddressSelectedListener,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<PayoutAddressAdapter.AddressViewHolder>() {

    val addresses = mutableListOf<Address>()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val database: DatabaseReference? =
        userId?.let { FirebaseDatabase.getInstance().getReference("Addresses").child(it) }


    private var selectedPosition: Int
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("AddressPreferences", Context.MODE_PRIVATE)

    init {
        selectedPosition = sharedPreferences.getInt(
            "selectedPosition",
            0
        ) // Retrieve saved position or default to 0
        fetchData()

    }

    interface OnAddressSelectedListener {
        fun onAddressSelected(newSelectedAddress: String)
    }


    private fun saveAddressesLocally() {
        AddressStorage.saveAddresses(context, addresses)
    }

    private fun fetchData() {
        if (isNetworkAvailable(context)) {
            val paths = listOf("HOME", "WORK", "OTHER")
            paths.forEach { path ->
                fetchAddressesFromPath(path)
                checked()
            }
        } else {
            addresses.addAll(AddressStorage.getAddresses(context))
            notifyDataSetChanged()
        }
    }


    private fun fetchAddressesFromPath(path: String) {
        database?.child(path)?.child("address")
            ?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("AddressAdapter", "Data fetched for path: $path")
                    val address = snapshot.getValue(String::class.java)
                    if (address != null) {
                        Log.d("AddressAdapter", "Address found: $address")
                        val existingAddress = addresses.find { it.addressType == path }
                        if (existingAddress != null) {
                            existingAddress.address = address
                        } else {
                            addresses.add(Address(path, address))
                        }
                        notifyDataSetChanged()
                        saveAddressesLocally()

                    } else {
                        Log.d("AddressAdapter", "No address found for path: $path")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(
                        "AddressAdapter",
                        "Error fetching data for path: $path",
                        error.toException()
                    )
                }
            })
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun checked() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseRef =
            FirebaseDatabase.getInstance().getReference("Addresses").child(userId).child("type")

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val type = snapshot.getValue(String::class.java) ?: return

                selectedPosition = when (type) {
                    "WORK" -> addresses.indexOfFirst { it.addressType == "WORK" }
                    "OTHER" -> addresses.indexOfFirst { it.addressType == "OTHER" }
                    "HOME" -> addresses.indexOfFirst { it.addressType == "HOME" }
                    else -> 0
                }

            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val binding =
            PayoutSavedAddressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AddressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        holder.bind(addresses[position])
    }

    override fun getItemCount() = addresses.size
    fun setAddresses(newAddresses: List<Address>) {

        addresses.addAll(newAddresses)
        notifyDataSetChanged()
    }

    inner class AddressViewHolder(private val binding: PayoutSavedAddressBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            checked()
            binding.selectaddress.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    // Check shop name before selecting
                    val selectedAddressType = addresses[adapterPosition].addressType
                    checkShopNameAndProceed(selectedAddressType, adapterPosition)


                }
            }
        }

        fun bind(address: Address) {
            // Split the address into lines
            val lines = address.address.split("\n")

            // Remove comma at the end of each line if present
            val cleanedLines = lines.map { line ->
                line.trim().let {
                    if (it.isNotEmpty() && it.last() == ',') {
                        it.dropLast(1)
                    } else {
                        it
                    }
                }
            }
            // Join the lines back together with '\n'
            val displayAddress = cleanedLines.joinToString("\n")
            binding.address.setText(displayAddress)

            // Set icon based on address type
            when (address.addressType.toUpperCase()) { // Ensure comparison is case insensitive
                "HOME" -> {
                    binding.iconimg.setImageResource(R.drawable.addresshome)
                }

                "WORK" -> {
                    binding.iconimg.setImageResource(R.drawable.work)
                }

                "OTHER" -> {
                    binding.iconimg.setImageResource(R.drawable.loco)
                }

            }
            binding.Home.text = address.addressType
            binding.selectaddress.isChecked = adapterPosition == selectedPosition



        }

        private fun checkShopNameAndProceed(selectedAddressType: String, newPosition: Int) {
            // Ensure newPosition is within bounds of the addresses list
            if (newPosition < 0 || newPosition >= addresses.size) {
                Log.e("AddressAdapter", "Invalid newPosition: $newPosition")
                return
            }

            // Fetch the shop name of the current selected address
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val selectedAddressShopNameRef = database?.child(selectedAddressType)?.child("shopname")

            selectedAddressShopNameRef?.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val selectedShopName = snapshot.getValue(String::class.java) ?: ""

                    // Ensure selectedPosition is valid
                    if (selectedPosition < 0 || selectedPosition >= addresses.size) {
                        Log.e("AddressAdapter", "Invalid selectedPosition: $selectedPosition")
                        return
                    }

                    // Fetch the shop name of the previously selected address (from Firebase or local storage)
                    val previousAddressShopNameRef =
                        database?.child(addresses[selectedPosition].addressType)?.child("Shop Id")
                    previousAddressShopNameRef?.addListenerForSingleValueEvent(object :
                        ValueEventListener {
                        override fun onDataChange(previousSnapshot: DataSnapshot) {
                            val previousShopName =
                                previousSnapshot.getValue(String::class.java) ?: ""

                            // Compare the shop names
                            if (selectedShopName == previousShopName) {
                                // Shop names match, allow selection and save the new position
                                selectedPosition = newPosition
                                saveSelectedPosition(selectedPosition)
                                listener.onAddressSelected(addresses[selectedPosition].addressType)
                                notifyDataSetChanged()

                                // Store the newly selected address in the "PayoutAddress" path in Firebase
                                val newSelectedAddress = addresses[newPosition].address
                                val payoutAddressRef =
                                    FirebaseDatabase.getInstance().getReference("PayoutAddress")
                                        .child(userId)
                                        .child("address")

                                // Save the new address to Firebase
                                payoutAddressRef.setValue(newSelectedAddress)
                                    .addOnSuccessListener {
                                        Log.d("AddressAdapter", "New address saved successfully.")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("AddressAdapter", "Error saving new address", e)
                                    }
                            } else {
                                // Shop names don't match, show a toast and don't select the radio button
                                binding.selectaddress.isChecked = false
                                ToastHelper.showCustomToast(
                                    context,
                                    "Does not deliver to your location. Change address in profile")

                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(
                                "AddressAdapter",
                                "Error fetching previous shop name",
                                error.toException()
                            )
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(
                        "AddressAdapter",
                        "Error fetching selected shop name",
                        error.toException()
                    )
                }
            })
        }

        private fun saveSelectedPosition(position: Int) {
            with(sharedPreferences.edit()) {
                putInt("selectedPosition", position)
                apply()
                checked()

            }
        }

        private fun showEditDialog(addressType: String, currentAddress: String) {
            val context = binding.root.context
            val layoutInflater = LayoutInflater.from(context)
            val customLayout = layoutInflater.inflate(R.layout.dialog_edit_address, null)

            val editTextAddress = customLayout.findViewById<EditText>(R.id.edit_text_address)
            val buttonSave = customLayout.findViewById<AppCompatButton>(R.id.button_save)
            val buttonCancel = customLayout.findViewById<AppCompatButton>(R.id.button_cancel)

            // Set initial text to currentAddress
            editTextAddress.setText(currentAddress)

            // Set maxLines to 5
            editTextAddress.maxLines = 5

            // Set input filter to limit the length to 150 characters
            val maxLength = 150
            val inputFilter = InputFilter.LengthFilter(maxLength)
            editTextAddress.filters = arrayOf(inputFilter)

            // Set up TextWatcher to handle automatic insertion of "+91" after 10 digits
            editTextAddress.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val text = s.toString()

                    // Check if there are exactly 10 digits and no "+91" prefix yet
                    if (text.length == 10 && !text.startsWith("+91")) {
                        editTextAddress.setText("+91$text")
                        editTextAddress.setSelection(editTextAddress.text.length) // Move cursor to end
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            val dialog = AlertDialog.Builder(context)
                .setView(customLayout)
                .create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            buttonSave.setOnClickListener {
                val newAddress = editTextAddress.text.toString()

                // Check if the entered text length is within the limit
                if (newAddress.length > maxLength) {
                    editTextAddress.error = "Address cannot be longer than $maxLength characters"
                } else if (newAddress.startsWith("+91") && newAddress.length < 13) {
                    // Display toast if more than 10 digits are entered after "+91"
                    Toast.makeText(
                        context,
                        "Please enter only 10 numeric digits after +91",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    updateAddressInFirebase(addressType, newAddress)
                    updateAddressInFirebase1(addressType, newAddress)
                    dialog.dismiss()
                }
            }

            buttonCancel.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        private fun updateAddressInFirebase(addressType: String, newAddress: String) {
            database?.child(addressType)?.child("address")?.setValue(newAddress)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        addresses.find { it.addressType == addressType }?.address = newAddress
                        notifyDataSetChanged()
                        Toast.makeText(
                            binding.root.context,
                            "Address updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            binding.root.context,
                            "Failed to update address",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        private fun saveSelectedAddressToFirebase(selectedAddress: String) {
            userId?.let { user ->
                FirebaseDatabase.getInstance().getReference("PayoutAddress").child(user)
                    .child("address").setValue(selectedAddress)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(
                                "AddressAdapter",
                                "Selected address saved under PayoutAddress: $selectedAddress"
                            )
                        } else {
                            Log.e(
                                "AddressAdapter",
                                "Error saving selected address under PayoutAddress",
                                task.exception
                            )
                        }
                    }
            }
        }

        private fun updateAddressInFirebase1(addressType: String, newAddress: String) {
            userId?.let {
                FirebaseDatabase.getInstance().getReference("PayoutAddress").child(it)
                    .child("address")
                    .setValue(newAddress)
            }?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    addresses.find { it.addressType == addressType }?.address = newAddress
                    notifyDataSetChanged()

                } else {
                    Toast.makeText(
                        binding.root.context,
                        "Failed to update Payout address",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }


        private fun saveSelectedAddressToFirebase1(selectedAddress: Int, addressType: String) {
            userId?.let { user ->
                val addressTypeRef =
                    FirebaseDatabase.getInstance().getReference("Addresses").child(user)
                        .child(addressType)

                // Retrieve existing address type details
                addressTypeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            // Retrieve address details
                            val locality =
                                snapshot.child("locality").getValue(String::class.java) ?: ""
                            val shopName =
                                snapshot.child("shopname").getValue(String::class.java) ?: ""
                            val latitude =
                                snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                            val longitude =
                                snapshot.child("longitude").getValue(Double::class.java) ?: 0.0


                            // Create a map to store the shopname, locality, latitude, and longitude
                            val addressData = mapOf(
                                "shopname" to shopName,
                                "locality" to locality,
                                "latitude" to latitude,
                                "longitude" to longitude,
                                "type" to addressType

                            )

                            // Retrieve existing data under "Locations/userId"
                            val userLocationsRef =
                                FirebaseDatabase.getInstance().getReference("Addresses").child(user)

                            userLocationsRef.addListenerForSingleValueEvent(object :
                                ValueEventListener {
                                override fun onDataChange(existingSnapshot: DataSnapshot) {
                                    // Preserve existing data
                                    val existingData =
                                        existingSnapshot.value as? Map<String, Any> ?: mapOf()
                                    val updatedData = existingData.toMutableMap()

                                    // Add or update new address data
                                    updatedData.putAll(addressData)

                                    // Store updated data in "Locations/userId"
                                    userLocationsRef.setValue(updatedData)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                Toast.makeText(
                                                    context,
                                                    "Selected address saved successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Failed to save selected address",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(
                                        context,
                                        "Error retrieving existing data: ${error.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            })
                        } else {
                            // Address type does not exist, show a toast message
                            Toast.makeText(
                                context,
                                "Address type does not exist",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            context,
                            "Error checking address type: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
        }

        private fun updateSelectedPositionAfterDeletion() {
            // If the deleted item was the selected one
            if (adapterPosition == selectedPosition) {
                selectedPosition = 0 // Reset to the first item or handle accordingly
            } else if (adapterPosition < selectedPosition) {
                selectedPosition-- // Adjust if an earlier item was deleted
            }
            saveSelectedPosition(selectedPosition) // Save updated position
        }

        private fun deleteAddressFromFirebase(addressType: String) {
            val addressRef = database?.child(addressType)
            val payoutAddressRef = userId?.let {
                FirebaseDatabase.getInstance().getReference("PayoutAddress").child(it)
                    .child("address")
            }

            addressRef?.removeValue()
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        payoutAddressRef?.removeValue()?.addOnCompleteListener { task2 ->
                            if (task2.isSuccessful) {
                                addresses.removeIf { it.addressType == addressType }
                                updateSelectedPositionAfterDeletion()
                                notifyDataSetChanged()
                                Log.d(
                                    "AddressAdapter",
                                    "Address removed from both Locations and PayoutAddress"
                                )
                            } else {
                                Log.e(
                                    "AddressAdapter",
                                    "Error deleting address from PayoutAddress",
                                    task2.exception
                                )
                            }
                        }
                    } else {
                        Log.e(
                            "AddressAdapter",
                            "Error deleting address for path: $addressType",
                            task.exception
                        )
                    }
                }

            addressRef?.removeValue()
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val removedIndex = addresses.indexOfFirst { it.addressType == addressType }
                        if (removedIndex != -1) {
                            addresses.removeAt(removedIndex)
                            notifyItemRemoved(removedIndex)
                            notifyItemRangeChanged(removedIndex, addresses.size)

                            // Automatically select a position based on the list size
                            val newSelectedPosition = when {
                                addresses.isEmpty() -> -1 // No items left
                                addresses.size >= 3 -> 0 // Select 3rd item if it exists
                                addresses.size >= 2 -> 1 // Select 2nd item if it exists
                                else -> 0 // Select 1st item if it exists
                            }

                            if (newSelectedPosition >= 0) {
                                selectedPosition = newSelectedPosition
                                saveSelectedPosition(selectedPosition)
                                val newSelectedAddress = addresses[selectedPosition].address
                                saveSelectedAddressToFirebase(newSelectedAddress)
                                notifyDataSetChanged()
                                onItemClick(newSelectedAddress)
                                saveSelectedAddressToFirebase1(
                                    selectedPosition,
                                    addresses[selectedPosition].addressType
                                )
                            } else {
                                // Handle case when no address is left
                                selectedPosition = -1
                                saveSelectedPosition(selectedPosition)
                                notifyDataSetChanged()
                            }
                        } else {
                            Log.e(
                                "AddressAdapter",
                                "Error finding address to delete for path: $addressType"
                            )
                        }
                    } else {
                        Log.e(
                            "AddressAdapter",
                            "Error deleting address for path: $addressType",
                            task.exception
                        )
                    }
                }
        }

    }
}