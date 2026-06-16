#!/usr/bin/env bash
set -u

missing=0
preview=0
if [ "${1:-}" = "--preview" ]; then
  preview=1
fi

print_header() {
  printf '\n== %s ==\n' "$1"
}

fail() {
  name="$1"
  guidance="$2"
  printf '[MISSING] %s\n' "$name"
  printf '  guidance: %s\n' "$guidance"
  missing=1
}

pass() {
  name="$1"
  version="$2"
  printf '[OK] %s\n' "$name"
  printf '  version: %s\n' "$version"
}

check_cmd() {
  name="$1"
  cmd="$2"
  guidance="$3"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    fail "$name" "$guidance"
    return
  fi
  version="$("$cmd" --version 2>&1 | head -n 1)"
  pass "$name" "$version"
}

print_header "Platform"
uname -a
if [ "$(uname -s)" != "Linux" ]; then
  fail "Linux live platform" "Run the live backend on Linux or invoke it through WSL2 from Windows."
fi

print_header "Core tools"
if command -v java >/dev/null 2>&1; then
  java_line="$(java -version 2>&1 | head -n 1)"
  java_major="$(printf '%s' "$java_line" | sed -E 's/.*version "([0-9]+).*/\1/')"
  if [ "$java_major" -ge 17 ] 2>/dev/null; then
    pass "Java 17+" "$java_line"
  else
    fail "Java 17+" "Install Java 17 or newer."
  fi
else
  fail "Java 17+" "Install Java 17 or newer and ensure java is on PATH."
fi
check_cmd "Maven" mvn "Install Apache Maven and ensure mvn is on PATH."
if command -v python3 >/dev/null 2>&1; then
  pass "Python 3" "$(python3 --version 2>&1)"
else
  fail "Python 3" "Install Python 3 and ensure python3 is on PATH."
fi

print_header "ArduPilot"
if [ -n "${ARDUPILOT_HOME:-}" ] && [ -d "$ARDUPILOT_HOME" ]; then
  pass "ArduPilot source" "$ARDUPILOT_HOME"
elif [ -n "${ARDUPILOT_ROOT:-}" ] && [ -d "$ARDUPILOT_ROOT" ]; then
  pass "ArduPilot source" "$ARDUPILOT_ROOT"
elif [ -d "$HOME/ardupilot" ]; then
  pass "ArduPilot source" "$HOME/ardupilot"
else
  fail "ArduPilot source" "Clone ArduPilot and set ARDUPILOT_HOME to the checkout."
fi
if command -v sim_vehicle.py >/dev/null 2>&1; then
  pass "sim_vehicle.py" "$(command -v sim_vehicle.py)"
else
  fail "sim_vehicle.py" "Install ArduPilot tools and ensure sim_vehicle.py is on PATH."
fi

print_header "Gazebo Harmonic"
if command -v gz >/dev/null 2>&1; then
  pass "gz command" "$(gz --version 2>&1 | head -n 1)"
else
  fail "gz command" "Install Gazebo Harmonic and ensure gz is on PATH."
fi
if [ -n "${ARDUPILOT_GAZEBO_PLUGIN_PATH:-}" ] && [ -e "$ARDUPILOT_GAZEBO_PLUGIN_PATH" ]; then
  pass "ardupilot_gazebo plugin" "$ARDUPILOT_GAZEBO_PLUGIN_PATH"
else
  fail "ardupilot_gazebo plugin" "Build the official ArduPilot Gazebo plugin and set ARDUPILOT_GAZEBO_PLUGIN_PATH."
fi

print_header "ROS 2"
if command -v ros2 >/dev/null 2>&1; then
  pass "ROS 2" "$(ros2 --help 2>&1 | head -n 1)"
  if ros2 pkg prefix ros_gz_bridge >/dev/null 2>&1; then
    pass "ros_gz_bridge" "$(ros2 pkg prefix ros_gz_bridge)"
  else
    fail "ros_gz_bridge" "Install ros_gz_bridge for your ROS 2 distribution."
  fi
  if ros2 pkg prefix rosbridge_server >/dev/null 2>&1; then
    pass "rosbridge_suite" "$(ros2 pkg prefix rosbridge_server)"
  else
    fail "rosbridge_suite" "Install rosbridge_suite when using the existing Jneopallium ROS 2 bridge."
  fi
else
  fail "ROS 2" "Install ROS 2 and source install/setup.bash before running."
  fail "ros_gz_bridge" "Install ros_gz_bridge after ROS 2 is available."
  fail "rosbridge_suite" "Install rosbridge_suite after ROS 2 is available."
fi

if [ "$preview" -eq 1 ]; then
  print_header "Operator preview"
  if command -v gst-launch-1.0 >/dev/null 2>&1; then
    pass "GStreamer" "$(gst-launch-1.0 --version 2>&1 | head -n 1)"
  else
    fail "GStreamer" "Install GStreamer or run without --preview."
  fi
fi

if [ "$missing" -ne 0 ]; then
  printf '\nJNeoBattlespace live backend is unavailable.\n'
  exit 1
fi

printf '\nJNeoBattlespace live dependency check passed.\n'

