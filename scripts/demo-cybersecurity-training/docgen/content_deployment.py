# -*- coding: utf-8 -*-
"""Deployment guide content (EN + UK), block DSL.

Deployment-only: it consumes the deployable network the Training Guide
produces and runs it as an advisory worker. Reflects the updated
pipeline, where training emits ready-to-load JNeopallium layer/neuron
configuration with real runtime classes.
"""

from __future__ import annotations

DATE = "23 June 2026"
DATE_UK = "23 червня 2026"


def deployment(lang: str) -> list:
    return _EN if lang == "en" else _UK


_EN = [
    ("cover", "Jneopallium · Demo 06",
     "Deployment Guide",
     "Cybersecurity Temporal Threat Correlation — run the trained network as an advisory worker",
     [("Document", "Production Deployment Guide"),
      ("Product", "Jneopallium Cybersecurity Module (Demo 06)"),
      ("Consumes", "The deployable network from the Training Guide"),
      ("Safety mode", "ADVISORY (recommend-only)"),
      ("Platforms", "Windows (PowerShell) and Linux/macOS (bash)"),
      ("Author", "Dmytro Rakovskyi — Kharkiv, Ukraine"),
      ("Date", DATE),
      ("License", "BSD 3-Clause")],
     "Deployment Guide",
     "The deployment half of the pipeline: take the JNeopallium network the trainer produced, wire it to "
     "your event sources and output sink, and run it as a continuous, shadow-to-advisory security worker — "
     "configured exactly as the architecture intends."),

    ("toc", "Contents",
     ["What you will achieve",
      "Prerequisites — the trained package and runtime",
      "The launch architecture",
      "The deployable package (what training produced)",
      "Point the worker at the generated neuron-net structure",
      "Package the deployment JAR",
      "Runtime configuration: the IContext property reference",
      "Configure event sources and streaming input",
      "Wire the output aggregator and launch the worker",
      "Promote from shadow mode to operator-visible advisory",
      "Monitoring in production",
      "Safety, rollback and packaging",
      "Troubleshooting",
      "Appendix — deployment cheat-sheet"]),

    ("h1", "What you will achieve", "1"),
    ("p", "By the end of this guide you will have taken the **deployable network produced by the Training "
          "Guide** — a directory of JNeopallium layer and neuron configuration with real runtime classes "
          "and embedded trained weights — and run it as a continuous **advisory worker** that recommends, "
          "but never autonomously enforces, security responses."),
    ("callout", "Safety ceiling for the whole guide",
     "Everything below operates in ADVISORY mode. The worker may recommend investigation or quarantine "
     "candidates, but it must not isolate hosts or block traffic without a separate safety case, approval "
     "workflow, and rollback path. The trained network already marks the response layer ADVISORY and the "
     "hard gate as fixed configuration.", "warning"),

    ("h1", "Prerequisites — the trained package and runtime", "2"),
    ("bullet", "**A trained network** from the Training Guide — the artifact directory containing "
               "`model-descriptor.json`, `layer-0.json` … `result-layer.json`, and "
               "`trained-temporal-threat-model.json`."),
    ("bullet", "**Java 17+** to run the worker, and the built `worker` artifact (`mvn clean install`)."),
    ("bullet", "**Your telemetry source** — a Kafka-style event bus (or any feed you can adapt to typed "
               "signals)."),
    ("bullet", "**An output destination** — a JSONL sink for shadow mode, or your SIEM / Kafka topic / "
               "webhook for operator-visible advisory."),
    ("callout", "Training first",
     "If you do not yet have a trained network, run the companion Training Guide. Deployment consumes its "
     "output directory directly — there is no manual translation step between training and deployment.",
     "info"),

    ("h1", "The launch architecture", "3"),
    ("p", "A production deployment uses the worker's real entry point — the architecture intends you to "
          "supply your own JAR, the trained network, configuration, and event sources. The worker is "
          "started by one entry point with four arguments:"),
    ("code",
     "java com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
     "    <mode> <model-jar-url> <context-class> <context-json-or-path>"),
    ("table", ["Argument", "Meaning", "Example"],
     [["`mode`", "Deployment topology", "`local`, `http`, `grpc-client`, `grpc-master`"],
      ["`model-jar-url`", "URL of your deployment JAR", "`file:///opt/jneo/cyber-model.jar`"],
      ["`context-class`", "An `IContext` implementation", "`…runtime.DemoJsonContext`"],
      ["`context-json-or-path`", "Inline JSON or a path to a JSON config file", "`/opt/jneo/cyber-context.json`"]],
     [1.9, 2.6, 2.3]),
    ("p", "Internally, `Entry` hands the four arguments to the `Runner`, which: (1) deserialises the "
          "context JSON into your `IContext` class with Jackson; (2) loads the JAR into an isolated class "
          "loader (`JarClassLoaderService`); (3) selects an application by `mode`; and (4) starts it. The "
          "mode maps directly onto the deployment topologies from the architecture:"),
    ("table", ["Mode", "Application", "Topology"],
     [["`local`", "`LocalApplication`", "Single JVM — pilots, edge sites"],
      ["`http`", "`HttpClusterApplication`", "Distributed worker nodes over HTTP"],
      ["`grpc-master` / `grpc-client`", "`GRPCServerApplication` / `GRPCClient`", "gRPC cluster; FPGA-capable"]],
     [2.2, 2.6, 2.0]),
    ("callout", "The four production ingredients",
     "A deployment is exactly four things: (1) the **trained network** (from training), (2) the **IContext "
     "configuration**, (3) a **deployment JAR** (worker security classes plus your event source and sink), "
     "and (4) your **event sources and output sink**. The next sections wire each one.", "info"),

    ("h1", "The deployable package (what training produced)", "4"),
    ("p", "The trainer does not emit an abstract model that you must translate — it emits the **actual "
          "JNeopallium network**. The package is a directory of ten files; the five layer files plus the "
          "descriptor are what the worker boots from:"),
    ("table", ["Artifact", "Role at deploy time"],
     [["`model-descriptor.json`", "Whole-network map: 5 layers, 8 neurons, frequency map, runtime classes"],
      ["`layer-0.json`", "Multi-source event input boundary"],
      ["`layer-1-fast-evidence.json`", "Fast evidence receptors (4 neurons)"],
      ["`layer-2-temporal-correlation.json`", "Trained correlator — embedded weights, gates, baseline policy"],
      ["`layer-3-response-planning.json`", "Advisory response bands + the fixed hard safety gate"],
      ["`result-layer.json`", "Security-advisory result output"],
      ["`trained-temporal-threat-model.json`", "Compact trained model the correlation layer references"],
      ["`trained-model-update.json` / `quantitative-summary.json` / `source-mapping.json`", "Provenance, metrics, and source mapping"]],
     [3.2, 3.6]),
    ("p", "Total network: **5 layers, 8 real neurons, 34 trainable weights, 1 bias**. Every neuron names a "
          "concrete class from the worker's security package — so deployment is a matter of pointing the "
          "worker at this directory, not re-implementing anything."),

    ("h1", "Point the worker at the generated neuron-net structure", "5"),
    ("p", "The neuron-net structure — the architecture's \"neuron-network structure file\" — is **generated "
          "by training**, not hand-written. Point `configuration.input.layermeta` at the directory holding "
          "the five layer files and the worker boots the network directly. Each file is a real layer of "
          "real neurons:"),
    ("table", ["Layer file", "Layer", "Real neuron classes"],
     [["`layer-0.json`", "Input", "(no neuron objects; canonical inputs only)"],
      ["`layer-1-fast-evidence.json`", "Fast evidence", "`NetworkFlowNeuron`, `SignaturePatternNeuron`, `ProcessBehaviourNeuron`, `EntityBehaviourBaselineNeuron`"],
      ["`layer-2-temporal-correlation.json`", "Correlation", "`TemporalThreatCorrelationNeuron`"],
      ["`layer-3-response-planning.json`", "Planning + gate", "`ResponsePlanningNeuron`, `ResponseGateNeuron`"],
      ["`result-layer.json`", "Result", "`ResponsePlanningNeuron` (result)"]],
     [2.3, 1.4, 3.1]),
    ("p", "The correlation layer embeds everything the model learned, ready to run:"),
    ("code",
     '"dendrites": {\n'
     '  "weights": { "technique_command_and_control": 0.428,\n'
     '               "network_receptor_score": 0.424,\n'
     '               "maintenance_ratio": -0.452, ... },\n'
     '  "bias": 2.1597, "decisionThreshold": 0.2 },\n'
     '"baselineAdaptation": {\n'
     '  "freezeWhenPosteriorAtLeast": 0.3,\n'
     '  "freezeWhenSignatureConfidenceAtLeast": 0.8,\n'
     '  "trustedBenignOnly": true }'),
    ("callout", "No translation step",
     "Because the structure is generated with the trained weights already inside, what you deploy is "
     "exactly what was trained and tested. The decision threshold (0.2), the five sequence gates, the "
     "response bands, and the baseline-freeze thresholds all travel inside the layer files — nothing is "
     "re-entered by hand.", "success"),

    ("h1", "Package the deployment JAR", "6"),
    ("p", "The neuron, processor, and signal classes the structure references **already ship in the "
          "worker's security module**, so you do not re-implement them:"),
    ("bullet", "Neurons — `com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.*` "
               "(`TemporalThreatCorrelationNeuron`, `ResponseGateNeuron`, …)."),
    ("bullet", "Processors — `…worker.signalprocessor.impl.security.*` (`AnomalyHypothesisProcessor`, "
               "`HypothesisResponseProcessor`, `QuarantineGateProcessor`, …)."),
    ("bullet", "Signals — `…worker.net.signals.impl.security.*` (`AnomalyScoreSignal`, "
               "`ThreatHypothesisSignal`, `QuarantineRequestSignal`, …)."),
    ("p", "Your deployment JAR therefore needs to provide only the two integration pieces that are specific "
          "to your environment: a custom **event source** (`IInitInput`, e.g. a Kafka reader) and a custom "
          "**output sink** (`IOutputAggregator`, e.g. a SIEM forwarder). Build it as a Maven module that "
          "depends on the `worker` artifact:"),
    ("code",
     "mvn -pl cyber-deploy clean package\n"
     "# -> target/cyber-model-1.0.jar  (your event source + sink + worker on the classpath)\n"
     "# passed to Entry as: file:///opt/jneo/cyber-model.jar"),
    ("callout", "Fail-fast class check",
     "List the runtime classes your structure references in `configuration.neuronnet.classes` (the trainer "
     "records them in `model-descriptor.json` under `generatedFrom.sourceRuntimeClasses`). At startup the "
     "worker verifies each is present and aborts with a clear error if any is missing.", "info"),

    ("h1", "Runtime configuration: the IContext property reference", "7"),
    ("p", "Configuration reaches the worker through an `IContext`. The bundled `DemoJsonContext` reads a "
          "JSON document holding a flat `properties` map; the alternative `Context` class reads a classic "
          "`.properties` file. Both expose the same keys via `getProperty(...)`. The properties the worker "
          "reads, with the values that define a **deployed advisory (inference) service**:"),
    ("table", ["Property", "Purpose / advisory value"],
     [["`configuration.input.layermeta`", "Directory of the generated per-layer structure files"],
      ["`configuration.neuronnet.classes`", "CSV of runtime classes that must exist in the JAR (validated at startup)"],
      ["`configuration.input.inputs`", "Event sources — an `InputArray` of `IInitInput` bindings"],
      ["`configuration.outputAggregator`", "`IOutputAggregator` class that receives every advisory"],
      ["`configuration.storage.json`", "`IStorage` definition (filesystem state for neuron / history dumps)"],
      ["`configuration.processing.frequency.map`", "Per-signal `{epoch, loop}` cadence (from the descriptor)"],
      ["`configuration.slowfast.ratio`", "Fast-to-slow loop ratio (N)"],
      ["`configuration.history.slow.runs` / `.fast.runs`", "Signal-history depth for the slow / fast loop"],
      ["`configuration.isteacherstudying`", "**`false`** for a deployed advisory worker; `true` only to teach/learn"],
      ["`configuration.infiniteRun`", "**`true`** for a continuous streaming service"],
      ["`configuration.maxRun`", "Run count when not infinite (batch replay)"],
      ["`configuration.runoncein`", "Milliseconds to sleep between cycles (poll cadence)"],
      ["`configuration.discriminatorsAmount`", "**`0`** for a plain advisory worker"],
      ["`worker.threads.amount`", "Worker threads per layer"]],
     [3.2, 3.6]),
    ("callout", "Inference vs. teaching mode",
     "Set `configuration.isteacherstudying=false` with `discriminatorsAmount=0` and `infiniteRun=true`. "
     "This puts the worker into its continuous advisory loop: pull signals from the event sources → run the "
     "trained net → write advisories through the aggregator → repeat. Teaching mode (`true`) is for "
     "training, not for a live detector.", "success"),

    ("h1", "Configure event sources and streaming input", "8"),
    ("p", "Event sources are where production telemetry enters the net. "
          "`configuration.input.inputs` is an `InputArray` of one or more sources; each binds an "
          "`IInitInput` (the source) to an `InputInitStrategy` (how its signals are placed into the first "
          "layer):"),
    ("code",
     '{\n'
     '  "inputData": [\n'
     '    {\n'
     '      "iInputSource": {\n'
     '        "clazz": "com.acme.cyber.KafkaSecurityEventSource",\n'
     '        "initInput": { "name": "cyber-stream", "epoch": 1, "loop": 1,\n'
     '                       "topics": "auth,proc,dns,flow,intel" }\n'
     '      },\n'
     '      "mandatory": true,\n'
     '      "initStrategy": {\n'
     '        "clazz": "…signals.OneToAllFirstLayerInputStrategy",\n'
     '        "iNeuronNetInput": {}\n'
     '      },\n'
     '      "amountOfRuns": 0\n'
     '    }\n'
     '  ]\n'
     '}'),
    ("p", "An event source implements the `IInitInput` contract — four methods: `readSignals()` returns the "
          "typed signals for this cycle; `getName()`; `getDesiredResults()` (training only — empty for a "
          "live detector); and `getDefaultProcessingFrequency()`."),
    ("h3", "Plugging in a real Kafka event source"),
    ("p", "To consume your live telemetry, implement an `IInitInput` that polls your Kafka topic(s) and "
          "converts each record into the typed security signals the network consumes — `PacketSignal`, "
          "`SyscallSignal`, `LogEventSignal` at the receptors, feeding `SignatureMatchSignal` and "
          "`AnomalyScoreSignal` into the correlator. Reference its class as the source `clazz`. Keep each "
          "event's **event-time** field so delayed, replayed, or out-of-order telemetry still correlates by "
          "event time, exactly as the architecture requires."),
    ("h3", "Continuous streaming vs. batch replay"),
    ("p", "For a long-running service set `configuration.infiniteRun=true` and "
          "`configuration.runoncein=<ms>` as the poll interval; the worker loops, pulling from the event "
          "source and emitting advisories continuously. For a bounded replay (for example, re-running an "
          "incident), set `infiniteRun=false` and `configuration.maxRun=<cycles>`."),
    ("h3", "Per-signal cadence (typed timescales)"),
    ("p", "`configuration.processing.frequency.map` assigns each signal its natural timescale via "
          "`{epoch, loop}`. The trainer records the recommended cadence in the descriptor's "
          "`signalFrequencyMap` — fast receptors every cycle, hypotheses and quarantine requests every "
          "second cycle, incident reports every fifth:"),
    ("code",
     '{\n'
     '  "…security.PacketSignal":           { "epoch": "1", "loop": "1" },\n'
     '  "…security.SyscallSignal":          { "epoch": "1", "loop": "1" },\n'
     '  "…security.AnomalyScoreSignal":     { "epoch": "1", "loop": "1" },\n'
     '  "…security.ThreatHypothesisSignal": { "epoch": "1", "loop": "2" },\n'
     '  "…security.IncidentReportSignal":   { "epoch": "1", "loop": "5" }\n'
     '}'),

    ("h1", "Wire the output aggregator and launch the worker", "9"),
    ("p", "`configuration.outputAggregator` names an `IOutputAggregator`. Its single method — "
          "`save(results, timestamp, run, context)` — is called every cycle with the advisories the net "
          "produced. The bundled `JsonlResultAggregator` writes advisory JSONL, which is ideal as the "
          "**shadow-mode audit sink**. For production, implement an aggregator that forwards advisories to "
          "your SIEM, a Kafka topic, or a webhook — preserving the full evidence lineage (entity, "
          "event-time range, contributing sources, posterior, response band, baseline-freeze state, model "
          "id and checksum)."),
    ("p", "A complete production context pulls it all together — note `configuration.input.layermeta` "
          "pointing at the generated layer directory:"),
    ("code",
     '{\n'
     '  "properties": {\n'
     '    "configuration.input.layermeta": "/opt/jneo/cyber/layers",\n'
     '    "configuration.neuronnet.classes": "…security.TemporalThreatCorrelationNeuron,…security.ResponsePlanningNeuron,…security.ResponseGateNeuron",\n'
     '    "configuration.input.inputs": "{ ...InputArray from section 8... }",\n'
     '    "configuration.processing.frequency.map": "{ ...frequency map... }",\n'
     '    "configuration.outputAggregator": "com.acme.cyber.SiemAdvisorySink",\n'
     '    "configuration.storage.json": "{\\"storageClass\\":\\"…DemoFileStorage\\",\\"storage\\":{\\"rootPath\\":\\"/opt/jneo/cyber/state\\"}}",\n'
     '    "configuration.slowfast.ratio": "10",\n'
     '    "configuration.history.slow.runs": "8",\n'
     '    "configuration.history.fast.runs": "64",\n'
     '    "configuration.isteacherstudying": "false",\n'
     '    "configuration.discriminatorsAmount": "0",\n'
     '    "configuration.infiniteRun": "true",\n'
     '    "configuration.runoncein": "1000",\n'
     '    "configuration.maxRun": "0",\n'
     '    "worker.threads.amount": "4"\n'
     '  }\n'
     '}'),
    ("p", "Launch the worker, putting the `worker` artifact and its dependencies on the classpath and "
          "passing your deployment JAR by URL (use `;` as the classpath separator on Windows, `:` on "
          "Linux/macOS):"),
    ("code",
     "java -cp \"worker-1.0.jar:libs/*\" \\\n"
     "  com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
     "  local \\\n"
     "  file:///opt/jneo/cyber-model.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext \\\n"
     "  /opt/jneo/cyber-context.json"),
    ("callout", "Start in shadow mode",
     "Point `configuration.outputAggregator` at an audit-only sink first (the JSONL aggregator is perfect), "
     "with no operator notification. The worker streams advisories you can compare against incident tickets "
     "before promoting to operator-visible advisory — the subject of the next section.", "warning"),

    ("h1", "Promote from shadow mode to operator-visible advisory", "10"),
    ("p", "Promote the network gradually. Never jump straight to operator-visible output, and never enable "
          "enforcement without a separate approved design."),
    ("num", "**Deploy adapters** that convert real telemetry into the typed security signals."),
    ("num", "**Load the network** by pointing `configuration.input.layermeta` at the generated layer "
            "directory."),
    ("num", "**Start in shadow mode:** write advisories to an audit topic or JSONL sink without notifying "
            "operators."),
    ("num", "**Compare** advisories against incident tickets, red-team logs, and sampled benign activity."),
    ("num", "**Promote to operator-visible advisory** only after false-positive budgets and latency "
            "targets are met."),
    ("num", "**Keep active enforcement disabled** unless a separate response-control design is approved."),
    ("p", "A real Kafka bridge can replace the demo input. Keep the same typed event contract and "
          "event-time fields so delayed, replayed, or out-of-order telemetry is still correlated by event "
          "time rather than by processing time."),

    ("h1", "Monitoring in production", "11"),
    ("p", "Track these metrics continuously:"),
    ("bullet", "Events processed per source per minute; dropped, delayed, and out-of-order events."),
    ("bullet", "Advisory count per host group and tenant; false-positive review rate; confirmed "
               "true-positive rate."),
    ("bullet", "Mean time to detection and mean time to operator acknowledgement."),
    ("bullet", "Baseline updates accepted, frozen, and rejected; maintenance-window suppression rate."),
    ("bullet", "Model version and threshold used for every advisory."),
    ("p", "Every advisory must preserve its evidence lineage: `entity_id`, the `event_tick` range, the "
          "contributing sources and techniques, the posterior, the response band, `baselineFrozen`, the "
          "`modelId`, and the model checksum (the `trainingChecksum` from `trained-model-update.json`)."),

    ("h1", "Safety, rollback and packaging", "12"),
    ("h2", "Packaging"),
    ("p", "A deployment package is the generated artifact directory plus provenance:"),
    ("bullet", "The generated layer files and model artifacts (`model-descriptor.json`, `layer-0.json` … "
               "`result-layer.json`, `trained-temporal-threat-model.json`, `trained-model-update.json`)."),
    ("bullet", "Git commit SHA, training command, manifest checksum, the `trainingChecksum`, generated "
               "artifact checksums, the advisory-deployment approval record, and a rollback package "
               "identifier."),
    ("h2", "Safety and rollback rules"),
    ("bullet", "Hard safety gates are fixed configuration in `layer-3-response-planning.json`, never "
               "learned model weights."),
    ("bullet", "Critical-asset allow-lists and maintenance windows reduce urgency but never erase evidence."),
    ("bullet", "Baseline adaptation freezes when the posterior reaches 0.3 or signature confidence reaches "
               "0.8 — encoded in the layer's `baselineAdaptation` block."),
    ("bullet", "Keep the previous network package available for immediate rollback."),
    ("bullet", "Roll back if false positives exceed budget, source coverage drops, or parsing errors create "
               "systematic blind spots."),
    ("callout", "Honest limitation",
     "The checked-in reference network is trained on a synthetic, deterministic corpus. It is excellent for "
     "repeatable pipeline evidence and safety-gate regression, but it does not establish real-world "
     "detection quality. Production claims require retraining on external multi-source datasets "
     "(see the Training Guide) and validation on representative enterprise telemetry.", "warning"),

    ("h1", "Troubleshooting", "13"),
    ("table", ["Symptom", "Likely cause and fix"],
     [["`Cannot find class … in provided jar`", "A class in `configuration.neuronnet.classes` is missing — rebuild the deployment JAR"],
      ["Worker starts but emits nothing", "Check the event source `clazz` and that `readSignals()` returns typed signals"],
      ["Layer not loaded / structure error", "Confirm `configuration.input.layermeta` points at the generated layer directory"],
      ["Worker exits immediately", "For a live detector set `infiniteRun=true`; `maxRun` only applies to bounded replay"],
      ["Advisories emit a blocking action", "Verify the response layer is ADVISORY; this is a regression to report"],
      ["Wrong threshold or weights at runtime", "You deployed a stale layer directory; redeploy the latest trained package"],
      ["Cyrillic / encoding issues in output", "Ensure the aggregator writes UTF-8"]],
     [3.0, 3.8]),

    ("h1", "Appendix — deployment cheat-sheet", "14"),
    ("h3", "Launch a production advisory worker"),
    ("code",
     "java -cp \"worker-1.0.jar:libs/*\" \\\n"
     "  com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
     "  local file:///opt/jneo/cyber-model.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext \\\n"
     "  /opt/jneo/cyber-context.json"),
    ("h3", "The advisory-worker context essentials"),
    ("code",
     "configuration.input.layermeta   = <generated layer directory>\n"
     "configuration.isteacherstudying = false\n"
     "configuration.discriminatorsAmount = 0\n"
     "configuration.infiniteRun       = true\n"
     "configuration.runoncein         = 1000\n"
     "configuration.outputAggregator  = <your IOutputAggregator>"),
    ("spacer", 8),
    ("pi", "Jneopallium Cybersecurity Module · Demo 06 · Deployment Guide. "
           "Consumes the network produced by the Training Guide. Safety mode: ADVISORY. "
           "Active enforcement requires a separate approved safety case. License: BSD 3-Clause."),
]


_UK = [
    ("cover", "Jneopallium · Демо 06",
     "Посібник з розгортання",
     "Часова кореляція кіберзагроз — запустіть навчену мережу як рекомендаційний worker",
     [("Документ", "Посібник з промислового розгортання"),
      ("Продукт", "Модуль кібербезпеки Jneopallium (Демо 06)"),
      ("Споживає", "Готову до розгортання мережу з Посібника з навчання"),
      ("Режим безпеки", "ADVISORY (лише рекомендації)"),
      ("Платформи", "Windows (PowerShell) і Linux/macOS (bash)"),
      ("Автор", "Дмитро Раковський — Харків, Україна"),
      ("Дата", DATE_UK),
      ("Ліцензія", "BSD 3-Clause")],
     "Посібник з розгортання",
     "Половина конвеєра, що відповідає за розгортання: візьміть мережу JNeopallium, яку створив тренер, "
     "під'єднайте її до джерел подій і приймача виводу та запустіть як безперервний worker безпеки в режимі "
     "«тінь → рекомендації» — налаштований саме так, як передбачає архітектура."),

    ("toc", "Зміст",
     ["Чого ви досягнете",
      "Передумови — навчений пакет і середовище виконання",
      "Архітектура запуску",
      "Готовий до розгортання пакет (що створило навчання)",
      "Спрямуйте worker на згенеровану структуру нейромережі",
      "Спакуйте JAR для розгортання",
      "Конфігурація середовища: довідник властивостей IContext",
      "Налаштуйте джерела подій і потоковий вхід",
      "Під'єднайте агрегатор виводу та запустіть worker",
      "Перехід від тіньового режиму до видимих оператору рекомендацій",
      "Моніторинг у промислі",
      "Безпека, відкат і пакування",
      "Усунення несправностей",
      "Додаток — шпаргалка розгортання"]),

    ("h1", "Чого ви досягнете", "1"),
    ("p", "До кінця цього посібника ви візьмете **готову до розгортання мережу, створену Посібником з "
          "навчання** — теку конфігурації рівнів і нейронів JNeopallium зі справжніми класами часу "
          "виконання та вбудованими навченими вагами — і запустите її як безперервний **рекомендаційний "
          "worker**, що радить, але ніколи автономно не примушує до дій безпеки."),
    ("callout", "Стеля безпеки для всього посібника",
     "Усе нижче працює в режимі ADVISORY. Worker може рекомендувати кандидатів на розслідування чи карантин, "
     "але не повинен ізолювати вузли чи блокувати трафік без окремого обґрунтування безпеки, процесу "
     "погодження та шляху відкату. Навчена мережа вже позначає рівень відповіді як ADVISORY, а жорсткий "
     "запобіжник — як фіксовану конфігурацію.", "warning"),

    ("h1", "Передумови — навчений пакет і середовище виконання", "2"),
    ("bullet", "**Навчена мережа** з Посібника з навчання — тека артефактів із `model-descriptor.json`, "
               "`layer-0.json` … `result-layer.json` і `trained-temporal-threat-model.json`."),
    ("bullet", "**Java 17+** для запуску worker і зібраний артефакт `worker` (`mvn clean install`)."),
    ("bullet", "**Ваше джерело телеметрії** — шина подій у стилі Kafka (або будь-який потік, який можна "
               "адаптувати до типізованих сигналів)."),
    ("bullet", "**Призначення виводу** — приймач JSONL для тіньового режиму або ваша SIEM / тема Kafka / "
               "вебхук для видимих оператору рекомендацій."),
    ("callout", "Спершу навчання",
     "Якщо у вас ще немає навченої мережі, виконайте супровідний Посібник з навчання. Розгортання споживає "
     "його вихідну теку напряму — між навчанням і розгортанням немає кроку ручного перекладу.", "info"),

    ("h1", "Архітектура запуску", "3"),
    ("p", "Промислове розгортання використовує справжній вхідний пункт worker — архітектура передбачає, що "
          "ви надаєте власний JAR, навчену мережу, конфігурацію та джерела подій. Worker запускається одним "
          "вхідним пунктом із чотирма аргументами:"),
    ("code",
     "java com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
     "    <mode> <model-jar-url> <context-class> <context-json-or-path>"),
    ("table", ["Аргумент", "Значення", "Приклад"],
     [["`mode`", "Топологія розгортання", "`local`, `http`, `grpc-client`, `grpc-master`"],
      ["`model-jar-url`", "URL вашого JAR для розгортання", "`file:///opt/jneo/cyber-model.jar`"],
      ["`context-class`", "Реалізація `IContext`", "`…runtime.DemoJsonContext`"],
      ["`context-json-or-path`", "Вбудований JSON або шлях до файлу JSON", "`/opt/jneo/cyber-context.json`"]],
     [1.9, 2.6, 2.3]),
    ("p", "Усередині `Entry` передає чотири аргументи до `Runner`, який: (1) десеріалізує контекстний JSON "
          "у ваш клас `IContext` через Jackson; (2) завантажує JAR в ізольований завантажувач класів "
          "(`JarClassLoaderService`); (3) обирає застосунок за `mode`; і (4) запускає його. Режим прямо "
          "відображається на топології розгортання з архітектури:"),
    ("table", ["Режим", "Застосунок", "Топологія"],
     [["`local`", "`LocalApplication`", "Один JVM — пілоти, периферія"],
      ["`http`", "`HttpClusterApplication`", "Розподілені робочі вузли через HTTP"],
      ["`grpc-master` / `grpc-client`", "`GRPCServerApplication` / `GRPCClient`", "Кластер gRPC; підтримка FPGA"]],
     [2.2, 2.6, 2.0]),
    ("callout", "Чотири промислові складові",
     "Розгортання — це рівно чотири речі: (1) **навчена мережа** (з навчання), (2) **конфігурація IContext**, "
     "(3) **JAR для розгортання** (класи безпеки worker плюс ваше джерело подій і приймач) і (4) ваші "
     "**джерела подій та приймач виводу**. Наступні розділи з'єднують кожну з них.", "info"),

    ("h1", "Готовий до розгортання пакет (що створило навчання)", "4"),
    ("p", "Тренер не видає абстрактну модель, яку треба перекладати, — він видає **власне мережу "
          "JNeopallium**. Пакет — це тека з десяти файлів; п'ять файлів рівнів плюс опис — це те, з чого "
          "завантажується worker:"),
    ("table", ["Артефакт", "Роль під час розгортання"],
     [["`model-descriptor.json`", "Карта всієї мережі: 5 рівнів, 8 нейронів, мапа частот, класи часу виконання"],
      ["`layer-0.json`", "Межа багатоджерельного входу подій"],
      ["`layer-1-fast-evidence.json`", "Швидкі рецептори доказів (4 нейрони)"],
      ["`layer-2-temporal-correlation.json`", "Навчений корелятор — вбудовані ваги, гейти, політика базової лінії"],
      ["`layer-3-response-planning.json`", "Смуги рекомендаційної відповіді + фіксований жорсткий запобіжник"],
      ["`result-layer.json`", "Вивід рекомендацій безпеки"],
      ["`trained-temporal-threat-model.json`", "Компактна навчена модель, на яку посилається рівень кореляції"],
      ["`trained-model-update.json` / `quantitative-summary.json` / `source-mapping.json`", "Походження, метрики й відображення джерел"]],
     [3.2, 3.6]),
    ("p", "Уся мережа: **5 рівнів, 8 справжніх нейронів, 34 навчувані ваги, 1 зсув**. Кожен нейрон називає "
          "конкретний клас із пакета безпеки worker — тож розгортання зводиться до спрямування worker на цю "
          "теку, а не до повторної реалізації будь-чого."),

    ("h1", "Спрямуйте worker на згенеровану структуру нейромережі", "5"),
    ("p", "Структура нейромережі — «файл структури нейронної мережі» з архітектури — **генерується "
          "навчанням**, а не пишеться вручну. Спрямуйте `configuration.input.layermeta` на теку з п'ятьма "
          "файлами рівнів, і worker завантажить мережу напряму. Кожен файл — це справжній рівень справжніх "
          "нейронів:"),
    ("table", ["Файл рівня", "Рівень", "Справжні класи нейронів"],
     [["`layer-0.json`", "Вхід", "(без об'єктів нейронів; лише канонічні входи)"],
      ["`layer-1-fast-evidence.json`", "Швидкі докази", "`NetworkFlowNeuron`, `SignaturePatternNeuron`, `ProcessBehaviourNeuron`, `EntityBehaviourBaselineNeuron`"],
      ["`layer-2-temporal-correlation.json`", "Кореляція", "`TemporalThreatCorrelationNeuron`"],
      ["`layer-3-response-planning.json`", "Планування + запобіжник", "`ResponsePlanningNeuron`, `ResponseGateNeuron`"],
      ["`result-layer.json`", "Результат", "`ResponsePlanningNeuron` (результат)"]],
     [2.3, 1.4, 3.1]),
    ("p", "Рівень кореляції вбудовує все, що вивчила модель, готове до запуску:"),
    ("code",
     '"dendrites": {\n'
     '  "weights": { "technique_command_and_control": 0.428,\n'
     '               "network_receptor_score": 0.424,\n'
     '               "maintenance_ratio": -0.452, ... },\n'
     '  "bias": 2.1597, "decisionThreshold": 0.2 },\n'
     '"baselineAdaptation": {\n'
     '  "freezeWhenPosteriorAtLeast": 0.3,\n'
     '  "freezeWhenSignatureConfidenceAtLeast": 0.8,\n'
     '  "trustedBenignOnly": true }'),
    ("callout", "Без кроку перекладу",
     "Оскільки структуру згенеровано з уже вбудованими навченими вагами, те, що ви розгортаєте, — це саме "
     "те, що навчили й протестували. Поріг рішення (0.2), п'ять гейтів послідовності, смуги відповіді та "
     "пороги заморожування базової лінії — усе всередині файлів рівнів; нічого не вводиться вручну.",
     "success"),

    ("h1", "Спакуйте JAR для розгортання", "6"),
    ("p", "Класи нейронів, процесорів і сигналів, на які посилається структура, **вже постачаються в модулі "
          "безпеки worker**, тож ви їх не реалізуєте повторно:"),
    ("bullet", "Нейрони — `com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.*` "
               "(`TemporalThreatCorrelationNeuron`, `ResponseGateNeuron`, …)."),
    ("bullet", "Процесори — `…worker.signalprocessor.impl.security.*` (`AnomalyHypothesisProcessor`, "
               "`HypothesisResponseProcessor`, `QuarantineGateProcessor`, …)."),
    ("bullet", "Сигнали — `…worker.net.signals.impl.security.*` (`AnomalyScoreSignal`, "
               "`ThreatHypothesisSignal`, `QuarantineRequestSignal`, …)."),
    ("p", "Тож вашому JAR для розгортання потрібно надати лише дві інтеграційні частини, специфічні для "
          "вашого середовища: власне **джерело подій** (`IInitInput`, напр. читач Kafka) і власний "
          "**приймач виводу** (`IOutputAggregator`, напр. пересилач у SIEM). Зберіть його як модуль Maven, "
          "що залежить від артефакту `worker`:"),
    ("code",
     "mvn -pl cyber-deploy clean package\n"
     "# -> target/cyber-model-1.0.jar  (ваше джерело подій + приймач + worker на classpath)\n"
     "# передається в Entry як: file:///opt/jneo/cyber-model.jar"),
    ("callout", "Перевірка класів із швидкою помилкою",
     "Перелічіть класи часу виконання, на які посилається ваша структура, у `configuration.neuronnet.classes` "
     "(тренер записує їх у `model-descriptor.json` під `generatedFrom.sourceRuntimeClasses`). Під час запуску "
     "worker перевіряє наявність кожного й переривається з ясною помилкою, якщо якогось бракує.", "info"),

    ("h1", "Конфігурація середовища: довідник властивостей IContext", "7"),
    ("p", "Конфігурація доходить до worker через `IContext`. Вбудований `DemoJsonContext` читає JSON-"
          "документ із пласкою мапою `properties`; альтернативний клас `Context` читає класичний файл "
          "`.properties`. Обидва надають ті самі ключі через `getProperty(...)`. Властивості, які читає "
          "worker, зі значеннями, що визначають **розгорнуту рекомендаційну службу (висновок)**:"),
    ("table", ["Властивість", "Призначення / рекомендаційне значення"],
     [["`configuration.input.layermeta`", "Тека згенерованих файлів структури по рівнях"],
      ["`configuration.neuronnet.classes`", "CSV класів часу виконання, які мають бути в JAR (перевірка при старті)"],
      ["`configuration.input.inputs`", "Джерела подій — `InputArray` прив'язок `IInitInput`"],
      ["`configuration.outputAggregator`", "Клас `IOutputAggregator`, що отримує кожну рекомендацію"],
      ["`configuration.storage.json`", "Визначення `IStorage` (файловий стан для дампів нейронів / історії)"],
      ["`configuration.processing.frequency.map`", "Каданс `{epoch, loop}` кожного сигналу (з опису)"],
      ["`configuration.slowfast.ratio`", "Співвідношення швидкого до повільного циклу (N)"],
      ["`configuration.history.slow.runs` / `.fast.runs`", "Глибина історії сигналів для повільного / швидкого циклу"],
      ["`configuration.isteacherstudying`", "**`false`** для розгорнутого рекомендаційного worker; `true` лише для навчання"],
      ["`configuration.infiniteRun`", "**`true`** для безперервної потокової служби"],
      ["`configuration.maxRun`", "Кількість запусків, коли не нескінченно (пакетне відтворення)"],
      ["`configuration.runoncein`", "Мілісекунди сну між циклами (каданс опитування)"],
      ["`configuration.discriminatorsAmount`", "**`0`** для звичайного рекомендаційного worker"],
      ["`worker.threads.amount`", "Робочі потоки на рівень"]],
     [3.2, 3.6]),
    ("callout", "Режим висновку проти режиму навчання",
     "Установіть `configuration.isteacherstudying=false` з `discriminatorsAmount=0` та `infiniteRun=true`. "
     "Це переводить worker у безперервний рекомендаційний цикл: тягне сигнали з джерел подій → проганяє "
     "навчену мережу → записує рекомендації через агрегатор → повторює. Режим навчання (`true`) — для "
     "тренування, а не для живого детектора.", "success"),

    ("h1", "Налаштуйте джерела подій і потоковий вхід", "8"),
    ("p", "Джерела подій — це місце, де промислова телеметрія входить у мережу. "
          "`configuration.input.inputs` — це `InputArray` з одного чи кількох джерел; кожне прив'язує "
          "`IInitInput` (джерело) до `InputInitStrategy` (як його сигнали розміщуються в першому рівні):"),
    ("code",
     '{\n'
     '  "inputData": [\n'
     '    {\n'
     '      "iInputSource": {\n'
     '        "clazz": "com.acme.cyber.KafkaSecurityEventSource",\n'
     '        "initInput": { "name": "cyber-stream", "epoch": 1, "loop": 1,\n'
     '                       "topics": "auth,proc,dns,flow,intel" }\n'
     '      },\n'
     '      "mandatory": true,\n'
     '      "initStrategy": {\n'
     '        "clazz": "…signals.OneToAllFirstLayerInputStrategy",\n'
     '        "iNeuronNetInput": {}\n'
     '      },\n'
     '      "amountOfRuns": 0\n'
     '    }\n'
     '  ]\n'
     '}'),
    ("p", "Джерело подій реалізує контракт `IInitInput` — чотири методи: `readSignals()` повертає "
          "типізовані сигнали для цього циклу; `getName()`; `getDesiredResults()` (лише для навчання — "
          "порожній для живого детектора); і `getDefaultProcessingFrequency()`."),
    ("h3", "Під'єднання справжнього джерела подій Kafka"),
    ("p", "Щоб споживати вашу живу телеметрію, реалізуйте `IInitInput`, що опитує ваші теми Kafka й "
          "перетворює кожен запис на типізовані сигнали безпеки, які споживає мережа — `PacketSignal`, "
          "`SyscallSignal`, `LogEventSignal` на рецепторах, що живлять `SignatureMatchSignal` і "
          "`AnomalyScoreSignal` у корелятор. Вкажіть його клас як `clazz` джерела. Зберігайте поле **часу "
          "події** кожної події, тож запізніла, повторювана чи невпорядкована телеметрія все одно "
          "корелюється за часом події, точно як вимагає архітектура."),
    ("h3", "Безперервний потік проти пакетного відтворення"),
    ("p", "Для тривалої служби встановіть `configuration.infiniteRun=true` і "
          "`configuration.runoncein=<мс>` як інтервал опитування; worker зациклюється, тягнучи з джерела "
          "подій і видаючи рекомендації безперервно. Для обмеженого відтворення (наприклад, повторення "
          "інциденту) встановіть `infiniteRun=false` і `configuration.maxRun=<цикли>`."),
    ("h3", "Каданс кожного сигналу (типізовані часові масштаби)"),
    ("p", "`configuration.processing.frequency.map` призначає кожному сигналу його природний часовий "
          "масштаб через `{epoch, loop}`. Тренер записує рекомендований каданс в `signalFrequencyMap` "
          "опису — швидкі рецептори щоцикл, гіпотези й запити карантину кожен другий цикл, звіти про "
          "інциденти кожен п'ятий:"),
    ("code",
     '{\n'
     '  "…security.PacketSignal":           { "epoch": "1", "loop": "1" },\n'
     '  "…security.SyscallSignal":          { "epoch": "1", "loop": "1" },\n'
     '  "…security.AnomalyScoreSignal":     { "epoch": "1", "loop": "1" },\n'
     '  "…security.ThreatHypothesisSignal": { "epoch": "1", "loop": "2" },\n'
     '  "…security.IncidentReportSignal":   { "epoch": "1", "loop": "5" }\n'
     '}'),

    ("h1", "Під'єднайте агрегатор виводу та запустіть worker", "9"),
    ("p", "`configuration.outputAggregator` вказує `IOutputAggregator`. Його єдиний метод — "
          "`save(results, timestamp, run, context)` — викликається щоцикл із рекомендаціями, які видала "
          "мережа. Вбудований `JsonlResultAggregator` записує рекомендаційний JSONL, що ідеально як "
          "**приймач аудиту тіньового режиму**. Для промислу реалізуйте агрегатор, що пересилає "
          "рекомендації у вашу SIEM, тему Kafka чи вебхук, зберігаючи повний родовід доказів (сутність, "
          "діапазон часу події, джерела, апостеріорну, смугу відповіді, стан заморожування базової лінії, "
          "id і контрольну суму моделі)."),
    ("p", "Повний промисловий контекст поєднує все докупи — зверніть увагу, що "
          "`configuration.input.layermeta` вказує на згенеровану теку рівнів:"),
    ("code",
     '{\n'
     '  "properties": {\n'
     '    "configuration.input.layermeta": "/opt/jneo/cyber/layers",\n'
     '    "configuration.neuronnet.classes": "…security.TemporalThreatCorrelationNeuron,…security.ResponsePlanningNeuron,…security.ResponseGateNeuron",\n'
     '    "configuration.input.inputs": "{ ...InputArray з розділу 8... }",\n'
     '    "configuration.processing.frequency.map": "{ ...мапа частот... }",\n'
     '    "configuration.outputAggregator": "com.acme.cyber.SiemAdvisorySink",\n'
     '    "configuration.storage.json": "{\\"storageClass\\":\\"…DemoFileStorage\\",\\"storage\\":{\\"rootPath\\":\\"/opt/jneo/cyber/state\\"}}",\n'
     '    "configuration.slowfast.ratio": "10",\n'
     '    "configuration.history.slow.runs": "8",\n'
     '    "configuration.history.fast.runs": "64",\n'
     '    "configuration.isteacherstudying": "false",\n'
     '    "configuration.discriminatorsAmount": "0",\n'
     '    "configuration.infiniteRun": "true",\n'
     '    "configuration.runoncein": "1000",\n'
     '    "configuration.maxRun": "0",\n'
     '    "worker.threads.amount": "4"\n'
     '  }\n'
     '}'),
    ("p", "Запустіть worker, помістивши артефакт `worker` і його залежності на classpath і передавши ваш "
          "JAR для розгортання за URL (роздільник classpath: `;` у Windows, `:` у Linux/macOS):"),
    ("code",
     "java -cp \"worker-1.0.jar:libs/*\" \\\n"
     "  com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
     "  local \\\n"
     "  file:///opt/jneo/cyber-model.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext \\\n"
     "  /opt/jneo/cyber-context.json"),
    ("callout", "Починайте з тіньового режиму",
     "Спершу спрямуйте `configuration.outputAggregator` на приймач лише для аудиту (JSONL-агрегатор "
     "ідеальний), без сповіщення оператора. Worker транслює рекомендації, які ви можете порівнювати з "
     "тікетами інцидентів, перш ніж переходити до видимих оператору рекомендацій — тема наступного "
     "розділу.", "warning"),

    ("h1", "Перехід від тіньового режиму до видимих оператору рекомендацій", "10"),
    ("p", "Переводьте мережу поступово. Ніколи не переходьте одразу до видимого оператору виходу й ніколи "
          "не вмикайте примусове виконання без окремого погодженого проєкту."),
    ("num", "**Розгорніть адаптери**, що перетворюють реальну телеметрію на типізовані сигнали безпеки."),
    ("num", "**Завантажте мережу**, спрямувавши `configuration.input.layermeta` на згенеровану теку "
            "рівнів."),
    ("num", "**Почніть у тіньовому режимі:** записуйте рекомендації в тему аудиту чи приймач JSONL без "
            "сповіщення операторів."),
    ("num", "**Порівняйте** рекомендації з тікетами інцидентів, журналами red-team і вибірковою "
            "доброякісною активністю."),
    ("num", "**Переведіть у видимий оператору рекомендаційний режим** лише після досягнення бюджетів "
            "хибних спрацювань і цілей затримки."),
    ("num", "**Тримайте активне примусове виконання вимкненим**, доки не погоджено окремий проєкт "
            "керування відповіддю."),
    ("p", "Справжній міст Kafka може замінити вхід демо. Зберігайте той самий типізований контракт подій і "
          "поля часу події, тож запізніла, повторювана чи невпорядкована телеметрія все одно корелюється за "
          "часом події, а не за часом обробки."),

    ("h1", "Моніторинг у промислі", "11"),
    ("p", "Постійно відстежуйте ці метрики:"),
    ("bullet", "Події, оброблені на джерело за хвилину; відкинуті, запізнілі та невпорядковані події."),
    ("bullet", "Кількість рекомендацій на групу вузлів і орендаря; рівень перегляду хибних спрацювань; "
               "підтверджений рівень істинних позитивів."),
    ("bullet", "Середній час до виявлення та середній час до підтвердження оператором."),
    ("bullet", "Прийняті, заморожені та відхилені оновлення базової лінії; рівень придушення вікном "
               "обслуговування."),
    ("bullet", "Версія моделі та поріг, використані для кожної рекомендації."),
    ("p", "Кожна рекомендація має зберігати родовід доказів: `entity_id`, діапазон `event_tick`, джерела й "
          "техніки, що долучилися, апостеріорну, смугу відповіді, `baselineFrozen`, `modelId` і контрольну "
          "суму моделі (`trainingChecksum` з `trained-model-update.json`)."),

    ("h1", "Безпека, відкат і пакування", "12"),
    ("h2", "Пакування"),
    ("p", "Пакет розгортання — це згенерована тека артефактів плюс походження:"),
    ("bullet", "Згенеровані файли рівнів і артефакти моделі (`model-descriptor.json`, `layer-0.json` … "
               "`result-layer.json`, `trained-temporal-threat-model.json`, `trained-model-update.json`)."),
    ("bullet", "SHA коміту Git, команда навчання, контрольна сума маніфесту, `trainingChecksum`, контрольні "
               "суми згенерованих артефактів, запис погодження рекомендаційного розгортання та "
               "ідентифікатор пакета відкату."),
    ("h2", "Правила безпеки та відкату"),
    ("bullet", "Жорсткі запобіжники — це фіксована конфігурація в `layer-3-response-planning.json`, ніколи "
               "не навчені ваги моделі."),
    ("bullet", "Білі списки критичних активів і вікна обслуговування знижують терміновість, але ніколи не "
               "стирають докази."),
    ("bullet", "Адаптація базової лінії заморожується, коли апостеріорна сягає 0.3 або впевненість "
               "сигнатури сягає 0.8 — закодовано в блоці `baselineAdaptation` рівня."),
    ("bullet", "Тримайте попередній пакет мережі доступним для негайного відкату."),
    ("bullet", "Відкочуйте, якщо хибні спрацювання перевищують бюджет, покриття джерел падає або помилки "
               "розбору створюють систематичні сліпі зони."),
    ("callout", "Чесне обмеження",
     "Вбудована еталонна мережа навчена на синтетичному детермінованому корпусі. Вона чудова для "
     "відтворюваних доказів конвеєра та регресії запобіжників, але не встановлює реальної якості виявлення. "
     "Промислові твердження потребують перенавчання на зовнішніх багатоджерельних наборах даних (див. "
     "Посібник з навчання) і валідації на репрезентативній корпоративній телеметрії.", "warning"),

    ("h1", "Усунення несправностей", "13"),
    ("table", ["Симптом", "Імовірна причина й виправлення"],
     [["`Cannot find class … in provided jar`", "Бракує класу з `configuration.neuronnet.classes` — перезберіть JAR розгортання"],
      ["Worker стартує, але нічого не видає", "Перевірте `clazz` джерела подій і що `readSignals()` повертає типізовані сигнали"],
      ["Рівень не завантажено / помилка структури", "Переконайтеся, що `configuration.input.layermeta` вказує на згенеровану теку рівнів"],
      ["Worker одразу завершується", "Для живого детектора встановіть `infiniteRun=true`; `maxRun` стосується лише обмеженого відтворення"],
      ["Рекомендації видають блокувальну дію", "Перевірте, що рівень відповіді ADVISORY; це регресія для звіту"],
      ["Неправильні поріг чи ваги під час роботи", "Ви розгорнули застарілу теку рівнів; розгорніть найновіший навчений пакет"],
      ["Проблеми з кирилицею / кодуванням у виводі", "Переконайтеся, що агрегатор пише UTF-8"]],
     [3.0, 3.8]),

    ("h1", "Додаток — шпаргалка розгортання", "14"),
    ("h3", "Запуск промислового рекомендаційного worker"),
    ("code",
     "java -cp \"worker-1.0.jar:libs/*\" \\\n"
     "  com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
     "  local file:///opt/jneo/cyber-model.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext \\\n"
     "  /opt/jneo/cyber-context.json"),
    ("h3", "Основи контексту рекомендаційного worker"),
    ("code",
     "configuration.input.layermeta   = <згенерована тека рівнів>\n"
     "configuration.isteacherstudying = false\n"
     "configuration.discriminatorsAmount = 0\n"
     "configuration.infiniteRun       = true\n"
     "configuration.runoncein         = 1000\n"
     "configuration.outputAggregator  = <ваш IOutputAggregator>"),
    ("spacer", 8),
    ("pi", "Модуль кібербезпеки Jneopallium · Демо 06 · Посібник з розгортання. "
           "Споживає мережу, створену Посібником з навчання. Режим безпеки: ADVISORY. "
           "Активне примусове виконання потребує окремого погодженого обґрунтування безпеки. "
           "Ліцензія: BSD 3-Clause."),
]
