package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AmoledGuardViewModel
import com.example.ui.CalibratorTabScreen
import com.example.ui.EngineGuideTabScreen
import com.example.ui.InAppLiveOverlayCanvas
import com.example.ui.PixelScannerTabScreen
import com.example.ui.theme.AmoledBorderSubtle
import com.example.ui.theme.AmoledDeepSurface
import com.example.ui.theme.AmoledElevatedCard
import com.example.ui.theme.AmoledPitchBlack
import com.example.ui.theme.CalibrationEmerald
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OpticalCyan
import com.example.ui.theme.ScannerAmber
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AmoledLineGuardApp()
            }
        }
    }
}

@Composable
fun AmoledLineGuardApp(
    viewModel: AmoledGuardViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isMasterEnabled by viewModel.isMasterEnabled.collectAsStateWithLifecycle()
    val isServiceConnected by viewModel.isServiceConnected.collectAsStateWithLifecycle()
    val showCalibrationGuides by viewModel.showCalibrationGuides.collectAsStateWithLifecycle()
    val simulateFaultyLine by viewModel.simulateFaultyGreenLine.collectAsStateWithLifecycle()
    val lines by viewModel.lines.collectAsStateWithLifecycle()
    val selectedLineId by viewModel.selectedLineId.collectAsStateWithLifecycle()
    val scannerState by viewModel.scannerState.collectAsStateWithLifecycle()
    val displayInfo by viewModel.displayInfo.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableIntStateOf(0) }

    // Mandatory BackHandler for secondary tabs to return to Home/Calibrator tab
    if (currentTab != 0) {
        BackHandler {
            currentTab = 0
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledPitchBlack)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AmoledPitchBlack,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                AmoledTopHeaderBar(
                    isMasterEnabled = isMasterEnabled,
                    activeLineCount = lines.count { it.isEnabled },
                    isScannerActive = scannerState.isActive,
                    onToggleMaster = { viewModel.setMasterEnabled(it) }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = AmoledDeepSurface,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.tab_calibrator)) },
                        label = { Text(stringResource(R.string.tab_calibrator)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CalibrationEmerald,
                            selectedTextColor = CalibrationEmerald,
                            indicatorColor = CalibrationEmerald.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.testTag("nav_tab_calibrator")
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.Radar, contentDescription = stringResource(R.string.tab_scanner)) },
                        label = { Text(stringResource(R.string.tab_scanner)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ScannerAmber,
                            selectedTextColor = ScannerAmber,
                            indicatorColor = ScannerAmber.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.testTag("nav_tab_scanner")
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.DeveloperBoard, contentDescription = stringResource(R.string.tab_efficiency)) },
                        label = { Text(stringResource(R.string.tab_efficiency)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OpticalCyan,
                            selectedTextColor = OpticalCyan,
                            indicatorColor = OpticalCyan.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.testTag("nav_tab_efficiency")
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(visible = bannerMessage != null) {
                    bannerMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmoledElevatedCard)
                                .border(1.dp, CalibrationEmerald.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { viewModel.clearBannerMessage() }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("status_feedback_banner")
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelLarge,
                                color = CalibrationEmerald
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                ) {
                    when (currentTab) {
                        0 -> CalibratorTabScreen(
                            isMasterEnabled = isMasterEnabled,
                            isServiceConnected = isServiceConnected,
                            showCalibrationGuides = showCalibrationGuides,
                            simulateFaultyLine = simulateFaultyLine,
                            lines = lines,
                            selectedLineId = selectedLineId,
                            displayInfo = displayInfo,
                            viewModel = viewModel,
                            onJumpToScannerTab = {
                                viewModel.startOrToggleScanner(true)
                                currentTab = 1
                            }
                        )
                        1 -> PixelScannerTabScreen(
                            lines = lines,
                            selectedLineId = selectedLineId,
                            scannerState = scannerState,
                            simulateFaultyLine = simulateFaultyLine,
                            viewModel = viewModel,
                            onAppliedCropReturnToCalibrator = {
                                currentTab = 0
                            }
                        )
                        else -> EngineGuideTabScreen(
                            isServiceConnected = isServiceConnected,
                            isMasterEnabled = isMasterEnabled,
                            lines = lines,
                            displayInfo = displayInfo,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // Full-bleed non-touchable live overlay preview layer (covers status bar to nav bar)
        InAppLiveOverlayCanvas(
            isMasterEnabled = isMasterEnabled,
            isAccessibilityConnected = isServiceConnected,
            simulateFaultyLine = simulateFaultyLine,
            showCalibrationGuides = showCalibrationGuides,
            lines = lines,
            selectedLineId = selectedLineId,
            scannerState = scannerState,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun AmoledTopHeaderBar(
    isMasterEnabled: Boolean,
    activeLineCount: Int,
    isScannerActive: Boolean,
    onToggleMaster: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmoledDeepSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = if (isMasterEnabled) CalibrationEmerald else TextSecondary
                )
                Column {
                    Text(
                        text = "AMOLED Line Guard",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            !isMasterEnabled -> "Superposición en pausa"
                            isScannerActive -> "Escáner de Píxel Detonante activo"
                            else -> "$activeLineCount máscara(s) #000000 activa(s)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            !isMasterEnabled -> TextSecondary
                            isScannerActive -> ScannerAmber
                            else -> CalibrationEmerald
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Switch(
                checked = isMasterEnabled,
                onCheckedChange = onToggleMaster,
                modifier = Modifier.testTag("master_overlay_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = CalibrationEmerald
                )
            )
        }
    }
}
