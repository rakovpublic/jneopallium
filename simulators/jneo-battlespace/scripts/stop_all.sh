#!/usr/bin/env bash
set -euo pipefail

patterns=(
  "gz sim"
  "sim_vehicle.py"
  "arducopter"
  "ros_gz_bridge"
  "rosbridge_websocket"
  "jneo-battlespace"
)

for pattern in "${patterns[@]}"; do
  if pgrep -f "$pattern" >/dev/null 2>&1; then
    pkill -TERM -f "$pattern" || true
  fi
done

sleep 2

for pattern in "${patterns[@]}"; do
  if pgrep -f "$pattern" >/dev/null 2>&1; then
    pkill -KILL -f "$pattern" || true
  fi
done

printf 'JNeoBattlespace stop request completed.\n'

