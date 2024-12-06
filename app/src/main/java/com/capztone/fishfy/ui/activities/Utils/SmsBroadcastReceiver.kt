package com.capztone.fishfy.ui.activities.Utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever

class SmsBroadcastReceiver : BroadcastReceiver() {

    private var otpReceivedListener: OTPListener? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
            val extras = intent.extras
            val status = extras?.get(SmsRetriever.EXTRA_STATUS) as com.google.android.gms.common.api.Status
            when (status.statusCode) {
                com.google.android.gms.common.api.CommonStatusCodes.SUCCESS -> {
                    val message = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE)
                    if (message != null) {
                        otpReceivedListener?.onOTPReceived(message)
                    }
                }
                com.google.android.gms.common.api.CommonStatusCodes.TIMEOUT -> {
                    otpReceivedListener?.onOTPTimeout()
                }
            }
        }
    }

    fun setOTPListener(listener: OTPListener) {
        this.otpReceivedListener = listener
    }

    interface OTPListener {
        fun onOTPReceived(otp: String)
        fun onOTPTimeout()
    }
}
