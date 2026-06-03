#!/usr/bin/env bash
set -euo pipefail

SCENARIO_ID="${1:-baseline_foraging}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
OUTPUT_DIR="${2:-$ROOT_DIR/target/jneopallium-autonomous-ai-demo}"
LAUNCHER="com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime.AutonomousAiDemoLauncher"
MVN="${MAVEN_CMD:-mvn}"

cd "$ROOT_DIR"

"$MVN" -q -DskipTests=false install
"$MVN" -q -pl worker -DincludeScope=runtime dependency:build-classpath -Dmdep.outputFile=target/autonomous-ai-classpath.txt

WORKER_JAR="$ROOT_DIR/worker/target/worker-1.0-SNAPSHOT.jar"
DEPS="$(cat "$ROOT_DIR/worker/target/autonomous-ai-classpath.txt")"
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

echo "Autonomous AI demo manifest: $OUTPUT_DIR/$SCENARIO_ID/manifest.json"
