# -*- coding: utf-8 -*-
"""Industrial Loop Guardian — test report (EN + UK), block DSL."""

from __future__ import annotations

DATE = "23 June 2026"
DATE_UK = "23 червня 2026"


def testreport(lang: str) -> list:
    return _EN if lang == "en" else _UK


_EN = [
    ("cover", "Jneopallium · Industrial Loop Guardian",
     "Complete Test Report",
     "FMI Skid Demo — model quality, scenario behaviour, evidence, and an honest reading",
     [("Document", "Verification & Test Report"),
      ("Product", "Jneopallium Industrial Loop Guardian (FMI Skid Demo)"),
      ("Model", "industrial-loop-guardian 1.0.0-reference-maintenance-energy"),
      ("Test scope", "Model quality, scenario behaviour, safety boundary, scale"),
      ("Result", "All checks passed (reference skid corpus)"),
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
      "Layer 1 — Diagnostic-head model quality",
      "What the model learned (feature evidence)",
      "Layer 2 — Scenario behaviour (nine runs)",
      "Layer 3 — Acceptance checks (the safety boundary)",
      "Layer 4 — Production-scale evidence and the deployable network",
      "The honest limitations (read this carefully)",
      "Overall verdict",
      "Glossary"]),

    ("h1", "Executive summary of results", "1"),
    ("p", "The Industrial Loop Guardian was verified at four independent levels: **model quality** (the "
          "four diagnostic heads separate each fault from normal operation), **scenario behaviour** (the "
          "running demo makes the right calls across nine plant scenarios), **acceptance checks** (the "
          "OPC UA / MQTT / PLC safety boundary holds), and **scale** (the training pipeline holds up at a "
          "100 GiB logical-corpus target). Every check passed."),
    ("table", ["Test layer", "What it checks", "Result"],
     [["Model quality", "Per-finding precision / recall / F1 / FP rate", "**macro-F1 ≈ 0.9995**, near-zero FP"],
      ["Scenario behaviour", "Nine deterministic plant scenarios", "**Pass** — diagnosis + safety as expected"],
      ["Acceptance", "OPC UA / MQTT / PLC boundary", "**Pass** — boundary holds in every scenario"],
      ["Scale", "100 GiB logical-corpus target & guardrails", "**Pass** — target reached, fitted sample bounded"]],
     [1.7, 3.1, 2.0]),
    ("callout", "The single most important sentence in this report",
     "The near-perfect scores below are PIPELINE evidence on a deterministic, synthetic skid corpus — they "
     "prove the system is wired correctly and the four faults are cleanly separable. They are NOT a claim "
     "of real-world accuracy, which must be earned on external historian, CMMS, and energy-meter data. We "
     "state this here, at the top, on purpose.", "warning"),

    ("h1", "What we tested, and why", "2"),
    ("p", "A supervisory product earns trust by being tested the way it will be used — not just \"does it "
          "run\", but \"does it diagnose correctly, stay inside the safety boundary, and explain "
          "itself.\" The verification is therefore layered:"),
    ("bullet", "**The trained heads** — given windows of plant behaviour, does each head separate its fault "
               "(pump wear, oscillation, sensor drift, energy deterioration) from normal operation, and "
               "are its false positives within budget?"),
    ("bullet", "**The running system** — across nine realistic scenarios, does it diagnose correctly, while "
               "the deterministic controls keep the plant safe?"),
    ("bullet", "**The safety boundary** — do OPC UA, MQTT, and PLC/SIS keep their roles in every scenario, "
               "including outages and interlock trips?"),
    ("bullet", "**The pipeline at scale** — does training stay correct, bounded, and reproducible at a "
               "100 GiB logical corpus?"),

    ("h1", "Test environment", "3"),
    ("table", ["Item", "Value"],
     [["Platform", "Jneopallium multi-module Maven project (master + worker)"],
      ["Runtime", "Java 17+ worker/controller; Python 3 trainer (no third-party packages)"],
      ["Model under test", "industrial-loop-guardian, 1.0.0-reference-maintenance-energy"],
      ["Task", "Multi-label: four independent diagnostic finding heads"],
      ["Feature count", "39 engineered features"],
      ["Safety mode", "ADVISORY, supervisory above PLC/PID/SIS"],
      ["Reference command", "train_loop_guardian_model.py --reference-multiplier 1000 --target-corpus-bytes 100gb …"],
      ["Determinism", "Fully reproducible: same command → same checksum and metrics"]],
     [2.0, 4.8]),

    ("h1", "A plain-language primer on the metrics", "4"),
    ("p", "Two families of numbers appear. First, **classification metrics** for the diagnostic heads — "
          "here an \"example\" is a window of plant behaviour the model labels as having a fault or not:"),
    ("table", ["Metric", "Plain-language meaning", "Best"],
     [["Precision", "Of the windows flagged with a fault, how many really had it", "1.0 (no false alarms)"],
      ["Recall", "Of the real faults, how many it caught", "1.0 (missed nothing)"],
      ["F1", "A single fair blend of precision and recall", "1.0"],
      ["False-positive rate", "How often it cried wolf on healthy windows", "0.0"],
      ["Macro-F1", "The F1 averaged equally across all four heads", "1.0"]],
     [1.7, 3.7, 1.4]),
    ("p", "Second, **control KPIs** for each scenario, which describe how the plant behaved, not the model: "
          "**IAE** (total tracking error), **overshoot**, **settling time**, **energy** (kWh), **interlock "
          "latency** (how fast a hard safety rule fired), and **availability** (fraction of time fast "
          "control stayed up)."),

    ("h1", "Layer 1 — Diagnostic-head model quality", "5"),
    ("p", "Each head was evaluated on held-out windows, split leakage-safe. Three heads are perfect; the "
          "energy head — the hardest, most subjective target — has a small, honest false-positive rate. "
          "Overall results across all 9,000 windows:"),
    ("table", ["Finding head", "Positives", "Precision", "Recall", "F1", "FP rate"],
     [["Pump wear / cavitation risk", "1,000", "1.0", "1.0", "1.0", "0.0"],
      ["Control-loop oscillation / tuning", "1,000", "1.0", "1.0", "1.0", "0.0"],
      ["Temperature-sensor drift", "1,000", "1.0", "1.0", "1.0", "0.0"],
      ["Energy-per-unit deterioration", "3,000", "0.9957", "1.0", "0.9978", "0.0022"]],
     [2.6, 1.0, 1.0, 0.8, 0.7, 0.7]),
    ("p", "By split, the macro-averaged scores stay essentially perfect, with the validation split clean "
          "and the energy head accounting for the only false positives:"),
    ("table", ["Split", "Windows", "Macro-F1", "Macro FP rate"],
     [["Training", "5,400", "0.99924", "0.00076"],
      ["Validation", "1,800", "1.00000", "0.00000"],
      ["Test", "1,800", "0.99958", "0.00042"],
      ["Overall", "9,000", "0.99946", "0.00054"]],
     [1.6, 1.4, 1.9, 1.9]),
    ("callout", "Why this is more believable than a clean sweep",
     "The energy head missing precision by a hair (13 false positives across 6,000 healthy windows) is "
     "exactly what you expect from the fuzziest target — energy waste is a matter of degree, not a sharp "
     "fault. A model that scored a flawless 1.0 on everything, including the subjective head, would be "
     "more suspicious, not less.", "info"),

    ("h1", "What the model learned (feature evidence)", "6"),
    ("p", "Because the heads are transparent, we can read *why* each decides as it does. The evidence each "
          "leans on is physically sensible — this is not a black box."),
    ("table", ["Head", "Top evidence for the fault", "Top evidence against"],
     [["Pump wear", "vibration, bearing temp, pump power, maintenance context", "high suction pressure, high flow"],
      ["Oscillation", "valve-stiction proxy, load transitions, reversal density", "stable flow autocorrelation"],
      ["Sensor drift", "large temperature-model residual, error zero-crossings", "consistent error autocorrelation"],
      ["Energy", "energy consumption, vibration, health risk, actuator activity", "clean startup, stable suction"]],
     [1.3, 3.0, 2.5]),
    ("p", "Note how the pump-wear head weighs **low suction pressure** and **low flow** as evidence *for* "
          "cavitation, and high pressure/flow *against* it — the physics of cavitation, learned directly "
          "from the coupled skid behaviour."),

    ("h1", "Layer 2 — Scenario behaviour (nine runs)", "7"),
    ("p", "The deterministic runner exercised all nine scenarios. Selected KPIs show the supervisory and "
          "safety character (full metrics are in each scenario's `metrics.json`):"),
    ("table", ["Scenario", "Energy kWh", "Reversals", "Interlock lat. s", "MQTT avail.", "Note"],
     [["normal", "0.563", "351", "—", "—", "Baseline operation"],
      ["pump-wear", "0.531", "88", "—", "—", "Fault evidence ~58 s in"],
      ["oscillation", "0.515", "289", "—", "—", "Tuning degradation diagnosed"],
      ["temperature-sensor-drift", "0.440", "260", "—", "—", "Attributed to sensor"],
      ["high-temperature-interlock", "0.076", "3", "0.1", "—", "Cooling+heater fail-safe; 31.9 s safe-out"],
      ["mqtt-outage", "0.453", "0", "—", "1.0", "Control unaffected"],
      ["opcua-outage", "0.409", "2", "—", "—", "Local fail-safe applied"]],
     [2.2, 1.0, 1.0, 1.1, 0.9, 2.0]),
    ("p", "The story is consistent: the model adds diagnosis and economic context, while the deterministic "
          "layer keeps the plant safe. At the interlock trip, energy collapses to 0.076 kWh (heater off) "
          "and the hard rule fires in ~0.1 s — owned by the PLC/SIS, not the model."),

    ("h1", "Layer 3 — Acceptance checks (the safety boundary)", "8"),
    ("p", "The runner records explicit evidence that the commercial safety boundary holds. All checks pass:"),
    ("table", ["Acceptance check", "Result"],
     [["MQTT advisories are separate from actuator command tags", "Pass"],
      ["High-temperature interlock writes cooling + heater fail-safe", "Pass"],
      ["MQTT outage leaves fast-loop control availability at 1.0", "Pass"],
      ["OPC UA outage applies local fail-safe values", "Pass"],
      ["Each scenario produces deterministic traces + baseline comparison", "Pass"],
      ["Advisory JSON includes owning neuron, recommendation, economic basis", "Pass"],
      ["Advisory JSON includes safety-envelope result and the PLC-vs-Jneopallium boundary", "Pass"]],
     [5.4, 1.4]),

    ("h1", "Layer 4 — Production-scale evidence and the deployable network", "9"),
    ("p", "The production-scale run records a 100 GiB *logical* corpus target by deterministically "
          "replicating the bundled skid corpus, without writing 100 GiB to disk:"),
    ("table", ["Measure", "Value"],
     [["Fitted training windows", "9,000 (9 scenarios)"],
      ["Fitted reference multiplier", "1,000"],
      ["Estimated fitted corpus", "15.46 MiB"],
      ["Effective reference multiplier", "6,624,764"],
      ["Effective example count", "59,622,876"],
      ["Target corpus size", "100.00 GiB"],
      ["Target reach ratio", "0.9999999 (≈100%)"]],
     [3.2, 3.6]),
    ("p", "The run also produced and validated the **deployable JNeopallium network**: five layer files "
          "plus `model-descriptor.json` describing 5 layers, 17 real neurons, 156 trainable weights, and 4 "
          "biases — each neuron referencing a concrete runtime industrial class — and a ready-made "
          "`production-context.json`. The trained heads, feature gates, response planning, and the fixed "
          "safety gate are embedded directly, so **the artifact that was tested is the artifact that "
          "deploys**."),

    ("h1", "The honest limitations (read this carefully)", "10"),
    ("num", "**The reference corpus is synthetic and deterministic.** It is a physically-coupled skid "
            "simulation, built so each fault is separable. Near-perfect scores confirm the pipeline and the "
            "diagnostic separation — not real-world accuracy."),
    ("num", "**Real accuracy must be earned on external data.** Genuine production claims require site "
            "historian, CMMS, energy-meter, and maintenance-ticket validation, with a simple-algorithm "
            "baseline, a false-positive review, and detection-lead-time evidence."),
    ("num", "**This is simulation / HIL evidence, not a certified controller.** It does not replace PLC/SIS "
            "validation, hazard analysis, management of change, or site acceptance testing. The product is "
            "advisory and supervisory by design."),
    ("callout", "Why we lead with the caveat",
     "A supervisory product that blurs the line between pipeline evidence and field accuracy cannot be "
     "trusted near a plant. The honesty is part of the product: every advisory carries its economic basis "
     "and safety-envelope result, and every claim in this report carries its scope.", "info"),

    ("h1", "Overall verdict", "11"),
    ("p", "**The Industrial Loop Guardian passes every test at every level on the reference skid corpus.** "
          "The four diagnostic heads separate their faults with honest, well-behaved confidence; the "
          "running system diagnoses correctly across nine scenarios while the deterministic controls keep "
          "the plant safe; the OPC UA / MQTT / PLC boundary holds, including outages and interlock trips; "
          "and the training pipeline is correct, bounded, and reproducible at a 100 GiB logical scale."),
    ("p", "The system is **ready for the next stage**: offline replay and shadow-pilot validation on "
          "real plant data, which is the only thing that can convert this strong pipeline evidence into a "
          "field-accuracy claim. The architecture, the safety boundary, and the evidence discipline needed "
          "for that stage are already in place."),

    ("h1", "Glossary", "12"),
    ("table", ["Term", "Meaning"],
     [["Finding head", "A trained detector for one fault type (e.g. pump wear)"],
      ["Multi-label", "Several independent yes/no findings can be true at once"],
      ["Precision / recall", "Share of flags that were right / share of real faults that were caught"],
      ["Macro-F1", "F1 averaged equally across the four heads"],
      ["IAE", "Integral of absolute error — total control tracking error"],
      ["Interlock latency", "How quickly a hard safety rule fired"],
      ["Availability", "Fraction of time fast control stayed up"],
      ["HIL", "Hardware-/simulation-in-the-loop testing"],
      ["Deterministic", "Same input and command always produce the same output"]],
     [2.2, 4.6]),
    ("spacer", 8),
    ("pi", "Jneopallium Industrial Loop Guardian · Complete Test Report. Results are pipeline evidence on a "
           "deterministic reference skid corpus — simulation/HIL, not a certified controller or a "
           "real-world accuracy claim. Safety mode: ADVISORY. License: BSD 3-Clause."),
]


_UK = [
    ("cover", "Jneopallium · Industrial Loop Guardian",
     "Повний звіт про тестування",
     "Демо FMI-скіда — якість моделі, поведінка за сценаріями, докази та чесне прочитання",
     [("Документ", "Звіт про верифікацію та тестування"),
      ("Продукт", "Jneopallium Industrial Loop Guardian (демо FMI-скіда)"),
      ("Модель", "industrial-loop-guardian 1.0.0-reference-maintenance-energy"),
      ("Обсяг тестів", "Якість моделі, поведінка за сценаріями, межа безпеки, масштаб"),
      ("Результат", "Усі перевірки пройдено (еталонний корпус скіда)"),
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
      "Рівень 1 — Якість моделі діагностичних голів",
      "Чого навчилася модель (докази ознак)",
      "Рівень 2 — Поведінка за сценаріями (дев'ять запусків)",
      "Рівень 3 — Приймальні перевірки (межа безпеки)",
      "Рівень 4 — Докази масштабу та готова до розгортання мережа",
      "Чесні обмеження (прочитайте уважно)",
      "Загальний вердикт",
      "Словник"]),

    ("h1", "Стислий огляд результатів", "1"),
    ("p", "Industrial Loop Guardian перевірено на чотирьох незалежних рівнях: **якість моделі** (чотири "
          "діагностичні голови відділяють кожну несправність від нормальної роботи), **поведінка за "
          "сценаріями** (робоче демо ухвалює правильні рішення на дев'яти сценаріях заводу), **приймальні "
          "перевірки** (межа безпеки OPC UA / MQTT / PLC тримається) та **масштаб** (конвеєр навчання "
          "витримує логічну ціль корпусу 100 ГіБ). Усі перевірки пройдено."),
    ("table", ["Рівень тестів", "Що перевіряє", "Результат"],
     [["Якість моделі", "Точність / повнота / F1 / рівень хибних на голову", "**macro-F1 ≈ 0.9995**, майже нуль хибних"],
      ["Поведінка за сценаріями", "Дев'ять детермінованих сценаріїв заводу", "**Пройдено** — діагностика + безпека як очікувалось"],
      ["Приймальні", "Межа OPC UA / MQTT / PLC", "**Пройдено** — межа тримається в кожному сценарії"],
      ["Масштаб", "Логічна ціль 100 ГіБ і запобіжники", "**Пройдено** — ціль досягнуто, вибірка обмежена"]],
     [1.8, 3.0, 2.0]),
    ("callout", "Найважливіше речення цього звіту",
     "Майже ідеальні оцінки нижче — це докази КОНВЕЄРА на детермінованому синтетичному корпусі скіда: вони "
     "доводять, що система правильно з'єднана й чотири несправності чисто роздільні. Це НЕ твердження про "
     "реальну точність, яку треба заслужити на зовнішніх даних історіана, CMMS та лічильників енергії. Ми "
     "зазначаємо це тут, на початку, навмисно.", "warning"),

    ("h1", "Що ми тестували і навіщо", "2"),
    ("p", "Наглядовий продукт заслуговує довіру, коли його тестують так, як він використовуватиметься — не "
          "просто «чи запускається», а «чи правильно діагностує, чи лишається в межі безпеки й чи може "
          "пояснити себе». Тому верифікація багаторівнева:"),
    ("bullet", "**Навчені голови** — на вікнах поведінки заводу, чи відділяє кожна голова свою несправність "
               "(знос насоса, коливання, дрейф датчика, погіршення енергії) від нормальної роботи і чи її "
               "хибні спрацювання в межах бюджету?"),
    ("bullet", "**Робоча система** — на дев'яти реалістичних сценаріях, чи правильно діагностує, поки "
               "детерміноване керування тримає завод безпечним?"),
    ("bullet", "**Межа безпеки** — чи зберігають OPC UA, MQTT і PLC/SIS свої ролі в кожному сценарії, "
               "включно з відмовами й спрацюваннями інтерлоків?"),
    ("bullet", "**Конвеєр у масштабі** — чи лишається навчання правильним, обмеженим і відтворюваним при "
               "логічному корпусі 100 ГіБ?"),

    ("h1", "Тестове середовище", "3"),
    ("table", ["Пункт", "Значення"],
     [["Платформа", "Багатомодульний проєкт Maven Jneopallium (master + worker)"],
      ["Середовище виконання", "Worker/контролер на Java 17+; тренер на Python 3 (без сторонніх пакетів)"],
      ["Модель під тестом", "industrial-loop-guardian, 1.0.0-reference-maintenance-energy"],
      ["Задача", "Багатоміткова: чотири незалежні діагностичні голови висновків"],
      ["Кількість ознак", "39 інженерних ознак"],
      ["Режим безпеки", "ADVISORY, наглядовий над PLC/PID/SIS"],
      ["Еталонна команда", "train_loop_guardian_model.py --reference-multiplier 1000 --target-corpus-bytes 100gb …"],
      ["Детермінізм", "Повністю відтворювано: та сама команда → та сама контрольна сума й метрики"]],
     [2.0, 4.8]),

    ("h1", "Метрики простою мовою", "4"),
    ("p", "З'являються дві сім'ї чисел. Перша — **метрики класифікації** для діагностичних голів, де "
          "«приклад» — це вікно поведінки заводу, яке модель позначає як таке, що має несправність, чи ні:"),
    ("table", ["Метрика", "Значення простою мовою", "Найкраще"],
     [["Точність", "Зі вікон, позначених несправністю, скільки справді її мали", "1.0 (без хибних тривог)"],
      ["Повнота", "Зі справжніх несправностей скільки зловлено", "1.0 (нічого не пропущено)"],
      ["F1", "Єдина справедлива суміш точності й повноти", "1.0"],
      ["Рівень хибних спрацювань", "Як часто «кричала вовк» на здорових вікнах", "0.0"],
      ["Macro-F1", "F1, усереднений порівну по всіх чотирьох головах", "1.0"]],
     [1.8, 3.6, 1.4]),
    ("p", "Друга — **KPI керування** для кожного сценарію, що описують поведінку заводу, а не моделі: "
          "**IAE** (сумарна похибка стеження), **перерегулювання**, **час усталення**, **енергія** (кВт·год), "
          "**затримка інтерлока** (як швидко спрацювало жорстке правило) та **доступність** (частка часу, "
          "коли швидке керування лишалося справним)."),

    ("h1", "Рівень 1 — Якість моделі діагностичних голів", "5"),
    ("p", "Кожну голову оцінено на відкладених вікнах із розбиттям без витоку. Три голови ідеальні; "
          "енергетична — найважча, найсуб'єктивніша ціль — має малий, чесний рівень хибних спрацювань. "
          "Загальні результати по всіх 9 000 вікнах:"),
    ("table", ["Голова висновку", "Позитиви", "Точність", "Повнота", "F1", "Рівень хибних"],
     [["Знос насоса / ризик кавітації", "1 000", "1.0", "1.0", "1.0", "0.0"],
      ["Коливання контуру / розладнання", "1 000", "1.0", "1.0", "1.0", "0.0"],
      ["Дрейф датчика температури", "1 000", "1.0", "1.0", "1.0", "0.0"],
      ["Погіршення енергії на одиницю", "3 000", "0.9957", "1.0", "0.9978", "0.0022"]],
     [2.6, 1.0, 1.0, 0.9, 0.6, 0.7]),
    ("p", "За розбиттям макро-усереднені оцінки лишаються по суті ідеальними: валідаційне розбиття чисте, а "
          "енергетична голова дає єдині хибні спрацювання:"),
    ("table", ["Розбиття", "Вікна", "Macro-F1", "Macro рівень хибних"],
     [["Навчання", "5 400", "0.99924", "0.00076"],
      ["Валідація", "1 800", "1.00000", "0.00000"],
      ["Тест", "1 800", "0.99958", "0.00042"],
      ["Загалом", "9 000", "0.99946", "0.00054"]],
     [1.6, 1.4, 1.9, 1.9]),
    ("callout", "Чому це переконливіше за абсолютний нуль помилок",
     "Те, що енергетична голова не дотягує точності на волосину (13 хибних спрацювань на 6 000 здорових "
     "вікон), — саме те, що очікуєш від найрозмитішої цілі: марнування енергії — питання міри, а не різкої "
     "несправності. Модель, що показала б бездоганну 1.0 на всьому, включно із суб'єктивною головою, була "
     "б підозрілішою, а не навпаки.", "info"),

    ("h1", "Чого навчилася модель (докази ознак)", "6"),
    ("p", "Оскільки голови прозорі, ми можемо прочитати, *чому* кожна вирішує так, як вирішує. Докази, на "
          "які кожна спирається, фізично розумні — це не чорна скриня."),
    ("table", ["Голова", "Найвагоміші докази за несправність", "Найвагоміші проти"],
     [["Знос насоса", "вібрація, темп. підшипника, потужність, контекст обслуговування", "високий тиск, висока витрата"],
      ["Коливання", "проксі заклинювання, переходи навантаження, реверси", "стабільна автокореляція витрати"],
      ["Дрейф датчика", "великий залишок моделі температури, перетини нуля", "стійка автокореляція похибки"],
      ["Енергія", "споживання енергії, вібрація, ризик здоров'я, активність", "чистий запуск, стабільне всмоктування"]],
     [1.3, 3.0, 2.5]),
    ("p", "Зверніть увагу, як голова зносу насоса зважує **низький тиск на всмоктуванні** й **низьку "
          "витрату** як докази *за* кавітацію, а високі тиск/витрату — *проти*: фізика кавітації, вивчена "
          "прямо зі зв'язаної поведінки скіда."),

    ("h1", "Рівень 2 — Поведінка за сценаріями (дев'ять запусків)", "7"),
    ("p", "Детермінований раннер проганяє всі дев'ять сценаріїв. Вибрані KPI показують наглядовий і "
          "безпековий характер (повні метрики — у `metrics.json` кожного сценарію):"),
    ("table", ["Сценарій", "Енергія кВт·год", "Реверси", "Затр. інтерл. с", "Дост. MQTT", "Примітка"],
     [["normal", "0.563", "351", "—", "—", "Базова робота"],
      ["pump-wear", "0.531", "88", "—", "—", "Докази несправності ~58 с"],
      ["oscillation", "0.515", "289", "—", "—", "Діагностовано розладнання"],
      ["temperature-sensor-drift", "0.440", "260", "—", "—", "Приписано датчику"],
      ["high-temperature-interlock", "0.076", "3", "0.1", "—", "Fail-safe; 31.9 с поза межами"],
      ["mqtt-outage", "0.453", "0", "—", "1.0", "Керування не зачеплено"],
      ["opcua-outage", "0.409", "2", "—", "—", "Застосовано локальний fail-safe"]],
     [2.3, 1.1, 0.9, 1.1, 0.9, 1.9]),
    ("p", "Історія узгоджена: модель додає діагностику й економічний контекст, а детермінований рівень "
          "тримає завод безпечним. На спрацюванні інтерлока енергія падає до 0.076 кВт·год (нагрівач "
          "вимкнено), а жорстке правило спрацьовує за ~0.1 с — належить PLC/SIS, а не моделі."),

    ("h1", "Рівень 3 — Приймальні перевірки (межа безпеки)", "8"),
    ("p", "Раннер записує явні докази того, що комерційна межа безпеки тримається. Усі перевірки пройдено:"),
    ("table", ["Приймальна перевірка", "Результат"],
     [["Рекомендації MQTT відокремлені від тегів команд виконавчих механізмів", "Пройдено"],
      ["Інтерлок високої температури пише fail-safe охолодження + нагрівача", "Пройдено"],
      ["Відмова MQTT лишає доступність швидкого контуру на 1.0", "Пройдено"],
      ["Відмова OPC UA застосовує локальні безпечні значення", "Пройдено"],
      ["Кожен сценарій дає детерміновані траси + порівняння з базовою лінією", "Пройдено"],
      ["Рекомендаційний JSON містить нейрон-власник, рекомендацію, економічний базис", "Пройдено"],
      ["Рекомендаційний JSON містить результат межі безпеки та межу PLC-vs-Jneopallium", "Пройдено"]],
     [5.4, 1.4]),

    ("h1", "Рівень 4 — Докази масштабу та готова до розгортання мережа", "9"),
    ("p", "Запуск промислового масштабу фіксує *логічну* ціль корпусу 100 ГіБ, детерміновано реплікуючи "
          "вбудований корпус скіда, без запису 100 ГіБ на диск:"),
    ("table", ["Показник", "Значення"],
     [["Припасовані вікна навчання", "9 000 (9 сценаріїв)"],
      ["Припасований множник еталона", "1 000"],
      ["Оцінений припасований корпус", "15.46 МіБ"],
      ["Ефективний множник еталона", "6 624 764"],
      ["Ефективна кількість прикладів", "59 622 876"],
      ["Цільовий розмір корпусу", "100.00 ГіБ"],
      ["Коефіцієнт досягнення цілі", "0.9999999 (≈100%)"]],
     [3.2, 3.6]),
    ("p", "Запуск також створив і перевірив **готову до розгортання мережу JNeopallium**: п'ять файлів "
          "рівнів плюс `model-descriptor.json`, що описує 5 рівнів, 17 справжніх нейронів, 156 навчуваних "
          "ваг і 4 зсуви — кожен нейрон посилається на конкретний промисловий клас часу виконання — та "
          "готовий `production-context.json`. Навчені голови, фільтри ознак, планування відповіді та "
          "фіксований запобіжник вбудовані прямо, тож **протестований артефакт — це той самий артефакт, що "
          "розгортається**."),

    ("h1", "Чесні обмеження (прочитайте уважно)", "10"),
    ("num", "**Еталонний корпус синтетичний і детермінований.** Це фізично зв'язана симуляція скіда, "
            "побудована так, щоб кожна несправність була роздільною. Майже ідеальні оцінки підтверджують "
            "конвеєр і діагностичне розділення — не реальну точність."),
    ("num", "**Реальну точність треба заслужити на зовнішніх даних.** Справжні промислові твердження "
            "потребують валідації на історіані майданчика, CMMS, лічильниках енергії та нарядах "
            "обслуговування, з базовою лінією простого алгоритму, переглядом хибних спрацювань і доказом "
            "часу випередження виявлення."),
    ("num", "**Це доказ симуляції / HIL, а не сертифікований контролер.** Він не замінює валідацію PLC/SIS, "
            "аналіз небезпек, керування змінами чи приймальні випробування на майданчику. Продукт "
            "рекомендаційний і наглядовий за задумом."),
    ("callout", "Чому ми починаємо із застереження",
     "Наглядовий продукт, що розмиває межу між доказами конвеєра й польовою точністю, не можна довірити "
     "поряд із заводом. Чесність — частина продукту: кожна рекомендація несе свій економічний базис і "
     "результат межі безпеки, а кожне твердження звіту несе свій обсяг.", "info"),

    ("h1", "Загальний вердикт", "11"),
    ("p", "**Industrial Loop Guardian проходить кожен тест на кожному рівні на еталонному корпусі скіда.** "
          "Чотири діагностичні голови відділяють свої несправності з чесною, добре поведеною впевненістю; "
          "робоча система правильно діагностує на дев'яти сценаріях, поки детерміноване керування тримає "
          "завод безпечним; межа OPC UA / MQTT / PLC тримається, включно з відмовами й спрацюваннями "
          "інтерлоків; а конвеєр навчання правильний, обмежений і відтворюваний у логічному масштабі 100 ГіБ."),
    ("p", "Система **готова до наступного етапу**: офлайн-відтворення й валідація тіньовим пілотом на "
          "реальних даних заводу — єдиного, що може перетворити ці сильні докази конвеєра на твердження "
          "про польову точність. Архітектура, межа безпеки та дисципліна доказів для цього етапу вже на "
          "місці."),

    ("h1", "Словник", "12"),
    ("table", ["Термін", "Значення"],
     [["Голова висновку", "Навчений детектор одного типу несправності (напр. знос насоса)"],
      ["Багатоміткова", "Кілька незалежних висновків «так/ні» можуть бути істинними водночас"],
      ["Точність / повнота", "Частка правильних позначок / частка зловлених справжніх несправностей"],
      ["Macro-F1", "F1, усереднений порівну по чотирьох головах"],
      ["IAE", "Інтеграл абсолютної похибки — сумарна похибка стеження"],
      ["Затримка інтерлока", "Як швидко спрацювало жорстке правило безпеки"],
      ["Доступність", "Частка часу, коли швидке керування лишалося справним"],
      ["HIL", "Тестування з апаратурою/симуляцією в контурі"],
      ["Детермінований", "Той самий вхід і команда завжди дають той самий результат"]],
     [2.2, 4.6]),
    ("spacer", 8),
    ("pi", "Jneopallium Industrial Loop Guardian · Повний звіт про тестування. Результати — це докази "
           "конвеєра на детермінованому еталонному корпусі скіда (симуляція/HIL), а не сертифікований "
           "контролер чи твердження про реальну точність. Режим безпеки: ADVISORY. Ліцензія: BSD 3-Clause."),
]
