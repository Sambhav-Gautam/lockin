package com.example.lockin.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.lockin.ui.theme.LockinTheme
import com.example.lockin.util.LockInDeviceAdminReceiver
import com.example.lockin.util.PasswordManager

class AdminPasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LockinTheme {
                AdminPasswordScreen(
                    onPasswordVerified = {
                        // Allow deactivation by finishing the activity
                        finish()
                    },
                    onCancel = {
                        // Lock screen and finish
                        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
                        val adminComponent = LockInDeviceAdminReceiver.getComponentName(this)
                        if (dpm.isAdminActive(adminComponent)) {
                            dpm.lockNow()
                        }
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun AdminPasswordScreen(
    onPasswordVerified: () -> Unit,
    onCancel: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Enter Admin Password",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                isError = errorMessage != null,
                modifier = Modifier.fillMaxWidth()
            )
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onCancel) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (PasswordManager.verifyPassword(password)) {
                            onPasswordVerified()
                        } else {
                            errorMessage = "Incorrect password"
                        }
                    }
                ) {
                    Text("Submit")
                }
            }
        }
    }
}