package com.example.lockin

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.lockin.service.AppBlockService
import com.example.lockin.service.AppBlockWorker
import com.example.lockin.ui.LockInApp
import com.example.lockin.ui.theme.LockinTheme
import com.example.lockin.util.LockInDeviceAdminReceiver
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val accessibilityPermissionRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        if (isAccessibilityServiceEnabled()) {
            Log.d("LockIn", "Accessibility service enabled")
            Toast.makeText(this, "Accessibility service enabled", Toast.LENGTH_SHORT).show()
            startService(Intent(this, AppBlockService::class.java))
            // No need to redirect here; handled in AppBlockAccessibilityService
        } else {
            Log.w("LockIn", "Accessibility service not enabled")
            Toast.makeText(this, "Please enable accessibility service to block apps", Toast.LENGTH_LONG).show()
        }
    }

    private val usageStatsPermissionRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        if (mode != AppOpsManager.MODE_ALLOWED) {
            Toast.makeText(this, "Usage stats permission required", Toast.LENGTH_LONG).show()
        }
    }

    private val deviceAdminRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, LockInDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(adminComponent)) {
            Log.d("LockIn", "Device admin enabled")
            Toast.makeText(this, "Device admin enabled", Toast.LENGTH_SHORT).show()
        } else {
            Log.w("LockIn", "Device admin not enabled")
            Toast.makeText(this, "Please enable device admin to lock screen", Toast.LENGTH_LONG).show()
        }
    }

    private val batteryOptimizationRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
            Log.d("LockIn", "Battery optimization disabled")
            Toast.makeText(this, "Battery optimization disabled", Toast.LENGTH_SHORT).show()
        } else {
            Log.w("LockIn", "Battery optimization not disabled")
            Toast.makeText(this, "Please disable battery optimization for reliable performance", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request usage stats permission
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        if (mode != AppOpsManager.MODE_ALLOWED) {
            usageStatsPermissionRequest.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        // Request accessibility service permission
        if (!isAccessibilityServiceEnabled()) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            accessibilityPermissionRequest.launch(intent)
        } else {
            startService(Intent(this, AppBlockService::class.java))
        }

        // Request device admin permission
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, LockInDeviceAdminReceiver::class.java)
        if (!dpm.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable device admin to lock the screen and protect the app.")
            }
            deviceAdminRequest.launch(intent)
        }

        // Request battery optimization exemption
        val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            batteryOptimizationRequest.launch(intent)
        }

        setContent {
            LockinTheme {
                LockInApp()
            }
        }

        // Schedule work for app blocking
        val workRequest = PeriodicWorkRequestBuilder<AppBlockWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = ComponentName(this, com.example.lockin.service.AppBlockAccessibilityService::class.java)
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.id == service.flattenToString() }
    }
}