package com.capztone.fishfy.ui.activities.models

data class MenuItem(
    var foodId: String? = null,
    var foodName: ArrayList<String>? =null,
    val foodPrice: String? = null,
    val foodDescription: String? = null,
    val foodImage: String? = null,
    var favorite: Boolean = false,
    var productQuantity:String?=null,
    var firebaseKey: String? = null,
    var path: String? = null,
    var key: String? = null,
    var stock: String? = null // Add this property


)

