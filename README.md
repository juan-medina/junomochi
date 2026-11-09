<!--
SPDX-FileCopyrightText: 2026 Juan Medina
SPDX-License-Identifier: MIT
-->

# Junomochi

Arduino UNO R4 WiFi project using ahead-of-time (AOT) Java compilation with [JUNo](https://github.com/jabrena/juno).

## Requirements

- **Java Development Kit (JDK) 27+**
- **Arduino CLI**
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
