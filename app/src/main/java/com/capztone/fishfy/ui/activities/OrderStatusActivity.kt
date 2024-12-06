// OrderStatusActivity.kt

package com.capztone.fishfy.ui.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.capztone.fishfy.databinding.ActivityOrderStatusBinding
import com.capztone.fishfy.ui.activities.ViewModel.OrderStatusViewModel

import com.google.firebase.database.*

class OrderStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderStatusBinding
    private lateinit var viewModel: OrderStatusViewModel
    private lateinit var itemPushKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val itemPushKey = intent.getStringExtra("itemPushKey")
        val orderDate = intent.getStringExtra("orderDate")
        binding.orderId.text="Order Id: $itemPushKey"
        binding.orderDate.text="Order Date: $orderDate"

binding.backArrow.setOnClickListener {
    finish()
}
        viewModel = ViewModelProvider(this).get(OrderStatusViewModel::class.java)
        if (itemPushKey != null) {
            viewModel.init(itemPushKey, binding)
        }


    }
}
