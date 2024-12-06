package com.capztone.fishfy.ui.activities.Utils

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.capztone.fishfy.R

object ToastHelper {
    fun showCustomToast(context: Context, message: String) {
        val layout = LayoutInflater.from(context).inflate(R.layout.toast_layout, null)
        val text = layout.findViewById<TextView>(R.id.text)
        text.text = message

        val toast = Toast(context)
        toast.setGravity(Gravity.BOTTOM, 0, 100)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }
}
