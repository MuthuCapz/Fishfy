package com.capztone.fishfy.ui.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.capztone.fishfy.databinding.ActivityDeleteAccountBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import android.Manifest
import android.content.pm.PackageManager
import com.capztone.admin.utils.FirebaseAuthUtil

class DeleteAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeleteAccountBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private val handler = Handler(Looper.getMainLooper()) // Initialize Handler
    private val SMS_PERMISSION_CODE = 101
    private lateinit var smsReceiver: SmsReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeleteAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
auth = FirebaseAuthUtil.auth
        databaseRef = FirebaseDatabase.getInstance().reference
        fetchUserInfo()
        binding.deleteButton.setOnClickListener {
            validateInput()
        }
        binding.deleteBackButton.setOnClickListener {
            finish() // This will close the current activity and go back to the previous one
        }

        binding.textview111.setOnClickListener {
            val url = "https://www.astracape.com/privacy-policy.html"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            startActivity(intent)
        }

        setupSmsReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
    }



    private fun setupSmsReceiver() {
        smsReceiver = SmsReceiver()
        val intentFilter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
        registerReceiver(smsReceiver, intentFilter)
    }

    private fun fetchUserInfo() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            val userRef = databaseRef.child("users").child(currentUserId)
            userRef.get().addOnSuccessListener { dataSnapshot ->
                val email = dataSnapshot.child("email").getValue(String::class.java)
                val phoneNumber = dataSnapshot.child("phoneNumber").getValue(String::class.java)
                val userId = dataSnapshot.child("userid").getValue(String::class.java)
                binding.userId.text=userId
                // Update visibility based on the retrieved data
                if (email != null) {
                    binding.usermail.setText(email)
                    binding.mailid.visibility = View.VISIBLE
                } else {
                    binding.mailid.visibility = View.GONE
                }

                if (phoneNumber != null) {
                    binding.usermobilenumber.setText(phoneNumber)
                    binding.usermobile.visibility = View.VISIBLE
                } else {
                    binding.usermobile.visibility = View.GONE
                }

                // If neither email nor phone number is present, hide both fields
                if (email == null && phoneNumber == null) {
                    binding.usermail.visibility = View.GONE
                    binding.usermobilenumber.visibility = View.GONE
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to retrieve user info: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No user is currently logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInput() {
        val name = binding.username.text.toString().trim()
        val email = binding.usermail.text.toString().trim()
        val mobile = binding.usermobilenumber.text.toString().trim()
        val reason = binding.userreason.text.toString().trim()
        val isCheckboxChecked = binding.deletecomfirm.isChecked

        if (name.isEmpty()) {
            binding.username.error = "Name is required"
            binding.username.requestFocus()
            return
        }

        if (name.length > 15) {
            binding.username.error = "Name must be at most 15 characters"
            binding.username.requestFocus()
            return
        }

        if (email.isEmpty() && mobile.isEmpty()) {
            Toast.makeText(this, "Either email or phone number is required", Toast.LENGTH_SHORT).show()
            return
        }

        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.usermail.error = "Invalid email format"
            binding.usermail.requestFocus()
            return
        }

        if (reason.isEmpty()) {
            binding.userreason.error = "Reason is required"
            binding.userreason.requestFocus()
            return
        }

        if (reason.length > 100) {
            binding.userreason.error = "Reason must be at most 100 characters"
            binding.userreason.requestFocus()
            return
        }

        if (!isCheckboxChecked) {
            Toast.makeText(this, "Please confirm by checking the checkbox", Toast.LENGTH_SHORT)
                .show()
            return
        }

        checkUserInDatabase(email, mobile)
    }

    private fun checkUserInDatabase(email: String, phoneNumber: String) {
        val usersRef = databaseRef.child("users")
        usersRef.get().addOnSuccessListener { dataSnapshot ->
            var userId: String? = null
            var retrievedEmail: String? = null
            var retrievedPhone: String? = null

            for (userSnapshot in dataSnapshot.children) {
                val userEmail = userSnapshot.child("email").getValue(String::class.java)
                val userPhone = userSnapshot.child("phoneNumber").getValue(String::class.java)
                if (userEmail == email || userPhone == phoneNumber) {
                    userId = userSnapshot.key // Get the user ID
                    retrievedEmail = userEmail
                    retrievedPhone = userPhone
                    break
                }
            }

            if (userId != null) {
                auth.currentUser?.let { user ->
                    when {
                        retrievedEmail == email -> {
                            deleteUserAccount(userId, true) // Email account deletion
                        }
                        retrievedPhone == phoneNumber -> {
                            deleteUserAccount(userId, false) // Phone number account deletion
                        }
                        else -> {
                            Toast.makeText(this, "Email or phone number does not match the current user", Toast.LENGTH_SHORT).show()
                        }
                    }
                } ?: run {
                    Toast.makeText(this, "No user is currently logged in", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "User not found in Firebase 'users' path", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to check user: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteUserAccount(userId: String, isEmail: Boolean) {
        val deletedDate = getCurrentDateTime()

        val deleteRequestRef = FirebaseDatabase.getInstance().getReference("DeletionRequests").child(userId)
        val deleteRequest = mapOf(
            "deletedDate" to deletedDate  // Store only the deletedDate in the desired format
        )

        deleteRequestRef.setValue(deleteRequest).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val usersRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                usersRef.removeValue().addOnCompleteListener { deleteUserTask ->
                    if (deleteUserTask.isSuccessful) {
                        saveDeleteReason(userId)
                        scheduleDeletion(userId)
                        Toast.makeText(this, "Your account deletion request has been submitted", Toast.LENGTH_SHORT).show()
                        navigateToAppropriateActivity()
                    } else {
                        Toast.makeText(this, "Request failed", Toast.LENGTH_SHORT).show()
                        sendSMSAndNavigate()
                    }
                }
            } else {
                Toast.makeText(this, "Failed to mark account for deletion", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun sendSMSAndNavigate() {
        val receiveNumber = binding.textview111.text.toString().trim() // Get the phone number from TextView
        if (receiveNumber.isNotEmpty()) {
            if (checkSmsPermission()) {
                sendSMSNotification(receiveNumber)
            } else {
                requestSmsPermission()
            }
        } else {
            navigateToAppropriateActivity()
        }
    }

    private fun checkSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_CODE)
    }

    private fun sendSMSNotification(receiveNumber: String) {
        try {
            val smsManager = SmsManager.getDefault()
            // Message content includes the sender name (Fishfy) and sending number (Fishfy506070).
            val message = "From: Fishfy (ID: Fishfy506070)\nYour account has been deleted successfully."
            smsManager.sendTextMessage(receiveNumber, null, message, null, null)
            Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
            navigateToAppropriateActivity()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SMS: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scheduleDeletion(userId: String) {
        val delayMillis = 30L * 24 * 60 * 60 * 1000 // 30 days in milliseconds
        handler.postDelayed({
            deleteUserPaths(userId)
        }, delayMillis)
    }

    private fun deleteUserPaths(userId: String) {
        val pathsToDelete = listOf(
            "Addresses",
            "PayoutAddress",
            "OrderDetails",
            "Favourite",

            )

        val deleteTasks = pathsToDelete.map { path ->
            FirebaseDatabase.getInstance().getReference(path).child(userId).removeValue()
        }

        Tasks.whenAllComplete(deleteTasks).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Your account has been permanently deleted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to delete user account", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun saveDeleteReason(userId: String) {
        val deleteRef = FirebaseDatabase.getInstance().getReference("DeletedAccounts").child(userId)
        val reason = binding.userreason.text.toString().trim()
        val formattedTime = getCurrentDateTime()
        val deleteInfo = mapOf(
            "name" to binding.username.text.toString().trim(),
            "email" to binding.usermail.text.toString().trim(),
            "phoneNumber" to binding.usermobilenumber.text.toString().trim(),
            "reason" to reason,
            "deletedTime" to formattedTime,
            "userUid" to binding.userId.text.toString().trim(),
        )

        deleteRef.setValue(deleteInfo).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Toast.makeText(this, "Failed to save delete reason", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to get current date and time in "dd-MM-yyyy hh:mm a" format
    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun navigateToAppropriateActivity() {
        auth.signOut()
        val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)
        googleSignInClient.signOut().addOnCompleteListener {
            val intent = Intent(this, NotThereActivity::class.java)
            startActivity(intent)
            finish() // Finish the current activity
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to sign out from Google", Toast.LENGTH_SHORT).show()
        }
    }

    inner class SmsReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Handle received SMS here
            // For example, you can extract the SMS and take some action if needed
            Toast.makeText(context, "SMS received", Toast.LENGTH_SHORT).show()
        }
    }
}