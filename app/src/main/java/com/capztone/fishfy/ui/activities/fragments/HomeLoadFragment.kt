package com.capztone.fishfy.ui.activities.fragments

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.FragmentHomeLoadBinding

class HomeLoadFragment : Fragment() {

    private var _binding: FragmentHomeLoadBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeLoadBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.progress.visibility = View.VISIBLE

        binding.progress.setProgressVector(resources.getDrawable(R.drawable.spinload))
        binding.progress.setTextViewVisibility(true)
        binding.progress.setTextStyle(true)
        binding.progress.setTextColor(Color.YELLOW)
        binding.progress.setTextSize(12F)
        binding.progress.setTextMsg("Please Wait")
        binding.progress.setEnlarge(5)

        // Start a delay to hide the loading indicator after 1500 milliseconds (1.5 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progress.visibility = View.GONE

            // Navigate to HomeFragment
            findNavController().navigate(R.id.action_homeloadfragemnt_to_homefragment)

        }, 100)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
