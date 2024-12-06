package com.capztone.fishfy.ui.activities.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.capztone.fishfy.databinding.FragmentCongratsBottomSheetBinding
import com.capztone.fishfy.ui.activities.MainActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.DatabaseReference

class CongratsBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentCongratsBottomSheetBinding
    private lateinit var databaseReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCongratsBottomSheetBinding.inflate(inflater, container, false)
        binding.imageView4.setAnimation("Animation.json")

        // Play animation
        binding.imageView4.playAnimation()

        // Delay for 4 seconds before navigating to MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(activity, MainActivity::class.java)
            startActivity(intent)
            activity?.finish() // Optional: call finish() if you want to close the current activity
        }, 2000) // 4000 milliseconds = 3 seconds

        return binding.root
    }
}