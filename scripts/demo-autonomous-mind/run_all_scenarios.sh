#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
OUTPUT_DIR="${1:-$ROOT_DIR/target/jneopallium-autonomous-mind}"
LAUNCHER="com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindFullRunLauncher"
MVN="${MAVEN_CMD:-mvn}"

cd "$ROOT_DIR"

"$MVN" -q -DskipTests install
"$MVN" -q -pl demos/demo-autonomousmind -am -DincludeScope=runtime dependency:build-classpath -Dmdep.outputFile=target/autonomous-mind-classpath.txt

WORKER_JAR="$ROOT_DIR/demos/demo-autonomousmind/target/demo-autonomousmind-1.0-SNAPSHOT.jar"
DEPS="$(cat "$ROOT_DIR/demos/demo-autonomousmind/target/autonomous-mind-classpath.txt")"
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

java -cp "$WORKER_JAR$PATH_SEPARATOR$DEPS" "$LAUNCHER" all --output "$OUTPUT_DIR"

echo "AutonomousMind summary: $OUTPUT_DIR/summary.json"
