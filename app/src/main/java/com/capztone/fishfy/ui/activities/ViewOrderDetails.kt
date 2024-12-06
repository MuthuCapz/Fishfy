package com.capztone.fishfy.ui.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.ActivityViewOrderDetailsBinding
import com.capztone.fishfy.ui.activities.models.Order
import com.capztone.fishfy.ui.activities.Utils.ToastHelper
import com.capztone.fishfy.ui.activities.ViewModel.ViewODViewModel
import com.google.firebase.database.FirebaseDatabase

class ViewOrderDetails : AppCompatActivity() {

    private lateinit var binding: ActivityViewOrderDetailsBinding
    private lateinit var viewModel: ViewODViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var orderId: String
    val ONE_MINUTE_IN_MILLIS = 60000

    private lateinit var cancelHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewOrderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

            viewModel = ViewModelProvider(this).get(ViewODViewModel::class.java)


binding.detailGoToBackImageButton.setOnClickListener {
    finish()
}

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        orderId = intent.getStringExtra("order_id") ?: ""

        if (orderId.isNotEmpty()) {
            viewModel.fetchOrderDetails(orderId)
            viewModel.fetchOrderImages(orderId)
        } else {
            ToastHelper.showCustomToast(this, "Order id not found")
            finish() // Finish activity if order id is not provided
        }

        cancelHandler = Handler()
        sharedPreferences = getSharedPreferences("OrderCancellation", Context.MODE_PRIVATE) // Initialize SharedPreferences


        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.orderDetails.observe(this) { order ->
            if (order != null) {
                displayOrderDetails(order)
            }
        }

        // Check order cancellation status from Firebase
        val database = FirebaseDatabase.getInstance()
        val orderRef = database.getReference("OrderDetails").child(orderId)

        orderRef.child("cancellationMessage").get().addOnSuccessListener { snapshot ->
            val cancellationMessage = snapshot.getValue(String::class.java)
            if (cancellationMessage != null && cancellationMessage == "Order Cancelled") {
                // If order is cancelled, disable the order view
                binding.radio.isEnabled = false
                disableOrderView()
            }
        }

        // Fetch order status and perform appropriate actions
        viewModel.fetchOrderStatus(orderId) { status ->
            when (status) {
                "Order confirmed" -> {
                    binding.radio.setOnClickListener {
                        showOrderTakenDialog()
                    }
                }
                "Order picked" -> {
                    binding.radio.setOnClickListener {
                        showOrderTakenDialog()
                    }
                }
                "Order delivered" -> {
                    binding.radio.setOnClickListener {
                        showOrderTakenDialog()
                    }
                    disableOrderView1()
                }
                else -> {
                    binding.radio.isEnabled = true
                    binding.radio.setOnClickListener {
                        showCancelOrderDialog()
                    }
                }
            }
        }


    viewModel.orderCancellationStatus.observe(this) { cancelled ->
            if (cancelled) {
                ToastHelper.showCustomToast(this, "Order cancelled")
                // Disable the view
                disableOrderView()
                // Store cancellation status in SharedPreferences
                sharedPreferences.edit().putBoolean("order_cancelled_$orderId", true).apply()
            }
        }

        // Check cancellation status when activity is created
        if (sharedPreferences.getBoolean("order_cancelled_$orderId", false)) {
            disableOrderView()
        }
    }

    private fun showOrderTakenDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_order_taken, null)
        val alertDialogBuilder = AlertDialog.Builder(this, R.style.CustomDialogThem)
            .setView(dialogView)
            .setCancelable(false)

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()

        val btnOk = dialogView.findViewById<Button>(R.id.btn_ok)
        btnOk.setOnClickListener {
            binding.radio.isChecked = false
            alertDialog.dismiss()
        }
    }
    private fun loadImageIntoImageView(imageUrl: String, imageView: ImageView) {
        Glide.with(this@ViewOrderDetails)
            .load(imageUrl)
            .into(imageView)
    }

    private fun extractFoodName(foodName: String): String {
        val slashIndex = foodName.indexOf('/')
        return if (slashIndex != -1) {
            foodName.substring(slashIndex + 1).trimEnd(']')
        } else {
            foodName.trimEnd(']')
        }
    }

    private fun extractFoodNames(foodNames: List<String>): String {
        return foodNames.joinToString(", ") { extractFoodName(it) }
    }

    private fun displayOrderDetails(order: Order) {
        binding.apply {
            oid.text = "${order.itemPushKey}"
            cid.text = "${order.userUid}"
            foodName.text = "${extractFoodNames(order.foodNames)}"
            foodPrice.text = "${order.adjustedTotalAmount}"
            quantity.text = "${order.foodQuantities}"
            time.text = "${order.orderDate}"
            slot.text = "${order.selectedSlot}"
            address.text = "${order.address}"

            binding.orderstatus.setOnClickListener {
                val intent = Intent(this@ViewOrderDetails, OrderStatusActivity::class.java)
                intent.putExtra("itemPushKey", order.itemPushKey)
                intent.putExtra("orderDate", order.orderDate)
                startActivity(intent)
            }
            binding.radio.setOnClickListener {
                showCancelOrderDialog()
            }
        }
    }
    private fun showCancelOrderDialog() {
        viewModel.fetchOrderStatus(orderId) { status ->
            when (status) {
                "Order Confirmed" -> {
                    showOrderTakenDialog()
                }
                else -> {
                    if (sharedPreferences.getBoolean("order_cancelled_$orderId", false)) {
                        ToastHelper.showCustomToast(this, "Your order is already cancelled")
                    } else {
                        // Inflate the custom layout
                        val dialogView = layoutInflater.inflate(R.layout.dialog_cancel_order, null)
                        val alertDialogBuilder = AlertDialog.Builder(this, R.style.CustomDialogThem)
                            .setView(dialogView)
                            .setCancelable(false)

                        val alertDialog = alertDialogBuilder.create()
                        alertDialog.show()
                        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                        // Set up the buttons in the custom layout
                        val btnYes = dialogView.findViewById<Button>(R.id.btn_yes)
                        val btnNo = dialogView.findViewById<Button>(R.id.btn_no)

                        btnYes.setOnClickListener {
                            viewModel.cancelOrder(orderId)
                            alertDialog.dismiss()
                        }

                        btnNo.setOnClickListener {
                            alertDialog.dismiss()
                            binding.radio.isChecked = false
                        }
                    }
                }
            }
        }
    }

    private fun disableOrderView1() {
        binding.viewOrder.isEnabled = false
        binding.viewOrder.setBackgroundColor(ContextCompat.getColor(this, R.color.Lgreen))
        binding.viewOrder.alpha = 0.5f
        // Display cancellation message on the disabled view
        binding.orderstatus.isEnabled = true // Enable the orderstatus button
        binding.radio.isEnabled = false
        binding.radio.isClickable = false
    }

    private fun disableOrderView() {
        binding.viewOrder.isEnabled = false
        binding.viewOrder.setBackgroundColor(ContextCompat.getColor(this, R.color.LiteAsh))
        binding.viewOrder.alpha = 0.5f
        // Display cancellation message on the disabled view
        val cancelledImageView = ImageView(this)
        binding.cancelimg.visibility = View.VISIBLE
        cancelledImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        binding.orderstatus.isEnabled = false
        binding.radio.isEnabled = false
        val marginParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        marginParams.setMargins(150, 150, 150, 150)
        cancelledImageView.layoutParams = marginParams
        binding.viewOrder.addView(cancelledImageView)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove any pending callbacks to avoid memory leaks
        cancelHandler.removeCallbacksAndMessages(null)
    }
}
