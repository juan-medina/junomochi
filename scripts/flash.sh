#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Juan Medina
# SPDX-License-Identifier: MIT

set -euo pipefail

PORT="${1:-}"

# 1. Compile Java to Arduino sketch using Juno
echo "[flash.sh] Compiling Java with Juno..."
./mvnw compile juno:compile

# 2. Patch AppShim.cpp to fix SD SPI clock speed for Renesas Uno R4 WiFi
SHIM_PATH="target/juno/AppAsm/AppShim.cpp"
if [ -f "$SHIM_PATH" ]; then
    python3 -c "
with open('$SHIM_PATH', 'r') as f:
    c = f.read()
old = 'if (juno_sd.begin(chipSelectPin)) return 1;'
new = '''    if (juno_sd.begin(SdSpiConfig(chipSelectPin, SHARED_SPI, SD_SCK_MHZ(4)))) {
      return 1;
    }
    if (juno_sd.begin(SdSpiConfig(chipSelectPin, SHARED_SPI, SD_SCK_MHZ(1)))) {
      return 1;
    }
    if (juno_sd.begin(chipSelectPin)) {
      return 1;
    }'''
if old in c:
    c = c.replace(old, new)
    with open('$SHIM_PATH', 'w') as f:
        f.write(c)
    print('[flash.sh] Patched AppShim.cpp with 4MHz/1MHz SPI fallback.')
" || true
fi

# 3. Detect port if not specified
if [ -z "$PORT" ]; then
    PORT=$(arduino-cli board list | grep -E "UNO R4" | awk '{print $1}' | head -n1 || true)
fi

# 4. Compile sketch with arduino-cli
echo "[flash.sh] Compiling sketch with arduino-cli..."
arduino-cli compile --fqbn arduino:renesas_uno:unor4wifi target/juno/AppAsm

# 5. Upload sketch
if [ -n "$PORT" ]; then
    echo "[flash.sh] Uploading to $PORT..."
    arduino-cli upload -p "$PORT" --fqbn arduino:renesas_uno:unor4wifi target/juno/AppAsm
else
    echo "[flash.sh] Uploading..."
    arduino-cli upload --fqbn arduino:renesas_uno:unor4wifi target/juno/AppAsm
fi
