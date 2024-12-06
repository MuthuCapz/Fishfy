package com.capztone.fishfy.ui.activities

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController

import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.capztone.admin.utils.FirebaseAuthUtil
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.ActivityMainBinding
import com.capztone.fishfy.ui.activities.ViewModel.MainViewModel

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

// Add import statements
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var viewModel: MainViewModel
    private lateinit var database: DatabaseReference
    private lateinit var carItemTextView: TextView
    private lateinit var shopnameTextView: TextView
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        auth = FirebaseAuthUtil.auth

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().reference
           // Initialize ViewModel
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(MainViewModel::class.java)



        // Initialize carItem and shopName TextViews
        carItemTextView = findViewById(R.id.carItem)
        shopnameTextView = findViewById(R.id.shopnameTextView)


        // Fetch cart items count and shop name, update TextViews
        fetchCartItemsCount()


        // Find TextView and ImageView by their IDs
        val textViewViewCart = findViewById<TextView>(R.id.viewcart)
        val imageViewViewCart = findViewById<ImageView>(R.id.viewcart1)

        // Set click listeners for both TextView and ImageView
        textViewViewCart.setOnClickListener {
            navigateToCartFragment()
        }

        imageViewViewCart.setOnClickListener {
            navigateToCartFragment()
        }

        binding.tvAddress.setOnClickListener {
            showPopupMenu(it)
        }

        // Check if fragmentContainerView exists in the layout
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView)
            ?: // Handle the case where fragmentContainerView is not found
            // You might want to log an error or handle it in an appropriate way
            return

        // Find NavController only if fragmentContainerView is found
        val navController = navHostFragment.findNavController()


        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val address = intent.getStringExtra("ADDRESS")
        val locality = intent.getStringExtra("LOCALITY")
        val shoptextview = intent.getStringExtra("SHOP_TEXT_VIEW_VALUE")

        // Display the address and locality in TextViews
        binding.tvAddress.text = "Address: $address"
        binding.tvLocality.text = " $locality"

        // Check for null before setting up with NavController
        navController?.let {
            bottomNav.setupWithNavController(it)
        }

        // Observe the user address from ViewModel
        viewModel.userAddress.observe(this) { userAddress ->
            // Update UI with the user's address
            binding.tvAddress.text = "Address: $userAddress"
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Ensure the CardView is hidden if carItemTextView.text is "0"
            val shouldShowCardView = carItemTextView.text.toString() != "0"
            when (destination.id) {
                R.id.shopOneFragment,
                R.id.contactusActivity,
                R.id.freshFishFragment,

                R.id.detailsFragment-> {
                    bottomNav.visibility = View.GONE
                    binding.addcart.visibility = if (shouldShowCardView) View.VISIBLE else View.GONE
                    binding.showItemCardView.visibility = if (shouldShowCardView) View.VISIBLE else View.GONE
                }
                R.id.historyFragment,
                R.id.profileFragment,
                R.id.cartFragment -> {
                    bottomNav.visibility = View.VISIBLE
                    binding.showItemCardView.visibility = View.GONE
                    binding.addcart.visibility=View.GONE
                }
                R.id.accountFragment ,
                R.id.productSearchFragment,
                R.id.contactusActivity,
                R.id.addressFragment,
                R.id.profilelanguageActivity, -> {
                    bottomNav.visibility = View.GONE
                    binding.showItemCardView.visibility = View.GONE
                    binding.addcart.visibility=View.GONE
                }
                R.id.detailsFragment -> {
                    bottomNav.visibility = View.VISIBLE
                    binding.addcart.visibility = if (shouldShowCardView) View.VISIBLE else View.GONE
                    binding.showItemCardView.visibility = if (shouldShowCardView) View.VISIBLE else View.GONE

                }
                R.id.noNetworkFragment -> {
                    bottomNav.visibility = View.INVISIBLE
                    binding.addcart.visibility = if (shouldShowCardView) View.VISIBLE else View.GONE
                    binding.showItemCardView.visibility = if (shouldShowCardView) View.VISIBLE else View.GONE

                }
                R.id.historyFragment-> {
                    bottomNav.visibility = View.VISIBLE
                    binding.addcart.visibility = if (shouldShowCardView) View.VISIBLE else View.GONE
                    binding.showItemCardView.visibility = if (shouldShowCardView) View.VISIBLE else View.GONE

                }
                R.id.myordersfragment-> {
                    bottomNav.visibility = View.GONE

                }

                else -> {
                    bottomNav.visibility = View.VISIBLE
                    binding.showItemCardView.visibility = if (shouldShowCardView) View.VISIBLE else View.GONE
                    binding.addcart.visibility = if (shouldShowCardView) View.VISIBLE else View.GONE
                }
            }
        }

    }

    private fun navigateToCartFragment() {
        val navController = findNavController(R.id.fragmentContainerView)
        navController.navigate(R.id.action_homefragment_to_cartfragment)
        binding.bottomNavigationView.visibility=View.GONE
    }


    private fun isCartFragmentVisible(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView)
        if (navHostFragment != null) {
            val navController = navHostFragment.findNavController()
            return navController.currentDestination?.id == R.id.cartFragment
        }
        return false
    }

    private fun fetchCartItemsCount() {
        val userId = auth.currentUser?.uid ?: return

        // Check network connectivity before fetching data
        if (!isNetworkAvailable()) {
            binding.showItemCardView.visibility = View.GONE
            binding.addcart.visibility = View.GONE
            return
        }

        database.child("user").child(userId).child("cartItems").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                carItemTextView.text = count.toString()

                // Determine visibility based on cart items count
                if (count > 0) {
                    if (!isCartFragmentVisible()) {
                        binding.showItemCardView.visibility = View.VISIBLE
                        binding.addcart.visibility = View.VISIBLE
                    } else {
                        binding.showItemCardView.visibility = View.GONE
                        binding.addcart.visibility = View.GONE
                    }
                } else {
                    binding.showItemCardView.visibility = View.GONE
                    binding.addcart.visibility = View.GONE
                }

                // Fetch shopName for each cart item
                for (cartItemSnapshot in snapshot.children) {
                    val productId = cartItemSnapshot.key ?: continue
                    database.child("user").child(userId).child("cartItems").child(productId).child("path").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(pathSnapshot: DataSnapshot) {
                            if (pathSnapshot.exists()) {
                                val pathValue = pathSnapshot.value.toString()
                                fetchShopName(pathValue)
                            } else {
                                shopnameTextView.text = "Shop name not found"
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            shopnameTextView.text = "Error: ${error.message}"
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                carItemTextView.text = "Error: ${error.message}"
            }
        })
    }

    private fun fetchShopName(pathValue: String) {
        database.child("ShopNames").child(pathValue).child("shopName").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(shopNameSnapshot: DataSnapshot) {
                if (shopNameSnapshot.exists()) {
                    val shopName = shopNameSnapshot.value.toString()
                    shopnameTextView.text = "$shopName "
                } else {
                    shopnameTextView.text = "Shop name not found"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                shopnameTextView.text = "Error: ${error.message}"
            }
        })
    }


    override fun onResume() {
        super.onResume()
        fetchCartItemsCount()
    }



    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            return networkInfo.isConnected
        }
    }
    private fun loadAnimation(animationView: LottieAnimationView, animationFileName: String) {
        LottieCompositionFactory.fromAsset(this, animationFileName)
            .addListener { composition ->
                animationView.setComposition(composition)
            }
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.address, popupMenu.menu)

        // Set text color for the PopupMenu items
        val menu = popupMenu.menu
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val spannableString = SpannableString(menuItem.title.toString())
            spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.navy)), 0, spannableString.length, 0)
            menuItem.title = spannableString
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.use_current_location -> {
                    // Handle "Use Current Location" click
                    startActivity(Intent(this, LocationActivity::class.java))
                    // Hide the dropdown icon after picking an address
                    binding.dropdown.visibility = View.GONE
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

    override fun onBackPressed() {
        // Check if there are fragments in the back stack
        if (supportFragmentManager.backStackEntryCount > 0) {
            // Pop the fragment from the back stack
            supportFragmentManager.popBackStack()
        } else {
            // If no fragments in the back stack, let the system handle the back button press
            super.onBackPressed()
        }
    }

}