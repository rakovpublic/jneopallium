#!/usr/bin/env bash
set -euo pipefail

SCENARIO="${1:-all}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PYTHON_BIN="${PYTHON_BIN:-python3}"

cd "$ROOT"

command -v "$PYTHON_BIN" >/dev/null
command -v java >/dev/null
command -v mvn >/dev/null

mvn -q -pl worker -DskipTests package
"$PYTHON_BIN" -m pip install -q -r scripts/demo-industrial-fmi/requirements.txt
"$PYTHON_BIN" scripts/demo-industrial-fmi/run_demo.py "$SCENARIO"
