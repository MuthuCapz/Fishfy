package com.capztone.fishfy.ui.activities.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.capztone.fishfy.ui.activities.models.Order
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext

class HistoryViewModel : ViewModel() {

    private val _orders = MutableLiveData<List<Order>>()
    val orders: LiveData<List<Order>> get() = _orders

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference.child("OrderDetails")
    private val auth: FirebaseAuth = Firebase.auth
    private val databases: DatabaseReference = FirebaseDatabase.getInstance().reference


    fun fetchOrders() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            // Fetch the userUid from the 'users' path
            databases.child("users").child(currentUserId).child("userid")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userUid = snapshot.getValue(String::class.java)
                        if (userUid != null) {
                            // Query orders based on the retrieved userUid
                            database.orderByChild("userUid").equalTo(userUid)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val orderList = mutableListOf<Order>()
                                        for (orderSnapshot in snapshot.children) {
                                            val order = orderSnapshot.getValue(Order::class.java)
                                            order?.let { orderList.add(it) }
                                        }
                                        // Reverse the order of the list
                                        orderList.reverse()
                                        _orders.value = orderList
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        // Handle error
                                    }
                                })
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                    }
                })
        }
    }


    suspend fun cancelOrder(orderId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val orderRef = database.child(orderId)
                orderRef.updateChildren(
                    mapOf(
                        "cancellationMessage" to "Order Cancelled"
                    )
                )
                true
            } catch (e: Exception) {
                // Handle exception
                false
            }
        }
    }
}
