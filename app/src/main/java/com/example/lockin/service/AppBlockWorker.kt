package com.example.lockin.service

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.lockin.data.preferences.EncryptedPrefsHelper

class AppBlockWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val prefs = EncryptedPrefsHelper.getEncryptedPrefs(applicationContext)
        val blockedApps = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
        Log.d("LockIn", "Worker checked blocked apps: $blockedApps")
        return Result.success()
    }
}