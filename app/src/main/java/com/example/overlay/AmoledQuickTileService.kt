package com.example.overlay

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.data.OverlayStateManager

/**
 * Botón en el Panel de Ajustes Rápidos (Quick Settings Tile) para activar/pausar
 * la máscara AMOLED al instante sin tener notificaciones en la barra de estado.
 */
class AmoledQuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        OverlayStateManager.initialize(applicationContext)
        syncTileUi()
    }

    override fun onClick() {
        super.onClick()
        OverlayStateManager.initialize(applicationContext)
        OverlayStateManager.toggleMasterEnabled(applicationContext)
        syncTileUi()
    }

    private fun syncTileUi() {
        val tile = qsTile ?: return
        val isMasterOn = OverlayStateManager.isMasterEnabled.value
        val isAccessibilityReady = OverlayStateManager.refreshAccessibilityStatus(applicationContext)

        tile.state = if (isMasterOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Línea AMOLED"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                !isMasterOn -> "En pausa"
                isAccessibilityReady -> "Máscara activa"
                else -> "Vista en app"
            }
        }
        tile.updateTile()
    }
}
