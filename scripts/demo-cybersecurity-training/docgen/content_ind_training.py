# -*- coding: utf-8 -*-
"""Industrial Loop Guardian — training guide (EN + UK), block DSL."""

from __future__ import annotations

DATE = "23 June 2026"
DATE_UK = "23 червня 2026"


def training(lang: str) -> list:
    return _EN if lang == "en" else _UK


_EN = [
    ("cover", "Jneopallium · Industrial Loop Guardian",
     "Training Guide",
     "FMI Skid Demo — build, run, train, and produce a deployable supervisory network",
     [("Document", "Model Training Guide"),
      ("Product", "Jneopallium Industrial Loop Guardian (FMI Skid Demo)"),
      ("Model", "industrial-loop-guardian 1.0.0"),
      ("Trainer", "scripts/demo-industrial-fmi/train_loop_guardian_model.py"),
      ("Output", "Deployable JNeopallium layer/neuron config"),
      ("Author", "Dmytro Rakovskyi — Kharkiv, Ukraine"),
      ("Date", DATE),
      ("License", "BSD 3-Clause")],
     "Training Guide",
     "A complete, copy-paste walkthrough of the training half of the pipeline: install the toolchain, "
     "build the platform, run the skid scenarios, train the four diagnostic heads, and produce the "
     "ready-to-deploy supervisory network the Deployment Guide consumes."),

    ("toc", "Contents",
     ["What you will achieve",
      "Where training ends and deployment begins",
      "Prerequisites and system requirements",
      "Step 1 — Get the source and build",
      "Step 2 — Run the FMI skid scenarios",
      "Step 3 — Understand the generated outputs",
      "Step 4 — Train the Loop Guardian model",
      "Step 5 — Fast Python model verification",
      "Step 6 — Generate training and run reports",
      "Step 7 — The generated artifacts: a deployable network",
      "Step 8 — Verify the run against the gates",
      "Troubleshooting",
      "Appendix — training cheat-sheet"]),

    ("h1", "What you will achieve", "1"),
    ("p", "By the end of this guide you will have: a built Jneopallium platform; deterministic FMI skid "
          "runs across nine scenarios; a freshly trained **Industrial Loop Guardian** maintenance-and-"
          "energy model; and — the key output — a **complete, deployable JNeopallium network** (five layer "
          "files, real runtime classes, embedded trained heads, a fixed safety gate, and a ready-made "
          "production context) that the companion **Deployment Guide** loads directly."),
    ("callout", "Safety ceiling for the whole pipeline",
     "Everything the trained model does is supervisory and ADVISORY. PLC/PID/SIS controls remain "
     "deterministic and authoritative; the trainer encodes this — the response layer is ADVISORY, hard "
     "safety gates are fixed configuration, and OPC UA actuator writes stay separate.", "warning"),

    ("h1", "Where training ends and deployment begins", "2"),
    ("table", ["Phase", "What happens", "Document"],
     [["**Training** (this guide)", "Build, run scenarios, train on data, **emit the deployable network**",
       "Training Guide"],
      ["**Deployment**", "Load the emitted network, configure event sources, run the advisory worker",
       "Deployment Guide"]],
     [1.9, 3.3, 1.6]),
    ("p", "The hand-off is a single directory of generated artifacts (Step 7). Training **produces** it; "
          "deployment **consumes** it. The trainer writes JNeopallium-ready layer and neuron "
          "configuration *and* a production context file, so the same files that record your training run "
          "are the files the worker boots from."),

    ("h1", "Prerequisites and system requirements", "3"),
    ("h2", "Toolchain"),
    ("table", ["Tool", "Version", "Why"],
     [["Java JDK", "17 or newer (LTS)", "Build and run the worker / controller"],
      ["Apache Maven", "3.9 or newer", "Multi-module build of master + worker"],
      ["Python", "3.10+ (3.13 tested)", "Runs the trainer, the demo runner, and report generator"],
      ["Git", "any recent", "Clone the repository"],
      ["Docker + Mosquitto (optional)", "any recent", "Only for the full OPC UA + MQTT protocol path"]],
     [2.2, 1.8, 3.0]),
    ("p", "The default one-command runner produces deterministic evidence **without** Docker, a broker, or "
          "an OPC UA server. The full protocol path (gateway + `IndustrialFmiDemoMain`) additionally needs "
          "Maven, Mosquitto/Docker, the Python gateway dependencies, and a built FMU."),
    ("h2", "Verify the toolchain"),
    ("code",
     "java -version       # expect 17 or newer\n"
     "mvn -version        # expect 3.9 or newer\n"
     "python --version    # expect 3.10 or newer"),

    ("h1", "Step 1 — Get the source and build", "4"),
    ("code",
     "git clone https://github.com/rakovpublic/jneopallium.git\n"
     "cd jneopallium\n"
     "mvn clean install"),
    ("p", "The build installs `com.rakovpublic.jneopallium:worker:1.0`. The worker already contains the "
          "real industrial neuron, processor, and signal classes the trained network references "
          "(`com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.*` and friends), so no extra "
          "hand-written classes are required to train."),

    ("h1", "Step 2 — Run the FMI skid scenarios", "5"),
    ("p", "The deterministic runner advances the simulated skid and writes evidence for each scenario. Run "
          "all nine, or one at a time:"),
    ("h3", "Linux / macOS"),
    ("code",
     "scripts/demo-industrial-fmi/run_demo.sh all\n"
     "scripts/demo-industrial-fmi/run_demo.sh pump-wear\n"
     "scripts/demo-industrial-fmi/run_demo.sh high-temperature-interlock"),
    ("h3", "Windows (PowerShell)"),
    ("code", "powershell -ExecutionPolicy Bypass -File scripts/demo-industrial-fmi/run_demo.ps1 all"),
    ("p", "The nine scenarios are `normal`, `load-disturbance`, `oscillation`, `pump-wear`, "
          "`temperature-sensor-drift`, `mqtt-outage`, `opcua-outage`, `high-temperature-interlock`, and "
          "`operator-override`. Each exercises a different part of the supervisory and safety behaviour."),

    ("h1", "Step 3 — Understand the generated outputs", "6"),
    ("p", "Each run writes a per-scenario folder under `target/`:"),
    ("code",
     "target/jneopallium-industrial-fmi/<scenario>/\n"
     "  manifest.json            process_trace.csv        controller_results.jsonl\n"
     "  advisory_findings.jsonl  model_advisory_findings.jsonl\n"
     "  heuristic_advisory_findings.jsonl\n"
     "  opcua_audit.jsonl        mqtt_audit.jsonl         alarms.jsonl\n"
     "  interventions.jsonl      metrics.json             comparison.json"),
    ("table", ["File", "What it holds"],
     [["`metrics.json`", "Scenario KPIs: IAE, overshoot, settling, energy, interlock latency, availability"],
      ["`comparison.json`", "Baseline-versus-Jneopallium comparison"],
      ["`model_advisory_findings.jsonl`", "Advisories from the trained model"],
      ["`advisory_findings.jsonl`", "Advisories with owning neuron, economic basis, safety envelope, boundary"],
      ["`opcua_audit.jsonl` / `mqtt_audit.jsonl`", "Every command/telemetry event, for the protocol boundary"]],
     [3.0, 3.8]),

    ("h1", "Step 4 — Train the Loop Guardian model", "7"),
    ("p", "The trainer needs no third-party Python packages. Train and export the maintenance-and-energy "
          "model with the production-scale reference command:"),
    ("code",
     "python scripts/demo-industrial-fmi/train_loop_guardian_model.py \\\n"
     "  --reference-multiplier 1000 \\\n"
     "  --target-corpus-bytes 100gb \\\n"
     "  --max-corpus-bytes 100gb"),
    ("p", "It writes the full artifact set to "
          "`worker/src/main/resources/model/industrial-loop-guardian/` (see Step 7). The trainer fits four "
          "diagnostic heads over 39 engineered features, evaluates them on a leakage-safe split, and "
          "prints a summary including per-finding precision/recall/F1."),
    ("callout", "What the flags mean",
     "`--reference-multiplier` deterministically expands the bundled skid corpus. `--target-corpus-bytes` "
     "records the logical production-scale target (100 GiB) reached by replication. `--max-corpus-bytes` "
     "is a hard guardrail that aborts rather than inflate the repository — the fitted sample stays a few "
     "tens of MiB so the run completes on a workstation.", "info"),

    ("h1", "Step 5 — Fast Python model verification", "8"),
    ("p", "For a quick, build-free check of the model logic, run the Python unit tests:"),
    ("code", "python -m unittest discover -s scripts/demo-industrial-fmi/tests"),
    ("p", "These exercise the trainer and the loop-guardian model end to end without Maven, a broker, or a "
          "built FMU — ideal for a fast feedback loop while iterating on features or the corpus."),

    ("h1", "Step 6 — Generate training and run reports", "9"),
    ("p", "After training and replay, render consolidated reports:"),
    ("code", "python scripts/demo-industrial-fmi/generate_run_reports.py"),
    ("p", "This collates the per-scenario metrics and the trained-model evidence into reproducible report "
          "artifacts you can attach to a pilot assessment."),

    ("h1", "Step 7 — The generated artifacts: a deployable network", "10"),
    ("p", "This is the pipeline's hand-off. The training run writes a directory of JNeopallium-style "
          "configuration that is ready to deploy as-is — the actual layer-and-neuron network the worker "
          "boots from, plus a ready-made production context:"),
    ("table", ["Artifact", "What it is"],
     [["`trained-industrial-loop-guardian-model.json`", "The compact trained model: features, four heads, weights, metrics"],
      ["`model-descriptor.json`", "Whole-network descriptor: 5 layers, 17 neurons, frequency map, runtime classes"],
      ["`layer-0.json`", "Plant + supervisory-context input boundary"],
      ["`layer-1-fast-telemetry.json`", "Fast telemetry validation and loop state — 7 neurons"],
      ["`layer-2-maintenance-energy.json`", "The four trained diagnostic finding heads"],
      ["`layer-3-advisory-planning.json`", "Economic advisory planning + the fixed safety gate — 4 neurons"],
      ["`result-layer.json`", "Industrial advisory JSONL output"],
      ["`trained-model-update.json`", "Latest trained snapshot: status, checksum, weights, training summary"],
      ["`quantitative-summary.json`", "Scale, metrics, and top positive / negative feature weights per head"],
      ["`source-mapping.json`", "Which typed signals each data source maps onto"],
      ["`production-context.json`", "A ready-to-run advisory-worker context for deployment"]],
     [3.3, 3.5]),
    ("h2", "The network the trainer builds"),
    ("p", "`model-descriptor.json` records a five-layer network of **17 real neurons**, with **156 "
          "trainable weights** and **4 biases**, every neuron referencing a concrete class from the "
          "worker's industrial package:"),
    ("table", ["Layer", "Size", "Role"],
     [["0 — Input", "0", "Plant / OPC UA / MQTT / FMI replay / Kafka boundary"],
      ["1 — Fast telemetry", "7", "Validation, interlocks, override, fast loop state"],
      ["2 — Diagnostic heads", "4", "Pump wear, oscillation/stiction, sensor drift, energy"],
      ["3 — Advisory planning", "4", "Maintenance scheduling, bounded trim, economic basis, safety gate"],
      ["4 — Result", "2", "Maintenance and energy advisories as JSONL"]],
     [2.2, 0.8, 3.7]),
    ("p", "Each diagnostic head embeds its trained weights, the feature gate it is allowed to use (the "
          "oscillation head, for example, is gated to 17 of the 39 features), and its logical role and "
          "owned reasoning. The descriptor also carries a `signalFrequencyMap` giving each signal its loop "
          "cadence — fast tags every loop, degradation/energy/setpoint every 10, maintenance windows every "
          "60 — the multi-timescale design made concrete."),
    ("callout", "Why this matters",
     "Because training emits real, inspectable, deployable configuration, there is no translation step "
     "between \"the model we trained\" and \"the network we run.\" The Deployment Guide simply points the "
     "worker at this directory and loads `production-context.json`.", "success"),

    ("h1", "Step 8 — Verify the run against the gates", "11"),
    ("num", "All expected source families are present in `source-mapping.json` (FMI replay, OPC UA, MQTT, "
            "Kafka shadow, CMMS, energy meter)."),
    ("num", "Per-finding F1 is at or near 1.0, and the energy head's false-positive rate is within budget "
            "(reference run: macro-F1 ≈ 0.9995, macro false-positive rate ≈ 0.0005)."),
    ("num", "The five layer files, `trained-model-update.json`, and `production-context.json` were written "
            "and reference the expected runtime classes."),
    ("num", "Every scenario produced deterministic traces and a baseline comparison."),
    ("num", "Advisory JSON includes the owning neuron, recommendation code, economic basis, "
            "safety-envelope result, and the PLC/PID/SIS-versus-Jneopallium boundary."),
    ("callout", "Reproducibility check",
     "The reference run is deterministic. Re-running the same command must produce the same training "
     "checksum (`trained-model-update.json`) and the same per-finding metrics. A drift in either signals "
     "that an input or the toolchain changed.", "info"),

    ("h1", "Troubleshooting", "12"),
    ("table", ["Symptom", "Likely cause and fix"],
     [["`java -version` shows < 17", "Install a JDK 17+ and set `JAVA_HOME`"],
      ["Trainer cannot find scenarios", "Run from the repo root; scenarios live in `scripts/demo-industrial-fmi/config/scenarios/`"],
      ["\"estimated canonical corpus exceeds --max-corpus-bytes\"", "Lower `--reference-multiplier`/`--target-corpus-bytes` or raise the guardrail deliberately"],
      ["FMU / gateway errors", "The default runner needs none of these; only the full protocol path requires the FMU + broker"],
      ["Layer files not written", "Confirm `--output-dir` is writable; the trainer writes all artifacts there"],
      ["Unit tests fail after a change", "A feature or corpus edit changed behaviour; review the diff and expected metrics"]],
     [3.1, 3.7]),

    ("h1", "Appendix — training cheat-sheet", "13"),
    ("h3", "Build"),
    ("code", "git clone https://github.com/rakovpublic/jneopallium.git && cd jneopallium && mvn clean install"),
    ("h3", "Run scenarios"),
    ("code",
     "scripts/demo-industrial-fmi/run_demo.sh all            # Linux/macOS\n"
     "powershell -File scripts/demo-industrial-fmi/run_demo.ps1 all   # Windows"),
    ("h3", "Train, verify, report"),
    ("code",
     "python scripts/demo-industrial-fmi/train_loop_guardian_model.py \\\n"
     "  --reference-multiplier 1000 --target-corpus-bytes 100gb --max-corpus-bytes 100gb\n"
     "\n"
     "python -m unittest discover -s scripts/demo-industrial-fmi/tests\n"
     "python scripts/demo-industrial-fmi/generate_run_reports.py"),
    ("spacer", 8),
    ("pi", "Jneopallium Industrial Loop Guardian · Training Guide. The trainer emits a deployable "
           "JNeopallium network plus a production context; see the Deployment Guide to run it. "
           "Supervisory above PLC/PID/SIS. Safety mode: ADVISORY. License: BSD 3-Clause."),
]


_UK = [
    ("cover", "Jneopallium · Industrial Loop Guardian",
     "Посібник з навчання",
     "Демо FMI-скіда — зберіть, запустіть, навчіть і отримайте готову наглядову мережу",
     [("Документ", "Посібник з навчання моделі"),
      ("Продукт", "Jneopallium Industrial Loop Guardian (демо FMI-скіда)"),
      ("Модель", "industrial-loop-guardian 1.0.0"),
      ("Тренер", "scripts/demo-industrial-fmi/train_loop_guardian_model.py"),
      ("Вихід", "Готова до розгортання конфігурація JNeopallium"),
      ("Автор", "Дмитро Раковський — Харків, Україна"),
      ("Дата", DATE_UK),
      ("Ліцензія", "BSD 3-Clause")],
     "Посібник з навчання",
     "Повний покроковий маршрут для копіювання навчальної половини конвеєра: встановіть інструментарій, "
     "зберіть платформу, запустіть сценарії скіда, навчіть чотири діагностичні голови й отримайте готову "
     "до розгортання наглядову мережу, яку споживає Посібник з розгортання."),

    ("toc", "Зміст",
     ["Чого ви досягнете",
      "Де закінчується навчання і починається розгортання",
      "Передумови та системні вимоги",
      "Крок 1 — Отримати код і зібрати",
      "Крок 2 — Запустити сценарії FMI-скіда",
      "Крок 3 — Зрозуміти згенеровані результати",
      "Крок 4 — Навчити модель Loop Guardian",
      "Крок 5 — Швидка перевірка моделі на Python",
      "Крок 6 — Згенерувати звіти про навчання та запуски",
      "Крок 7 — Згенеровані артефакти: готова до розгортання мережа",
      "Крок 8 — Перевірити запуск за контрольними критеріями",
      "Усунення несправностей",
      "Додаток — шпаргалка навчання"]),

    ("h1", "Чого ви досягнете", "1"),
    ("p", "До кінця цього посібника ви матимете: зібрану платформу Jneopallium; детерміновані запуски "
          "FMI-скіда за дев'ятьма сценаріями; свіжонавчену модель обслуговування й енергії **Industrial "
          "Loop Guardian**; і — ключовий результат — **повну, готову до розгортання мережу JNeopallium** "
          "(п'ять файлів рівнів, справжні класи часу виконання, вбудовані навчені голови, фіксований "
          "запобіжник і готовий промисловий контекст), яку напряму завантажує супровідний **Посібник з "
          "розгортання**."),
    ("callout", "Стеля безпеки для всього конвеєра",
     "Усе, що робить навчена модель, є наглядовим і ADVISORY. Керування PLC/PID/SIS лишається "
     "детермінованим і авторитетним; тренер це кодує — рівень відповіді ADVISORY, жорсткі запобіжники — "
     "фіксована конфігурація, а записи виконавчих механізмів OPC UA лишаються окремими.", "warning"),

    ("h1", "Де закінчується навчання і починається розгортання", "2"),
    ("table", ["Фаза", "Що відбувається", "Документ"],
     [["**Навчання** (цей посібник)", "Зборка, запуск сценаріїв, навчання, **видача мережі**",
       "Посібник з навчання"],
      ["**Розгортання**", "Завантаження виданої мережі, налаштування джерел подій, запуск worker",
       "Посібник з розгортання"]],
     [2.0, 3.2, 1.6]),
    ("p", "Передача — це єдина тека згенерованих артефактів (Крок 7). Навчання її **створює**; розгортання "
          "її **споживає**. Тренер записує готову для JNeopallium конфігурацію рівнів і нейронів *і* файл "
          "промислового контексту, тож ті самі файли, що фіксують навчання, є файлами, з яких "
          "завантажується worker."),

    ("h1", "Передумови та системні вимоги", "3"),
    ("h2", "Інструментарій"),
    ("table", ["Інструмент", "Версія", "Навіщо"],
     [["Java JDK", "17 або новіша (LTS)", "Зборка й запуск worker / контролера"],
      ["Apache Maven", "3.9 або новіша", "Багатомодульна збірка master + worker"],
      ["Python", "3.10+ (тестовано 3.13)", "Запускає тренер, раннер демо й генератор звітів"],
      ["Git", "будь-яка свіжа", "Клонування репозиторію"],
      ["Docker + Mosquitto (опційно)", "будь-яка свіжа", "Лише для повного шляху OPC UA + MQTT"]],
     [2.4, 1.7, 2.9]),
    ("p", "Типовий раннер «однієї команди» дає детерміновані докази **без** Docker, брокера чи сервера "
          "OPC UA. Повний протокольний шлях (шлюз + `IndustrialFmiDemoMain`) додатково потребує Maven, "
          "Mosquitto/Docker, залежностей Python-шлюзу та зібраного FMU."),
    ("h2", "Перевірте інструментарій"),
    ("code",
     "java -version       # очікуємо 17 або новіше\n"
     "mvn -version        # очікуємо 3.9 або новіше\n"
     "python --version    # очікуємо 3.10 або новіше"),

    ("h1", "Крок 1 — Отримати код і зібрати", "4"),
    ("code",
     "git clone https://github.com/rakovpublic/jneopallium.git\n"
     "cd jneopallium\n"
     "mvn clean install"),
    ("p", "Збірка встановлює `com.rakovpublic.jneopallium:worker:1.0`. Worker уже містить справжні класи "
          "промислових нейронів, процесорів і сигналів, на які посилається навчена мережа "
          "(`com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.*` та інші), тож для навчання "
          "не потрібні додаткові написані вручну класи."),

    ("h1", "Крок 2 — Запустити сценарії FMI-скіда", "5"),
    ("p", "Детермінований раннер просуває змодельований скід і записує докази для кожного сценарію. "
          "Запустіть усі дев'ять або по одному:"),
    ("h3", "Linux / macOS"),
    ("code",
     "scripts/demo-industrial-fmi/run_demo.sh all\n"
     "scripts/demo-industrial-fmi/run_demo.sh pump-wear\n"
     "scripts/demo-industrial-fmi/run_demo.sh high-temperature-interlock"),
    ("h3", "Windows (PowerShell)"),
    ("code", "powershell -ExecutionPolicy Bypass -File scripts/demo-industrial-fmi/run_demo.ps1 all"),
    ("p", "Дев'ять сценаріїв: `normal`, `load-disturbance`, `oscillation`, `pump-wear`, "
          "`temperature-sensor-drift`, `mqtt-outage`, `opcua-outage`, `high-temperature-interlock` та "
          "`operator-override`. Кожен вправляє іншу частину наглядової та безпекової поведінки."),

    ("h1", "Крок 3 — Зрозуміти згенеровані результати", "6"),
    ("p", "Кожен запуск записує теку для сценарію під `target/`:"),
    ("code",
     "target/jneopallium-industrial-fmi/<scenario>/\n"
     "  manifest.json            process_trace.csv        controller_results.jsonl\n"
     "  advisory_findings.jsonl  model_advisory_findings.jsonl\n"
     "  heuristic_advisory_findings.jsonl\n"
     "  opcua_audit.jsonl        mqtt_audit.jsonl         alarms.jsonl\n"
     "  interventions.jsonl      metrics.json             comparison.json"),
    ("table", ["Файл", "Що містить"],
     [["`metrics.json`", "KPI сценарію: IAE, перерегулювання, час усталення, енергія, затримка інтерлока, доступність"],
      ["`comparison.json`", "Порівняння базової лінії та Jneopallium"],
      ["`model_advisory_findings.jsonl`", "Рекомендації від навченої моделі"],
      ["`advisory_findings.jsonl`", "Рекомендації з нейроном-власником, економічним базисом, межею безпеки, межею"],
      ["`opcua_audit.jsonl` / `mqtt_audit.jsonl`", "Кожна подія команди/телеметрії, для межі протоколів"]],
     [3.0, 3.8]),

    ("h1", "Крок 4 — Навчити модель Loop Guardian", "7"),
    ("p", "Тренер не потребує сторонніх пакетів Python. Навчіть і експортуйте модель обслуговування й "
          "енергії командою еталона промислового масштабу:"),
    ("code",
     "python scripts/demo-industrial-fmi/train_loop_guardian_model.py \\\n"
     "  --reference-multiplier 1000 \\\n"
     "  --target-corpus-bytes 100gb \\\n"
     "  --max-corpus-bytes 100gb"),
    ("p", "Він записує повний набір артефактів у "
          "`worker/src/main/resources/model/industrial-loop-guardian/` (див. Крок 7). Тренер припасовує "
          "чотири діагностичні голови над 39 інженерними ознаками, оцінює їх на розбитті без витоку й "
          "друкує зведення з точністю/повнотою/F1 для кожного висновку."),
    ("callout", "Що означають прапорці",
     "`--reference-multiplier` детерміновано розширює вбудований корпус скіда. `--target-corpus-bytes` "
     "фіксує логічну ціль промислового масштабу (100 ГіБ), досягнуту реплікацією. `--max-corpus-bytes` — "
     "жорсткий запобіжник, що переривається, замість роздувати репозиторій — припасована вибірка лишається "
     "кілька десятків МіБ, тож запуск завершується на робочій станції.", "info"),

    ("h1", "Крок 5 — Швидка перевірка моделі на Python", "8"),
    ("p", "Для швидкої перевірки логіки моделі без збірки запустіть модульні тести Python:"),
    ("code", "python -m unittest discover -s scripts/demo-industrial-fmi/tests"),
    ("p", "Вони проганяють тренер і модель loop-guardian наскрізно без Maven, брокера чи зібраного FMU — "
          "ідеально для швидкого зворотного зв'язку під час ітерацій над ознаками чи корпусом."),

    ("h1", "Крок 6 — Згенерувати звіти про навчання та запуски", "9"),
    ("p", "Після навчання й відтворення згенеруйте зведені звіти:"),
    ("code", "python scripts/demo-industrial-fmi/generate_run_reports.py"),
    ("p", "Це збирає метрики кожного сценарію та докази навченої моделі у відтворювані звіти, які можна "
          "додати до пілотної оцінки."),

    ("h1", "Крок 7 — Згенеровані артефакти: готова до розгортання мережа", "10"),
    ("p", "Це передача конвеєра. Запуск навчання записує теку конфігурації у стилі JNeopallium, готову до "
          "розгортання як є, — власне мережу рівнів і нейронів, з якої завантажується worker, плюс готовий "
          "промисловий контекст:"),
    ("table", ["Артефакт", "Що це"],
     [["`trained-industrial-loop-guardian-model.json`", "Компактна навчена модель: ознаки, чотири голови, ваги, метрики"],
      ["`model-descriptor.json`", "Опис усієї мережі: 5 рівнів, 17 нейронів, мапа частот, класи часу виконання"],
      ["`layer-0.json`", "Межа входу заводу + наглядового контексту"],
      ["`layer-1-fast-telemetry.json`", "Валідація швидкої телеметрії і стан контуру — 7 нейронів"],
      ["`layer-2-maintenance-energy.json`", "Чотири навчені діагностичні голови висновків"],
      ["`layer-3-advisory-planning.json`", "Економічне планування рекомендацій + фіксований запобіжник — 4 нейрони"],
      ["`result-layer.json`", "Вивід промислових рекомендацій JSONL"],
      ["`trained-model-update.json`", "Останній знімок навчання: статус, контрольна сума, ваги, зведення"],
      ["`quantitative-summary.json`", "Масштаб, метрики та найвагоміші ваги ознак на голову"],
      ["`source-mapping.json`", "На які типізовані сигнали відображається кожне джерело даних"],
      ["`production-context.json`", "Готовий контекст рекомендаційного worker для розгортання"]],
     [3.3, 3.5]),
    ("h2", "Мережа, яку будує тренер"),
    ("p", "`model-descriptor.json` фіксує п'ятирівневу мережу з **17 справжніх нейронів**, з **156 "
          "навчуваними вагами** та **4 зсувами**, де кожен нейрон посилається на конкретний клас із "
          "промислового пакета worker:"),
    ("table", ["Рівень", "Розмір", "Роль"],
     [["0 — Вхід", "0", "Межа заводу / OPC UA / MQTT / FMI-відтворення / Kafka"],
      ["1 — Швидка телеметрія", "7", "Валідація, інтерлоки, перевизначення, стан контуру"],
      ["2 — Діагностичні голови", "4", "Знос насоса, коливання/заклинювання, дрейф датчика, енергія"],
      ["3 — Планування рекомендацій", "4", "Планування обслуговування, обмежений трим, економіка, запобіжник"],
      ["4 — Результат", "2", "Рекомендації з обслуговування та енергії як JSONL"]],
     [2.2, 0.8, 3.7]),
    ("p", "Кожна діагностична голова вбудовує свої навчені ваги, дозволений їй фільтр ознак (голова "
          "коливань, наприклад, обмежена 17 із 39 ознак) та свою логічну роль і власне міркування. Опис "
          "також містить `signalFrequencyMap`, що дає кожному сигналу його каданс — швидкі теги щоцикл, "
          "деградація/енергія/уставка кожні 10, вікна обслуговування кожні 60 — багаточасовий дизайн, "
          "втілений конкретно."),
    ("callout", "Чому це важливо",
     "Оскільки навчання видає справжню, придатну до перевірки, готову до розгортання конфігурацію, між "
     "«тим, що ми навчили» і «тим, що ми запускаємо», немає кроку перекладу. Посібник з розгортання просто "
     "спрямовує worker на цю теку й завантажує `production-context.json`.", "success"),

    ("h1", "Крок 8 — Перевірити запуск за контрольними критеріями", "11"),
    ("num", "Усі очікувані родини джерел присутні в `source-mapping.json` (FMI-відтворення, OPC UA, MQTT, "
            "тіньовий Kafka, CMMS, лічильник енергії)."),
    ("num", "F1 кожного висновку на рівні або близько 1.0, а рівень хибних спрацювань енергетичної голови "
            "в межах бюджету (еталонний запуск: macro-F1 ≈ 0.9995, macro рівень хибних ≈ 0.0005)."),
    ("num", "П'ять файлів рівнів, `trained-model-update.json` і `production-context.json` записано, і вони "
            "посилаються на очікувані класи часу виконання."),
    ("num", "Кожен сценарій дав детерміновані траси й порівняння з базовою лінією."),
    ("num", "Рекомендаційний JSON містить нейрон-власник, код рекомендації, економічний базис, результат "
            "межі безпеки та межу PLC/PID/SIS проти Jneopallium."),
    ("callout", "Перевірка відтворюваності",
     "Еталонний запуск детермінований. Повторний запуск тієї самої команди має дати ту саму контрольну "
     "суму (`trained-model-update.json`) і ті самі метрики на голову. Розбіжність — сигнал, що змінилися "
     "вхідні дані чи інструментарій.", "info"),

    ("h1", "Усунення несправностей", "12"),
    ("table", ["Симптом", "Імовірна причина й виправлення"],
     [["`java -version` показує < 17", "Встановіть JDK 17+ і вкажіть `JAVA_HOME`"],
      ["Тренер не знаходить сценарії", "Запускайте з кореня репозиторію; сценарії в `scripts/demo-industrial-fmi/config/scenarios/`"],
      ["«estimated canonical corpus exceeds --max-corpus-bytes»", "Знизьте `--reference-multiplier`/`--target-corpus-bytes` або свідомо підніміть запобіжник"],
      ["Помилки FMU / шлюзу", "Типовий раннер їх не потребує; лише повний шлях потребує FMU + брокер"],
      ["Файли рівнів не записано", "Переконайтеся, що `--output-dir` доступний для запису; тренер пише туди всі артефакти"],
      ["Модульні тести падають після зміни", "Зміна ознаки чи корпусу змінила поведінку; перегляньте diff та очікувані метрики"]],
     [3.1, 3.7]),

    ("h1", "Додаток — шпаргалка навчання", "13"),
    ("h3", "Збірка"),
    ("code", "git clone https://github.com/rakovpublic/jneopallium.git && cd jneopallium && mvn clean install"),
    ("h3", "Запуск сценаріїв"),
    ("code",
     "scripts/demo-industrial-fmi/run_demo.sh all            # Linux/macOS\n"
     "powershell -File scripts/demo-industrial-fmi/run_demo.ps1 all   # Windows"),
    ("h3", "Навчання, перевірка, звіти"),
    ("code",
     "python scripts/demo-industrial-fmi/train_loop_guardian_model.py \\\n"
     "  --reference-multiplier 1000 --target-corpus-bytes 100gb --max-corpus-bytes 100gb\n"
     "\n"
     "python -m unittest discover -s scripts/demo-industrial-fmi/tests\n"
     "python scripts/demo-industrial-fmi/generate_run_reports.py"),
    ("spacer", 8),
    ("pi", "Jneopallium Industrial Loop Guardian · Посібник з навчання. Тренер видає готову до розгортання "
           "мережу JNeopallium плюс промисловий контекст; як її запустити — у Посібнику з розгортання. "
           "Наглядовий над PLC/PID/SIS. Режим безпеки: ADVISORY. Ліцензія: BSD 3-Clause."),
]
