package com.capztone.fishfy.ui.activities.fragments

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.fishfy.databinding.FragmentFavouriteBinding
import com.capztone.fishfy.ui.activities.adapters.FavouriteAdapter
import com.capztone.fishfy.ui.activities.models.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FavouriteFragment : Fragment() {

    private var _binding: FragmentFavouriteBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: FavouriteAdapter
    private lateinit var database: DatabaseReference
    private val menuItems = mutableListOf<MenuItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavouriteBinding.inflate(inflater, container, false)

        binding.back.setOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.btnRetry.setOnClickListener {
            if (isNetworkAvailable()) {

                findNavController().popBackStack() // Example action, modify as needed
            } else {
                // Show toast if network is still not available
                Toast.makeText(requireContext(), "Please check your network", Toast.LENGTH_SHORT).show()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        fetchFavorites()
    }

    private fun setupRecyclerView() {
        adapter = FavouriteAdapter(requireContext(), menuItems) { menuItem ->
            // Handle item click if needed
        }
        binding.favRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.favRecyclerView.adapter = adapter
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    private fun fetchFavorites() {
        if (!isNetworkAvailable()) {
            // Network is off, show the RelativeLayout
            binding.network.visibility = View.VISIBLE
            binding.scrollViewCart.visibility = View.GONE
        } else {
            // Network is available, proceed with fetching data
            binding.network.visibility = View.GONE
            binding.scrollViewCart.visibility = View.VISIBLE

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                database = FirebaseDatabase.getInstance().getReference("Favourite").child(userId)
                database.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        menuItems.clear()
                        for (dataSnapshot in snapshot.children) {
                            val menuItem = dataSnapshot.getValue(MenuItem::class.java)
                            if (menuItem != null && menuItem.favorite) {
                                // Fetch the stock information from the database
                                val stockStatus =
                                    dataSnapshot.child("stock").getValue(String::class.java)

                                // If stock data is available, assign it to the MenuItem
                                menuItem.stock = stockStatus

                                // Ensure firebaseKey is set
                                menuItem.firebaseKey = dataSnapshot.key
                                menuItems.add(menuItem)
                            }
                        }
                        adapter.notifyDataSetChanged()
                        updateEmptyCartMessageVisibility()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle the error appropriately
                        Log.e(
                            "FetchFavoriteItems",
                            "Error fetching favorite items: ${error.message}"
                        )
                    }
                })
            }

        }
    }

    private fun updateEmptyCartMessageVisibility() {
        _binding?.let {
            if (menuItems.isEmpty()) {
                it.emptyCartMessage.visibility = View.VISIBLE
                it.scrollViewCart.visibility = View.GONE
            } else {
                it.emptyCartMessage.visibility = View.GONE
                it.scrollViewCart.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}