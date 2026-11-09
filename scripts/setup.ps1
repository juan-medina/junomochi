# SPDX-FileCopyrightText: 2026 Juan Medina
# SPDX-License-Identifier: MIT

[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

Write-Host "=== Setting up Junomochi Project ===" -ForegroundColor Cyan

# 1. Update git submodules
Write-Host "[1/4] Updating git submodules..." -ForegroundColor Yellow
git submodule update --init --recursive

# 2. Check / Setup Java 27+
Write-Host "[2/4] Checking Java..." -ForegroundColor Yellow
$scoopJava = "$env:USERPROFILE\scoop\apps\openjdk27\current"
if (Test-Path "$scoopJava\bin\java.exe") {
    $env:JAVA_HOME = $scoopJava
    $env:Path = "$scoopJava\bin;$env:USERPROFILE\scoop\shims;" + $env:Path
} else {
    $isJava27 = $false
    try {
        $output = & java -version 2>&1
        if ($output -match 'version "(27|[3-9][0-9])\.') {
            $isJava27 = $true
        }
    } catch {}

    if (-not $isJava27) {
        Write-Host "OpenJDK 27 is required. Installing via scoop..." -ForegroundColor Yellow
        scoop bucket add java
        scoop install openjdk27
        $env:JAVA_HOME = $scoopJava
        $env:Path = "$scoopJava\bin;$env:USERPROFILE\scoop\shims;" + $env:Path
    }
}
Write-Host "Java is configured: $env:JAVA_HOME" -ForegroundColor Green

# 3. Check / Setup Arduino CLI and Renesas Core
Write-Host "[3/4] Checking Arduino CLI and Renesas core..." -ForegroundColor Yellow
$cliCmd = Get-Command arduino-cli -ErrorAction SilentlyContinue
if (-not $cliCmd) {
    if (Test-Path "$env:USERPROFILE\scoop\apps\arduino-cli\current\arduino-cli.exe") {
        $env:Path = "$env:USERPROFILE\scoop\shims;" + $env:Path
    } else {
        Write-Host "Installing arduino-cli via scoop..." -ForegroundColor Yellow
        scoop install arduino-cli
        $env:Path = "$env:USERPROFILE\scoop\shims;" + $env:Path
    }
}
arduino-cli core update-index
arduino-cli core install arduino:renesas_uno
Write-Host "Arduino CLI and Renesas UNO core ready." -ForegroundColor Green

# 4. Build JUno core modules into local Maven cache
Write-Host "[4/4] Building JUno dependencies into local ~/.m2 repository..." -ForegroundColor Yellow
Push-Location juno
try {
    .\mvnw.cmd clean install -DskipTests "-Dspotless.check.skip=true" "-Dpmd.skip=true" -pl juno-api,juno-compiler,juno-maven-plugin -am
} finally {
    Pop-Location
}

Write-Host "`n=== Setup Complete! ===" -ForegroundColor Green
Write-Host "To flash your Arduino Uno R4 WiFi, connect the board and run:" -ForegroundColor Cyan
Write-Host "  .\mvnw.cmd compile juno:upload" -ForegroundColor White
