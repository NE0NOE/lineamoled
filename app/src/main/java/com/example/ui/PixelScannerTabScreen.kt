package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LineMaskEntity
import com.example.data.OverlayStateManager
import com.example.data.PixelScannerState
import com.example.data.ScannerMethod
import com.example.ui.theme.AmoledBorderSubtle
import com.example.ui.theme.AmoledDeepSurface
import com.example.ui.theme.AmoledElevatedCard
import com.example.ui.theme.CalibrationEmerald
import com.example.ui.theme.OpticalCyan
import com.example.ui.theme.ScannerAmber
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun PixelScannerTabScreen(
    lines: List<LineMaskEntity>,
    selectedLineId: Long?,
    scannerState: PixelScannerState,
    simulateFaultyLine: Boolean,
    viewModel: AmoledGuardViewModel,
    onAppliedCropReturnToCalibrator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val selectedLine = lines.find { it.id == selectedLineId } ?: lines.firstOrNull()
    val maxAxisPx = viewModel.getMaxSegmentAxisFor(selectedLine)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Scanner Header & Activation Switch
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (scannerState.isActive) ScannerAmber else AmoledBorderSubtle,
                    shape = RoundedCornerShape(18.dp)
                ),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AmoledDeepSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = ScannerAmber
                        )
                        Column {
                            Text(
                                text = "Escáner de Píxel Detonante",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Busca si al apagar un solo píxel o micro-segmento desaparece toda la línea verde",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = scannerState.isActive,
                        onCheckedChange = { enabled ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.startOrToggleScanner(enabled)
                        },
                        modifier = Modifier.testTag("scanner_active_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = ScannerAmber
                        )
                    )
                }

                if (selectedLine != null) {
                    Text(
                        text = "Línea objetivo: ${selectedLine.name} (${if (selectedLine.isVertical) "X" else "Y"} = ${selectedLine.positionPx}px · Grosor ${selectedLine.thicknessPx}px)",
                        style = MaterialTheme.typography.labelLarge,
                        color = CalibrationEmerald
                    )
                }

                // Mode A vs Mode B Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !scannerState.invertWindowMode,
                        onClick = { viewModel.toggleInvertScannerWindow(false) },
                        label = { Text("Apagar Solo Sonda (Aislar)", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ScannerAmber.copy(alpha = 0.22f),
                            selectedLabelColor = ScannerAmber
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = scannerState.invertWindowMode,
                        onClick = { viewModel.toggleInvertScannerWindow(true) },
                        label = { Text("Invertir (Ocultar Resto)", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OpticalCyan.copy(alpha = 0.22f),
                            selectedLabelColor = OpticalCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Method Selector (Step-by-step, Auto-sweep, Binary Search)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledBorderSubtle, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AmoledDeepSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Método de Escaneo",
                    style = MaterialTheme.typography.titleMedium
                )
                ScannerMethod.entries.forEach { method ->
                    val isSelected = scannerState.method == method
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AmoledElevatedCard else Color(0xFF06080A))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) ScannerAmber else AmoledBorderSubtle,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setScannerMethod(method)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("scanner_method_${method.name}")
                    ) {
                        Column {
                            Text(
                                text = method.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) ScannerAmber else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = method.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 3. Active Scanner Controls Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledBorderSubtle, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AmoledDeepSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rango de Sonda Activa",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${scannerState.activeSegmentStartPx} px → ${scannerState.activeSegmentEndPx} px",
                        style = MaterialTheme.typography.labelLarge,
                        color = ScannerAmber
                    )
                }

                if (scannerState.method != ScannerMethod.BINARY_SEARCH) {
                    // Probe Length Selector (1px to 64px)
                    Text(
                        text = "Tamaño del bloque de prueba: ${scannerState.probeLengthPx} px",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 4, 8, 16, 32, 64).forEach { sizePx ->
                            FilterChip(
                                selected = scannerState.probeLengthPx == sizePx,
                                onClick = { viewModel.setScannerProbeLength(sizePx) },
                                label = { Text("${sizePx}px", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ScannerAmber.copy(alpha = 0.22f),
                                    selectedLabelColor = ScannerAmber
                                )
                            )
                        }
                    }

                    // Position Slider along the vertical/horizontal axis
                    Slider(
                        value = scannerState.probeStartPx.toFloat().coerceIn(0f, maxAxisPx.toFloat()),
                        onValueChange = {
                            viewModel.startOrToggleScanner(true)
                            viewModel.setScannerProbeStart(it.roundToInt())
                        },
                        valueRange = 0f..(maxAxisPx - scannerState.probeLengthPx).coerceAtLeast(1).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = ScannerAmber,
                            activeTrackColor = ScannerAmber
                        ),
                        modifier = Modifier.testTag("scanner_probe_slider")
                    )

                    // Manual Step Buttons (-10, -1, +1, +10 px)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MicroStepButton(
                            label = "-10 px",
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.stepScannerProbe(-10)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scanner_step_minus_10")
                        )
                        MicroStepButton(
                            label = "-1 px",
                            isPrimary = true,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.stepScannerProbe(-1)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scanner_step_minus_1")
                        )
                        MicroStepButton(
                            label = "+1 px",
                            isPrimary = true,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.stepScannerProbe(1)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scanner_step_plus_1")
                        )
                        MicroStepButton(
                            label = "+10 px",
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.stepScannerProbe(10)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scanner_step_plus_10")
                        )
                    }

                    if (simulateFaultyLine) {
                        OutlinedButton(
                            onClick = {
                                viewModel.startOrToggleScanner(true)
                                viewModel.setScannerProbeStart(
                                    OverlayStateManager.SIMULATED_TRIGGER_Y_PX - (scannerState.probeLengthPx / 2)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("jump_probe_to_simulated_trigger")
                        ) {
                            Text(
                                text = "Probar Sonda sobre Píxel Detonante Simulado (Y=480px)",
                                style = MaterialTheme.typography.labelLarge,
                                color = CalibrationEmerald
                            )
                        }
                    }

                    if (scannerState.method == ScannerMethod.AUTO_SWEEP) {
                        HorizontalDivider(color = AmoledBorderSubtle)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = scannerState.sweepIntervalMs == 140L,
                                onClick = { viewModel.setSweepSpeed(stepPx = 2, intervalMs = 140L) },
                                label = { Text("Lento (2px)", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = scannerState.sweepIntervalMs == 90L,
                                onClick = { viewModel.setSweepSpeed(stepPx = 4, intervalMs = 90L) },
                                label = { Text("Normal (4px)", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = scannerState.sweepIntervalMs == 45L,
                                onClick = { viewModel.setSweepSpeed(stepPx = 8, intervalMs = 45L) },
                                label = { Text("Rápido (8px)", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = { viewModel.toggleAutoSweep() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("toggle_auto_sweep_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (scannerState.isAutoSweeping) CalibrationEmerald else ScannerAmber
                            )
                        ) {
                            Icon(
                                imageVector = if (scannerState.isAutoSweeping) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (scannerState.isAutoSweeping) {
                                    "¡DETENER AQUÍ! (En ${scannerState.probeStartPx} px)"
                                } else {
                                    "Iniciar Barrido Automático"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Binary Search Controls
                    val spanPx = (scannerState.binaryHighPx - scannerState.binaryLowPx).coerceAtLeast(1)
                    Text(
                        text = "Rango acotado actual: ${scannerState.binaryLowPx}px .. ${scannerState.binaryHighPx}px (Amplitud: $spanPx px)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OpticalCyan
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = scannerState.binaryTestingFirstHalf,
                            onClick = { viewModel.toggleBinaryHalfSelection(true) },
                            label = {
                                Text(
                                    "Mitad A (${scannerState.binaryLowPx}..${scannerState.currentBinaryMidPx}px)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !scannerState.binaryTestingFirstHalf,
                            onClick = { viewModel.toggleBinaryHalfSelection(false) },
                            label = {
                                Text(
                                    "Mitad B (${scannerState.currentBinaryMidPx}..${scannerState.binaryHighPx}px)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.confirmBinaryHalfContainsTrigger() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("binary_narrow_half_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = OpticalCyan)
                        ) {
                            Icon(Icons.Default.CallSplit, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Acotar a Esta Mitad",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.resetBinarySearchRange() },
                            modifier = Modifier.testTag("binary_reset_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reiniciar rango")
                        }
                    }
                }

                HorizontalDivider(color = AmoledBorderSubtle)

                // Lock isolated pixel/segment button
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.lockCurrentProbeAsPermanentCrop()
                        onAppliedCropReturnToCalibrator()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("lock_probe_crop_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CalibrationEmerald)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fijar Recorte en Este Punto (${scannerState.activeSegmentStartPx}–${scannerState.activeSegmentEndPx}px)",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
