package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.service.BatteryOptimizationWorker
import com.example.ui.AuthViewModel
import com.example.ui.DashboardScreen
import com.example.ui.DashboardViewModel
import com.example.ui.theme.FloodAlertTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    private val dashboardViewModel: DashboardViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(applicationContext) as T
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermission()

        // Check for battery optimization intent extra on start
        if (intent?.getBooleanExtra("LAUNCH_BATTERY_WHITELIST_DIALOG", false) == true) {
            dashboardViewModel.triggerBatteryDialog(true)
        }

        // Setup periodic battery optimization checks
        val batteryWorkRequest = PeriodicWorkRequestBuilder<BatteryOptimizationWorker>(
            1, TimeUnit.DAYS
        ).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "BatteryOptimizationCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            batteryWorkRequest
        )

        setContent {
            FloodAlertTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("LAUNCH_BATTERY_WHITELIST_DIALOG", false) == true) {
            dashboardViewModel.triggerBatteryDialog(true)
        }
    }

    override fun onResume() {
        super.onResume()
        dashboardViewModel.refreshServiceStatus()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
