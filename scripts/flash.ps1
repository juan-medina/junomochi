# SPDX-FileCopyrightText: 2026 Juan Medina
# SPDX-License-Identifier: MIT

[CmdletBinding()]
param(
    [string]$Port
)

$ErrorActionPreference = "Stop"

# 1. Compile Java to Arduino sketch using Juno
Write-Host "[flash.ps1] Compiling Java with Juno..."
& ".\mvnw.cmd" clean compile juno:compile
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# 2. Detect port if not specified
if (-not $Port) {
    $boards = arduino-cli board list | Out-String
    if ($boards -match '(COM\d+)\s+serial.*UNO R4') {
        $Port = $Matches[1]
    } elseif ($boards -match '(COM\d+)') {
        $Port = $Matches[1]
    }
}

# 3. Compile sketch with arduino-cli
Write-Host "[flash.ps1] Compiling sketch with arduino-cli..."
arduino-cli compile --fqbn arduino:renesas_uno:unor4wifi target\juno\AppAsm
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# 4. Upload sketch
if ($Port) {
    Write-Host "[flash.ps1] Uploading to $Port..."
    arduino-cli upload -p $Port --fqbn arduino:renesas_uno:unor4wifi target\juno\AppAsm
} else {
    Write-Host "[flash.ps1] Uploading..."
    arduino-cli upload --fqbn arduino:renesas_uno:unor4wifi target\juno\AppAsm
}
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
