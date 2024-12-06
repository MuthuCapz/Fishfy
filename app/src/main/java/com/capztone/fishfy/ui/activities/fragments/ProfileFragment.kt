package com.capztone.fishfy.ui.activities.fragments

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.AccountDeleteDialogBinding
import com.capztone.fishfy.databinding.DialogDeleteConfirmationBinding
import com.capztone.fishfy.databinding.FragmentProfileBinding
import com.capztone.fishfy.ui.activities.DeleteAccountActivity
import com.capztone.fishfy.ui.activities.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient


    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        auth = Firebase.auth

        binding.back.setOnClickListener {
            requireActivity().onBackPressed()
        }
        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_Id)) // Replace with your web client ID
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set OnClickListener on the profile container layout
        binding.containerProfile.setOnClickListener {
            // Navigate to the profile fragment
            findNavController().navigate(R.id.action_profileFragment_to_myProfileFragment)
        }
        binding.containerOrders.setOnClickListener {
            // Navigate to the profile fragment
            findNavController().navigate(R.id.action_profileFragment_to_myorders)
        }
        binding.containerLanguage.setOnClickListener {
            // Navigate to the profile fragment
            findNavController().navigate(R.id.action_profileFragment_to_profileLanguage)
        }
        binding.containerAboutus.setOnClickListener {
            // Navigate to the profile fragment
            findNavController().navigate(R.id.action_profileFragment_to_about)
        }
        binding.containerAddress.setOnClickListener {
            // Navigate to the profile fragment
            findNavController().navigate(R.id.action_profileFragment_to_AddressFragment)
        }
        binding.containerLogout.setOnClickListener {
            // Show confirmation dialog
            showLogoutConfirmationDialog()
        }
        binding.containerDelete.setOnClickListener {

            // Show confirmation dialog
            val intent = Intent(requireContext(), DeleteAccountActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showDeleteConfirmationDialog() {
        val context = binding.root.context
        val dialogBinding =  AccountDeleteDialogBinding.inflate(LayoutInflater.from(context))

        val dialog = AlertDialog.Builder(context)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set title and message
        dialogBinding.tvDialogTitle.text = "Delete Confirmation"
        dialogBinding.tvDialogMessage.text = "Are you sure you want to delete your account?"

        // Button actions
        dialogBinding.btnDialogNo.setOnClickListener {
            dialog.dismiss() // Dismiss the dialog if "No" is clicked
        }

        dialogBinding.btnDialogYes.setOnClickListener {
            // Create an Intent to open the URL
            val url = "https://www.capztone.com/privacy-policy.html"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            context.startActivity(intent)

            dialog.dismiss() // Dismiss the dialog after opening the URL
        }

        dialog.show()
    }

    private fun showLogoutConfirmationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_logout_confirmation, null)
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)

        val alertDialog = dialogBuilder.create()

        dialogView.findViewById<View>(R.id.btnDialogYes).setOnClickListener {
            // Perform logout action
            performLogout()
            alertDialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnDialogNo).setOnClickListener {
            // Dismiss the dialog
            alertDialog.dismiss()
        }

        alertDialog.show()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
    private fun performLogout() {
        // Firebase sign out
        auth.signOut()

        // Google sign out
        googleSignInClient.signOut().addOnCompleteListener(requireActivity()) {
            // Navigate to login activity
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}