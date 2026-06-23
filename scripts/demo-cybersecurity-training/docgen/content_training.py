# -*- coding: utf-8 -*-
"""Training guide content (EN + UK), block DSL.

Covers build, demo run, and every training mode, and documents the
deployable JNeopallium network artifacts the trainer now emits.
"""

from __future__ import annotations

DATE = "23 June 2026"
DATE_UK = "23 червня 2026"


def training(lang: str) -> list:
    return _EN if lang == "en" else _UK


_EN = [
    ("cover", "Jneopallium · Demo 06",
     "Training Guide",
     "Cybersecurity Temporal Threat Correlation — build, train, and produce a deployable network",
     [("Document", "Model Training Guide"),
      ("Product", "Jneopallium Cybersecurity Module (Demo 06)"),
      ("Model", "cybersecurity-temporal-threat-correlator 1.0.0"),
      ("Trainer", "scripts/demo-cybersecurity-training/train_temporal_model.py"),
      ("Output", "Deployable JNeopallium layer/neuron config"),
      ("Author", "Dmytro Rakovskyi — Kharkiv, Ukraine"),
      ("Date", DATE),
      ("License", "BSD 3-Clause")],
     "Training Guide",
     "A complete, copy-paste walkthrough of the training half of the pipeline: install the toolchain, "
     "build the platform, run the demo, train the model on reference and real datasets, and produce the "
     "ready-to-deploy network the Deployment Guide consumes."),

    ("toc", "Contents",
     ["What you will achieve",
      "Where training ends and deployment begins",
      "Prerequisites and system requirements",
      "Step 1 — Get the source and build",
      "Step 2 — Run the cybersecurity demo",
      "Step 3 — Understand the generated demo outputs",
      "Step 4 — Train the reference model",
      "Step 5 — Train at production scale",
      "Step 6 — Train on external datasets (manifest mode)",
      "Step 7 — Train on raw LANL + ToN_IoT",
      "Step 8 — The generated artifacts: a deployable network",
      "Step 9 — Verify the run against the gates",
      "Troubleshooting",
      "Appendix — training cheat-sheet"]),

    ("h1", "What you will achieve", "1"),
    ("p", "By the end of this guide you will have: a built Jneopallium platform; a runnable cybersecurity "
          "demo producing advisory JSONL; a freshly trained temporal threat-correlation model; and — the "
          "key output — a **complete, deployable JNeopallium network** (layer and neuron configuration "
          "files with real runtime classes, embedded trained weights, and a fixed safety gate) that the "
          "companion **Deployment Guide** loads directly."),
    ("callout", "Safety ceiling for the whole pipeline",
     "Everything the trained model does operates in ADVISORY mode. It may recommend investigation or "
     "quarantine candidates, but it must not isolate hosts or block traffic. The trainer encodes this "
     "directly: the response layer is marked ADVISORY and the hard gate is fixed configuration, never "
     "learned.", "warning"),

    ("h1", "Where training ends and deployment begins", "2"),
    ("p", "The pipeline has two clean halves, and this guide covers the first one:"),
    ("table", ["Phase", "What happens", "Document"],
     [["**Training** (this guide)", "Build, run the demo, train on data, **emit the deployable network**",
       "Training Guide"],
      ["**Deployment**", "Load the emitted network, configure event sources, run the advisory worker",
       "Deployment Guide"]],
     [1.9, 3.3, 1.6]),
    ("p", "The hand-off between the two is a single directory of generated artifacts (Step 8). Training "
          "**produces** it; deployment **consumes** it. Nothing is hand-authored between them — the trainer "
          "writes JNeopallium-ready layer and neuron configuration, so the same files that record your "
          "training run are the files the worker boots from."),

    ("h1", "Prerequisites and system requirements", "3"),
    ("h2", "Toolchain"),
    ("table", ["Tool", "Version", "Why"],
     [["Java JDK", "17 or newer (LTS)", "The platform targets the Java 17 baseline API set"],
      ["Apache Maven", "3.9 or newer", "Multi-module build of master + worker"],
      ["Python", "3.10+ (3.13 tested)", "Runs the model trainer; no third-party packages required"],
      ["Git", "any recent", "Clone the repository"]],
     [1.8, 2.0, 3.0]),
    ("h2", "Hardware"),
    ("bullet", "**Demo / pilot:** any modern laptop. The reference trainer runs comfortably in a single JVM "
               "and the desktop-scale dataset is a few tens of megabytes."),
    ("bullet", "**Production-scale reference run:** the trainer records a 100 GiB *logical* corpus target "
               "without writing 100 GiB to disk, so it still runs on a workstation. Real external-dataset "
               "training scales with the data you feed it."),
    ("h2", "Verify the toolchain"),
    ("code",
     "java -version       # expect 17 or newer\n"
     "mvn -version        # expect 3.9 or newer\n"
     "python --version    # expect 3.10 or newer\n"
     "git --version"),

    ("h1", "Step 1 — Get the source and build", "4"),
    ("p", "Clone the repository and build both modules. The build installs the `master` and `worker` "
          "artifacts into your local Maven repository."),
    ("code",
     "git clone https://github.com/rakovpublic/jneopallium.git\n"
     "cd jneopallium\n"
     "mvn clean install"),
    ("p", "A successful build compiles the platform, runs the test suite (including the cybersecurity "
          "`SecurityModuleTest`), and installs `com.rakovpublic.jneopallium:worker:1.0`. The worker already "
          "contains the real security neuron, processor, and signal classes the trained network will "
          "reference, so no extra hand-written classes are required to train."),

    ("h1", "Step 2 — Run the cybersecurity demo", "5"),
    ("p", "The full-run demo suite launches through the real worker entry path. Run the whole suite (Demo "
          "06 is the cybersecurity scenario):"),
    ("h3", "Windows (PowerShell)"),
    ("code", "powershell -ExecutionPolicy Bypass -File scripts/demo-fullrun/run_all_fullrun_demos.ps1"),
    ("h3", "Linux / macOS (bash)"),
    ("code", "scripts/demo-fullrun/run_all_fullrun_demos.sh"),
    ("p", "The cybersecurity scenario correlates authentication, process, DNS, network-flow, asset, "
          "threat-intelligence, and maintenance context for three entities — an ordered attack chain, a "
          "benign maintenance pattern, and a low-and-slow exfiltration pattern — and writes advisory "
          "results without taking any blocking action."),

    ("h1", "Step 3 — Understand the generated demo outputs", "6"),
    ("p", "Default full-run evidence is written under `target/`:"),
    ("code",
     "target/jneopallium-fullrun-demos/summary.json\n"
     "target/jneopallium-fullrun-demos/demo-06-cybersecurity-kafka-triage/"),
    ("p", "The Demo 06 output is advisory JSONL. Every line carries the posterior probability, impact, "
          "sequence confidence, the threat-intelligence gate, the maintenance gate, and the "
          "baseline-freeze state. The expected, deterministic behaviour is:"),
    ("table", ["Stream", "Expected verdict"],
     [["Ordered attack chain", "`TEMPORAL_THREAT_ADVISORY` (baseline frozen during the attack window)"],
      ["Maintenance-window activity", "`CONTEXT_SUPPRESSED_OBSERVATION` (scored lower, still audited)"],
      ["Low-and-slow outbound", "`LOW_AND_SLOW_CORRELATION`"],
      ["Any stream", "No active blocking result is ever emitted"]],
     [2.4, 4.4]),

    ("h1", "Step 4 — Train the reference model", "7"),
    ("p", "The trainer needs no third-party Python packages. The simplest run uses the bundled, "
          "deterministic multi-source reference corpus:"),
    ("code", "python scripts/demo-cybersecurity-training/train_temporal_model.py"),
    ("p", "It emits the full artifact set into the worker resources (see Step 8 for the complete list and "
          "what each file is). The trainer prints a JSON summary on completion, including the chosen "
          "decision threshold, the held-out test F1, and the estimated corpus size. The run fails "
          "(non-zero exit) if the test F1 falls below the configured `--min-test-f1` gate (default 0.85), "
          "so a broken pipeline cannot silently ship a weak model."),

    ("h1", "Step 5 — Train at production scale", "8"),
    ("p", "This run records a 100 GiB *logical* corpus target by deterministically replicating the "
          "reference campaigns, while keeping the fitted sample bounded so it still completes on a "
          "workstation. It exercises the scaling metadata and guardrails."),
    ("h3", "Windows (PowerShell)"),
    ("code",
     "python scripts\\demo-cybersecurity-training\\train_temporal_model.py `\n"
     "  --reference-multiplier 1000 `\n"
     "  --target-corpus-bytes 100gb `\n"
     "  --max-corpus-bytes 100gb `\n"
     "  --max-train-windows-per-epoch 4096"),
    ("h3", "Linux / macOS (bash)"),
    ("code",
     "python scripts/demo-cybersecurity-training/train_temporal_model.py \\\n"
     "  --reference-multiplier 1000 \\\n"
     "  --target-corpus-bytes 100gb \\\n"
     "  --max-corpus-bytes 100gb \\\n"
     "  --max-train-windows-per-epoch 4096"),
    ("callout", "What the flags mean",
     "`--reference-multiplier` deterministically expands the fitted sample. `--target-corpus-bytes` is the "
     "logical production-scale target recorded in the artifacts. `--max-corpus-bytes` is a hard guardrail: "
     "the run aborts rather than inflate the repository. `--max-train-windows-per-epoch` caps per-epoch "
     "work for very large expanded corpora.", "info"),

    ("h1", "Step 6 — Train on external datasets (manifest mode)", "9"),
    ("p", "For real performance you must train on external canonical data. Manifest mode keeps the same "
          "leakage-safe split policy while reading your own JSONL or CSV sources."),
    ("code",
     "python scripts\\demo-cybersecurity-training\\train_temporal_model.py `\n"
     "  --manifest scripts\\demo-cybersecurity-training\\dataset-manifest-template.json `\n"
     "  --output-dir worker\\src\\main\\resources\\model\\cybersecurity-temporal `\n"
     "  --max-corpus-bytes 100gb"),
    ("h2", "The canonical event contract"),
    ("p", "Every training row must be canonical JSONL or CSV with these fields:"),
    ("code",
     "dataset, entity_id, event_tick, source, event_type, technique,\n"
     "evidence_confidence, threat_intel_confidence, asset_criticality,\n"
     "maintenance_active, malicious, campaign_id, host_group, attack_type, split"),
    ("table", ["Rule", "Why it matters"],
     [["`event_tick` is event time, not ingestion time", "Lets the engine rebuild the true attack sequence"],
      ["`campaign_id` groups one incident or matched benign period", "Keeps related events together for splitting"],
      ["`split` is by time / campaign / host / attack type", "Never randomise rows — that leaks duplicates and inflates scores"],
      ["`maintenance_active` is a soft context feature", "It lowers urgency but must never delete evidence"],
      ["`malicious` is ground truth for training only", "Runtime inference must never depend on it"]],
     [3.2, 3.6]),

    ("h1", "Step 7 — Train on raw LANL + ToN_IoT", "10"),
    ("p", "A dedicated template accepts the raw public datasets directly. Download the files and place them "
          "under the external folders:"),
    ("code",
     "scripts/demo-cybersecurity-training/external/lanl/\n"
     "    auth.txt.gz  proc.txt.gz  dns.txt.gz  flows.txt.gz  redteam.txt.gz\n"
     "\n"
     "scripts/demo-cybersecurity-training/external/toniot/\n"
     "    ... extracted ToN_IoT CSV folders ..."),
    ("bullet", "LANL Comprehensive Multi-Source Cyber-Security Events: `https://csr.lanl.gov/data/cyber1/`"),
    ("bullet", "ToN_IoT datasets: `https://research.unsw.edu.au/projects/toniot-datasets`"),
    ("p", "Then run the LANL + ToN_IoT template (stream the compressed files rather than loading them into "
          "memory):"),
    ("code",
     "python scripts\\demo-cybersecurity-training\\train_temporal_model.py `\n"
     "  --manifest scripts\\demo-cybersecurity-training\\dataset-manifest-lanl-toniot-template.json `\n"
     "  --output-dir worker\\src\\main\\resources\\model\\cybersecurity-temporal `\n"
     "  --max-corpus-bytes 100gb `\n"
     "  --max-train-windows-per-epoch 4096"),
    ("p", "The template also accepts the raw LANL files (`lanl-auth`, `lanl-proc`, `lanl-dns`, `lanl-flow`, "
          "`lanl-redteam`) and common ToN_IoT CSV files (`toniot-network`, `toniot-windows`, "
          "`toniot-linux`). Tune `maxRows`, `startTick`, `endTick`, and `sampleModulo` in the manifest "
          "before training on large extracts. Keep dataset licences and download instructions outside "
          "generated artifacts."),

    ("h1", "Step 8 — The generated artifacts: a deployable network", "11"),
    ("p", "This is the pipeline's hand-off. Every training run writes a directory of JNeopallium-style "
          "configuration that is ready to deploy as-is — not just an abstract model, but the actual "
          "layer-and-neuron network the worker boots from. Ten files are produced:"),
    ("table", ["Artifact", "What it is"],
     [["`trained-temporal-threat-model.json`", "The compact trained model: features, scaler, weights, bias, threshold, metrics"],
      ["`model-descriptor.json`", "The whole-network descriptor: 5 layers, 8 neurons, frequency map, source classes"],
      ["`layer-0.json`", "Multi-source security event input boundary (canonical inputs per source family)"],
      ["`layer-1-fast-evidence.json`", "Fast evidence receptors — 4 neurons (flow, signature, process, baseline)"],
      ["`layer-2-temporal-correlation.json`", "The trained temporal correlator — embeds weights, gates, dendrites"],
      ["`layer-3-response-planning.json`", "Advisory response planning + the fixed hard safety gate"],
      ["`result-layer.json`", "The security-advisory result output layer"],
      ["`trained-model-update.json`", "Latest trained snapshot: status, checksum, weights, training summary"],
      ["`quantitative-summary.json`", "Scale, metrics, and top positive / negative feature weights"],
      ["`source-mapping.json`", "Which typed signals each data source maps onto"]],
     [3.0, 3.8]),
    ("h2", "The network the trainer builds"),
    ("p", "`model-descriptor.json` records a five-layer network of eight real neurons, with 34 trainable "
          "weights and one bias — all referencing concrete runtime classes from the worker's security "
          "package:"),
    ("table", ["Layer", "Role", "Real neuron classes"],
     [["0 — Input", "Multi-source event boundary", "(no neuron objects; canonical inputs only)"],
      ["1 — Fast evidence", "Cheap, instant scoring", "`NetworkFlowNeuron`, `SignaturePatternNeuron`, `ProcessBehaviourNeuron`, `EntityBehaviourBaselineNeuron`"],
      ["2 — Correlation", "Trained temporal correlation", "`TemporalThreatCorrelationNeuron`"],
      ["3 — Planning + gate", "Advisory bands + safety gate", "`ResponsePlanningNeuron`, `ResponseGateNeuron`"],
      ["4 — Result", "Advisory output", "`ResponsePlanningNeuron` (result)"]],
     [1.5, 2.0, 3.3]),
    ("p", "The correlation layer (`layer-2-temporal-correlation.json`) embeds the trained logistic weights "
          "as named **dendrite weights**, the standardisation scaler, the decision threshold (0.2), the "
          "five temporal sequence gates, and a `baselineAdaptation` block that freezes learning when the "
          "posterior reaches **0.3** or signature confidence reaches **0.8**, and only adapts from trusted "
          "benign periods. The descriptor also carries a `signalFrequencyMap` giving each signal its loop "
          "cadence — fast receptors every cycle, hypotheses and quarantine requests every second cycle, "
          "incident reports every fifth — the typed-timescale design made concrete."),
    ("callout", "Why this matters",
     "Because training emits real, inspectable, deployable configuration, there is no translation step "
     "between \"the model we trained\" and \"the network we run.\" The Deployment Guide simply points the "
     "worker at this directory. Every weight, gate, and threshold you see in the report is exactly what "
     "executes in production.", "success"),

    ("h1", "Step 9 — Verify the run against the gates", "12"),
    ("p", "Before treating any run as deployable, confirm the production gates pass:"),
    ("num", "All expected source families are present in `source-mapping.json`."),
    ("num", "Test F1 is at least the configured `--min-test-f1`."),
    ("num", "False-positive rate is within the per-host and per-tenant budget."),
    ("num", "Calibration bins show monotonic risk ordering (higher score → higher real positive rate)."),
    ("num", "Mean time to detection is below your incident-response objective."),
    ("num", "The five layer files and `trained-model-update.json` were written and reference the expected "
            "runtime classes."),
    ("num", "Generated artifacts include checksum, source counts, split policy, metrics, and threshold."),
    ("callout", "Reproducibility check",
     "The reference run is deterministic. Re-running the same command must produce the same training "
     "checksum (`trained-model-update.json`) and the same metrics. A drift in either is a signal that an "
     "input or the toolchain changed.", "info"),

    ("h1", "Troubleshooting", "13"),
    ("table", ["Symptom", "Likely cause and fix"],
     [["`java -version` shows < 17", "Install a JDK 17+ and set `JAVA_HOME` to it"],
      ["Build fails on JDK 17 with reflection warnings", "Ensure Maven plugin floors from the README; use `<release>17</release>`"],
      ["Trainer exits non-zero with \"test F1 below required\"", "The split or data is too weak; check the manifest split policy and `--min-test-f1`"],
      ["\"estimated canonical corpus exceeds --max-corpus-bytes\"", "Lower `--reference-multiplier`/`--target-corpus-bytes` or raise the guardrail deliberately"],
      ["A source is missing in `source-mapping.json`", "Check manifest globs and `requireAllSources`; confirm files were downloaded"],
      ["Layer files not written", "Confirm `--output-dir` is writable; the trainer writes all ten artifacts there"],
      ["Cyrillic / encoding issues in CSV", "Save sources as UTF-8; the reader expects UTF-8 JSONL/CSV"]],
     [3.0, 3.8]),

    ("h1", "Appendix — training cheat-sheet", "14"),
    ("h3", "Build"),
    ("code", "git clone https://github.com/rakovpublic/jneopallium.git && cd jneopallium && mvn clean install"),
    ("h3", "Run the demo suite"),
    ("code",
     "# Windows\n"
     "powershell -ExecutionPolicy Bypass -File scripts/demo-fullrun/run_all_fullrun_demos.ps1\n"
     "# Linux / macOS\n"
     "scripts/demo-fullrun/run_all_fullrun_demos.sh"),
    ("h3", "Train (reference / scaled / external / raw)"),
    ("code",
     "# reference\n"
     "python scripts/demo-cybersecurity-training/train_temporal_model.py\n"
     "\n"
     "# production-scale reference\n"
     "python scripts/demo-cybersecurity-training/train_temporal_model.py \\\n"
     "  --reference-multiplier 1000 --target-corpus-bytes 100gb \\\n"
     "  --max-corpus-bytes 100gb --max-train-windows-per-epoch 4096\n"
     "\n"
     "# external manifest\n"
     "python scripts/demo-cybersecurity-training/train_temporal_model.py \\\n"
     "  --manifest scripts/demo-cybersecurity-training/dataset-manifest-template.json\n"
     "\n"
     "# raw LANL + ToN_IoT\n"
     "python scripts/demo-cybersecurity-training/train_temporal_model.py \\\n"
     "  --manifest scripts/demo-cybersecurity-training/dataset-manifest-lanl-toniot-template.json \\\n"
     "  --max-corpus-bytes 100gb --max-train-windows-per-epoch 4096"),
    ("spacer", 8),
    ("pi", "Jneopallium Cybersecurity Module · Demo 06 · Training Guide. The trainer emits a deployable "
           "JNeopallium network; see the Deployment Guide to run it. Safety mode: ADVISORY. "
           "License: BSD 3-Clause."),
]


_UK = [
    ("cover", "Jneopallium · Демо 06",
     "Посібник з навчання",
     "Часова кореляція кіберзагроз — зберіть, навчіть і отримайте готову до розгортання мережу",
     [("Документ", "Посібник з навчання моделі"),
      ("Продукт", "Модуль кібербезпеки Jneopallium (Демо 06)"),
      ("Модель", "cybersecurity-temporal-threat-correlator 1.0.0"),
      ("Тренер", "scripts/demo-cybersecurity-training/train_temporal_model.py"),
      ("Вихід", "Готова до розгортання конфігурація JNeopallium"),
      ("Автор", "Дмитро Раковський — Харків, Україна"),
      ("Дата", DATE_UK),
      ("Ліцензія", "BSD 3-Clause")],
     "Посібник з навчання",
     "Повний покроковий маршрут для копіювання навчальної половини конвеєра: встановіть інструментарій, "
     "зберіть платформу, запустіть демо, навчіть модель на еталонних і реальних даних і отримайте готову "
     "до розгортання мережу, яку споживає Посібник з розгортання."),

    ("toc", "Зміст",
     ["Чого ви досягнете",
      "Де закінчується навчання і починається розгортання",
      "Передумови та системні вимоги",
      "Крок 1 — Отримати код і зібрати",
      "Крок 2 — Запустити демо кібербезпеки",
      "Крок 3 — Зрозуміти згенеровані результати демо",
      "Крок 4 — Навчити еталонну модель",
      "Крок 5 — Навчання у промисловому масштабі",
      "Крок 6 — Навчання на зовнішніх даних (режим маніфесту)",
      "Крок 7 — Навчання на сирих LANL + ToN_IoT",
      "Крок 8 — Згенеровані артефакти: готова до розгортання мережа",
      "Крок 9 — Перевірити запуск за контрольними критеріями",
      "Усунення несправностей",
      "Додаток — шпаргалка навчання"]),

    ("h1", "Чого ви досягнете", "1"),
    ("p", "До кінця цього посібника ви матимете: зібрану платформу Jneopallium; робоче демо кібербезпеки, "
          "що видає рекомендаційний JSONL; свіжонавчену модель часової кореляції загроз; і — ключовий "
          "результат — **повну, готову до розгортання мережу JNeopallium** (файли конфігурації рівнів і "
          "нейронів зі справжніми класами часу виконання, вбудованими навченими вагами та фіксованим "
          "запобіжником), яку напряму завантажує супровідний **Посібник з розгортання**."),
    ("callout", "Стеля безпеки для всього конвеєра",
     "Усе, що робить навчена модель, працює в режимі ADVISORY. Вона може рекомендувати кандидатів на "
     "розслідування чи карантин, але не повинна ізолювати вузли чи блокувати трафік. Тренер кодує це прямо: "
     "рівень відповіді позначено ADVISORY, а жорсткий запобіжник — це фіксована конфігурація, ніколи не "
     "навчена.", "warning"),

    ("h1", "Де закінчується навчання і починається розгортання", "2"),
    ("p", "Конвеєр має дві чисті половини, і цей посібник охоплює першу:"),
    ("table", ["Фаза", "Що відбувається", "Документ"],
     [["**Навчання** (цей посібник)", "Зборка, запуск демо, навчання на даних, **видача мережі**",
       "Посібник з навчання"],
      ["**Розгортання**", "Завантаження виданої мережі, налаштування джерел подій, запуск worker",
       "Посібник з розгортання"]],
     [2.0, 3.2, 1.6]),
    ("p", "Передача між двома половинами — це єдина тека згенерованих артефактів (Крок 8). Навчання її "
          "**створює**; розгортання її **споживає**. Між ними нічого не пишеться вручну — тренер записує "
          "готову для JNeopallium конфігурацію рівнів і нейронів, тож ті самі файли, що фіксують ваш запуск "
          "навчання, є файлами, з яких завантажується worker."),

    ("h1", "Передумови та системні вимоги", "3"),
    ("h2", "Інструментарій"),
    ("table", ["Інструмент", "Версія", "Навіщо"],
     [["Java JDK", "17 або новіша (LTS)", "Платформа орієнтована на базовий API Java 17"],
      ["Apache Maven", "3.9 або новіша", "Багатомодульна збірка master + worker"],
      ["Python", "3.10+ (тестовано 3.13)", "Запускає тренер моделі; сторонні пакети не потрібні"],
      ["Git", "будь-яка свіжа", "Клонування репозиторію"]],
     [1.8, 2.0, 3.0]),
    ("h2", "Апаратне забезпечення"),
    ("bullet", "**Демо / пілот:** будь-який сучасний ноутбук. Еталонний тренер зручно працює в одному JVM, "
               "а набір даних настільного масштабу займає кілька десятків мегабайтів."),
    ("bullet", "**Еталонний запуск промислового масштабу:** тренер фіксує *логічну* ціль корпусу 100 ГіБ "
               "без запису 100 ГіБ на диск, тож усе одно працює на робочій станції. Реальне навчання на "
               "зовнішніх даних масштабується відповідно до обсягу, який ви подаєте."),
    ("h2", "Перевірте інструментарій"),
    ("code",
     "java -version       # очікуємо 17 або новіше\n"
     "mvn -version        # очікуємо 3.9 або новіше\n"
     "python --version    # очікуємо 3.10 або новіше\n"
     "git --version"),

    ("h1", "Крок 1 — Отримати код і зібрати", "4"),
    ("p", "Клонуйте репозиторій і зберіть обидва модулі. Збірка встановлює артефакти `master` і `worker` у "
          "ваш локальний репозиторій Maven."),
    ("code",
     "git clone https://github.com/rakovpublic/jneopallium.git\n"
     "cd jneopallium\n"
     "mvn clean install"),
    ("p", "Успішна збірка компілює платформу, виконує набір тестів (зокрема `SecurityModuleTest`) і "
          "встановлює `com.rakovpublic.jneopallium:worker:1.0`. Worker уже містить справжні класи нейронів, "
          "процесорів і сигналів безпеки, на які посилатиметься навчена мережа, тож для навчання не "
          "потрібні додаткові написані вручну класи."),

    ("h1", "Крок 2 — Запустити демо кібербезпеки", "5"),
    ("p", "Набір повних демо запускається через справжній вхідний шлях worker. Запустіть увесь набір "
          "(Демо 06 — це сценарій кібербезпеки):"),
    ("h3", "Windows (PowerShell)"),
    ("code", "powershell -ExecutionPolicy Bypass -File scripts/demo-fullrun/run_all_fullrun_demos.ps1"),
    ("h3", "Linux / macOS (bash)"),
    ("code", "scripts/demo-fullrun/run_all_fullrun_demos.sh"),
    ("p", "Сценарій кібербезпеки корелює контекст автентифікації, процесів, DNS, мережевих потоків, "
          "активів, кіберрозвідки та обслуговування для трьох сутностей — впорядкований ланцюг атаки, "
          "доброякісний шаблон обслуговування та шаблон повільного витоку — і записує рекомендаційні "
          "результати без жодної блокувальної дії."),

    ("h1", "Крок 3 — Зрозуміти згенеровані результати демо", "6"),
    ("p", "Типові докази повного запуску записуються в `target/`:"),
    ("code",
     "target/jneopallium-fullrun-demos/summary.json\n"
     "target/jneopallium-fullrun-demos/demo-06-cybersecurity-kafka-triage/"),
    ("p", "Вихід Демо 06 — це рекомендаційний JSONL. Кожен рядок несе апостеріорну ймовірність, вплив, "
          "впевненість послідовності, запобіжник кіберрозвідки, запобіжник обслуговування та стан "
          "заморожування базової лінії. Очікувана детермінована поведінка така:"),
    ("table", ["Потік", "Очікуваний вердикт"],
     [["Впорядкований ланцюг атаки", "`TEMPORAL_THREAT_ADVISORY` (базова лінія заморожена під час атаки)"],
      ["Активність у вікні обслуговування", "`CONTEXT_SUPPRESSED_OBSERVATION` (нижча оцінка, але в аудиті)"],
      ["Повільний вихідний трафік", "`LOW_AND_SLOW_CORRELATION`"],
      ["Будь-який потік", "Жодного активного блокувального результату не видається"]],
     [2.4, 4.4]),

    ("h1", "Крок 4 — Навчити еталонну модель", "7"),
    ("p", "Тренер не потребує сторонніх пакетів Python. Найпростіший запуск використовує вбудований "
          "детермінований багатоджерельний еталонний корпус:"),
    ("code", "python scripts/demo-cybersecurity-training/train_temporal_model.py"),
    ("p", "Він видає повний набір артефактів у ресурси worker (повний перелік і призначення кожного файлу — "
          "у Кроці 8). Після завершення тренер друкує зведення JSON: вибраний поріг рішення, тестовий F1 на "
          "відкладеній вибірці й оцінений розмір корпусу. Запуск завершується помилкою (ненульовий код), "
          "якщо тестовий F1 нижчий за поріг `--min-test-f1` (за замовчуванням 0.85), тож зламаний конвеєр не "
          "може тихо відвантажити слабку модель."),

    ("h1", "Крок 5 — Навчання у промисловому масштабі", "8"),
    ("p", "Цей запуск фіксує *логічну* ціль корпусу 100 ГіБ, детерміновано реплікуючи еталонні кампанії, "
          "водночас тримаючи припасовану вибірку обмеженою, тож усе одно завершується на робочій станції. "
          "Він вправляє метадані масштабування та запобіжники."),
    ("h3", "Windows (PowerShell)"),
    ("code",
     "python scripts\\demo-cybersecurity-training\\train_temporal_model.py `\n"
     "  --reference-multiplier 1000 `\n"
     "  --target-corpus-bytes 100gb `\n"
     "  --max-corpus-bytes 100gb `\n"
     "  --max-train-windows-per-epoch 4096"),
    ("h3", "Linux / macOS (bash)"),
    ("code",
     "python scripts/demo-cybersecurity-training/train_temporal_model.py \\\n"
     "  --reference-multiplier 1000 \\\n"
     "  --target-corpus-bytes 100gb \\\n"
     "  --max-corpus-bytes 100gb \\\n"
     "  --max-train-windows-per-epoch 4096"),
    ("callout", "Що означають прапорці",
     "`--reference-multiplier` детерміновано розширює припасовану вибірку. `--target-corpus-bytes` — це "
     "логічна ціль промислового масштабу, записана в артефакти. `--max-corpus-bytes` — жорсткий запобіжник: "
     "запуск переривається, замість роздувати репозиторій. `--max-train-windows-per-epoch` обмежує роботу "
     "за епоху для дуже великих розширених корпусів.", "info"),

    ("h1", "Крок 6 — Навчання на зовнішніх даних (режим маніфесту)", "9"),
    ("p", "Для реальної якості потрібно навчати на зовнішніх канонічних даних. Режим маніфесту зберігає ту "
          "саму політику розбиття без витоку, читаючи ваші власні джерела JSONL або CSV."),
    ("code",
     "python scripts\\demo-cybersecurity-training\\train_temporal_model.py `\n"
     "  --manifest scripts\\demo-cybersecurity-training\\dataset-manifest-template.json `\n"
     "  --output-dir worker\\src\\main\\resources\\model\\cybersecurity-temporal `\n"
     "  --max-corpus-bytes 100gb"),
    ("h2", "Канонічний контракт події"),
    ("p", "Кожен рядок навчання має бути канонічним JSONL або CSV з такими полями:"),
    ("code",
     "dataset, entity_id, event_tick, source, event_type, technique,\n"
     "evidence_confidence, threat_intel_confidence, asset_criticality,\n"
     "maintenance_active, malicious, campaign_id, host_group, attack_type, split"),
    ("table", ["Правило", "Чому це важливо"],
     [["`event_tick` — час події, а не час прийому", "Дає рушію відтворити справжню послідовність атаки"],
      ["`campaign_id` групує один інцидент чи зіставлений доброякісний період", "Тримає пов'язані події разом для розбиття"],
      ["`split` — за часом / кампанією / вузлом / типом атаки", "Ніколи не рандомізуйте рядки — це просочує дублікати й завищує оцінки"],
      ["`maintenance_active` — м'яка контекстна ознака", "Знижує терміновість, але ніколи не видаляє докази"],
      ["`malicious` — істинна мітка лише для навчання", "Висновок під час роботи ніколи не повинен від неї залежати"]],
     [3.2, 3.6]),

    ("h1", "Крок 7 — Навчання на сирих LANL + ToN_IoT", "10"),
    ("p", "Окремий шаблон приймає сирі публічні набори даних безпосередньо. Завантажте файли й покладіть їх "
          "у зовнішні теки:"),
    ("code",
     "scripts/demo-cybersecurity-training/external/lanl/\n"
     "    auth.txt.gz  proc.txt.gz  dns.txt.gz  flows.txt.gz  redteam.txt.gz\n"
     "\n"
     "scripts/demo-cybersecurity-training/external/toniot/\n"
     "    ... видобуті теки CSV ToN_IoT ..."),
    ("bullet", "LANL Comprehensive Multi-Source Cyber-Security Events: `https://csr.lanl.gov/data/cyber1/`"),
    ("bullet", "Набори даних ToN_IoT: `https://research.unsw.edu.au/projects/toniot-datasets`"),
    ("p", "Потім запустіть шаблон LANL + ToN_IoT (потокова обробка стиснених файлів замість завантаження в "
          "пам'ять):"),
    ("code",
     "python scripts\\demo-cybersecurity-training\\train_temporal_model.py `\n"
     "  --manifest scripts\\demo-cybersecurity-training\\dataset-manifest-lanl-toniot-template.json `\n"
     "  --output-dir worker\\src\\main\\resources\\model\\cybersecurity-temporal `\n"
     "  --max-corpus-bytes 100gb `\n"
     "  --max-train-windows-per-epoch 4096"),
    ("p", "Шаблон також приймає сирі файли LANL (`lanl-auth`, `lanl-proc`, `lanl-dns`, `lanl-flow`, "
          "`lanl-redteam`) і поширені файли CSV ToN_IoT (`toniot-network`, `toniot-windows`, "
          "`toniot-linux`). Налаштуйте `maxRows`, `startTick`, `endTick` і `sampleModulo` у маніфесті перед "
          "навчанням на великих витягах. Тримайте ліцензії наборів даних та інструкції зі завантаження поза "
          "згенерованими артефактами."),

    ("h1", "Крок 8 — Згенеровані артефакти: готова до розгортання мережа", "11"),
    ("p", "Це передача конвеєра. Кожен запуск навчання записує теку конфігурації у стилі JNeopallium, "
          "готову до розгортання як є, — не просто абстрактну модель, а власне мережу рівнів і нейронів, з "
          "якої завантажується worker. Створюється десять файлів:"),
    ("table", ["Артефакт", "Що це"],
     [["`trained-temporal-threat-model.json`", "Компактна навчена модель: ознаки, скейлер, ваги, зсув, поріг, метрики"],
      ["`model-descriptor.json`", "Опис усієї мережі: 5 рівнів, 8 нейронів, мапа частот, класи джерел"],
      ["`layer-0.json`", "Межа багатоджерельного входу подій безпеки (канонічні входи на родину джерел)"],
      ["`layer-1-fast-evidence.json`", "Швидкі рецептори доказів — 4 нейрони (потік, сигнатура, процес, базова лінія)"],
      ["`layer-2-temporal-correlation.json`", "Навчений часовий корелятор — вбудовує ваги, гейти, дендрити"],
      ["`layer-3-response-planning.json`", "Планування рекомендаційної відповіді + фіксований жорсткий запобіжник"],
      ["`result-layer.json`", "Рівень виводу рекомендацій безпеки"],
      ["`trained-model-update.json`", "Останній знімок навчання: статус, контрольна сума, ваги, зведення"],
      ["`quantitative-summary.json`", "Масштаб, метрики та найвагоміші позитивні / негативні ваги ознак"],
      ["`source-mapping.json`", "На які типізовані сигнали відображається кожне джерело даних"]],
     [3.0, 3.8]),
    ("h2", "Мережа, яку будує тренер"),
    ("p", "`model-descriptor.json` фіксує п'ятирівневу мережу з восьми справжніх нейронів, з 34 навчуваними "
          "вагами та одним зсувом — усі посилаються на конкретні класи часу виконання з пакета безпеки "
          "worker:"),
    ("table", ["Рівень", "Роль", "Справжні класи нейронів"],
     [["0 — Вхід", "Межа багатоджерельних подій", "(без об'єктів нейронів; лише канонічні входи)"],
      ["1 — Швидкі докази", "Дешева миттєва оцінка", "`NetworkFlowNeuron`, `SignaturePatternNeuron`, `ProcessBehaviourNeuron`, `EntityBehaviourBaselineNeuron`"],
      ["2 — Кореляція", "Навчена часова кореляція", "`TemporalThreatCorrelationNeuron`"],
      ["3 — Планування + запобіжник", "Смуги рекомендацій + запобіжник", "`ResponsePlanningNeuron`, `ResponseGateNeuron`"],
      ["4 — Результат", "Рекомендаційний вивід", "`ResponsePlanningNeuron` (результат)"]],
     [1.6, 2.0, 3.2]),
    ("p", "Рівень кореляції (`layer-2-temporal-correlation.json`) вбудовує навчені логістичні ваги як "
          "іменовані **ваги дендритів**, скейлер стандартизації, поріг рішення (0.2), п'ять часових гейтів "
          "послідовності та блок `baselineAdaptation`, що заморожує навчання, коли апостеріорна сягає "
          "**0.3** або впевненість сигнатури сягає **0.8**, і адаптується лише з довірених доброякісних "
          "періодів. Опис також містить `signalFrequencyMap`, що дає кожному сигналу його каданс циклу — "
          "швидкі рецептори щоцикл, гіпотези й запити карантину кожен другий цикл, звіти про інциденти "
          "кожен п'ятий — дизайн типізованих часових масштабів, втілений конкретно."),
    ("callout", "Чому це важливо",
     "Оскільки навчання видає справжню, придатну до перевірки, готову до розгортання конфігурацію, між «тим, "
     "що ми навчили» і «тим, що ми запускаємо», немає кроку перекладу. Посібник з розгортання просто "
     "спрямовує worker на цю теку. Кожна вага, гейт і поріг, які ви бачите у звіті, — це саме те, що "
     "виконується в промислі.", "success"),

    ("h1", "Крок 9 — Перевірити запуск за контрольними критеріями", "12"),
    ("p", "Перш ніж вважати будь-який запуск придатним до розгортання, переконайтеся, що промислові критерії "
          "пройдено:"),
    ("num", "Усі очікувані родини джерел присутні в `source-mapping.json`."),
    ("num", "Тестовий F1 не нижчий за поріг `--min-test-f1`."),
    ("num", "Рівень хибних спрацювань у межах бюджету на вузол і на орендаря."),
    ("num", "Кошики калібрування показують монотонне впорядкування ризику (вища оцінка → вищий реальний "
            "рівень позитивів)."),
    ("num", "Середній час до виявлення нижчий за вашу ціль реагування на інциденти."),
    ("num", "П'ять файлів рівнів і `trained-model-update.json` записано, і вони посилаються на очікувані "
            "класи часу виконання."),
    ("num", "Згенеровані артефакти містять контрольну суму, кількості джерел, політику розбиття, метрики "
            "та поріг."),
    ("callout", "Перевірка відтворюваності",
     "Еталонний запуск детермінований. Повторний запуск тієї самої команди має дати ту саму контрольну "
     "суму навчання (`trained-model-update.json`) і ті самі метрики. Розбіжність у будь-чому з них — сигнал, "
     "що змінилися вхідні дані чи інструментарій.", "info"),

    ("h1", "Усунення несправностей", "13"),
    ("table", ["Симптом", "Імовірна причина й виправлення"],
     [["`java -version` показує < 17", "Встановіть JDK 17+ і вкажіть на нього `JAVA_HOME`"],
      ["Збірка падає на JDK 17 з попередженнями рефлексії", "Дотримайте мінімумів плагінів Maven з README; вживайте `<release>17</release>`"],
      ["Тренер виходить з помилкою «test F1 below required»", "Розбиття чи дані заслабкі; перевірте політику розбиття маніфесту та `--min-test-f1`"],
      ["«estimated canonical corpus exceeds --max-corpus-bytes»", "Знизьте `--reference-multiplier`/`--target-corpus-bytes` або свідомо підніміть запобіжник"],
      ["Джерело відсутнє в `source-mapping.json`", "Перевірте шаблони маніфесту та `requireAllSources`; підтвердьте, що файли завантажено"],
      ["Файли рівнів не записано", "Переконайтеся, що `--output-dir` доступний для запису; тренер пише туди всі десять артефактів"],
      ["Проблеми з кирилицею / кодуванням у CSV", "Зберігайте джерела як UTF-8; читач очікує UTF-8 JSONL/CSV"]],
     [3.0, 3.8]),

    ("h1", "Додаток — шпаргалка навчання", "14"),
    ("h3", "Збірка"),
    ("code", "git clone https://github.com/rakovpublic/jneopallium.git && cd jneopallium && mvn clean install"),
    ("h3", "Запуск набору демо"),
    ("code",
     "# Windows\n"
     "powershell -ExecutionPolicy Bypass -File scripts/demo-fullrun/run_all_fullrun_demos.ps1\n"
     "# Linux / macOS\n"
     "scripts/demo-fullrun/run_all_fullrun_demos.sh"),
    ("h3", "Навчання (еталон / масштаб / зовнішнє / сире)"),
    ("code",
     "# еталон\n"
     "python scripts/demo-cybersecurity-training/train_temporal_model.py\n"
     "\n"
     "# еталон промислового масштабу\n"
     "python scripts/demo-cybersecurity-training/train_temporal_model.py \\\n"
     "  --reference-multiplier 1000 --target-corpus-bytes 100gb \\\n"
     "  --max-corpus-bytes 100gb --max-train-windows-per-epoch 4096\n"
     "\n"
     "# зовнішній маніфест\n"
     "python scripts/demo-cybersecurity-training/train_temporal_model.py \\\n"
     "  --manifest scripts/demo-cybersecurity-training/dataset-manifest-template.json\n"
     "\n"
     "# сирі LANL + ToN_IoT\n"
     "python scripts/demo-cybersecurity-training/train_temporal_model.py \\\n"
     "  --manifest scripts/demo-cybersecurity-training/dataset-manifest-lanl-toniot-template.json \\\n"
     "  --max-corpus-bytes 100gb --max-train-windows-per-epoch 4096"),
    ("spacer", 8),
    ("pi", "Модуль кібербезпеки Jneopallium · Демо 06 · Посібник з навчання. Тренер видає готову до "
           "розгортання мережу JNeopallium; як її запустити — у Посібнику з розгортання. "
           "Режим безпеки: ADVISORY. Ліцензія: BSD 3-Clause."),
]
