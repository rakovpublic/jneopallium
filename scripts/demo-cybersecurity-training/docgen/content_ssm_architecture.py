# -*- coding: utf-8 -*-
"""Self-Supervised Maintenance Guardian — architecture article (EN + UK)."""

from __future__ import annotations

DATE = "2 July 2026"
DATE_UK = "2 липня 2026"


def architecture(lang: str) -> list:
    return _EN if lang == "en" else _UK


_META_EN = [
    ("Document", "Architecture Article"),
    ("Product", "Jneopallium Self-Supervised Maintenance Guardian"),
    ("Module", "self-supervised-maintenance 1.0.0-label-free-continuous"),
    ("Runtime", "Java neurons · net.neuron.impl.ssmaint"),
    ("Training", "Python (initial fit only) · standard library"),
    ("Safety", "ADVISORY / read-only — never actuates"),
    ("Author", "Dmytro Rakovskyi — Kharkiv, Ukraine"),
    ("Date", DATE),
    ("License", "BSD 3-Clause"),
]

_EN = [
    ("cover", "Jneopallium · Self-Supervised Maintenance Guardian",
     "Predictive Maintenance Without Labels",
     "How the model learns 'maintenance is required' from ordinary telemetry — and keeps learning from "
     "operators while it runs",
     _META_EN, "Architecture Article",
     "A complete walk-through of a label-free predictive-maintenance model built as a Jneopallium neuron "
     "network: what each neuron does, why it needs no fault history, how it keeps improving from operator "
     "feedback without a redeploy, and why it stays strictly advisory."),

    ("toc", "Contents",
     ["The problem in one paragraph",
      "For the non-specialist: the whole idea",
      "What the model observes",
      "The five layers at a glance",
      "Layer 1 — self-supervised reconstruction",
      "Layer 2 — the maintenance hypothesis",
      "Layer 3 — continuous learning from feedback",
      "Layer 4 — the read-only advisory gate",
      "The signals that connect it all",
      "Why no labels are needed",
      "How it keeps learning without a redeploy",
      "Safety model",
      "What it cannot do (honest limits)",
      "Deployment shape"]),

    ("h1", "The problem in one paragraph", "1"),
    ("p", "Most predictive-maintenance models are supervised: you feed them a history of past failures and "
          "repairs, and they learn to recognise the run-up to each one. That works — once you have a "
          "labelled failure history. Most sites do not. They have years of sensor telemetry and a "
          "maintenance log that is patchy, free-text, or simply absent. The Self-Supervised Maintenance "
          "Guardian is the model for exactly that situation: it learns what 'healthy' looks like directly "
          "from the telemetry, flags departures from it as developing faults with a lead time, and sharpens "
          "itself over time from the one form of supervision that arrives for free — an operator saying "
          "'yes, that was real' or 'no, that was a false alarm.'"),

    ("h1", "For the non-specialist: the whole idea", "2"),
    ("p", "Imagine a seasoned operator who has listened to a pump for years. Nobody handed them a textbook of "
          "labelled failures; they simply know what the machine sounds like on a good day, and they notice "
          "when something drifts. When it does, they don't shout 'FAILURE' — they say 'keep an eye on "
          "bearing 2, it's warming up.' If they're wrong a few times, they recalibrate their own sense of "
          "'normal.' This model is that operator, written down."),
    ("bullet", "It learns 'normal' from the data itself — no labelled failures required."),
    ("bullet", "It watches several sensors together, because a real fault shows up as a pattern across "
               "them, not a single needle in the red."),
    ("bullet", "It only raises a flag when the drift is persistent, trending, and consistent — not on a "
               "one-second blip."),
    ("bullet", "It tells you which kind of problem it looks like and roughly how long you have."),
    ("bullet", "When an operator confirms or dismisses a flag, it quietly adjusts — while it keeps running."),
    ("bullet", "It never touches the machine. It only ever advises a human."),
    ("callout", "One sentence to remember",
     "The model's targets are the sensors themselves, not a failure label — which is why it can be trained "
     "on nothing but ordinary operating data.", "info"),

    ("h1", "What the model observes", "3"),
    ("p", "The only input is a telemetry frame — one reading of each sensor at one moment, tagged with the "
          "current operating regime (low / medium / high production load). The reference build uses eight "
          "coupled pump-skid channels:"),
    ("table", ["Channel", "What it is", "Why it matters"],
     [["suction_pressure", "Pump inlet pressure", "Falls in cavitation"],
      ["flow", "Process flow rate", "Sets the operating point; unstable in cavitation"],
      ["pump_speed", "Shaft speed", "Normalises everything else"],
      ["pump_power", "Motor power draw", "Rises with wear and with lost efficiency"],
      ["vibration_rms", "Vibration level", "Rises with bearing wear and cavitation"],
      ["bearing_temp", "Bearing temperature", "Rises with bearing wear"],
      ["process_temp", "Process temperature", "Drifts when its sensor fails"],
      ["valve_position", "Control valve travel", "Oscillates with loop/stiction problems"]],
     [1.7, 2.2, 2.6]),
    ("p", "The channels are physically coupled — power tracks flow, bearing temperature tracks vibration — "
          "and that coupling is exactly what the model exploits. The regime tag lets it judge a reading "
          "relative to its load, so a busy shift does not look like a fault."),

    ("h1", "The five layers at a glance", "4"),
    ("table", ["Layer", "Neuron", "Turns … into …"],
     [["0", "(telemetry ingest)", "sensor frame → AssetTelemetrySignal"],
      ["1", "CrossSensorReconstructionNeuron", "telemetry → reconstruction residual"],
      ["2", "MaintenanceHypothesisNeuron", "residual → maintenance hypothesis + lead time"],
      ["3", "FeedbackAdaptationNeuron", "operator feedback → threshold update"],
      ["4", "SsAdvisoryGateNeuron", "hypothesis (+ update) → read-only advisory"]],
     [0.7, 3.1, 2.7]),
    ("p", "Layers 1–2 and 4 form the fast detection path (telemetry in, advisory out). Layer 3 is the slow "
          "learning path (feedback in, thresholds out) that feeds back into layer 4. Each neuron is "
          "single-purpose; capacity is added as layers, not as tangled logic inside one neuron."),

    ("h1", "Layer 1 — self-supervised reconstruction", "5"),
    ("p", "This is the heart of the label-free idea. For each sensor the neuron holds a small linear model "
          "— fitted offline on a trusted-healthy window — that predicts that sensor from all the others, "
          "after standardising by regime. At runtime it compares prediction to reality:"),
    ("code", "residual_i = actual_i − predicted_from_peers_i     (standardised)\n"
             "total      = mean over sensors of residual_i²"),
    ("p", "A large **total** means the joint pattern no longer looks healthy. A large **single** residual "
          "means one sensor disagrees with everyone else — the classic signature of a failing sensor. "
          "Because each model's target is another sensor, no failure label is ever needed; the data "
          "supervises itself. The neuron also reports a **domain-shift** fraction: how many sensors sit far "
          "outside the envelope it was trained on, i.e. how unfamiliar this reading is."),
    ("callout", "Plain language",
     "The neuron asks each sensor: 'given what your neighbours are doing, what should you read?' When a "
     "sensor's real value drifts away from what its neighbours imply, that gap is the early warning.",
     "info"),

    ("h1", "Layer 2 — the maintenance hypothesis", "6"),
    ("p", "One residual is noise; a maintenance case is a story over time. The hypothesis neuron keeps, per "
          "asset, four running quantities and fuses them:"),
    ("bullet", "**Trend** — a slow moving average of the residual and its slope, so a steady climb is "
               "distinguished from a spike."),
    ("bullet", "**Change-point** — a Page-Hinkley detector that fires when behaviour steps to a new level."),
    ("bullet", "**Severity** — the level expressed in the asset's own baseline-percentile units, so '1.0' "
               "means 'at the 99th percentile this asset has ever shown.' Calibrated from healthy history, "
               "not from labels."),
    ("bullet", "**Evidence** — an accumulator that only rises when the deviation is severe, trending, "
               "consistent across at least two sensors, and not merely a domain shift."),
    ("p", "It attributes a **fault family** heuristically from which sensors dominate the residual "
          "(bearing, cavitation, sensor, energy, oscillation, or an honest 'unknown'), and it extrapolates "
          "the trend to the baseline limit to estimate a **lead time** — the label-free stand-in for "
          "'how long until this needs attention.' Uncertainty rises with domain shift, so an unfamiliar "
          "asset reads as 'watching, not sure' instead of a confident wrong answer."),

    ("h1", "Layer 3 — continuous learning from feedback", "7"),
    ("p", "When an operator marks an advisory confirmed or false-positive, that verdict arrives as an "
          "OperatorFeedbackSignal. The adaptation neuron holds a per-fault-family offset on top of a static "
          "base threshold and moves it:"),
    ("bullet", "A **false positive** raises that family's threshold — fewer nuisance alerts of that kind."),
    ("bullet", "A **confirmed need** relaxes it slightly — recall is already high, so the nudge is small "
               "and deliberately asymmetric."),
    ("p", "Every update is **bounded** (the offset is clamped), **rate-limited** (a minimum interval "
          "between changes), and **frozen during domain shift** so a novel or abnormal period cannot poison "
          "the thresholds. The neuron owns this state, which the runtime persists to storage — so the "
          "learning survives a restart. Crucially, none of this requires rebuilding or redeploying the "
          "model: it emits a ThresholdUpdateSignal that the gate applies in place."),

    ("h1", "Layer 4 — the read-only advisory gate", "8"),
    ("p", "The terminal stage turns a hypothesis into a human advisory when the accumulated evidence clears "
          "the family's live threshold, subject to a per-asset de-duplication window so one developing "
          "fault is not reported every tick. It holds its thresholds in a live map that the feedback path "
          "mutates in place — which is how the operators' verdicts reshape gate behaviour while the process "
          "keeps running. The only thing it can emit is a MaintenanceAdvisorySignal; it has no path to an "
          "actuator, by construction."),
    ("callout", "Regression guard worth noting",
     "The dedup window and the feedback rate-limit originally used a sentinel that overflowed on real "
     "epoch-millisecond timestamps, which would have suppressed the very first advisory and the first "
     "feedback in production. Both are fixed and covered by tests.", "warning"),

    ("h1", "The signals that connect it all", "9"),
    ("table", ["Signal", "Carries", "Cadence (epoch/loop)"],
     [["AssetTelemetrySignal", "one multi-sensor frame + regime", "1 / 1 (fast)"],
      ["ReconResidualSignal", "total + per-sensor residuals + domain shift", "1 / 1"],
      ["HealthHypothesisSignal", "family, severity, evidence, lead time, uncertainty", "2 / 2"],
      ["OperatorFeedbackSignal", "confirmed / false-positive + provenance", "10 / 2 (slow)"],
      ["ThresholdUpdateSignal", "family threshold + offset", "10 / 2"],
      ["MaintenanceAdvisorySignal", "read-only recommendation + lead time", "2 / 2"]],
     [2.6, 2.6, 1.5]),
    ("p", "Every signal is a typed ISignal with its own ProcessingFrequency, so the fast detection path and "
          "the slow learning path are scheduled independently on the same engine."),

    ("h1", "Why no labels are needed", "10"),
    ("p", "Each learning objective is self-supervised — its target is something the model can check against "
          "reality without a human: reconstruct a sensor from its peers, calibrate severity against the "
          "asset's own history, extrapolate a trend to a limit. None of these require knowing that a "
          "specific past event 'was a bearing failure.' The consequence is practical: you can deploy on a "
          "site that has telemetry but no maintenance-labelled history, which is most sites."),

    ("h1", "How it keeps learning without a redeploy", "11"),
    ("p", "Learning at runtime lives in neuron state, not in the packaged JAR. The worker runs continuously; "
          "the adaptation neuron adjusts thresholds on the slow loop and persists them to the configured "
          "storage. A separate champion/challenger discipline is the recommended way to evolve the heavier "
          "parameters safely: a stable champion serves advisories while a challenger adapts in shadow and is "
          "promoted only when it beats the champion on independent outcomes. Nothing here interrupts the "
          "running process."),

    ("h1", "Safety model", "12"),
    ("bullet", "**Advisory-only invariant.** SsMaintConfig forbids disabling the advisory posture; no "
               "neuron in the module has a path to an actuator."),
    ("bullet", "**The safety layer is never learned.** Threshold adaptation touches scores and alert "
               "thresholds — never a hard interlock or a safety gate."),
    ("bullet", "**Honest uncertainty.** Domain-shift and uncertainty are first-class outputs, so the model "
               "can decline to claim confidence on an unfamiliar asset."),
    ("bullet", "**Anti-poisoning.** Adaptation freezes during domain shift; baselines adapt only from "
               "trusted-healthy periods."),

    ("h1", "What it cannot do (honest limits)", "13"),
    ("p", "Label-free mode detects **degradation**, not a contractually-due repair. Fault families are "
          "heuristic hypotheses until confirmed events accumulate, so expect more false positives than a "
          "fully label-trained model — which is exactly why the feedback loop exists. It also needs a "
          "**mostly-healthy** training window (or fleet peers) to define normal; a brand-new asset with no "
          "history starts in a high-uncertainty, domain-shift-aware mode rather than making confident "
          "claims."),

    ("h1", "Deployment shape", "14"),
    ("p", "The trained model is a Jneopallium bundle — a model descriptor, an IContext, and per-layer neuron "
          "configuration carrying the fitted parameters — loaded by the standard Entry runner in local, "
          "HTTP, or gRPC mode. Telemetry arrives through the usual OPC UA / MQTT inputs; advisories leave "
          "through an output aggregator (JSONL, Kafka, or MQTT). The companion Deployment Guide covers the "
          "full runbook; the Training Guide covers fitting the parameters from your own historian."),
    ("pi", "Self-Supervised Maintenance Guardian — label-free detection, feedback-driven learning, and a "
           "safety posture that never leaves advisory. It is the model for the site that has data but not "
           "yet a labelled failure history."),
]


_META_UK = [
    ("Документ", "Архітектурна стаття"),
    ("Продукт", "Jneopallium Self-Supervised Maintenance Guardian"),
    ("Модуль", "self-supervised-maintenance 1.0.0-label-free-continuous"),
    ("Середовище виконання", "Java-нейрони · net.neuron.impl.ssmaint"),
    ("Навчання", "Python (лише початкове припасування) · стандартна бібліотека"),
    ("Безпека", "ADVISORY / лише читання — ніколи не керує"),
    ("Автор", "Дмитро Раковський — Харків, Україна"),
    ("Дата", DATE_UK),
    ("Ліцензія", "BSD 3-Clause"),
]

_UK = [
    ("cover", "Jneopallium · Self-Supervised Maintenance Guardian",
     "Прогнозне обслуговування без міток",
     "Як модель вчиться визначати, що «потрібне обслуговування», зі звичайної телеметрії — і продовжує "
     "навчатися від операторів під час роботи",
     _META_UK, "Архітектурна стаття",
     "Повний огляд моделі прогнозного обслуговування без міток, побудованої як нейронна мережа Jneopallium: "
     "що робить кожен нейрон, чому не потрібна історія відмов, як вона вдосконалюється зі зворотного "
     "зв'язку операторів без переустановлення та чому залишається суто рекомендаційною."),

    ("toc", "Зміст",
     ["Проблема в одному абзаці",
      "Для нефахівця: сама ідея",
      "Що спостерігає модель",
      "П'ять шарів коротко",
      "Шар 1 — самокероване відновлення",
      "Шар 2 — гіпотеза обслуговування",
      "Шар 3 — безперервне навчання зі зворотного зв'язку",
      "Шар 4 — рекомендаційний шлюз лише для читання",
      "Сигнали, що все поєднують",
      "Чому не потрібні мітки",
      "Як воно навчається без переустановлення",
      "Модель безпеки",
      "Чого воно не може (чесні межі)",
      "Форма розгортання"]),

    ("h1", "Проблема в одному абзаці", "1"),
    ("p", "Більшість моделей прогнозного обслуговування є керованими: їм подають історію минулих відмов і "
          "ремонтів, і вони вчаться розпізнавати наростання перед кожною. Це працює — коли у вас є "
          "розмічена історія відмов. У більшості майданчиків її немає. Є роки телеметрії та журнал "
          "обслуговування, що уривчастий, у вільній формі або відсутній. Self-Supervised Maintenance "
          "Guardian — це модель саме для такої ситуації: вона вчиться, який вигляд має «здоровий» стан, "
          "безпосередньо з телеметрії, позначає відхилення як несправності, що розвиваються, із запасом "
          "часу, і згодом уточнює себе завдяки єдиному виду нагляду, що надходить безкоштовно — оператор "
          "каже «так, це справжнє» або «ні, це хибна тривога»."),

    ("h1", "Для нефахівця: сама ідея", "2"),
    ("p", "Уявіть досвідченого оператора, який роками слухає насос. Ніхто не давав йому підручник "
          "розмічених відмов; він просто знає, як машина звучить у добрий день, і помічає, коли щось "
          "відхиляється. Тоді він не кричить «ВІДМОВА» — він каже «пильнуй підшипник 2, він гріється». Якщо "
          "він кілька разів помилиться, він переналаштовує власне відчуття «норми». Ця модель — той оператор, "
          "записаний у код."),
    ("bullet", "Вона вчиться «нормі» із самих даних — розмічені відмови не потрібні."),
    ("bullet", "Вона дивиться на кілька сенсорів разом, бо справжня несправність проявляється як "
               "закономірність між ними, а не одна стрілка в червоній зоні."),
    ("bullet", "Вона піднімає прапорець лише тоді, коли відхилення стійке, трендове й узгоджене — а не на "
               "миттєвий сплеск."),
    ("bullet", "Вона підказує, на який тип проблеми це схоже і приблизно скільки у вас часу."),
    ("bullet", "Коли оператор підтверджує чи відхиляє прапорець, вона тихо коригується — не зупиняючись."),
    ("bullet", "Вона ніколи не торкається машини. Вона лише радить людині."),
    ("callout", "Одне речення для запам'ятовування",
     "Цілі моделі — це самі сенсори, а не мітка відмови, тому її можна навчати лише на звичайних робочих "
     "даних.", "info"),

    ("h1", "Що спостерігає модель", "3"),
    ("p", "Єдиний вхід — це кадр телеметрії: одне зчитування кожного сенсора в один момент, позначене "
          "поточним режимом роботи (низьке / середнє / високе навантаження). Еталонна збірка використовує "
          "вісім пов'язаних каналів насосної установки:"),
    ("table", ["Канал", "Що це", "Чому важливо"],
     [["suction_pressure", "Тиск на вході насоса", "Падає при кавітації"],
      ["flow", "Витрата процесу", "Задає робочу точку; нестабільна при кавітації"],
      ["pump_speed", "Швидкість валу", "Нормалізує решту"],
      ["pump_power", "Споживана потужність", "Зростає з зносом і втратою ефективності"],
      ["vibration_rms", "Рівень вібрації", "Зростає при зносі підшипника й кавітації"],
      ["bearing_temp", "Температура підшипника", "Зростає при зносі підшипника"],
      ["process_temp", "Температура процесу", "Дрейфує при відмові її сенсора"],
      ["valve_position", "Хід клапана", "Коливається при проблемах контуру/залипанні"]],
     [1.7, 2.2, 2.6]),
    ("p", "Канали фізично пов'язані — потужність слідує за витратою, температура підшипника за вібрацією — "
          "і саме цей зв'язок модель використовує. Позначка режиму дозволяє оцінювати зчитування відносно "
          "навантаження, тож напружена зміна не виглядає як несправність."),

    ("h1", "П'ять шарів коротко", "4"),
    ("table", ["Шар", "Нейрон", "Перетворює … на …"],
     [["0", "(прийом телеметрії)", "кадр сенсорів → AssetTelemetrySignal"],
      ["1", "CrossSensorReconstructionNeuron", "телеметрія → залишок відновлення"],
      ["2", "MaintenanceHypothesisNeuron", "залишок → гіпотеза + запас часу"],
      ["3", "FeedbackAdaptationNeuron", "зворотний зв'язок → оновлення порога"],
      ["4", "SsAdvisoryGateNeuron", "гіпотеза (+ оновлення) → рекомендація"]],
     [0.7, 3.1, 2.7]),
    ("p", "Шари 1–2 і 4 утворюють швидкий шлях виявлення (телеметрія на вході, рекомендація на виході). "
          "Шар 3 — повільний шлях навчання (зворотний зв'язок на вході, пороги на виході), що живить шар 4. "
          "Кожен нейрон однопризначений; потужність додається шарами, а не заплутаною логікою в одному "
          "нейроні."),

    ("h1", "Шар 1 — самокероване відновлення", "5"),
    ("p", "Це серце ідеї без міток. Для кожного сенсора нейрон містить невелику лінійну модель — припасовану "
          "офлайн на довіреному здоровому вікні — яка передбачає цей сенсор за всіма іншими після "
          "стандартизації за режимом. Під час роботи вона порівнює передбачення з реальністю:"),
    ("code", "residual_i = actual_i − predicted_from_peers_i     (стандартизовано)\n"
             "total      = середнє по сенсорах від residual_i²"),
    ("p", "Великий **total** означає, що спільна картина більше не виглядає здоровою. Великий **окремий** "
          "залишок означає, що один сенсор суперечить усім іншим — класична ознака сенсора, що виходить з "
          "ладу. Оскільки ціллю кожної моделі є інший сенсор, мітка відмови не потрібна; дані навчають "
          "самі себе. Нейрон також повідомляє частку **зсуву домену**: скільки сенсорів лежить далеко поза "
          "межами, на яких він навчався, тобто наскільки незвичне це зчитування."),
    ("callout", "Простими словами",
     "Нейрон запитує кожен сенсор: «зважаючи на те, що роблять твої сусіди, яким має бути твоє показання?» "
     "Коли реальне значення сенсора відхиляється від того, що передбачають сусіди, цей розрив і є раннім "
     "попередженням.", "info"),

    ("h1", "Шар 2 — гіпотеза обслуговування", "6"),
    ("p", "Один залишок — це шум; випадок обслуговування — це історія в часі. Нейрон гіпотези тримає для "
          "кожного активу чотири поточні величини й поєднує їх:"),
    ("bullet", "**Тренд** — повільне ковзне середнє залишку та його нахил, щоб відрізнити стійке зростання "
               "від сплеску."),
    ("bullet", "**Точка зміни** — детектор Пейджа-Хінклі, що спрацьовує, коли поведінка переходить на новий "
               "рівень."),
    ("bullet", "**Тяжкість** — рівень у власних перцентильних одиницях активу, тож «1.0» означає «на 99-му "
               "перцентилі, який цей актив колись показував». Каліброване зі здорової історії, не з міток."),
    ("bullet", "**Свідчення** — накопичувач, що зростає лише коли відхилення тяжке, трендове, узгоджене "
               "щонайменше по двох сенсорах і не є просто зсувом домену."),
    ("p", "Він приписує **сімейство несправності** евристично за тим, які сенсори домінують у залишку "
          "(підшипник, кавітація, сенсор, енергія, коливання чи чесне «невідоме»), і екстраполює тренд до "
          "базової межі, щоб оцінити **запас часу** — замінник без міток для «скільки лишилось до потреби в "
          "увазі». Невизначеність зростає зі зсувом домену, тож незнайомий актив читається як «спостерігаю, "
          "не впевнений» замість впевненої хибної відповіді."),

    ("h1", "Шар 3 — безперервне навчання зі зворотного зв'язку", "7"),
    ("p", "Коли оператор позначає рекомендацію як підтверджену чи хибну, цей вердикт надходить як "
          "OperatorFeedbackSignal. Нейрон адаптації тримає для кожного сімейства зсув поверх статичного "
          "базового порога й рухає його:"),
    ("bullet", "**Хибне спрацювання** підвищує поріг цього сімейства — менше зайвих тривог такого типу."),
    ("bullet", "**Підтверджена потреба** трохи послаблює його — повнота вже висока, тож поштовх малий і "
               "навмисно асиметричний."),
    ("p", "Кожне оновлення **обмежене** (зсув затиснуто), **з обмеженням частоти** (мінімальний інтервал "
          "між змінами) і **заморожене під час зсуву домену**, щоб новий чи аномальний період не отруїв "
          "пороги. Нейрон володіє цим станом, який середовище зберігає у сховищі — тож навчання переживає "
          "перезапуск. Головне: ніщо з цього не потребує перезбирання чи переустановлення моделі: він "
          "видає ThresholdUpdateSignal, який шлюз застосовує на місці."),

    ("h1", "Шар 4 — рекомендаційний шлюз лише для читання", "8"),
    ("p", "Кінцевий етап перетворює гіпотезу на рекомендацію для людини, коли накопичене свідчення долає "
          "живий поріг сімейства, з урахуванням вікна дедуплікації для кожного активу, щоб одна несправність, "
          "що розвивається, не повідомлялася щотакту. Він тримає пороги в живій мапі, яку шлях зворотного "
          "зв'язку змінює на місці — саме так вердикти операторів переформовують поведінку шлюзу під час "
          "роботи процесу. Єдине, що він може видати — це MaintenanceAdvisorySignal; шляху до виконавчого "
          "механізму в нього немає за побудовою."),
    ("callout", "Вартий уваги захист від регресії",
     "Вікно дедуплікації та обмеження частоти зворотного зв'язку спершу використовували сентинел, що "
     "переповнювався на реальних мітках часу в мілісекундах, що придушило б найпершу рекомендацію та "
     "перший зворотний зв'язок у продакшені. Обидва виправлено й покрито тестами.", "warning"),

    ("h1", "Сигнали, що все поєднують", "9"),
    ("table", ["Сигнал", "Несе", "Каданс (епоха/цикл)"],
     [["AssetTelemetrySignal", "один багатосенсорний кадр + режим", "1 / 1 (швидкий)"],
      ["ReconResidualSignal", "total + залишки по сенсорах + зсув домену", "1 / 1"],
      ["HealthHypothesisSignal", "сімейство, тяжкість, свідчення, запас часу, невизначеність", "2 / 2"],
      ["OperatorFeedbackSignal", "підтверджено / хибне + походження", "10 / 2 (повільний)"],
      ["ThresholdUpdateSignal", "поріг сімейства + зсув", "10 / 2"],
      ["MaintenanceAdvisorySignal", "рекомендація лише для читання + запас часу", "2 / 2"]],
     [2.6, 2.6, 1.5]),
    ("p", "Кожен сигнал — типізований ISignal зі своєю ProcessingFrequency, тож швидкий шлях виявлення й "
          "повільний шлях навчання плануються незалежно на одному рушії."),

    ("h1", "Чому не потрібні мітки", "10"),
    ("p", "Кожна ціль навчання є самокерованою — її ціль це те, що модель може перевірити з реальністю без "
          "людини: відновити сенсор за сусідами, каліброзувати тяжкість за власною історією активу, "
          "екстраполювати тренд до межі. Жодна з них не вимагає знати, що конкретна минула подія «була "
          "відмовою підшипника». Наслідок практичний: можна розгортати на майданчику, де є телеметрія, але "
          "немає розміченої історії обслуговування, а це більшість майданчиків."),

    ("h1", "Як воно навчається без переустановлення", "11"),
    ("p", "Навчання під час роботи живе у стані нейронів, а не в упакованому JAR. Робітник працює "
          "безперервно; нейрон адаптації коригує пороги на повільному циклі й зберігає їх у налаштованому "
          "сховищі. Окрема дисципліна «чемпіон/претендент» — рекомендований спосіб безпечно розвивати важчі "
          "параметри: стабільний чемпіон видає рекомендації, поки претендент адаптується в тіні й "
          "просувається лише коли перевершує чемпіона за незалежними наслідками. Ніщо тут не перериває "
          "робочий процес."),

    ("h1", "Модель безпеки", "12"),
    ("bullet", "**Інваріант «лише рекомендація».** SsMaintConfig забороняє вимикати рекомендаційний режим; "
               "жоден нейрон модуля не має шляху до виконавчого механізму."),
    ("bullet", "**Шар безпеки ніколи не навчається.** Адаптація порогів торкається оцінок і порогів тривог "
               "— ніколи жорсткого блокування чи шлюзу безпеки."),
    ("bullet", "**Чесна невизначеність.** Зсув домену й невизначеність — повноправні виходи, тож модель "
               "може відмовитися заявляти впевненість щодо незнайомого активу."),
    ("bullet", "**Захист від отруєння.** Адаптація заморожується під час зсуву домену; базові рівні "
               "адаптуються лише з довірених здорових періодів."),

    ("h1", "Чого воно не може (чесні межі)", "13"),
    ("p", "Режим без міток виявляє **деградацію**, а не договірно належний ремонт. Сімейства несправностей — "
          "евристичні гіпотези, доки не накопичаться підтверджені події, тож очікуйте більше хибних "
          "спрацювань, ніж у повністю навченої на мітках моделі — саме для цього існує цикл зворотного "
          "зв'язку. Також потрібне **переважно здорове** вікно навчання (або однолітки з парку), щоб "
          "визначити норму; цілком новий актив без історії починає у режимі високої невизначеності з "
          "усвідомленням зсуву домену, а не робить упевнені заяви."),

    ("h1", "Форма розгортання", "14"),
    ("p", "Навчена модель — це збірка Jneopallium: дескриптор моделі, IContext і поshарова конфігурація "
          "нейронів із припасованими параметрами — завантажується стандартним запускачем Entry в режимі "
          "local, HTTP чи gRPC. Телеметрія надходить через звичні входи OPC UA / MQTT; рекомендації "
          "виходять через агрегатор виходу (JSONL, Kafka чи MQTT). Супутній посібник з розгортання охоплює "
          "повний регламент; посібник з навчання охоплює припасування параметрів з вашого історіана."),
    ("pi", "Self-Supervised Maintenance Guardian — виявлення без міток, навчання зі зворотного зв'язку та "
           "режим безпеки, що ніколи не виходить за межі рекомендації. Це модель для майданчика, де є дані, "
           "але ще немає розміченої історії відмов."),
]
