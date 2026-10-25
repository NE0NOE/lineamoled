package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.example.data.LineMaskEntity
import com.example.data.OverlayStateManager
import com.example.data.PixelScannerState
import com.example.ui.theme.CalibrationEmerald
import com.example.ui.theme.ScannerAmber

/**
 * Capa de superposición en tiempo real dentro de la aplicación:
 * - Dibuja la línea verde de prueba opcional (si el usuario activa el Simulador).
 * - Cuando el Servicio de Accesibilidad aún no está activo, renderiza exactamente las mismas
 *   máscaras al píxel físico real (1:1 con HardwareMaskOverlayView) para permitir calibración
 *   inmediata en vivo.
 * - Si el usuario cubre el píxel detonante simulado (X=540, Y=480) con una máscara o con el
 *   escáner, la línea verde simulada se apaga automáticamente para demostrar el aislamiento.
 */
@Composable
fun InAppLiveOverlayCanvas(
    isMasterEnabled: Boolean,
    isAccessibilityConnected: Boolean,
    simulateFaultyLine: Boolean,
    showCalibrationGuides: Boolean,
    lines: List<LineMaskEntity>,
    selectedLineId: Long?,
    scannerState: PixelScannerState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val screenW = size.width.coerceAtLeast(1f)
        val screenH = size.height.coerceAtLeast(1f)

        // 1. Optional Simulated Hardware Green Line (at X = 540px, Trigger Pixel at Y = 480px)
        if (simulateFaultyLine) {
            val simX = OverlayStateManager.SIMULATED_LINE_X_PX.toFloat().coerceIn(0f, screenW - 2f)
            val triggerY = OverlayStateManager.SIMULATED_TRIGGER_Y_PX

            // Check if any active vertical mask covers the simulated trigger pixel (X=540, Y=480)
            val isTriggerSuppressed = isMasterEnabled && lines.any { line ->
                if (!line.isEnabled || !line.isVertical) return@any false
                val coversX = simX >= line.positionPx && simX < (line.positionPx + line.thicknessPx + 1)
                if (!coversX) return@any false

                val maxAxis = screenH.toInt()
                if (scannerState.isActive && scannerState.targetLineId == line.id) {
                    val sStart = scannerState.activeSegmentStartPx
                    val sEnd = scannerState.activeSegmentEndPx
                    if (scannerState.invertWindowMode) {
                        triggerY < sStart || triggerY > sEnd
                    } else {
                        triggerY in sStart..sEnd
                    }
                } else {
                    val sStart = line.startOffsetPx
                    val sEnd = if (line.endOffsetPx < 0) maxAxis else line.endOffsetPx
                    triggerY in sStart..sEnd
                }
            }

            if (!isTriggerSuppressed) {
                // Outer subpixel glow
                drawRect(
                    color = Color(0x5500FF66),
                    topLeft = Offset((simX - 2f).coerceAtLeast(0f), 0f),
                    size = Size(6f, screenH)
                )
                // Bright faulty green column
                drawRect(
                    color = Color(0xFF00FF66),
                    topLeft = Offset(simX, 0f),
                    size = Size(2f, screenH)
                )
                // Highlight the trigger subpixel anomaly at Y=480px
                drawRect(
                    color = Color(0xFFFFFFFF),
                    topLeft = Offset((simX - 1f).coerceAtLeast(0f), triggerY.toFloat()),
                    size = Size(4f, 4f)
                )
            }
        }

        // 2. Render the exact pixel masks in-app if Accessibility Service is not yet drawing them,
        // or draw subtle edge guide ticks so the user always sees instant 1px feedback
        if (!isMasterEnabled) return@Canvas

        for (line in lines) {
            if (!line.isEnabled) continue

            val baseColor = Color(line.maskColorHex).copy(alpha = line.opacity.coerceIn(0.05f, 1.0f))
            val isScannerTarget = scannerState.isActive && scannerState.targetLineId == line.id
            val maxAxis = if (line.isVertical) screenH.toInt() else screenW.toInt()

            if (isScannerTarget) {
                val segStart = scannerState.activeSegmentStartPx.coerceIn(0, maxAxis).toFloat()
                val segEnd = scannerState.activeSegmentEndPx.coerceIn(segStart.toInt() + 1, maxAxis).toFloat()

                if (!isAccessibilityConnected) {
                    if (scannerState.invertWindowMode) {
                        drawLineSegment(line, 0f, segStart, baseColor)
                        drawLineSegment(line, segEnd, maxAxis.toFloat(), baseColor)
                    } else {
                        drawLineSegment(line, segStart, segEnd, baseColor)
                    }
                }

                // Amber edge indicators for the scanner probe
                val tickLen = (segEnd - segStart).coerceAtLeast(6f)
                if (line.isVertical) {
                    drawRect(
                        color = ScannerAmber,
                        topLeft = Offset(0f, segStart),
                        size = Size(14f, tickLen)
                    )
                    drawRect(
                        color = ScannerAmber,
                        topLeft = Offset(screenW - 14f, segStart),
                        size = Size(14f, tickLen)
                    )
                } else {
                    drawRect(
                        color = ScannerAmber,
                        topLeft = Offset(segStart, 0f),
                        size = Size(tickLen, 14f)
                    )
                    drawRect(
                        color = ScannerAmber,
                        topLeft = Offset(segStart, screenH - 14f),
                        size = Size(tickLen, 14f)
                    )
                }
            } else {
                val startPx = line.startOffsetPx.coerceIn(0, maxAxis).toFloat()
                val endPx = if (line.endOffsetPx < 0 || line.endOffsetPx > maxAxis) {
                    maxAxis.toFloat()
                } else {
                    line.endOffsetPx.coerceAtLeast(line.startOffsetPx + 1).toFloat()
                }

                if (!isAccessibilityConnected) {
                    drawLineSegment(line, startPx, endPx, baseColor)
                }

                if (showCalibrationGuides && line.id == selectedLineId) {
                    val pos = line.positionPx.toFloat()
                    val thick = line.thicknessPx.coerceAtLeast(1).toFloat()
                    if (line.isVertical) {
                        val indicatorLeft = (pos - 8f).coerceAtLeast(0f)
                        val indicatorWidth = (thick + 16f).coerceAtMost(screenW - indicatorLeft)
                        drawRect(
                            color = CalibrationEmerald,
                            topLeft = Offset(indicatorLeft, 0f),
                            size = Size(indicatorWidth, 6f)
                        )
                        drawRect(
                            color = CalibrationEmerald,
                            topLeft = Offset(indicatorLeft, screenH - 6f),
                            size = Size(indicatorWidth, 6f)
                        )
                    } else {
                        val indicatorTop = (pos - 8f).coerceAtLeast(0f)
                        val indicatorHeight = (thick + 16f).coerceAtMost(screenH - indicatorTop)
                        drawRect(
                            color = CalibrationEmerald,
                            topLeft = Offset(0f, indicatorTop),
                            size = Size(6f, indicatorHeight)
                        )
                        drawRect(
                            color = CalibrationEmerald,
                            topLeft = Offset(screenW - 6f, indicatorTop),
                            size = Size(6f, indicatorHeight)
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLineSegment(
    line: LineMaskEntity,
    startPx: Float,
    endPx: Float,
    color: Color
) {
    if (endPx <= startPx) return
    val pos = line.positionPx.toFloat()
    val thick = line.thicknessPx.coerceAtLeast(1).toFloat()
    if (line.isVertical) {
        drawRect(
            color = color,
            topLeft = Offset(pos, startPx),
            size = Size(thick, endPx - startPx)
        )
    } else {
        drawRect(
            color = color,
            topLeft = Offset(startPx, pos),
            size = Size(endPx - startPx, thick)
        )
    }
}
