package com.capztone.fishfy.ui.activities.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.FragmentPayoutAddressBinding
import com.capztone.fishfy.ui.activities.ProfileManualAddress
import com.capztone.fishfy.ui.activities.adapters.PayoutAddressAdapter
import com.capztone.fishfy.ui.activities.models.Address
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class  PayoutAddressFragment : BottomSheetDialogFragment(),PayoutAddressAdapter.OnAddressSelectedListener {

    private var _binding:   FragmentPayoutAddressBinding? = null
    private val binding get() = _binding!!
    private lateinit var addressAdapter: PayoutAddressAdapter
    private lateinit var navController: NavController

    private var selectedAddress: String? = null
    interface AddressSelectionListener {
        fun onAddressSelected(address: String)
    }






    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPayoutAddressBinding.inflate(inflater, container, false)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Initialize AddressAdapter
        addressAdapter = PayoutAddressAdapter(requireContext(),this) { address ->
            selectedAddress = address.toString()
        }




        // Set up RecyclerView
        binding.addressrecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = addressAdapter
        }


    }

    override fun onAddressSelected(newSelectedAddress: String) {
        // Save the selected address or any other necessary actions
        // Example: Save to SharedPreferences or Firebase

        // Dismiss the fragment after selection
        dismiss()
    }


    // Helper function to get the address at position 0 from adapter
    private fun PayoutAddressAdapter.getFirstAddress(): Address? {
        return if (addresses.isNotEmpty()) {
            addresses[0]
        } else {
            null
        }
    }
    private fun storeAddressInFirebase(address: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val databaseReference = FirebaseDatabase.getInstance().getReference("PayoutAddress").child(uid).child("address")

            // Store the address directly
            databaseReference.setValue(address)
                .addOnSuccessListener {
                    Toast.makeText(context, "Address changed successfully", Toast.LENGTH_SHORT).show()
                    dismiss()
                    // Optionally, navigate to the next screen or perform other actions
                }
                .addOnFailureListener { e ->
                    Log.e("AddressFragment", "Error storing address", e)
                    Toast.makeText(context, "Failed to store address", Toast.LENGTH_SHORT).show()
                }
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