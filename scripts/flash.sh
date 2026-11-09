#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Juan Medina
# SPDX-License-Identifier: MIT

set -euo pipefail

PORT="${1:-}"

# 1. Compile Java to Arduino sketch using Juno
echo "[flash.sh] Compiling Java with Juno..."
./mvnw clean compile juno:compile

# 2. Detect port if not specified
if [ -z "$PORT" ]; then
    PORT=$(arduino-cli board list | grep -E "UNO R4" | awk '{print $1}' | head -n1 || true)
fi

# 3. Compile sketch with arduino-cli
echo "[flash.sh] Compiling sketch with arduino-cli..."
arduino-cli compile --fqbn arduino:renesas_uno:unor4wifi target/juno/AppAsm

# 4. Upload sketch
if [ -n "$PORT" ]; then
    echo "[flash.sh] Uploading to $PORT..."
    arduino-cli upload -p "$PORT" --fqbn arduino:renesas_uno:unor4wifi target/juno/AppAsm
else
    echo "[flash.sh] Uploading..."
    arduino-cli upload --fqbn arduino:renesas_uno:unor4wifi target/juno/AppAsm
fi
