package com.capztone.fishfy.ui.activities.models

import android.os.Parcel
import android.os.Parcelable

class OrderDetails() : Parcelable {
    var userUid: String? = null
    var foodNames: MutableList<String>? = null
    var foodPrices: MutableList<Double>? = null
    var foodImage: MutableList<String>? = null
    var foodQuantities: MutableList<Int>? = null
    var address: String? = null
    var skuUnitQuantities: MutableList<String>? = null // Only unit quantities stored
    var PaymentReceived: Boolean = true
    var itemPushKey: String? = null
    var adjustedTotalAmount: String? = null
    var orderDate: String? = null
    var ShopNames: MutableList<String>? = null
    var selectedSlot: String? = null
    var fooddescription: MutableList<String>? = null
    var skuList: MutableList<String>? = null
    var delLat: Double = 0.0 // Latitude
    var delLng: Double = 0.0 // Longitude

    constructor(parcel: Parcel) : this() {
        userUid = parcel.readString()
        address = parcel.readString()
        PaymentReceived = parcel.readByte() != 0.toByte()
        itemPushKey = parcel.readString()
        adjustedTotalAmount = parcel.readString()
        orderDate = parcel.readString()
        ShopNames = parcel.createStringArrayList()
        selectedSlot = parcel.readString()
        fooddescription = parcel.createStringArrayList()?.toMutableList()
        skuList = parcel.createStringArrayList()?.toMutableList()
        skuUnitQuantities = parcel.createStringArrayList()
        delLat = parcel.readDouble() // Read latitude
        delLng = parcel.readDouble() // Read longitude
    }

    constructor(
        userId: String?,
        foodItemName: MutableList<String>?,
        foodItemPrice: ArrayList<Double>,
        foodItemImage: MutableList<String>?,
        foodItemQuantities: MutableList<Int>?,
        address: String?,
        adjustedTotalAmount: Int,
        itemPushKey: String?,
        orderDate: String?,
        PaymentReceived: Boolean,
        ShopNames: MutableList<String>?,
        selectedSlot: String?,
        foodItemDescription: MutableList<String>?,
        skuList: List<String>,
        skuUnitQuantities: List<String>,
        lati: Double,
        lngi: Double
    ) : this() {
        this.userUid = userId
        this.foodNames = foodItemName
        this.foodPrices = foodItemPrice
        this.foodImage = foodItemImage
        this.foodQuantities = foodItemQuantities
        this.address = address
        this.adjustedTotalAmount = adjustedTotalAmount.toString()
        this.itemPushKey = itemPushKey
        this.orderDate = orderDate
        this.PaymentReceived = PaymentReceived
        this.ShopNames = ShopNames
        this.selectedSlot = selectedSlot
        this.fooddescription = foodItemDescription
        this.skuList = skuList.toMutableList()
        this.skuUnitQuantities = skuUnitQuantities.toMutableList()
        this.delLat = lati
        this.delLng = lngi
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userUid)
        parcel.writeString(address)
        parcel.writeByte(if (PaymentReceived) 1 else 0)
        parcel.writeString(itemPushKey)
        parcel.writeString(adjustedTotalAmount)
        parcel.writeString(orderDate)
        parcel.writeStringList(ShopNames)
        parcel.writeString(selectedSlot)
        parcel.writeStringList(fooddescription)
        parcel.writeStringList(skuList)
        parcel.writeList(skuUnitQuantities)
        parcel.writeDouble(delLat) // Write latitude
        parcel.writeDouble(delLng) // Write longitude
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OrderDetails> {
        override fun createFromParcel(parcel: Parcel): OrderDetails {
            return OrderDetails(parcel)
        }

        override fun newArray(size: Int): Array<OrderDetails?> {
            return arrayOfNulls(size)
        }
    }
}