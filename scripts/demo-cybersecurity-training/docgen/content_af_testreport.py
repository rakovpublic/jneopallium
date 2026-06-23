# -*- coding: utf-8 -*-
"""Ad-Fraud Guardian — test report (EN + UK), block DSL."""

from __future__ import annotations

DATE = "23 June 2026"
DATE_UK = "23 червня 2026"


def testreport(lang: str) -> list:
    return _EN if lang == "en" else _UK


_EN = [
    ("cover", "Jneopallium · Ad-Fraud Guardian",
     "Complete Test Report",
     "Multi-label invalid-traffic detection — results, the upgrade that lifted them, and an honest reading",
     [("Document", "Verification & Test Report"),
      ("Product", "Jneopallium Ad-Fraud Guardian (advertising-fraud module)"),
      ("Model", "advertising-fraud 1.0.0-reference-advisory"),
      ("Headline", "macro-F1 0.66 → 0.97 after a deliberate model upgrade"),
      ("Result", "All checks passed (reference corpus); 12/12 Java tests"),
      ("Author", "Dmytro Rakovskyi — Kharkiv, Ukraine"),
      ("Date", DATE),
      ("License", "BSD 3-Clause")],
     "Test Report",
     "What was tested, how it performed, what a recent upgrade changed, and — stated plainly and up front "
     "— exactly what the results do and do not prove. Written so a non-specialist can read every number "
     "with confidence."),

    ("toc", "Contents",
     ["Executive summary of results",
      "What we tested, and why",
      "Test environment",
      "A plain-language primer on the metrics",
      "Model quality — per-label, before and after",
      "The upgrade that lifted the score",
      "Calibration — are the probabilities honest?",
      "The deterministic workflow and the leakage audit",
      "The deployable network and the Java test suite",
      "The honest limitations (read this carefully)",
      "Overall verdict and readiness",
      "Glossary"]),

    ("h1", "Executive summary of results", "1"),
    ("p", "The Ad-Fraud Guardian was verified at four independent levels: **model quality** (how well it "
          "separates each fraud family), **calibration** (whether its probabilities are honest), "
          "**workflow integrity** (leakage-safe, reproducible), and **runtime** (the deployable network "
          "loads and the Java suite passes). Every check passed, and a recent, deliberate upgrade lifted "
          "the headline number sharply."),
    ("table", ["Test layer", "What it checks", "Result"],
     [["Model quality", "Per-label precision / recall / F1, macro-F1", "**macro-F1 ≈ 0.97** (was 0.66)"],
      ["Calibration", "Whether a 0.9 score means ~90% likely", "**Real Platt scaling** per label (was identity)"],
      ["Workflow", "Leakage-safe split, reproducibility", "**Pass** — leakage audit clean, deterministic"],
      ["Runtime", "Bundle loads, Java tests", "**Pass** — 12/12 Java tests green"]],
     [1.7, 3.1, 2.0]),
    ("callout", "The single most important sentence in this report",
     "The near-perfect scores are on a deterministic simulator plus weak public-source metadata, evaluated "
     "leakage-safe. They prove the pipeline and the label separation — they are NOT a claim of real-world "
     "accuracy, which must be earned on first-party labelled production traffic. The model stays "
     "SHADOW/ADVISORY until then. We state this here, at the top, on purpose.", "warning"),

    ("h1", "What we tested, and why", "2"),
    ("p", "An invalid-traffic product earns trust by being tested the way it will be used — not just "
          "\"does it run\", but \"does it name the right fraud, calibrate its confidence, and refuse to "
          "over-accuse.\" The verification is therefore layered:"),
    ("bullet", "**Each fraud family** — given held-out events, does each of the ten labels separate its "
               "fraud type from everything else, and is precision high enough to act on?"),
    ("bullet", "**The probabilities** — does a 0.9 really mean roughly 90% likely, so analysts and "
               "thresholds can trust them?"),
    ("bullet", "**The workflow** — is the split leakage-safe (no identifier crosses train/test) and the "
               "run reproducible for a fixed seed?"),
    ("bullet", "**The runtime** — does the exported network load with verified checksums, and does the "
               "Java test suite pass?"),

    ("h1", "Test environment", "3"),
    ("table", ["Item", "Value"],
     [["Platform", "Jneopallium multi-module Maven project (master + worker)"],
      ["Runtime", "Java 17+ runtime scorer; Python 3 workflow (no third-party packages)"],
      ["Model under test", "advertising-fraud, 1.0.0-reference-advisory"],
      ["Task", "Multi-label: ten independent invalid-traffic labels"],
      ["Network", "8 layers, 22 neurons, 21 features (8 base + 5 evidence + 8 interaction)"],
      ["Safety mode", "ADVISORY / SHADOW; AUTOMATED_ACTION_READY = false"],
      ["Reference command", "python scripts/demo-ad-fraud/run_all.py --offline"],
      ["Determinism", "Reproducible for a fixed seed: same command → same metrics"]],
     [2.0, 4.8]),

    ("h1", "A plain-language primer on the metrics", "4"),
    ("p", "An \"example\" is a single ad event the model labels. The classification metrics:"),
    ("table", ["Metric", "Plain-language meaning", "Best"],
     [["Precision", "Of the events flagged with a label, how many really had it", "1.0 (no false alarms)"],
      ["Recall", "Of the real cases of a label, how many it caught", "1.0 (missed nothing)"],
      ["F1", "A single fair blend of precision and recall", "1.0"],
      ["Macro-F1", "The F1 averaged equally across all ten labels", "1.0"],
      ["PR / ROC-AUC", "How well the score *ranks* fraud above non-fraud", "1.0"],
      ["Calibration error", "How far a 0.9 score is from a true 90%", "0.0"]],
     [1.7, 3.7, 1.4]),
    ("callout", "Ranking vs. threshold — why both matter",
     "A head can rank perfectly (AUC 1.0) yet score a poor F1 if its decision threshold is wrong. The "
     "earlier model failed exactly this way on two labels; the upgrade fixed the thresholds and "
     "calibration, and added the evidence the conflated labels were missing.", "info"),

    ("h1", "Model quality — per-label, before and after", "5"),
    ("p", "Each label was evaluated on a held-out, leakage-safe split (including an adversarial holdout). "
          "A recent upgrade improved or held every label; the macro-F1 rose from **0.66 to 0.97**:"),
    ("table", ["Fraud label", "F1 before", "F1 after", "Precision after", "Recall after"],
     [["bot", "1.00", "1.00", "1.00", "1.00"],
      ["eventSpoofing", "1.00", "1.00", "1.00", "1.00"],
      ["attributionHijack", "1.00", "1.00", "1.00", "1.00"],
      ["unknownSuspicious", "1.00", "1.00", "1.00", "1.00"],
      ["clickInjection", "0.25", "**1.00**", "1.00", "1.00"],
      ["inventorySpoofing", "0.25", "**1.00**", "1.00", "1.00"],
      ["incentivized", "0.67", "**1.00**", "1.00", "1.00"],
      ["clickFarm", "0.67", "**1.00**", "1.00", "1.00"],
      ["clickSpam", "0.29", "**0.98**", "0.95", "1.00"],
      ["accidentalOrLowValue", "0.44", "0.69", "0.52", "1.00"]],
     [2.3, 1.1, 1.1, 1.2, 1.1]),
    ("p", "**macro-F1: 0.656 → 0.966.** Every previously-broken head is fixed. The one remaining soft spot, "
          "`accidentalOrLowValue` (0.69), is deliberately a fuzzy *union* label — accidental clicks, "
          "low-conversion traffic, and incentivized installs are all folded into it — so it is intrinsically "
          "harder, and pushing it higher needs first-party labels rather than more architecture."),

    ("h1", "The upgrade that lifted the score", "6"),
    ("p", "The jump came from a diagnosis, not guesswork. The earlier model failed in two distinct ways, "
          "each fixed by a specific change:"),
    ("h3", "Threshold / calibration failures (cheap to fix)"),
    ("p", "`clickInjection` and `inventorySpoofing` ranked perfectly (AUC 1.0) but scored F1 0.25 because "
          "their decision threshold flagged *everything*, and calibration was a no-op. Fixing per-label "
          "thresholds, adding real Platt calibration, and giving those adversarial families "
          "in-distribution training coverage lifted both to 1.0."),
    ("h3", "Conflated labels (needed new model capacity)"),
    ("p", "`clickSpam` shared an identical head with `attributionHijack`, and `incentivized` with "
          "`clickFarm` — the 8 base features could not tell the look-alikes apart. Two new layers fixed "
          "this: a **behavioural-evidence layer** (single-purpose neurons emitting click-volume, "
          "conversion-timing, incentive, and low-value features) and a **non-linear feature-interaction "
          "layer**. Combined with class-weighted, L2-regularized logistic heads, the conflated labels "
          "separated."),
    ("table", ["Improvement", "Effect"],
     [["Per-label cost-aware thresholds", "Fixed clickInjection / inventorySpoofing (0.25 → 1.0)"],
      ["Real Platt calibration (was identity)", "Honest probabilities; lower calibration error"],
      ["Fitted, class-weighted logistic heads", "Replaced a one-shot mean-difference rule"],
      ["+5 behavioural-evidence features + a new layer", "De-conflated clickSpam, incentivized, low-value"],
      ["+8 non-linear interaction units (a new layer)", "Captured AND/OR evidence combinations"],
      ["Larger, balanced corpus", "Stable fitting and calibration for every label"]],
     [3.3, 3.5]),

    ("h1", "Calibration — are the probabilities honest?", "7"),
    ("p", "A confidence score is only useful if it means what it says. The earlier model applied **no** "
          "calibration (it claimed to test Platt / isotonic / temperature but applied identity). The "
          "current model fits **real per-label Platt scaling** on a held-out calibration split, so a 0.9 "
          "genuinely means roughly 90% likely. That is what makes the response bands — and any future "
          "review-capacity threshold — trustworthy."),

    ("h1", "The deterministic workflow and the leakage audit", "8"),
    ("p", "Quality numbers are only meaningful if the evaluation is honest. The workflow enforces this:"),
    ("bullet", "**Leakage-safe split** — by scenario, campaign episode, and deterministic replicate block, "
               "with an adversarial holdout. The audit **fails the run** if any device / click / campaign "
               "identifier crosses split boundaries."),
    ("bullet", "**Reproducible** — for a fixed seed, the same command produces the same corpus, the same "
               "metrics, and the same exported bundle."),
    ("bullet", "**Privacy-checked** — identifiers are HMAC-pseudonymised; raw IPs, precise geo, and "
               "user-agents are never exported; missing is distinct from zero."),

    ("h1", "The deployable network and the Java test suite", "9"),
    ("p", "The run also produced and validated the **deployable JNeopallium network**: eight layer files "
          "plus `model-descriptor.json` describing 8 layers, 22 real neurons, and 21 features, each neuron "
          "referencing a concrete runtime class, with the calibrated heads and the non-linear hidden layer "
          "embedded. The Java runtime loads this bundle, verifies its checksums, and evaluates the hidden "
          "layer — so **the artifact that was tested is the artifact that deploys**."),
    ("p", "The Java test suite — **12 tests** covering event-type coverage, signal cadences, the "
          "deterministic integrity rules, the baseline-freeze and graph-TTL behaviour, the advisory-only "
          "response gate, bundle loading with checksum verification, the descriptor / layer structure, the "
          "interface-typed processors, and the HTTP scoring service — **passes 12 / 12**."),

    ("h1", "The honest limitations (read this carefully)", "10"),
    ("num", "**The reference corpus is synthetic plus weak labels.** A deterministic simulator supplies "
            "the fraud classes that lack public labels, augmented with weak public-source metadata. "
            "Near-perfect scores confirm the pipeline and label separation — not real-world accuracy."),
    ("num", "**Real accuracy must be earned on first-party labels.** Genuine production claims require "
            "confirmed chargebacks, MMP fraud rulings, and analyst verdicts, with forward-time and "
            "unseen-publisher validation, false-positive financial-cost limits, and a documented data-"
            "handling review."),
    ("num", "**Advisory-only, and never an accusation.** The model emits invalid-traffic evidence and "
            "candidate actions; it never blocks, withholds money, or asserts that a named person committed "
            "fraud. `AUTOMATED_ACTION_READY` stays false until validation, legal review, and appeal/"
            "rollback exist."),
    ("callout", "Why we lead with the caveat",
     "A fraud product that blurs the line between pipeline evidence and field accuracy cannot be trusted "
     "near a payout decision. The honesty is part of the product: every advisory carries its evidence, and "
     "every claim in this report carries its scope.", "info"),

    ("h1", "Overall verdict and readiness", "11"),
    ("p", "**The Ad-Fraud Guardian passes every test at every level on the reference corpus, and a recent "
          "upgrade lifted macro-F1 from 0.66 to 0.97.** The ten labels separate with honest, well-"
          "calibrated probabilities; the workflow is leakage-safe and reproducible; the deployable network "
          "loads and the Java suite is green; and the safety ceiling (advisory, no accusation) is preserved "
          "throughout."),
    ("p", "Readiness: **ENGINEERING_READY, SHADOW_READY, ADVISORY_READY = true; AUTOMATED_ACTION_READY = "
          "false.** The system is ready for the next stage — offline replay and shadow validation on real "
          "first-party traffic, which is the only thing that can convert this strong pipeline evidence into "
          "a field-accuracy claim."),

    ("h1", "Glossary", "12"),
    ("table", ["Term", "Meaning"],
     [["Multi-label", "Several independent fraud labels can be true for one event"],
      ["Precision / recall", "Share of flags that were right / share of real cases caught"],
      ["Macro-F1", "Detection quality averaged equally across the ten labels"],
      ["AUC", "How well the score ranks fraud above non-fraud, independent of threshold"],
      ["Calibration", "Whether a confidence score means what it says"],
      ["Platt scaling", "A standard way to turn raw scores into honest probabilities"],
      ["Leakage", "When near-duplicate data leaks across train/test and inflates scores"],
      ["Adversarial holdout", "Attack types kept fully unseen until evaluation"],
      ["Advisory / shadow", "The system recommends; it never blocks or withholds on its own"]],
     [2.2, 4.6]),
    ("spacer", 8),
    ("pi", "Jneopallium Ad-Fraud Guardian · Complete Test Report. Results are pipeline evidence on a "
           "deterministic reference corpus plus weak labels — not a real-world accuracy claim. Safety mode: "
           "ADVISORY / SHADOW. License: BSD 3-Clause."),
]


_UK = [
    ("cover", "Jneopallium · Ad-Fraud Guardian",
     "Повний звіт про тестування",
     "Багатоміткове виявлення невалідного трафіку — результати, оновлення, що їх підняло, і чесне прочитання",
     [("Документ", "Звіт про верифікацію та тестування"),
      ("Продукт", "Jneopallium Ad-Fraud Guardian (модуль advertising-fraud)"),
      ("Модель", "advertising-fraud 1.0.0-reference-advisory"),
      ("Заголовок", "macro-F1 0.66 → 0.97 після навмисного оновлення моделі"),
      ("Результат", "Усі перевірки пройдено (еталонний корпус); 12/12 тестів Java"),
      ("Автор", "Дмитро Раковський — Харків, Україна"),
      ("Дата", DATE_UK),
      ("Ліцензія", "BSD 3-Clause")],
     "Звіт про тестування",
     "Що тестували, як воно показало себе, що змінило нещодавнє оновлення і — прямо й наперед — що саме "
     "результати доводять, а що ні. Написано так, щоб нефахівець міг упевнено прочитати кожне число."),

    ("toc", "Зміст",
     ["Стислий огляд результатів",
      "Що ми тестували і навіщо",
      "Тестове середовище",
      "Метрики простою мовою",
      "Якість моделі — на мітку, до і після",
      "Оновлення, що підняло оцінку",
      "Калібрування — чи чесні ймовірності?",
      "Детермінований процес і аудит витоку",
      "Готова до розгортання мережа і набір тестів Java",
      "Чесні обмеження (прочитайте уважно)",
      "Загальний вердикт і готовність",
      "Словник"]),

    ("h1", "Стислий огляд результатів", "1"),
    ("p", "Ad-Fraud Guardian перевірено на чотирьох незалежних рівнях: **якість моделі** (наскільки добре "
          "вона розділяє кожну родину фроду), **калібрування** (чи чесні її ймовірності), **цілісність "
          "процесу** (без витоку, відтворювано) та **середовище виконання** (готова мережа завантажується, "
          "набір Java проходить). Усі перевірки пройдено, а нещодавнє навмисне оновлення різко підняло "
          "головне число."),
    ("table", ["Рівень тестів", "Що перевіряє", "Результат"],
     [["Якість моделі", "Точність / повнота / F1 на мітку, macro-F1", "**macro-F1 ≈ 0.97** (було 0.66)"],
      ["Калібрування", "Чи означає бал 0.9 ~90% імовірності", "**Справжнє Platt** на мітку (було тотожність)"],
      ["Процес", "Розбиття без витоку, відтворюваність", "**Пройдено** — аудит чистий, детерміновано"],
      ["Виконання", "Пакет завантажується, тести Java", "**Пройдено** — 12/12 тестів Java зелені"]],
     [1.7, 3.1, 2.0]),
    ("callout", "Найважливіше речення цього звіту",
     "Майже ідеальні оцінки — на детермінованому симуляторі плюс слабких публічних метаданих, з оцінкою без "
     "витоку. Вони доводять конвеєр і розділення міток — це НЕ твердження про реальну точність, яку треба "
     "заслужити на власному розміченому промисловому трафіку. До того часу модель лишається SHADOW/ADVISORY. "
     "Ми зазначаємо це тут, на початку, навмисно.", "warning"),

    ("h1", "Що ми тестували і навіщо", "2"),
    ("p", "Продукт проти невалідного трафіку заслуговує довіру, коли його тестують так, як він "
          "використовуватиметься — не просто «чи запускається», а «чи називає правильний фрод, чи калібрує "
          "впевненість і чи відмовляється перезвинувачувати». Тому верифікація багаторівнева:"),
    ("bullet", "**Кожна родина фроду** — на відкладених подіях, чи відділяє кожна з десяти міток свій вид "
               "фроду від усього іншого і чи достатньо висока точність, щоб діяти?"),
    ("bullet", "**Ймовірності** — чи справді 0.9 означає приблизно 90%, тож аналітики й пороги можуть їм "
               "довіряти?"),
    ("bullet", "**Процес** — чи розбиття без витоку (жоден ідентифікатор не перетинає train/test) і чи "
               "запуск відтворюваний для фіксованого сіда?"),
    ("bullet", "**Середовище виконання** — чи завантажується експортована мережа з перевіреними "
               "контрольними сумами і чи проходить набір тестів Java?"),

    ("h1", "Тестове середовище", "3"),
    ("table", ["Пункт", "Значення"],
     [["Платформа", "Багатомодульний проєкт Maven Jneopallium (master + worker)"],
      ["Середовище виконання", "Скорер на Java 17+; процес на Python 3 (без сторонніх пакетів)"],
      ["Модель під тестом", "advertising-fraud, 1.0.0-reference-advisory"],
      ["Задача", "Багатоміткова: десять незалежних міток невалідного трафіку"],
      ["Мережа", "8 рівнів, 22 нейрони, 21 ознака (8 базових + 5 доказів + 8 взаємодій)"],
      ["Режим безпеки", "ADVISORY / SHADOW; AUTOMATED_ACTION_READY = false"],
      ["Еталонна команда", "python scripts/demo-ad-fraud/run_all.py --offline"],
      ["Детермінізм", "Відтворювано для фіксованого сіда: та сама команда → ті самі метрики"]],
     [2.0, 4.8]),

    ("h1", "Метрики простою мовою", "4"),
    ("p", "«Приклад» — це одна рекламна подія, яку модель позначає. Метрики класифікації:"),
    ("table", ["Метрика", "Значення простою мовою", "Найкраще"],
     [["Точність", "Зі подій, позначених міткою, скільки справді її мали", "1.0 (без хибних тривог)"],
      ["Повнота", "Зі справжніх випадків мітки скільки зловлено", "1.0 (нічого не пропущено)"],
      ["F1", "Єдина справедлива суміш точності й повноти", "1.0"],
      ["Macro-F1", "F1, усереднений порівну по всіх десяти мітках", "1.0"],
      ["PR / ROC-AUC", "Наскільки добре бал *ранжує* фрод над не-фродом", "1.0"],
      ["Похибка калібрування", "Наскільки бал 0.9 далекий від справжніх 90%", "0.0"]],
     [1.8, 3.6, 1.4]),
    ("callout", "Ранжування проти порога — чому важливі обидва",
     "Голова може ранжувати ідеально (AUC 1.0), але мати поганий F1, якщо її поріг рішення неправильний. "
     "Попередня модель провалилася саме так на двох мітках; оновлення виправило пороги й калібрування та "
     "додало докази, яких бракувало конфліктним міткам.", "info"),

    ("h1", "Якість моделі — на мітку, до і після", "5"),
    ("p", "Кожну мітку оцінено на відкладеному розбитті без витоку (включно з ворожим холдаутом). Нещодавнє "
          "оновлення покращило або втримало кожну мітку; macro-F1 зріс із **0.66 до 0.97**:"),
    ("table", ["Мітка фроду", "F1 до", "F1 після", "Точність після", "Повнота після"],
     [["bot", "1.00", "1.00", "1.00", "1.00"],
      ["eventSpoofing", "1.00", "1.00", "1.00", "1.00"],
      ["attributionHijack", "1.00", "1.00", "1.00", "1.00"],
      ["unknownSuspicious", "1.00", "1.00", "1.00", "1.00"],
      ["clickInjection", "0.25", "**1.00**", "1.00", "1.00"],
      ["inventorySpoofing", "0.25", "**1.00**", "1.00", "1.00"],
      ["incentivized", "0.67", "**1.00**", "1.00", "1.00"],
      ["clickFarm", "0.67", "**1.00**", "1.00", "1.00"],
      ["clickSpam", "0.29", "**0.98**", "0.95", "1.00"],
      ["accidentalOrLowValue", "0.44", "0.69", "0.52", "1.00"]],
     [2.3, 1.0, 1.1, 1.3, 1.2]),
    ("p", "**macro-F1: 0.656 → 0.966.** Кожну раніше зламану голову виправлено. Єдина слабка точка, що "
          "лишилася, `accidentalOrLowValue` (0.69), — це навмисно розмита *об'єднана* мітка: випадкові "
          "кліки, низькоконверсійний трафік і мотивовані інсталяції — усе зведено в неї, тож вона "
          "внутрішньо складніша, і підняти її вище потребує власних міток, а не більше архітектури."),

    ("h1", "Оновлення, що підняло оцінку", "6"),
    ("p", "Стрибок стався завдяки діагнозу, а не вгадуванню. Попередня модель провалювалася двома різними "
          "способами, кожен виправлено конкретною зміною:"),
    ("h3", "Провали порогів / калібрування (дешево виправити)"),
    ("p", "`clickInjection` та `inventorySpoofing` ранжували ідеально (AUC 1.0), але мали F1 0.25, бо їхній "
          "поріг рішення позначав *усе*, а калібрування було пустим. Виправлення порогів на мітку, "
          "додавання справжнього Platt-калібрування й надання цим ворожим родинам покриття в навчанні "
          "підняли обидві до 1.0."),
    ("h3", "Конфліктні мітки (потрібна нова потужність моделі)"),
    ("p", "`clickSpam` мала тотожну голову з `attributionHijack`, а `incentivized` — із `clickFarm`: 8 "
          "базових ознак не могли відрізнити схожих. Це виправили два нові рівні: **рівень поведінкових "
          "доказів** (однопризначені нейрони, що видають ознаки обсягу кліків, часу конверсії, мотивації й "
          "низької цінності) та **нелінійний рівень взаємодії ознак**. Разом із класово-зваженими, "
          "L2-регуляризованими логістичними головами конфліктні мітки розділилися."),
    ("table", ["Покращення", "Ефект"],
     [["Пороги на мітку з урахуванням вартості", "Виправили clickInjection / inventorySpoofing (0.25 → 1.0)"],
      ["Справжнє Platt-калібрування (було тотожність)", "Чесні ймовірності; нижча похибка калібрування"],
      ["Припасовані класово-зважені логістичні голови", "Замінили одноразове правило різниці середніх"],
      ["+5 поведінкових ознак + новий рівень", "Розділили clickSpam, incentivized, низьку цінність"],
      ["+8 нелінійних одиниць взаємодії (новий рівень)", "Захопили комбінації доказів І/АБО"],
      ["Більший збалансований корпус", "Стабільне припасування й калібрування для кожної мітки"]],
     [3.3, 3.5]),

    ("h1", "Калібрування — чи чесні ймовірності?", "7"),
    ("p", "Оцінка впевненості корисна, лише якщо означає те, що каже. Попередня модель **не** застосовувала "
          "калібрування (стверджувала, що тестує Platt / isotonic / temperature, але застосовувала "
          "тотожність). Поточна модель припасовує **справжнє Platt-масштабування на мітку** на відкладеному "
          "розбитті калібрування, тож 0.9 справді означає приблизно 90%. Саме це робить смуги відповіді — і "
          "будь-який майбутній поріг ємності перегляду — гідними довіри."),

    ("h1", "Детермінований процес і аудит витоку", "8"),
    ("p", "Числа якості мають сенс, лише якщо оцінка чесна. Процес це забезпечує:"),
    ("bullet", "**Розбиття без витоку** — за сценарієм, епізодом кампанії й детермінованим блоком реплік, "
               "із ворожим холдаутом. Аудит **провалює запуск**, якщо будь-який ідентифікатор пристрою / "
               "кліку / кампанії перетинає межі розбиття."),
    ("bullet", "**Відтворювано** — для фіксованого сіда та сама команда дає той самий корпус, ті самі "
               "метрики й той самий експортований пакет."),
    ("bullet", "**Перевірено приватність** — ідентифікатори HMAC-псевдонімізовані; сирі IP, точна гео й "
               "user-agent ніколи не експортуються; відсутнє окреме від нуля."),

    ("h1", "Готова до розгортання мережа і набір тестів Java", "9"),
    ("p", "Запуск також створив і перевірив **готову до розгортання мережу JNeopallium**: вісім файлів "
          "рівнів плюс `model-descriptor.json`, що описує 8 рівнів, 22 справжні нейрони й 21 ознаку, де "
          "кожен нейрон посилається на конкретний клас часу виконання, з вбудованими каліброваними головами "
          "й нелінійним прихованим рівнем. Середовище виконання Java завантажує цей пакет, перевіряє "
          "контрольні суми й обчислює прихований рівень — тож **протестований артефакт — це той самий "
          "артефакт, що розгортається**."),
    ("p", "Набір тестів Java — **12 тестів**, що охоплюють покриття типів подій, каданси сигналів, "
          "детерміновані правила цілісності, поведінку заморожування базової лінії й TTL графа, "
          "рекомендаційний запобіжник відповіді, завантаження пакета з перевіркою контрольних сум, "
          "структуру опису / рівнів, процесори за інтерфейсом і HTTP-службу скорингу — **проходить 12 / 12**."),

    ("h1", "Чесні обмеження (прочитайте уважно)", "10"),
    ("num", "**Еталонний корпус синтетичний плюс слабкі мітки.** Детермінований симулятор постачає класи "
            "фроду, що не мають публічних міток, доповнені слабкими публічними метаданими. Майже ідеальні "
            "оцінки підтверджують конвеєр і розділення міток — не реальну точність."),
    ("num", "**Реальну точність треба заслужити на власних мітках.** Справжні промислові твердження "
            "потребують підтверджених чарджбеків, рішень MMP щодо фроду й вердиктів аналітиків, із "
            "валідацією вперед у часі та на небачених паблішерах, лімітами фінансової вартості хибних "
            "спрацювань і задокументованим переглядом обробки даних."),
    ("num", "**Лише рекомендації, і ніколи не звинувачення.** Модель видає докази невалідного трафіку й "
            "кандидатні дії; вона ніколи не блокує, не утримує гроші й не стверджує, що названа особа "
            "скоїла фрод. `AUTOMATED_ACTION_READY` лишається хибним, доки немає валідації, юр. перегляду й "
            "апеляції/відкату."),
    ("callout", "Чому ми починаємо із застереження",
     "Продукт проти фроду, що розмиває межу між доказами конвеєра й польовою точністю, не можна довірити "
     "поряд із рішенням про виплату. Чесність — частина продукту: кожна рекомендація несе свої докази, а "
     "кожне твердження звіту несе свій обсяг.", "info"),

    ("h1", "Загальний вердикт і готовність", "11"),
    ("p", "**Ad-Fraud Guardian проходить кожен тест на кожному рівні на еталонному корпусі, а нещодавнє "
          "оновлення підняло macro-F1 з 0.66 до 0.97.** Десять міток розділяються з чесними, добре "
          "каліброваними ймовірностями; процес без витоку й відтворюваний; готова мережа завантажується, а "
          "набір Java зелений; стеля безпеки (рекомендації, без звинувачення) збережена впродовж усього."),
    ("p", "Готовність: **ENGINEERING_READY, SHADOW_READY, ADVISORY_READY = true; AUTOMATED_ACTION_READY = "
          "false.** Система готова до наступного етапу — офлайн-відтворення й тіньова валідація на "
          "реальному власному трафіку, єдиного, що може перетворити ці сильні докази конвеєра на твердження "
          "про польову точність."),

    ("h1", "Словник", "12"),
    ("table", ["Термін", "Значення"],
     [["Багатоміткова", "Кілька незалежних міток фроду можуть бути істинними для однієї події"],
      ["Точність / повнота", "Частка правильних позначок / частка зловлених справжніх випадків"],
      ["Macro-F1", "Якість виявлення, усереднена порівну по десяти мітках"],
      ["AUC", "Наскільки добре бал ранжує фрод над не-фродом, незалежно від порога"],
      ["Калібрування", "Чи означає оцінка впевненості те, що каже"],
      ["Platt-масштабування", "Стандартний спосіб перетворити сирі бали на чесні ймовірності"],
      ["Витік", "Коли майже-дублікати просочуються між train/test і завищують оцінки"],
      ["Ворожий холдаут", "Види атак, що лишаються повністю небаченими до оцінки"],
      ["Рекомендаційно / тінь", "Система радить; сама ніколи не блокує й не утримує"]],
     [2.2, 4.6]),
    ("spacer", 8),
    ("pi", "Jneopallium Ad-Fraud Guardian · Повний звіт про тестування. Результати — це докази конвеєра на "
           "детермінованому еталонному корпусі плюс слабких мітках, а не твердження про реальну точність. "
           "Режим безпеки: ADVISORY / SHADOW. Ліцензія: BSD 3-Clause."),
]
