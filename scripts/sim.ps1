# SPDX-FileCopyrightText: 2026 Juan Medina
# SPDX-License-Identifier: MIT

[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$scoopJava = "$env:USERPROFILE\scoop\apps\openjdk27\current"
if (Test-Path "$scoopJava\bin\java.exe") {
    $env:JAVA_HOME = $scoopJava
    $env:Path = "$scoopJava\bin;" + $env:Path
}

& ".\mvnw.cmd" compile -q
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$junoJar = "$env:USERPROFILE\.m2\repository\io\github\jabrena\juno-api\0.1.0-SNAPSHOT\juno-api-0.1.0-SNAPSHOT.jar"
& java -cp "target\classes;$junoJar" com.juanmedina.junomochi.sim.DesktopSimulator @args

