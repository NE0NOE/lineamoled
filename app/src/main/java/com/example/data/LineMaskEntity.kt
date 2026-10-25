package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LineOrientation {
    VERTICAL,
    HORIZONTAL
}

enum class MaskPresetColor(val label: String, val hex: Int, val description: String) {
    PURE_BLACK("Negro Puro AMOLED (#000000)", 0xFF000000.toInt(), "Apaga físicamente el píxel por completo (0V)"),
    ANTI_GREEN_MAGENTA("Filtro Magenta Oscuro", 0xFF1E001E.toInt(), "Atenúa el subpíxel verde manteniendo rojos/azules tenues"),
    DEEP_NIGHT_WARM("Atenuador Ámbar Nocturno", 0xFF140B00.toInt(), "Suaviza el deslumbramiento en lectura nocturna"),
    DEEP_OPTICAL_BLUE("Filtro Azul Profundo", 0xFF000C1D.toInt(), "Contraste frío para bordes de barra de estado")
}

@Entity(tableName = "line_masks")
data class LineMaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isEnabled: Boolean = true,
    val orientation: String = LineOrientation.VERTICAL.name,
    val positionPx: Int = 540,
    val thicknessPx: Int = 2,
    val startOffsetPx: Int = 0,
    val endOffsetPx: Int = -1, // -1 means full screen height/width
    val opacity: Float = 1.0f, // 1.0f = pure opaque blackout, < 1.0f = dimmer mode
    val maskColorHex: Int = 0xFF000000.toInt(),
    val isolatedTriggerPx: Int = -1, // Saved trigger pixel coordinate if isolated
    val createdAt: Long = System.currentTimeMillis()
) {
    val isVertical: Boolean
        get() = orientation == LineOrientation.VERTICAL.name
}
