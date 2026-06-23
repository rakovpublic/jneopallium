# -*- coding: utf-8 -*-
"""Ad-Fraud Guardian — training guide (EN + UK), block DSL."""

from __future__ import annotations

DATE = "23 June 2026"
DATE_UK = "23 червня 2026"


def training(lang: str) -> list:
    return _EN if lang == "en" else _UK


_EN = [
    ("cover", "Jneopallium · Ad-Fraud Guardian",
     "Training Guide",
     "Build, run the deterministic workflow, train, and produce a deployable invalid-traffic network",
     [("Document", "Model Training Guide"),
      ("Product", "Jneopallium Ad-Fraud Guardian (advertising-fraud module)"),
      ("Model", "advertising-fraud 1.0.0 · macro-F1 ≈ 0.97"),
      ("Trainer", "scripts/demo-ad-fraud/run_all.py"),
      ("Output", "Deployable JNeopallium layer/neuron config"),
      ("Author", "Dmytro Rakovskyi — Kharkiv, Ukraine"),
      ("Date", DATE),
      ("License", "BSD 3-Clause")],
     "Training Guide",
     "A complete, copy-paste walkthrough of the training half of the pipeline: install the toolchain, "
     "build the platform, run the deterministic ad-fraud workflow, bring your own data, and produce the "
     "ready-to-deploy multi-label network the Deployment Guide consumes."),

    ("toc", "Contents",
     ["What you will achieve",
      "Where training ends and deployment begins",
      "Prerequisites and system requirements",
      "Step 1 — Get the source and build",
      "Step 2 — Run the deterministic workflow",
      "Step 3 — Understand the generated outputs",
      "Step 4 — The workflow stages, explained",
      "Step 5 — Bring your own data (the canonical event)",
      "Step 6 — The generated artifacts: a deployable network",
      "Step 7 — Verify the run against the gates",
      "Troubleshooting",
      "Appendix — training cheat-sheet"]),

    ("h1", "What you will achieve", "1"),
    ("p", "By the end of this guide you will have: a built Jneopallium platform; a fully reproducible "
          "ad-fraud workflow run; a freshly trained, calibrated **multi-label invalid-traffic model**; and "
          "— the key output — a **complete, deployable JNeopallium network** (eight layer files, real "
          "runtime classes, embedded calibrated heads, and an advisory response gate) that the companion "
          "**Deployment Guide** loads directly."),
    ("callout", "Safety ceiling for the whole pipeline",
     "Everything the trained model does is advisory. It produces candidate actions and evidence; it never "
     "blocks a publisher, withholds a payout, or accuses a person. The readiness report keeps "
     "AUTOMATED_ACTION_READY false until first-party labelled production traffic, legal review, and an "
     "appeal/rollback process are recorded.", "warning"),

    ("h1", "Where training ends and deployment begins", "2"),
    ("table", ["Phase", "What happens", "Document"],
     [["**Training** (this guide)", "Build, run the workflow, train and calibrate, **emit the network**",
       "Training Guide"],
      ["**Deployment**", "Load the emitted network, configure event sources, run the advisory scorer",
       "Deployment Guide"]],
     [1.9, 3.3, 1.6]),
    ("p", "The hand-off is a single directory of generated artifacts (Step 6). Training **produces** it; "
          "deployment **consumes** it. The trainer writes JNeopallium-ready layer and neuron configuration "
          "with the trained heads, calibration, and thresholds inside, so the files that record your run "
          "are the files the runtime boots from."),

    ("h1", "Prerequisites and system requirements", "3"),
    ("h2", "Toolchain"),
    ("table", ["Tool", "Version", "Why"],
     [["Java JDK", "17 or newer (LTS)", "Build and run the worker / runtime scorer"],
      ["Apache Maven", "3.9 or newer", "Multi-module build of master + worker"],
      ["Python", "3.10+ (3.13 tested)", "Runs the workflow; no third-party packages required"],
      ["Git", "any recent", "Clone the repository"]],
     [1.8, 2.0, 3.0]),
    ("p", "The workflow runs fully **offline and deterministically** — it does not ask for manual files. "
          "Optional public sources that are unavailable, license-blocked, or credential-gated are simply "
          "recorded as such; the deterministic simulator supplies coverage for the fraud classes that lack "
          "complete public labels."),
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
          "real ad-fraud neuron, processor, and signal classes the trained network references "
          "(`com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud.*` and friends), so no extra "
          "hand-written classes are required to train."),

    ("h1", "Step 2 — Run the deterministic workflow", "5"),
    ("p", "One command performs the entire workflow: source discovery, deterministic scenario generation, "
          "normalization, leakage audit, training, calibration, evaluation, model export, Java tests, demo "
          "replay, and a readiness report."),
    ("h3", "Linux / macOS"),
    ("code", "scripts/demo-ad-fraud/run_all.sh"),
    ("h3", "Windows (PowerShell)"),
    ("code", "powershell -ExecutionPolicy Bypass -File scripts/demo-ad-fraud/run_all.ps1"),
    ("h3", "Common flags"),
    ("table", ["Flag", "Effect"],
     [["`--offline`", "Never attempt network downloads; record sources as unavailable"],
      ["`--quick`", "Smaller, faster corpus (still ≥60 events per scenario)"],
      ["`--max-rows N`", "Corpus size (default 1900 ≈ 100 events per scenario)"],
      ["`--seed N`", "Deterministic seed (default 1729)"],
      ["`--skip-java`", "Skip the Maven Java tests (useful while iterating)"]],
     [1.8, 5.0]),
    ("p", "Example: `python scripts/demo-ad-fraud/run_all.py --offline`. The run prints a JSON summary "
          "with the macro-F1, the readiness flags, and the Java-test status."),

    ("h1", "Step 3 — Understand the generated outputs", "6"),
    ("p", "Workflow evidence is written under `target/jneopallium-ad-fraud/`, and the deployable model "
          "bundle is written to `worker/src/main/resources/model/advertising-fraud/`. The summary looks "
          "like:"),
    ("code",
     '{\n'
     '  "macroF1": 0.966132,\n'
     '  "readiness": {\n'
     '    "ENGINEERING_READY": true, "SHADOW_READY": true,\n'
     '    "ADVISORY_READY": true, "AUTOMATED_ACTION_READY": false\n'
     '  },\n'
     '  "javaTests": "passed"\n'
     '}'),
    ("table", ["Readiness flag", "Meaning"],
     [["`ENGINEERING_READY`", "The pipeline is wired, reproducible, and exports a valid bundle"],
      ["`SHADOW_READY`", "Safe to run read-only beside production, writing advisories to an audit sink"],
      ["`ADVISORY_READY`", "Safe to surface advisories to analysts"],
      ["`AUTOMATED_ACTION_READY`", "**Always false** until first-party labels + legal review + appeal/rollback"]],
     [2.4, 4.4]),

    ("h1", "Step 4 — The workflow stages, explained", "7"),
    ("num", "**Source discovery & bounded download.** Public sources (e.g. Criteo attribution / click "
            "logs, ads.txt / sellers.json crawl, benign crawler identifiers) are catalogued; unavailable "
            "or credential-gated ones are recorded, not requested."),
    ("num", "**Deterministic scenario generation.** A seeded simulator produces realistic events across "
            "~19 scenarios (bots, click farms, incentivized cohorts, click spam/injection, postback "
            "forgery, inventory spoofing, plus legitimate and accidental traffic)."),
    ("num", "**Normalization & leakage audit.** Events become canonical rows; the audit fails the run if "
            "any device / click / campaign identifier crosses split boundaries."),
    ("num", "**Training & calibration.** Class-weighted, L2-regularized logistic heads are fitted over "
            "base + behavioural-evidence + non-linear interaction features, then each head is Platt-"
            "calibrated on a held-out calibration split."),
    ("num", "**Cost-aware thresholds & evaluation.** Per-label operating points are chosen by expected "
            "utility; metrics include precision/recall/F1, PR/ROC-AUC, Brier, calibration error, and "
            "cost-weighted savings."),
    ("num", "**Export, tests, readiness, docs.** The deployable network is written, Java tests run, and a "
            "readiness report and model/data cards are produced."),

    ("h1", "Step 5 — Bring your own data (the canonical event)", "8"),
    ("p", "To move beyond the reference corpus you supply your own canonical events. Each event keeps both "
          "event and ingest time and carries integrity, behavioural, supply-chain, and delayed-quality "
          "fields, plus label-provenance metadata. Raw personal identifiers must be HMAC-hashed before "
          "training, persistence, or logs."),
    ("callout", "The split policy is the integrity guarantee",
     "Never split by random row. Split by scenario, campaign episode, and deterministic replicate block, "
     "and keep an adversarial holdout — so reported quality reflects genuinely unseen traffic. The leakage "
     "audit enforces this automatically.", "info"),
    ("p", "Label provenance matters: the reference model uses deterministic simulator labels and weak "
          "public-source metadata. Real production accuracy requires **first-party labelled traffic** — "
          "confirmed chargebacks, MMP fraud rulings, manual analyst verdicts — and forward-time and "
          "unseen-publisher validation."),

    ("h1", "Step 6 — The generated artifacts: a deployable network", "9"),
    ("p", "Every run writes a directory of JNeopallium-style configuration that is ready to deploy as-is — "
          "the actual layer-and-neuron network the runtime boots from:"),
    ("table", ["Artifact", "What it is"],
     [["`model-descriptor.json`", "Whole-network descriptor: 8 layers, 22 neurons, frequency map, runtime classes"],
      ["`fallback-model.json`", "The trained calibrated heads + the non-linear hidden layer (featureNames, hidden, heads)"],
      ["`layer-0.json` … `layer-6-…`, `result-layer.json`", "The eight deployable layer files"],
      ["`thresholds.json` / `calibration.json`", "Cost-aware per-label thresholds and Platt calibration"],
      ["`feature-schema.json` / `label-schema.json`", "The input features and the ten labels"],
      ["`training-manifest.json`", "Sources, example count, split policy, full metrics"],
      ["`MODEL_CARD.md` / `DATA_CARD.md`", "Safety cards (advisory-only; source statuses)"],
      ["`checksums.sha256`", "Integrity manifest the Java bundle verifies on load"]],
     [3.0, 3.8]),
    ("p", "`model-descriptor.json` records the **8-layer, 22-neuron** network — input, event-integrity, "
          "entity/graph/quality, behavioural-evidence, feature-interaction (the non-linear hidden layer), "
          "trained correlation, advisory response gate, and result — every neuron referencing a concrete "
          "class from the worker's ad-fraud package."),
    ("callout", "Why this matters",
     "Because training emits real, inspectable, deployable configuration with the calibrated heads and "
     "thresholds inside, there is no translation step between \"the model we trained\" and \"the network "
     "we run.\" The Deployment Guide simply points the runtime at this directory.", "success"),

    ("h1", "Step 7 — Verify the run against the gates", "10"),
    ("num", "The leakage audit passed (no identifier crosses split boundaries)."),
    ("num", "Macro-F1 meets your bar (the reference run reaches ≈ 0.97; the per-label table is in the "
            "descriptor and the Test Report)."),
    ("num", "Calibration is real Platt scaling per label (not identity), and calibration error is within "
            "budget."),
    ("num", "The eight layer files, the descriptor, and `checksums.sha256` were written and reference the "
            "expected runtime classes."),
    ("num", "The Java tests pass (`javaTests: passed`) and the readiness report keeps "
            "`AUTOMATED_ACTION_READY=false`."),
    ("callout", "Reproducibility check",
     "The run is deterministic for a given seed. Re-running the same command must reproduce the same "
     "macro-F1 and the same per-label metrics. A drift signals that an input, the seed, or the toolchain "
     "changed.", "info"),

    ("h1", "Troubleshooting", "11"),
    ("table", ["Symptom", "Likely cause and fix"],
     [["`java -version` shows < 17", "Install a JDK 17+ and set `JAVA_HOME`"],
      ["Workflow exits at the leakage audit", "An identifier crosses splits; check the split policy in your data"],
      ["Java tests fail after a change", "Structural counts changed; re-export the bundle and align the test expectations"],
      ["Sources show `dataset_unavailable`", "Expected offline; the deterministic simulator still provides coverage"],
      ["Macro-F1 lower than expected on your data", "Add first-party labels and more per-scenario variety; check calibration"],
      ["Bundle checksum mismatch in Java", "Re-run the workflow so `checksums.sha256` matches the exported files"]],
     [3.1, 3.7]),

    ("h1", "Appendix — training cheat-sheet", "12"),
    ("h3", "Build"),
    ("code", "git clone https://github.com/rakovpublic/jneopallium.git && cd jneopallium && mvn clean install"),
    ("h3", "Run the workflow"),
    ("code",
     "# full deterministic run (offline)\n"
     "python scripts/demo-ad-fraud/run_all.py --offline\n"
     "\n"
     "# quick iteration, skip Java tests\n"
     "python scripts/demo-ad-fraud/run_all.py --quick --offline --skip-java\n"
     "\n"
     "# Linux/macOS and Windows wrappers\n"
     "scripts/demo-ad-fraud/run_all.sh\n"
     "powershell -File scripts/demo-ad-fraud/run_all.ps1"),
    ("spacer", 8),
    ("pi", "Jneopallium Ad-Fraud Guardian · Training Guide. The trainer emits a deployable JNeopallium "
           "network; see the Deployment Guide to run it. Safety mode: ADVISORY / SHADOW. "
           "License: BSD 3-Clause."),
]


_UK = [
    ("cover", "Jneopallium · Ad-Fraud Guardian",
     "Посібник з навчання",
     "Зберіть, запустіть детермінований процес, навчіть і отримайте готову мережу виявлення невалідного трафіку",
     [("Документ", "Посібник з навчання моделі"),
      ("Продукт", "Jneopallium Ad-Fraud Guardian (модуль advertising-fraud)"),
      ("Модель", "advertising-fraud 1.0.0 · macro-F1 ≈ 0.97"),
      ("Тренер", "scripts/demo-ad-fraud/run_all.py"),
      ("Вихід", "Готова до розгортання конфігурація JNeopallium"),
      ("Автор", "Дмитро Раковський — Харків, Україна"),
      ("Дата", DATE_UK),
      ("Ліцензія", "BSD 3-Clause")],
     "Посібник з навчання",
     "Повний покроковий маршрут для копіювання навчальної половини конвеєра: встановіть інструментарій, "
     "зберіть платформу, запустіть детермінований процес ad-fraud, додайте власні дані й отримайте готову "
     "до розгортання багатоміткову мережу, яку споживає Посібник з розгортання."),

    ("toc", "Зміст",
     ["Чого ви досягнете",
      "Де закінчується навчання і починається розгортання",
      "Передумови та системні вимоги",
      "Крок 1 — Отримати код і зібрати",
      "Крок 2 — Запустити детермінований процес",
      "Крок 3 — Зрозуміти згенеровані результати",
      "Крок 4 — Етапи процесу, пояснені",
      "Крок 5 — Додати власні дані (канонічна подія)",
      "Крок 6 — Згенеровані артефакти: готова до розгортання мережа",
      "Крок 7 — Перевірити запуск за критеріями",
      "Усунення несправностей",
      "Додаток — шпаргалка навчання"]),

    ("h1", "Чого ви досягнете", "1"),
    ("p", "До кінця цього посібника ви матимете: зібрану платформу Jneopallium; повністю відтворюваний "
          "запуск процесу ad-fraud; свіжонавчену каліброву **багатоміткову модель невалідного трафіку**; і "
          "— ключовий результат — **повну, готову до розгортання мережу JNeopallium** (вісім файлів "
          "рівнів, справжні класи часу виконання, вбудовані каліброві голови й рекомендаційний запобіжник "
          "відповіді), яку напряму завантажує супровідний **Посібник з розгортання**."),
    ("callout", "Стеля безпеки для всього конвеєра",
     "Усе, що робить навчена модель, є рекомендаційним. Вона видає кандидатні дії й докази; вона ніколи не "
     "блокує паблішера, не утримує виплату й не звинувачує людину. Звіт готовності тримає "
     "AUTOMATED_ACTION_READY хибним, доки не зафіксовано власний розмічений промисловий трафік, юридичний "
     "перегляд і процес апеляції/відкату.", "warning"),

    ("h1", "Де закінчується навчання і починається розгортання", "2"),
    ("table", ["Фаза", "Що відбувається", "Документ"],
     [["**Навчання** (цей посібник)", "Зборка, запуск процесу, навчання й калібрування, **видача мережі**",
       "Посібник з навчання"],
      ["**Розгортання**", "Завантаження виданої мережі, налаштування джерел подій, запуск скорера",
       "Посібник з розгортання"]],
     [2.0, 3.2, 1.6]),
    ("p", "Передача — це єдина тека згенерованих артефактів (Крок 6). Навчання її **створює**; розгортання "
          "її **споживає**. Тренер записує готову для JNeopallium конфігурацію рівнів і нейронів із "
          "навченими головами, калібруванням і порогами всередині, тож файли, що фіксують ваш запуск, є "
          "файлами, з яких завантажується середовище виконання."),

    ("h1", "Передумови та системні вимоги", "3"),
    ("h2", "Інструментарій"),
    ("table", ["Інструмент", "Версія", "Навіщо"],
     [["Java JDK", "17 або новіша (LTS)", "Зборка й запуск worker / скорера"],
      ["Apache Maven", "3.9 або новіша", "Багатомодульна збірка master + worker"],
      ["Python", "3.10+ (тестовано 3.13)", "Запускає процес; сторонні пакети не потрібні"],
      ["Git", "будь-яка свіжа", "Клонування репозиторію"]],
     [1.8, 2.0, 3.0]),
    ("p", "Процес працює повністю **офлайн і детерміновано** — він не просить файлів вручну. Опційні "
          "публічні джерела, недоступні, заблоковані ліцензією чи захищені обліковими даними, просто "
          "фіксуються; детермінований симулятор забезпечує покриття класів фроду, що не мають повних "
          "публічних міток."),
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
          "нейронів, процесорів і сигналів ad-fraud, на які посилається навчена мережа "
          "(`com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud.*` та інші), тож для навчання не "
          "потрібні додаткові написані вручну класи."),

    ("h1", "Крок 2 — Запустити детермінований процес", "5"),
    ("p", "Одна команда виконує весь процес: виявлення джерел, детермінована генерація сценаріїв, "
          "нормалізація, аудит витоку, навчання, калібрування, оцінка, експорт моделі, тести Java, "
          "відтворення демо та звіт готовності."),
    ("h3", "Linux / macOS"),
    ("code", "scripts/demo-ad-fraud/run_all.sh"),
    ("h3", "Windows (PowerShell)"),
    ("code", "powershell -ExecutionPolicy Bypass -File scripts/demo-ad-fraud/run_all.ps1"),
    ("h3", "Поширені прапорці"),
    ("table", ["Прапорець", "Ефект"],
     [["`--offline`", "Ніколи не намагатися завантажувати з мережі; фіксувати джерела як недоступні"],
      ["`--quick`", "Менший, швидший корпус (усе одно ≥60 подій на сценарій)"],
      ["`--max-rows N`", "Розмір корпусу (за замовчуванням 1900 ≈ 100 подій на сценарій)"],
      ["`--seed N`", "Детермінований сід (за замовчуванням 1729)"],
      ["`--skip-java`", "Пропустити тести Java Maven (корисно під час ітерацій)"]],
     [1.8, 5.0]),
    ("p", "Приклад: `python scripts/demo-ad-fraud/run_all.py --offline`. Запуск друкує зведення JSON із "
          "macro-F1, прапорцями готовності та статусом тестів Java."),

    ("h1", "Крок 3 — Зрозуміти згенеровані результати", "6"),
    ("p", "Докази процесу записуються в `target/jneopallium-ad-fraud/`, а готовий до розгортання пакет "
          "моделі — у `worker/src/main/resources/model/advertising-fraud/`. Зведення має вигляд:"),
    ("code",
     '{\n'
     '  "macroF1": 0.966132,\n'
     '  "readiness": {\n'
     '    "ENGINEERING_READY": true, "SHADOW_READY": true,\n'
     '    "ADVISORY_READY": true, "AUTOMATED_ACTION_READY": false\n'
     '  },\n'
     '  "javaTests": "passed"\n'
     '}'),
    ("table", ["Прапорець готовності", "Значення"],
     [["`ENGINEERING_READY`", "Конвеєр з'єднано, відтворювано й експортує валідний пакет"],
      ["`SHADOW_READY`", "Безпечно запускати лише для читання поруч із промислом, пишучи в приймач аудиту"],
      ["`ADVISORY_READY`", "Безпечно показувати рекомендації аналітикам"],
      ["`AUTOMATED_ACTION_READY`", "**Завжди хибний**, доки немає власних міток + юр. перегляду + апеляції/відкату"]],
     [2.4, 4.4]),

    ("h1", "Крок 4 — Етапи процесу, пояснені", "7"),
    ("num", "**Виявлення джерел і обмежене завантаження.** Публічні джерела (напр. атрибуція / клік-логи "
            "Criteo, краулінг ads.txt / sellers.json, ідентифікатори доброякісних краулерів) каталогізуються; "
            "недоступні чи захищені обліковими даними фіксуються, а не запитуються."),
    ("num", "**Детермінована генерація сценаріїв.** Сід-симулятор виробляє реалістичні події по ~19 "
            "сценаріях (боти, клік-ферми, мотивовані когорти, click spam/injection, підробка постбеків, "
            "підробка інвентаря, а також законний і випадковий трафік)."),
    ("num", "**Нормалізація й аудит витоку.** Події стають канонічними рядками; аудит провалює запуск, "
            "якщо будь-який ідентифікатор пристрою / кліку / кампанії перетинає межі розбиття."),
    ("num", "**Навчання й калібрування.** Класово-зважені, L2-регуляризовані логістичні голови "
            "припасовуються над базовими + поведінковими + нелінійними ознаками взаємодії, потім кожну "
            "голову калібрують за Platt на відкладеному розбитті калібрування."),
    ("num", "**Пороги з урахуванням вартості й оцінка.** Робочі точки на мітку обираються за очікуваною "
            "користю; метрики включають точність/повноту/F1, PR/ROC-AUC, Brier, похибку калібрування й "
            "зважену за вартістю економію."),
    ("num", "**Експорт, тести, готовність, документи.** Готова до розгортання мережа записується, тести "
            "Java запускаються, а звіт готовності й картки моделі/даних формуються."),

    ("h1", "Крок 5 — Додати власні дані (канонічна подія)", "8"),
    ("p", "Щоб вийти за межі еталонного корпусу, ви надаєте власні канонічні події. Кожна подія зберігає й "
          "час події, й час прийому та несе поля цілісності, поведінкові, постачання й відкладеної якості, "
          "а також метадані походження мітки. Сирі персональні ідентифікатори мають бути хешовані HMAC "
          "перед навчанням, збереженням чи логами."),
    ("callout", "Політика розбиття — це гарантія цілісності",
     "Ніколи не розбивайте за випадковим рядком. Розбивайте за сценарієм, епізодом кампанії й детермінованим "
     "блоком реплік і тримайте ворожий холдаут — тож заявлена якість відображає справді небачений трафік. "
     "Аудит витоку забезпечує це автоматично.", "info"),
    ("p", "Походження міток має значення: еталонна модель використовує детерміновані мітки симулятора й "
          "слабкі публічні метадані. Реальна промислова точність потребує **власного розміченого трафіку** "
          "— підтверджених чарджбеків, рішень MMP щодо фроду, ручних вердиктів аналітиків — і валідації "
          "вперед у часі та на небачених паблішерах."),

    ("h1", "Крок 6 — Згенеровані артефакти: готова до розгортання мережа", "9"),
    ("p", "Кожен запуск записує теку конфігурації у стилі JNeopallium, готову до розгортання як є, — "
          "власне мережу рівнів і нейронів, з якої завантажується середовище виконання:"),
    ("table", ["Артефакт", "Що це"],
     [["`model-descriptor.json`", "Опис усієї мережі: 8 рівнів, 22 нейрони, мапа частот, класи часу виконання"],
      ["`fallback-model.json`", "Навчені каліброві голови + нелінійний прихований рівень (featureNames, hidden, heads)"],
      ["`layer-0.json` … `layer-6-…`, `result-layer.json`", "Вісім файлів рівнів для розгортання"],
      ["`thresholds.json` / `calibration.json`", "Пороги на мітку з урахуванням вартості та Platt-калібрування"],
      ["`feature-schema.json` / `label-schema.json`", "Вхідні ознаки та десять міток"],
      ["`training-manifest.json`", "Джерела, кількість прикладів, політика розбиття, повні метрики"],
      ["`MODEL_CARD.md` / `DATA_CARD.md`", "Картки безпеки (лише рекомендації; статуси джерел)"],
      ["`checksums.sha256`", "Маніфест цілісності, який пакет Java перевіряє при завантаженні"]],
     [3.0, 3.8]),
    ("p", "`model-descriptor.json` фіксує **8-рівневу, 22-нейронну** мережу — вхід, цілісність подій, "
          "сутність/граф/якість, поведінкові докази, взаємодія ознак (нелінійний прихований рівень), "
          "навчена кореляція, рекомендаційний запобіжник і результат — де кожен нейрон посилається на "
          "конкретний клас із пакета ad-fraud worker."),
    ("callout", "Чому це важливо",
     "Оскільки навчання видає справжню, придатну до перевірки, готову до розгортання конфігурацію з "
     "каліброваними головами й порогами всередині, між «тим, що навчили» і «тим, що запускаємо», немає "
     "кроку перекладу. Посібник з розгортання просто спрямовує середовище виконання на цю теку.", "success"),

    ("h1", "Крок 7 — Перевірити запуск за критеріями", "10"),
    ("num", "Аудит витоку пройдено (жоден ідентифікатор не перетинає межі розбиття)."),
    ("num", "Macro-F1 відповідає вашій планці (еталонний запуск сягає ≈ 0.97; таблиця на мітку — в описі та "
            "Звіті про тестування)."),
    ("num", "Калібрування — справжнє Platt-масштабування на мітку (не тотожність), а похибка калібрування "
            "в межах бюджету."),
    ("num", "Вісім файлів рівнів, опис і `checksums.sha256` записано, і вони посилаються на очікувані класи "
            "часу виконання."),
    ("num", "Тести Java проходять (`javaTests: passed`), а звіт готовності тримає "
            "`AUTOMATED_ACTION_READY=false`."),
    ("callout", "Перевірка відтворюваності",
     "Запуск детермінований для заданого сіда. Повторний запуск тієї самої команди має відтворити той самий "
     "macro-F1 і ті самі метрики на мітку. Розбіжність сигналізує, що змінилися вхід, сід чи інструментарій.",
     "info"),

    ("h1", "Усунення несправностей", "11"),
    ("table", ["Симптом", "Імовірна причина й виправлення"],
     [["`java -version` показує < 17", "Встановіть JDK 17+ і вкажіть `JAVA_HOME`"],
      ["Процес завершується на аудиті витоку", "Ідентифікатор перетинає розбиття; перевірте політику розбиття даних"],
      ["Тести Java падають після зміни", "Змінилися структурні лічильники; перезекспортуйте пакет і узгодьте очікування тесту"],
      ["Джерела показують `dataset_unavailable`", "Очікувано офлайн; детермінований симулятор усе одно дає покриття"],
      ["Macro-F1 нижчий за очікуваний на ваших даних", "Додайте власні мітки й більше варіативності на сценарій; перевірте калібрування"],
      ["Невідповідність контрольної суми в Java", "Перезапустіть процес, щоб `checksums.sha256` відповідав експортованим файлам"]],
     [3.1, 3.7]),

    ("h1", "Додаток — шпаргалка навчання", "12"),
    ("h3", "Збірка"),
    ("code", "git clone https://github.com/rakovpublic/jneopallium.git && cd jneopallium && mvn clean install"),
    ("h3", "Запуск процесу"),
    ("code",
     "# повний детермінований запуск (офлайн)\n"
     "python scripts/demo-ad-fraud/run_all.py --offline\n"
     "\n"
     "# швидка ітерація, без тестів Java\n"
     "python scripts/demo-ad-fraud/run_all.py --quick --offline --skip-java\n"
     "\n"
     "# обгортки Linux/macOS і Windows\n"
     "scripts/demo-ad-fraud/run_all.sh\n"
     "powershell -File scripts/demo-ad-fraud/run_all.ps1"),
    ("spacer", 8),
    ("pi", "Jneopallium Ad-Fraud Guardian · Посібник з навчання. Тренер видає готову до розгортання мережу "
           "JNeopallium; як її запустити — у Посібнику з розгортання. Режим безпеки: ADVISORY / SHADOW. "
           "Ліцензія: BSD 3-Clause."),
]
