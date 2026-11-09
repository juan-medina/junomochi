# SPDX-FileCopyrightText: 2026 Juan Medina
# SPDX-License-Identifier: MIT

[CmdletBinding()]
param(
    [string]$Port
)

$ErrorActionPreference = "Stop"

$argsList = @("compile", "juno:upload")
if ($Port) {
    $argsList += "-Djuno.port=$Port"
}

& ".\mvnw.cmd" @argsList
