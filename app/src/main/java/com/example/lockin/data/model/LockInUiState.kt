package com.example.lockin.data.model

data class LockInUiState(
    val blockedApps: List<String> = emptyList(),
    val usageStats: String = "0 minutes"
)