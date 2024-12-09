package com.capztone.fishfy.ui.activities.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.FragmentAddressBinding
import com.capztone.fishfy.ui.activities.ProfileManualAddress
import com.capztone.fishfy.ui.activities.adapters.AddressAdapter
import com.capztone.fishfy.ui.activities.models.Address
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AddressFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddressBinding? = null
    private val binding get() = _binding!!
    private lateinit var addressAdapter: AddressAdapter
    private var selectedAddress: String? = null
    interface AddressSelectionListener {
        fun onAddressSelected(address: String)
    }

    private var listener: AddressSelectionListener? = null

    fun setAddressSelectionListener(listener: AddressSelectionListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddressBinding.inflate(inflater, container, false)
        // Show loading indicator
        binding.progress.visibility = View.VISIBLE

        binding.progress.setProgressVector(resources.getDrawable(R.drawable.spinload))
        binding.progress.setTextViewVisibility(true)
        binding.progress.setTextStyle(true)
        binding.progress.setTextColor(Color.YELLOW)
        binding.progress.setTextSize(12F)
        binding.progress.setTextMsg("Please Wait")
        binding.progress.setEnlarge(5)
        // Start a delay to hide the loading indicator after 1200 milliseconds (1.2 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            _binding?.let {
                it.progress.visibility = View.GONE
                // Call your method to retrieve cart items or perform other operations
            }
        }, 1200)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Initialize AddressAdapter
        addressAdapter = AddressAdapter(requireContext()) { address ->
            selectedAddress = address.toString()
        }
        binding.detailGoToBackImageButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Set up RecyclerView
        binding.addressrecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = addressAdapter
        }

        binding.newAddress.setOnClickListener {
            val intent = Intent(requireContext(), ProfileManualAddress::class.java)
            startActivity(intent)
        }

        // Check if network is available, if not, load from local storage
        if (isNetworkAvailable()) {
            // Retrieve addresses from Firebase
            retrieveAddressesFromFirebase()
        } else {
            // Load addresses from local storage
            loadAddressesFromLocalStorage()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    private fun retrieveAddressesFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val databaseReference = FirebaseDatabase.getInstance().getReference("PayoutAddress").child(uid).child("address")
            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val addresses = mutableListOf<Address>()
                    for (addressSnapshot in snapshot.children) {
                        val address = addressSnapshot.getValue(Address::class.java)
                        address?.let { addresses.add(it) }
                    }
                    addressAdapter.setAddresses(addresses)
                    saveAddressesToLocalStorage(addresses) // Save to local storage
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load addresses", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun saveAddressesToLocalStorage(addresses: List<Address>) {
        val sharedPreferences = requireContext().getSharedPreferences("addresses", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(addresses)
        editor.putString("address_list", json)
        editor.apply()
    }

    private fun loadAddressesFromLocalStorage() {
        val sharedPreferences = requireContext().getSharedPreferences("addresses", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("address_list", null)
        val type = object : TypeToken<List<Address>>() {}.type
        val addresses: List<Address>? = gson.fromJson(json, type)
        addresses?.let {
            addressAdapter.setAddresses(it)
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        dialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        dialog?.behavior?.skipCollapsed = true
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
