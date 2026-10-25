# AMOLED Line Guard — Supresor y Escáner de Líneas para Pantallas AMOLED

Una utilidad de precisión de ultra-bajo consumo diseñada para pantallas Samsung AMOLED que permite apagar (`#000000`) o atenuar líneas verdes/defectuosas mediante superposición de extremo a extremo, aislar posibles píxeles detonantes por escaneo o recorte de segmentos, y alternar su estado desde el panel de Ajustes Rápidos sin notificaciones molestas.

### User Review & Critical Decisions

> [!IMPORTANT]
> Estas son las decisiones de diseño y arquitectura confirmadas según tus respuestas:

- **Superposición de Pantalla Completa sin Notificaciones**: Se utilizará un **Servicio de Accesibilidad (`TYPE_ACCESSIBILITY_OVERLAY`)** en lugar de una superposición convencional con servicio en primer plano. Esto permite cubrir incluso la barra de estado superior y la barra de navegación inferior sin mostrar ninguna notificación fija en tu celular.
- **Control Diario Silencioso**: Se incluirá un **Botón de Ajustes Rápidos (`Quick Settings Tile`)** en el panel deslizable del sistema para encender o pausar las máscaras al instante, además del control completo dentro de la aplicación.
- **Tres Modos de Corrección e Investigación**:
  1. **Máscara Completa / Atenuador Oscuro**: Apagado total en negro puro AMOLED (`#000000`) o atenuación regulable para ambientes con poca luz.
  2. **Recorte por Segmentos**: Control exacto del punto de inicio y fin de cada línea para ocultar únicamente la sección afectada o el píxel específico, dejando el resto de la imagen intacta.
  3. **Escáner Paso a Paso de Píxel Detonante**: Herramienta interactiva para recorrer el eje de la línea (píxel a píxel, barrido automático o búsqueda por mitades) y fijar únicamente el punto que desencadena el fallo si el panel responde al apagado selectivo.
- **Soporte Multilínea y Multieje**: Capacidad para crear, calibrar y activar múltiples líneas tanto **verticales** como **horizontales** de forma independiente.

---

### 1. Overview & Core Concept

- **Qué Hace**: Dibuja una capa de hardware estática, invisible al tacto, que apaga o atenúa con precisión submilimétrica (nivel de píxel físico nativo) las columnas o filas defectuosas de una pantalla AMOLED.
- **Para Quién Es**: Usuarios con pantallas OLED/AMOLED afectadas por líneas verticales u horizontales brillantes (verde, rosa, blanca) que buscan eliminar el deslumbramiento nocturno o probar el apagado selectivo de píxeles detonantes sin gastar batería ni recursos.
- **Valor Principal**: Precisión absoluta a nivel de `1 px` real del panel y consumo de CPU/RAM virtualmente nulo (`0% CPU` en reposo gracias al renderizado estático por `SurfaceFlinger`).

---

### 2. User Experience & Visual Design

- **Flujos Principales de Usuario**:
  1. **Activación Rápida y Permiso Único**: Al abrir la app por primera vez, una tarjeta de estado guía al usuario directamente al menú de Accesibilidad de Android para habilitar el motor de superposición de extremo a extremo.
  2. **Creación y Calibración de Precisión (D-Pad de Píxeles)**: El usuario añade una línea (Vertical u Horizontal) y la posiciona usando un deslizador grueso o botones de micro-ajuste (`-10 px`, `-1 px`, `+1 px`, `+10 px`) mientras observa cómo la máscara negra cubre la línea física real en tiempo real.
  3. **Recorte de Segmento**: Desde el panel de la línea activa, el usuario ajusta los deslizadores y botones de `Inicio (px)` y `Fin (px)` para acortar la línea vertical u horizontal al tramo exacto deseado.
  4. **Laboratorio de Escaneo de Píxel Detonante**: Una pestaña dedicada donde el usuario selecciona una línea ya alineada en su coordenada `X` (o `Y`) y lanza una sonda de prueba (de `1 px` a `20 px` de largo) que recorre la línea mediante avance manual paso a paso, barrido automático con velocidad regulable o división por mitades (búsqueda binaria). Si nota que en cierta coordenada la línea parpadea o disminuye, pulsa **"Fijar Recorte en Este Punto"** para guardar solo ese micro-segmento.
  5. **Interruptor en Ajustes Rápidos**: Desde cualquier aplicación o juego, el usuario baja la cortina de Ajustes Rápidos de Samsung y toca el botón **"Línea AMOLED"** para encender o pausar todas las máscaras sin abrir la app ni tener notificaciones activas.

- **Identidad Visual y Tema (True AMOLED Instrument)**:
  - *Dirección Estética*: **Instrumento Técnico de Calibración OLED** — interfaz oscura de alto contraste inspirada en equipos de diagnóstico óptico, pensada para no encender píxeles innecesarios ni deslumbrar en habitaciones a oscuras.
  - *Paleta de Colores*:
    - Fondo principal (`Background` / `Surface`): Negro Puro AMOLED (`#000000`) y Gris Carbón Profundo (`#0A0D0C`) para tarjetas elevadas.
    - Acento Primario (`Primary`): Verde Esmeralda Calibración (`#00E676`) para indicadores activos y controles de precisión.
    - Acento Secundario (`Secondary` / `Tertiary`): Ámbar Técnico (`#FFB300`) para el modo de Escaneo de Píxel Detonante y Cian Óptico (`#00B0FF`) para guías de coordenadas.
  - *Tipografía y Jerarquía*:
    - Encabezados y títulos: **Space Grotesk** para una lectura técnica y moderna.
    - Coordenadas de píxeles, contadores `X/Y` y telemetría: **JetBrains Mono** (monoespaciada) para evitar que los números salten al cambiar de píxel en vivo.
  - *Retroalimentación Interactiva*: Respuesta háptica sutil al pulsar los botones de micro-ajuste (`-1 px` / `+1 px`) y actualización instantánea a 0 ms en la capa superpuesta.

---

### 3. Key Product Decisions & Trade-Offs

- **Decisión 1: Renderizado de Superposición en `View` Nativa Estática vs. Jetpack Compose en el Overlay**
  - *Enfoque Elegido*: La app principal usa Jetpack Compose (Material 3), pero la ventana flotante del Servicio de Accesibilidad utiliza una única clase `View` nativa ultraligera de Android que dibuja directamente sobre `Canvas` solo cuando cambia un dato.
  - *Por Qué*: Una vista Compose en segundo plano mantiene un árbol de recomposición y mayor huella de memoria. Una `View` nativa sin animaciones continuas se dibuja una sola vez en el buffer gráfico; Android (`SurfaceFlinger`) la compone por hardware sin ejecutar código en la CPU en cada fotograma (`0% CPU`, gasto mínimo de batería y cero lag en juegos).
- **Decisión 2: Ventana `FLAG_NOT_TOUCHABLE` y `FLAG_NOT_FOCUSABLE` con Coordenadas Físicas Reales**
  - *Enfoque Elegido*: La ventana superpuesta nunca intercepta toques ni teclado, y utiliza `WindowMetrics` / `DisplayMetrics` reales de la pantalla completa (incluyendo el área del agujero de cámara / barra de estado y barra de navegación) en modo `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`.
  - *Por Qué*: Garantiza que una línea vertical se cubra desde el píxel `0` superior hasta el último píxel inferior del panel Samsung sin bloquear ningún gesto del sistema.
- **Decisión 3: Almacenamiento Local Reactivo con Room Database**
  - *Enfoque Elegido*: Base de datos local Room + `Flow` compartido con el Servicio de Accesibilidad y el `TileService`.
  - *Por Qué*: Las coordenadas exactas de los píxeles dañados pertenecen exclusivamente al panel físico de este dispositivo. Room permite guardar múltiples líneas y perfiles de calibración de forma instantánea, totalmente offline y sin requerir inicio de sesión ni permisos de red.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Diagrama de Arquitectura y Componentes**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     CAPA DE PRESENTACIÓN Y CONTROL                      │
│                                                                         │
│  ┌───────────────────────────────┐    ┌──────────────────────────────┐  │
│  │   App Principal (Compose M3)  │    │  Botón de Ajustes Rápidos    │  │
│  │  • Lista de Líneas (V / H)    │    │       (QuickSettingsTile)    │  │
│  │  • Micro-Control D-Pad (1px)  │    │  • Alternar ON/OFF global    │  │
│  │  • Recorte de Segmentos       │    │  • Sin notificaciones        │  │
│  │  • Escáner de Píxel Detonante │    └──────────────┬───────────────┘  │
│  └───────────────┬───────────────┘                   │                  │
└──────────────────┼───────────────────────────────────┼──────────────────┘
                   │                                   │
                   ▼                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                 ESTADO REACTIVO Y PERSISTENCIA LOCAL                    │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │           OverlayStateManager & LineProfileRepository             │  │
│  │  • StateFlow<OverlayConfig> en memoria compartida del proceso     │  │
│  │  • Persistencia Room (LineMaskEntity) + Preferencias Globales     │  │
│  │  • Motor de Escaneo (Paso manual, Barrido corrutina, Binario)     │  │
│  └───────────────────────────────┬───────────────────────────────────┘  │
└──────────────────────────────────┼──────────────────────────────────────┘
                                   │ (Emisión solo al cambiar coordenadas)
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│             MOTOR DE SUPERPOSICIÓN DE CERO RECURSOS (HARDWARE)          │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │        AmoledMaskAccessibilityService (AccessibilityService)      │  │
│  │  • WindowManager: TYPE_ACCESSIBILITY_OVERLAY                      │  │
│  │  • Flags: NOT_TOUCHABLE | NOT_FOCUSABLE | LAYOUT_IN_SCREEN        │  │
│  │  • Cutout: LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS                   │  │
│  │  • HardwareOverlayView: Canvas.drawRect() estático (#000000/Alpha)│  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

- **Modelo de Datos (`LineMaskEntity`)**:
  - `id`: Identificador único de la línea.
  - `name`: Etiqueta descriptiva (ej. "Línea Verde Derecha").
  - `isEnabled`: Estado individual activo/inactivo.
  - `orientation`: `VERTICAL` (eje X fijo, recorte en Y) u `HORIZONTAL` (eje Y fijo, recorte en X).
  - `positionPx`: Coordenada exacta en píxeles físicos desde el borde izquierdo (si es vertical) o superior (si es horizontal).
  - `thicknessPx`: Ancho de la máscara en píxeles (`1 px` a `30 px`).
  - `startOffsetPx` y `endOffsetPx`: Punto de inicio y fin del segmento a lo largo de la pantalla (`0` hasta la altura/anchura máxima del panel).
  - `opacity`: Nivel de oscuridad (`1.0f` para Negro Puro `#000000` que apaga el píxel AMOLED, o `0.15f–0.95f` para modo atenuador nocturno).
  - `colorHex`: Por defecto `#000000` (apagado AMOLED), con opción de subpíxel complementario (magenta oscuro/rojo/azul) para pruebas de cancelación óptica.

- **Mapeo Interactivo de Componentes y Estado**:
  - **Conmutador Maestro y Quick Settings Tile**: Actualiza `isGlobalOverlayEnabled`; cuando se apaga, el servicio remueve la vista del `WindowManager` liberando el 100% de la ventana; cuando se enciende, la vuelve a adjuntar.
  - **D-Pad de Precisión (`-10`, `-1`, `+1`, `+10 px`)**: Modifica `positionPx` o `thicknessPx` con límites validados según la resolución física real del dispositivo (`RealDisplayMetrics`) e invoca `invalidate()` únicamente en ese instante.
  - **Modo Escáner de Píxel Detonante**: Toma la línea seleccionada y reemplaza temporalmente su segmento completo por una ventana móvil `[probeStartPx, probeStartPx + probeSizePx]`, permitiendo mover la sonda con botones paso a paso, ejecutar un barrido automático pausable o dividir el rango a la mitad (búsqueda binaria superior/inferior) y aplicar el hallazgo como recorte definitivo.
