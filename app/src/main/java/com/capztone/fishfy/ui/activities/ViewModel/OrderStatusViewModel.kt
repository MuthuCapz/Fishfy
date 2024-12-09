// OrderStatusViewModel.kt

package com.capztone.fishfy.ui.activities.ViewModel

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.lottie.LottieAnimationView
import com.capztone.fishfy.databinding.ActivityOrderStatusBinding
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OrderStatusViewModel : ViewModel() {

    private lateinit var database: DatabaseReference
    private lateinit var itemPushKey: String
    private lateinit var orderDeliveredAnimationView: LottieAnimationView
    private lateinit var binding: ActivityOrderStatusBinding

    fun init(itemPushKey: String, binding: ActivityOrderStatusBinding) {
        this.itemPushKey = itemPushKey
        this.binding = binding
        database = FirebaseDatabase.getInstance().reference
        orderDeliveredAnimationView = binding.deliverylottie


        observeOrderStatusChanges()
        observeEstimatedTimeChanges()
        observeUsernameChanges()
    }

    private fun observeOrderStatusChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            database.child("OrderDetails").child(itemPushKey).child("Status")
                .addValueEventListener(object : ValueEventListener {
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val status = dataSnapshot.child("message").getValue(String::class.java)

                        val picked =
                            dataSnapshot.child("pickedDate").getValue(String::class.java)
                        binding.pickDate.text =  picked
                        val deliver =
                            dataSnapshot.child("deliveredDate").getValue(String::class.java)
                        binding.deliDate.text =  deliver
                        val confirm =
                            dataSnapshot.child("confirmDate").getValue(String::class.java)
                        binding.confirmDate.text =  confirm
                        updateUI(status)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle error
                    }
                })
        }
    }

    private fun updateUI(status: String?) {
        when (status) {
            "Order picked" -> {
                // Show line from Confirmed to Picked
                binding.lineConfirmToPicked.visibility = View.VISIBLE
            }
            "Order delivered" -> {
                // Show line from Picked to Delivered
                binding.linePickedToDelivered.visibility = View.VISIBLE
                binding.lineConfirmToPicked.visibility = View.VISIBLE
                orderDeliveredAnimationView.visibility = View.VISIBLE
                orderDeliveredAnimationView.playAnimation()

            }
            else -> {
                // Hide lines for other statuses
                binding.linePickedToDelivered.visibility = View.GONE
                orderDeliveredAnimationView.visibility = View.GONE

            }
        }
    }

    private fun observeEstimatedTimeChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            database.child("OrderDetails").child(itemPushKey)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val estimatedTime =
                            dataSnapshot.child("Estimated Time").getValue(String::class.java)
                        binding.estimatedtime.text = "Estimated Time: ${estimatedTime ?: "Waiting for Update"}"

                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle error
                    }
                })
        }
    }
    private fun observeUsernameChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            database.child("OrderDetails").child(itemPushKey).child("Status").child("username")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val username = dataSnapshot.getValue(String::class.java)
                        val displayText = "The order was taken by ${username ?: "..."}"
                        binding.username.text = displayText

                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle error
                    }
                })
        }
    }

 
}
