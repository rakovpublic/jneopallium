#!/usr/bin/env bash
set -euo pipefail

plugin_root="${ARDUPILOT_GAZEBO_ROOT:-${HOME}/ardupilot_gazebo}"
if [ ! -d "$plugin_root" ]; then
  printf 'Missing ardupilot_gazebo checkout: %s\n' "$plugin_root" >&2
  printf 'Clone the official ArduPilot Gazebo plugin and set ARDUPILOT_GAZEBO_ROOT.\n' >&2
  exit 1
fi

cmake -S "$plugin_root" -B "$plugin_root/build"
cmake --build "$plugin_root/build" --parallel

printf 'Build complete. Set ARDUPILOT_GAZEBO_PLUGIN_PATH to the built plugin path before running live scenarios.\n'

