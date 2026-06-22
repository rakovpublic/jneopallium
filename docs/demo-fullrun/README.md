# Jneopallium Full-Run Demos

## What Full-Run Means

A full-run demo is a runnable Jneopallium application, not an in-process bridge harness. Each demo builds a model JAR, generates layer metadata, writes a full context JSON file, and starts the real worker entry point:

```bash
java -cp "<worker runtime classpath>" \
  com.rakovpublic.jneuropallium.worker.application.Entry \
  local \
  "file:///<absolute path to demo-model.jar>" \
  com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext \
  "<context JSON or generated context.json path>"
```

The wrapper passes the generated `context.json` path to avoid platform-specific JSON escaping. `Runner` resolves that file path into the full JSON before constructing `DemoJsonContext`, so direct callers may still pass the full JSON string.

## Run All Demos

Linux/macOS:

```bash
scripts/demo-fullrun/run_all_fullrun_demos.sh
```

Windows PowerShell:

```powershell
scripts\demo-fullrun\run_all_fullrun_demos.ps1
```

Both scripts run:

```bash
mvn -q -DskipTests=false clean install
```

They then build the worker runtime classpath and launch the full-run demo runner.

## Run One Demo

Linux/macOS:

```bash
scripts/demo-fullrun/run_demo.sh demo-01-industrial-control
```

Windows PowerShell:

```powershell
scripts\demo-fullrun\run_demo.ps1 demo-01-industrial-control
```

## Reports

Per-demo reports are available in [reports/README.md](reports/README.md). They summarize the application story, network structure, signal surface, I/O path, safety mode, deterministic behavior, and default verification evidence for each demo.

## Output Layout

By default, outputs are written to:

```text
target/jneopallium-fullrun-demos/
  summary.json
  demo-01-industrial-control/
    demo-model.jar
    context.json
    layers/
      0
      1
      ...
    results.jsonl
    audit.jsonl
    entry.log
    manifest.json
```

`summary.json` contains one manifest per demo with status, tick count, output row count, model JAR path, context path, layer metadata path, and behavior assertions.

## Runtime Pieces

`DemoJsonContext` is the context implementation loaded by `Runner` before the user model JAR is attached. It is map-backed and Jackson-deserializable.

`DemoFileStorage` is a small filesystem storage used by `LocalApplication.getStorage(storageJson)` to read layer metadata and persist layer dumps.

`JsonlResultAggregator` is the real `IOutputAggregator` used by the network. It writes one JSON line per run with demo id, run, deterministic timestamp, and result rows containing neuron id, layer id, signal type, result type, value, confidence, mode, decision, and reason.

`DemoFullRunLauncher` generates demo-specific model JARs, layer metadata, contexts, manifests, and the dashboard summary. It spawns `Entry local ...`; it does not call `LocalApplication` directly.

## Demo Safety Modes

`demo-01-industrial-control` is `AUTONOMOUS-MOCK`: safety-gated reactor control against deterministic local mock data only.

`demo-02-pump-fleet-maintenance` is `ADVISORY`: MQTT/Sparkplug-like pump fleet monitoring with no autonomous writes.

`demo-03-drone-mavlink-guard` is `SIM-ONLY`: mission guard recommendations and command vetoes for simulated MAVLink-style telemetry only.

`demo-04-clinical-fhir-advisory` is `ADVISORY`: clinical decision support cards requiring clinician review, never treatment orders.

`demo-05-dicom-readonly-context` is `READ-ONLY`: image metadata and routing/QC advisory, no pixel diagnosis and no writeback.

`demo-06-cybersecurity-kafka-triage` is `ADVISORY`: temporal threat correlation across authentication, process, DNS, network-flow, threat-intelligence, asset, and maintenance streams; it emits investigation recommendations and no blocking action.

`demo-07-observability-otel-export` is `EXPORT-ONLY`: anomaly summaries and root-cause candidates exported as JSONL/OTel-like records.

`demo-08-adaptive-tutoring-lti` is `ADVISORY`: learner-state hints and difficulty recommendations.

`demo-09-nengo-interop` is `ADVISORY`: deterministic mock Nengo vector stream in, vector/confidence stream out.

## Replacing Mocks With Real Bridges

Each demo input class is deterministic by default and lives under `worker/src/main/java/com/rakovpublic/jneuropallium/worker/demo/fullrun/demoXX/inputs`. A real transport can be introduced later by replacing the input source while keeping the same typed signal classes, layer metadata shape, result aggregator, and safety mode.

For external systems such as OPC UA, MQTT, Kafka, FHIR, DICOM, OpenTelemetry, LTI/xAPI, or a real Nengo process, keep the transport at the input edge and preserve the advisory/read-only/export-only ceiling unless a separate safety case allows more.

## Troubleshooting

Use Java 17. The Maven build is configured for release 17.

Make sure `mvn` is on `PATH`, then run:

```bash
mvn -q -DskipTests=false clean install
```

On Windows, prefer the PowerShell scripts so `file:///` model JAR paths, runtime classpath separators, and context JSON escaping are handled for you.

To inspect result output:

```bash
cat target/jneopallium-fullrun-demos/demo-01-industrial-control/results.jsonl
cat target/jneopallium-fullrun-demos/summary.json
```

If a demo fails, inspect its `entry.log`, `context.json`, `layers/`, `results.jsonl`, `audit.jsonl`, and `manifest.json` in the demo output directory.
