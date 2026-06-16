#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
scenario="live_swarm_three_uav"
if [ "${1:-}" != "" ] && [[ "${1:-}" != --* ]]; then
  scenario="$1"
  shift
fi
exec "$repo_root/simulators/jneo-battlespace/scripts/run_scenario.sh" "$scenario" --backend JNEO_BATTLESPACE "$@"

