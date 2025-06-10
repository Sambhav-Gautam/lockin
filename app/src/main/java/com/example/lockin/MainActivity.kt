package com.example.lockin


import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.lockin.ui.theme.LockinTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val accessibilityPermissionRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        if (isAccessibilityServiceEnabled()) {
            Log.d("LockIn", "Accessibility service enabled")
            Toast.makeText(this, "Accessibility service enabled", Toast.LENGTH_SHORT).show()
            startService(Intent(this, AppBlockService::class.java))
        } else {
            Log.w("LockIn", "Accessibility service not enabled")
            Toast.makeText(this, "Please enable accessibility service to block apps", Toast.LENGTH_LONG).show()
        }
    }

    private val usageStatsPermissionRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        if (mode != AppOpsManager.MODE_ALLOWED) {
            Toast.makeText(this, "Usage stats permission required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request usage stats permission
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        if (mode != AppOpsManager.MODE_ALLOWED) {
            usageStatsPermissionRequest.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        // Request accessibility service permission
        if (!isAccessibilityServiceEnabled()) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            accessibilityPermissionRequest.launch(intent)
        } else {
            startService(Intent(this, AppBlockService::class.java))
        }

        // Schedule work for app blocking
        val workRequest = PeriodicWorkRequestBuilder<AppBlockWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueue(workRequest)

        setContent {
            LockinTheme {
                LockInApp()
            }
        }
    }

    @SuppressLint("ServiceCast")
    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = ComponentName(this, AppBlockAccessibilityService::class.java)
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { serviceInfo: AccessibilityServiceInfo -> serviceInfo.id == service.flattenToString() }
    }
}

@Composable
fun LockInApp(viewModel: LockInViewModel = viewModel(
    factory = LockInViewModelFactory(LocalContext.current.applicationContext)
)) {
    MainScreen(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: LockInViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
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

data class AppInfo(val packageName: String, val appName: String)

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

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val apps = packageManager.getInstalledPackages(0)
                .filter { pkgInfo ->
                    pkgInfo.packageName != context.packageName &&
                            pkgInfo.applicationInfo != null &&
                            (pkgInfo.applicationInfo?.flags?.and(android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0)
                }
                .mapNotNull { pkgInfo ->
                    pkgInfo.applicationInfo?.let { appInfo ->
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        AppInfo(pkgInfo.packageName, appName)
                    }
                }
                .sortedBy { it.appName }
            appList = apps
            isLoading = false
        }
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
                    items(appList) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = app.packageName in selectedApps,
                                onCheckedChange = {
                                    selectedApps = if (it) {
                                        selectedApps + app.packageName
                                    } else {
                                        selectedApps - app.packageName
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

class LockInViewModel(context: Context) : ViewModel() {
    private val applicationContext = context.applicationContext
    private val _uiState = MutableStateFlow(LockInUiState())
    val uiState: StateFlow<LockInUiState> = _uiState
    private lateinit var encryptedPrefs: EncryptedSharedPreferences

    init {
        initializePrefs()
    }

    private fun initializePrefs() {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            encryptedPrefs = EncryptedSharedPreferences.create(
                "lockin_prefs",
                masterKeyAlias,
                applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences
            _uiState.value = _uiState.value.copy(
                blockedApps = encryptedPrefs.getStringSet("blocked_apps", emptySet())?.toList() ?: emptyList(),
                usageStats = "0 minutes"
            )
        } catch (e: Exception) {
            Log.e("LockIn", "Failed to initialize encrypted preferences", e)
        }
    }

    fun setBlockedApps(apps: List<String>) {
        encryptedPrefs.edit().putStringSet("blocked_apps", apps.toSet()).apply()
        _uiState.value = _uiState.value.copy(blockedApps = apps)
    }

    fun updateUsageStats(context: Context) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            System.currentTimeMillis() - 24 * 60 * 60 * 1000,
            System.currentTimeMillis()
        )
        val totalTime = stats.sumOf { it.totalTimeInForeground } / 1000 / 60 // in minutes
        _uiState.value = _uiState.value.copy(usageStats = "$totalTime minutes")
    }
}

class LockInViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LockInViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LockInViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class LockInUiState(
    val blockedApps: List<String> = emptyList(),
    val usageStats: String = "0 minutes"
)

class AppBlockWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val prefs = EncryptedSharedPreferences.create(
            "lockin_prefs",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val blockedApps = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
        Log.d("LockIn", "Worker checked blocked apps: $blockedApps")
        return Result.success()
    }
}

class AppBlockAccessibilityService : AccessibilityService() {
    private lateinit var encryptedPrefs: EncryptedSharedPreferences
    private var blockedApps = setOf<String>()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        initializePrefs()
    }

    private fun initializePrefs() {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            encryptedPrefs = EncryptedSharedPreferences.create(
                "lockin_prefs",
                masterKeyAlias,
                applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences
            blockedApps = encryptedPrefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
        } catch (e: Exception) {
            Log.e("LockIn", "Failed to initialize encrypted preferences", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.packageName != null) {
            val packageName = event.packageName.toString()
            blockedApps = encryptedPrefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
            if (packageName in blockedApps && packageName != applicationContext.packageName) {
                Log.d("LockIn", "Blocked app detected: $packageName")
                mainHandler.post {
                    Toast.makeText(applicationContext, "App Blocked: Please focus on your work or studies", Toast.LENGTH_SHORT).show()
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(homeIntent)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d("LockIn", "Accessibility service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        setServiceInfo(info)
        Log.d("LockIn", "Accessibility service connected")
    }
}

class AppBlockService : Service() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(15, createNotificationChannel())
    }

    private fun createNotificationChannel(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "lockin_service_channel",
                "LockIn Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, "lockin_service_channel")
            .setContentTitle("LockIn Running")
            .setContentText("Monitoring and blocking selected apps")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    override fun onBind(intent: Intent?): android.os.IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}