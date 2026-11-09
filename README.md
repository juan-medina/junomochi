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
  - **Pixel Agent Office:** An animated pixel character walks around the room, paths to the desk, sits on the chair, types code on the PC with an animated screen, stands up, and wanders again.
  - **SD Card Sprite Streaming:** Streams sprite assets on demand from the microSD card into a single reusable 1.5 KB buffer, keeping RAM consumption under 10 KB on the Uno R4.
  - **Auto-sleep / Wake-on-touch:** Turns screen backlight off after 3 minutes of inactivity; instantly wakes and resets the timer when the screen is touched.

## MicroSD Card Setup

The project streams pixel graphics from the microSD card inserted into the Waveshare TFT shield (`SD_CS`: Pin 5).

1. Format a microSD card as **FAT32** (or FAT16).
2. Copy the contents of the [`sdcard/`](sdcard/) folder directly to the **root** directory of your microSD card:
   - `palette.bin`: Shared RGB565 color palette
   - `desk.bin`: Desk front sprite (48×32 px)
   - `chair.bin`: Cushioned chair sprite (16×16 px)
   - `pc_off.bin`, `pc_on1.bin`, `pc_on2.bin`: Animated PC monitor sprites (16×32 px)
   - `ch_d0.bin`, `ch_d1.bin`, `ch_d2.bin`: Character walk down frames (16×32 px)
   - `ch_u0.bin`, `ch_u1.bin`, `ch_u2.bin`: Character walk up frames (16×32 px)
   - `ch_r0.bin`, `ch_r1.bin`, `ch_r2.bin`: Character walk right frames (16×32 px, flipped for left)
   - `ch_t0.bin`, `ch_t1.bin`: Character typing / coding frames (16×32 px)
3. Insert the card into the shield's SD slot before powering on. If the card is missing or unreadable, the screen will display `[SD CARD ERROR!]`.

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
