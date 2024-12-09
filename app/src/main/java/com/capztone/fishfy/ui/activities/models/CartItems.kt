package com.capztone.fishfy.ui.activities.models

import com.google.firebase.database.PropertyName

data class CartItems(
    var path: String? = null,
    var foodName : String? = null,
    var foodPrice : String? = null,
    var foodDescription : String? = null,
    var foodImage : String? = null,
    var foodQuantity : Int? = null,
    var CartItemAddTime : String? = null,
    var key:String?=null,
    @get:PropertyName("UnitQuantity") @set:PropertyName("UnitQuantity")
    var UnitQuantity: String? = null // Use exact casing

)