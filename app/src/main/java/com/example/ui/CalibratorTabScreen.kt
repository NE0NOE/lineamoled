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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DisplayHardwareInfo
import com.example.data.LineMaskEntity
import com.example.data.LineOrientation
import com.example.data.MaskPresetColor
import com.example.ui.theme.AmoledBorderSubtle
import com.example.ui.theme.AmoledDeepSurface
import com.example.ui.theme.AmoledElevatedCard
import com.example.ui.theme.CalibrationEmerald
import com.example.ui.theme.DangerRed
import com.example.ui.theme.OpticalCyan
import com.example.ui.theme.ScannerAmber
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun CalibratorTabScreen(
    isMasterEnabled: Boolean,
    isServiceConnected: Boolean,
    showCalibrationGuides: Boolean,
    simulateFaultyLine: Boolean,
    lines: List<LineMaskEntity>,
    selectedLineId: Long?,
    displayInfo: DisplayHardwareInfo,
    viewModel: AmoledGuardViewModel,
    onJumpToScannerTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val selectedLine = lines.find { it.id == selectedLineId } ?: lines.firstOrNull()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. System Overlay Status & Quick Settings Card
        OverlayPermissionAndQuickTileCard(
            isServiceConnected = isServiceConnected,
            simulateFaultyLine = simulateFaultyLine,
            onOpenAccessibility = { viewModel.openAccessibilitySettings(context) },
            onAddQuickTile = { viewModel.requestAddQuickSettingsTile(context) },
            onToggleSimulator = { enabled ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.setSimulateFaultyGreenLine(enabled)
            },
            onAlignToSimulator = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.alignSelectedLineToSimulatedFault()
            }
        )

        // 2. Multi-Line Carousel (Vertical & Horizontal Independent Lines)
        MultiLineSelectorCard(
            lines = lines,
            selectedLineId = selectedLine?.id,
            showCalibrationGuides = showCalibrationGuides,
            onSelectLine = { viewModel.selectLine(it) },
            onToggleLine = { viewModel.toggleLineEnabled(it) },
            onAddVertical = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addLine(LineOrientation.VERTICAL)
            },
            onAddHorizontal = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addLine(LineOrientation.HORIZONTAL)
            },
            onDeleteLine = { viewModel.deleteLine(it) },
            onToggleGuides = { viewModel.setShowCalibrationGuides(it) }
        )

        if (selectedLine != null) {
            // 3. Precision 1px Position & Thickness Calibrator
            PrecisionPositionAndThicknessCard(
                line = selectedLine,
                displayInfo = displayInfo,
                onNudgePosition = { delta ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.nudgePositionPx(delta)
                },
                onSlidePosition = { pos -> viewModel.setPositionPx(pos) },
                onNudgeThickness = { delta ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.nudgeThicknessPx(delta)
                },
                onSelectThickness = { thick ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.setThicknessPx(thick)
                },
                onOpenScannerForThisLine = onJumpToScannerTab
            )

            // 4. Vertical / Horizontal Segment Cropping Card
            SegmentCroppingCard(
                line = selectedLine,
                maxAxisPx = viewModel.getMaxSegmentAxisFor(selectedLine),
                onResetFullScreen = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.setFullScreenSegment()
                },
                onUpdateRange = { start, end -> viewModel.updateSegmentRange(start, end) },
                onNudgeStart = { delta ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.nudgeSegmentStart(delta)
                },
                onNudgeEnd = { delta ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.nudgeSegmentEnd(delta)
                }
            )

            // 5. AMOLED Shutoff (#000000) vs Night Dimmer & Subpixel Filter Card
            OpacityAndColorModeCard(
                line = selectedLine,
                onChangeOpacity = { viewModel.setLineOpacity(it) },
                onSelectPresetColor = { preset ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.setLinePresetColor(preset)
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun OverlayPermissionAndQuickTileCard(
    isServiceConnected: Boolean,
    simulateFaultyLine: Boolean,
    onOpenAccessibility: () -> Unit,
    onAddQuickTile: () -> Unit,
    onToggleSimulator: (Boolean) -> Unit,
    onAlignToSimulator: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isServiceConnected) CalibrationEmerald.copy(alpha = 0.45f) else ScannerAmber.copy(alpha = 0.55f),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AmoledDeepSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            if (isServiceConnected) CalibrationEmerald.copy(alpha = 0.10f) else ScannerAmber.copy(alpha = 0.12f),
                             Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isServiceConnected) Icons.Default.CheckCircle else Icons.Default.SettingsAccessibility,
                        contentDescription = "Estado de superposición",
                        tint = if (isServiceConnected) CalibrationEmerald else ScannerAmber
                    )
                    Column {
                        Text(
                            text = if (isServiceConnected) {
                                "Superposición Total Activa (0% CPU)"
                            } else {
                                "Modo Vista Previa en App"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isServiceConnected) {
                                "Cubriendo pantalla completa y barra de estado sin notificaciones"
                            } else {
                                "Activa el Servicio de Accesibilidad para cubrir fuera de la app"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isServiceConnected) {
                    Button(
                        onClick = onOpenAccessibility,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("enable_accessibility_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CalibrationEmerald),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Activar en Accesibilidad",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                OutlinedButton(
                    onClick = onAddQuickTile,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_quick_tile_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "+ Botón Ajustes Rápidos",
                        style = MaterialTheme.typography.labelLarge,
                        color = OpticalCyan
                    )
                }
            }

            HorizontalDivider(color = AmoledBorderSubtle)

            // Optional Simulated Green Line bar for easy calibration testing
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Simular Línea Verde de Prueba (X=540px)",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (simulateFaultyLine) CalibrationEmerald else TextSecondary
                    )
                    Text(
                        text = "Incluye un píxel detonante simulado en Y=480px para probar el escáner",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = simulateFaultyLine,
                    onCheckedChange = onToggleSimulator,
                    modifier = Modifier.testTag("simulate_green_line_switch"),
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = CalibrationEmerald)
                )
            }

            if (simulateFaultyLine) {
                OutlinedButton(
                    onClick = onAlignToSimulator,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("align_to_simulator_button")
                ) {
                    Text(
                        text = "Alinear Máscara Actual a X = 540 px",
                        style = MaterialTheme.typography.labelLarge,
                        color = CalibrationEmerald
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiLineSelectorCard(
    lines: List<LineMaskEntity>,
    selectedLineId: Long?,
    showCalibrationGuides: Boolean,
    onSelectLine: (Long) -> Unit,
    onToggleLine: (LineMaskEntity) -> Unit,
    onAddVertical: () -> Unit,
    onAddHorizontal: () -> Unit,
    onDeleteLine: (LineMaskEntity) -> Unit,
    onToggleGuides: (Boolean) -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Líneas Activas (${lines.count { it.isEnabled }}/${lines.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Soporte independiente para ejes verticales y horizontales",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onAddVertical,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_vertical_line_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Vertical", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onAddHorizontal,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_horizontal_line_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Horiz.", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                lines.forEach { line ->
                    val isSelected = line.id == selectedLineId
                    val axisTag = if (line.isVertical) "X=${line.positionPx}px" else "Y=${line.positionPx}px"
                    val cropTag = if (line.endOffsetPx < 0 && line.startOffsetPx == 0) {
                        "Completa"
                    } else {
                        "Recorte ${line.startOffsetPx}..${line.endOffsetPx}px"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) AmoledElevatedCard else Color(0xFF06080A))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) CalibrationEmerald else AmoledBorderSubtle,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectLine(line.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("line_chip_${line.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (line.isEnabled) CalibrationEmerald else TextSecondary)
                                    .clickable { onToggleLine(line) }
                            )
                            Column {
                                Text(
                                    text = line.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$axisTag · ${line.thicknessPx}px · $cropTag",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) CalibrationEmerald else TextSecondary
                                )
                            }
                            if (lines.size > 1) {
                                IconButton(
                                    onClick = { onDeleteLine(line) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Eliminar línea",
                                        tint = DangerRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = OpticalCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Guías verdes en los bordes al calibrar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = showCalibrationGuides,
                    onCheckedChange = onToggleGuides,
                    modifier = Modifier.testTag("calibration_guides_switch")
                )
            }
        }
    }
}

@Composable
private fun PrecisionPositionAndThicknessCard(
    line: LineMaskEntity,
    displayInfo: DisplayHardwareInfo,
    onNudgePosition: (Int) -> Unit,
    onSlidePosition: (Int) -> Unit,
    onNudgeThickness: (Int) -> Unit,
    onSelectThickness: (Int) -> Unit,
    onOpenScannerForThisLine: () -> Unit
) {
    val maxPos = if (line.isVertical) (displayInfo.widthPx - 1).coerceAtLeast(1) else (displayInfo.heightPx - 1).coerceAtLeast(1)
    val axisLetter = if (line.isVertical) "X (Horizontal)" else "Y (Vertical)"

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = CalibrationEmerald)
                    Column {
                        Text(
                            text = "Control de Precisión al Píxel",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Coordenada $axisLetter · Máx ${maxPos}px",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmoledElevatedCard)
                        .border(1.dp, CalibrationEmerald.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${if (line.isVertical) "X" else "Y"} = ${line.positionPx} px",
                        style = MaterialTheme.typography.labelLarge,
                        color = CalibrationEmerald
                    )
                }
            }

            // Coarse Slider
            Slider(
                value = line.positionPx.toFloat().coerceIn(0f, maxPos.toFloat()),
                onValueChange = { onSlidePosition(it.roundToInt()) },
                valueRange = 0f..maxPos.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = CalibrationEmerald,
                    activeTrackColor = CalibrationEmerald
                ),
                modifier = Modifier.testTag("position_slider")
            )

            // Fine Micro-Step D-Pad (-10px, -1px, +1px, +10px)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MicroStepButton(
                    label = "-10 px",
                    onClick = { onNudgePosition(-10) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pos_minus_10_button")
                )
                MicroStepButton(
                    label = "-1 px",
                    isPrimary = true,
                    onClick = { onNudgePosition(-1) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pos_minus_1_button")
                )
                MicroStepButton(
                    label = "+1 px",
                    isPrimary = true,
                    onClick = { onNudgePosition(1) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pos_plus_1_button")
                )
                MicroStepButton(
                    label = "+10 px",
                    onClick = { onNudgePosition(10) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pos_plus_10_button")
                )
            }

            HorizontalDivider(color = AmoledBorderSubtle)

            // Thickness (Ancho de Línea)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ancho / Grosor de la Máscara",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${line.thicknessPx} px",
                    style = MaterialTheme.typography.labelLarge,
                    color = OpticalCyan
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MicroStepButton(
                    label = "-1 px",
                    onClick = { onNudgeThickness(-1) },
                    modifier = Modifier.testTag("thick_minus_1_button")
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 2, 3, 4, 6, 8, 12).forEach { presetPx ->
                        FilterChip(
                            selected = line.thicknessPx == presetPx,
                            onClick = { onSelectThickness(presetPx) },
                            label = { Text("${presetPx}px", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OpticalCyan.copy(alpha = 0.22f),
                                selectedLabelColor = OpticalCyan
                            )
                        )
                    }
                }
                MicroStepButton(
                    label = "+1 px",
                    onClick = { onNudgeThickness(1) },
                    modifier = Modifier.testTag("thick_plus_1_button")
                )
            }

            // Shortcut to Trigger Pixel Scanner for this aligned line
            Button(
                onClick = onOpenScannerForThisLine,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("jump_to_scanner_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ScannerAmber)
            ) {
                Icon(Icons.Default.Radar, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Escanear Píxel Detonante en Esta Línea",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SegmentCroppingCard(
    line: LineMaskEntity,
    maxAxisPx: Int,
    onResetFullScreen: () -> Unit,
    onUpdateRange: (Int, Int) -> Unit,
    onNudgeStart: (Int) -> Unit,
    onNudgeEnd: (Int) -> Unit
) {
    val isFullLine = line.startOffsetPx == 0 && (line.endOffsetPx < 0 || line.endOffsetPx >= maxAxisPx)
    val effectiveEnd = if (line.endOffsetPx < 0 || line.endOffsetPx > maxAxisPx) maxAxisPx else line.endOffsetPx
    val lengthPx = (effectiveEnd - line.startOffsetPx).coerceAtLeast(1)

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = null, tint = ScannerAmber)
                    Column {
                        Text(
                            text = "Recorte por Segmentos",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isFullLine) {
                                "Cubriendo toda la pantalla (0px a ${maxAxisPx}px)"
                            } else {
                                "Segmento activo: ${line.startOffsetPx}px a ${effectiveEnd}px ($lengthPx px)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isFullLine) TextSecondary else ScannerAmber
                        )
                    }
                }

                if (!isFullLine) {
                    OutlinedButton(
                        onClick = onResetFullScreen,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("reset_full_line_button")
                    ) {
                        Icon(Icons.Default.OpenInFull, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Completa", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Start Coordinate Controls
            Text(
                text = "Punto de Inicio: ${line.startOffsetPx} px",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = line.startOffsetPx.toFloat().coerceIn(0f, (effectiveEnd - 1).coerceAtLeast(1).toFloat()),
                onValueChange = { onUpdateRange(it.roundToInt(), effectiveEnd) },
                valueRange = 0f..(maxAxisPx - 1).coerceAtLeast(1).toFloat(),
                colors = SliderDefaults.colors(thumbColor = ScannerAmber, activeTrackColor = ScannerAmber)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MicroStepButton("-10 px", onClick = { onNudgeStart(-10) }, modifier = Modifier.weight(1f))
                MicroStepButton("-1 px", onClick = { onNudgeStart(-1) }, modifier = Modifier.weight(1f))
                MicroStepButton("+1 px", onClick = { onNudgeStart(1) }, modifier = Modifier.weight(1f))
                MicroStepButton("+10 px", onClick = { onNudgeStart(10) }, modifier = Modifier.weight(1f))
            }

            // End Coordinate Controls
            Text(
                text = "Punto Final: $effectiveEnd px",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = effectiveEnd.toFloat().coerceIn((line.startOffsetPx + 1).toFloat(), maxAxisPx.toFloat()),
                onValueChange = { onUpdateRange(line.startOffsetPx, it.roundToInt()) },
                valueRange = 1f..maxAxisPx.toFloat(),
                colors = SliderDefaults.colors(thumbColor = ScannerAmber, activeTrackColor = ScannerAmber)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MicroStepButton("-10 px", onClick = { onNudgeEnd(-10) }, modifier = Modifier.weight(1f))
                MicroStepButton("-1 px", onClick = { onNudgeEnd(-1) }, modifier = Modifier.weight(1f))
                MicroStepButton("+1 px", onClick = { onNudgeEnd(1) }, modifier = Modifier.weight(1f))
                MicroStepButton("+10 px", onClick = { onNudgeEnd(10) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OpacityAndColorModeCard(
    line: LineMaskEntity,
    onChangeOpacity: (Float) -> Unit,
    onSelectPresetColor: (MaskPresetColor) -> Unit
) {
    val opacityPercent = (line.opacity * 100f).roundToInt()

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Modo de Apagado / Atenuación Nocturna",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (opacityPercent == 100 && line.maskColorHex == MaskPresetColor.PURE_BLACK.hex) {
                            "Negro Puro AMOLED 100% — Subpíxel apagado (0V)"
                        } else {
                            "Modo Atenuador ($opacityPercent%) — Deja ver contenido debajo sin deslumbrar"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (opacityPercent == 100) CalibrationEmerald else OpticalCyan
                    )
                }
                Text(
                    text = "$opacityPercent%",
                    style = MaterialTheme.typography.labelLarge,
                    color = CalibrationEmerald
                )
            }

            Slider(
                value = line.opacity,
                onValueChange = onChangeOpacity,
                valueRange = 0.15f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = CalibrationEmerald,
                    activeTrackColor = CalibrationEmerald
                ),
                modifier = Modifier.testTag("opacity_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = opacityPercent == 100,
                    onClick = { onChangeOpacity(1.0f) },
                    label = { Text("100% Apagado Total", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = opacityPercent in 80..90,
                    onClick = { onChangeOpacity(0.85f) },
                    label = { Text("85% Noche", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = opacityPercent in 55..70,
                    onClick = { onChangeOpacity(0.65f) },
                    label = { Text("65% Suave", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Tono de Máscara / Filtro de Subpíxel",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MaskPresetColor.entries.forEach { preset ->
                    val isSelected = line.maskColorHex == preset.hex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AmoledElevatedCard else Color(0xFF06080A))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CalibrationEmerald else AmoledBorderSubtle,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectPresetColor(preset) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) CalibrationEmerald else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MicroStepButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isPrimary) CalibrationEmerald.copy(alpha = 0.12f) else AmoledElevatedCard
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isPrimary) CalibrationEmerald else MaterialTheme.colorScheme.onSurface
        )
    }
}
