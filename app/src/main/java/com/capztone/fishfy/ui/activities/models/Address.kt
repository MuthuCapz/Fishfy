package com.capztone.fishfy.ui.activities.models

data class Address(
    val addressType: String,
    var address: String,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var locality: String = "",
    var type: String = ""
)
