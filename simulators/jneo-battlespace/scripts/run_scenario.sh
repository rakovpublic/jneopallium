#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root="$(cd "$script_dir/.." && pwd)"
python_bin="${PYTHON:-python3}"
if ! command -v "$python_bin" >/dev/null 2>&1; then
  python_bin="python"
fi
export GZ_SIM_RESOURCE_PATH="$root/models:${GZ_SIM_RESOURCE_PATH:-}"

scenario="${1:-all}"
shift || true
exec "$python_bin" "$root/supervisor/process_supervisor.py" "$scenario" "$@"
