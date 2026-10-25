package com.example.data

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import com.example.overlay.AmoledMaskAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScannerMethod(val title: String, val subtitle: String) {
    STEP_BY_STEP(
        "Paso a Paso Manual",
        "Desplaza un bloque negro píxel a píxel para detectar si un punto apaga la línea"
    ),
    AUTO_SWEEP(
        "Barrido Automático",
        "Recorre automáticamente el eje vertical u horizontal hasta que pulses ¡Detener!"
    ),
    BINARY_SEARCH(
        "Búsqueda por Mitades",
        "Divide la línea en mitad superior/inferior sucesivamente para aislar el píxel rápido"
    )
}

data class PixelScannerState(
    val isActive: Boolean = false,
    val targetLineId: Long? = null,
    val method: ScannerMethod = ScannerMethod.STEP_BY_STEP,
    val probeStartPx: Int = 200,
    val probeLengthPx: Int = 8,
    val isAutoSweeping: Boolean = false,
    val sweepStepPx: Int = 4,
    val sweepIntervalMs: Long = 90L,
    // Binary search bounds along the line axis
    val binaryLowPx: Int = 0,
    val binaryHighPx: Int = 1920,
    val binaryTestingFirstHalf: Boolean = true,
    // If false: only turns off the probe block [probeStartPx..probeStartPx+probeLengthPx]
    // to test if turning off that trigger pixel kills the rest of the green line.
    // If true: blacks out the entire line EXCEPT the probe window.
    val invertWindowMode: Boolean = false
) {
    val currentBinaryMidPx: Int
        get() = (binaryLowPx + binaryHighPx) / 2

    val activeSegmentStartPx: Int
        get() = if (method == ScannerMethod.BINARY_SEARCH) {
            if (binaryTestingFirstHalf) binaryLowPx else currentBinaryMidPx
        } else {
            probeStartPx
        }

    val activeSegmentEndPx: Int
        get() = if (method == ScannerMethod.BINARY_SEARCH) {
            if (binaryTestingFirstHalf) currentBinaryMidPx else binaryHighPx
        } else {
            probeStartPx + probeLengthPx
        }
}

data class DisplayHardwareInfo(
    val widthPx: Int = 1080,
    val heightPx: Int = 2400,
    val densityDpi: Int = 420,
    val refreshRateHz: Int = 120
)

object OverlayStateManager {
    private const val PREFS_NAME = "amoled_guard_prefs"
    private const val KEY_MASTER_ENABLED = "master_overlay_enabled"
    private const val KEY_CALIBRATION_GUIDES = "calibration_guides_enabled"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var initialized = false

    private val _isMasterEnabled = MutableStateFlow(true)
    val isMasterEnabled: StateFlow<Boolean> = _isMasterEnabled.asStateFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val _showCalibrationGuides = MutableStateFlow(true)
    val showCalibrationGuides: StateFlow<Boolean> = _showCalibrationGuides.asStateFlow()

    // Optional visual simulator of a faulty green AMOLED line (with trigger pixel at Y=480)
    // so the user can test precision alignment and trigger-pixel isolation safely
    private val _simulateFaultyGreenLine = MutableStateFlow(false)
    val simulateFaultyGreenLine: StateFlow<Boolean> = _simulateFaultyGreenLine.asStateFlow()
    const val SIMULATED_LINE_X_PX = 540
    const val SIMULATED_TRIGGER_Y_PX = 480

    private val _lines = MutableStateFlow<List<LineMaskEntity>>(emptyList())
    val lines: StateFlow<List<LineMaskEntity>> = _lines.asStateFlow()

    private val _selectedLineId = MutableStateFlow<Long?>(null)
    val selectedLineId: StateFlow<Long?> = _selectedLineId.asStateFlow()

    private val _scannerState = MutableStateFlow(PixelScannerState())
    val scannerState: StateFlow<PixelScannerState> = _scannerState.asStateFlow()

    private val _displayInfo = MutableStateFlow(DisplayHardwareInfo())
    val displayInfo: StateFlow<DisplayHardwareInfo> = _displayInfo.asStateFlow()

    private val _redrawCounter = MutableStateFlow(0L)
    val redrawCounter: StateFlow<Long> = _redrawCounter.asStateFlow()

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        refreshDisplayMetrics(appContext)
        refreshAccessibilityStatus(appContext)

        if (initialized) return
        initialized = true

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isMasterEnabled.value = prefs.getBoolean(KEY_MASTER_ENABLED, true)
        _showCalibrationGuides.value = prefs.getBoolean(KEY_CALIBRATION_GUIDES, true)

        val dao = AppDatabase.getInstance(appContext).lineMaskDao()
        scope.launch {
            if (dao.getLineCount() == 0) {
                val metrics = _displayInfo.value
                val defaultX = (metrics.widthPx / 2).coerceAtLeast(100)
                val initialId = dao.insertLine(
                    LineMaskEntity(
                        name = "Línea Verde Principal",
                        isEnabled = true,
                        orientation = LineOrientation.VERTICAL.name,
                        positionPx = defaultX,
                        thicknessPx = 2,
                        startOffsetPx = 0,
                        endOffsetPx = -1,
                        opacity = 1.0f,
                        maskColorHex = MaskPresetColor.PURE_BLACK.hex
                    )
                )
                _selectedLineId.value = initialId
            }
            dao.observeAllLines().collect { dbLines ->
                _lines.value = dbLines
                if (_selectedLineId.value == null || dbLines.none { it.id == _selectedLineId.value }) {
                    _selectedLineId.value = dbLines.firstOrNull()?.id
                }
                _redrawCounter.update { it + 1 }
            }
        }
    }

    fun refreshDisplayMetrics(context: Context) {
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            val display = wm.defaultDisplay
            @Suppress("DEPRECATION")
            display.getRealMetrics(metrics)
            @Suppress("DEPRECATION")
            val refreshRate = display.refreshRate.toInt().coerceAtLeast(60)

            _displayInfo.value = DisplayHardwareInfo(
                widthPx = metrics.widthPixels.coerceAtLeast(320),
                heightPx = metrics.heightPixels.coerceAtLeast(640),
                densityDpi = metrics.densityDpi,
                refreshRateHz = refreshRate
            )
        } catch (_: Exception) {
        }
    }

    fun refreshAccessibilityStatus(context: Context): Boolean {
        val enabled = isAccessibilityServiceEnabledInSettings(context)
        _isServiceConnected.value = enabled
        return enabled
    }

    fun onServiceConnectionChanged(connected: Boolean) {
        _isServiceConnected.value = connected
        _redrawCounter.update { it + 1 }
    }

    private fun isAccessibilityServiceEnabledInSettings(context: Context): Boolean {
        return try {
            val expectedComponent = ComponentName(context, AmoledMaskAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return _isServiceConnected.value
            enabledServices.split(':').any {
                ComponentName.unflattenFromString(it) == expectedComponent
            }
        } catch (_: Exception) {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                ?.any { it.resolveInfo.serviceInfo.packageName == context.packageName } == true
        }
    }

    fun setMasterEnabled(context: Context, enabled: Boolean) {
        _isMasterEnabled.value = enabled
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MASTER_ENABLED, enabled)
            .apply()
        _redrawCounter.update { it + 1 }
    }

    fun toggleMasterEnabled(context: Context): Boolean {
        val next = !_isMasterEnabled.value
        setMasterEnabled(context, next)
        return next
    }

    fun setShowCalibrationGuides(context: Context, show: Boolean) {
        _showCalibrationGuides.value = show
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CALIBRATION_GUIDES, show)
            .apply()
        _redrawCounter.update { it + 1 }
    }

    fun setSimulateFaultyGreenLine(simulate: Boolean) {
        _simulateFaultyGreenLine.value = simulate
        _redrawCounter.update { it + 1 }
    }

    fun selectLine(lineId: Long) {
        _selectedLineId.value = lineId
        _redrawCounter.update { it + 1 }
    }

    fun updateScannerState(transform: (PixelScannerState) -> PixelScannerState) {
        _scannerState.update(transform)
        _redrawCounter.update { it + 1 }
    }
}
