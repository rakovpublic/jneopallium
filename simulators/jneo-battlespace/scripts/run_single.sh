#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
scenario="${1:-live_single_autonomous}"
shift || true
exec "$script_dir/run_scenario.sh" "$scenario" --backend JNEO_BATTLESPACE "$@"

