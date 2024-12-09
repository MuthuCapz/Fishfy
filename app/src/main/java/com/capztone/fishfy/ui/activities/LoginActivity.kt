package com.capztone.fishfy.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.capztone.admin.utils.FirebaseAuthUtil
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.ActivityLoginBinding
import com.capztone.fishfy.ui.activities.VerifyNumberActivity.Companion.phoneNumberKey
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var binding: ActivityLoginBinding
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuthUtil.auth
        configureGoogleSignIn()



        database = FirebaseDatabase.getInstance().reference

        binding.googleLoginbutton.setOnClickListener {
            signIn()
        }

        binding.btnGetOtp.setOnClickListener {
            validateNumber()
        }
    }

    private fun validateNumber() {
        val phoneNumber = binding.etPhoneNum.editableText?.toString()

        if (phoneNumber.isNullOrEmpty()) {
            binding.etPhoneNum.error = "Enter your Phone Number"
            binding.etPhoneNum.requestFocus()
            return
        }

        if (phoneNumber.length != 10) {
            Toast.makeText(this, "Enter 10 digit number", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if the phone number exists in the DeletedAccounts path
        val databaseReference = FirebaseDatabase.getInstance().getReference("DeletedAccounts")
        val query = databaseReference.orderByChild("phoneNumber").equalTo(phoneNumber)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val targetActivity = if (dataSnapshot.exists()) {
                    NotThereActivity::class.java
                } else {
                    VerifyNumberActivity::class.java
                }
                val intent = Intent(this@LoginActivity, targetActivity).apply {
                    putExtra(phoneNumberKey, phoneNumber)
                }
                startActivity(intent)
                finish()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@LoginActivity, "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_Id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signIn() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val email = account.email
                if (email != null) {
                    checkDeletedAccounts(email) { isDeleted ->
                        if (isDeleted) {
                            navigateToNotThereActivity()
                        } else {
                            signInWithGoogle(account.idToken!!)
                        }
                    }
                }
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

    private fun checkDeletedAccounts(email: String, callback: (Boolean) -> Unit) {
        val deletedAccountsRef = database.child("DeletedAccounts")
        deletedAccountsRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "checkDeletedAccounts:onCancelled", error.toException())
                callback(false)
            }
        })
    }

    private fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user != null) {
                    checkUserSetupAndStoreData(user)
                }
            } else {
                Log.w(TAG, "Google sign in failed", task.exception)
                Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUserSetupAndStoreData(user: FirebaseUser) {
        val userRef = database.child("users").child(user.uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // User exists, navigate to MainActivity
                    navigateToMainActivity()
                } else {
                    // User does not exist, navigate to LanguageActivity and then store data
                    navigateToLanguageActivity()
                    storeUserData(user)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "checkUserSetup:onCancelled", error.toException())
                Toast.makeText(this@LoginActivity, "Failed to check user setup", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun storeUserData(user: FirebaseUser) {
        val userRef = database.child("users")
        val counterRef = database.child("UserIDCounter") // Counter to keep track of the last used user ID

        // Get current date and time
        val currentDate = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())

        // Retrieve the current counter value
        counterRef.get().addOnCompleteListener { counterTask ->
            if (counterTask.isSuccessful) {
                val currentCounter = counterTask.result.getValue(Int::class.java) ?: 1000 // Default to 1000 if null

                // Generate a custom user ID (e.g., USER1001, USER1002, etc.)
                val customUserId = "USER${currentCounter + 1}"

                // Check if the user signed in with Google or mobile
                val profileImageUrl = if (user.photoUrl != null) {
                    user.photoUrl.toString() // Google profile URL
                } else {
                    R.drawable.bg_profile
                }

                // Create user data map
                val userMap = mapOf(
                    "userid" to customUserId,
                    "email" to user.email,
                    "username" to user.displayName,
                    "profileImage" to profileImageUrl, // Save "default" or Google profile URL
                    "LoginDate" to currentDate // Store current date and time
                )

                // Store the user data under the custom user ID
                userRef.child(user.uid).setValue(userMap).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "User data stored successfully with login time")

                        // Update the counter in the database
                        counterRef.setValue(currentCounter + 1).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Log.d(TAG, "User ID counter updated successfully")
                            } else {
                                Log.w(TAG, "Failed to update user ID counter")
                            }
                        }
                    } else {
                        Log.w(TAG, "Failed to store user data")
                        Toast.makeText(baseContext, "Failed to store user data", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Log.w(TAG, "Failed to retrieve user ID counter")
            }
        }
    }
    private fun navigateToNotThereActivity() {
        val intent = Intent(this, NotThereActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToLanguageActivity() {
        val intent = Intent(this, LanguageActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }
}