# -*- coding: utf-8 -*-
"""Test report content (EN + UK), block DSL."""

from __future__ import annotations

DATE = "23 June 2026"
DATE_UK = "23 червня 2026"


def testreport(lang: str) -> list:
    return _EN if lang == "en" else _UK


_EN = [
    ("cover", "Jneopallium · Demo 06",
     "Complete Test Report",
     "Cybersecurity Temporal Threat Correlation — results, evidence, and an honest reading",
     [("Document", "Verification & Test Report"),
      ("Product", "Jneopallium Cybersecurity Module (Demo 06)"),
      ("Model", "cybersecurity-temporal-threat-correlator 1.0.0-reference-temporal"),
      ("Test scope", "Unit, behavioural, model-quality, scale"),
      ("Result", "All gates passed (reference corpus)"),
      ("Author", "Dmytro Rakovskyi — Kharkiv, Ukraine"),
      ("Date", DATE),
      ("License", "BSD 3-Clause")],
     "Test Report",
     "What was tested, how it performed, and — stated plainly and up front — exactly what the results do "
     "and do not prove. Written so a non-specialist can read every number with confidence."),

    ("toc", "Contents",
     ["Executive summary of results",
      "What we tested, and why",
      "Test environment",
      "A plain-language primer on the metrics",
      "Layer 1 — Unit tests (the SecurityModuleTest suite)",
      "Layer 2 — Behavioural tests (the full-run demo)",
      "Layer 3 — Trained-model quality metrics",
      "Calibration: are the probabilities honest?",
      "What the model learned (feature evidence)",
      "Layer 4 — Production-scale pipeline evidence",
      "The honest limitations (read this carefully)",
      "Overall verdict",
      "Glossary"]),

    ("h1", "Executive summary of results", "1"),
    ("p", "The Demo 06 cybersecurity module was verified at four independent levels: **unit tests** "
          "(individual components behave correctly), **behavioural tests** (the running demo makes the "
          "right decisions), **model-quality metrics** (the trained correlator separates attacks from "
          "benign activity), and **scale evidence** (the training pipeline holds up at a 100 GiB logical "
          "corpus target). Every gate passed."),
    ("table", ["Test layer", "What it checks", "Result"],
     [["Unit suite", "35 component-level cases + interface invariant", "**Pass** — all cases, no regressions"],
      ["Behavioural", "7 end-to-end demo assertions", "**Pass** — all seven hold"],
      ["Model quality", "Precision / recall / F1 / false-positive rate", "**1.0 / 1.0 / 1.0 / 0.0** on reference data"],
      ["Scale", "100 GiB logical-corpus target & guardrails", "**Pass** — target reached, F1 gate held"]],
     [1.6, 3.2, 2.0]),
    ("callout", "The single most important sentence in this report",
     "The perfect scores below are PIPELINE evidence on a deterministic reference corpus — they prove the "
     "system is wired correctly end to end. They are NOT a claim of real-world detection accuracy, which "
     "must be earned on external datasets. We state this here, at the top, on purpose.",
     "warning"),

    ("h1", "What we tested, and why", "2"),
    ("p", "A security product earns trust by being tested the way it will be used — not just \"does the "
          "code run\", but \"does it make the right call, refuse the wrong call, and explain itself.\" Our "
          "verification is therefore layered, from the smallest part to the whole system:"),
    ("bullet", "**Components in isolation** — does each neuron and processor do its one job correctly, even "
               "in awkward edge cases (a flood of traffic, a forbidden system-call sequence, a quarantine "
               "that must expire)?"),
    ("bullet", "**The system as a whole** — when three realistic streams run together, does the demo raise "
               "the attack, calm the planned maintenance, surface the quiet leak, and never block anything?"),
    ("bullet", "**The trained model** — given windows of events it has never seen, does it tell attacks "
               "from benign activity, and are its confidence scores honest?"),
    ("bullet", "**The pipeline at scale** — does training stay correct, bounded, and reproducible when the "
               "logical corpus is pushed to 100 GiB?"),

    ("h1", "Test environment", "3"),
    ("table", ["Item", "Value"],
     [["Platform", "Jneopallium multi-module Maven project (master + worker)"],
      ["Runtime", "Java 17+ worker; Python 3 trainer (no third-party packages)"],
      ["Model under test", "cybersecurity-temporal-threat-correlator, 1.0.0-reference-temporal"],
      ["Feature count", "34 temporal-window features"],
      ["Decision threshold", "0.2 (auto-selected on validation)"],
      ["Safety mode", "ADVISORY"],
      ["Reference command", "train_temporal_model.py --reference-multiplier 1000 --target-corpus-bytes 100gb …"],
      ["Determinism", "Fully reproducible: same command → same checksum and metrics"]],
     [2.0, 4.8]),

    ("h1", "A plain-language primer on the metrics", "4"),
    ("p", "Before the numbers, here is what each one means — in everyday terms. Throughout, a \"window\" is "
          "a slice of time on one entity that the model judges as either *attack* or *benign*."),
    ("table", ["Metric", "Plain-language meaning", "Best"],
     [["Precision", "Of the windows it flagged as attacks, how many really were", "1.0 (no false alarms)"],
      ["Recall", "Of the real attacks, how many it caught", "1.0 (missed nothing)"],
      ["F1", "A single fair blend of precision and recall", "1.0"],
      ["Accuracy", "Of all windows, how many it judged correctly", "1.0"],
      ["False-positive rate", "How often it cried wolf on benign windows", "0.0"],
      ["Mean time to detection", "How long into an attack before it raised the alarm", "Low (near 0 ticks)"]],
     [1.9, 3.6, 1.3]),
    ("callout", "Why precision and recall both matter",
     "A tool that flags everything has perfect recall but useless precision (constant false alarms). A "
     "tool that flags nothing has perfect precision but zero recall (it misses every attack). F1 keeps "
     "both honest at once — that is why it is the gate we enforce.",
     "info"),

    ("h1", "Layer 1 — Unit tests (the SecurityModuleTest suite)", "5"),
    ("p", "The unit suite covers **35 cases** plus a structural invariant. Each case pins down one piece of "
          "behaviour so a future change cannot quietly break it. In plain terms, the suite proves that:"),
    ("bullet", "Ingestion is **rate-limited** — a flood cannot overwhelm the stages behind it."),
    ("bullet", "Signature matching fires on a **known-bad pattern**, and a **forbidden system-call "
               "sequence** is caught."),
    ("bullet", "The **tolerance filter** drops known-good patterns, and the **hard gate** always blocks "
               "hard-allow entities and protects critical assets below a safe score."),
    ("bullet", "The adaptive baseline **freezes during inflammation** (an attack window), so the attacker's "
               "behaviour is never absorbed as \"normal.\""),
    ("bullet", "**Beaconing** (regular phone-home) and **lateral-movement fan-out** (one account, many "
               "hosts) are detected."),
    ("bullet", "The **attack-memory lookup** and **incident timeline** bind evidence correctly, and the "
               "**Bayesian hypothesis update** combines signature and anomaly evidence."),
    ("bullet", "**Graduated-response bands** map a posterior to the right action level."),
    ("bullet", "**Quarantine auto-lifts** — a full apply → tick → lift cycle is exercised, proving "
               "containment is never permanent."),
    ("bullet", "The **alert-fatigue multiplier** rises with the false-positive rate, and the "
               "**exhaustion budget** bounds rule evaluation under load."),
    ("h3", "The interface invariant"),
    ("p", "A dedicated test, `processors_allInterfaceTyped`, asserts that **every** signal processor "
          "depends on an *interface*, never a concrete neuron class. This is what makes detectors swappable "
          "per deployment without touching the rest of the system — a structural quality, automatically "
          "enforced. The full worker suite passes with the security module added, with **no regressions**."),

    ("h1", "Layer 2 — Behavioural tests (the full-run demo)", "6"),
    ("p", "The full-run demo runs the three realistic streams through the real worker entry path and checks "
          "seven assertions. All seven hold:"),
    ("table", ["Assertion", "Plain meaning", "Result"],
     [["temporalAttackChainDetected", "The ordered attack chain was raised", "Pass"],
      ["maintenanceContextSuppressed", "Planned maintenance was calmed, not alarmed", "Pass"],
      ["lowAndSlowCorrelation", "The quiet data leak was surfaced", "Pass"],
      ["baselineFrozenDuringAttack", "Learning froze during the attack window", "Pass"],
      ["allTrainingSourcesReferenced", "Every training source family is represented", "Pass"],
      ["attackScoreGreaterThanBenign", "The attack scored higher than benign maintenance", "Pass"],
      ["advisoryOnly", "No active blocking action was ever emitted", "Pass"]],
     [2.6, 3.0, 1.0]),
    ("p", "Together these assertions prove the property that single-event tools cannot deliver: the system "
          "raises real attacks, calms benign context **without erasing its evidence**, catches the "
          "low-and-slow case, and stays strictly advisory throughout."),

    ("h1", "Layer 3 — Trained-model quality metrics", "7"),
    ("p", "The trained correlator was evaluated on held-out windows, split by campaign / host / time / "
          "attack type so the test measures genuinely unseen patterns. The results:"),
    ("table", ["Split", "Windows", "True pos.", "False pos.", "Precision", "Recall", "F1", "FP rate"],
     [["Training", "10,000", "7,000", "0", "1.0", "1.0", "1.0", "0.0"],
      ["Validation", "6,000", "4,000", "0", "1.0", "1.0", "1.0", "0.0"],
      ["Test", "5,000", "3,000", "0", "1.0", "1.0", "1.0", "0.0"],
      ["Overall", "21,000", "14,000", "0", "1.0", "1.0", "1.0", "0.0"]],
     [1.2, 0.95, 0.85, 0.85, 0.85, 0.75, 0.55, 0.7]),
    ("p", "Mean time to detection is effectively immediate: **0.126 ticks** on training, **0.0** on "
          "validation and test, **0.063** overall. In plain terms, when the model does detect an attack "
          "chain on this data, it does so at the first decisive evidence rather than after the damage is "
          "done."),
    ("callout", "Read these numbers correctly",
     "Zero false positives and perfect recall on a clean, deterministic reference corpus is exactly what a "
     "correctly wired pipeline should produce — the reference data is separable by design. This confirms "
     "the machinery; it does not forecast performance on noisy real-world telemetry. See Section 11.",
     "warning"),

    ("h1", "Calibration: are the probabilities honest?", "8"),
    ("p", "A confidence score is only useful if it means what it says — a window scored 0.9 should really be "
          "an attack about 90% of the time. This is called **calibration**. On the reference corpus the "
          "model's scores are sharply, correctly separated:"),
    ("table", ["Score band", "Windows", "Real attack rate", "Mean score"],
     [["0.0 – 0.1 (confident benign)", "7,000", "0% — none were attacks", "0.004"],
      ["0.9 – 1.0 (confident attack)", "14,000", "100% — all were attacks", "0.995"]],
     [2.4, 1.4, 1.6, 1.4]),
    ("p", "The model places benign windows near 0 and attack windows near 1, with almost nothing in the "
          "uncertain middle. Higher score reliably means higher real risk — the monotonic ordering the "
          "production gate requires."),

    ("h1", "What the model learned (feature evidence)", "9"),
    ("p", "Because the model is deliberately transparent, we can read *why* it decides as it does. The "
          "evidence it weighs most heavily tells a coherent, sensible story — it is not a black box.")
    ,
    ("h3", "Evidence that pushes toward \"attack\""),
    ("table", ["Feature", "Weight", "Plain meaning"],
     [["technique_command_and_control", "+0.43", "A machine appears to be phoning home to an attacker"],
      ["network_receptor_score", "+0.42", "Strong network-side evidence"],
      ["max_threat_intel", "+0.42", "A high-confidence external threat indicator"],
      ["slow_context_score", "+0.39", "Threat intel and asset criticality combined"],
      ["mean_evidence", "+0.39", "Consistently strong evidence across the window"]],
     [2.8, 0.9, 3.1]),
    ("h3", "Evidence that pushes toward \"benign\""),
    ("table", ["Feature", "Weight", "Plain meaning"],
     [["maintenance_ratio", "-0.45", "It happened during approved maintenance"],
      ["benign_context_ratio", "-0.39", "Mostly expected, benign context"],
      ["technique_unusual_login", "-0.27", "An isolated odd login with nothing following it"],
      ["source_diversity", "-0.18", "Evidence from one source only, not a coordinated chain"]],
     [2.8, 0.9, 3.1]),
    ("p", "Note how the strongest *calming* signal is approved maintenance — the model has learned the "
          "single most common cause of false alarms and weighs it down, exactly as a careful analyst "
          "would."),

    ("h1", "Layer 4 — Production-scale pipeline evidence", "10"),
    ("p", "The production-scale run records a 100 GiB *logical* corpus target by deterministically "
          "replicating the reference campaigns, without writing 100 GiB to disk. This stress-tests the "
          "scaling metadata and guardrails on an ordinary workstation."),
    ("table", ["Measure", "Value"],
     [["Fitted reference multiplier", "1,000"],
      ["Fitted events / windows", "96,000 / 21,000"],
      ["Estimated fitted corpus", "41.45 MiB"],
      ["Effective reference multiplier", "2,411,186"],
      ["Effective events", "231,473,856"],
      ["Effective temporal windows", "50,634,906"],
      ["Target corpus size", "100.00 GiB"],
      ["Target reach ratio", "0.99997 (99.997%)"],
      ["Training epochs", "2,400 (cap 4,096 windows/epoch)"]],
     [3.2, 3.6]),
    ("p", "Verification performed on this run: the trainer's Python compiled cleanly; the pipeline "
          "completed with test F1 above the 0.85 gate; the JSON artifacts validated the 100 GiB target, "
          "the fitted and effective counts, and the held-out false-positive rate; and — importantly — **no "
          "100 GiB file was written**, so the repository is never inflated by generated data."),
    ("p", "The run also produced and validated the **deployable JNeopallium network** the worker boots "
          "from: five layer files plus `model-descriptor.json` describing 5 layers, 8 real neurons, 34 "
          "trainable weights, and 1 bias, each neuron referencing a concrete runtime security class. The "
          "trained weights, decision threshold (0.2), sequence gates, and the baseline-freeze policy "
          "(freeze at posterior 0.3 or signature confidence 0.8) are embedded directly in the correlation "
          "layer — so **the artifact that was tested is the artifact that deploys**, with no translation "
          "step in between."),

    ("h1", "The honest limitations (read this carefully)", "11"),
    ("p", "We hold ourselves to the same standard we ask of the model: tell the truth about the evidence. "
          "Three limitations bound everything above."),
    ("num", "**The reference corpus is synthetic and deterministic.** It is built to be separable so the "
            "pipeline can be exercised and regression-tested repeatably. Perfect scores on it confirm "
            "correctness of the machinery, not real-world detection quality."),
    ("num", "**Real accuracy must be earned on external data.** Genuine production claims require LANL, "
            "ToN_IoT, OpTC, CIC/CSE-CIC, UNSW-NB15, and controlled CALDERA campaigns — with calibration "
            "curves, per-entity false-positive budgets, and streaming-memory/backpressure tests on "
            "representative enterprise telemetry."),
    ("num", "**Advisory-only by default.** Active enforcement (blocking, host isolation) is deliberately "
            "out of scope here and requires a separate safety case, approval workflow, and rollback path "
            "before it may be enabled."),
    ("callout", "Why we lead with the caveat instead of hiding it",
     "A security vendor that buries the difference between pipeline evidence and real-world accuracy is a "
     "vendor you cannot trust with your network. The honesty is the product feature: every advisory carries "
     "its evidence, and every claim in this report carries its scope.",
     "info"),

    ("h1", "Overall verdict", "12"),
    ("p", "**The cybersecurity module passes every test at every level on the reference corpus.** The "
          "components behave correctly and are structurally swappable; the running system makes the right "
          "calls on three realistic streams and stays strictly advisory; the trained model separates "
          "attack from benign with honest, well-calibrated confidence; and the training pipeline is "
          "correct, bounded, and reproducible at a 100 GiB logical scale."),
    ("p", "The system is **ready for the next stage**: external-dataset validation on representative "
          "telemetry, which is the only thing that can convert this strong pipeline evidence into a "
          "real-world accuracy claim. The architecture, the safety design, and the evidence discipline "
          "needed for that stage are already in place."),

    ("h1", "Glossary", "13"),
    ("table", ["Term", "Meaning"],
     [["Window", "A slice of time on one entity that the model judges as attack or benign"],
      ["True / false positive", "A correct / incorrect \"this is an attack\" judgement"],
      ["Precision / recall", "Share of flags that were right / share of real attacks that were caught"],
      ["F1", "A single fair blend of precision and recall"],
      ["Calibration", "Whether a confidence score means what it says"],
      ["Tick", "The platform's internal unit of time"],
      ["Posterior", "The model's probability that an attack is underway"],
      ["Regression", "A change that quietly breaks behaviour that used to work"],
      ["Deterministic", "Same input and command always produce the same output"]],
     [2.2, 4.6]),
    ("spacer", 8),
    ("pi", "Jneopallium Cybersecurity Module · Demo 06 · Complete Test Report. "
           "Results are pipeline evidence on a deterministic reference corpus, not a real-world accuracy "
           "claim. Safety mode: ADVISORY. License: BSD 3-Clause."),
]


_UK = [
    ("cover", "Jneopallium · Демо 06",
     "Повний звіт про тестування",
     "Часова кореляція кіберзагроз — результати, докази та чесне прочитання",
     [("Документ", "Звіт про верифікацію та тестування"),
      ("Продукт", "Модуль кібербезпеки Jneopallium (Демо 06)"),
      ("Модель", "cybersecurity-temporal-threat-correlator 1.0.0-reference-temporal"),
      ("Обсяг тестів", "Модульні, поведінкові, якість моделі, масштаб"),
      ("Результат", "Усі критерії пройдено (еталонний корпус)"),
      ("Автор", "Дмитро Раковський — Харків, Україна"),
      ("Дата", DATE_UK),
      ("Ліцензія", "BSD 3-Clause")],
     "Звіт про тестування",
     "Що тестували, як воно показало себе і — прямо й наперед — що саме результати доводять, а що ні. "
     "Написано так, щоб нефахівець міг упевнено прочитати кожне число."),

    ("toc", "Зміст",
     ["Стислий огляд результатів",
      "Що ми тестували і навіщо",
      "Тестове середовище",
      "Метрики простою мовою",
      "Рівень 1 — Модульні тести (набір SecurityModuleTest)",
      "Рівень 2 — Поведінкові тести (повне демо)",
      "Рівень 3 — Метрики якості навченої моделі",
      "Калібрування: чи чесні ймовірності?",
      "Чого навчилася модель (докази ознак)",
      "Рівень 4 — Докази конвеєра промислового масштабу",
      "Чесні обмеження (прочитайте уважно)",
      "Загальний вердикт",
      "Словник"]),

    ("h1", "Стислий огляд результатів", "1"),
    ("p", "Модуль кібербезпеки Демо 06 перевірено на чотирьох незалежних рівнях: **модульні тести** "
          "(окремі компоненти поводяться правильно), **поведінкові тести** (робоче демо ухвалює правильні "
          "рішення), **метрики якості моделі** (навчений корелятор відрізняє атаки від доброякісної "
          "активності) та **докази масштабу** (конвеєр навчання витримує логічну ціль корпусу 100 ГіБ). "
          "Усі критерії пройдено."),
    ("table", ["Рівень тестів", "Що перевіряє", "Результат"],
     [["Модульний набір", "35 кейсів рівня компонентів + інваріант інтерфейсів", "**Пройдено** — усі кейси, без регресій"],
      ["Поведінковий", "7 наскрізних тверджень демо", "**Пройдено** — усі сім виконуються"],
      ["Якість моделі", "Точність / повнота / F1 / рівень хибних спрацювань", "**1.0 / 1.0 / 1.0 / 0.0** на еталонних даних"],
      ["Масштаб", "Логічна ціль 100 ГіБ і запобіжники", "**Пройдено** — ціль досягнуто, критерій F1 утримано"]],
     [1.7, 3.1, 2.0]),
    ("callout", "Найважливіше речення цього звіту",
     "Ідеальні оцінки нижче — це докази КОНВЕЄРА на детермінованому еталонному корпусі: вони доводять, що "
     "система правильно з'єднана наскрізно. Це НЕ твердження про реальну точність виявлення, яку треба "
     "заслужити на зовнішніх наборах даних. Ми зазначаємо це тут, на початку, навмисно.",
     "warning"),

    ("h1", "Що ми тестували і навіщо", "2"),
    ("p", "Продукт безпеки заслуговує довіру, коли його тестують так, як він використовуватиметься — не "
          "просто «чи запускається код», а «чи ухвалює він правильне рішення, чи відмовляє в "
          "неправильному й чи може пояснити себе». Тому наша верифікація багаторівнева — від найменшої "
          "частини до всієї системи:"),
    ("bullet", "**Компоненти окремо** — чи кожен нейрон і процесор виконує свою єдину роботу правильно "
               "навіть у незручних граничних випадках (потік трафіку, заборонена послідовність системних "
               "викликів, карантин, що має завершитися)?"),
    ("bullet", "**Система в цілому** — коли три реалістичні потоки працюють разом, чи піднімає демо атаку, "
               "заспокоює планове обслуговування, виявляє тихий витік і ніколи нічого не блокує?"),
    ("bullet", "**Навчена модель** — на вікнах подій, яких вона ніколи не бачила, чи відрізняє вона атаки "
               "від доброякісної активності і чи чесні її оцінки впевненості?"),
    ("bullet", "**Конвеєр у масштабі** — чи лишається навчання правильним, обмеженим і відтворюваним, коли "
               "логічний корпус доведено до 100 ГіБ?"),

    ("h1", "Тестове середовище", "3"),
    ("table", ["Пункт", "Значення"],
     [["Платформа", "Багатомодульний проєкт Maven Jneopallium (master + worker)"],
      ["Середовище виконання", "Worker на Java 17+; тренер на Python 3 (без сторонніх пакетів)"],
      ["Модель під тестом", "cybersecurity-temporal-threat-correlator, 1.0.0-reference-temporal"],
      ["Кількість ознак", "34 ознаки часового вікна"],
      ["Поріг рішення", "0.2 (автодобір на валідації)"],
      ["Режим безпеки", "ADVISORY"],
      ["Еталонна команда", "train_temporal_model.py --reference-multiplier 1000 --target-corpus-bytes 100gb …"],
      ["Детермінізм", "Повністю відтворювано: та сама команда → та сама контрольна сума й метрики"]],
     [2.0, 4.8]),

    ("h1", "Метрики простою мовою", "4"),
    ("p", "Перед числами — що означає кожне з них повсякденними словами. Усюди «вікно» — це відрізок часу "
          "на одній сутності, який модель оцінює як *атаку* або *доброякісність*."),
    ("table", ["Метрика", "Значення простою мовою", "Найкраще"],
     [["Точність (precision)", "Зі сповіщених як атаки вікон скільки справді були атаками", "1.0 (без хибних тривог)"],
      ["Повнота (recall)", "Зі справжніх атак скільки вона зловила", "1.0 (нічого не пропущено)"],
      ["F1", "Єдина справедлива суміш точності й повноти", "1.0"],
      ["Влучність (accuracy)", "З усіх вікон скільки оцінено правильно", "1.0"],
      ["Рівень хибних спрацювань", "Як часто вона «кричала вовк» на доброякісних вікнах", "0.0"],
      ["Сер. час до виявлення", "Скільки триває атака до підняття тривоги", "Низький (близько 0 тактів)"]],
     [1.9, 3.6, 1.3]),
    ("callout", "Чому важливі і точність, і повнота",
     "Інструмент, що позначає все, має ідеальну повноту, але марну точність (постійні хибні тривоги). "
     "Інструмент, що не позначає нічого, має ідеальну точність, але нульову повноту (пропускає кожну "
     "атаку). F1 тримає обидві чесними водночас — тому це критерій, який ми застосовуємо.",
     "info"),

    ("h1", "Рівень 1 — Модульні тести (набір SecurityModuleTest)", "5"),
    ("p", "Модульний набір охоплює **35 кейсів** плюс структурний інваріант. Кожен кейс фіксує одну рису "
          "поведінки, тож майбутня зміна не зможе тихо її зламати. Простими словами, набір доводить, що:"),
    ("bullet", "Прийом **обмежений за швидкістю** — потік не може перевантажити етапи позаду."),
    ("bullet", "Сигнатурне зіставлення спрацьовує на **відомо-поганому шаблоні**, а **заборонену "
               "послідовність системних викликів** ловлять."),
    ("bullet", "**Фільтр толерантності** відкидає відомо-добрі шаблони, а **жорсткий запобіжник** завжди "
               "блокує сутності з жорсткого білого списку й захищає критичні активи нижче безпечної оцінки."),
    ("bullet", "Адаптивна базова лінія **заморожується під час запалення** (вікна атаки), тож поведінка "
               "зловмисника ніколи не вбирається як «норма»."),
    ("bullet", "**Маяки** (регулярний «дзвінок додому») і **розгалуження бічного руху** (один запис, багато "
               "вузлів) виявляються."),
    ("bullet", "**Пошук у пам'яті про атаки** та **хронологія інциденту** правильно прив'язують докази, а "
               "**байєсове оновлення гіпотези** поєднує сигнатурні докази й докази аномалій."),
    ("bullet", "**Смуги градуйованої відповіді** відображають апостеріорну на правильний рівень дії."),
    ("bullet", "**Карантин автоматично знімається** — повний цикл застосувати → такт → зняти перевіряється, "
               "що доводить: стримування ніколи не постійне."),
    ("bullet", "**Множник втоми від сповіщень** зростає з рівнем хибних спрацювань, а **бюджет виснаження** "
               "обмежує оцінку правил під навантаженням."),
    ("h3", "Інваріант інтерфейсів"),
    ("p", "Окремий тест `processors_allInterfaceTyped` стверджує, що **кожен** процесор сигналів залежить "
          "від *інтерфейсу*, а не від конкретного класу нейрона. Саме це робить детектори замінними для "
          "кожного розгортання, не торкаючись решти системи — структурна якість, що забезпечується "
          "автоматично. Повний набір тестів worker проходить із доданим модулем безпеки **без регресій**."),

    ("h1", "Рівень 2 — Поведінкові тести (повне демо)", "6"),
    ("p", "Повне демо проганяє три реалістичні потоки через справжній вхідний шлях worker і перевіряє сім "
          "тверджень. Усі сім виконуються:"),
    ("table", ["Твердження", "Простий зміст", "Результат"],
     [["temporalAttackChainDetected", "Впорядкований ланцюг атаки піднято", "Пройдено"],
      ["maintenanceContextSuppressed", "Планове обслуговування заспокоєне, не тривога", "Пройдено"],
      ["lowAndSlowCorrelation", "Тихий витік даних виявлено", "Пройдено"],
      ["baselineFrozenDuringAttack", "Навчання заморожене під час вікна атаки", "Пройдено"],
      ["allTrainingSourcesReferenced", "Кожна родина джерел навчання представлена", "Пройдено"],
      ["attackScoreGreaterThanBenign", "Атака набрала більше за доброякісне обслуговування", "Пройдено"],
      ["advisoryOnly", "Жодної активної блокувальної дії не видано", "Пройдено"]],
     [2.7, 3.0, 1.0]),
    ("p", "Разом ці твердження доводять властивість, якої не можуть дати інструменти «одна подія»: система "
          "піднімає справжні атаки, заспокоює доброякісний контекст **без стирання доказів**, ловить "
          "випадок «повільно й тихо» й лишається суворо рекомендаційною впродовж усього часу."),

    ("h1", "Рівень 3 — Метрики якості навченої моделі", "7"),
    ("p", "Навчений корелятор оцінено на відкладених вікнах, розбитих за кампанією / вузлом / часом / типом "
          "атаки, тож тест вимірює справді небачені шаблони. Результати:"),
    ("table", ["Розбиття", "Вікна", "Іст. поз.", "Хиб. поз.", "Точність", "Повнота", "F1", "Хиб. рівень"],
     [["Навчання", "10 000", "7 000", "0", "1.0", "1.0", "1.0", "0.0"],
      ["Валідація", "6 000", "4 000", "0", "1.0", "1.0", "1.0", "0.0"],
      ["Тест", "5 000", "3 000", "0", "1.0", "1.0", "1.0", "0.0"],
      ["Загалом", "21 000", "14 000", "0", "1.0", "1.0", "1.0", "0.0"]],
     [1.2, 0.95, 0.85, 0.85, 0.85, 0.8, 0.55, 0.75]),
    ("p", "Середній час до виявлення фактично миттєвий: **0.126 такту** на навчанні, **0.0** на валідації "
          "й тесті, **0.063** загалом. Простими словами: коли модель виявляє ланцюг атаки на цих даних, "
          "вона робить це на першому вирішальному доказі, а не після завданої шкоди."),
    ("callout", "Читайте ці числа правильно",
     "Нуль хибних спрацювань і ідеальна повнота на чистому детермінованому еталонному корпусі — саме те, "
     "що має давати правильно з'єднаний конвеєр: еталонні дані роздільні за задумом. Це підтверджує "
     "механізм; це не прогнозує якість на шумній реальній телеметрії. Див. розділ 11.",
     "warning"),

    ("h1", "Калібрування: чи чесні ймовірності?", "8"),
    ("p", "Оцінка впевненості корисна, лише якщо означає те, що каже — вікно з оцінкою 0.9 справді має бути "
          "атакою приблизно у 90% випадків. Це називають **калібруванням**. На еталонному корпусі оцінки "
          "моделі чітко й правильно розділені:"),
    ("table", ["Смуга оцінок", "Вікна", "Реальний рівень атак", "Сер. оцінка"],
     [["0.0 – 0.1 (упевнено доброякісні)", "7 000", "0% — жодне не було атакою", "0.004"],
      ["0.9 – 1.0 (упевнено атака)", "14 000", "100% — усі були атаками", "0.995"]],
     [2.5, 1.3, 1.6, 1.4]),
    ("p", "Модель розміщує доброякісні вікна біля 0, а атакувальні — біля 1, майже нічого не лишаючи в "
          "невизначеній середині. Вища оцінка надійно означає вищий реальний ризик — монотонне "
          "впорядкування, якого вимагає промисловий критерій."),

    ("h1", "Чого навчилася модель (докази ознак)", "9"),
    ("p", "Оскільки модель навмисно прозора, ми можемо прочитати, *чому* вона вирішує так, як вирішує. "
          "Докази, які вона зважує найбільше, розповідають узгоджену, розумну історію — це не чорна скриня."),
    ("h3", "Докази, що схиляють до «атаки»"),
    ("table", ["Ознака", "Вага", "Простий зміст"],
     [["technique_command_and_control", "+0.43", "Машина, схоже, «телефонує» зловмиснику"],
      ["network_receptor_score", "+0.42", "Сильні докази з боку мережі"],
      ["max_threat_intel", "+0.42", "Зовнішній індикатор загрози високої впевненості"],
      ["slow_context_score", "+0.39", "Кіберрозвідка й критичність активу разом"],
      ["mean_evidence", "+0.39", "Стабільно сильні докази по всьому вікну"]],
     [2.8, 0.9, 3.1]),
    ("h3", "Докази, що схиляють до «доброякісності»"),
    ("table", ["Ознака", "Вага", "Простий зміст"],
     [["maintenance_ratio", "-0.45", "Сталося під час погодженого обслуговування"],
      ["benign_context_ratio", "-0.39", "Здебільшого очікуваний, доброякісний контекст"],
      ["technique_unusual_login", "-0.27", "Поодинокий дивний вхід, за яким нічого не йде"],
      ["source_diversity", "-0.18", "Докази лише з одного джерела, не злагоджений ланцюг"]],
     [2.8, 0.9, 3.1]),
    ("p", "Зверніть увагу, як найсильніший *заспокійливий* сигнал — це погоджене обслуговування: модель "
          "вивчила найпоширенішу причину хибних тривог і зважує її донизу, точно як зробив би уважний "
          "аналітик."),

    ("h1", "Рівень 4 — Докази конвеєра промислового масштабу", "10"),
    ("p", "Запуск промислового масштабу фіксує *логічну* ціль корпусу 100 ГіБ, детерміновано реплікуючи "
          "еталонні кампанії, без запису 100 ГіБ на диск. Це навантажувальне випробування метаданих "
          "масштабування й запобіжників на звичайній робочій станції."),
    ("table", ["Показник", "Значення"],
     [["Припасований множник еталона", "1 000"],
      ["Припасовані події / вікна", "96 000 / 21 000"],
      ["Оцінений припасований корпус", "41.45 МіБ"],
      ["Ефективний множник еталона", "2 411 186"],
      ["Ефективні події", "231 473 856"],
      ["Ефективні часові вікна", "50 634 906"],
      ["Цільовий розмір корпусу", "100.00 ГіБ"],
      ["Коефіцієнт досягнення цілі", "0.99997 (99.997%)"],
      ["Епохи навчання", "2 400 (ліміт 4 096 вікон/епоху)"]],
     [3.2, 3.6]),
    ("p", "Виконана верифікація цього запуску: Python тренера компілюється без помилок; конвеєр завершився "
          "з тестовим F1 вище критерію 0.85; артефакти JSON підтвердили ціль 100 ГіБ, припасовані й "
          "ефективні кількості та відкладений рівень хибних спрацювань; і — важливо — **жодного файлу на "
          "100 ГіБ не записано**, тож репозиторій ніколи не роздувається згенерованими даними."),
    ("p", "Запуск також створив і перевірив **готову до розгортання мережу JNeopallium**, з якої "
          "завантажується worker: п'ять файлів рівнів плюс `model-descriptor.json`, що описує 5 рівнів, "
          "8 справжніх нейронів, 34 навчувані ваги та 1 зсув, де кожен нейрон посилається на конкретний "
          "клас безпеки часу виконання. Навчені ваги, поріг рішення (0.2), гейти послідовності та політика "
          "заморожування базової лінії (заморозити при апостеріорній 0.3 чи впевненості сигнатури 0.8) "
          "вбудовані прямо в рівень кореляції — тож **протестований артефакт — це той самий артефакт, що "
          "розгортається**, без кроку перекладу між ними."),

    ("h1", "Чесні обмеження (прочитайте уважно)", "11"),
    ("p", "Ми тримаємо себе того самого стандарту, якого вимагаємо від моделі: казати правду про докази. "
          "Три обмеження окреслюють усе вище."),
    ("num", "**Еталонний корпус синтетичний і детермінований.** Його побудовано роздільним, щоб конвеєр "
            "можна було вправляти й відтворювано тестувати на регресії. Ідеальні оцінки на ньому "
            "підтверджують правильність механізму, а не реальну якість виявлення."),
    ("num", "**Реальну точність треба заслужити на зовнішніх даних.** Справжні промислові твердження "
            "потребують LANL, ToN_IoT, OpTC, CIC/CSE-CIC, UNSW-NB15 і контрольованих кампаній CALDERA — з "
            "кривими калібрування, бюджетами хибних спрацювань на сутність і тестами потокової "
            "пам'яті/протитиску на репрезентативній корпоративній телеметрії."),
    ("num", "**Лише рекомендації за замовчуванням.** Активне примусове виконання (блокування, ізоляція "
            "вузлів) тут навмисно поза обсягом і потребує окремого обґрунтування безпеки, процесу "
            "погодження та шляху відкату, перш ніж його можна ввімкнути."),
    ("callout", "Чому ми починаємо із застереження, а не ховаємо його",
     "Постачальник безпеки, який ховає різницю між доказами конвеєра й реальною точністю, — це "
     "постачальник, якому не можна довірити вашу мережу. Чесність — це і є перевага продукту: кожна "
     "рекомендація несе свої докази, а кожне твердження цього звіту несе свій обсяг.",
     "info"),

    ("h1", "Загальний вердикт", "12"),
    ("p", "**Модуль кібербезпеки проходить кожен тест на кожному рівні на еталонному корпусі.** Компоненти "
          "поводяться правильно й структурно замінні; робоча система ухвалює правильні рішення на трьох "
          "реалістичних потоках і лишається суворо рекомендаційною; навчена модель відрізняє атаку від "
          "доброякісності з чесною, добре каліброваною впевненістю; а конвеєр навчання правильний, "
          "обмежений і відтворюваний у логічному масштабі 100 ГіБ."),
    ("p", "Система **готова до наступного етапу**: валідації на зовнішніх наборах даних на репрезентативній "
          "телеметрії — єдиного, що може перетворити ці сильні докази конвеєра на твердження про реальну "
          "точність. Архітектура, конструкція безпеки та дисципліна доказів, потрібні для цього етапу, вже "
          "на місці."),

    ("h1", "Словник", "13"),
    ("table", ["Термін", "Значення"],
     [["Вікно", "Відрізок часу на одній сутності, який модель оцінює як атаку чи доброякісність"],
      ["Істинне / хибне спрацювання", "Правильне / неправильне судження «це атака»"],
      ["Точність / повнота", "Частка правильних позначок / частка зловлених справжніх атак"],
      ["F1", "Єдина справедлива суміш точності й повноти"],
      ["Калібрування", "Чи означає оцінка впевненості те, що каже"],
      ["Такт (tick)", "Внутрішня одиниця часу платформи"],
      ["Апостеріорна", "Ймовірність атаки за моделлю"],
      ["Регресія", "Зміна, що тихо ламає поведінку, яка раніше працювала"],
      ["Детермінований", "Той самий вхід і команда завжди дають той самий результат"]],
     [2.2, 4.6]),
    ("spacer", 8),
    ("pi", "Модуль кібербезпеки Jneopallium · Демо 06 · Повний звіт про тестування. "
           "Результати — це докази конвеєра на детермінованому еталонному корпусі, а не твердження про "
           "реальну точність. Режим безпеки: ADVISORY. Ліцензія: BSD 3-Clause."),
]
