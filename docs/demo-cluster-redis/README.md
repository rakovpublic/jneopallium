# Demo 09 — Distributed cluster on Redis (1 master + N workers)

> Runtime: **`master` (Spring Boot) + N × `worker` in `http` cluster mode** ·
> Shared state: **stock Redis 5+ (no modules)** ·
> Status: **implemented and verified** — see [§8](#8-verified-behaviour) ·
> Run it: `scripts\demo-cluster-redis\run_cluster_demo.ps1`

Unlike the existing demos (`demo-01` … `demo-08`, `docs/demo-fullrun/`), which all run a
single JVM in `local` mode, this demo exercises the *cluster* path: `HttpClusterApplication`
on the workers, `NodeManagerController` / `InputService` on the master, and Redis as the single
shared store for **both** signal state and neuron/layer configuration.

The point being demonstrated is a **control-plane property**, not a modelling one: the master
hands each worker a *coordinate* — `(layerId, start, end)` plus Redis connection details — and
never ships neuron JSON or signal payloads over HTTP. Both sides resolve the coordinate against
Redis, so the assignment payload is constant in the size of the model.

Measured on the reference model: **336 bytes per assignment for a 1 850-neuron net**, and the
same 336 bytes when the net is ten times larger.

---

## 1. What it demonstrates

| Property | Where it shows up |
|---|---|
| Layer config (neurons, weights, axon wiring) lives in Redis, not in the master's heap | `RedisLayersMeta` / `RedisLayerMeta` |
| Signal state lives in Redis, shared by all workers | `RedisSignalStorage`, `RedisSignalHistoryStorage` |
| Master→worker payload is a **neuron range**, ~340 B, independent of model size | `RedisSplitInput` |
| The whole model is 11 Redis keys | `ClusterDemoSeeder` |
| Horizontal scale: 1 → N workers on the same model with no config change | `run_cluster_demo.ps1 -Workers N` |
| Workers hold **disjoint** ranges of the same layer at the same time | `GET /debug/state` |
| Elastic membership: a worker joining mid-run gets the next partition | `InputService.getNext` |
| Fault tolerance: a stalled worker's partition is reassigned after `nodeTimeout` | `InputService.reclaimTimedOutPartition` |
| Storage choice is configuration, not code | `POST /configuration/update` |
| Every intermediate state is inspectable with `redis-cli` | [§4](#4-redis-key-layout) |

---

## 2. Topology

```
                      ┌─────────────────────────────┐
                      │  master (Spring Boot :8080) │
                      │  ConfigurationService       │
                      │  InputService  ← scheduler  │
                      │  NodeManager   ← membership │
                      └──┬───────────────────────┬──┘
        POST /nodeManager/register               │  reads layer sizes only
        POST /nodeManager/nextRun  ──────────────┼──────────┐
        POST /input/callback       ◄─────────────┘          │
                      │                                     ▼
        336 B split-input JSON                     ┌──────────────────┐
              │        │        │                  │   Redis          │
              ▼        ▼        ▼                  │                  │
       ┌────────┐ ┌────────┐ ┌────────┐            │ layers, neurons, │
       │worker-1│ │worker-2│ │worker-3│ ═════════► │ signals, history │
       │ http   │ │ http   │ │ http   │  neurons + │ input queue      │
       └────────┘ └────────┘ └────────┘  signals   └──────────────────┘
```

A worker is started with three coordinates and nothing else; it reads the master address and
its thread count from the `demo09_properties` hash:

```bash
java -cp <worker runtime classpath> \
  com.rakovpublic.jneuropallium.worker.application.Entry \
  http file:///<worker jar> \
  com.rakovpublic.jneuropallium.worker.util.RedisContext \
  <path to context.json>
```

`context.json` is `{"host":"127.0.0.1","port":6379,"neuronNetName":"demo09"}`. `Runner` accepts
either the JSON itself or a path to it; the demo passes a path, because a shell will otherwise
strip the quotes out of an inline JSON argument.

---

## 3. Running it

### One-time: a local Redis

The demo needs plain Redis — no modules, no Docker. On Windows the portable build works
without an installer or admin rights:

```bash
curl -L -o redis.zip https://github.com/tporadowski/redis/releases/download/v5.0.14.1/Redis-x64-5.0.14.1.zip
```

Unzip it to `%LOCALAPPDATA%\jneopallium-demo\redis` and put a `redis-demo.conf` next to it in
`%LOCALAPPDATA%\jneopallium-demo`:

```
port 6379
bind 127.0.0.1
save ""
appendonly no
```

The script starts the server itself if nothing answers on the port. On Linux/macOS any
`redis-server` on `127.0.0.1:6379` will do.

### The demo

```bash
powershell -File scripts/demo-cluster-redis/run_cluster_demo.ps1 -Workers 3 -Neurons 600
```

Useful switches: `-SkipBuild` after the first run, `-KeepRunning` to leave the cluster up for
poking at with `redis-cli`, `-Neurons 6000` to show the payload is unchanged on a model ten
times the size.

### Phases

| # | Phase | What the audience sees |
|---|---|---|
| 1 | **Seed** | `ClusterDemoSeeder` writes the whole model into **11 Redis keys** and queues the input batches |
| 2 | **Start master** | boots knowing nothing about the model |
| 3 | **Configure** | one JSON request naming the Redis storage classes; the master then lists the layers and their sizes, read from Redis |
| 4 | **Payload** | one `nextRun` call, printed verbatim, with its byte count next to the neuron count |
| 5 | **Attach workers** | N workers register and start pulling partitions |
| 6 | **Concurrency** | `/debug/state` shows the workers holding disjoint ranges of the same layer |
| 7 | **Results** | `/nodeManager/getResults` returns the result-layer neurons for a completed epoch |

### Reference model

| Layer | Position | Neuron ids | Count | Role |
|---|---|---|---|---|
| `-2147483648` | 0 | `0` | 1 | cycle/control layer, created by the loading strategy |
| `0` | 1 | `1 000 000 – 1 000 599` | 600 | ingest |
| `1` | 2 | `2 000 000 – 2 000 599` | 600 | features |
| `2` | 3 | `3 000 000 – 3 000 599` | 600 | scoring |
| `2147483647` | 4 | `9 000 000 – 9 000 049` | 50 | result layer |

Neuron ids are layer-prefixed on purpose: a range in a log line says which layer it belongs to
without a lookup. The model itself is deliberately trivial — `ClusterSignalProcessor` scales a
value and the result stage turns it into an advisory decision — because the demo is about how
work is distributed, not about what is computed.

---

## 4. Redis key layout

Only plain Redis types; **no RedisJSON**, so stock Redis works.

| Key | Type | Contents |
|---|---|---|
| `demo09_properties` | HASH | `master.address`, `worker.threads.amount`, … — read by `RedisContext` |
| `demo09_layerIds` | LIST | layer ids in processing order (cycle layer first, result layer excluded) |
| `demo09_layer_neurons_<L>` | HASH | `neuronId → neuron JSON` |
| `demo09_layer_index_<L>` | ZSET | `score = member = neuronId` — the range index |
| `demo09_layer_meta_<L>` | HASH | layer meta params |
| `demo09_signals_<L>` | HASH | `neuronId → JSON array of pending signals` |
| `demo09_history_<L>_<epoch>_<loop>_<neuronId>` | STRING | one past step of one neuron |
| `demo09_input_<name>` | LIST | one element per input batch; `LPOP` per population |

A partition fetch is two round trips:

```
ZRANGE demo09_layer_index_1 150 299      # ranks, not ids: the master partitions by index
HMGET  demo09_layer_neurons_1 <ids…>
```

Signals are stored per neuron rather than per layer, so two workers writing different
partitions of the same layer never overwrite each other. Writes are batched per partition:
one `HMGET` + one `HSET` for the signals, one pipeline for the neurons.

---

## 5. Components

| Component | Package | Responsibility |
|---|---|---|
| `RedisSplitInput` | `worker.net.signals.storage.redis` | the assignment: `{host, port, neuronNetName, threads, layerId, start, end, run, loop, cycleNeuronMapping, nodeIdentifier}` and nothing else |
| `RedisInputResolver` | `worker.net.layers.impl.redis` | worker-side run state; replaces `InMemoryInputResolver`, which holds the input loading strategy — and with it the whole input history — as a field |
| `RedisLayerMeta` / `RedisLayersMeta` / `RedisResultLayerMeta` | `worker.net.layers.impl.redis` | layer configuration over HASH + ZSET |
| `RedisLayer` | `worker.net.layers.impl.redis` | the layer handle a neuron sees while a worker processes it |
| `RedisSignalStorage` / `RedisSignalHistoryStorage` | `worker.net.signals.storage.redis` | pending signals and history |
| `RedisInitInput` | `worker.net.signals.storage.redis` | input source over a Redis list (was a stub returning `null`) |
| `RedisClientFactory` / `RedisKeys` / `SignalJson` | `worker.util` | one pooled client per endpoint, the key layout, signal (de)serialisation |
| `ClusterDemoSeeder` / `ClusterDemoConfigurator` / `ClusterSignalProcessor` / `ClusterResultLayerRunner` | `worker.demo.cluster` | the demo model, its configuration and its arithmetic |
| `DebugController` | `master.controllers` | `GET /debug/state` — nodes, their partitions, layer sizes |

### The payload

```json
{"className":"com.rakovpublic.jneuropallium.worker.net.signals.storage.redis.RedisSplitInput",
 "splitInput":{"host":"127.0.0.1","port":6379,"neuronNetName":"demo09","threads":2,
               "discriminatorName":null,"layerId":-2147483648,"start":0,"end":1,
               "run":0,"loop":1,"cycleNeuronMapping":{"clusterDemoInput":0},
               "nodeIdentifier":"payload-probe"}}
```

| Configuration | Per assignment |
|---|---|
| `FileLayersMeta` + `InMemoryInputResolver` (the previous cluster path) | model JSON + input history |
| `RedisLayersMeta` + `RedisSplitInput` | **336 bytes**, constant in model size |

---

## 6. Configuration

`POST /configuration/update` with `Content-Type: application/json` (abridged; produced by
`ClusterDemoConfigurator`):

```json
{
  "partitions": 4,
  "nodeTimeout": 180000,
  "layersMetaClass":     "…net.layers.impl.redis.RedisLayersMeta",
  "layersMetaJson":      "{\"host\":\"127.0.0.1\",\"port\":6379,\"neuronNetName\":\"demo09\"}",
  "signalsPersistClass": "…storage.redis.RedisSignalStorage",
  "historyClass":        "…storage.redis.RedisSignalHistoryStorage",
  "splitInputClass":     "…storage.redis.RedisSplitInput",
  "splitInputJson":      "{…,\"threads\":2}",
  "inputLoadingStrategyClass": "…net.signals.CycledInputLoadingStrategy",
  "resultRunnerClass":   "…demo.cluster.ClusterResultLayerRunner",
  "discriminators": []
}
```

Every `*Class` is resolved by `Class.forName`, so moving the cluster from file-backed to
Redis-backed storage is this request — not a code change. Note `inputLoadingStrategyJson` is
absent on purpose: without it the master builds the strategy with its no-arg constructor and
supplies the layers afterwards.

`nodeTimeout` has to sit well above the time one partition takes. It is the point at which the
master assumes a node is gone and hands its partition to somebody else; set too low, the
cluster spends its time reprocessing the same range.

---

## 7. Defects fixed to make this run

Everything below was on the master's critical path and had to be fixed before a single epoch
could complete. Grouped by what it broke.

### Scheduling (`worker/net/core/InputService.java`)

| ID | Symptom | Fix |
|---|---|---|
| D-01 | `getNext` never recorded the partition it handed out, so `uploadWorkerResult` **silently dropped every result** and the timeout branch NPEd | record `currentInput` on assignment, clear it on completion |
| D-02 | one `ISplitInput` instance created **outside** the partition loop and mutated — every worker got the same last slice | new instance per partition (`split`) |
| D-03 | `nodeMetas.get(0)` on a `HashMap<String, …>` — NPE as soon as a second worker registered | look the node up properly |
| D-04 | the "wait until all nodes idle" barrier slept **inside** `synchronized`, so one worker blocked all others; `i = 0` also skipped re-checking index 0 | return instead of blocking; the worker retries on `204` |
| D-05 | `runCompleted()` compared the result layer **id** (`Integer.MAX_VALUE` under Redis) with a layer **position** — never true, so the master never emitted results | compare positions |
| D-09 | partition count conflated "partitions" with "nodes", divided by a possibly empty node set | `max(partitions, nodes)`, guarded |
| D-20 | `run` was never initialised; `run++` NPEd on the first epoch rollover | initialise to `0` |
| D-21 | layer at position 0 was **never processed** — `currentLayer` started at `0` and the code prepared `currentLayer + 1` | `-1` means "before the first layer" |
| D-22 | a newly registered node was marked busy, so the idle barrier could never clear | a fresh node is idle |
| D-26 | `register()` never told the loading strategy about the input, so the input was registered but never read | forward to `updateInputs` |
| D-27 | nothing populated the input before the first layer was scheduled — the first epoch ran on empty layers | populate at the start of each epoch |

### Worker loop and transport

| ID | Symptom | Fix |
|---|---|---|
| D-06 | `getAsString()` on a `JsonObject` — the `http` mode could not parse a single assignment | `toString()` |
| D-07 | the worker never reported completion, so the master's barrier never cleared | `POST /input/callback` + `/completeRun` after each partition |
| D-08 | "no work available" surfaced as a `500` (NPE in `SplitInputResponse`) and the worker spun on it | `204 No Content`, worker backs off |
| D-23 | requests carried no `Content-Type`, so every master endpoint answered `415` | set it in `HttpRequestResolver` |
| D-32 | a neuron that cannot produce a result was retried **forever**, holding the partition and stalling the cluster | stop when a full pass frees nothing |
| D-33 | Redis round trips per neuron made a 300-neuron partition exceed the node timeout | batch per partition (`saveResults`/`saveNeurons`) |

### Storage and serialisation

| ID | Symptom | Fix |
|---|---|---|
| D-11 | `$..[?(@.neuronId => …)]` is not valid JSONPath, and even fixed it scanned the whole layer per partition | ZSET index + `HMGET` |
| D-12 | the Redis classes had no Jackson creator, so the master could not be configured for Redis at all | `@JsonCreator` + accessors |
| D-13 | `pool.getResource()` without try-with-resources and a fresh client per call — pool exhaustion within seconds of a 3-worker run | one shared pool per endpoint |
| D-14 | reading an absent key NPEd — which is the state on the very first write | null-safe reads |
| D-15/D-16 | `linsert` pivoted on an object's `toString()`; `saveNeurons` threw on an empty list; `addNeuron` merged instead of appending | rewritten |
| D-24 | `RedisContext` could not be deserialised, so no worker could start with it | `@JsonCreator` |
| D-30 | `@JsonDeserialize` on `IInputLoadingStrategy` / `ISignalHistoryStorage` is **inherited by concrete classes**, where the wrapper envelope it expects can only recurse — the master silently fell back to a half-built in-memory history | cancel it on the concrete classes |
| D-31 | signals and neurons could not round-trip: serialisation emits derived getters (`currentSignalClass`, `paramClass`, …) that have no setter | ignore unknown properties on read |

### Master service and packaging

| ID | Symptom | Fix |
|---|---|---|
| D-17 | `@RequestParam` cannot bind a complex type; the config endpoint was unusable | `@RequestPart` + a JSON variant with no multipart |
| D-18 | the master had no `config.properties` and silently used the **worker jar's** developer file, so uploads landed in a folder called `null` | ship one; system properties win; `${…}` resolved |
| D-19 | `NodeManager` and `InputService.nodeMetas` were two independent registries | both surfaced in `/debug/state` |
| D-28 | the war had no main manifest — `java -jar` refused to start it | bind `spring-boot:repackage` |
| D-29 | logback (via `tahu-core`) next to the log4j binding made log4j refuse to initialise; the master died before its context loaded | exclude logback from `tahu-core`, and Spring's logging starter |
| D-34 | configuration failures were logged without the exception, so a broken config produced a message with no cause | log the throwable |

---

## 8. Verified behaviour

`run_cluster_demo.ps1 -Workers 3 -Neurons 200 -ResultNeurons 20 -Partitions 3 -Epochs 3`
against Redis 5.0.14 on `127.0.0.1:6379`:

```
=== Phase 1 - seed the model into Redis ===
  layer 0: 200 neurons, ids 1000000..1000199
  layer 1: 200 neurons, ids 2000000..2000199
  layer 2: 200 neurons, ids 3000000..3000199
  result layer 2147483647: 20 neurons, ids 9000000..9000019
redis keys holding the whole model: 11

=== Phase 4 - what the master actually sends a worker ===
payload: 336 bytes for a model of 620 neurons

=== Phase 6 - concurrent partitions ===
  worker-1   layer 0   neurons [0,66)
  worker-2   layer 0   neurons [66,132)
  worker-3   layer 0   neurons [132,200)

=== Phase 7 - results ===
epoch 0 loop 1 -> 20 result neurons
  sample: {"neuronId":9000000,"layerRole":"result","neuronLabel":"result-0"}
```

Also observed:

- The three workers tile the layer exactly — no gap, no overlap — and the same holds on a
  600-neuron model (`[0,150) [150,300) [300,450)`).
- The payload stays **336 bytes** whether the model has 620 or 1 850 neurons: it carries
  ids and ranges, so it does not grow with the model.
- Layer positions advance `0 → 1 → 2 → 3 → 4` in lockstep, `runCompleted` flips at the end of
  the epoch, one history key per neuron per layer is written (620 per epoch on the reference
  model), and the next epoch starts from position 0.
- Results are available for every completed epoch, 20 of 20 result neurons each.
- `mvn test` on the repository: **955 tests, 0 failures**.

## 9. Known limitations

- **Reassignment is time-based only.** A worker slower than `nodeTimeout` has its partition
  handed to somebody else while it is still working on it, and both write the same results.
  Worker heartbeats — or a lease the worker renews — would fix this properly; the demo works
  around it with a generous timeout.
- **The cycle layer lives in the master's memory**, not in Redis: its single control neuron is
  mutated in place on every population, so it cannot be a document re-parsed on each read. Its
  id is published to the shared layer list, so workers see the same processing order and simply
  receive an empty partition for it. A master restart therefore restarts the input cycle.
- **Neurons are persisted with their processing state.** A worker writes the whole neuron back
  after processing, transient fields included. It round-trips correctly but the stored layer
  configuration is larger than the configuration alone.
- **The run does not stop by itself.** Once the input queue is drained the cluster keeps
  cycling epochs over empty input. A terminating condition (max epochs) belongs in the master.
- **Terminal neurons log a warning per signal.** The result layer has no outgoing connections,
  so `Axon` warns for every signal it receives; at `INFO` this produces tens of megabytes of
  worker log for a short run. Harmless, but it is framework-wide behaviour rather than
  something this demo should silence on its own.
