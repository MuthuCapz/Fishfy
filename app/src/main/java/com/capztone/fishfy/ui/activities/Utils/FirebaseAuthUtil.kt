package com.capztone.admin.utils

import com.google.firebase.auth.FirebaseAuth

object FirebaseAuthUtil {
    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
}
