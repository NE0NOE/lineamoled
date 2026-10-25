package com.example.overlay

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.example.data.OverlayStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Servicio de Accesibilidad para superposición AMOLED de pantalla completa (incluyendo barra
 * de estado superior, cámara perforada y barra de navegación) sin mostrar notificaciones.
 */
class AmoledMaskAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var windowManager: WindowManager? = null
    private var overlayView: HardwareMaskOverlayView? = null
    private var isViewAttached = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        OverlayStateManager.initialize(applicationContext)
        OverlayStateManager.onServiceConnectionChanged(true)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        serviceScope.launch {
            OverlayStateManager.isMasterEnabled.collectLatest { enabled ->
                if (enabled) {
                    attachOverlayIfNeeded()
                    overlayView?.invalidate()
                } else {
                    detachOverlayIfNeeded()
                }
            }
        }

        serviceScope.launch {
            OverlayStateManager.redrawCounter.collectLatest {
                if (OverlayStateManager.isMasterEnabled.value) {
                    attachOverlayIfNeeded()
                    overlayView?.invalidate()
                }
            }
        }
    }

    private fun attachOverlayIfNeeded() {
        if (isViewAttached) return
        val wm = windowManager ?: return

        OverlayStateManager.refreshDisplayMetrics(this)
        val view = overlayView ?: HardwareMaskOverlayView(this).also { overlayView = it }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        try {
            wm.addView(view, params)
            isViewAttached = true
        } catch (_: Exception) {
            isViewAttached = false
        }
    }

    private fun detachOverlayIfNeeded() {
        if (!isViewAttached) return
        val wm = windowManager ?: return
        val view = overlayView ?: return
        try {
            wm.removeView(view)
        } catch (_: Exception) {
        } finally {
            isViewAttached = false
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        OverlayStateManager.refreshDisplayMetrics(this)
        overlayView?.invalidate()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intencionalmente vacío: android:accessibilityEventTypes no está suscrito
        // para garantizar 0% de uso de CPU en segundo plano.
    }

    override fun onInterrupt() {
        detachOverlayIfNeeded()
    }

    override fun onDestroy() {
        detachOverlayIfNeeded()
        OverlayStateManager.onServiceConnectionChanged(false)
        serviceScope.cancel()
        super.onDestroy()
    }
}
