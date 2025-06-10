package com.example.lockin.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.lockin.data.preferences.EncryptedPrefsHelper
import com.example.lockin.ui.MotivatingImageActivity
import com.example.lockin.viewmodel.LockInViewModel

class AppBlockAccessibilityService : AccessibilityService() {
    private lateinit var encryptedPrefs: androidx.security.crypto.EncryptedSharedPreferences
    private var blockedApps = setOf<String>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var viewModel: LockInViewModel

    override fun onCreate() {
        super.onCreate()
        initializePrefs()
        viewModel = LockInViewModel(this)
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

            // Block attempts to access app or device admin settings
            if (packageName == "com.android.settings" &&
                (event.className?.contains("AppInfo") == true ||
                        event.className?.contains("DeviceAdmin") == true)) {
                Log.d("LockIn", "Detected attempt to access app or device admin settings: $packageName")
                mainHandler.post {
                    val intent = Intent(this@AppBlockAccessibilityService, MotivatingImageActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(intent)
                }
                return
            }

            // Block access to blocked apps
            if (packageName in blockedApps && packageName != applicationContext.packageName) {
                Log.d("LockIn", "Blocked app detected: $packageName")
                viewModel.incrementBlockCount() // Increment block count
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

        // Redirect to MainActivity after enabling accessibility
        val intent = Intent(this, com.example.lockin.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }
}