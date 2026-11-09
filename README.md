<!--
SPDX-FileCopyrightText: 2026 Juan Medina
SPDX-License-Identifier: MIT
-->

# Junomichi 🐾

A Tamagotchi-like virtual pet running natively on an **Arduino UNO R4 WiFi** using ahead-of-time (AOT) Java compilation with [JUNo](https://github.com/jabrena/juno).

Junomichi animates an interactive pet on the built-in 12×8 LED matrix, with idle breathing, blinking, excited jumping, eating snacks, and sleeping with floating Zs.

## Requirements

- **Java Development Kit (JDK) 27+**
- **Arduino CLI**
- **Git**

## Quick Start

### 1. Setup

The setup script verifies Java 27 and Arduino CLI, installs the Renesas UNO core (`arduino:renesas_uno`), and builds JUno compiler dependencies into your local Maven repository:

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

Connect your Arduino UNO R4 WiFi via USB and flash:

**Windows (PowerShell):**
```powershell
.\scripts\flash.ps1
```

Or specify the COM port manually if needed:
```powershell
.\scripts\flash.ps1 -Port COM3
```

**macOS / Linux:**
```bash
./scripts/flash.sh /dev/ttyACM0
```

Alternatively using Maven directly:
```bash
./mvnw compile juno:upload
```

## Pet Life Cycle & Animations

- **Idle**: Gentle breathing and eye blinking.
- **Happy**: Bouncing jumping animation and heart sparkle.
- **Eating**: A snack drifts in and the pet munches it down.
- **Sleep**: Closed resting eyes with drifting 'Z' animations.
- **Heartbeat**: Onboard pin 13 LED pulses to indicate pet life.

## License

MIT License. See [LICENSE](LICENSE) for details.
