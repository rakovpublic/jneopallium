#!/usr/bin/env bash
# Runs the Redis cluster demo: one master, N workers, all model and signal state in Redis.
#
# Every master->worker assignment is a neuron range plus Redis coordinates - never model or
# signal data - so the payload stays a few hundred bytes whatever the model size.
#
# Requires a redis-server on 127.0.0.1:6379 (no modules needed) and maven on PATH.
set -euo pipefail

WORKERS=${WORKERS:-3}
LAYERS=${LAYERS:-3}
NEURONS=${NEURONS:-600}
RESULT_NEURONS=${RESULT_NEURONS:-50}
EPOCHS=${EPOCHS:-3}
PARTITIONS=${PARTITIONS:-4}
THREADS=${THREADS:-2}
REDIS_PORT=${REDIS_PORT:-6379}
MASTER_PORT=${MASTER_PORT:-8080}
NET=${NET:-demo09}
SKIP_BUILD=${SKIP_BUILD:-0}

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
run_dir="$repo_root/target/demo-cluster-redis"
master_url="http://127.0.0.1:$MASTER_PORT"
pids=()
mkdir -p "$run_dir"

cleanup() {
  for pid in "${pids[@]:-}"; do
    kill "$pid" 2>/dev/null || true
  done
}
trap cleanup EXIT

phase() { printf '\n=== %s ===\n' "$1"; }

phase "Redis"
redis-cli -p "$REDIS_PORT" ping

if [ "$SKIP_BUILD" != "1" ]; then
  phase "Build"
  mvn -B -q -DskipTests -f "$repo_root/pom.xml" install
fi

classpath_file="$run_dir/worker-classpath.txt"
[ -f "$classpath_file" ] || mvn -B -q -f "$repo_root/pom.xml" -pl worker dependency:build-classpath "-Dmdep.outputFile=$classpath_file"
worker_cp="$repo_root/worker/target/classes:$(cat "$classpath_file")"
worker_jar_url="file://$repo_root/worker/target/worker-1.0-SNAPSHOT.jar"
context_path="$run_dir/context.json"
printf '{"host":"127.0.0.1","port":%s,"neuronNetName":"%s"}' "$REDIS_PORT" "$NET" > "$context_path"

phase "Phase 1 - seed the model into Redis"
java -cp "$worker_cp" com.rakovpublic.jneuropallium.worker.demo.cluster.ClusterDemoSeeder \
  --host 127.0.0.1 --port "$REDIS_PORT" --net "$NET" --layers "$LAYERS" --neurons "$NEURONS" \
  --result "$RESULT_NEURONS" --epochs "$EPOCHS" --partitions "$PARTITIONS" \
  --master "$master_url" --threads "$THREADS" --flush

phase "Phase 2 - start the master"
java -jar "$repo_root/master/target/jneuronnetmaster.war" "--server.port=$MASTER_PORT" \
  > "$run_dir/master.log" 2>&1 &
pids+=($!)
for _ in $(seq 1 120); do
  curl -sf "$master_url/debug/state" >/dev/null && break
  sleep 1
done
echo "master up on $master_url"

phase "Phase 3 - configure the master"
java -cp "$worker_cp" com.rakovpublic.jneuropallium.worker.demo.cluster.ClusterDemoConfigurator \
  --host 127.0.0.1 --port "$REDIS_PORT" --net "$NET" --master "$master_url" \
  --partitions "$PARTITIONS" --threads "$THREADS"

phase "Phase 4 - what the master actually sends a worker"
payload=$(curl -s -X POST -H 'Content-Type: application/json' \
  -d '{"nodeName":"payload-probe"}' "$master_url/nodeManager/nextRun")
echo "$payload"
echo "payload: ${#payload} bytes for a model of $((LAYERS * NEURONS + RESULT_NEURONS)) neurons"

phase "Phase 5 - attach $WORKERS workers"
for i in $(seq 1 "$WORKERS"); do
  java -cp "$worker_cp" "-Djneuropallium.node.name=worker-$i" \
    -Dorg.apache.logging.log4j.level=INFO \
    com.rakovpublic.jneuropallium.worker.application.Entry \
    http "$worker_jar_url" \
    com.rakovpublic.jneuropallium.worker.util.RedisContext "$context_path" \
    > "$run_dir/worker-$i.log" 2>&1 &
  pids+=($!)
  echo "worker-$i started, log: $run_dir/worker-$i.log"
done

phase "Phase 6 - concurrent partitions"
for _ in $(seq 1 120); do
  state=$(curl -s "$master_url/debug/state")
  if [ "$(grep -o '"idle":false' <<< "$state" | wc -l)" -ge 2 ]; then
    echo "$state"
    break
  fi
  sleep 1
done

phase "Phase 7 - results"
for _ in $(seq 1 300); do
  for epoch in 0 1 2 3 4 5 6; do
    for loop in 0 1 2; do
      results=$(curl -sf "$master_url/nodeManager/getResults?loop=$loop&epoch=$epoch" || true)
      if [ -n "$results" ]; then
        echo "epoch $epoch loop $loop -> $(grep -o '"neuronId"' <<< "$results" | wc -l) result neurons"
        exit 0
      fi
    done
  done
  sleep 1
done
echo "No results yet; inspect the logs in $run_dir" >&2
exit 1
