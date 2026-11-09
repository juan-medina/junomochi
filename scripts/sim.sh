#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Juan Medina
# SPDX-License-Identifier: MIT

set -euo pipefail

./mvnw compile -q
JUNO_JAR="$HOME/.m2/repository/io/github/jabrena/juno-api/0.1.0-SNAPSHOT/juno-api-0.1.0-SNAPSHOT.jar"
java -cp "target/classes:$JUNO_JAR" com.juanmedina.junomochi.sim.DesktopSimulator "$@"

