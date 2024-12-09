package com.capztone.fishfy.ui.activities

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.capztone.fishfy.ui.activities.Utils.updateUserStatus

class AppLifecycleObserver : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onEnterForeground() {
        updateUserStatus("active")  // Set status to active
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onEnterBackground() {
        updateUserStatus("inactive")  // Set status to inactive
    }
}

