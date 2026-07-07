#!/usr/bin/env bash
# Live demo: label-free detection + continuous feedback learning, on the real
# Java neurons. Python generates a telemetry replay; Java runs the deployed model.
set -euo pipefail
cd "$(dirname "$0")"
DEMO_DIR="$(pwd)"
ROOT=$(cd ../../.. && pwd)

WIN=0; SEP=":"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) WIN=1; SEP=";";; esac
w() { if [ "$WIN" = 1 ]; then cygpath -w "$1"; else printf '%s' "$1"; fi; }

python make_demo_replay.py

CLASSES="$ROOT/worker/target/classes"
# reuse a dependency classpath from a prior build if present, else ask Maven
CP_FILE=""
for f in "$ROOT/worker/target/ssmaint-demo-cp.txt" "$ROOT/worker/target/industrial-cp.txt"; do
  [ -f "$f" ] && CP_FILE="$f" && break
done
if [ ! -d "$CLASSES" ] || [ -z "$CP_FILE" ]; then
  echo "Compiling worker and resolving classpath via Maven..."
  CP_FILE="$ROOT/worker/target/ssmaint-demo-cp.txt"
  ( cd "$ROOT" && mvn -q -pl worker -am compile \
      && mvn -q -pl worker dependency:build-classpath -Dmdep.outputFile="$CP_FILE" )
fi
DEPCP="$(cat "$CP_FILE")"          # maven-generated, native to this platform

# compile the ssmaint neurons from source + the demo, so this runs from a fresh
# checkout without a full Maven build; build/ goes first on the classpath
BUILD="$DEMO_DIR/build"; mkdir -p "$BUILD"
SRCS=""
for s in $(find "$ROOT/worker/src/main/java/com/rakovpublic/jneuropallium/worker" \
             -path '*ssmaint*' -name '*.java') "$DEMO_DIR/SelfSupervisedMaintenanceDemo.java"; do
  SRCS="$SRCS $(w "$s")"
done
CLASSES_C="$(w "$CLASSES")"; BUILD_C="$(w "$BUILD")"

javac -cp "${CLASSES_C}${SEP}${DEPCP}" -d "$BUILD_C" $SRCS
java -cp "${BUILD_C}${SEP}${CLASSES_C}${SEP}${DEPCP}" SelfSupervisedMaintenanceDemo \
  "$(w "$ROOT/worker/src/main/resources/model/self-supervised-maintenance/fitted-model.json")" \
  "$(w "$ROOT/target/jneopallium-ss-maintenance-demo/replay.json")"
