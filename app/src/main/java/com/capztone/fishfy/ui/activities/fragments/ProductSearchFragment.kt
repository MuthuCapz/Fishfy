package com.capztone.fishfy.ui.activities.fragments

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.fishfy.databinding.FragmentProductSearchBinding
import com.capztone.fishfy.ui.activities.MainActivity
import com.capztone.fishfy.ui.activities.Utils.NetworkReceiver
import com.capztone.fishfy.ui.activities.ViewModel.SearchViewModel
import com.capztone.fishfy.ui.activities.adapters.SearchAdapter
import com.capztone.fishfy.ui.activities.models.MenuItem

class ProductSearchFragment : Fragment() {
    private lateinit var binding: FragmentProductSearchBinding
    private lateinit var adapter: SearchAdapter
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var networkReceiver: NetworkReceiver


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProductSearchBinding.inflate(inflater, container, false)

        setupObservers()
        viewModel.retrieveMenuItems()
        setupNetworkReceiver()

        binding.btnRetry.setOnClickListener {
            if (isNetworkConnected()) {
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show()
            }
        }



        binding.searchBackButton.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
        }

        setupSearchView()
        return binding.root
    }

    private fun setupObservers() {
        viewModel.menuItemsLiveData.observe(viewLifecycleOwner) { menuItems ->
            menuItems?.let {
                setupRecyclerView(it)
                // Apply the filter after adapter is initialized
                applySearchQuery()
            }
        }
    }
    private fun isNetworkConnected(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
    private fun setupRecyclerView(menuItems: List<MenuItem>) {
        adapter = SearchAdapter(menuItems, requireContext(), binding.noResultsTextView)
        binding.menuRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.menuRecyclerView.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    viewModel.setSearchQuery(it) // Save the search query
                    filterMenuItems(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    viewModel.setSearchQuery(it) // Save the search query
                    filterMenuItems(it)
                }
                return true
            }
        })
    }

    private fun filterMenuItems(query: String) {
        if (::adapter.isInitialized) {
            adapter.filter(query)
        }
    }

    private fun applySearchQuery() {
        val query = viewModel.getSearchQuery()
        binding.searchView.setQuery(query, false) // Restore the previous search query
        adapter.filter(query) // Restore the filtered items
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            applySearchQuery()
        }
        requireContext().registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

    }

    override fun onPause() {
        super.onPause()
        // Unregister the network receiver
        requireContext().unregisterReceiver(networkReceiver)
    }
    private fun setupNetworkReceiver() {
        networkReceiver = NetworkReceiver { isConnected ->
            Log.d("NetworkReceiver", "Network status changed: $isConnected")
            requireActivity().runOnUiThread {
                binding.network.visibility = if (isConnected) View.GONE else View.VISIBLE
                binding.menuRecyclerView.visibility = if (isConnected) View.VISIBLE else View. GONE

            }
        }
    }


}