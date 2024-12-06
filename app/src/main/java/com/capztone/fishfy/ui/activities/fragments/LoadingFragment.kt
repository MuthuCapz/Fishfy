package com.capztone.fishfy.ui.activities.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.capztone.fishfy.R

class LoadingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_loading, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Post delayed handler to dismiss fragment after 1000 milliseconds
        Handler(Looper.getMainLooper()).postDelayed({
            activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
        }, 1000)
    }
}
