package com.capztone.fishfy.ui.activities.fragments

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.FragmentShopOneBinding
import com.capztone.fishfy.ui.activities.ViewModel.ShopOneViewModel
import com.capztone.fishfy.ui.activities.adapters.CategoryAdapter
import com.capztone.fishfy.ui.activities.adapters.MenuAdapter
import com.capztone.fishfy.ui.activities.models.Category
import com.capztone.fishfy.ui.activities.models.MenuItem
import com.google.firebase.database.*

class ShopOneFragment : Fragment() {

    private lateinit var binding: FragmentShopOneBinding
    private val viewModel: ShopOneViewModel by viewModels()
    private lateinit var database: FirebaseDatabase
    private lateinit var categories: MutableList<Category>
    private lateinit var categoryAdapter: CategoryAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShopOneBinding.inflate(inflater, container, false)
        binding.recentBackButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Show loading indicator
        categories = mutableListOf()
        categoryAdapter = CategoryAdapter(requireContext(), categories) { category ->
         storeCategoryAndOpenFragment(category)
            // Pass the category name to the ViewModel's selectCategory method
        }


        binding.categoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter  // Set the adapter here
        }

        binding.progress.visibility = View.VISIBLE
        database = FirebaseDatabase.getInstance()
        binding.progress.setProgressVector(resources.getDrawable(R.drawable.spinload))
        binding.progress.setTextViewVisibility(true)
        binding.progress.setTextStyle(true)
        binding.progress.setTextColor(Color.YELLOW)
        binding.progress.setTextSize(12F)
        binding.progress.setTextMsg("Please Wait")
        binding.progress.setEnlarge(5)
        // Start a delay to hide the loading indicator after 5000 milliseconds (5 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progress.visibility = View.GONE

            // Call your method to retrieve cart items or perform other operations

        }, 1500)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val shopId = arguments?.getString("shopId")

        // Display the shopId in the TextView
        shopId?.let {
            binding.shopname.text = it
            val textValue = binding.textView13.text.toString()

            viewModel.retrieveData(shopId, textValue)

            // Fetch shopName from Firebase based on shopId
            fetchShopName(it)
            fetchCategoriesForAllShops(it)

        }
        viewModel.menuItems.observe(viewLifecycleOwner) { menuItems ->
            // Update RecyclerView adapter with menuItems
            if (menuItems.isNotEmpty()) {
                binding.textView13.visibility = View.VISIBLE


            }
            if (menuItems.isEmpty()) {
                binding.popularRecyclerView.visibility = View.GONE


            }
        }

        setupMenuRecyclerView(binding.popularRecyclerView)



        observeViewModel()
    }

    private fun fetchCategoriesForAllShops(shopId: String) {
        // Reference to the categories node for the specific shop
        val categoriesRef = database.getReference("Categories").child(shopId)

        categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if there are children in the snapshot
                if (!snapshot.exists()) {
                    Log.d("fetchCategories", "No categories found for Shop ID: $shopId")
                    return
                }

                // Get the first child category
                val firstChildSnapshot = snapshot.children.firstOrNull()
                if (firstChildSnapshot != null) {
                    // Fetch the first category name (the key)
                    val firstCategoryName = firstChildSnapshot.key ?: ""

                    // Display the first category name in TextView13
                    binding.textView13.text = firstCategoryName

                    // Optional: Log the first category name for debugging
                    Log.d("fetchCategories", "First category name: $firstCategoryName")
                } else {
                    Log.d("fetchCategories", "No categories available")
                }

                // Iterate through all categories (if needed)
                for (categorySnapshot in snapshot.children) {
                    val categoryName = categorySnapshot.key ?: ""
                    val imageUrl = categorySnapshot.child("image").getValue(String::class.java) ?: ""

                    // Skip the category if the name is "discount"
                    if (categoryName.equals("discount", ignoreCase = true)) continue

                    // Add category if both name and image are valid
                    if (categoryName.isNotEmpty() && imageUrl.isNotEmpty()) {
                        val category = Category(categoryName, imageUrl)
                        categories.add(category)  // Add category to the list
                    }
                }

                // Notify the adapter that the data has changed
                categoryAdapter.notifyDataSetChanged()

                // Update the UI after fetching categories
                binding.categoryRecyclerView.visibility = View.VISIBLE  // Make sure the RecyclerView is visible
                binding.progress.visibility = View.GONE  // Hide the progress indicator
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("fetchCategories", "Failed to fetch categories for Shop ID $shopId: ${error.message}")
            }
        })
    }


    private fun storeCategoryAndOpenFragment(category: Category) {
        binding.textView13.text = category.name
        val shopId = arguments?.getString("shopId")

        // Display the shopId in the TextView
        shopId?.let {
            binding.shopname.text = it
            val textValue = binding.textView13.text.toString()

            viewModel.retrieveData(shopId, textValue)
        }

    }


    private fun fetchShopName(shopId: String) {
        // Reference to the "ShopNames" path in Firebase
        val databaseReference = FirebaseDatabase.getInstance().getReference("ShopNames").child(shopId)

        // Fetch the shopName value
        databaseReference.child("shopName").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Check if the shopName exists in the snapshot
                val shopName = dataSnapshot.getValue(String::class.java)
                shopName?.let {
                    // Set the shopName in the TextView
                    binding.shoplabel.text = it
                } ?: run {
                    // Handle case where shopName is not found
                    binding.shoplabel.text = ""
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors
                binding.shoplabel.text = ""
            }
        })
    }

    private fun setupMenuRecyclerView(recyclerView: RecyclerView) {
        recyclerView.layoutManager =  LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }



    private fun observeViewModel() {
        viewModel.menuItems.observe(viewLifecycleOwner) { items ->
            setMenuAdapter(items, binding.popularRecyclerView)

            // Check if items are empty and hide/show dealoftheday layout
        }

    }

    private fun setMenuAdapter(menuItems: List<MenuItem>, recyclerView: RecyclerView) {
        val adapter = MenuAdapter(menuItems.toMutableList(), requireContext())
        recyclerView.adapter = adapter

        // Set GridLayoutManager with vertical orientation and 3 items per row
        val gridLayoutManager = GridLayoutManager(requireContext(), 3)  // 3 items per row
        gridLayoutManager.orientation = GridLayoutManager.VERTICAL  // Set vertical orientation
        recyclerView.layoutManager = gridLayoutManager
    }




}