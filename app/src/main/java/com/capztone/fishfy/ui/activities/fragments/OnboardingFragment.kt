package com.capztone.fishfy.ui.activities.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.datastore.core.DataStore

import androidx.viewpager2.widget.ViewPager2
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.FragmentOnboardingBinding
import com.capztone.fishfy.ui.activities.LoginActivity
import com.capztone.fishfy.ui.activities.adapters.IntroSliderAdapter
import com.capztone.fishfy.ui.activities.models.IntroSlide
import java.util.prefs.Preferences
import javax.inject.Inject


class OnboardingFragment : Fragment() {
    private var binding: FragmentOnboardingBinding? = null

    @Inject
    lateinit var prefs: DataStore<Preferences>

    //the items are added to the adapter
    private val introSliderAdapter = IntroSliderAdapter(
        listOf(
            IntroSlide(
                "Welcome to Fishfy!!!",
                " Dive into the freshest catch with Fisfy! We're here to make your seafood cravings a delightful experience. Let's get started!",
                "fisfy.jpg"
            ),
            IntroSlide(
                "Choose Your Favorites",
                "Tell us what you love! Select your favorite types of seafood to tailor your Fishfy experience.",
                "favfood.jpg"
            ),
            IntroSlide(
                "Doorstep Delivery",
                " Enjoy the convenience of doorstep delivery! Simply place your order and have your fresh seafood delivered right to your doorstep at your preferred time.",
                "deliboy.jpg"
            )
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentOnboardingBinding.inflate(layoutInflater)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//set the adapter to the viewpager2
        binding?.viewPager?.adapter = introSliderAdapter
//sets the viewpager2 to the indicator
        binding?.indicator?.setViewPager(binding?.viewPager)

        binding?.viewPager?.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {

                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    /*
                    *check if its the last page, change text on the button
                    *from next to finish and set the click listener to
                    *to navigate to welcome screen else set the text to next
                    * and click listener to move to next page
                    */
                    if (position == introSliderAdapter.itemCount - 1) {
//this animation is added to the finish button
                        val animation = AnimationUtils.loadAnimation(
                            requireActivity(),
                            R.anim.app_name_animation
                        )
                        binding?.buttonNext?.animation = animation
                        binding?.buttonNext?.text = "Get Started"
                        binding?.buttonNext?.setOnClickListener {
                            val intent = Intent(requireContext(), LoginActivity::class.java)
                            startActivity(intent)

                        }
                    } else {
                        binding?.buttonNext?.text = "Next"
                        binding?.buttonNext?.setOnClickListener {
                            binding?.viewPager?.currentItem?.let {
                                binding?.viewPager?.setCurrentItem(it + 1, false)
                            }
                        }
                    }
                }
            })
    }

}

//suspend function to save the onboarding to datastore


//suspend function to save the onboarding to datastore



