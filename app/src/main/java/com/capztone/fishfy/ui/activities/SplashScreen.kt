package com.capztone.fishfy.ui.activities

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.capztone.admin.utils.FirebaseAuthUtil
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.ActivitySplashScreenBinding
import com.google.firebase.auth.FirebaseAuth

class SplashScreen : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var fishViews: List<ImageView>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
         auth = FirebaseAuthUtil.auth



// Translate animation to move image from top to center
        val translateAnimation = TranslateAnimation(0f, 500f, 500f, 0f)
        translateAnimation.duration = 1000 // Set duration as needed

// Rotate and zoom animations
        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_animation)
        val zoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in_animation)

// Animate simultaneously
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(translateAnimation)
        animationSet.addAnimation(rotateAnimation)


        binding.iconTxt.startAnimation(zoomInAnimation)

        // Fish animation
        fishViews = listOf(
            binding.one, binding.two, binding.three, binding.four,
            binding.five, binding.six, binding.seven, binding.eight,
            binding.nine, binding.ten, binding.eleven,  binding.twelve, binding.thirteen, binding.fourteen, binding.fifteen,
            binding.sixteen, binding.seventeen, binding.eighteen, binding.nineteen,
            binding.twenty, binding.twentyone, binding.twentytwo,binding.twentythree, binding.twentyfour, binding.twentyfive
        )

        startFishAnimation()

        Handler().postDelayed({
            if (auth.currentUser != null) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, StartActivity::class.java))
            }
            finish()
        }, 3300) // Adjust delay as needed
    }



    private fun startFishAnimation() {
        val translationDistance = 2000f // Adjust this value as needed
        val duration = 5000L // Adjust this value for slower motion

        // Create ObjectAnimators for translationX property for each ImageView
        val animators: List<Animator> = fishViews.mapIndexed { index, imageView ->
            val translationX = if (index % 2 == 0) translationDistance else -translationDistance
            ObjectAnimator.ofFloat(imageView, "translationX", translationX).apply {
                this.duration = duration
                this.interpolator = AccelerateDecelerateInterpolator()
            }
        }

        // Start the animation
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animators)
        animatorSet.start()

        // Hide the fish views after the animation completes
        Handler().postDelayed({
            fishViews.forEach { it.isVisible = false }
        }, duration)
    }
    // Implement this method to check if the user is logged in
}
