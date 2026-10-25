package com.example.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.example.data.LineMaskEntity
import com.example.data.OverlayStateManager
import com.example.data.PixelScannerState
import kotlin.math.roundToInt

/**
 * Vista nativa ultraligera para el Servicio de Accesibilidad.
 * Diseñada para 0% uso de CPU en reposo:
 * - Cero asignaciones de objetos dentro de onDraw().
 * - isAntiAlias = false en la máscara para apagar subpíxeles físicos exactos sin bordes difuminados.
 * - Solo se redibuja cuando el usuario cambia una coordenada o mueve el escáner.
 */
class HardwareMaskOverlayView(context: Context) : View(context) {

    private val maskPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
        isDither = false
        color = Color.BLACK
    }

    private val guideTickPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
        color = Color.parseColor("#00E676")
    }

    private val scannerGuidePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
        color = Color.parseColor("#FFB300")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!OverlayStateManager.isMasterEnabled.value) return

        val screenW = width.toFloat().coerceAtLeast(1f)
        val screenH = height.toFloat().coerceAtLeast(1f)
        val activeLines = OverlayStateManager.lines.value
        val scanner = OverlayStateManager.scannerState.value
        val showGuides = OverlayStateManager.showCalibrationGuides.value
        val selectedId = OverlayStateManager.selectedLineId.value

        for (i in activeLines.indices) {
            val line = activeLines[i]
            if (!line.isEnabled) continue

            drawSingleLineMask(
                canvas = canvas,
                line = line,
                screenW = screenW,
                screenH = screenH,
                scanner = scanner,
                isSelected = (line.id == selectedId),
                showGuides = showGuides
            )
        }
    }

    private fun drawSingleLineMask(
        canvas: Canvas,
        line: LineMaskEntity,
        screenW: Float,
        screenH: Float,
        scanner: PixelScannerState,
        isSelected: Boolean,
        showGuides: Boolean
    ) {
        val alphaInt = (line.opacity.coerceIn(0.05f, 1.0f) * 255f).roundToInt()
        val rgb = line.maskColorHex and 0x00FFFFFF
        maskPaint.color = (alphaInt shl 24) or rgb

        val isScannerTargetingThisLine = scanner.isActive && scanner.targetLineId == line.id
        val maxAxisLength = if (line.isVertical) screenH.toInt() else screenW.toInt()

        val startPx: Float
        val endPx: Float

        if (isScannerTargetingThisLine) {
            val segStart = scanner.activeSegmentStartPx.coerceIn(0, maxAxisLength).toFloat()
            val segEnd = scanner.activeSegmentEndPx.coerceIn(segStart.toInt() + 1, maxAxisLength).toFloat()

            if (scanner.invertWindowMode) {
                // Mode B: Mask the whole line EXCEPT the inspection window [segStart..segEnd]
                drawAxisSegment(canvas, line, 0f, segStart)
                drawAxisSegment(canvas, line, segEnd, maxAxisLength.toFloat())
            } else {
                // Mode A: Mask ONLY the test probe block [segStart..segEnd] to test trigger pixel shutoff
                drawAxisSegment(canvas, line, segStart, segEnd)
            }

            // Draw small amber ticks on the edges so the user knows where the probe is positioned
            if (line.isVertical) {
                canvas.drawRect(0f, segStart, 14f, (segEnd).coerceAtLeast(segStart + 4f), scannerGuidePaint)
                canvas.drawRect(screenW - 14f, segStart, screenW, (segEnd).coerceAtLeast(segStart + 4f), scannerGuidePaint)
            } else {
                canvas.drawRect(segStart, 0f, (segEnd).coerceAtLeast(segStart + 4f), 14f, scannerGuidePaint)
                canvas.drawRect(segStart, screenH - 14f, (segEnd).coerceAtLeast(segStart + 4f), screenH, scannerGuidePaint)
            }
            return
        }

        // Standard Full Line or Cropped Segment Mode
        startPx = line.startOffsetPx.coerceIn(0, maxAxisLength).toFloat()
        endPx = if (line.endOffsetPx < 0 || line.endOffsetPx > maxAxisLength) {
            maxAxisLength.toFloat()
        } else {
            line.endOffsetPx.coerceAtLeast(line.startOffsetPx + 1).toFloat()
        }

        drawAxisSegment(canvas, line, startPx, endPx)

        // Optional edge reticle markers when calibrating inside the app
        if (showGuides && isSelected) {
            val pos = line.positionPx.toFloat()
            val thick = line.thicknessPx.coerceAtLeast(1).toFloat()
            if (line.isVertical) {
                // Small emerald pointer ticks at top and bottom edge flanking the line
                canvas.drawRect((pos - 8f).coerceAtLeast(0f), 0f, (pos + thick + 8f).coerceAtMost(screenW), 6f, guideTickPaint)
                canvas.drawRect((pos - 8f).coerceAtLeast(0f), screenH - 6f, (pos + thick + 8f).coerceAtMost(screenW), screenH, guideTickPaint)
            } else {
                canvas.drawRect(0f, (pos - 8f).coerceAtLeast(0f), 6f, (pos + thick + 8f).coerceAtMost(screenH), guideTickPaint)
                canvas.drawRect(screenW - 6f, (pos - 8f).coerceAtLeast(0f), screenW, (pos + thick + 8f).coerceAtMost(screenH), guideTickPaint)
            }
        }
    }

    private fun drawAxisSegment(
        canvas: Canvas,
        line: LineMaskEntity,
        startPx: Float,
        endPx: Float
    ) {
        if (endPx <= startPx) return
        val pos = line.positionPx.toFloat()
        val thick = line.thicknessPx.coerceAtLeast(1).toFloat()

        if (line.isVertical) {
            canvas.drawRect(pos, startPx, pos + thick, endPx, maskPaint)
        } else {
            canvas.drawRect(startPx, pos, endPx, pos + thick, maskPaint)
        }
    }
}
