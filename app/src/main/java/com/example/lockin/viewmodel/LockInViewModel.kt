package com.example.lockin.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.security.crypto.EncryptedSharedPreferences
import com.example.lockin.data.model.LockInUiState
import com.example.lockin.data.preferences.EncryptedPrefsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LockInViewModel(context: Context) : ViewModel() {
    private val applicationContext = context.applicationContext
    private val _uiState = MutableStateFlow(LockInUiState())
    val uiStateFlow: StateFlow<LockInUiState> = _uiState
    private lateinit var encryptedPrefs: EncryptedSharedPreferences

    init {
        initializePrefs()
    }

    private fun initializePrefs() {
        try {
            encryptedPrefs = EncryptedPrefsHelper.getEncryptedPrefs(applicationContext)
            _uiState.value = _uiState.value.copy(
                blockedApps = encryptedPrefs.getStringSet("blocked_apps", emptySet())?.toList() ?: emptyList(),
                usageStats = "0 minutes"
            )
        } catch (e: Exception) {
            Log.e("LockIn", "Failed to initialize encrypted preferences", e)
            // Initialize encryptedPrefs with a fallback to prevent uninitialized access
            encryptedPrefs = EncryptedPrefsHelper.getEncryptedPrefs(applicationContext)
            _uiState.value = _uiState.value.copy(
                blockedApps = emptyList(),
                usageStats = "0 minutes"
            )
        }
    }

    fun setBlockedApps(apps: List<String>) {
        try {
            encryptedPrefs.edit().putStringSet("blocked_apps", apps.toSet()).apply()
            _uiState.value = _uiState.value.copy(blockedApps = apps)
        } catch (e: Exception) {
            Log.e("LockIn", "Failed to save blocked apps to preferences", e)
        }
    }
}