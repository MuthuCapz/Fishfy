package com.capztone.fishfy.ui.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import com.capztone.fishfy.R
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.capztone.fishfy.databinding.ActivityProfileLanguageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class ProfileLanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileLanguageBinding
    private lateinit var userLanguageRef: DatabaseReference
    private lateinit var userId: String
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("LanguagePreferences", Context.MODE_PRIVATE)

        // Firebase database reference for user language preference
        val currentUser = FirebaseAuth.getInstance().currentUser
        userId = currentUser?.uid ?: ""

        userLanguageRef = FirebaseDatabase.getInstance().getReference("user").child(userId).child("language")

        // Populate the spinner with languages using a custom ArrayAdapter
        val languages = resources.getStringArray(R.array.languages_array)
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, languages) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(resources.getColor(R.color.black))
                textView.typeface = ResourcesCompat.getFont(this@ProfileLanguageActivity, R.font.nunito) // Set Nunito font
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f) // Set text size to 12sp
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(resources.getColor(R.color.black))
                textView.setBackgroundColor(resources.getColor(R.color.white))
                textView.typeface = ResourcesCompat.getFont(this@ProfileLanguageActivity, R.font.nunito) // Set Nunito font
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f) // Set text size to 12sp
                return view
            }
        }

        // Set the adapter to the spinner
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.languageSpinner.adapter = adapter

        // Retrieve and set language preference from SharedPreferences
        val savedLanguage = sharedPreferences.getString("language", null)
        if (savedLanguage != null) {
            setLocale(savedLanguage)
            updateSpinner(savedLanguage, languages)
        } else {
            // Retrieve and set language preference from Firebase
            userLanguageRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val languageCode = snapshot.getValue(String::class.java)
                    if (languageCode != null) {
                        setLocale(languageCode)
                        updateSpinner(languageCode, languages)
                    } else {
                        val defaultLanguage = "ta"  // Tamil language code
                        setLocale(defaultLanguage)
                        updateSpinner(defaultLanguage, languages)
                        updateLanguagePreference(defaultLanguage)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }

        // Spinner item selected listener to change language preference
        binding.languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            var isFirstSelection = true // Flag to ignore the first automatic call

            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (isFirstSelection) {
                    isFirstSelection = false
                    return
                }
                val selectedLanguage = languages[position].lowercase(Locale.ROOT)
                updateLanguagePreference(selectedLanguage)
                setLocale(selectedLanguage)
                // Save language preference to SharedPreferences
                saveLanguagePreference(selectedLanguage)
                // Navigate to MainActivity
                startActivity(Intent(this@ProfileLanguageActivity, MainActivity::class.java))
                finish()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    private fun updateLanguagePreference(languageCode: String) {
        userLanguageRef.setValue(languageCode)
    }

    private fun setLocale(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun updateSpinner(languageCode: String, languages: Array<String>) {
        val position = languages.indexOfFirst { it.equals(languageCode, ignoreCase = true) }
        if (position >= 0) {
            binding.languageSpinner.setSelection(position)
        }
    }

    private fun saveLanguagePreference(languageCode: String) {
        val editor = sharedPreferences.edit()
        editor.putString("language", languageCode)
        editor.apply()
    }
}
