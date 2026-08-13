package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppNodeMode
import com.example.data.FloodAlert
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleBlueDark
import com.example.ui.theme.MaterialRed
import com.example.ui.theme.SlateGrey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val villageName by viewModel.villageName.collectAsState()
    val nodeMode by viewModel.nodeMode.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val liveDelta by viewModel.liveDelta.collectAsState()
    val liveDurationMs by viewModel.liveDurationMs.collectAsState()
    val threshold by viewModel.vibrationThreshold.collectAsState()
    val requiredDurationMs by viewModel.durationRequirementMs.collectAsState()
    val deviceName by viewModel.deviceName.collectAsState()
    val locationName by viewModel.locationName.collectAsState()
    val recentAlerts by viewModel.recentAlerts.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    val showBatteryDialogFromVm by viewModel.showBatteryDialog.collectAsState()

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = { name, village, password ->
                viewModel.loginUser(name, village, password)
            }
        )
        return
    }

    LaunchedEffect(Unit) {
        viewModel.refreshServiceStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Flood Alert System",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isServiceRunning || nodeMode == AppNodeMode.CITIZEN_NODE)
                                            Color(0xFF34A853) else SlateGrey,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (nodeMode == AppNodeMode.SENSOR_UNIT) {
                                    if (isServiceRunning) "Sensor Active • Monitoring" else "Sensor Disarmed"
                                } else "Receiver Node • FCM Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Calibration Settings"
                        )
                    }
                    IconButton(
                        onClick = { viewModel.logoutUser() },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "User Profile ($userName, $villageName)"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // User Profile Banner
            item {
                UserProfileCard(
                    userName = userName,
                    villageName = villageName,
                    onLogout = { viewModel.logoutUser() }
                )
            }

            // 1. Connection & Mode Status Banner
            item {
                ConnectionStatusCard(
                    nodeMode = nodeMode,
                    isServiceRunning = isServiceRunning,
                    authState = authState,
                    onSignInClicked = {
                        // Launch Google Sign-In helper
                        authViewModel.signInWithGoogle(context, "123456789-example.apps.googleusercontent.com")
                    }
                )
            }

            // 2. Role / Mode Switcher Card
            item {
                ModeSwitcherCard(
                    currentMode = nodeMode,
                    onModeSelected = { selectedMode ->
                        viewModel.setNodeMode(selectedMode, context)
                    }
                )
            }

            // 3. Mode-Specific Interactive Section
            if (nodeMode == AppNodeMode.SENSOR_UNIT) {
                // SENSOR / LISTENER NODE UI
                item {
                    SensorArmDisarmCard(
                        isMonitoring = isServiceRunning,
                        deviceName = deviceName,
                        locationName = locationName,
                        onToggleMonitoring = {
                            viewModel.toggleMonitoring(context)
                        },
                        onRequestBatteryOptimization = {
                            viewModel.triggerBatteryDialog(true)
                        }
                    )
                }

                item {
                    LiveVibrationMeterCard(
                        currentDelta = liveDelta,
                        threshold = threshold,
                        currentDurationMs = liveDurationMs,
                        requiredDurationMs = requiredDurationMs
                    )
                }
            } else {
                // RECEIVER / CITIZEN NODE UI
                item {
                    ReceiverNodeStatusCard()
                }
            }

            // 4. Manual Emergency Test Trigger
            item {
                ManualTestCard(
                    onTestTrigger = {
                        viewModel.triggerTestAlarm(context)
                    }
                )
            }

            // 5. Recent Alerts History Log
            item {
                Text(
                    text = "Recent Flood Alerts Log",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (recentAlerts.isEmpty()) {
                item {
                    EmptyAlertsCard()
                }
            } else {
                items(recentAlerts, key = { it.id.ifEmpty { it.timestamp.toString() } }) { alert ->
                    AlertLogItemCard(alert = alert)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showSettingsDialog) {
        CalibrationSettingsDialog(
            currentThreshold = threshold,
            currentDurationMs = requiredDurationMs,
            currentDeviceName = deviceName,
            currentLocationName = locationName,
            onDismiss = { showSettingsDialog = false },
            onSave = { newThreshold, newDurationMs, newName, newLocation ->
                viewModel.updateThreshold(newThreshold)
                viewModel.updateDurationMs(newDurationMs)
                viewModel.updateDeviceDetails(newName, newLocation)
                showSettingsDialog = false
            }
        )
    }

    if (showBatteryDialogFromVm) {
        BatteryOptimizationExplanationDialog(
            onDismiss = { viewModel.triggerBatteryDialog(false) },
            onConfirm = {
                viewModel.triggerBatteryDialog(false)
                requestIgnoreBatteryOptimization(context)
            }
        )
    }
}

@Composable
fun ConnectionStatusCard(
    nodeMode: AppNodeMode,
    isServiceRunning: Boolean,
    authState: AuthState,
    onSignInClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = null,
                tint = GoogleBlue,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Firebase Firestore Connected",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (authState) {
                        is AuthState.Authenticated -> "Signed in as ${authState.user.email ?: "Authorized Node"}"
                        is AuthState.Loading -> "Authenticating with Google..."
                        else -> "Topic: 'flood_alerts' • Real-time cloud sync ready"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ModeSwitcherCard(
    currentMode: AppNodeMode,
    onModeSelected: (AppNodeMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "System Node Role",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Select whether this phone monitors physical vibration or receives alerts",
                style = MaterialTheme.typography.bodySmall,
                color = SlateGrey
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mode 1: Sensor Unit Button
                val isSensorSelected = currentMode == AppNodeMode.SENSOR_UNIT
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (isSensorSelected) 2.dp else 1.dp,
                            color = if (isSensorSelected) GoogleBlue else Color.LightGray,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onModeSelected(AppNodeMode.SENSOR_UNIT) }
                        .testTag("mode_sensor_button"),
                    color = if (isSensorSelected) GoogleBlue.copy(alpha = 0.1f) else Color.Transparent
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = if (isSensorSelected) GoogleBlue else SlateGrey,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sensor Unit",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isSensorSelected) GoogleBlue else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Attached to Pipe",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = SlateGrey
                        )
                    }
                }

                // Mode 2: Citizen / Receiver Button
                val isCitizenSelected = currentMode == AppNodeMode.CITIZEN_NODE
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (isCitizenSelected) 2.dp else 1.dp,
                            color = if (isCitizenSelected) GoogleBlue else Color.LightGray,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onModeSelected(AppNodeMode.CITIZEN_NODE) }
                        .testTag("mode_citizen_button"),
                    color = if (isCitizenSelected) GoogleBlue.copy(alpha = 0.1f) else Color.Transparent
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (isCitizenSelected) GoogleBlue else SlateGrey,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Alert Recipient",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isCitizenSelected) GoogleBlue else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Community / Family",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = SlateGrey
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SensorArmDisarmCard(
    isMonitoring: Boolean,
    deviceName: String,
    locationName: String,
    onToggleMonitoring: () -> Unit,
    onRequestBatteryOptimization: () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = if (isMonitoring) GoogleBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        label = "cardColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = if (isMonitoring) GoogleBlue else SlateGrey,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = locationName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = deviceName,
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateGrey
                        )
                    }
                }

                Switch(
                    checked = isMonitoring,
                    onCheckedChange = { onToggleMonitoring() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GoogleBlue
                    ),
                    modifier = Modifier.testTag("arm_disarm_switch")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Arm / Disarm Button
            Button(
                onClick = onToggleMonitoring,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("arm_disarm_button"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMonitoring) MaterialRed else GoogleBlue,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isMonitoring) "DISARM SENSOR MONITORING" else "ARM SENSOR MONITORING",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Battery Optimization hint button
            OutlinedButton(
                onClick = onRequestBatteryOptimization,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = SlateGrey,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ignore Battery Optimizations (For Continuous Run)",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGrey
                )
            }
        }
    }
}

@Composable
fun LiveVibrationMeterCard(
    currentDelta: Float,
    threshold: Float,
    currentDurationMs: Long,
    requiredDurationMs: Long
) {
    val animatedDeltaProgress by animateFloatAsState(
        targetValue = (currentDelta / 5.0f).coerceIn(0.0f, 1.0f),
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "deltaProgress"
    )

    val durationProgress = (currentDurationMs.toFloat() / requiredDurationMs.toFloat()).coerceIn(0.0f, 1.0f)
    val isExceedingThreshold = currentDelta > threshold

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        tint = if (isExceedingThreshold) MaterialRed else GoogleBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Live Motion Force (Δ)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = String.format("%.2f m/s²", currentDelta),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isExceedingThreshold) MaterialRed else GoogleBlue
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Realtime Linear Gauge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // Fill Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedDeltaProgress)
                        .height(20.dp)
                        .background(if (isExceedingThreshold) MaterialRed else GoogleBlue)
                )

                // Threshold Indicator Line
                val thresholdFraction = (threshold / 5.0f).coerceIn(0.0f, 1.0f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(thresholdFraction)
                        .height(20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .background(Color.DarkGray)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(text = "0.0 m/s²", style = MaterialTheme.typography.bodySmall, color = SlateGrey)
                Text(
                    text = "Threshold: ${threshold} m/s²",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(text = "5.0+ m/s²", style = MaterialTheme.typography.bodySmall, color = SlateGrey)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Continuous Duration Progress Bar
            Text(
                text = "Continuous Vibration Timer: ${currentDurationMs}ms / ${requiredDurationMs}ms",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isExceedingThreshold) FontWeight.Bold else FontWeight.Normal,
                    color = if (isExceedingThreshold) MaterialRed else SlateGrey
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { durationProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialRed,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            AnimatedVisibility(visible = isExceedingThreshold) {
                Text(
                    text = "⚠️ Continuous vibration threshold exceeded! Holding for trigger...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialRed,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ReceiverNodeStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = GoogleBlue,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Citizen Alert Receiver Active",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Listening for FCM payload notifications from community sensor units",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateGrey
                        )
                    }
                }
                
                // Pulsing Online Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE6F4EA))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF137333), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF137333)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Firebase Project Details
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = GoogleBlue.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoogleBlue.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = GoogleBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Connected to Firebase Cloud",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = GoogleBlue
                        )
                        Text(
                            text = "Project: automatic-flood-alert-app",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateGrey
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Channels & Emergency Fallbacks
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Active Emergency Listeners",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "✓", color = Color(0xFF137333), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FCM Broadcast Topic: 'flood_alerts'",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateGrey
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "✓", color = Color(0xFF137333), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Emergency Audio Override: 100% Volume on Alert",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateGrey
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "✓", color = Color(0xFF137333), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lockscreen Emergency Window Display: Enabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateGrey
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ManualTestCard(onTestTrigger: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Test Emergency System",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Simulate a critical flood alarm to test full-screen popup & audio",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGrey
                )
            }

            FilledTonalButton(
                onClick = onTestTrigger,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("test_alarm_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Test")
            }
        }
    }
}

@Composable
fun EmptyAlertsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.WaterDamage,
                contentDescription = null,
                tint = SlateGrey,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No Flood Alerts Recorded",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "All water sensor stations operating within safe thresholds.",
                style = MaterialTheme.typography.bodySmall,
                color = SlateGrey
            )
        }
    }
}

@Composable
fun AlertLogItemCard(alert: FloodAlert) {
    val formattedDate = remember(alert.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault())
        sdf.format(Date(alert.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialRed.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialRed,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = alert.locationName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        color = MaterialRed,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = alert.severity,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${alert.deviceName} • Peak Force Δ: ${String.format("%.2f", alert.peakDelta)} m/s²",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGrey,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun CalibrationSettingsDialog(
    currentThreshold: Float,
    currentDurationMs: Long,
    currentDeviceName: String,
    currentLocationName: String,
    onDismiss: () -> Unit,
    onSave: (Float, Long, String, String) -> Unit
) {
    var threshold by remember { mutableStateOf(currentThreshold) }
    var durationMs by remember { mutableStateOf(currentDurationMs) }
    var deviceName by remember { mutableStateOf(currentDeviceName) }
    var locationName by remember { mutableStateOf(currentLocationName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Sensor Calibration & Settings",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Station / Location Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device ID / Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text(
                        text = "Vibration Threshold (Δ): ${String.format("%.1f", threshold)} m/s²",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Default: 1.5 m/s². Higher ignores minor shocks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGrey
                    )
                    Slider(
                        value = threshold,
                        onValueChange = { threshold = it },
                        valueRange = 0.5f..5.0f,
                        steps = 45
                    )
                }

                Column {
                    Text(
                        text = "Continuous Requirement: ${durationMs} ms",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Default: 3000 ms (3 seconds continuous motion)",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGrey
                    )
                    Slider(
                        value = durationMs.toFloat(),
                        onValueChange = { durationMs = it.toLong() },
                        valueRange = 1000f..10000f,
                        steps = 18
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(threshold, durationMs, deviceName, locationName) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BatteryOptimizationExplanationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(text = "Exempt Battery Limit")
            }
        },
        text = {
            Text(
                text = "To ensure continuous, reliable, real-time background flood monitoring, Android requires that this application be whitelisted from battery optimization. Otherwise, the background monitoring service may be killed or suspended by the operating system, which could delay critical emergency alerts.\n\nOn the next screen, please select 'Allow' or change the app setting to 'Unrestricted' / 'Don't Optimize'.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Proceed to Whitelist")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}

fun requestIgnoreBatteryOptimization(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
        }
    }
}

@Composable
fun UserProfileCard(
    userName: String,
    villageName: String,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_profile_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(GoogleBlue.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = GoogleBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (userName.isBlank()) "Guest User" else userName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (villageName.isBlank()) "Community Station" else "📍 $villageName",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGrey
                    )
                }
            }

            OutlinedButton(
                onClick = onLogout,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("switch_user_button")
            ) {
                Text(
                    text = "Switch User",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
