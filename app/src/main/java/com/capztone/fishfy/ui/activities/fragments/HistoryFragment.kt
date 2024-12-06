package com.capztone.fishfy.ui.activities.fragments

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.FragmentHistoryBinding
import com.capztone.fishfy.ui.activities.Utils.ToastHelper
import com.capztone.fishfy.ui.activities.adapters.RecentBuyAdapter
import com.capztone.fishfy.ui.activities.ViewModel.HistoryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

 private lateinit var binding: FragmentHistoryBinding
 private lateinit var viewModel: HistoryViewModel
 private lateinit var adapter: RecentBuyAdapter

 override fun onCreateView(
  inflater: LayoutInflater, container: ViewGroup?,
  savedInstanceState: Bundle?
 ): View {
  binding = FragmentHistoryBinding.inflate(inflater, container, false)
  return binding.root
 }

 override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
  super.onViewCreated(view, savedInstanceState)

  viewModel = ViewModelProvider(this).get(HistoryViewModel::class.java)
  adapter = RecentBuyAdapter(requireContext(), viewModel)



  binding.recentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
  binding.recentRecyclerView.adapter = adapter

  binding.shopnow.setOnClickListener {
   findNavController().navigate(R.id.action_cartFragment_to_homefragment)
  }

  binding.recentBackButton.setOnClickListener {
   requireActivity().onBackPressed()
  }
  binding.btnRetry.setOnClickListener {
   if (isNetworkAvailable(requireContext())) {

    findNavController().popBackStack() // Example action, modify as needed
   } else {
    // Show toast if network is still not available
    Toast.makeText(requireContext(), "Please check your network", Toast.LENGTH_SHORT).show()
   }
  }

  observeOrders()
 }

 private fun observeOrders() {
  viewModel.orders.observe(viewLifecycleOwner) { orders ->
   orders?.let {
    adapter.submitList(orders)
    updateEmptyOrdersMessageVisibility(orders.isEmpty())
   }
  }
 }

 private fun updateEmptyOrdersMessageVisibility(isEmpty: Boolean) {
  if (isEmpty) {
   binding.emptyCartMessage.visibility = View.VISIBLE
   binding.recentRecyclerView.visibility = View.GONE
  } else {
   binding.emptyCartMessage.visibility = View.GONE
   binding.recentRecyclerView.visibility = View.VISIBLE
  }
 }

 private fun isNetworkAvailable(context: Context): Boolean {
  val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
   val network = connectivityManager.activeNetwork ?: return false
   val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
   return when {
    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
    else -> false
   }
  } else {
   val networkInfo = connectivityManager.activeNetworkInfo
   return networkInfo != null && networkInfo.isConnected
  }
 }

 private fun fetchOrders() {
  if (isNetworkAvailable(requireContext())) {
   CoroutineScope(Dispatchers.Main).launch {
    viewModel.fetchOrders()
   }
   binding.network.visibility = View.GONE
   binding.recentRecyclerView.visibility = View.VISIBLE
  } else {
   binding.network.visibility = View.VISIBLE
   binding.emptyCartMessage.visibility = View.GONE
   binding.recentRecyclerView.visibility = View.GONE
  }
 }

 fun onCancelOrder(orderId: String) {
  CoroutineScope(Dispatchers.Main).launch {
   val isSuccess = viewModel.cancelOrder(orderId)
   if (isSuccess) {
    context?.let { ToastHelper.showCustomToast(it, "Your order has been cancelled") }
   } else {
    Toast.makeText(requireContext(), "Failed to cancel order", Toast.LENGTH_SHORT).show()
   }
  }
 }

 override fun onResume() {
  super.onResume()
  fetchOrders()
 }
}