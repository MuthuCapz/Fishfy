package com.capztone.fishfy.ui.activities.models

data class DiscountItem(
    var foodNames: ArrayList<String>? =null,
    val foodPrices: String? = null,
    val foodDescriptions: String? = null,
    val foodImages: String? = null,
    val quantity: Int? = null,
    val discounts: String? = null,
    val productQuantity:String?=null,
    var path: String? = null,
    var CartItemAddTime: String?=null,
    var key:String?=null,
    var stocks: String? = null // Add this property

)

