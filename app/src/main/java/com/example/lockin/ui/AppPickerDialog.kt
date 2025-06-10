package com.example.lockin.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lockin.data.model.AppInfo
import com.example.lockin.data.preferences.EncryptedPrefsHelper
import com.example.lockin.util.PasswordManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppPickerDialog(
    onAppsSelected: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    var selectedApps by remember { mutableStateOf(listOf<String>()) }
    var appList by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var appToUnblock by remember { mutableStateOf<String?>(null) }
    val blockedApps = remember { mutableStateOf(setOf<String>()) }

    // Load blocked apps from EncryptedSharedPreferences
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val encryptedPrefs = EncryptedPrefsHelper.getEncryptedPrefs(context)
            blockedApps.value = encryptedPrefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
            val apps = packageManager.getInstalledPackages(0)
                .filter { pkgInfo ->
                    pkgInfo.packageName != context.packageName &&
                            pkgInfo.applicationInfo != null &&
                            (pkgInfo.applicationInfo!!.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0)
                }
                .mapNotNull { pkgInfo ->
                    pkgInfo.applicationInfo?.let { appInfo ->
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        AppInfo(pkgInfo.packageName, appName)
                    }
                }
                .sortedBy { it.appName }
            appList = apps
            selectedApps = blockedApps.value.toList()
            isLoading = false
        }
    }

    if (showPasswordDialog && appToUnblock != null) {
        PasswordDialog(
            onPasswordEntered = { password ->
                if (PasswordManager.verifyPassword(password)) {
                    selectedApps = selectedApps - appToUnblock!!
                    appToUnblock = null
                    showPasswordDialog = false
                } else {
                    Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show()
                    showPasswordDialog = false
                }
            },
            onDismiss = { showPasswordDialog = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Apps to Block") },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
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
                    items(appList.size) { index ->
                        val app = appList[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = app.packageName in selectedApps,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        selectedApps = selectedApps + app.packageName
                                    } else if (app.packageName in blockedApps.value) {
                                        appToUnblock = app.packageName
                                        showPasswordDialog = true
                                    } else {
                                        selectedApps = selectedApps - app.packageName
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