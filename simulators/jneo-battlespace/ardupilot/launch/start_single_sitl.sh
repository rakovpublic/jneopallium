#!/usr/bin/env bash
set -euo pipefail

instance="${INSTANCE:-0}"
sysid="${SYSID_THISMAV:-1}"
model="${MODEL:-gazebo-iris}"
params="${PARAMS:-simulators/jneo-battlespace/ardupilot/params/jneo-fpv-copter.parm}"

exec sim_vehicle.py \
  -v ArduCopter \
  -f "$model" \
  -I "$instance" \
  --sysid "$sysid" \
  --add-param-file "$params" \
  --no-rebuild \
  --out "udp:127.0.0.1:$((14550 + instance * 10))"

