package com.example.ui

import android.app.Application
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AppDatabase
import com.example.data.DisplayHardwareInfo
import com.example.data.LineMaskEntity
import com.example.data.LineOrientation
import com.example.data.MaskPresetColor
import com.example.data.OverlayStateManager
import com.example.data.PixelScannerState
import com.example.data.ScannerMethod
import com.example.overlay.AmoledQuickTileService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AmoledGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val dao = AppDatabase.getInstance(appContext).lineMaskDao()

    val isMasterEnabled: StateFlow<Boolean> = OverlayStateManager.isMasterEnabled
    val isServiceConnected: StateFlow<Boolean> = OverlayStateManager.isServiceConnected
    val showCalibrationGuides: StateFlow<Boolean> = OverlayStateManager.showCalibrationGuides
    val simulateFaultyGreenLine: StateFlow<Boolean> = OverlayStateManager.simulateFaultyGreenLine
    val lines: StateFlow<List<LineMaskEntity>> = OverlayStateManager.lines
    val selectedLineId: StateFlow<Long?> = OverlayStateManager.selectedLineId
    val scannerState: StateFlow<PixelScannerState> = OverlayStateManager.scannerState
    val displayInfo: StateFlow<DisplayHardwareInfo> = OverlayStateManager.displayInfo

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private var autoSweepJob: Job? = null

    init {
        OverlayStateManager.initialize(appContext)
    }

    fun refreshSystemState() {
        OverlayStateManager.refreshDisplayMetrics(appContext)
        OverlayStateManager.refreshAccessibilityStatus(appContext)
    }

    fun clearBannerMessage() {
        _statusBannerMessage.value = null
    }

    fun postMessage(msg: String) {
        _statusBannerMessage.value = msg
    }

    fun setMasterEnabled(enabled: Boolean) {
        OverlayStateManager.setMasterEnabled(appContext, enabled)
    }

    fun setShowCalibrationGuides(show: Boolean) {
        OverlayStateManager.setShowCalibrationGuides(appContext, show)
    }

    fun setSimulateFaultyGreenLine(simulate: Boolean) {
        OverlayStateManager.setSimulateFaultyGreenLine(simulate)
        if (simulate) {
            postMessage("Simulador activo en X=${OverlayStateManager.SIMULATED_LINE_X_PX}px (Píxel detonante en Y=${OverlayStateManager.SIMULATED_TRIGGER_Y_PX}px)")
        }
    }

    fun selectLine(lineId: Long) {
        OverlayStateManager.selectLine(lineId)
        if (scannerState.value.isActive) {
            val target = lines.value.find { it.id == lineId }
            val maxAxis = getMaxSegmentAxisFor(target)
            OverlayStateManager.updateScannerState {
                it.copy(
                    targetLineId = lineId,
                    binaryLowPx = 0,
                    binaryHighPx = maxAxis
                )
            }
        }
    }

    fun addLine(orientation: LineOrientation) {
        viewModelScope.launch {
            val metrics = displayInfo.value
            val count = lines.value.size + 1
            val defaultPos = if (orientation == LineOrientation.VERTICAL) {
                ((metrics.widthPx / 2) + (count * 24)) % metrics.widthPx.coerceAtLeast(200)
            } else {
                ((metrics.heightPx / 3) + (count * 36)) % metrics.heightPx.coerceAtLeast(300)
            }
            val label = if (orientation == LineOrientation.VERTICAL) {
                "Línea Vertical #$count"
            } else {
                "Línea Horizontal #$count"
            }
            val newId = dao.insertLine(
                LineMaskEntity(
                    name = label,
                    isEnabled = true,
                    orientation = orientation.name,
                    positionPx = defaultPos,
                    thicknessPx = 2,
                    startOffsetPx = 0,
                    endOffsetPx = -1,
                    opacity = 1.0f,
                    maskColorHex = MaskPresetColor.PURE_BLACK.hex
                )
            )
            OverlayStateManager.selectLine(newId)
            postMessage("$label creada en ${defaultPos}px")
        }
    }

    fun deleteLine(line: LineMaskEntity) {
        viewModelScope.launch {
            if (scannerState.value.targetLineId == line.id) {
                stopScanner()
            }
            dao.deleteLine(line)
            postMessage("${line.name} eliminada")
        }
    }

    fun updateSelectedLine(transform: (LineMaskEntity) -> LineMaskEntity) {
        val currentId = selectedLineId.value ?: return
        val currentLine = lines.value.find { it.id == currentId } ?: return
        val updated = transform(currentLine)
        viewModelScope.launch {
            dao.updateLine(updated)
        }
    }

    fun toggleLineEnabled(line: LineMaskEntity) {
        viewModelScope.launch {
            dao.updateLine(line.copy(isEnabled = !line.isEnabled))
        }
    }

    fun nudgePositionPx(deltaPx: Int) {
        updateSelectedLine { line ->
            val maxPos = if (line.isVertical) {
                displayInfo.value.widthPx - 1
            } else {
                displayInfo.value.heightPx - 1
            }
            line.copy(positionPx = (line.positionPx + deltaPx).coerceIn(0, maxPos))
        }
    }

    fun setPositionPx(newPx: Int) {
        updateSelectedLine { line ->
            val maxPos = if (line.isVertical) {
                displayInfo.value.widthPx - 1
            } else {
                displayInfo.value.heightPx - 1
            }
            line.copy(positionPx = newPx.coerceIn(0, maxPos))
        }
    }

    fun nudgeThicknessPx(deltaPx: Int) {
        updateSelectedLine { line ->
            line.copy(thicknessPx = (line.thicknessPx + deltaPx).coerceIn(1, 40))
        }
    }

    fun setThicknessPx(thicknessPx: Int) {
        updateSelectedLine { line ->
            line.copy(thicknessPx = thicknessPx.coerceIn(1, 40))
        }
    }

    fun setFullScreenSegment() {
        updateSelectedLine { line ->
            line.copy(startOffsetPx = 0, endOffsetPx = -1, isolatedTriggerPx = -1)
        }
        postMessage("Cobertura restaurada a línea completa de extremo a extremo")
    }

    fun updateSegmentRange(startPx: Int, endPx: Int) {
        updateSelectedLine { line ->
            val maxAxis = getMaxSegmentAxisFor(line)
            val clampedStart = startPx.coerceIn(0, maxAxis - 1)
            val clampedEnd = endPx.coerceIn(clampedStart + 1, maxAxis)
            line.copy(startOffsetPx = clampedStart, endOffsetPx = clampedEnd)
        }
    }

    fun nudgeSegmentStart(deltaPx: Int) {
        updateSelectedLine { line ->
            val maxAxis = getMaxSegmentAxisFor(line)
            val effectiveEnd = if (line.endOffsetPx < 0) maxAxis else line.endOffsetPx
            val nextStart = (line.startOffsetPx + deltaPx).coerceIn(0, (effectiveEnd - 1).coerceAtLeast(0))
            line.copy(startOffsetPx = nextStart, endOffsetPx = effectiveEnd)
        }
    }

    fun nudgeSegmentEnd(deltaPx: Int) {
        updateSelectedLine { line ->
            val maxAxis = getMaxSegmentAxisFor(line)
            val currentEnd = if (line.endOffsetPx < 0) maxAxis else line.endOffsetPx
            val nextEnd = (currentEnd + deltaPx).coerceIn(line.startOffsetPx + 1, maxAxis)
            line.copy(endOffsetPx = nextEnd)
        }
    }

    fun setLineOpacity(opacity: Float) {
        updateSelectedLine { line ->
            line.copy(opacity = opacity.coerceIn(0.10f, 1.0f))
        }
    }

    fun setLinePresetColor(preset: MaskPresetColor) {
        updateSelectedLine { line ->
            line.copy(maskColorHex = preset.hex)
        }
    }

    fun alignSelectedLineToSimulatedFault() {
        updateSelectedLine { line ->
            line.copy(
                orientation = LineOrientation.VERTICAL.name,
                positionPx = OverlayStateManager.SIMULATED_LINE_X_PX,
                isEnabled = true
            )
        }
        postMessage("Línea alineada con el fallo simulado en X=${OverlayStateManager.SIMULATED_LINE_X_PX}px")
    }

    // --- Trigger Pixel Scanner Logic ---

    fun startOrToggleScanner(enable: Boolean) {
        val target = getSelectedLine() ?: lines.value.firstOrNull() ?: return
        val maxAxis = getMaxSegmentAxisFor(target)
        if (!enable) {
            stopScanner()
            return
        }
        OverlayStateManager.updateScannerState { current ->
            current.copy(
                isActive = true,
                targetLineId = target.id,
                probeStartPx = current.probeStartPx.coerceIn(0, (maxAxis - current.probeLengthPx).coerceAtLeast(0)),
                binaryLowPx = 0,
                binaryHighPx = maxAxis,
                binaryTestingFirstHalf = true
            )
        }
    }

    fun stopScanner() {
        autoSweepJob?.cancel()
        autoSweepJob = null
        OverlayStateManager.updateScannerState {
            it.copy(isActive = false, isAutoSweeping = false)
        }
    }

    fun setScannerMethod(method: ScannerMethod) {
        autoSweepJob?.cancel()
        autoSweepJob = null
        val maxAxis = getMaxSegmentAxisFor(getSelectedLine())
        OverlayStateManager.updateScannerState {
            it.copy(
                isActive = true,
                targetLineId = selectedLineId.value ?: lines.value.firstOrNull()?.id,
                method = method,
                isAutoSweeping = false,
                binaryLowPx = 0,
                binaryHighPx = maxAxis,
                binaryTestingFirstHalf = true
            )
        }
    }

    fun setScannerProbeLength(lengthPx: Int) {
        val maxAxis = getMaxSegmentAxisFor(getSelectedLine())
        val validLength = lengthPx.coerceIn(1, 200)
        OverlayStateManager.updateScannerState {
            it.copy(
                probeLengthPx = validLength,
                probeStartPx = it.probeStartPx.coerceIn(0, (maxAxis - validLength).coerceAtLeast(0))
            )
        }
    }

    fun setScannerProbeStart(startPx: Int) {
        val maxAxis = getMaxSegmentAxisFor(getSelectedLine())
        OverlayStateManager.updateScannerState {
            val maxStart = (maxAxis - it.probeLengthPx).coerceAtLeast(0)
            it.copy(probeStartPx = startPx.coerceIn(0, maxStart))
        }
    }

    fun stepScannerProbe(deltaPx: Int) {
        val maxAxis = getMaxSegmentAxisFor(getSelectedLine())
        OverlayStateManager.updateScannerState {
            val maxStart = (maxAxis - it.probeLengthPx).coerceAtLeast(0)
            it.copy(
                isActive = true,
                targetLineId = selectedLineId.value ?: lines.value.firstOrNull()?.id,
                probeStartPx = (it.probeStartPx + deltaPx).coerceIn(0, maxStart)
            )
        }
    }

    fun toggleInvertScannerWindow(invert: Boolean) {
        OverlayStateManager.updateScannerState {
            it.copy(invertWindowMode = invert)
        }
    }

    fun toggleAutoSweep() {
        val current = scannerState.value
        if (current.isAutoSweeping) {
            autoSweepJob?.cancel()
            autoSweepJob = null
            OverlayStateManager.updateScannerState { it.copy(isAutoSweeping = false) }
            return
        }

        val target = getSelectedLine() ?: lines.value.firstOrNull() ?: return
        OverlayStateManager.updateScannerState {
            it.copy(
                isActive = true,
                targetLineId = target.id,
                method = ScannerMethod.AUTO_SWEEP,
                isAutoSweeping = true
            )
        }

        autoSweepJob?.cancel()
        autoSweepJob = viewModelScope.launch {
            while (isActive && scannerState.value.isAutoSweeping) {
                val state = scannerState.value
                val maxAxis = getMaxSegmentAxisFor(getSelectedLine())
                val maxStart = (maxAxis - state.probeLengthPx).coerceAtLeast(0)
                val nextPos = if (state.probeStartPx + state.sweepStepPx > maxStart) {
                    0
                } else {
                    state.probeStartPx + state.sweepStepPx
                }
                OverlayStateManager.updateScannerState {
                    it.copy(probeStartPx = nextPos)
                }
                delay(state.sweepIntervalMs)
            }
        }
    }

    fun setSweepSpeed(stepPx: Int, intervalMs: Long) {
        OverlayStateManager.updateScannerState {
            it.copy(
                sweepStepPx = stepPx.coerceIn(1, 30),
                sweepIntervalMs = intervalMs.coerceIn(30L, 400L)
            )
        }
    }

    // Binary search helpers
    fun toggleBinaryHalfSelection(firstHalf: Boolean) {
        OverlayStateManager.updateScannerState {
            it.copy(binaryTestingFirstHalf = firstHalf)
        }
    }

    fun confirmBinaryHalfContainsTrigger() {
        OverlayStateManager.updateScannerState { state ->
            val mid = state.currentBinaryMidPx
            val newLow = if (state.binaryTestingFirstHalf) state.binaryLowPx else mid
            val newHigh = if (state.binaryTestingFirstHalf) mid else state.binaryHighPx
            state.copy(
                binaryLowPx = newLow,
                binaryHighPx = newHigh.coerceAtLeast(newLow + 1),
                binaryTestingFirstHalf = true
            )
        }
    }

    fun resetBinarySearchRange() {
        val maxAxis = getMaxSegmentAxisFor(getSelectedLine())
        OverlayStateManager.updateScannerState {
            it.copy(
                binaryLowPx = 0,
                binaryHighPx = maxAxis,
                binaryTestingFirstHalf = true
            )
        }
    }

    /**
     * Convierte la posición actual del escáner en el recorte permanente de esa línea,
     * apagando únicamente ese píxel o micro-segmento y dejando el resto de la pantalla intacta.
     */
    fun lockCurrentProbeAsPermanentCrop() {
        val state = scannerState.value
        val segStart = state.activeSegmentStartPx
        val segEnd = state.activeSegmentEndPx
        stopScanner()

        updateSelectedLine { line ->
            val maxAxis = getMaxSegmentAxisFor(line)
            val clampedStart = segStart.coerceIn(0, maxAxis - 1)
            val clampedEnd = segEnd.coerceIn(clampedStart + 1, maxAxis)
            line.copy(
                isEnabled = true,
                startOffsetPx = clampedStart,
                endOffsetPx = clampedEnd,
                isolatedTriggerPx = (clampedStart + clampedEnd) / 2
            )
        }
        postMessage("Recorte fijado en ${segStart}px – ${segEnd}px. El resto de la línea queda libre.")
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            postMessage("Abre Ajustes > Accesibilidad > Aplicaciones instaladas > AMOLED Line Guard")
        }
    }

    fun requestAddQuickSettingsTile(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                statusBarManager?.requestAddTileService(
                    ComponentName(context, AmoledQuickTileService::class.java),
                    context.getString(R.string.quick_tile_label),
                    Icon.createWithResource(context, R.drawable.ic_quick_tile_mask),
                    context.mainExecutor
                ) { result ->
                    if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                        result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
                    ) {
                        postMessage("Botón 'Línea AMOLED' añadido a tus Ajustes Rápidos")
                    }
                }
            } catch (_: Exception) {
                postMessage("Baja el panel de Ajustes Rápidos, toca el lápiz (Editar) y añade 'Línea AMOLED'")
            }
        } else {
            postMessage("Baja la cortina de Ajustes Rápidos, toca Editar (+) y arrastra 'Línea AMOLED'")
        }
    }

    private fun getSelectedLine(): LineMaskEntity? {
        val id = selectedLineId.value
        return lines.value.find { it.id == id } ?: lines.value.firstOrNull()
    }

    fun getMaxSegmentAxisFor(line: LineMaskEntity?): Int {
        val metrics = displayInfo.value
        return if (line == null || line.isVertical) {
            metrics.heightPx
        } else {
            metrics.widthPx
        }
    }
}
