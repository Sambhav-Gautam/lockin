package com.example.lockin.data.model

data class LockInUiState(
    val blockedApps: List<String> = emptyList(),
    val mostUsedApps: List<Pair<String, Long>> = emptyList(),
    val weeklyBlockCounts: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0)
)