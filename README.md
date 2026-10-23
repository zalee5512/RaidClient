# ⚡ Raid Client & Raid Launcher (Minecraft 1.8.9)

Proyecto completo compuesto por:
1. **Raid Client**: Cliente de Minecraft 1.8.9 optimizado para PvP con menú HUD y mods integrados.
2. **Raid Launcher**: Lanzador de escritorio moderno con soporte para cuentas Premium (Microsoft) y No-Premium (Offline).

---

## 🚀 Inicio Rápido

Para abrir el launcher directamente:
* Ejecuta el archivo: `Iniciar_RaidLauncher.bat` o navega a `raid-launcher/bin/Debug/net10.0-windows/RaidLauncher.exe`.

---

## 🎮 Raid Client (En el Juego)

### Controles y Menú
* **Abrir Menú de Mods / ClickGUI**: Presiona la tecla **Shift Derecho (`RIGHT SHIFT`)**.
* **Editar HUD (Arrastrar elementos)**: Desde el ClickGUI haz click en el botón **"Editar HUD"** en la esquina superior derecha, arrastra los módulos con el ratón a donde quieras y presiona `ESC` para guardar.

### Módulos Incluidos
* **Custom Crosshair**:
  * Cambia color RGBA (Cian, Verde, Rojo, etc.), tamaño de mira, grosor de líneas, espacio central y punto central.
  * Anula automáticamente la mira vanilla para mostrar una mira limpia y precisa.
* **Hitboxes Viewer**:
  * Muestra las cajas de colisión sin necesidad de presionar F3+B.
  * Grosor de línea configurable, opción de relleno translúcido y vector de línea de visión (hacia dónde mira el jugador).
* **FPS Counter**:
  * Contador de cuadros por segundo con diseño redondeado oscuro y opción Chroma RGB.
* **CPS Counter**:
  * Contador de clicks por segundo independiente para click izquierdo y click derecho.
* **Keystrokes**:
  * Teclas WASD, Espacio y Clicks LMB / RMB con animación y color de iluminación al presionar.
* **ToggleSprint**:
  * Mantiene la carrera activa automáticamente sin desgastar la tecla de correr.
* **Fullbright**:
  * Visión clara en cuevas y noches sin necesidad de antorchas.

---

## 💻 Raid Launcher (Aplicación de Escritorio)

* **Cuentas No-Premium**:
  * Pestaña **Cuentas** -> Ingresa tu nombre en **Agregar No-Premium** y haz click en el botón.
* **Cuentas Premium**:
  * Pestaña **Cuentas** -> Haz click en **Iniciar con Microsoft** -> Se abrirá la web de Microsoft con tu código de dispositivo para autenticarte de forma segura.
* **Asignación de RAM**:
  * Pestaña **Ajustes** -> Selecciona con el slider desde 1 GB hasta 16 GB.
* **Optimizaciones de JVM para PvP**:
  * Incluye flags de recolector de basura G1GC optimizados para evitar tirones y congelamientos durante el combate.

---

## 🛠️ Estructura del Código Fuente

```
c:\Programacion\
├── Iniciar_RaidLauncher.bat         <- Acceso directo para abrir el launcher
├── raid-client\                     <- Código fuente de Minecraft 1.8.9 (Java / Forge & Mixins)
│   ├── src\main\java\com\raidclient\
│   │   ├── RaidClient.java          <- Clase principal del cliente
│   │   ├── gui\ClickGUI.java        <- Menú de Shift Derecho
│   │   ├── gui\HUDConfigScreen.java <- Editor de HUD arrastrable
│   │   ├── mod\impl\                <- Módulos (Crosshair, Hitbox, FPS, Keystrokes, etc.)
│   │   └── mixin\client\            <- Mixins inyectados en Minecraft
│   └── build\libs\RaidClient-1.0.jar<- JAR compilado y listo
└── raid-launcher\                   <- Launcher de escritorio (.NET 10 / WPF)
    ├── MainWindow.xaml              <- Interfaz gráfica moderna estilo Lunar Client
    ├── Services\GameLaunchService.cs<- Gestor de lanzamiento y copiado de mods
    └── Services\MicrosoftAuthService.cs <- Autenticación oficial de Microsoft OAuth
```
