package com.capztone.fishfy.ui.activities.Utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

fun updateUserStatus(status: String) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    userId?.let {
        val statusRef = FirebaseDatabase.getInstance().getReference("users").child(it).child("Status")
        statusRef.setValue(status)
    }
}
