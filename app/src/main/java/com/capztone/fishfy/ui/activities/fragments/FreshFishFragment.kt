package com.capztone.fishfy.ui.activities.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.FragmentFreshFishBinding
import com.capztone.fishfy.ui.activities.MainActivity
import com.capztone.fishfy.ui.activities.ViewModel.FreshFishViewModel
import com.capztone.fishfy.ui.activities.models.MenuItem
import com.capztone.fishfy.ui.activities.adapters.FreshFishAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
class FreshFishFragment : Fragment() {
    private lateinit var binding: FragmentFreshFishBinding
    private lateinit var adapter: FreshFishAdapter
    private val viewModel: FreshFishViewModel by viewModels()

    // Create a variable to hold the category name
    private var categoryName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFreshFishBinding.inflate(inflater, container, false)

        // Setup status bar

        // Retrieve category name from arguments and set it to the TextView
        categoryName = arguments?.getString("categoryName")
        binding.heading.text = categoryName

        // Show loading indicator
        binding.progress.visibility = View.VISIBLE
        setupLoadingIndicator()

        // Start a delay to hide the loading indicator
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progress.visibility = View.GONE
        }, 1000)

        setupObservers()
        binding.backBtn.setOnClickListener {
            // Navigate back to MainActivity
            val intent = Intent(activity, MainActivity::class.java)
            startActivity(intent)
        }

        // Retrieve menu items with the category name
        viewModel.retrieveMenuItems(categoryName)

        return binding.root
    }

    // ... (rest of your methods remain unchanged)





    private fun setupLoadingIndicator() {
        binding.progress.setProgressVector(resources.getDrawable(R.drawable.spinload))
        binding.progress.setTextViewVisibility(true)
        binding.progress.setTextStyle(true)
        binding.progress.setTextColor(Color.YELLOW)
        binding.progress.setTextSize(12F)
        binding.progress.setTextMsg("Please Wait")
        binding.progress.setEnlarge(5)
    }


    private fun setupObservers() {
        viewModel.menuItemsLiveData.observe(viewLifecycleOwner) { menuItems ->
            if (menuItems.isNullOrEmpty()) {
                // Show the "no product" view when the list is empty
                binding.noproduct.visibility = View.VISIBLE
            } else {
                // Hide the "no product" view and show the menu items
                binding.noproduct.visibility = View.GONE
                showAllMenu(menuItems)
            }
        }
    }


    private fun showAllMenu(menuItems: List<MenuItem>) {
        adapter = FreshFishAdapter(menuItems.toMutableList(), requireContext())
        binding.freshfishrecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.freshfishrecycler.adapter = adapter
    }
}
