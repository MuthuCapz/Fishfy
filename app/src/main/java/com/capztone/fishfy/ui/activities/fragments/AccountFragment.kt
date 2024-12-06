package com.capztone.fishfy.ui.activities.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation.findNavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.capztone.admin.utils.FirebaseAuthUtil
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.FragmentAccountBinding
import com.capztone.fishfy.ui.activities.LoginActivity
import com.capztone.fishfy.ui.activities.ViewModel.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AccountFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentAccountBinding
    private lateinit var userNameEditText: TextView
    private lateinit var userEmailEditText: TextView
    private lateinit var banner: TextView

    private var isInEditMode = false // Track if in edit mode

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

auth = FirebaseAuthUtil.auth
        val user = auth.currentUser

        // Set up status bar and background
        setupStatusBar()

        // Load user details from local storage
        loadFromLocalStorage()

        userNameEditText = binding.profileName
        userEmailEditText = binding.profileEmail

        // Fetch and display user details
        fetchAndDisplayUserDetails(user)
        // Add TextWatcher for profileName to validate on focus change
        binding.profileName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val newName = s.toString()
                if (newName.length < 3 || newName.length > 15) {
                    binding.profileName.error = "User name must be between 3 and 15 characters"
                } else {
                    binding.profileName.error = null
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Add TextWatcher for profileEmail to validate on focus change
        binding.profileEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val newEmail = s.toString().trim() // Trim any leading or trailing whitespace
                var mobileNumber = newEmail.replace("\\D".toRegex(), "") // Remove non-digit characters

                // Remove leading "+91" or "91" if present
                if (mobileNumber.startsWith("91")) {
                    mobileNumber = mobileNumber.substring(2) // Remove "91"
                } else if (mobileNumber.startsWith("+91")) {
                    mobileNumber = mobileNumber.substring(3) // Remove "+91"
                }

                if (mobileNumber.length != 10) {
                    binding.profileEmail.error = "Mobile number must be exactly 10 digits"
                } else {
                    binding.profileEmail.error = null
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })



        binding.recentBackButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
// In onViewCreated or onCreateView after binding views
        binding.profileName.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && !isInEditMode) {
                return@setOnTouchListener true
            }
            false
        }

        binding.profileEmail.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && !isInEditMode) {
                return@setOnTouchListener true
            }
            false
        }
        // Set onClickListener for logout text

        binding.btnDialogCancel.setOnClickListener {
            cancelEditMode() // Call toggleEditMode to exit edit mode
        }

        // Handle edit button click
        binding.edit.setOnClickListener {
            toggleEditMode()
        }

        // Save button click listener
        binding.btnDialogSave.setOnClickListener {
            saveChanges(user)
        }
    }
    // Method to fetch and display user details with offline handling
    private fun fetchAndDisplayUserDetails(user: FirebaseUser?) {
        if (user != null) {
            val userId = user.uid
            val database = FirebaseDatabase.getInstance()
            val userDetailsRef = database.getReference("Addresses").child(userId).child("User Details")

            // Enable offline capabilities for this reference
            userDetailsRef.keepSynced(true)

            userDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userName = snapshot.child("user name").getValue(String::class.java) ?: "No Name"
                        var mobileNumber = snapshot.child("mobile number").getValue(String::class.java) ?: "No Mobile Number"

                        // Ensure mobile number includes "+91" prefix
                        if (!mobileNumber.startsWith("+91")) {
                            mobileNumber = "+91 $mobileNumber"
                        }

                        // Display retrieved user details
                        binding.profileName.setText(userName)
                        binding.profileEmail.setText(mobileNumber)
                        binding.bannerName.text = userName
                        saveToLocalStorage(userName, mobileNumber)

                        // Load profile image if available
                        val photoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl
                        if (photoUrl != null) {
                            Glide.with(this@AccountFragment)
                                .load(photoUrl)
                                .into(binding.profileImage)
                        } else {
                            // Load a default image if no photo URL is available
                            Glide.with(this@AccountFragment)
                                .load(R.drawable.baseline_account_circle_24)
                                .into(binding.profileImage)
                        }
                    } else {
                        // Load from local storage if not available online
                        loadFromLocalStorage()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error and load from local storage
                    loadFromLocalStorage()
                }
            })
        } else {
            // Load from local storage if user is null
            loadFromLocalStorage()
        }
    }

    // Method to load user details from local storage
    private fun loadFromLocalStorage() {
        val sharedPrefs = requireActivity().getSharedPreferences("UserPrefs", 0)
        val userName = sharedPrefs.getString("userName", "No Name")
        val mobileNumber = sharedPrefs.getString("mobileNumber", "No Mobile Number")

        binding.profileName.setText(userName)
        binding.profileEmail.setText(mobileNumber)
        binding.bannerName.text = userName

        val photoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl

        Glide.with(this@AccountFragment)
            .load(photoUrl)
            .into(binding.profileImage)

    }

    // Method to save user details to local storage
    private fun saveToLocalStorage(userName: String, mobileNumber: String) {
        val photoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl
        val sharedPrefs = requireActivity().getSharedPreferences("UserPrefs", 0)
        val editor = sharedPrefs.edit()
        editor.putString("userName", userName)
        editor.putString("mobileNumber", mobileNumber)
        editor.putString("profilePicUrl", photoUrl.toString())

        editor.apply()
    }




    override fun onResume() {
        super.onResume()
        // Fetch and display user details again
        val user = auth.currentUser
        fetchAndDisplayUserDetails(user)
        loadFromLocalStorage()

    }

    // Modified toggleEditMode function
    private fun toggleEditMode() {
        isInEditMode = !isInEditMode

        // Enable or disable editing UI elements
        binding.profileName.isEnabled = isInEditMode
        binding.profileEmail.isEnabled = isInEditMode
        binding.btnDialogSave.visibility = if (isInEditMode) View.VISIBLE else View.GONE
        binding.btnDialogCancel.visibility = if (isInEditMode) View.VISIBLE else View.GONE
    }

    // New function to handle cancel action
    private fun cancelEditMode() {
        // Revert changes and exit edit mode
        val user = FirebaseAuth.getInstance().currentUser
        fetchAndDisplayUserDetails(user)
        isInEditMode = false

        // Disable editing UI elements
        binding.profileName.isEnabled = false
        binding.profileEmail.isEnabled = false
        binding.btnDialogSave.visibility = View.GONE
        binding.btnDialogCancel.visibility = View.GONE
    }

    // Modified saveChanges function
    // Modified saveChanges function
    private fun saveChanges(user: FirebaseUser?) {
        if (user != null) {
            val newName = binding.profileName.text.toString().trim()
            var newMobile = binding.profileEmail.text.toString().trim()

            // Validate user name (3 to 15 characters)
            if (newName.length < 3 || newName.length > 15) {
                Toast.makeText(requireContext(), "User name must be between 3 and 15 characters", Toast.LENGTH_SHORT).show()
                return
            }

            // Remove any non-numeric characters from mobile number
            newMobile = newMobile.replace("\\D".toRegex(), "")

            // Remove leading "+91" if present
            if (newMobile.startsWith("91")) {
                newMobile = newMobile.substring(2) // Remove "91"
            } else if (newMobile.startsWith("+91")) {
                newMobile = newMobile.substring(3) // Remove "+91"
            }

            // Validate mobile number (exactly 10 digits)
            if (newMobile.length != 10) {
                Toast.makeText(requireContext(), "Mobile number must be exactly 10 digits", Toast.LENGTH_SHORT).show()
                return
            }

            // Update Firebase user profile with new name
            val userProfileChangeRequest = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()

            user.updateProfile(userProfileChangeRequest)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Update mobile number if it's changed
                        val database = FirebaseDatabase.getInstance()
                        val userDetailsRef = database.getReference("Addresses").child(user.uid).child("User Details")

                        userDetailsRef.child("user name").setValue(newName)
                        userDetailsRef.child("mobile number").setValue("+91$newMobile") // Save with +91 prefix

                        // Update bannerName after successful profile update
                        binding.bannerName.text = newName

                        // Show toast indicating profile update success
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()

                        // Exit edit mode after saving changes
                        toggleEditMode()
                    } else {
                        // Handle profile update failure
                        Toast.makeText(requireContext(), "Failed to update profile. Please try again later.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun signOut() {
        // Sign out from Firebase Authentication
        FirebaseAuth.getInstance().signOut()

        // Sign out from Google
        val googleSignInClient =
            GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN)
        googleSignInClient.signOut().addOnCompleteListener {
            // Redirect to login activity
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun setupStatusBar() {
        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.statusBarColor = Color.TRANSPARENT
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.statusBarColor = Color.TRANSPARENT
            }
        }
    }
}