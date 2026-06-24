# -*- coding: utf-8 -*-
"""Industrial Loop Guardian — deployment guide (EN + UK), block DSL."""

from __future__ import annotations

DATE = "23 June 2026"
DATE_UK = "23 червня 2026"


def deployment(lang: str) -> list:
    return _EN if lang == "en" else _UK


_EN = [
    ("cover", "Jneopallium · Industrial Loop Guardian",
     "Deployment Guide",
     "Run the trained supervisory network as a shadow/advisory worker above PLC/PID/SIS",
     [("Document", "Production Deployment Guide"),
      ("Product", "Jneopallium Industrial Loop Guardian (FMI Skid Demo)"),
      ("Consumes", "The deployable network from the Training Guide"),
      ("Safety mode", "ADVISORY (supervisory, recommend-only)"),
      ("Platforms", "Windows (PowerShell) and Linux/macOS (bash)"),
      ("Author", "Dmytro Rakovskyi — Kharkiv, Ukraine"),
      ("Date", DATE),
      ("License", "BSD 3-Clause")],
     "Deployment Guide",
     "The deployment half of the pipeline: take the JNeopallium network the trainer produced — together "
     "with its ready-made production context — wire it to your OPC UA, MQTT, and Kafka feeds, and run it "
     "as a continuous shadow-to-advisory supervisory worker that never touches deterministic control."),

    ("toc", "Contents",
     ["What you will achieve",
      "Prerequisites — the trained package and runtime",
      "The launch architecture",
      "The deployable package (what training produced)",
      "Point the worker at the generated neuron-net structure",
      "Package the deployment JAR",
      "Runtime configuration: production-context.json",
      "Configure event sources and streaming input",
      "Wire the advisory output and launch the worker",
      "The control and safety boundary in production",
      "Promote: shadow → advisory → bounded autonomous",
      "Monitoring in production",
      "Safety, rollback and packaging",
      "Troubleshooting",
      "Appendix — deployment cheat-sheet"]),

    ("h1", "What you will achieve", "1"),
    ("p", "By the end of this guide you will have taken the **deployable network produced by the Training "
          "Guide** — a directory of JNeopallium layer/neuron configuration plus a ready-made "
          "`production-context.json` — and run it as a continuous **supervisory advisory worker** that "
          "diagnoses pump wear, oscillation, sensor drift, and energy deterioration, and recommends — but "
          "never autonomously controls."),
    ("callout", "Safety ceiling for the whole guide",
     "Everything below operates in ADVISORY mode, above PLC/PID/SIS. The worker may recommend maintenance, "
     "bounded setpoint trims, and energy actions; it must not actuate. OPC UA actuator writes stay blocked "
     "until an offline replay, a shadow pilot, an advisory subscription, and a site safety case have all "
     "passed.", "warning"),

    ("h1", "Prerequisites — the trained package and runtime", "2"),
    ("bullet", "**A trained network** from the Training Guide — the artifact directory containing "
               "`model-descriptor.json`, `layer-0.json` … `result-layer.json`, "
               "`trained-industrial-loop-guardian-model.json`, and `production-context.json`."),
    ("bullet", "**Java 17+** and the built worker (`worker-jar-with-dependencies.jar`)."),
    ("bullet", "**Your plant telemetry** — OPC UA process tags, MQTT IIoT telemetry, and optionally a "
               "Kafka shadow stream and CMMS / energy-meter context."),
    ("bullet", "**An output destination** — a JSONL sink for shadow mode, or Kafka / SIEM / CMMS / webhook "
               "for operator-visible advisory."),
    ("callout", "Training first",
     "If you do not yet have a trained network, run the companion Training Guide. Deployment consumes its "
     "output directory directly, including the bundled `production-context.json`.", "info"),

    ("h1", "The launch architecture", "3"),
    ("p", "A production deployment uses the worker's real entry point with four arguments:"),
    ("code",
     "java com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
     "    <mode> <model-jar-url> <context-class> <context-json-or-path>"),
    ("table", ["Argument", "Meaning", "Example"],
     [["`mode`", "Deployment topology", "`local`, `http`, `grpc-client`, `grpc-master`"],
      ["`model-jar-url`", "URL of your deployment JAR", "`file:///opt/jneopallium/industrial-loop-guardian.jar`"],
      ["`context-class`", "An `IContext` implementation", "`…runtime.DemoJsonContext`"],
      ["`context-json-or-path`", "Inline JSON or a path to a JSON config file", "`…/production-context.json`"]],
     [1.8, 2.5, 2.5]),
    ("p", "`Entry` deserialises the context with the context class, loads the model JAR into an isolated "
          "class loader, then maps `local`, `http`, or `grpc` to `LocalApplication`, "
          "`HttpClusterApplication`, or the gRPC server path — the same topologies as the architecture."),

    ("h1", "The deployable package (what training produced)", "4"),
    ("p", "The trainer emits the **actual JNeopallium network**, not an abstract model. The five layer "
          "files plus the descriptor are what the worker boots from:"),
    ("table", ["Artifact", "Role at deploy time"],
     [["`model-descriptor.json`", "Whole-network map: 5 layers, 17 neurons, frequency map, runtime classes"],
      ["`layer-0.json`", "Plant + supervisory-context input boundary"],
      ["`layer-1-fast-telemetry.json`", "Fast telemetry validation and loop state (7 neurons)"],
      ["`layer-2-maintenance-energy.json`", "The four trained diagnostic finding heads"],
      ["`layer-3-advisory-planning.json`", "Economic advisory planning + the fixed safety gate (4 neurons)"],
      ["`result-layer.json`", "Industrial advisory JSONL output (2 neurons)"],
      ["`production-context.json`", "A ready-to-run advisory-worker context"]],
     [3.2, 3.6]),
    ("p", "Total network: **5 layers, 17 real neurons, 156 trainable weights, 4 biases**. Every neuron "
          "names a concrete class from the worker's industrial package — deployment points the worker at "
          "this directory, it does not re-implement anything."),

    ("h1", "Point the worker at the generated neuron-net structure", "5"),
    ("p", "The neuron-net structure is **generated by training**, not hand-written. "
          "`configuration.input.layermeta` (set in `production-context.json` to "
          "`model/industrial-loop-guardian/`) points at the directory holding the five layer files, and "
          "the worker boots the network directly. Each file is a real layer of real neurons:"),
    ("table", ["Layer file", "Real neuron classes / role"],
     [["`layer-1-fast-telemetry.json`", "`SensorNeuron`, `MeasurementValidatorNeuron`, `OscillationMonitorNeuron`, …"],
      ["`layer-2-maintenance-energy.json`", "`DegradationModelNeuron`, `EnergyAccountingNeuron` — the four heads"],
      ["`layer-3-advisory-planning.json`", "`MaintenanceSchedulingNeuron`, `SetpointOptimiserNeuron`, `SafetyGateNeuron`"],
      ["`result-layer.json`", "Advisory JSONL output neurons"]],
     [2.6, 4.2]),
    ("p", "The diagnostic heads embed everything the model learned, ready to run — the trained weights, "
          "each head's allowed `featureGate`, its `logicalNeuronRole`, and its `ownedReasoning` — so a "
          "production reviewer can confirm the logic lives inside the model, not in glue code."),
    ("callout", "No translation step",
     "Because the structure is generated with the trained heads already inside, what you deploy is exactly "
     "what was trained and tested: the same weights, the same feature gates, the same response bands, the "
     "same fixed safety gate.", "success"),

    ("h1", "Package the deployment JAR", "6"),
    ("p", "The neuron, processor, and signal classes the structure references **already ship in the "
          "worker's industrial module**, so you do not re-implement them. Package these for deployment:"),
    ("table", ["Component", "Contents"],
     [["Typed signals", "`MeasurementSignal`, `AlarmSignal`, `DegradationSignal`, `EfficiencySignal`, `MaintenanceWindowSignal`, `SetpointSignal`, `InterlockSignal`"],
      ["Neurons", "`SensorNeuron`, `MeasurementValidatorNeuron`, `OscillationMonitorNeuron`, `DegradationModelNeuron`, `MaintenanceSchedulingNeuron`, `EnergyAccountingNeuron`, `SetpointOptimiserNeuron`, `SafetyGateNeuron`"],
      ["Processors", "validation, oscillation/stiction, degradation scheduling, maintenance-window scheduling, efficiency optimisation, safety gate, dispatch"],
      ["Sources", "OPC UA measurements/alarms, MQTT telemetry, Kafka shadow, CMMS, energy-meter context"],
      ["Sinks", "JSONL audit sink, Kafka/SIEM/webhook adapter; OPC UA command sink only with a safety case"]],
     [1.5, 5.3]),
    ("callout", "Fail-fast class check",
     "`production-context.json` sets `configuration.neuronnet.classes` to the full runtime class list. At "
     "startup `LocalApplication` checks every class is present in the JAR and fails fast if the package is "
     "incomplete.", "info"),

    ("h1", "Runtime configuration: production-context.json", "7"),
    ("p", "The trainer generates a ready-to-run context. It uses the standard shape "
          "`{\"properties\": { … }}` and already encodes the advisory-loop settings. The critical keys:"),
    ("table", ["Key", "Production value"],
     [["`configuration.input.layermeta`", "`model/industrial-loop-guardian/` (the generated layer directory)"],
      ["`configuration.isteacherstudying`", "`false`"],
      ["`configuration.discriminatorsAmount`", "`0`"],
      ["`configuration.infiniteRun`", "`true`"],
      ["`configuration.runoncein`", "`1` ms (one advisory tick per millisecond)"],
      ["`configuration.processing.frequency.map`", "fast tags every loop; degradation/energy every 10; maintenance every 60"],
      ["`configuration.outputAggregator`", "`JsonlResultAggregator` (swap for your SIEM/Kafka sink)"],
      ["`industrial.autonomousAction`", "`false`"],
      ["`industrial.opcua.role`", "`bounded-local-actuator-path`"],
      ["`industrial.mqtt.role`", "`telemetry-and-advisory-only`"],
      ["`industrial.neuronOwnedLogic`", "diagnosis, economic-basis, safety-envelope, bounded-recommendation"]],
     [3.4, 3.4]),
    ("callout", "The continuous advisory loop",
     "Together, `isteacherstudying=false`, `discriminatorsAmount=0`, and `infiniteRun=true` put the worker "
     "into its continuous advisory loop. `runoncein=1` paces it to one advisory tick per millisecond, before "
     "the per-signal frequency map fans fast and slow signals out at their own cadences.", "success"),

    ("h1", "Configure event sources and streaming input", "8"),
    ("p", "Local Entry replay uses the bundled `IndustrialLoopGuardianReplayInput` (started by the "
          "`IndustrialLoopGuardianEntryLauncher`). For production, plug in a site-specific `IInitInput`, or "
          "one of the shipped bridge inputs — **OPC UA, MQTT, FMI, and PLC4X** measurement/event inputs — "
          "that convert your records into typed industrial signals while preserving event time. Recommended "
          "Kafka topic groups:"),
    ("table", ["Topic group", "Maps to signal"],
     [["`plant.telemetry.measurements`", "`MeasurementSignal`"],
      ["`plant.telemetry.alarms`", "`AlarmSignal`"],
      ["`plant.maintenance.events`", "`DegradationSignal` / maintenance context"],
      ["`plant.energy.meters`", "`EfficiencySignal`"],
      ["`plant.cmms.workorders`", "`MaintenanceWindowSignal` / maintenance history"],
      ["`plant.condition.waveforms`", "`MachineWaveformSignal` (acoustic / vibration streams)"]],
     [3.2, 3.6]),
    ("p", "Keep the streaming cadence aligned with `configuration.processing.frequency.map`: fast-loop tags "
          "every loop; maintenance and energy findings every 10–60 loops; machine-health features and "
          "advisories on their own slower cadence. Because correlation is by event time, delayed or "
          "out-of-order telemetry still lands in the right place."),
    ("callout", "Machine-health condition-monitoring input",
     "Acoustic and vibration waveforms arrive as `MachineWaveformSignal`; the Java machine-health runtime "
     "extracts features, scores operating regime and domain shift, forms fault hypotheses, and emits an "
     "advisory — all read-only. Feed it from the same OPC UA / MQTT / FMI / PLC4X bridges, or a dedicated "
     "high-rate sensor stream.", "info"),

    ("h1", "Wire the advisory output and launch the worker", "9"),
    ("p", "`configuration.outputAggregator` names an `IOutputAggregator`, called every cycle with the "
          "advisories the net produced. The bundled `JsonlResultAggregator` writes advisory JSONL — ideal "
          "for the shadow-mode audit sink. Each advisory carries its full reasoning and economic basis:"),
    ("code",
     '{\n'
     '  "asset": "P-101",\n'
     '  "finding": "pump wear and cavitation risk",\n'
     '  "confidence": 0.73,\n'
     '  "evidence": { "neuron": "PumpHealthAndEfficiencyNeuron", "maxVibrationRms": 4.3 },\n'
     '  "recommendation": "SCHEDULE_PUMP_INSPECTION",\n'
     '  "recommendedAction": "Inspect P-101 impeller and suction strainer.",\n'
     '  "urgencyHours": 48,\n'
     '  "economicBasis": { "neuron": "EconomicBasisNeuron",\n'
     '                     "estimatedAvoidedShutdownValueUsd": 20000,\n'
     '                     "safetyEnvelopeSatisfied": true },\n'
     '  "controlBoundary": { "plcPidSis": "deterministic control and hard safety remain authoritative",\n'
     '                       "jneopallium": "supervisory diagnosis and bounded recommendations" },\n'
     '  "autonomousAction": false\n'
     '}'),
    ("p", "The machine-health subsystem emits a parallel **`MachineHealthAdvisorySignal`** with its own "
          "calibrated, uncertainty-aware shape — also advisory-only:"),
    ("code",
     '{\n'
     '  "asset": "P-101",\n'
     '  "healthScore": 0.62,\n'
     '  "anomalyProbability": 0.71,\n'
     '  "faultProbabilities": { "bearingDamage": 0.66, "cavitation": 0.21,\n'
     '                          "imbalance": 0.08, "sensorFault": 0.05 },\n'
     '  "unknownAnomalyProbability": 0.12,\n'
     '  "domainShiftScore": 0.18,\n'
     '  "uncertainty": 0.24,\n'
     '  "evidence": { "neuron": "MachineHealthCorrelationNeuron",\n'
     '                "acousticRms": 0.41, "vibrationEnvelopeEnergy": 0.58 },\n'
     '  "autonomousAction": false\n'
     '}'),
    ("p", "For production, forward this JSONL to your SIEM, a Kafka topic, a CMMS, or a webhook. Then launch "
          "the worker (use `;` as the classpath separator on Windows, `:` on Linux/macOS):"),
    ("code",
     "java -cp worker/target/worker-jar-with-dependencies.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
     "  local \\\n"
     "  file:///opt/jneopallium/industrial-loop-guardian.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext \\\n"
     "  /opt/jneopallium/model/industrial-loop-guardian/production-context.json"),

    ("h1", "The control and safety boundary in production", "10"),
    ("p", "The boundary the demo enforces is the boundary you keep in production. It is what makes the "
          "product insurable and easy to validate:"),
    ("bullet", "**PLC / PID / SIS** own deterministic millisecond control and hard safety. Always "
               "authoritative."),
    ("bullet", "**OPC UA** is the only bounded actuator path, and only when a safety case allows writes. "
               "Three command nodes; every write passes the priority chain below."),
    ("bullet", "**MQTT** is telemetry and advisory only — the bridge structurally rejects AUTONOMOUS."),
    ("bullet", "**Machine-health** is read-only: the `MachineHealthAdvisorySignal` never writes an actuator "
               "command; the interlocks, safety gate, and operator override stay authoritative over it too."),
    ("code",
     "hard interlock -> local fail-safe -> operator override -> safety mode\n"
     "  -> validation/quality -> clamp -> ramp limit -> diff suppression\n"
     "  -> OPC UA write -> audit"),
    ("p", "Fail-safe defaults on loss: cooling valve 100% open, pump speed 30%, heater power 0%. On OPC UA "
          "loss the skid applies these locally; on MQTT loss fast-loop control availability stays at 1.0."),

    ("h1", "Promote: shadow → advisory → bounded autonomous", "11"),
    ("p", "Promote gradually. Each step earns the next; OPC UA writes stay blocked until the whole "
          "sequence has passed."),
    ("num", "**Offline replay.** Analyse exported historian / OPC UA / MQTT data without connecting to "
            "production. Deliver discovered anomalies, an event timeline, estimated financial impact, and "
            "a false-positive review."),
    ("num", "**Shadow pilot.** Read-only connection to the plant, no actuator writes. Compare advisories "
            "against actual later outcomes and a simple-algorithm baseline."),
    ("num", "**Production advisory subscription.** Operator-visible advisories with economic basis, once "
            "false-positive budgets and latency targets are met."),
    ("num", "**Bounded autonomous optimisation.** Only after substantial shadow evidence and a site safety "
            "case — narrowly bounded setpoint recommendations or commands that never replace PLC "
            "interlocks or a certified SIS."),

    ("h1", "Monitoring in production", "12"),
    ("bullet", "Measurements processed per source per minute; dropped, delayed, out-of-order events."),
    ("bullet", "Advisory count per asset; false-positive review rate; confirmed true-positive rate."),
    ("bullet", "Detection lead time (how early before the eventual failure or alarm)."),
    ("bullet", "Energy and downtime estimates versus a simple-algorithm baseline."),
    ("bullet", "Model version and `trainingChecksum` used for every advisory; interlock and override events."),
    ("p", "Every advisory preserves its lineage: asset, contributing signals, the owning neuron, the "
          "economic basis, the safety-envelope result, and the control-boundary statement."),

    ("h1", "Safety, rollback and packaging", "13"),
    ("h2", "Packaging"),
    ("bullet", "The generated artifact directory (layer files, model, descriptor, "
               "`production-context.json`)."),
    ("bullet", "Git commit SHA, training command, the `trainingChecksum`, generated artifact checksums, "
               "the advisory-deployment approval record, and a rollback package identifier."),
    ("h2", "Safety and rollback rules"),
    ("bullet", "Hard safety gates are fixed configuration in `layer-3-advisory-planning.json`, never "
               "learned model weights; interlocks and operator overrides remain authoritative."),
    ("bullet", "Keep the previous network package available for immediate rollback."),
    ("bullet", "Roll back if false positives exceed budget, source coverage drops, or the simple-algorithm "
               "baseline is no longer beaten."),
    ("callout", "Honest limitation",
     "The checked-in reference network is trained on a synthetic, deterministic skid corpus. It is "
     "excellent for repeatable pipeline evidence and safety-boundary regression, but it is "
     "simulation/HIL evidence — not a certified safety controller. It does not replace PLC/SIS validation, "
     "hazard analysis, management of change, or site acceptance testing. Real accuracy requires external "
     "historian, CMMS, and energy-meter validation.", "warning"),

    ("h1", "Troubleshooting", "14"),
    ("table", ["Symptom", "Likely cause and fix"],
     [["`Cannot find class … in provided jar`", "A class in `configuration.neuronnet.classes` is missing — rebuild the deployment JAR"],
      ["Worker starts but emits nothing", "Check the event source `clazz` and that `readSignals()` returns typed signals"],
      ["Structure not loaded", "Confirm `configuration.input.layermeta` points at the generated layer directory"],
      ["Worker exits immediately", "For a live supervisor set `infiniteRun=true`; `maxRun` is for bounded replay"],
      ["An advisory tries to actuate", "Verify `industrial.autonomousAction=false` and the response layer is ADVISORY"],
      ["Stale weights at runtime", "You deployed an old layer directory; redeploy the latest trained package"]],
     [3.0, 3.8]),

    ("h1", "Appendix — deployment cheat-sheet", "15"),
    ("h3", "Launch a production advisory worker"),
    ("code",
     "java -cp worker/target/worker-jar-with-dependencies.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
     "  local file:///opt/jneopallium/industrial-loop-guardian.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext \\\n"
     "  /opt/jneopallium/model/industrial-loop-guardian/production-context.json"),
    ("h3", "The advisory-worker context essentials"),
    ("code",
     "configuration.input.layermeta   = model/industrial-loop-guardian/\n"
     "configuration.isteacherstudying = false\n"
     "configuration.discriminatorsAmount = 0\n"
     "configuration.infiniteRun       = true\n"
     "configuration.runoncein         = 1\n"
     "industrial.autonomousAction     = false"),
    ("spacer", 8),
    ("pi", "Jneopallium Industrial Loop Guardian · Deployment Guide. Consumes the network produced by the "
           "Training Guide. Supervisory above PLC/PID/SIS. Safety mode: ADVISORY. OPC UA writes require a "
           "separate site safety case. License: BSD 3-Clause."),
]


_UK = [
    ("cover", "Jneopallium · Industrial Loop Guardian",
     "Посібник з розгортання",
     "Запустіть навчену наглядову мережу як тіньовий/рекомендаційний worker над PLC/PID/SIS",
     [("Документ", "Посібник з промислового розгортання"),
      ("Продукт", "Jneopallium Industrial Loop Guardian (демо FMI-скіда)"),
      ("Споживає", "Готову до розгортання мережу з Посібника з навчання"),
      ("Режим безпеки", "ADVISORY (наглядовий, лише рекомендації)"),
      ("Платформи", "Windows (PowerShell) і Linux/macOS (bash)"),
      ("Автор", "Дмитро Раковський — Харків, Україна"),
      ("Дата", DATE_UK),
      ("Ліцензія", "BSD 3-Clause")],
     "Посібник з розгортання",
     "Половина конвеєра, що відповідає за розгортання: візьміть мережу JNeopallium, яку створив тренер, — "
     "разом із готовим промисловим контекстом — під'єднайте її до ваших потоків OPC UA, MQTT і Kafka та "
     "запустіть як безперервний тіньово-рекомендаційний наглядовий worker, що ніколи не торкається "
     "детермінованого керування."),

    ("toc", "Зміст",
     ["Чого ви досягнете",
      "Передумови — навчений пакет і середовище виконання",
      "Архітектура запуску",
      "Готовий до розгортання пакет (що створило навчання)",
      "Спрямуйте worker на згенеровану структуру нейромережі",
      "Спакуйте JAR для розгортання",
      "Конфігурація середовища: production-context.json",
      "Налаштуйте джерела подій і потоковий вхід",
      "Під'єднайте рекомендаційний вивід та запустіть worker",
      "Межа керування й безпеки в промислі",
      "Перехід: тінь → рекомендації → обмежена автономність",
      "Моніторинг у промислі",
      "Безпека, відкат і пакування",
      "Усунення несправностей",
      "Додаток — шпаргалка розгортання"]),

    ("h1", "Чого ви досягнете", "1"),
    ("p", "До кінця цього посібника ви візьмете **готову до розгортання мережу, створену Посібником з "
          "навчання** — теку конфігурації рівнів/нейронів JNeopallium плюс готовий `production-context.json` "
          "— і запустите її як безперервний **наглядовий рекомендаційний worker**, що діагностує знос "
          "насоса, коливання, дрейф датчиків і погіршення енергії та рекомендує, але ніколи автономно не "
          "керує."),
    ("callout", "Стеля безпеки для всього посібника",
     "Усе нижче працює в режимі ADVISORY, над PLC/PID/SIS. Worker може рекомендувати обслуговування, "
     "обмежені трими уставок та енергетичні дії; він не повинен керувати. Записи виконавчих механізмів "
     "OPC UA лишаються заблокованими, доки офлайн-відтворення, тіньовий пілот, рекомендаційна підписка та "
     "обґрунтування безпеки майданчика не пройдено.", "warning"),

    ("h1", "Передумови — навчений пакет і середовище виконання", "2"),
    ("bullet", "**Навчена мережа** з Посібника з навчання — тека артефактів із `model-descriptor.json`, "
               "`layer-0.json` … `result-layer.json`, `trained-industrial-loop-guardian-model.json` та "
               "`production-context.json`."),
    ("bullet", "**Java 17+** і зібраний worker (`worker-jar-with-dependencies.jar`)."),
    ("bullet", "**Ваша телеметрія заводу** — теги процесів OPC UA, телеметрія IIoT MQTT і за бажанням "
               "тіньовий потік Kafka та контекст CMMS / лічильника енергії."),
    ("bullet", "**Призначення виводу** — приймач JSONL для тіньового режиму або Kafka / SIEM / CMMS / "
               "вебхук для видимих оператору рекомендацій."),
    ("callout", "Спершу навчання",
     "Якщо у вас ще немає навченої мережі, виконайте супровідний Посібник з навчання. Розгортання споживає "
     "його вихідну теку напряму, включно з вбудованим `production-context.json`.", "info"),

    ("h1", "Архітектура запуску", "3"),
    ("p", "Промислове розгортання використовує справжній вхідний пункт worker із чотирма аргументами:"),
    ("code",
     "java com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
     "    <mode> <model-jar-url> <context-class> <context-json-or-path>"),
    ("table", ["Аргумент", "Значення", "Приклад"],
     [["`mode`", "Топологія розгортання", "`local`, `http`, `grpc-client`, `grpc-master`"],
      ["`model-jar-url`", "URL вашого JAR для розгортання", "`file:///opt/jneopallium/industrial-loop-guardian.jar`"],
      ["`context-class`", "Реалізація `IContext`", "`…runtime.DemoJsonContext`"],
      ["`context-json-or-path`", "Вбудований JSON або шлях до файлу JSON", "`…/production-context.json`"]],
     [1.8, 2.5, 2.5]),
    ("p", "`Entry` десеріалізує контекст класом контексту, завантажує JAR моделі в ізольований "
          "завантажувач класів, потім відображає `local`, `http` чи `grpc` на `LocalApplication`, "
          "`HttpClusterApplication` чи шлях сервера gRPC — ті самі топології, що в архітектурі."),

    ("h1", "Готовий до розгортання пакет (що створило навчання)", "4"),
    ("p", "Тренер видає **власне мережу JNeopallium**, а не абстрактну модель. П'ять файлів рівнів плюс "
          "опис — це те, з чого завантажується worker:"),
    ("table", ["Артефакт", "Роль під час розгортання"],
     [["`model-descriptor.json`", "Карта всієї мережі: 5 рівнів, 17 нейронів, мапа частот, класи часу виконання"],
      ["`layer-0.json`", "Межа входу заводу + наглядового контексту"],
      ["`layer-1-fast-telemetry.json`", "Валідація швидкої телеметрії і стан контуру (7 нейронів)"],
      ["`layer-2-maintenance-energy.json`", "Чотири навчені діагностичні голови висновків"],
      ["`layer-3-advisory-planning.json`", "Економічне планування + фіксований запобіжник (4 нейрони)"],
      ["`result-layer.json`", "Вивід промислових рекомендацій JSONL (2 нейрони)"],
      ["`production-context.json`", "Готовий контекст рекомендаційного worker"]],
     [3.2, 3.6]),
    ("p", "Уся мережа: **5 рівнів, 17 справжніх нейронів, 156 навчуваних ваг, 4 зсуви**. Кожен нейрон "
          "називає конкретний клас із промислового пакета worker — розгортання спрямовує worker на цю "
          "теку, а не реалізує щось наново."),

    ("h1", "Спрямуйте worker на згенеровану структуру нейромережі", "5"),
    ("p", "Структуру нейромережі **генерує навчання**, а не пишуть вручну. `configuration.input.layermeta` "
          "(заданий у `production-context.json` як `model/industrial-loop-guardian/`) вказує на теку з "
          "п'ятьма файлами рівнів, і worker завантажує мережу напряму. Кожен файл — справжній рівень "
          "справжніх нейронів:"),
    ("table", ["Файл рівня", "Справжні класи нейронів / роль"],
     [["`layer-1-fast-telemetry.json`", "`SensorNeuron`, `MeasurementValidatorNeuron`, `OscillationMonitorNeuron`, …"],
      ["`layer-2-maintenance-energy.json`", "`DegradationModelNeuron`, `EnergyAccountingNeuron` — чотири голови"],
      ["`layer-3-advisory-planning.json`", "`MaintenanceSchedulingNeuron`, `SetpointOptimiserNeuron`, `SafetyGateNeuron`"],
      ["`result-layer.json`", "Нейрони виводу рекомендаційного JSONL"]],
     [2.6, 4.2]),
    ("p", "Діагностичні голови вбудовують усе, що вивчила модель, готове до запуску — навчені ваги, "
          "дозволений кожній `featureGate`, її `logicalNeuronRole` та `ownedReasoning` — тож рецензент "
          "підтверджує, що логіка живе всередині моделі, а не в сполучному коді."),
    ("callout", "Без кроку перекладу",
     "Оскільки структуру згенеровано з уже вбудованими навченими головами, те, що ви розгортаєте, — це "
     "саме те, що навчили й протестували: ті самі ваги, ті самі фільтри ознак, ті самі смуги відповіді, "
     "той самий фіксований запобіжник.", "success"),

    ("h1", "Спакуйте JAR для розгортання", "6"),
    ("p", "Класи нейронів, процесорів і сигналів, на які посилається структура, **вже постачаються в "
          "промисловому модулі worker**, тож ви їх не реалізуєте наново. Спакуйте для розгортання:"),
    ("table", ["Компонент", "Вміст"],
     [["Типізовані сигнали", "`MeasurementSignal`, `AlarmSignal`, `DegradationSignal`, `EfficiencySignal`, `MaintenanceWindowSignal`, `SetpointSignal`, `InterlockSignal`"],
      ["Нейрони", "`SensorNeuron`, `MeasurementValidatorNeuron`, `OscillationMonitorNeuron`, `DegradationModelNeuron`, `MaintenanceSchedulingNeuron`, `EnergyAccountingNeuron`, `SetpointOptimiserNeuron`, `SafetyGateNeuron`"],
      ["Процесори", "валідація, коливання/заклинювання, планування деградації, планування вікон обслуговування, оптимізація ефективності, запобіжник, диспетчеризація"],
      ["Джерела", "вимірювання/тривоги OPC UA, телеметрія MQTT, тіньовий Kafka, CMMS, контекст лічильника енергії"],
      ["Приймачі", "приймач аудиту JSONL, адаптер Kafka/SIEM/вебхук; приймач команд OPC UA лише з обґрунтуванням безпеки"]],
     [1.6, 5.2]),
    ("callout", "Перевірка класів із швидкою помилкою",
     "`production-context.json` задає `configuration.neuronnet.classes` повним переліком класів часу "
     "виконання. Під час запуску `LocalApplication` перевіряє наявність кожного в JAR і переривається, "
     "якщо пакет неповний.", "info"),

    ("h1", "Конфігурація середовища: production-context.json", "7"),
    ("p", "Тренер генерує готовий контекст. Він має стандартну форму `{\"properties\": { … }}` і вже кодує "
          "налаштування рекомендаційного циклу. Критичні ключі:"),
    ("table", ["Ключ", "Промислове значення"],
     [["`configuration.input.layermeta`", "`model/industrial-loop-guardian/` (згенерована тека рівнів)"],
      ["`configuration.isteacherstudying`", "`false`"],
      ["`configuration.discriminatorsAmount`", "`0`"],
      ["`configuration.infiniteRun`", "`true`"],
      ["`configuration.runoncein`", "`1` мс (один рекомендаційний такт на мілісекунду)"],
      ["`configuration.processing.frequency.map`", "швидкі теги щоцикл; деградація/енергія кожні 10; обслуговування кожні 60"],
      ["`configuration.outputAggregator`", "`JsonlResultAggregator` (замініть на ваш приймач SIEM/Kafka)"],
      ["`industrial.autonomousAction`", "`false`"],
      ["`industrial.opcua.role`", "`bounded-local-actuator-path`"],
      ["`industrial.mqtt.role`", "`telemetry-and-advisory-only`"],
      ["`industrial.neuronOwnedLogic`", "діагностика, економічний базис, межа безпеки, обмежена рекомендація"]],
     [3.4, 3.4]),
    ("callout", "Безперервний рекомендаційний цикл",
     "Разом `isteacherstudying=false`, `discriminatorsAmount=0` та `infiniteRun=true` переводять worker у "
     "безперервний рекомендаційний цикл. `runoncein=1` задає темп одного такту на мілісекунду, перш ніж "
     "мапа частот фанить швидкі й повільні сигнали за їхніми кадансами.", "success"),

    ("h1", "Налаштуйте джерела подій і потоковий вхід", "8"),
    ("p", "Локальне відтворення через Entry використовує вбудований `IndustrialLoopGuardianReplayInput` "
          "(запускається `IndustrialLoopGuardianEntryLauncher`). Для промислу під'єднайте специфічний для "
          "майданчика `IInitInput` або один із наявних вхідних мостів — вимірювань/подій **OPC UA, MQTT, "
          "FMI та PLC4X** — що перетворюють ваші записи на типізовані промислові сигнали, зберігаючи час "
          "події. Рекомендовані групи тем Kafka:"),
    ("table", ["Група тем", "Відображається на сигнал"],
     [["`plant.telemetry.measurements`", "`MeasurementSignal`"],
      ["`plant.telemetry.alarms`", "`AlarmSignal`"],
      ["`plant.maintenance.events`", "`DegradationSignal` / контекст обслуговування"],
      ["`plant.energy.meters`", "`EfficiencySignal`"],
      ["`plant.cmms.workorders`", "`MaintenanceWindowSignal` / історія обслуговування"],
      ["`plant.condition.waveforms`", "`MachineWaveformSignal` (акустичні / вібраційні потоки)"]],
     [3.2, 3.6]),
    ("p", "Тримайте каданс потоку узгодженим із `configuration.processing.frequency.map`: швидкі теги "
          "щоцикл; висновки з обслуговування й енергії кожні 10–60 циклів; ознаки й рекомендації здоров'я "
          "машини — на власному повільнішому кадансі. Оскільки кореляція ведеться за часом події, запізніла "
          "чи невпорядкована телеметрія все одно потрапляє в правильне місце."),
    ("callout", "Вхід моніторингу стану машини",
     "Акустичні й вібраційні хвилі надходять як `MachineWaveformSignal`; рівень здоров'я машини на Java "
     "видобуває ознаки, оцінює робочий режим і зсув домену, формує гіпотези несправностей і видає "
     "рекомендацію — усе лише для читання. Подавайте його з тих самих мостів OPC UA / MQTT / FMI / PLC4X "
     "або з виділеного високочастотного потоку сенсорів.", "info"),

    ("h1", "Під'єднайте рекомендаційний вивід та запустіть worker", "9"),
    ("p", "`configuration.outputAggregator` вказує `IOutputAggregator`, що викликається щоцикл із "
          "рекомендаціями мережі. Вбудований `JsonlResultAggregator` записує рекомендаційний JSONL — "
          "ідеально для приймача аудиту тіньового режиму. Кожна рекомендація несе повне міркування й "
          "економічний базис:"),
    ("code",
     '{\n'
     '  "asset": "P-101",\n'
     '  "finding": "pump wear and cavitation risk",\n'
     '  "confidence": 0.73,\n'
     '  "evidence": { "neuron": "PumpHealthAndEfficiencyNeuron", "maxVibrationRms": 4.3 },\n'
     '  "recommendation": "SCHEDULE_PUMP_INSPECTION",\n'
     '  "recommendedAction": "Inspect P-101 impeller and suction strainer.",\n'
     '  "urgencyHours": 48,\n'
     '  "economicBasis": { "neuron": "EconomicBasisNeuron",\n'
     '                     "estimatedAvoidedShutdownValueUsd": 20000,\n'
     '                     "safetyEnvelopeSatisfied": true },\n'
     '  "controlBoundary": { "plcPidSis": "deterministic control and hard safety remain authoritative",\n'
     '                       "jneopallium": "supervisory diagnosis and bounded recommendations" },\n'
     '  "autonomousAction": false\n'
     '}'),
    ("p", "Підсистема здоров'я машини видає паралельний **`MachineHealthAdvisorySignal`** із власною "
          "каліброваною формою з урахуванням невпевненості — також лише рекомендаційний:"),
    ("code",
     '{\n'
     '  "asset": "P-101",\n'
     '  "healthScore": 0.62,\n'
     '  "anomalyProbability": 0.71,\n'
     '  "faultProbabilities": { "bearingDamage": 0.66, "cavitation": 0.21,\n'
     '                          "imbalance": 0.08, "sensorFault": 0.05 },\n'
     '  "unknownAnomalyProbability": 0.12,\n'
     '  "domainShiftScore": 0.18,\n'
     '  "uncertainty": 0.24,\n'
     '  "evidence": { "neuron": "MachineHealthCorrelationNeuron",\n'
     '                "acousticRms": 0.41, "vibrationEnvelopeEnergy": 0.58 },\n'
     '  "autonomousAction": false\n'
     '}'),
    ("p", "Для промислу пересилайте цей JSONL у вашу SIEM, тему Kafka, CMMS чи вебхук. Потім запустіть "
          "worker (роздільник classpath: `;` у Windows, `:` у Linux/macOS):"),
    ("code",
     "java -cp worker/target/worker-jar-with-dependencies.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
     "  local \\\n"
     "  file:///opt/jneopallium/industrial-loop-guardian.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext \\\n"
     "  /opt/jneopallium/model/industrial-loop-guardian/production-context.json"),

    ("h1", "Межа керування й безпеки в промислі", "10"),
    ("p", "Межа, яку забезпечує демо, — це межа, яку ви тримаєте в промислі. Саме вона робить продукт "
          "страхованим і легким для валідації:"),
    ("bullet", "**PLC / PID / SIS** володіють детермінованим мілісекундним керуванням і жорсткою безпекою. "
               "Завжди авторитетні."),
    ("bullet", "**OPC UA** — єдиний обмежений шлях виконавчих механізмів, і лише коли обґрунтування "
               "безпеки дозволяє записи. Три командні вузли; кожен запис проходить ланцюг пріоритету нижче."),
    ("bullet", "**MQTT** — лише телеметрія й рекомендації; міст структурно відхиляє AUTONOMOUS."),
    ("bullet", "**Здоров'я машини** — лише для читання: `MachineHealthAdvisorySignal` ніколи не записує "
               "команду виконавчого механізму; інтерлоки, запобіжник і перевизначення оператора лишаються "
               "авторитетними й над ним."),
    ("code",
     "жорсткий інтерлок -> локальний fail-safe -> перевизначення оператора -> режим безпеки\n"
     "  -> валідація/якість -> обмеження -> ліміт рампи -> придушення різниці\n"
     "  -> запис OPC UA -> аудит"),
    ("p", "Безпечні значення при втраті: клапан охолодження 100% відкрито, швидкість насоса 30%, "
          "потужність нагрівача 0%. При втраті OPC UA скід застосовує їх локально; при втраті MQTT "
          "доступність швидкого контуру лишається 1.0."),

    ("h1", "Перехід: тінь → рекомендації → обмежена автономність", "11"),
    ("p", "Переходьте поступово. Кожен крок заслуговує наступний; записи OPC UA лишаються заблокованими, "
          "доки вся послідовність не пройдена."),
    ("num", "**Офлайн-відтворення.** Аналізуйте експортовані дані історіана / OPC UA / MQTT без "
            "підключення до промислу. Видайте виявлені аномалії, хронологію подій, оцінений фінансовий "
            "вплив і перегляд хибних спрацювань."),
    ("num", "**Тіньовий пілот.** Підключення до заводу лише для читання, без записів виконавчих "
            "механізмів. Порівняйте рекомендації з фактичними подальшими результатами й базовою лінією "
            "простого алгоритму."),
    ("num", "**Промислова рекомендаційна підписка.** Видимі оператору рекомендації з економічним базисом, "
            "коли досягнуто бюджетів хибних спрацювань і цілей затримки."),
    ("num", "**Обмежена автономна оптимізація.** Лише після суттєвих тіньових доказів і обґрунтування "
            "безпеки майданчика — вузько обмежені рекомендації чи команди уставок, що ніколи не замінюють "
            "інтерлоки PLC чи сертифіковану SIS."),

    ("h1", "Моніторинг у промислі", "12"),
    ("bullet", "Вимірювання, оброблені на джерело за хвилину; відкинуті, запізнілі, невпорядковані події."),
    ("bullet", "Кількість рекомендацій на актив; рівень перегляду хибних; підтверджений рівень істинних "
               "позитивів."),
    ("bullet", "Час випередження виявлення (наскільки раніше за можливу відмову чи тривогу)."),
    ("bullet", "Оцінки енергії та простою проти базової лінії простого алгоритму."),
    ("bullet", "Версія моделі та `trainingChecksum` для кожної рекомендації; події інтерлоків і "
               "перевизначень."),
    ("p", "Кожна рекомендація зберігає родовід: актив, сигнали, що долучилися, нейрон-власник, економічний "
          "базис, результат межі безпеки та твердження про межу керування."),

    ("h1", "Безпека, відкат і пакування", "13"),
    ("h2", "Пакування"),
    ("bullet", "Згенерована тека артефактів (файли рівнів, модель, опис, `production-context.json`)."),
    ("bullet", "SHA коміту Git, команда навчання, `trainingChecksum`, контрольні суми згенерованих "
               "артефактів, запис погодження рекомендаційного розгортання та ідентифікатор пакета відкату."),
    ("h2", "Правила безпеки та відкату"),
    ("bullet", "Жорсткі запобіжники — фіксована конфігурація в `layer-3-advisory-planning.json`, ніколи не "
               "навчені ваги; інтерлоки й перевизначення оператора лишаються авторитетними."),
    ("bullet", "Тримайте попередній пакет мережі доступним для негайного відкату."),
    ("bullet", "Відкочуйте, якщо хибні спрацювання перевищують бюджет, покриття джерел падає або базову "
               "лінію простого алгоритму більше не перевершено."),
    ("callout", "Чесне обмеження",
     "Вбудована еталонна мережа навчена на синтетичному детермінованому корпусі скіда. Вона чудова для "
     "відтворюваних доказів конвеєра та регресії межі безпеки, але це доказ симуляції/HIL — не "
     "сертифікований контролер безпеки. Вона не замінює валідацію PLC/SIS, аналіз небезпек, керування "
     "змінами чи приймальні випробування на майданчику. Реальна точність потребує валідації на зовнішніх "
     "даних історіана, CMMS та лічильників енергії.", "warning"),

    ("h1", "Усунення несправностей", "14"),
    ("table", ["Симптом", "Імовірна причина й виправлення"],
     [["`Cannot find class … in provided jar`", "Бракує класу з `configuration.neuronnet.classes` — перезберіть JAR розгортання"],
      ["Worker стартує, але нічого не видає", "Перевірте `clazz` джерела подій і що `readSignals()` повертає типізовані сигнали"],
      ["Структуру не завантажено", "Переконайтеся, що `configuration.input.layermeta` вказує на згенеровану теку рівнів"],
      ["Worker одразу завершується", "Для живого наглядача встановіть `infiniteRun=true`; `maxRun` — для обмеженого відтворення"],
      ["Рекомендація намагається керувати", "Перевірте `industrial.autonomousAction=false` і що рівень відповіді ADVISORY"],
      ["Застарілі ваги під час роботи", "Ви розгорнули стару теку рівнів; розгорніть найновіший навчений пакет"]],
     [3.0, 3.8]),

    ("h1", "Додаток — шпаргалка розгортання", "15"),
    ("h3", "Запуск промислового рекомендаційного worker"),
    ("code",
     "java -cp worker/target/worker-jar-with-dependencies.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
     "  local file:///opt/jneopallium/industrial-loop-guardian.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext \\\n"
     "  /opt/jneopallium/model/industrial-loop-guardian/production-context.json"),
    ("h3", "Основи контексту рекомендаційного worker"),
    ("code",
     "configuration.input.layermeta   = model/industrial-loop-guardian/\n"
     "configuration.isteacherstudying = false\n"
     "configuration.discriminatorsAmount = 0\n"
     "configuration.infiniteRun       = true\n"
     "configuration.runoncein         = 1\n"
     "industrial.autonomousAction     = false"),
    ("spacer", 8),
    ("pi", "Jneopallium Industrial Loop Guardian · Посібник з розгортання. Споживає мережу, створену "
           "Посібником з навчання. Наглядовий над PLC/PID/SIS. Режим безпеки: ADVISORY. Записи OPC UA "
           "потребують окремого обґрунтування безпеки майданчика. Ліцензія: BSD 3-Clause."),
]
