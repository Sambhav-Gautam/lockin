package com.example.lockin.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.lockin.data.preferences.EncryptedPrefsHelper
import com.example.lockin.ui.MotivatingImageActivity

class AppBlockAccessibilityService : AccessibilityService() {
    private lateinit var encryptedPrefs: androidx.security.crypto.EncryptedSharedPreferences
    private var blockedApps = setOf<String>()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        initializePrefs()
    }

    private fun initializePrefs() {
        try {
            encryptedPrefs = EncryptedPrefsHelper.getEncryptedPrefs(this)
            blockedApps = encryptedPrefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
        } catch (e: Exception) {
            Log.e("LockIn", "Failed to initialize encrypted preferences", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.packageName != null) {
            val packageName = event.packageName.toString()
            blockedApps = encryptedPrefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
            if (packageName in blockedApps && packageName != applicationContext.packageName) {
                Log.d("LockIn", "Blocked app detected: $packageName")
                mainHandler.post {
                    val intent = Intent(this@AppBlockAccessibilityService, MotivatingImageActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(intent)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d("LockIn", "Accessibility service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        setServiceInfo(info)
        Log.d("LockIn", "Accessibility service connected")
    }
}