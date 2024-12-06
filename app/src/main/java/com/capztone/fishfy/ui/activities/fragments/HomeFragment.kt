package com.capztone.fishfy.ui.activities.fragments


import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.location.Address
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import com.capztone.fishfy.R
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import com.google.firebase.storage.FirebaseStorage
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.RequiresApi

import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.interfaces.ItemClickListener
import com.denzcoskun.imageslider.models.SlideModel
import com.capztone.fishfy.databinding.FragmentHomeBinding
import com.capztone.fishfy.ui.activities.LocationActivity
import com.capztone.fishfy.ui.activities.models.DiscountItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*
import com.google.android.gms.location.FusedLocationProviderClient
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.fishfy.ui.activities.adapters.ExploreShopAdapter
import com.capztone.fishfy.ui.activities.adapters.HomeDiscountAdapter
import com.capztone.fishfy.ui.activities.adapters.NearItemAdapter
import com.capztone.fishfy.ui.activities.adapters.PreviousOrderAdapter
import com.capztone.fishfy.ui.activities.models.MenuItem
import com.capztone.fishfy.ui.activities.models.PreviousItem
import com.capztone.fishfy.ui.activities.ViewModel.HomeViewModel
import com.capztone.fishfy.ui.activities.adapters.CategoryAdapter
import com.capztone.fishfy.ui.activities.adapters.DealItemAdapter
import com.capztone.fishfy.ui.activities.models.CartItems
import com.capztone.fishfy.ui.activities.models.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.os.Handler
import android.widget.Toast
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.NavController
import com.capztone.admin.utils.FirebaseAuthUtil
import com.capztone.fishfy.ui.activities.LocationNotAvailable
import com.capztone.fishfy.ui.activities.Utils.NetworkReceiver
import com.capztone.fishfy.ui.activities.adapters.OrderItem
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.io.IOException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


class HomeFragment : Fragment(), ExploreShopAdapter.OnItemClickListener {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databases: FirebaseDatabase
    private lateinit var viewModel: HomeViewModel
    private var quantity: Int = 0

    private lateinit var databaseReference: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var currentUserId: String
    private lateinit var networkReceiver: NetworkReceiver


    private val cartItems = mutableListOf<CartItems>()
    private val  discountItem = mutableListOf<DiscountItem>()
    private lateinit var userId: String
    private val previousItem = mutableListOf<PreviousItem>()
    private var menuItems = mutableListOf<MenuItem>()
    private lateinit var adapter: NearItemAdapter
    private lateinit var dealadapter: DealItemAdapter
    private lateinit var exploreShopAdapter: ExploreShopAdapter
    private lateinit var adapter1: PreviousOrderAdapter
    private lateinit var categories: MutableList<Category>
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var homeDiscountAdapter: HomeDiscountAdapter // Initialize HomeDiscountAdapter
    private lateinit var navController: NavController
    private val adminDestinations = mutableListOf<Pair<Double, Double>>()
    private val shopNames = mutableListOf<String>()




    private val buyHistory: MutableList<PreviousItem> = mutableListOf()
    private lateinit var homeViewModel: HomeViewModel
    private val sharedPreferences by lazy { requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        // Assuming buyHistory is of type MutableList<PreviousItem>
        // Assuming buyHistory is of type MutableList<PreviousItem>
        val orderItems = buyHistory.map { previousItem ->
            OrderItem.Previous(previousItem) as OrderItem // Cast each PreviousItem to OrderItem
        }.toMutableList() // Convert the list to MutableList<OrderItem>

// Now pass the correct list to the adapter
        adapter1 = PreviousOrderAdapter(orderItems, requireContext())

        // Initialize adapter1 here
        navController = findNavController()
        observeViewModel()

        database = FirebaseDatabase.getInstance()
        databaseReference = database.getReference("Addresses")
        databaseRef = FirebaseDatabase.getInstance().reference
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        // Add this part to check network connectivity
        // Initialize and register the network receiver
        networkReceiver = NetworkReceiver { isConnected ->
            handleNetworkStatus(isConnected) // This should be a Boolean
        }
        categories = mutableListOf()
        categoryAdapter = CategoryAdapter(requireContext(), categories) { category ->
            storeCategoryAndOpenFragment(category)
        }
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireContext().registerReceiver(networkReceiver, filter)

        // Check network status initially
        handleNetworkStatus(isNetworkAvailable())
        binding.btnRetry.setOnClickListener {
            if (isNetworkAvailable()) {

                findNavController().popBackStack() // Example action, modify as needed
            } else {
                // Show toast if network is still not available
                Toast.makeText(requireContext(), "Please check your network", Toast.LENGTH_SHORT).show()
            }
        }
        // Show loading indicator
        binding.progress.visibility = View.VISIBLE

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




        databases = FirebaseDatabase.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

auth = FirebaseAuthUtil.auth

        viewModel.menuItems.observe(viewLifecycleOwner) { items ->
            adapter.updateData(items)
            updateTitlesVisibility(menuItems, cartItems, categories)
        }



        homeDiscountAdapter = HomeDiscountAdapter(requireContext()) // Initialize HomeDiscountAdapter



        disableSearchViewInput(binding.searchView1)


        exploreShopAdapter = ExploreShopAdapter(listOf(), this)

        binding.Explorereclyview.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = exploreShopAdapter
        }

        categories = mutableListOf()
        categoryAdapter = CategoryAdapter(requireContext(), categories) { category ->
            storeCategoryAndOpenFragment(category)
        }

        binding.categoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
// Hide or show titles based on product list
        updateTitlesVisibility(menuItems,cartItems,categories)

        fetchCategories()
        fetchShopLocationsFromFirebase()
        fetchShopNameAndUpdateAdapter()

        fetchUserShopName()
        fetchUserShopNames()

        fetchShopName()
        // Rest of your code...
        fetchBuyHistory()
        // Retrieve and display address and locality
        retrieveAddressAndLocality()
        setupRecyclerView()
        setupRecyclerView1()


        // Set click listener on the dropdown ImageView
        binding.dropdown.setOnClickListener {
            // Navigate to LocationActivity
            val intent = Intent(requireContext(), LocationActivity::class.java)
            startActivity(intent)
        }
        binding.tvLocality.setOnClickListener {
            val intent = Intent(requireContext(), LocationActivity::class.java)
            startActivity(intent)
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // Close the app smoothly
            requireActivity().finishAffinity() // Closes all activities in the task
        }
        // Assuming discountItems is your list of DiscountItem
        val adapter = HomeDiscountAdapter(requireContext())
        binding.discountrecycler.adapter = adapter
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.discountrecycler.layoutManager = layoutManager


        this@HomeFragment.database = FirebaseDatabase.getInstance()

        return binding.root
    }
    private fun handleNetworkStatus(isConnected: Boolean) {
        if (isConnected) {
            binding.network.visibility = View.GONE
            binding.scrollView.visibility = View.VISIBLE

        } else {
            binding.network.visibility = View.VISIBLE
            binding.scrollView.visibility = View.GONE
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnected == true
    }
    private fun updateTitlesVisibility(
        menuItems: MutableList<MenuItem>,
        cartItems: MutableList<CartItems>,
        categories: MutableList<Category>
    ) {
        if (menuItems.isEmpty()  &&  cartItems.isEmpty() &&  categories.isEmpty()) {
            binding.textVieww1.visibility = View.INVISIBLE
            binding.textVieww2.visibility = View.INVISIBLE
            binding.textVieww4.visibility = View.INVISIBLE
        } else {
            binding.textVieww3.visibility = View.VISIBLE
            binding.textVieww.visibility = View.VISIBLE
            binding.textVieww1.visibility = View.VISIBLE
            binding.textVieww2.visibility = View.VISIBLE
            binding.textVieww4.visibility = View.VISIBLE
            binding.viewall.visibility = View.VISIBLE
        }
    }
    private fun fetchShopLocationsFromFirebase() {
        val shopLocationsRef = databaseRef.child("ShopLocations")
        shopLocationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                adminDestinations.clear()
                shopNames.clear()

                for (shopSnapshot in dataSnapshot.children) {
                    val shopName = shopSnapshot.key ?: continue
                    val lat = shopSnapshot.child("latitude").getValue(Double::class.java) ?: continue
                    val lng = shopSnapshot.child("longitude").getValue(Double::class.java) ?: continue
                    adminDestinations.add(Pair(lat, lng))
                    shopNames.add(shopName)
                }

                // Calculate distances once shop locations are fetched
                if (isAdded) {
                    calculateDistances()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to load shop locations",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun calculateDistances() {
        // Get the user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Fetch user's latitude and longitude from the "Addresses" path in Firebase
        val userLocationRef = databaseRef.child("Addresses").child(userId)
        userLocationRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userLat = dataSnapshot.child("latitude").getValue(Double::class.java) ?: return
                val userLng = dataSnapshot.child("longitude").getValue(Double::class.java) ?: return

                // Retrieve the distance threshold from Firebase
                val databaseReference = FirebaseDatabase.getInstance().getReference("Delivery Details/User Distance")
                databaseReference.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val distanceThresholdString = dataSnapshot.getValue(String::class.java)
                        val distanceThreshold = distanceThresholdString?.toDoubleOrNull() ?: 10.0

                        // Calculate distances between user location and shop locations
                        val nearbyShops = mutableSetOf<String>() // Use a Set to avoid duplicates

                        for (i in adminDestinations.indices) {
                            val shopLat = adminDestinations[i].first
                            val shopLng = adminDestinations[i].second

                            val distance = calculateDistance(userLat, userLng, shopLat, shopLng)

                            // Add shop name if within the threshold
                            if (distance < distanceThreshold) {
                                nearbyShops.add(shopNames[i].trim()) // Trim shop name to avoid issues
                            }
                        }

                        if (nearbyShops.isNotEmpty()) {
                            val shopsWithinThreshold = nearbyShops.joinToString(", ")
                            binding.tvLogitude.text = shopsWithinThreshold
                            storeNearbyShopsInFirebase(shopsWithinThreshold)
                        } else {
                            // Delete the shop name if no shops are within the threshold
                            deleteShopNameFromFirebase(userId)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(
                            requireContext(),
                            "Error fetching distance threshold: ${databaseError.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Error fetching user location: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun deleteShopNameFromFirebase(userId: String) {
        val userLocationRef = databaseRef.child("Addresses").child(userId).child("Shop Id")
        userLocationRef.removeValue()
            .addOnSuccessListener {
            }
            .addOnFailureListener {
            }
    }

    private fun storeNearbyShopsInFirebase(newShopId: String) {
        val userId = auth.currentUser?.uid ?: return
        val userLocationRef = databaseRef.child("Addresses").child(userId).child("Shop Id")

        // Retrieve the existing Shop Ids
        userLocationRef.get().addOnSuccessListener { dataSnapshot ->
            val existingShopIds = dataSnapshot.value as? String

            // Create a set of existing Shop Ids (case-insensitive check, original casing preserved)
            val shopIdSet = existingShopIds?.split(",")?.map { it.trim() }?.toMutableSet() ?: mutableSetOf()
            val shopIdSetLowerCase = shopIdSet.map { it.lowercase() }.toMutableSet()

            // Check if the new Shop Id (case-insensitive) is already in the set
            val newShopIdTrimmed = newShopId.trim()
            if (!shopIdSetLowerCase.contains(newShopIdTrimmed.lowercase())) {
                // Add the new Shop Id in its original casing
                shopIdSet.add(newShopIdTrimmed)

                // Convert the set back to a comma-separated string
                val updatedShopIds = shopIdSet.joinToString(",")

                // Store the updated Shop Ids in Firebase
                userLocationRef.setValue(newShopIdTrimmed)
                    .addOnSuccessListener {
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to store nearby shops", Toast.LENGTH_SHORT).show()
                    }
            } else {
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to check existing Shop IDs", Toast.LENGTH_SHORT).show()
        }
    }


    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Radius of the Earth in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c // Distance in kilometers
    }


    private fun fetchShopNameAndUpdateAdapter() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            databaseReference.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val shopNames = snapshot.child("Shop Id").getValue(String::class.java)
                        shopNames?.let { names ->
                            if (names.isNotBlank()) {
                                val shopNameList = names.split(",") // Split shop names by comma
                                shopNameList.forEach { shopName ->
                                    fetchDiscountItems(shopName.trim())
                                    // Trim whitespace from shop name
                                }
                            } else {
                                // Navigate to LocationNotAvailableActivity
                                startActivity(Intent(context, LocationNotAvailable::class.java))
                                activity?.finish() // Optionally finish current activity
                            }
                        }
                    } else {
                        // Navigate to LocationNotAvailableActivity
                        startActivity(Intent(context, LocationNotAvailable ::class.java))
                        activity?.finish() // Optionally finish current activity
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                    Log.e("fetchShopName", "Error fetching shop names", error.toException())
                }
            })
        }
    }

    private fun disableSearchViewInput(searchView: androidx.appcompat.widget.SearchView) {
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                searchView.clearFocus()
                findNavController().navigate(R.id.action_homeFragment_to_productSearchFragment)
            }
        }

    }
    private fun fetchDiscountItems(shopName: String) {
        val userId = auth.currentUser?.uid ?: return
        val discountItems = mutableListOf<DiscountItem>()

        // Show loading layout while fetching data

        database.getReference("user").child(userId).child("language")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(languageSnapshot: DataSnapshot) {
                    val userLanguage = languageSnapshot.getValue(String::class.java)?.toLowerCase() ?: "english"
                    val discountReferences = listOf("discount")
                    discountReferences.forEach { discount ->
                        database.getReference("Shops").child(shopName).child(discount)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        for (itemSnapshot in snapshot.children) {
                                            val discountItem = itemSnapshot.getValue(DiscountItem::class.java)
                                            discountItem?.let {
                                                it.path = shopName
                                                val foodNamesList = it.foodNames ?: arrayListOf()
                                                val englishName = foodNamesList.getOrNull(0) ?: ""
                                                val languageSpecificName = when (userLanguage) {
                                                    "tamil" -> foodNamesList.getOrNull(1) ?: ""
                                                    "malayalam" -> foodNamesList.getOrNull(2) ?: ""
                                                    "telugu" -> foodNamesList.getOrNull(3) ?: ""
                                                    else -> englishName
                                                }
                                                val combinedName = "$englishName / $languageSpecificName"
                                                it.foodNames = arrayListOf(combinedName)
                                                discountItems.add(it)
                                            }
                                        }

                                        // If items are found, hide the loading layout and update the adapter
                                        if (discountItems.isNotEmpty()) {
                                            binding.flash.visibility= View.VISIBLE
                                            homeDiscountAdapter.updateData(discountItems)
                                        } else {
                                            // If no items found, you can show an error message or a "no items" state
                                            binding.flash.visibility= View.GONE

                                        }
                                    } else {
                                        // If no data in the snapshot, handle it similarly
                                        binding.flash.visibility= View.GONE

                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.e("FirebaseError", "Error fetching discount items", error.toException())
                                    binding.flash.visibility= View.GONE

                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Error fetching user language", error.toException())
                    binding.flash.visibility= View.GONE

                }
            })
    }



    private fun fetchCategories() {
        // Check if categories are stored locally
        val sharedPreferences = requireContext().getSharedPreferences("CategoriesPrefs", Context.MODE_PRIVATE)
        val storedCategoriesJson = sharedPreferences.getString("categories", null)

        if (storedCategoriesJson != null) {
            // Convert the stored JSON string back to a list of categories
            val type = object : TypeToken<List<Category>>() {}.type
            val storedCategories: List<Category> = Gson().fromJson(storedCategoriesJson, type)

            // Populate the categories list and update UI
            categories.clear()
            categories.addAll(storedCategories)
            categoryAdapter.notifyDataSetChanged()
            updateTitlesVisibility()

            Log.d("fetchCategories", "Loaded categories from local storage.")
        }

        // Get the current user ID dynamically
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Fetch the Shop Id from "Addresses" based on the current user ID
        val locationsRef = database.getReference("Addresses").child(currentUserId).child("Shop Id")
        locationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val shopIdString = snapshot.getValue(String::class.java) ?: return

                // Split the Shop Id string by commas and trim each ID
                val shopIdList = shopIdString.split(",").map { it.trim() }

                if (shopIdList.isNotEmpty()) {
                    Log.d("fetchCategories", "All Shop IDs: $shopIdList")

                    // Fetch categories for all Shop IDs
                    fetchCategoriesForAllShops(shopIdList, sharedPreferences)
                } else {
                    Log.e("fetchCategories", "No Shop IDs found.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("fetchCategories", "Failed to fetch Shop Id: ${error.message}")
            }
        })
    }

    private fun fetchCategoriesForAllShops(shopIdList: List<String>, sharedPreferences: SharedPreferences) {
        categories.clear()
        val fetchedCategories = mutableListOf<Category>()
        val addedCategoryNames = mutableSetOf<String>() // Set to track added category names

        // Iterate through each Shop ID and fetch categories
        shopIdList.forEach { shopId ->
            val categoriesRef = database.getReference("Categories").child(shopId)

            categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (categorySnapshot in snapshot.children) {
                        // Fetch the category name and image URL
                        val categoryName = categorySnapshot.key ?: ""
                        val imageUrl = categorySnapshot.child("image").getValue(String::class.java) ?: ""

                        // Skip the category if the name is "discount"
                        if (categoryName.equals("discount", ignoreCase = true)) {
                            continue
                        }

                        // Check if the category name has already been added
                        if (categoryName.isNotEmpty() && imageUrl.isNotEmpty() && !addedCategoryNames.contains(categoryName)) {
                            val category = Category(categoryName, imageUrl)
                            fetchedCategories.add(category)
                            addedCategoryNames.add(categoryName) // Mark the category name as added
                            Log.d("fetchCategories", "Category added: $categoryName with image URL: $imageUrl")
                        }
                    }

                    // Save fetched categories to local storage after the last Shop ID is processed
                    if (shopId == shopIdList.last()) {
                        saveCategoriesToLocalStorage(fetchedCategories, sharedPreferences)

                        // Update UI with fetched categories
                        categories.addAll(fetchedCategories)
                        requireActivity().runOnUiThread {
                            categoryAdapter.notifyDataSetChanged()
                            updateTitlesVisibility()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("fetchCategories", "Failed to fetch categories for Shop ID $shopId: ${error.message}")
                }
            })
        }
    }


    private fun saveCategoriesToLocalStorage(categories: List<Category>, sharedPreferences: SharedPreferences) {
        // Convert categories to JSON
        val categoriesJson = Gson().toJson(categories)

        // Store the categories JSON string in SharedPreferences
        sharedPreferences.edit().putString("categories", categoriesJson).apply()
        Log.d("saveCategories", "Categories saved to local storage.")
    }

    private fun updateTitlesVisibility() {
        // Update visibility of titles or any UI elements based on the fetched categories
        Log.d("updateTitlesVisibility", "Updating UI with ${categories.size} categories.")
    }

    private fun storeCategoryAndOpenFragment(category: Category) {
        // Prepare the Bundle with the category name
        val bundle = Bundle().apply {
            putString("categoryName", category.name)
        }

        // Navigate to FreshFishFragment with the bundle
        openFreshFishFragment(bundle)
    }


    private fun openFreshFishFragment(bundle: Bundle) {
        try {
            Log.d("HomeFragment", "Opening FreshFishFragment with category: ${bundle.getString("categoryName")}")
            findNavController().navigate(R.id.action_homeFragment_to_FreshFishFragment, bundle)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error opening FreshFishFragment", e)
        }
    }

    private fun fetchBuyHistory() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val userBuyHistoryRef = FirebaseDatabase.getInstance().reference.child("user").child(userId).child("BuyHistory")
        val userLocationRef = FirebaseDatabase.getInstance().reference.child("Addresses").child(userId)

        // Fetch the shop name from the "Locations" node
        userLocationRef.child("Shop Id").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(locationSnapshot: DataSnapshot) {
                val locationShopName = locationSnapshot.getValue(String::class.java) ?: ""
                // Split shop names by comma, trimming spaces
                val shopNames = locationShopName.split(",").map { it.trim() }

                // Fetch discounts for all shop names
                fetchDiscountsForShops(shopNames, userBuyHistoryRef)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Locations", "Error fetching location shop name: ${error.message}")
            }
        })
    }

    private fun fetchDiscountsForShops(shopNames: List<String>, userBuyHistoryRef: DatabaseReference) {
        val foodDescriptionsMap = mutableMapOf<String, MutableList<String>>() // To hold food descriptions for each shop

        // Fetch discounts for each shop
        for (shopName in shopNames) {
            val discountRef = FirebaseDatabase.getInstance().reference.child("Shops").child(shopName).child("discount")
            discountRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(discountSnapshot: DataSnapshot) {
                    val foodDescriptions = mutableListOf<String>()

                    // Collect all food descriptions for the shop
                    for (discountChild in discountSnapshot.children) {
                        val foodDescription = discountChild.child("foodDescriptions").getValue(String::class.java)
                        if (foodDescription != null) {
                            foodDescriptions.add(foodDescription)
                        }
                    }
                    foodDescriptionsMap[shopName] = foodDescriptions

                    // Check if all discounts have been fetched before proceeding
                    if (foodDescriptionsMap.size == shopNames.size) {
                        fetchBuyHistoryItems(userBuyHistoryRef, foodDescriptionsMap)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Discount", "Error fetching discount data for $shopName: ${error.message}")
                }
            })
        }
    }

    private fun fetchBuyHistoryItems(userBuyHistoryRef: DatabaseReference, foodDescriptionsMap: Map<String, List<String>>) {
        userBuyHistoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = mutableListOf<OrderItem>()

                for (childSnapshot in snapshot.children) {
                    // Fetch food description from BuyHistory
                    val buyHistoryFoodDescriptions = childSnapshot.child("fooddescription").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()

                    // Check if food description in BuyHistory matches any in the discounts
                    val isMatchFound = buyHistoryFoodDescriptions.any { foodDescription ->
                        foodDescriptionsMap.values.flatten().contains(foodDescription)
                    }

                    if (!isMatchFound) {
                        // Fetch the shop names from BuyHistory as an ArrayList
                        val buyHistoryShopNames = childSnapshot.child("shopNames").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()

                        // Check if the shop name matches any of the location's shop names
                        for (buyHistoryShopName in buyHistoryShopNames) {
                            if (foodDescriptionsMap.containsKey(buyHistoryShopName)) {
                                // Proceed to retrieve other details
                                val foodNamesList = childSnapshot.child("foodNames").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
                                val foodPricesList = childSnapshot.child("foodPrices").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
                                val foodImagesList = childSnapshot.child("foodImage").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
                                val skuList = childSnapshot.child("skuList").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
                                val skuQuantity = childSnapshot.child("skuUnitQuantities").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()

                                // Ensure the sizes of all lists are consistent before accessing
                                if (foodNamesList.isNotEmpty() && foodPricesList.isNotEmpty() && foodImagesList.isNotEmpty() && skuList.isNotEmpty() && skuQuantity.isNotEmpty()) {
                                    // Ensure all lists are of the same length
                                    val listSize = minOf(foodNamesList.size, foodPricesList.size, foodImagesList.size, skuList.size, skuQuantity.size)

                                    // Only proceed if all lists have at least one element
                                    if (listSize > 0) {
                                        // Check all shops and categories for the SKUs
                                        val shopsRef = FirebaseDatabase.getInstance().reference.child("Shops")

                                        // Iterate over all shops
                                        shopsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(shopsSnapshot: DataSnapshot) {
                                                // Iterate through each shop
                                                for (shopSnapshot in shopsSnapshot.children) {
                                                    val shopName = shopSnapshot.key // Shop name (e.g., Shop1001)

                                                    // Iterate through all categories under this shop
                                                    for (categorySnapshot in shopSnapshot.children) {
                                                        val categoryName = categorySnapshot.key // Category name (e.g., Fresh Fish)

                                                        // Check each SKU in this category
                                                        for (skuSnapshot in categorySnapshot.children) {
                                                            val sku = skuSnapshot.key // SKU ID (e.g., Skuid)
                                                            val stockStatus = skuSnapshot.child("stock").getValue(String::class.java)

                                                            // Proceed only if the stock status is "In Stock" and SKU matches
                                                            if (skuList.contains(sku) && stockStatus == "In Stock") {
                                                                // Create PreviousItem with the retrieved details
                                                                val order = PreviousItem(
                                                                    foodName = foodNamesList[0],
                                                                    foodPrice = foodPricesList[0],
                                                                    foodImage = foodImagesList[0],
                                                                    key = skuList[0],
                                                                    skuUnitQuantities = skuQuantity[0],
                                                                    foodDescription = buyHistoryFoodDescriptions[0],
                                                                    shopNames = shopName // Use the matched shop name
                                                                )

                                                                // Wrap PreviousItem in OrderItem.Previous before adding to orders
                                                                orders.add(OrderItem.Previous(order))
                                                            }
                                                        }
                                                    }
                                                }

                                                // Update the adapter and UI visibility once the data is fetched
                                                adapter1.updateData(orders)
                                                updateBuyAgainVisibility(orders)
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                Log.e("ShopsCheck", "Error checking shops and categories: ${error.message}")
                                            }
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BuyHistory", "Error fetching buy history: ${error.message}")
            }
        })
    }


    private fun updateBuyAgainVisibility(orders: MutableList<OrderItem>) {
        if (orders.isEmpty()) {
            binding.buyy.visibility = View.INVISIBLE

            // Update textVieww constraints to be below category
            val paramsTextVieww = binding.textVieww.layoutParams as ConstraintLayout.LayoutParams
            paramsTextVieww.topToBottom = R.id.category  // Adjust R.id.category to your actual ID
            binding.textVieww.layoutParams = paramsTextVieww

            // Update textVieww2 constraints to be below textVieww
            val paramsTextVieww2 = binding.viewall.layoutParams as ConstraintLayout.LayoutParams
            paramsTextVieww2.topToBottom = R.id.category  // Adjust R.id.textVieww to your actual ID
            binding.viewall.layoutParams = paramsTextVieww2

        } else {
            binding.buyy.visibility = View.VISIBLE

            // Restore textVieww constraints to be below buyy
            val paramsTextVieww = binding.textVieww.layoutParams as ConstraintLayout.LayoutParams
            paramsTextVieww.topToBottom = R.id.buyy  // Adjust R.id.buyy to your actual ID
            binding.textVieww.layoutParams = paramsTextVieww

            // Restore textVieww2 constraints to be below textVieww
            val paramsTextVieww2 = binding.viewall.layoutParams as ConstraintLayout.LayoutParams
            paramsTextVieww2.topToBottom = R.id.buyy  // Adjust R.id.textVieww to your actual ID
            binding.viewall.layoutParams = paramsTextVieww2
        }
    }

    private fun setupRecyclerView() {
        binding.previousrRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter =this@HomeFragment.adapter1
        }
    }
    private fun setupRecyclerView1() {

        adapter = NearItemAdapter(menuItems,cartItems, navController,requireContext())
        binding.Nearitemrecycler.apply {
            layoutManager = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
            adapter = this@HomeFragment.adapter
        }
    }

    private fun fetchShopName() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val currentUserId = currentUser.uid
            val locationPath = "Addresses/$currentUserId"
            Log.d("FirebaseDebug", "Fetching data from path: $locationPath")
            databaseRef.child(locationPath)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val shopNames = mutableListOf<String>()
                        Log.d("FirebaseDebug", "Data snapshot: ${dataSnapshot.value}")
                        val shopName = dataSnapshot.child("Shop Id").getValue(String::class.java)
                        Log.d("FirebaseDebug", "Found shop name: $shopName")
                        if (shopName != null) {
                            // Split the shop names by comma and trim any extra spaces
                            val individualShopNames = shopName.split(",").map { it.trim() }
                            shopNames.addAll(individualShopNames)
                        }
                        exploreShopAdapter.setShopList(shopNames)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("FirebaseError", "Error fetching data", databaseError.toException())
                    }
                })
        } else {
            Log.e("FirebaseError", "No authenticated user found.")
        }
    }

    override fun onItemClick(shopName: String) {
        val bundle = Bundle()
        bundle.putString("shopId", shopName) // Passing the shopId to the fragment

        findNavController().navigate(R.id.action_homeFragment_to_shoponefragment, bundle)
    }


    override fun onResume() {
        super.onResume()
        fetchOrders()
        fetchShopName()
        loadFromLocalStorage()


    }

    private fun fetchOrders() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.fetchOrders()

        }
    }


    private fun fetchUserShopNames() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            databaseReference.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val shopNames = snapshot.child("Shop Id").getValue(String::class.java)
                        if (shopNames.isNullOrEmpty()) {
                            // Shop name is not available, navigate to LocationNotAvailable activity
                            val intent = Intent(requireContext(), LocationNotAvailable::class.java)
                            startActivity(intent)
                            requireActivity().finish() // Optional: Finish current activity
                        } else {
                            val shopNameList = shopNames.split(",") // Split shop names by comma
                            shopNameList.forEach { shopName ->
                                fetchMenuItems(shopName.trim())
                                updateTitlesVisibility(menuItems, cartItems, categories)
                                // Trim whitespace from shop name
                            }
                        }
                    } else {
                        // No snapshot exists, navigate to LocationNotAvailable activity
                        val intent = Intent(requireContext(), LocationNotAvailable::class.java)
                        startActivity(intent)
                        requireActivity().finish() // Optional: Finish current activity
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }
    private fun fetchUserShopName() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            databaseReference.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val shopNames = snapshot.child("Shop Id").getValue(String::class.java)
                        Log.d("FetchShopNames", "Shop Id: $shopNames") // Log the raw Shop Id
                        shopNames?.let { names ->
                            val shopNameList = names.split(",").map { it.trim() }
                            Log.d("FetchShopNames", "Shop Names List: $shopNameList") // Log the processed list
                            val allMenuItems = mutableListOf<MenuItem>()
                            var processedShops = 0

                            if (shopNameList.isEmpty()) {
                                Log.d("FetchShopNames", "No shop names to process.")
                                return
                            }

                            shopNameList.forEach { shopName ->
                                fetchMenuItem(shopName) { fetchedMenuItems ->
                                    allMenuItems.addAll(fetchedMenuItems)
                                    processedShops++

                                    // Check if all shops have been processed
                                    if (processedShops == shopNameList.size) {
                                        updateTitlesVisibility(allMenuItems, cartItems, categories)
                                        setupRecyclerView2(allMenuItems)
                                    }
                                }
                            }
                        }
                    } else {
                        Log.d("FetchShopNames", "Snapshot does not exist or Shop Id is missing.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FetchShopNames", "Error fetching Shop Id.", error.toException())
                }
            })
        }
    }


    private fun setupRecyclerView2(menuItems: List<MenuItem>) {
        dealadapter = DealItemAdapter(menuItems, cartItems, requireContext())
        binding.popularRecyclerView1.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = dealadapter
        }
    }

    // Fetch menu items for NearItemAdapter
    private fun fetchMenuItems(shopName: String) {
        val userId = auth.currentUser?.uid ?: return
        menuItems.clear()
        database.getReference("user").child(userId).child("language")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(languageSnapshot: DataSnapshot) {
                    val userLanguage = languageSnapshot.getValue(String::class.java)?.toLowerCase() ?: "english"

                    // Fetch all children under Shops.child(shopName)
                    database.getReference("Shops").child(shopName)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    // Exclude the specific paths
                                    val excludedPaths = setOf("Discount-items", "discount", "Products", "Shop name", "Inventory")

                                    for (menuSnapshot in snapshot.children) {
                                        if (menuSnapshot.key in excludedPaths) continue // Skip excluded paths

                                        // For each valid child (menu)
                                        for (itemSnapshot in menuSnapshot.children) {
                                            val menuItem = itemSnapshot.getValue(MenuItem::class.java)
                                            menuItem?.let {
                                                it.path = shopName

                                                // Handle food names based on user language
                                                val foodNamesList = it.foodName ?: arrayListOf()
                                                val englishName = foodNamesList.getOrNull(0) ?: ""
                                                val languageSpecificName = when (userLanguage) {
                                                    "tamil" -> foodNamesList.getOrNull(1) ?: ""
                                                    "malayalam" -> foodNamesList.getOrNull(2) ?: ""
                                                    "telugu" -> foodNamesList.getOrNull(3) ?: ""
                                                    else -> englishName // Default to English
                                                }

                                                val combinedName = "$englishName / $languageSpecificName"
                                                it.foodName = arrayListOf(combinedName)

                                                // Handle stock logic
                                                val stockStatus = itemSnapshot.child("stock").getValue(String::class.java)
                                                it.stock = stockStatus // Assign stock to the menuItem

                                                menuItems.add(it)
                                            }
                                        }
                                    }

                                    viewModel.setMenuItem(menuItems)
                                    loadFavoriteItems()

                                    binding.viewall.setOnClickListener {
                                        viewModel.setMenuItems(menuItems)
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Handle error
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
    private fun fetchMenuItem(shopName: String, onMenuItemsFetched: (List<MenuItem>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val menuItems = mutableListOf<MenuItem>()

        database.getReference("user").child(userId).child("language")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(languageSnapshot: DataSnapshot) {
                    val userLanguage = languageSnapshot.getValue(String::class.java)?.toLowerCase() ?: "english"

                    // Fetch the first child under Shops.child(shopName) excluding "discount" and "Shop name"
                    database.getReference("Shops").child(shopName)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(shopSnapshot: DataSnapshot) {
                                // Get the first valid child path excluding "discount" and "Shop name"
                                val validChildPath = shopSnapshot.children
                                    .firstOrNull { it.key != "discount" && it.key != "Shop name" }
                                    ?.key

                                if (validChildPath != null) {
                                    // Directly fetch the valid child path
                                    database.getReference("Shops").child(shopName).child(validChildPath)
                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                if (snapshot.exists()) {
                                                    for (itemSnapshot in snapshot.children) {
                                                        val menuItem = itemSnapshot.getValue(MenuItem::class.java)
                                                        menuItem?.let {
                                                            it.path = shopName
                                                            val foodNamesList = it.foodName ?: arrayListOf()
                                                            val englishName = foodNamesList.getOrNull(0) ?: ""
                                                            val languageSpecificName = when (userLanguage) {
                                                                "tamil" -> foodNamesList.getOrNull(1) ?: ""
                                                                "malayalam" -> foodNamesList.getOrNull(2) ?: ""
                                                                "telugu" -> foodNamesList.getOrNull(3) ?: ""
                                                                else -> englishName // Default to English
                                                            }

                                                            val combinedName = if (userLanguage == "english") {
                                                                englishName
                                                            } else {
                                                                "$englishName / $languageSpecificName"
                                                            }

                                                            it.foodName = arrayListOf(combinedName)

                                                            // Retrieve stock status
                                                            val stockStatus = itemSnapshot.child("stock").getValue(String::class.java)

                                                            // Handle stock status
                                                            when (stockStatus) {
                                                                "In Stock" -> {
                                                                    it.stock = "In Stock"
                                                                    menuItems.add(it)
                                                                }
                                                                "Out Of Stock" -> {
                                                                    it.stock = "Out Of Stock"
                                                                    menuItems.add(it)
                                                                }
                                                                else -> {
                                                                    // If stock status is not found or is unknown, handle it
                                                                    it.stock = "Unknown"
                                                                    menuItems.add(it)
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // Return the fetched menu items
                                                    onMenuItemsFetched(menuItems)
                                                } else {
                                                    Log.d("FetchMenuItem", "$validChildPath does not exist under $shopName.")
                                                    onMenuItemsFetched(emptyList())
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                Log.e("FetchMenuItem", "Error fetching items for $validChildPath under $shopName", error.toException())
                                                onMenuItemsFetched(emptyList())
                                            }
                                        })
                                } else {
                                    Log.d("FetchMenuItem", "No valid child found under $shopName excluding 'discount' and 'Shop name'.")
                                    onMenuItemsFetched(emptyList())
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("FetchMenuItem", "Error fetching shop data for $shopName", error.toException())
                                onMenuItemsFetched(emptyList())
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FetchMenuItem", "Error fetching language for user: $userId", error.toException())
                }
            })
    }

    private fun loadFavoriteItems() {
        userId?.let { userId ->
            val databaseRef = FirebaseDatabase.getInstance().getReference("Favourite").child(userId)

            databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val favoriteItems = mutableMapOf<String, MenuItem>()
                        snapshot.children.forEach { itemSnapshot ->
                            val favoriteItem = itemSnapshot.getValue(MenuItem::class.java)
                            favoriteItem?.let {
                                it.firebaseKey = itemSnapshot.key // Store the item key
                                it.foodName?.forEach { foodName ->
                                    favoriteItems[foodName] = it
                                }
                            }
                        }
                        updateMenuItemsWithFavorites(favoriteItems)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    private fun updateMenuItemsWithFavorites(favoriteItems: Map<String, MenuItem>) {
        menuItems.forEach { menuItem ->
            menuItem.foodName?.let { foodNames ->
                foodNames.forEach { foodName ->
                    favoriteItems[foodName]?.let { favoriteItem ->
                        menuItem.favorite = favoriteItem.favorite
                        menuItem.firebaseKey = favoriteItem.firebaseKey // Store the key in the menu item
                    }
                }
            }
        }
        adapter.notifyDataSetChanged()
    }
    private fun observeViewModel() {
        viewModel.address.observe(viewLifecycleOwner) { address ->
            binding.tvAddress.text = address
        }

        viewModel.locality.observe(viewLifecycleOwner) { locality ->
            binding.tvLocality.text = locality
        }
        viewModel.latitude.observe(viewLifecycleOwner) { latitude ->
            binding.tvLatitude.text = latitude.toString()
        }
        viewModel.longitude.observe(viewLifecycleOwner) { longitude ->
            binding.tvLogitude.text = longitude.toString()
        }


    }


    private fun retrieveAddressAndLocality() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val userLocationRef = databaseReference.child(uid)
            userLocationRef.addListenerForSingleValueEvent(object : ValueEventListener {
                @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val address = snapshot.child("address").getValue(String::class.java)
                        val locality = snapshot.child("locality").getValue(String::class.java)
                        val latitude = snapshot.child("latitude").getValue(Double::class.java)
                        val longitude = snapshot.child("longitude").getValue(Double::class.java)

                        if (isAdded) {
                            saveToLocalStorage(address, locality, latitude?.toString(), longitude?.toString())
                            updateUI(address, locality, latitude, longitude)
                        } else {
                            // Save data to local storage but defer UI update
                            saveToLocalStorage(address, locality, latitude?.toString(), longitude?.toString())
                        }
                    } else {
                        // Handle case where data doesn't exist
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    private fun updateUI(address: String?, locality: String?, latitude: Double?, longitude: Double?) {
        binding.tvAddress.text = address
        binding.tvLocality.text = locality
        binding.tvLatitude.text = latitude?.toString() ?: "N/A"
        binding.tvLogitude.text = longitude?.toString() ?: "N/A"
    }

    private fun saveToLocalStorage(address: String?, locality: String?, latitude: String?, longitude: String?) {
        if (!isAdded) {
            // Fragment is not attached to an activity, abort the operation
            Log.e("AccountFragment", "Fragment is not attached to an activity. Cannot save to local storage.")
            return
        }

        val sharedPreferences = requireActivity().getSharedPreferences("UserDetails", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("address", address)
        editor.putString("locality", locality)
        editor.putString("latitude", latitude)
        editor.putString("longitude", longitude)
        editor.apply()
    }
    private fun loadFromLocalStorage() {
        val sharedPreferences = requireActivity().getSharedPreferences("UserDetails", Context.MODE_PRIVATE)
        val address = sharedPreferences.getString("address", "N/A")
        val locality = sharedPreferences.getString("locality", "N/A")
        val latitude = sharedPreferences.getString("latitude", "N/A")
        val longitude = sharedPreferences.getString("longitude", "N/A")

        binding.tvAddress.text = address
        binding.tvLocality.text = locality
        binding.tvLatitude.text = latitude
        binding.tvLogitude.text = longitude
    }
    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.address, popupMenu.menu)

        // Set text color for the PopupMenu items
        val menu = popupMenu.menu
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val spannableString = SpannableString(menuItem.title.toString())
            spannableString.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.navy)),
                0,
                spannableString.length,
                0
            )
            menuItem.title = spannableString
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.use_current_location -> {
                    // Handle "Use Current Location" click
                    val intent = Intent(requireContext(), LocationActivity::class.java)
                    startActivity(intent)
                    // Hide the dropdown icon after picking an address
                    true
                }
                // Add more saved address clicks as needed
                else -> false
            }
        }

        popupMenu.setOnDismissListener {
            // Dismiss the PopupMenu when it's dismissed
            popupMenu.dismiss()
        }

        popupMenu.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupImageSlider()
        loadFromLocalStorage()

        binding.viewall.setOnClickListener {
            // Calculate the offset of textVieww1 relative to the top of scrollView
            val offset = binding.Nearitemrecycler.bottom
            // Scroll scrollView to the position of textVieww1
            binding.scrollView.smoothScrollTo(0, offset)
        }
    }

    private fun setupImageSlider() {
        val imageList = ArrayList<SlideModel>()
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference.child("Banners")

        // List all images under "Banners" path
        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                listResult.items.forEach { item ->
                    // Get download URL for each image
                    item.downloadUrl.addOnSuccessListener { uri ->
                        // Add each image to the imageList
                        imageList.add(SlideModel(uri.toString(), scaleType = ScaleTypes.FIT))
                        // Set imageList to the image slider
                        binding.imageSlider.setImageList(imageList)
                    }.addOnFailureListener { exception ->
                        // Handle any errors during getting download URL

                    }
                }
            }
            .addOnFailureListener { exception ->
                // Handle any errors during listing images

            }

        val imageSlide = binding.imageSlider
        imageSlide.setItemClickListener(object : ItemClickListener {
            override fun doubleClick(position: Int) {
                // Double click listener implementation
            }

            override fun onItemSelected(position: Int) {
                // Check if the selected image is "ban"
            }
        })
    }
}