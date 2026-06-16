#!/usr/bin/env bash
set -euo pipefail

SCENARIO_ID="${1:-all}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
OUTPUT_DIR="${2:-$ROOT_DIR/target/jneopallium-uav-single}"
LAUNCHER="com.rakovpublic.jneuropallium.worker.demo.uavsingle.UavSingleDemoLauncher"
MVN="${MAVEN_CMD:-mvn}"
SCENARIOS=(
  autonomous_success
  autonomous_priority_change
  confirm_approved
  confirm_denied
  confirm_timeout
  low_battery_rth
  geofence_veto
  lost_heartbeat
  poor_visibility
  duplicate_confirmation
)

cd "$ROOT_DIR"

"$MVN" -q -pl worker -Dtest=UavSingle*Test test
"$MVN" -q -DskipTests install
"$MVN" -q -pl worker -DincludeScope=runtime dependency:build-classpath -Dmdep.outputFile=target/uav-single-classpath.txt

WORKER_JAR="$ROOT_DIR/worker/target/worker-1.0-SNAPSHOT.jar"
DEPS="$(cat "$ROOT_DIR/worker/target/uav-single-classpath.txt")"
PATH_SEPARATOR=":"
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*)
    PATH_SEPARATOR=";"
    if command -v cygpath >/dev/null 2>&1; then
      WORKER_JAR="$(cygpath -w "$WORKER_JAR")"
      OUTPUT_DIR="$(cygpath -w "$OUTPUT_DIR")"
    fi
    ;;
esac

java -cp "$WORKER_JAR$PATH_SEPARATOR$DEPS" "$LAUNCHER" "$SCENARIO_ID" --output "$OUTPUT_DIR"

if [[ ! -f "$OUTPUT_DIR/summary.json" ]]; then
  echo "missing UAV single summary: $OUTPUT_DIR/summary.json" >&2
  exit 1
fi

if [[ "$SCENARIO_ID" == "all" ]]; then
  for scenario in "${SCENARIOS[@]}"; do
    [[ -f "$OUTPUT_DIR/$scenario/manifest.json" ]] || {
      echo "missing UAV single manifest: $OUTPUT_DIR/$scenario/manifest.json" >&2
      exit 1
    }
  done
else
  [[ -f "$OUTPUT_DIR/$SCENARIO_ID/manifest.json" ]] || {
    echo "missing UAV single manifest: $OUTPUT_DIR/$SCENARIO_ID/manifest.json" >&2
    exit 1
  }
fi

echo "UAV single summary:  $OUTPUT_DIR/summary.json"
echo "UAV single artifacts: $OUTPUT_DIR"
