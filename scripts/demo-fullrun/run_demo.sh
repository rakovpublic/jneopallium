#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "usage: scripts/demo-fullrun/run_demo.sh <demo-id> [output-dir]" >&2
  exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
DEMO_ID="$1"
OUTPUT_DIR="${2:-$ROOT_DIR/target/jneopallium-fullrun-demos}"
LAUNCHER="com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoFullRunLauncher"
MVN="${MAVEN_CMD:-mvn}"

cd "$ROOT_DIR"

"$MVN" -q -DskipTests=false clean install
"$MVN" -q -pl worker -DincludeScope=runtime dependency:build-classpath -Dmdep.outputFile=target/fullrun-classpath.txt

WORKER_JAR="$ROOT_DIR/worker/target/worker-1.0-SNAPSHOT.jar"
DEPS="$(cat "$ROOT_DIR/worker/target/fullrun-classpath.txt")"
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

java -cp "$WORKER_JAR$PATH_SEPARATOR$DEPS" "$LAUNCHER" "$DEMO_ID" --output "$OUTPUT_DIR"

echo "Full-run demo manifest: $OUTPUT_DIR/$DEMO_ID/manifest.json"
