package com.example.lockin.viewmodel

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lockin.data.model.LockInUiState
import com.example.lockin.data.preferences.EncryptedPrefsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class LockInViewModel(private val context: Context) : ViewModel() {
    private val _uiStateFlow = MutableStateFlow(LockInUiState())
    val uiStateFlow: StateFlow<LockInUiState> = _uiStateFlow.asStateFlow()
    private val encryptedPrefs = EncryptedPrefsHelper.getEncryptedPrefs(context)

    init {
        loadBlockedApps()
        updateMostUsedApps()
        updateWeeklyBlockCounts()
    }

    fun setBlockedApps(apps: List<String>) {
        encryptedPrefs.edit().putStringSet("blocked_apps", apps.toSet()).apply()
        loadBlockedApps()
    }

    fun incrementBlockCount() {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val weekStart = calendar.apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val key = "block_counts_$weekStart"
        val blockCounts = encryptedPrefs.getString(key, "0,0,0,0,0,0,0")!!.split(",").map { it.toIntOrNull() ?: 0 }.toMutableList()
        blockCounts[dayOfWeek - 1] = blockCounts[dayOfWeek - 1] + 1
        encryptedPrefs.edit().putString(key, blockCounts.joinToString(",")).apply()
        updateWeeklyBlockCounts()
    }

    private fun loadBlockedApps() {
        val blockedApps = encryptedPrefs.getStringSet("blocked_apps", emptySet())?.toList() ?: emptyList()
        _uiStateFlow.value = _uiStateFlow.value.copy(blockedApps = blockedApps)
    }

    private fun updateMostUsedApps() {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = calendar.timeInMillis
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        val packageManager = context.packageManager
        val mostUsedApps = stats
            .filter { it.packageName != context.packageName }
            .sortedByDescending { it.totalTimeInForeground }
            .take(5)
            .mapNotNull { stat ->
                try {
                    val appInfo = packageManager.getApplicationInfo(stat.packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    appName to stat.totalTimeInForeground / 1000 // Convert to seconds
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
        _uiStateFlow.value = _uiStateFlow.value.copy(mostUsedApps = mostUsedApps)
    }

    private fun updateWeeklyBlockCounts() {
        val calendar = Calendar.getInstance()
        val weekStart = calendar.apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val key = "block_counts_$weekStart"
        val blockCounts = encryptedPrefs.getString(key, "0,0,0,0,0,0,0")!!.split(",").map { it.toIntOrNull() ?: 0 }
        _uiStateFlow.value = _uiStateFlow.value.copy(weeklyBlockCounts = blockCounts)
    }
}

