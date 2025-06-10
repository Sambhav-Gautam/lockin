package com.example.lockin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lockin.viewmodel.LockInViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: LockInViewModel) {
    val uiState by viewModel.uiStateFlow.collectAsState()
    var showAppPicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("LockIn") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Select Apps to Block", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showAppPicker = true }) {
                Text("Choose Apps")
            }
            Text("Selected Apps: ${uiState.blockedApps.joinToString()}")
            Text("Usage Stats", style = MaterialTheme.typography.titleMedium)
            Text("Total time today: ${uiState.usageStats}")
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            onAppsSelected = { selectedApps ->
                viewModel.setBlockedApps(selectedApps)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }
}