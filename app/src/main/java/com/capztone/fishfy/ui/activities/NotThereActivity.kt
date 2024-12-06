package com.capztone.fishfy.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.capztone.fishfy.databinding.ActivityNotThereBinding
import com.capztone.fishfy.ui.activities.LoginActivity

class NotThereActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotThereBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNotThereBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.another.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    // Override back button to navigate to LoginActivity
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish() // Call finish() to remove this activity from the back stack
    }
}
