<!--
SPDX-FileCopyrightText: 2026 Juan Medina
SPDX-License-Identifier: MIT
-->

# Junomochi

Arduino UNO R4 WiFi project using ahead-of-time (AOT) Java compilation with [JUNo](https://github.com/jabrena/juno).

## Hardware Setup

- **Microcontroller:** Arduino UNO R4 WiFi
- **Shield:** Waveshare 2.8" TFT Touch LCD Shield (320x240, ST7789V / ILI9341 over SPI via ICSP header)
- **Pin Out:**
  - `LCD_CS`: Pin 10
  - `LCD_DC`: Pin 7
  - `LCD_BL`: Pin 9 (Backlight PWM / digital on-off)
  - `TP_CS`: Pin 4 (XPT2046 Touch Chip Select, held HIGH)
  - `TP_IRQ`: Pin 3 (XPT2046 Touch Interrupt line, active-low)
  - `SD_CS`: Pin 5 (MicroSD Chip Select, held HIGH)

## Architecture & Features

The project includes a lightweight, zero-allocation driver implementing Juno's standard `Display` interface:

- [`WaveshareTftShield.java`](src/main/java/com/juanmedina/junomochi/WaveshareTftShield.java):
  - Implements `io.github.jabrena.juno.hardware.display.Display`
  - Zero runtime heap allocation (`ArduinoUno.getSpi()` and `DigitalOutput` intrinsics)
  - Supports `fillScreen`, `fillRect`, `drawPixel`, `drawSprite`, `print`, `println`, `setCursor`, `setTextSize`
  - Backlight power management: `setBacklight(boolean on)`
  - Touch detection: `isTouched()` (via Pin 3 `TP_IRQ`)
- [`App.java`](src/main/java/com/juanmedina/junomochi/App.java):
  - Test scene with retro sprite, text header, and RGB565 color bars
  - **Auto-sleep / Wake-on-touch:** Turns screen backlight off after 5 seconds of inactivity; instantly wakes and resets the timer when the screen is touched.

## Requirements

- **Java Development Kit (JDK) 27+**
- **Arduino CLI** (with `arduino:renesas_uno` core)
- **Git**

## Quick Start

### 1. Setup

**Windows (PowerShell):**
```powershell
.\scripts\setup.ps1
```

**macOS / Linux:**
```bash
chmod +x ./scripts/*.sh ./mvnw
./scripts/setup.sh
```

### 2. Compile and Flash

**Windows (PowerShell):**
```powershell
.\scripts\flash.ps1
```

**macOS / Linux:**
```bash
./scripts/flash.sh
```

Alternatively using Maven directly:
```bash
./mvnw compile juno:upload
```

## License

MIT License. See [LICENSE](LICENSE) for details.
