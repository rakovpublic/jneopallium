# -*- coding: utf-8 -*-
"""Ad-Fraud Guardian — deployment guide (EN + UK), block DSL."""

from __future__ import annotations

DATE = "23 June 2026"
DATE_UK = "23 червня 2026"


def deployment(lang: str) -> list:
    return _EN if lang == "en" else _UK


_EN = [
    ("cover", "Jneopallium · Ad-Fraud Guardian",
     "Deployment Guide",
     "Run the trained invalid-traffic network as a shadow/advisory scoring service",
     [("Document", "Production Deployment Guide"),
      ("Product", "Jneopallium Ad-Fraud Guardian (advertising-fraud module)"),
      ("Consumes", "The deployable network from the Training Guide"),
      ("Safety mode", "ADVISORY / SHADOW (recommend-only)"),
      ("Interfaces", "HTTP scoring service + embeddable runtime scorer"),
      ("Author", "Dmytro Rakovskyi — Kharkiv, Ukraine"),
      ("Date", DATE),
      ("License", "BSD 3-Clause")],
     "Deployment Guide",
     "The deployment half of the pipeline: take the JNeopallium network the trainer produced, load it into "
     "the runtime scorer, expose it as an HTTP scoring service beside your ad stack, and run it as a "
     "continuous shadow-to-advisory invalid-traffic scorer that never blocks or withholds on its own."),

    ("toc", "Contents",
     ["What you will achieve",
      "Prerequisites — the trained bundle and runtime",
      "The runtime scorer and the model bundle",
      "The deployable package (what training produced)",
      "Option A — the HTTP scoring service",
      "Option B — the embedded worker network",
      "The advisory decision contract",
      "Configure event sources and streaming input",
      "Promote: shadow → advisory → bounded enforcement",
      "Monitoring in production",
      "Safety, privacy and rollback",
      "Troubleshooting",
      "Appendix — deployment cheat-sheet"]),

    ("h1", "What you will achieve", "1"),
    ("p", "By the end of this guide you will have taken the **deployable network produced by the Training "
          "Guide** and run it as a continuous **advisory invalid-traffic scorer** that names the fraud "
          "family, shows its evidence, and recommends a candidate action for every ad event — but never "
          "blocks a publisher, withholds a payout, or accuses a person."),
    ("callout", "Safety ceiling for the whole guide",
     "Everything below operates in ADVISORY / SHADOW mode. The scorer emits probabilities, evidence, and "
     "candidate actions; consequential actions (billing rejection, payout withholding, account blocking) "
     "stay disabled until first-party labelled validation, legal review, and an appeal/rollback process "
     "are in place.", "warning"),

    ("h1", "Prerequisites — the trained bundle and runtime", "2"),
    ("bullet", "**A trained network** from the Training Guide — the model bundle directory "
               "(`model-descriptor.json`, `fallback-model.json`, the eight layer files, `thresholds.json`, "
               "`calibration.json`, `checksums.sha256`)."),
    ("bullet", "**Java 17+** and the built worker (`mvn clean install`)."),
    ("bullet", "**Your event feed** — a Kafka-style stream or an HTTP integration from your ad server, "
               "SSP/DSP, or mobile measurement partner."),
    ("bullet", "**An output destination** — a JSONL/audit sink for shadow mode, or your SIEM / data "
               "warehouse / analyst queue for operator-visible advisories."),
    ("callout", "Training first",
     "If you do not yet have a trained network, run the companion Training Guide. Deployment consumes its "
     "output directory directly; the runtime bundle verifies its checksums on load.", "info"),

    ("h1", "The runtime scorer and the model bundle", "3"),
    ("p", "Two classes do the work at runtime:"),
    ("bullet", "**`AdFraudModelBundle`** loads the model bundle from the classpath "
               "(`model/advertising-fraud`), **verifies `checksums.sha256`**, and parses the calibrated "
               "heads, the non-linear hidden layer, the per-label thresholds, and the Platt calibration. "
               "If the bundle is missing or fails verification it falls back to a transparent rules-only "
               "model, so the service degrades safely rather than crashing."),
    ("bullet", "**`AdFraudRuntimeScorer`** extracts the typed features from each event (integrity, "
               "behavioural, supply-chain, quality, and the behavioural-evidence features), evaluates the "
               "hidden layer and the calibrated heads, and produces an `AdFraudDecision`."),
    ("p", "The decision carries per-label probabilities, an overall invalid-traffic probability, the "
          "contributing reasons, a duplicate-event flag, the model version, and the recommended candidate "
          "action — everything an analyst needs to act and audit."),

    ("h1", "The deployable package (what training produced)", "4"),
    ("p", "The trainer emits the **actual JNeopallium network**, not an abstract model. The bundle the "
          "runtime loads is:"),
    ("table", ["Artifact", "Role at deploy time"],
     [["`model-descriptor.json`", "Whole-network map: 8 layers, 22 neurons, frequency map, runtime classes"],
      ["`fallback-model.json`", "Calibrated heads + the non-linear hidden layer (featureNames, hidden, heads)"],
      ["`thresholds.json` / `calibration.json`", "Cost-aware per-label thresholds and Platt calibration"],
      ["`layer-0.json` … `result-layer.json`", "The eight deployable layer files"],
      ["`checksums.sha256`", "Integrity manifest verified on load"]],
     [3.0, 3.8]),
    ("p", "Total network: **8 layers, 22 real neurons, 21 features** (8 base risks + 5 behavioural-"
          "evidence + 8 non-linear interaction units). Every neuron names a concrete class from the "
          "worker's ad-fraud package, so deployment loads this directory — it does not re-implement "
          "anything."),

    ("h1", "Option A — the HTTP scoring service", "5"),
    ("p", "The simplest production shape is the bundled HTTP scoring service, which loads the model bundle "
          "and exposes a scoring endpoint plus Prometheus-style metrics. Point it at the verified bundle "
          "and POST canonical events:"),
    ("code",
     "# score a single canonical event\n"
     "POST http://host:port/v1/ad-fraud/score\n"
     "Content-Type: application/json\n"
     "{ \"eventId\": \"...\", \"eventType\": \"CLICK\", \"eventTime\": 1800000000000, ... }\n"
     "\n"
     "# operational metrics\n"
     "GET  http://host:port/metrics      # events_total, fraud_probability_histogram, ..."),
    ("p", "The response is the advisory decision (next section). For shadow mode, run the service read-"
          "only beside production and write its decisions to an audit sink; nothing in the response path "
          "touches your serving or billing."),

    ("h1", "Option B — the embedded worker network", "6"),
    ("p", "To run the full eight-layer network inside the Jneopallium worker (for cluster-scale or to "
          "compose with other modules), launch the worker entry point with the four standard arguments and "
          "point `configuration.input.layermeta` at the generated bundle directory:"),
    ("code",
     "java -cp worker/target/worker-jar-with-dependencies.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
     "  local \\\n"
     "  file:///opt/jneopallium/ad-fraud-model.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext \\\n"
     "  /opt/jneopallium/ad-fraud-context.json"),
    ("p", "`Entry` deserialises the context, loads the model JAR into an isolated class loader, and maps "
          "`local` / `http` / `grpc` to the single-process, HTTP-cluster, or gRPC application. The "
          "ad-fraud neuron, processor, and signal classes already ship in the worker, so the deployment "
          "JAR provides only your event source (`IInitInput`) and output sink (`IOutputAggregator`)."),

    ("h1", "The advisory decision contract", "7"),
    ("p", "Every scored event returns an auditable decision. Forward it to your SIEM, data warehouse, "
          "analyst queue, or billing-review system:"),
    ("code",
     '{\n'
     '  "eventId": "clk-123",\n'
     '  "modelVersion": "1.0.0-reference-advisory",\n'
     '  "mode": "ADVISORY",\n'
     '  "probabilities": {\n'
     '    "clickInjection": 0.94, "attributionHijack": 0.91, "bot": 0.03, ...\n'
     '  },\n'
     '  "overallInvalidTrafficProbability": 0.95,\n'
     '  "recommendedAction": "HOLD_PAYOUT_CANDIDATE",\n'
     '  "reasons": [ "conversion observed before click", "click injection timing anomaly" ],\n'
     '  "duplicateEvent": false,\n'
     '  "evidenceCompleteness": 0.8\n'
     '}'),
    ("p", "Candidate actions map from the response bands: `ALLOW` / `LOG` (low), `REVIEW` (0.30–0.60), "
          "`RATE_LIMIT_CANDIDATE` (0.60–0.75), `HOLD_PAYOUT_CANDIDATE` (0.75–0.90), `ESCALATE_TO_ANALYST` "
          "(0.90–1.0). Every action is a **candidate**; none is executed by the system."),

    ("h1", "Configure event sources and streaming input", "8"),
    ("p", "Feed the scorer your live events. Two patterns:"),
    ("bullet", "**HTTP integration** — your ad server / SSP / MMP POSTs each canonical event to "
               "`/v1/ad-fraud/score` and stores the returned decision."),
    ("bullet", "**Stream integration** — a site-specific `IInitInput` consumes your Kafka topics (bids, "
               "impressions, clicks, postbacks, retention/refund) and converts each record into the typed "
               "ad-fraud signals, preserving event time so delayed and out-of-order events still correlate."),
    ("p", "Keep delayed-quality events (retention, refund, chargeback, uninstall) flowing — they are the "
          "slow truth that confirms or revises an earlier advisory, and they run on the slow loop by "
          "design."),

    ("h1", "Promote: shadow → advisory → bounded enforcement", "9"),
    ("p", "Promote gradually. Each step earns the next; consequential actions stay disabled until the whole "
          "sequence has passed."),
    ("num", "**Offline replay.** Score an exported event log without touching production. Deliver named "
            "invalid-traffic findings, an evidence timeline, an estimated wasted-spend impact, and a "
            "false-positive review."),
    ("num", "**Shadow.** Read-only scoring beside production, decisions to an audit sink only. Compare "
            "advisories against first-party outcomes (chargebacks, MMP rulings) and analyst review."),
    ("num", "**Advisory.** Surface decisions to analysts and billing review, once precision at your review "
            "capacity and calibration meet target."),
    ("num", "**Bounded enforcement.** Only after first-party labelled validation, legal review, and an "
            "appeal/rollback process — and even then, narrowly scoped (e.g. hold a specific payout for "
            "review), never silent blocking or accusation."),

    ("h1", "Monitoring in production", "10"),
    ("bullet", "Events scored per minute; dropped, delayed, and out-of-order events."),
    ("bullet", "Per-label advisory rates; the fraud-probability histogram; false-positive review rate."),
    ("bullet", "Confirmed true-positive rate against first-party outcomes (chargebacks, MMP rulings)."),
    ("bullet", "Calibration drift and model-drift signals (the slow monitoring cadence)."),
    ("bullet", "Model version and bundle checksum used for every decision."),
    ("p", "Every decision preserves its lineage: event id, per-label probabilities, contributing reasons, "
          "the model version, and the recommended candidate action."),

    ("h1", "Safety, privacy and rollback", "11"),
    ("bullet", "**Advisory by construction.** The response gate emits candidates only; automatic billing "
               "rejection, payout withholding, account blocking, and accusations are structurally disabled."),
    ("bullet", "**Privacy preserved.** Identifiers are pseudonymised (HMAC) before training and logs; raw "
               "IPs, precise geolocation, and user-agent strings are never exported; missing values are "
               "distinct from zero risk."),
    ("bullet", "**Safe degradation.** If the bundle is missing or fails checksum verification, the runtime "
               "falls back to a transparent rules-only model rather than failing the request path."),
    ("bullet", "**Rollback.** Keep the previous bundle available; roll back if false positives exceed "
               "budget, calibration drifts, or first-party outcomes diverge from advisories."),
    ("callout", "Honest limitation",
     "The checked-in reference network is trained on a deterministic simulator plus weak public-source "
     "metadata. It is excellent for repeatable pipeline evidence and label-separation, but real-world "
     "accuracy requires first-party labelled production traffic, forward-time and unseen-publisher "
     "validation, and a documented review of data handling. It is advisory until then.", "warning"),

    ("h1", "Troubleshooting", "12"),
    ("table", ["Symptom", "Likely cause and fix"],
     [["Service logs \"rules-only fallback\"", "Bundle missing or checksum mismatch — re-export and redeploy the bundle"],
      ["All probabilities look flat / wrong", "Feature names changed; redeploy the matching bundle so the runtime augments correctly"],
      ["Decisions emit a blocking action", "Verify mode is ADVISORY; consequential actions must stay candidate-only"],
      ["Delayed-quality labels never improve", "Ensure retention/refund/chargeback events are fed on the slow loop"],
      ["High false positives on a publisher", "Tune per-label thresholds / review capacity; check for a baseline shift"],
      ["Scores diverge from training", "You deployed a stale bundle; redeploy the latest trained package"]],
     [3.0, 3.8]),

    ("h1", "Appendix — deployment cheat-sheet", "13"),
    ("h3", "Score an event (HTTP service)"),
    ("code",
     "curl -s -X POST http://host:port/v1/ad-fraud/score \\\n"
     "  -H 'Content-Type: application/json' \\\n"
     "  -d '{\"eventId\":\"clk-1\",\"eventType\":\"CLICK\",\"eventTime\":1800000000000}'\n"
     "curl -s http://host:port/metrics"),
    ("h3", "Embedded worker network"),
    ("code",
     "java -cp worker/target/worker-jar-with-dependencies.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
     "  local file:///opt/jneopallium/ad-fraud-model.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext \\\n"
     "  /opt/jneopallium/ad-fraud-context.json"),
    ("spacer", 8),
    ("pi", "Jneopallium Ad-Fraud Guardian · Deployment Guide. Consumes the network produced by the "
           "Training Guide. Safety mode: ADVISORY / SHADOW. Consequential actions require first-party "
           "validation, legal review, and an appeal/rollback process. License: BSD 3-Clause."),
]


_UK = [
    ("cover", "Jneopallium · Ad-Fraud Guardian",
     "Посібник з розгортання",
     "Запустіть навчену мережу невалідного трафіку як тіньову/рекомендаційну службу скорингу",
     [("Документ", "Посібник з промислового розгортання"),
      ("Продукт", "Jneopallium Ad-Fraud Guardian (модуль advertising-fraud)"),
      ("Споживає", "Готову до розгортання мережу з Посібника з навчання"),
      ("Режим безпеки", "ADVISORY / SHADOW (лише рекомендації)"),
      ("Інтерфейси", "HTTP-служба скорингу + вбудовуваний скорер"),
      ("Автор", "Дмитро Раковський — Харків, Україна"),
      ("Дата", DATE_UK),
      ("Ліцензія", "BSD 3-Clause")],
     "Посібник з розгортання",
     "Половина конвеєра, що відповідає за розгортання: візьміть мережу JNeopallium, яку створив тренер, "
     "завантажте її в скорер, виставте як HTTP-службу скорингу поруч із рекламним стеком і запустіть як "
     "безперервний тіньово-рекомендаційний скорер невалідного трафіку, що сам ніколи не блокує й не "
     "утримує."),

    ("toc", "Зміст",
     ["Чого ви досягнете",
      "Передумови — навчений пакет і середовище виконання",
      "Скорер і пакет моделі",
      "Готовий до розгортання пакет (що створило навчання)",
      "Варіант A — HTTP-служба скорингу",
      "Варіант B — вбудована мережа worker",
      "Контракт рекомендаційного рішення",
      "Налаштуйте джерела подій і потоковий вхід",
      "Перехід: тінь → рекомендації → обмежене виконання",
      "Моніторинг у промислі",
      "Безпека, приватність і відкат",
      "Усунення несправностей",
      "Додаток — шпаргалка розгортання"]),

    ("h1", "Чого ви досягнете", "1"),
    ("p", "До кінця цього посібника ви візьмете **готову до розгортання мережу, створену Посібником з "
          "навчання**, і запустите її як безперервний **рекомендаційний скорер невалідного трафіку**, що "
          "називає родину фроду, показує докази й рекомендує кандидатну дію для кожної рекламної події — "
          "але сам ніколи не блокує паблішера, не утримує виплату й не звинувачує людину."),
    ("callout", "Стеля безпеки для всього посібника",
     "Усе нижче працює в режимі ADVISORY / SHADOW. Скорер видає ймовірності, докази й кандидатні дії; "
     "відчутні дії (відхилення білінгу, утримання виплат, блокування акаунтів) лишаються вимкненими, доки "
     "не буде валідації на власних мітках, юридичного перегляду й процесу апеляції/відкату.", "warning"),

    ("h1", "Передумови — навчений пакет і середовище виконання", "2"),
    ("bullet", "**Навчена мережа** з Посібника з навчання — тека пакета моделі (`model-descriptor.json`, "
               "`fallback-model.json`, вісім файлів рівнів, `thresholds.json`, `calibration.json`, "
               "`checksums.sha256`)."),
    ("bullet", "**Java 17+** і зібраний worker (`mvn clean install`)."),
    ("bullet", "**Ваш канал подій** — потік у стилі Kafka або HTTP-інтеграція з вашого ad server, SSP/DSP "
               "чи мобільного партнера вимірювання."),
    ("bullet", "**Призначення виводу** — приймач JSONL/аудиту для тіньового режиму або ваш SIEM / сховище "
               "даних / черга аналітика для видимих оператору рекомендацій."),
    ("callout", "Спершу навчання",
     "Якщо у вас ще немає навченої мережі, виконайте супровідний Посібник з навчання. Розгортання споживає "
     "його вихідну теку напряму; пакет середовища виконання перевіряє контрольні суми при завантаженні.",
     "info"),

    ("h1", "Скорер і пакет моделі", "3"),
    ("p", "Дві класи виконують роботу під час роботи:"),
    ("bullet", "**`AdFraudModelBundle`** завантажує пакет моделі з classpath (`model/advertising-fraud`), "
               "**перевіряє `checksums.sha256`** і розбирає каліброві голови, нелінійний прихований рівень, "
               "пороги на мітку й Platt-калібрування. Якщо пакета бракує чи перевірка не проходить, він "
               "переходить на прозору модель лише з правил, тож служба деградує безпечно, а не падає."),
    ("bullet", "**`AdFraudRuntimeScorer`** видобуває типізовані ознаки з кожної події (цілісність, "
               "поведінкові, постачання, якість і поведінкові докази), обчислює прихований рівень і "
               "каліброві голови та видає `AdFraudDecision`."),
    ("p", "Рішення несе ймовірності на мітку, загальну ймовірність невалідного трафіку, причини, що "
          "долучилися, прапорець дубліката події, версію моделі й рекомендовану кандидатну дію — усе, що "
          "потрібно аналітику для дії та аудиту."),

    ("h1", "Готовий до розгортання пакет (що створило навчання)", "4"),
    ("p", "Тренер видає **власне мережу JNeopallium**, а не абстрактну модель. Пакет, який завантажує "
          "середовище виконання:"),
    ("table", ["Артефакт", "Роль під час розгортання"],
     [["`model-descriptor.json`", "Карта всієї мережі: 8 рівнів, 22 нейрони, мапа частот, класи часу виконання"],
      ["`fallback-model.json`", "Каліброві голови + нелінійний прихований рівень (featureNames, hidden, heads)"],
      ["`thresholds.json` / `calibration.json`", "Пороги на мітку з урахуванням вартості та Platt-калібрування"],
      ["`layer-0.json` … `result-layer.json`", "Вісім файлів рівнів для розгортання"],
      ["`checksums.sha256`", "Маніфест цілісності, що перевіряється при завантаженні"]],
     [3.0, 3.8]),
    ("p", "Уся мережа: **8 рівнів, 22 справжні нейрони, 21 ознака** (8 базових ризиків + 5 поведінкових + "
          "8 нелінійних одиниць взаємодії). Кожен нейрон називає конкретний клас із пакета ad-fraud worker, "
          "тож розгортання завантажує цю теку — воно нічого не реалізує наново."),

    ("h1", "Варіант A — HTTP-служба скорингу", "5"),
    ("p", "Найпростіша промислова форма — вбудована HTTP-служба скорингу, що завантажує пакет моделі й "
          "виставляє ендпоінт скорингу плюс метрики у стилі Prometheus. Спрямуйте її на перевірений пакет "
          "і надсилайте POST канонічних подій:"),
    ("code",
     "# оцінити одну канонічну подію\n"
     "POST http://host:port/v1/ad-fraud/score\n"
     "Content-Type: application/json\n"
     "{ \"eventId\": \"...\", \"eventType\": \"CLICK\", \"eventTime\": 1800000000000, ... }\n"
     "\n"
     "# операційні метрики\n"
     "GET  http://host:port/metrics      # events_total, fraud_probability_histogram, ..."),
    ("p", "Відповідь — це рекомендаційне рішення (наступний розділ). Для тіньового режиму запускайте службу "
          "лише для читання поруч із промислом і пишіть її рішення в приймач аудиту; ніщо у шляху відповіді "
          "не торкається вашого показу чи білінгу."),

    ("h1", "Варіант B — вбудована мережа worker", "6"),
    ("p", "Щоб запустити повну восьмирівневу мережу всередині worker Jneopallium (для масштабу кластера чи "
          "композиції з іншими модулями), запустіть вхідний пункт worker із чотирма стандартними "
          "аргументами й спрямуйте `configuration.input.layermeta` на згенеровану теку пакета:"),
    ("code",
     "java -cp worker/target/worker-jar-with-dependencies.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
     "  local \\\n"
     "  file:///opt/jneopallium/ad-fraud-model.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext \\\n"
     "  /opt/jneopallium/ad-fraud-context.json"),
    ("p", "`Entry` десеріалізує контекст, завантажує JAR моделі в ізольований завантажувач класів і "
          "відображає `local` / `http` / `grpc` на застосунок одного процесу, HTTP-кластера чи gRPC. Класи "
          "нейронів, процесорів і сигналів ad-fraud уже постачаються в worker, тож JAR розгортання надає "
          "лише ваше джерело подій (`IInitInput`) і приймач виводу (`IOutputAggregator`)."),

    ("h1", "Контракт рекомендаційного рішення", "7"),
    ("p", "Кожна оцінена подія повертає аудитоване рішення. Пересилайте його у ваш SIEM, сховище даних, "
          "чергу аналітика чи систему перегляду білінгу:"),
    ("code",
     '{\n'
     '  "eventId": "clk-123",\n'
     '  "modelVersion": "1.0.0-reference-advisory",\n'
     '  "mode": "ADVISORY",\n'
     '  "probabilities": {\n'
     '    "clickInjection": 0.94, "attributionHijack": 0.91, "bot": 0.03, ...\n'
     '  },\n'
     '  "overallInvalidTrafficProbability": 0.95,\n'
     '  "recommendedAction": "HOLD_PAYOUT_CANDIDATE",\n'
     '  "reasons": [ "conversion observed before click", "click injection timing anomaly" ],\n'
     '  "duplicateEvent": false,\n'
     '  "evidenceCompleteness": 0.8\n'
     '}'),
    ("p", "Кандидатні дії відображаються зі смуг відповіді: `ALLOW` / `LOG` (низько), `REVIEW` (0.30–0.60), "
          "`RATE_LIMIT_CANDIDATE` (0.60–0.75), `HOLD_PAYOUT_CANDIDATE` (0.75–0.90), `ESCALATE_TO_ANALYST` "
          "(0.90–1.0). Кожна дія — **кандидат**; жодна не виконується системою."),

    ("h1", "Налаштуйте джерела подій і потоковий вхід", "8"),
    ("p", "Подавайте скореру ваші живі події. Два шаблони:"),
    ("bullet", "**HTTP-інтеграція** — ваш ad server / SSP / MMP надсилає POST кожної канонічної події на "
               "`/v1/ad-fraud/score` і зберігає повернуте рішення."),
    ("bullet", "**Потокова інтеграція** — специфічний для майданчика `IInitInput` споживає ваші теми Kafka "
               "(ставки, покази, кліки, постбеки, утримання/повернення) і перетворює кожен запис на "
               "типізовані сигнали ad-fraud, зберігаючи час події, тож запізнілі й невпорядковані події все "
               "одно корелюються."),
    ("p", "Тримайте події відкладеної якості (утримання, повернення, чарджбек, видалення) в потоці — це "
          "повільна правда, що підтверджує чи переглядає попередню рекомендацію, і вона працює в "
          "повільному циклі за задумом."),

    ("h1", "Перехід: тінь → рекомендації → обмежене виконання", "9"),
    ("p", "Переходьте поступово. Кожен крок заслуговує наступний; відчутні дії лишаються вимкненими, доки "
          "вся послідовність не пройдена."),
    ("num", "**Офлайн-відтворення.** Оцініть експортований лог подій, не торкаючись промислу. Видайте "
            "названі знахідки невалідного трафіку, хронологію доказів, оцінений вплив змарнованого бюджету "
            "й перегляд хибних спрацювань."),
    ("num", "**Тінь.** Скоринг лише для читання поруч із промислом, рішення лише в приймач аудиту. "
            "Порівняйте рекомендації з власними результатами (чарджбеки, рішення MMP) і переглядом "
            "аналітика."),
    ("num", "**Рекомендації.** Показуйте рішення аналітикам і перегляду білінгу, коли точність на вашій "
            "ємності перегляду й калібрування сягнуть цілі."),
    ("num", "**Обмежене виконання.** Лише після валідації на власних мітках, юридичного перегляду й процесу "
            "апеляції/відкату — і навіть тоді вузько (напр. утримати конкретну виплату на перегляд), ніколи "
            "тихе блокування чи звинувачення."),

    ("h1", "Моніторинг у промислі", "10"),
    ("bullet", "Подій оцінено за хвилину; відкинуті, запізнілі та невпорядковані події."),
    ("bullet", "Рівні рекомендацій на мітку; гістограма ймовірності фроду; рівень перегляду хибних "
               "спрацювань."),
    ("bullet", "Підтверджений рівень істинних позитивів проти власних результатів (чарджбеки, рішення MMP)."),
    ("bullet", "Дрейф калібрування й сигнали дрейфу моделі (повільний каданс моніторингу)."),
    ("bullet", "Версія моделі й контрольна сума пакета, використані для кожного рішення."),
    ("p", "Кожне рішення зберігає родовід: id події, ймовірності на мітку, причини, що долучилися, версію "
          "моделі й рекомендовану кандидатну дію."),

    ("h1", "Безпека, приватність і відкат", "11"),
    ("bullet", "**Рекомендаційний за побудовою.** Запобіжник відповіді видає лише кандидатів; автоматичне "
               "відхилення білінгу, утримання виплат, блокування акаунтів і звинувачення структурно "
               "вимкнені."),
    ("bullet", "**Приватність збережено.** Ідентифікатори псевдонімізуються (HMAC) перед навчанням і "
               "логами; сирі IP, точна геолокація й рядки user-agent ніколи не експортуються; відсутні "
               "значення окремі від нульового ризику."),
    ("bullet", "**Безпечна деградація.** Якщо пакета бракує чи перевірка контрольної суми не проходить, "
               "середовище виконання переходить на прозору модель лише з правил, а не падає на шляху "
               "запиту."),
    ("bullet", "**Відкат.** Тримайте попередній пакет доступним; відкочуйте, якщо хибні спрацювання "
               "перевищують бюджет, калібрування дрейфує чи власні результати розходяться з рекомендаціями."),
    ("callout", "Чесне обмеження",
     "Вбудована еталонна мережа навчена на детермінованому симуляторі плюс слабких публічних метаданих. "
     "Вона чудова для відтворюваних доказів конвеєра й розділення міток, але реальна точність потребує "
     "власного розміченого промислового трафіку, валідації вперед у часі та на небачених паблішерах і "
     "задокументованого перегляду обробки даних. До того часу вона рекомендаційна.", "warning"),

    ("h1", "Усунення несправностей", "12"),
    ("table", ["Симптом", "Імовірна причина й виправлення"],
     [["Служба логує «rules-only fallback»", "Пакета бракує чи невідповідність контрольної суми — перезекспортуйте й перерозгорніть пакет"],
      ["Усі ймовірності пласкі / неправильні", "Змінилися імена ознак; перерозгорніть відповідний пакет, щоб скорер коректно доповнював"],
      ["Рішення видають блокувальну дію", "Перевірте, що режим ADVISORY; відчутні дії мають лишатися лише кандидатами"],
      ["Мітки відкладеної якості не покращуються", "Переконайтеся, що події утримання/повернення/чарджбеку подаються в повільному циклі"],
      ["Багато хибних спрацювань на паблішері", "Налаштуйте пороги на мітку / ємність перегляду; перевірте зсув базової лінії"],
      ["Оцінки розходяться з навчанням", "Ви розгорнули застарілий пакет; перерозгорніть найновіший навчений пакет"]],
     [3.0, 3.8]),

    ("h1", "Додаток — шпаргалка розгортання", "13"),
    ("h3", "Оцінити подію (HTTP-служба)"),
    ("code",
     "curl -s -X POST http://host:port/v1/ad-fraud/score \\\n"
     "  -H 'Content-Type: application/json' \\\n"
     "  -d '{\"eventId\":\"clk-1\",\"eventType\":\"CLICK\",\"eventTime\":1800000000000}'\n"
     "curl -s http://host:port/metrics"),
    ("h3", "Вбудована мережа worker"),
    ("code",
     "java -cp worker/target/worker-jar-with-dependencies.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
     "  local file:///opt/jneopallium/ad-fraud-model.jar \\\n"
     "  com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext \\\n"
     "  /opt/jneopallium/ad-fraud-context.json"),
    ("spacer", 8),
    ("pi", "Jneopallium Ad-Fraud Guardian · Посібник з розгортання. Споживає мережу, створену Посібником з "
           "навчання. Режим безпеки: ADVISORY / SHADOW. Відчутні дії потребують валідації на власних мітках, "
           "юридичного перегляду й процесу апеляції/відкату. Ліцензія: BSD 3-Clause."),
]
