package com.example.lockin.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.lockin.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppPickerDialog(
    onAppsSelected: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val selectedApps = remember { mutableStateListOf<String>() }
    var appList by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val apps = withContext(Dispatchers.IO) {
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { appInfo ->
                    appInfo.packageName != context.packageName &&
                            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
                }
                .map { appInfo ->
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    AppInfo(appInfo.packageName, appName)
                }
                .sortedBy { it.appName }
            installedApps
        }
        appList = apps
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Apps to Block") },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(appList) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = app.packageName in selectedApps,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedApps.add(app.packageName)
                                    } else {
                                        selectedApps.remove(app.packageName)
                                    }
                                }
                            )
                            Text(app.appName, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAppsSelected(selectedApps) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
