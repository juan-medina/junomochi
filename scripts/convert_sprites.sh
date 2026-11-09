#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Juan Medina
# SPDX-License-Identifier: MIT

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
VENV_DIR="$PROJECT_ROOT/.venv"
VENV_PYTHON="$VENV_DIR/bin/python"

# 1. Ensure .venv exists
if [ ! -f "$VENV_PYTHON" ]; then
    echo "[convert_sprites.sh] Initializing virtual environment in .venv..."
    python3 -m venv "$VENV_DIR" || python -m venv "$VENV_DIR"
fi

# 2. Run convert_sprites.py inside .venv
"$VENV_PYTHON" "$SCRIPT_DIR/convert_sprites.py" "$@"
