package com.capztone.fishfy.ui.activities.models

data class Order(
    var userUid: String = "",
    var itemPushKey: String = "",
    var foodNames: List<String> = emptyList(),
    var foodPrices: List<String> = emptyList(),
    var foodImage: List<String> = emptyList(),
    var adjustedTotalAmount: String = "",
    var foodQuantities: List<Int> = emptyList(),
    var orderDate: String = "",
    val currentTime: String = "",
    var selectedSlot: String = "",
    var address: String = "",
)




