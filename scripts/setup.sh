#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Juan Medina
# SPDX-License-Identifier: MIT

set -euo pipefail

echo "=== Setting up Junomichi Project ==="

# 1. Update git submodules
echo "[1/4] Updating git submodules..."
git submodule update --init --recursive

# 2. Check Java
echo "[2/4] Checking Java..."
if ! command -v java >/dev/null 2>&1; then
    echo "Error: Java is not installed. Please install OpenJDK 27+ (e.g., via sdkman: sdk install java 27-tem, or brew install openjdk@27)."
    exit 1
fi

# 3. Check Arduino CLI and core
echo "[3/4] Checking Arduino CLI..."
if ! command -v arduino-cli >/dev/null 2>&1; then
    echo "Installing arduino-cli via official script..."
    curl -fsSL https://raw.githubusercontent.com/arduino/arduino-cli/master/install.sh | sh
    export PATH="$PATH:$HOME/bin"
fi

arduino-cli core update-index
arduino-cli core install arduino:renesas_uno

# 4. Build JUno core modules into local Maven cache
echo "[4/4] Building JUno dependencies into local ~/.m2 repository..."
(
    cd juno
    ./mvnw clean install -DskipTests -Dspotless.check.skip=true -Dpmd.skip=true -pl juno-api,juno-compiler,juno-maven-plugin -am
)

echo ""
echo "=== Setup Complete! ==="
echo "To flash your Arduino Uno R4 WiFi, connect the board and run:"
echo "  ./mvnw compile juno:upload"
