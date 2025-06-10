package com.example.lockin.util

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.lockin.ui.AdminPasswordActivity

class LockInDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Device Admin enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Device Admin disabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence? {
        // Start AdminPasswordActivity to prompt for password
        val passwordIntent = Intent(context, AdminPasswordActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(passwordIntent)
        return "Enter the admin password to disable LockIn."
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, LockInDeviceAdminReceiver::class.java)
        }
    }
}