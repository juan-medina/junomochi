#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Juan Medina
# SPDX-License-Identifier: MIT

set -euo pipefail

PORT="${1:-}"

if [ -n "$PORT" ]; then
    ./mvnw compile juno:upload "-Djuno.port=$PORT"
else
    ./mvnw compile juno:upload
fi
