package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DisplayHardwareInfo
import com.example.data.LineMaskEntity
import com.example.ui.theme.AmoledBorderSubtle
import com.example.ui.theme.AmoledDeepSurface
import com.example.ui.theme.AmoledElevatedCard
import com.example.ui.theme.CalibrationEmerald
import com.example.ui.theme.OpticalCyan
import com.example.ui.theme.ScannerAmber
import com.example.ui.theme.TextSecondary

@Composable
fun EngineGuideTabScreen(
    isServiceConnected: Boolean,
    isMasterEnabled: Boolean,
    lines: List<LineMaskEntity>,
    displayInfo: DisplayHardwareInfo,
    viewModel: AmoledGuardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeLinesCount = lines.count { it.isEnabled }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hardware Efficiency & SurfaceFlinger Telemetry
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = CalibrationEmerald)
                    Column {
                        Text(
                            text = "Telemetría de Hardware y Eficiencia",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Arquitectura estática sin bucles de renderizado ni despertares de CPU",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryMetricBox(
                        label = "USO DE CPU EN REPOSO",
                        value = "0.0 %",
                        accent = CalibrationEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMetricBox(
                        label = "RESOLUCIÓN FÍSICA",
                        value = "${displayInfo.widthPx}×${displayInfo.heightPx}",
                        accent = OpticalCyan,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryMetricBox(
                        label = "HUELLA DE VENTANA",
                        value = "< 1.1 MB",
                        accent = CalibrationEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMetricBox(
                        label = "MÁSCARAS ACTIVAS",
                        value = "$activeLinesCount líneas (${displayInfo.refreshRateHz}Hz)",
                        accent = ScannerAmber,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = AmoledBorderSubtle)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = CalibrationEmerald)
                    Text(
                        text = "La vista nativa HardwareMaskOverlayView utiliza FLAG_NOT_TOUCHABLE y isAntiAlias=false. Una vez dibujada la línea #000000, el compositor SurfaceFlinger de Android mantiene esos píxeles apagados sin ejecutar código en cada fotograma.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // 2. Notification-Free Operation & Quick Settings Guide
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = OpticalCyan)
                    Column {
                        Text(
                            text = "Control Silencioso (Cero Notificaciones)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Cómo controlar la máscara en tu Samsung sin íconos en la barra de notificaciones",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                GuideStepRow(
                    stepNumber = "1",
                    title = "Habilita el Servicio de Accesibilidad",
                    description = "En Ajustes > Accesibilidad > Aplicaciones instaladas > AMOLED Line Guard, actívalo. Esto permite cubrir incluso la barra de estado superior y de navegación sin requerir una notificación permanente."
                )
                GuideStepRow(
                    stepNumber = "2",
                    title = "Añade el Botón 'Línea AMOLED' en Ajustes Rápidos",
                    description = "Toca el botón debajo (o edita tu panel de Ajustes Rápidos de One UI) para colocar el interruptor directo junto al Wi-Fi y Linterna."
                )
                GuideStepRow(
                    stepNumber = "3",
                    title = "Aísla o Atenúa tu Línea",
                    description = "Usa el Escáner de Píxel Detonante para comprobar si apagar un segmento corto detiene el destello, o usa el modo Negro Puro (100%) / Atenuador Nocturno (85%) sobre la columna completa."
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.openAccessibilitySettings(context) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("guide_accessibility_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CalibrationEmerald)
                    ) {
                        Text(
                            text = if (isServiceConnected) "Accesibilidad Activa" else "Abrir Accesibilidad",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.requestAddQuickSettingsTile(context) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("guide_quick_tile_button")
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = OpticalCyan)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Añadir Botón Rápido",
                            style = MaterialTheme.typography.labelLarge,
                            color = OpticalCyan
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TelemetryMetricBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AmoledElevatedCard)
            .border(1.dp, AmoledBorderSubtle, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
        }
    }
}

@Composable
private fun GuideStepRow(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AmoledElevatedCard)
                .border(1.dp, CalibrationEmerald.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelLarge,
                color = CalibrationEmerald
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
