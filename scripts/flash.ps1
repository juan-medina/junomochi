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

# 2. Patch AppShim.cpp to fix SD SPI clock speed for Renesas Uno R4 WiFi
$shimPath = "target\juno\AppAsm\AppShim.cpp"
if (Test-Path $shimPath) {
    $content = [System.IO.File]::ReadAllText($shimPath)
    $oldBegin = 'if (juno_sd.begin(chipSelectPin)) return 1;'
    $newBegin = @'
    if (juno_sd.begin(SdSpiConfig(chipSelectPin, SHARED_SPI, SD_SCK_MHZ(4)))) {
      return 1;
    }
    if (juno_sd.begin(SdSpiConfig(chipSelectPin, SHARED_SPI, SD_SCK_MHZ(1)))) {
      return 1;
    }
    if (juno_sd.begin(chipSelectPin)) {
      return 1;
    }
'@
    if ($content.Contains($oldBegin)) {
        $content = $content.Replace($oldBegin, $newBegin)
        [System.IO.File]::WriteAllText($shimPath, $content)
        Write-Host "[flash.ps1] Patched AppShim.cpp with 4MHz/1MHz SPI fallback."
    }
}

# 3. Detect port if not specified
if (-not $Port) {
    $boards = arduino-cli board list | Out-String
    if ($boards -match '(COM\d+)\s+serial.*UNO R4') {
        $Port = $Matches[1]
    } elseif ($boards -match '(COM\d+)') {
        $Port = $Matches[1]
    }
}

# 4. Compile sketch with arduino-cli
Write-Host "[flash.ps1] Compiling sketch with arduino-cli..."
arduino-cli compile --fqbn arduino:renesas_uno:unor4wifi target\juno\AppAsm
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# 5. Upload sketch
if ($Port) {
    Write-Host "[flash.ps1] Uploading to $Port..."
    arduino-cli upload -p $Port --fqbn arduino:renesas_uno:unor4wifi target\juno\AppAsm
} else {
    Write-Host "[flash.ps1] Uploading..."
    arduino-cli upload --fqbn arduino:renesas_uno:unor4wifi target\juno\AppAsm
}
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
