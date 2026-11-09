# SPDX-FileCopyrightText: 2026 Juan Medina
# SPDX-License-Identifier: MIT

[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ScriptArgs
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$venvDir = Join-Path $projectRoot ".venv"
$venvPython = Join-Path $venvDir "Scripts\python.exe"

# 1. Ensure .venv exists
if (-not (Test-Path $venvPython)) {
    Write-Host "[convert_sprites.ps1] Initializing virtual environment in .venv..." -ForegroundColor Cyan
    & python -m venv $venvDir
}

# 2. Run convert_sprites.py inside .venv
$scriptPath = Join-Path $PSScriptRoot "convert_sprites.py"
& $venvPython $scriptPath @ScriptArgs
