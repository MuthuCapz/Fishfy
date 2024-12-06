package com.capztone.fishfy.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import com.capztone.fishfy.databinding.ActivityLocationNotAvailableBinding

class LocationNotAvailable : AppCompatActivity() {
    private lateinit var binding: ActivityLocationNotAvailableBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLocationNotAvailableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.another.setOnClickListener {
            val intent = Intent(this, LocationActivity::class.java)
            startActivity(intent)
        }
    }
}
