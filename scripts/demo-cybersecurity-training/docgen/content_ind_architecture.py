# -*- coding: utf-8 -*-
"""Industrial Loop Guardian — architecture article (EN + UK), block DSL."""

from __future__ import annotations

DATE = "23 June 2026"
DATE_UK = "23 червня 2026"


def architecture(lang: str) -> list:
    return _EN if lang == "en" else _UK


_EN = [
    ("cover", "Jneopallium · Industrial Loop Guardian",
     "A Supervisory Brain for Your Plant",
     "Cross-loop, cross-timescale diagnosis above PLC/PID/SIS — complete architecture, explained",
     [("Document", "Architecture & Technical Overview"),
      ("Product", "Jneopallium Industrial Loop Guardian (FMI Skid Demo)"),
      ("Model", "industrial-loop-guardian 1.0.0"),
      ("Safety mode", "ADVISORY (supervisory, recommend-only)"),
      ("Author", "Dmytro Rakovskyi — Kharkiv, Ukraine"),
      ("Date", DATE),
      ("Audience", "Plant managers, controls engineers, executives, newcomers"),
      ("License", "BSD 3-Clause")],
     "Architecture Article",
     "How a brain-inspired supervisory layer turns the OPC UA and MQTT telemetry you already have into "
     "early diagnosis of pump wear, control-loop oscillation, sensor drift, and wasted energy — without "
     "ever touching your deterministic controls. Written so a non-specialist can follow every step."),

    ("toc", "What's inside",
     ["Executive summary — the one-page version",
      "The problem: why good controls still lose money",
      "The big idea: supervise, don't replace",
      "The platform underneath: the Jneopallium framework",
      "Architecture overview: how the pieces fit together",
      "The skid it supervises",
      "The protocol boundary: OPC UA, MQTT, and PLC/SIS",
      "Signals: the common language, on many clocks",
      "The four things it diagnoses",
      "The trained model and the deployable network",
      "Safety by design: why it cannot trip your plant",
      "Deployment topology: where it runs",
      "End-to-end walkthrough: nine scenarios",
      "Glossary for non-specialists"]),

    ("h1", "Executive summary", "1"),
    ("p", "A good PID controller holds one variable — a temperature, a flow, a level — beautifully. But a "
          "plant rarely loses money on a single well-tuned loop. It loses money on the things that **no "
          "single loop can see**: a pump quietly wearing out, a control loop slowly drifting out of tune, "
          "a temperature sensor creeping off-calibration, and a combination of controllers that each "
          "behave correctly while together burning more energy than they should."),
    ("p", "The **Jneopallium Industrial Loop Guardian** is a supervisory layer that sits **above** your "
          "PLC, PID, and safety systems — never instead of them. It reads the OPC UA and MQTT telemetry "
          "you already produce, correlates signals across many timescales (milliseconds to weeks), and "
          "emits a single, evidenced advisory: what is happening, why, how urgent it is, and what it is "
          "worth in money. It never blocks, never trips, never overrides a safety interlock."),
    ("callout", "The one sentence to remember",
     "Your PLC/PID/SIS keeps doing the fast, deterministic control and hard safety. The Loop Guardian adds "
     "cross-loop, cross-timescale operational intelligence on top — diagnosis, energy optimisation, and "
     "maintenance planning — and only ever recommends.", "info"),
    ("p", "Its design is borrowed from the brain: fast reflexes and slow judgement running at the same "
          "time, with specialised \"neurons\" that combine signals other tools look at in isolation. The "
          "result is a product that is **easy to trust** — it runs in ADVISORY mode, keeps every "
          "deterministic control authoritative, and explains every recommendation."),

    ("h1", "The problem: why good controls still lose money", "2"),
    ("h2", "A single loop is not where the money leaks"),
    ("p", "Classic controls and threshold alarms look at one variable at a time. That is exactly right for "
          "fast, stable regulation — and exactly wrong for the expensive, slow-moving problems:"),
    ("bullet", "**Hidden degradation.** A bearing wears over weeks. \"Bearing temperature > 85 °C → alarm\" "
               "fires only at the end — after the damage. The early evidence is spread across vibration, "
               "suction pressure, power-per-unit-flow, and running hours, on different clocks."),
    ("bullet", "**Cross-loop waste.** A heater raises temperature while a cooling valve removes heat while "
               "a pump runs faster than needed. Each local controller is \"correct\"; the combination "
               "wastes energy no single loop can detect."),
    ("bullet", "**Nuisance alarms.** The same rising temperature is normal at startup and alarming in "
               "steady state. Context-free thresholds either over-alarm or miss the real event."),
    ("bullet", "**Sensor-fault ambiguity.** Is the process actually getting hotter, or is one sensor "
               "drifting? Majority voting cannot tell; the answer needs the physical model, the actuator "
               "commands, and the maintenance history together."),
    ("h2", "What that costs"),
    ("p", "Unplanned shutdowns, unnecessary preventive maintenance, shortened equipment life, and quietly "
          "elevated energy bills. Every one of these is a decision that depends on **several signals "
          "evolving over different timescales** — precisely the case where a supervisory, multi-timescale "
          "architecture earns its keep, and a single PID loop cannot."),

    ("h1", "The big idea: supervise, don't replace", "3"),
    ("p", "The commercially credible — and safe — design is a hybrid. Keep what already works; add what it "
          "cannot do:"),
    ("code",
     "PLC / PID / SIS\n"
     "    +-- deterministic millisecond control and hard safety   (unchanged)\n"
     "Jneopallium Industrial Loop Guardian  (supervisory, above)\n"
     "    +-- multi-loop coordination\n"
     "    +-- oscillation and sensor-fault diagnosis\n"
     "    +-- energy optimisation\n"
     "    +-- degradation prediction and maintenance planning\n"
     "    +-- bounded setpoint recommendations (advisory)"),
    ("p", "A PID controller usually **beats** a sophisticated AI on cost, predictability, validation "
          "effort, and reliability when controlling one stable variable. So we do not compete there. The "
          "Loop Guardian becomes valuable only when a decision depends on many signals over many "
          "timescales — diagnosis, economic ranking, and planning."),
    ("callout", "Where it does and does not help",
     "High value: predictive maintenance, oscillation diagnosis, sensor-drift discrimination, energy and "
     "setpoint optimisation, batch-context anomaly detection. Low or no value: a single stable regulatory "
     "loop, and hard interlocks / emergency shutdown — which must remain deterministic. The product is "
     "designed to respect that line.", "success"),

    ("h1", "The platform underneath: the Jneopallium framework", "4"),
    ("p", "The Loop Guardian is one application of **Jneopallium**, a Java framework for building "
          "biologically-grounded neuron networks (published in the *International Journal of Science and "
          "Research*, 2024). Three of its ideas explain why it is good at supervisory work."),
    ("h2", "1. Typed signals — meaning, not just numbers"),
    ("p", "A flow measurement, a vibration reading, a maintenance work-order, and an energy meter are "
          "*different kinds of signal* with different meaning and urgency. The engine routes each to the "
          "right specialist and keeps the evidence intact all the way to the advisory."),
    ("h2", "2. Many clocks — fast reflexes, slow judgement"),
    ("p", "Biology runs on many clocks at once. Jneopallium copies this: fast signals (measurements, "
          "commands, interlocks) are processed every loop; slower context (degradation, efficiency, "
          "maintenance windows) is processed every 10 to 60 loops. A pump-health neuron can weigh "
          "instantaneous hydraulics against a multi-week wear trend in one coherent judgement."),
    ("table", ["Signal", "Cadence", "Function"],
     [["Flow, pressure, interlocks", "Every tick", "Immediate hydraulic condition and safety state"],
      ["Pump command vs. actual speed", "Every tick", "Actuator tracking"],
      ["Vibration RMS", "Every 5–10 ticks", "Mechanical anomaly"],
      ["Bearing temperature, power", "Every 10 ticks", "Thermal/mechanical condition, efficiency"],
      ["Production load", "Every ~50 ticks", "Operating context"],
      ["Degradation estimate", "Hundreds of ticks", "Long-term health"],
      ["Maintenance schedule", "Thousands of ticks", "Planning context"]],
     [2.4, 1.7, 2.7]),
    ("h2", "3. Stateless, swappable specialists"),
    ("p", "Each processing step is a small, stateless processor wired to a neuron through an interface. Any "
          "detector can be upgraded or replaced for a particular site without touching the rest — a lab "
          "runs a simple model; a refinery plugs in a richer one behind the same interface."),

    ("h1", "Architecture overview: how the pieces fit together", "5"),
    ("p", "Telemetry flows one way through increasingly intelligent stages; an auditable advisory flows "
          "out the other end:"),
    ("code",
     "FMI skid / OPC UA / MQTT / Kafka telemetry\n"
     "   -> fast telemetry validation and loop state\n"
     "   -> trained diagnostic finding heads\n"
     "   -> economic advisory planning + fixed safety gate\n"
     "   -> industrial advisory JSONL (asset, finding, confidence, economic basis)"),
    ("p", "The trained network is five layers of seventeen real neurons:"),
    ("table", ["Layer", "Size", "Job, in plain language"],
     [["0 — Input", "—", "Plant + supervisory-context boundary (OPC UA, MQTT, FMI replay, Kafka)"],
      ["1 — Fast telemetry", "7", "Validate measurements, track interlocks, override, fast loop state"],
      ["2 — Diagnostic heads", "4", "Trained detectors: pump wear, oscillation, sensor drift, energy"],
      ["3 — Advisory planning", "4", "Maintenance scheduling, bounded trim, economic basis, safety gate"],
      ["4 — Result", "2", "Emit maintenance and energy advisories as JSONL"]],
     [2.0, 0.8, 3.9]),

    ("h1", "The skid it supervises", "6"),
    ("p", "The demo runs a deterministic **heated circulation and heat-exchanger skid**, simulated with an "
          "FMI 2.0 Co-Simulation FMU and driven through a real protocol gateway. It is rich enough to be a "
          "commercially testable prototype — it models the physics where the money actually leaks:"),
    ("bullet", "Process and measured temperature, circulation flow, suction pressure."),
    ("bullet", "Pump speed and power, cooling-valve position, heater power."),
    ("bullet", "Vibration RMS and bearing temperature (the wear signals)."),
    ("bullet", "Injectable faults: valve sticking, pump wear, temperature-sensor drift, thermal runaway."),
    ("bullet", "Deterministic local interlocks: high temperature, low flow, low suction pressure."),
    ("p", "The model includes tank thermal inertia, pump and valve lag, pump-flow and power curves, "
          "vibration and bearing-temperature wear effects, and sensor drift/noise — so the diagnostic "
          "heads learn from realistic, physically coupled behaviour rather than a flat table."),

    ("h1", "The protocol boundary: OPC UA, MQTT, and PLC/SIS", "7"),
    ("p", "OPC UA and MQTT are complementary, and the demo keeps a strict, commercially sensible boundary:"),
    ("table", ["Path", "Role", "Authority"],
     [["PLC / PID / SIS", "Hard real-time control and interlocks", "Always authoritative"],
      ["OPC UA", "Bounded local actuator command path", "Only command path; tightly limited"],
      ["MQTT", "Telemetry and advisory distribution", "Never actuates (AUTONOMOUS rejected)"]],
     [1.8, 3.2, 1.8]),
    ("p", "There are exactly three OPC UA command nodes (cooling valve, pump speed, heater power), and the "
          "Java `MqttBridgeConfig` constructor structurally rejects an AUTONOMOUS mode — MQTT carries "
          "vibration, energy, status, and advisories, but its tags never match an actuator command tag. "
          "When a command is issued, it passes a fixed priority chain before any write happens:"),
    ("code",
     "hard interlock -> local fail-safe -> operator override -> safety mode\n"
     "  -> validation/quality -> clamp -> ramp limit -> diff suppression\n"
     "  -> OPC UA write -> audit"),
    ("table", ["Command", "Fail-safe default"],
     [["Cooling valve", "100% open (maximum cooling)"],
      ["Pump speed", "30%"],
      ["Heater power", "0% (off)"]],
     [3.0, 3.8]),

    ("h1", "Signals: the common language, on many clocks", "8"),
    ("p", "Everything the system sees becomes a **typed industrial signal**. Each declares how often it "
          "should be processed, realising the \"many clocks\" idea concretely:"),
    ("table", ["Signal", "Carries", "Cadence (loop)"],
     [["MeasurementSignal", "Process measurements (temp, flow, pressure…)", "Every loop"],
      ["AlarmSignal", "Plant alarms", "Every loop"],
      ["InterlockSignal", "Hard interlock state", "Every loop"],
      ["OperatorOverrideSignal", "Operator manual control", "Every loop"],
      ["ActuatorCommandSignal", "Bounded actuator command", "Every loop"],
      ["DegradationSignal", "Equipment degradation evidence", "Every 10 loops"],
      ["EfficiencySignal", "Energy / efficiency context", "Every 10 loops"],
      ["SetpointSignal", "Bounded setpoint recommendation", "Every 10 loops"],
      ["MaintenanceWindowSignal", "Planned-maintenance context", "Every 60 loops"]],
     [2.5, 2.7, 1.6]),
    ("p", "Fast signals give immediate condition and safety state; slow signals carry the context that "
          "*changes the meaning* of the fast ones — without ever slowing the fast path down."),

    ("h1", "The four things it diagnoses", "9"),
    ("p", "The trained layer holds four **diagnostic finding heads** — narrow, high-value detectors. Each "
          "leans on a coherent set of evidence (shown here in plain language, drawn from the model's "
          "learned weights):"),
    ("h3", "1 · Pump wear and cavitation risk"),
    ("p", "Rising vibration and bearing temperature, increasing pump power, and low suction pressure / low "
          "flow point to wear or cavitation — distinguished from a mere temporary high load by the "
          "*combination* and its trend."),
    ("h3", "2 · Control-loop oscillation and tuning degradation"),
    ("p", "A valve-stiction proxy (command moves, position lags), actuator reversal density, and "
          "load-transition patterns separate genuine tuning degradation from normal transients."),
    ("h3", "3 · Temperature-sensor drift"),
    ("p", "A physical temperature model produces a residual; a large, persistent residual with no matching "
          "power or process response means the **sensor** is drifting, not the process — avoiding an "
          "unnecessary shutdown."),
    ("h3", "4 · Energy-per-unit-production deterioration"),
    ("p", "Energy consumption relative to production, combined with health risk and actuator activity, "
          "flags a loop that holds its variable correctly while quietly wasting energy."),

    ("h1", "The trained model and the deployable network", "10"),
    ("p", "Bundled with the product is a checked-in reference model, `industrial-loop-guardian`. Training "
          "does not stop at a set of weights — it emits a **complete, deployable JNeopallium network**: "
          "five layers, seventeen real neurons, 39 features, 156 trainable weights and 4 biases, written "
          "into ready-to-load layer-configuration files that reference concrete runtime classes."),
    ("table", ["Generated layer", "Real neurons / role"],
     [["Fast telemetry", "SensorNeuron, MeasurementValidatorNeuron, OscillationMonitorNeuron, …"],
      ["Diagnostic heads", "DegradationModelNeuron, EnergyAccountingNeuron (the four finding heads)"],
      ["Advisory planning", "MaintenanceSchedulingNeuron, SetpointOptimiserNeuron, EconomicBasisNeuron, SafetyGateNeuron"],
      ["Result", "Advisory JSONL output neurons"]],
     [1.9, 4.9]),
    ("p", "Each diagnostic head also records a **logical role** (e.g. `PumpHealthAndEfficiencyNeuron`, "
          "`OscillationDiagnosisNeuron`, `SensorFaultDiscriminationNeuron`, `EconomicBasisNeuron`, "
          "`SafetyEnvelopeNeuron`), the features it is allowed to use (`featureGate`), and its "
          "`ownedReasoning` — so a production reviewer can see that the logic is encapsulated inside the "
          "Jneopallium model, not hidden in glue code."),
    ("callout", "Read this honestly",
     "On the bundled, deterministic reference skid the model scores near-perfectly (macro-F1 ≈ 0.9995, "
     "and only a handful of energy-head false positives out of thousands). That demonstrates the pipeline "
     "and the diagnostic separation — it is NOT a claim of real-world accuracy, which must be earned on "
     "external historian, CMMS, and energy-meter data. The Test Report states this plainly.", "warning"),

    ("h1", "Safety by design: why it cannot trip your plant", "11"),
    ("num", "**Supervisory and advisory by default.** The safety ceiling is ADVISORY. The Loop Guardian "
            "recommends; it does not control. Bounded autonomous setpoint changes require a separate, "
            "deliberately added safety case, approval workflow, and rollback path."),
    ("num", "**Deterministic controls stay authoritative.** PLC/PID/SIS own fast control and hard safety. "
            "Hard interlocks, local fail-safe logic, and operator overrides take priority over anything "
            "the model suggests — structurally, in the command priority chain."),
    ("num", "**MQTT cannot actuate.** The telemetry/advisory path is structurally barred from autonomous "
            "action; only the bounded OPC UA path can command, and only within clamps and ramp limits."),
    ("num", "**Fail-safe on loss.** On OPC UA loss the skid applies local fail-safe values; on MQTT loss "
            "fast-loop control availability stays at 1.0. Losing the supervisor never endangers the plant."),
    ("p", "And every advisory carries its **economic basis** and a **safety-envelope** result, so an "
          "engineer can see not just *what* is recommended but *why*, *what it is worth*, and *that it "
          "stays inside the configured envelope*."),

    ("h1", "Deployment topology: where it runs", "12"),
    ("table", ["Mode", "Description", "Typical use"],
     [["Local", "Single Java process", "Offline replay, pilots, edge box at the plant"],
      ["Cluster (HTTP)", "Distributed over HTTP", "Multi-asset, multi-line sites"],
      ["Cluster (gRPC)", "Distributed over gRPC; FPGA-capable", "High-throughput fleets"]],
     [1.8, 3.2, 1.8]),
    ("p", "Telemetry arrives through **bridges** that translate a protocol into typed signals: OPC UA "
          "measurement/alarm inputs, MQTT telemetry inputs, and a Kafka shadow stream. A site-specific "
          "input adapter converts your historian or live feeds into the same typed signals while "
          "preserving event time, so the same model runs on a laptop replay and a clustered plant alike."),

    ("h1", "End-to-end walkthrough: nine scenarios", "13"),
    ("p", "The deterministic runner exercises nine scenarios and records evidence for each. A few show the "
          "architecture's character:"),
    ("bullet", "**pump-wear** — degradation evidence accumulates; the pump-health head raises a "
               "maintenance advisory with an economic basis (fault evidence appears ~58 s into the run)."),
    ("bullet", "**oscillation** — the oscillation head separates tuning degradation from normal load "
               "transitions."),
    ("bullet", "**temperature-sensor-drift** — the sensor-drift head attributes the anomaly to the sensor, "
               "not the process, avoiding a needless trip."),
    ("bullet", "**high-temperature-interlock** — the hard interlock writes cooling and heater fail-safe; "
               "interlock latency is ~0.1 s, owned by the deterministic layer, not the model."),
    ("bullet", "**mqtt-outage** — fast-loop control availability stays at **1.0**; advisories pause, "
               "control does not."),
    ("bullet", "**opcua-outage** — local fail-safe values are applied; the plant stays safe without the "
               "supervisor."),
    ("callout", "The point of the scenarios",
     "In every case the model adds diagnosis and economic context, while the deterministic controls keep "
     "the plant safe and running. The supervisor improves the decision; it never owns the safety.",
     "success"),

    ("h1", "Glossary for non-specialists", "14"),
    ("table", ["Term", "Plain-language meaning"],
     [["Advisory mode", "The system recommends; humans and PLCs decide. It never trips the plant."],
      ["Cavitation", "Vapour bubbles forming at a pump's suction, damaging it — caused by low pressure."],
      ["Interlock", "A hard, deterministic safety rule (e.g. high temp → cooling fail-safe)."],
      ["FMI / FMU", "A standard way to package a plant simulation; used here to drive the demo skid."],
      ["OPC UA", "An industrial information/communication standard; here, the bounded command path."],
      ["MQTT", "A lightweight publish/subscribe protocol; here, telemetry and advisories only."],
      ["PLC / PID / SIS", "The deterministic controllers and safety system that own fast control."],
      ["Setpoint", "The target value a controller aims for (e.g. desired temperature)."],
      ["Stiction", "Static friction in a valve that makes its position lag its command."],
      ["Supervisory layer", "A layer that reasons across loops and timescales, above the controllers."],
      ["Economic basis", "The money value the system attaches to a finding (e.g. avoided shutdown)."]],
     [2.0, 4.8]),
    ("spacer", 8),
    ("pi", "Jneopallium Industrial Loop Guardian · FMI Skid Demo. Architecture article for technical and "
           "non-technical readers. Supervisory above PLC/PID/SIS. Safety mode: ADVISORY. "
           "License: BSD 3-Clause."),
]


_UK = [
    ("cover", "Jneopallium · Industrial Loop Guardian",
     "Наглядовий «мозок» для вашого заводу",
     "Міжконтурна, різночасова діагностика над PLC/PID/SIS — повна архітектура, пояснена просто",
     [("Документ", "Архітектурний і технічний огляд"),
      ("Продукт", "Jneopallium Industrial Loop Guardian (демо FMI-скіда)"),
      ("Модель", "industrial-loop-guardian 1.0.0"),
      ("Режим безпеки", "ADVISORY (наглядовий, лише рекомендації)"),
      ("Автор", "Дмитро Раковський — Харків, Україна"),
      ("Дата", DATE_UK),
      ("Аудиторія", "Керівники заводів, інженери КВПіА, керівники, новачки"),
      ("Ліцензія", "BSD 3-Clause")],
     "Архітектурна стаття",
     "Як наглядовий шар, натхненний роботою мозку, перетворює телеметрію OPC UA та MQTT, яку ви вже маєте, "
     "на раннє виявлення зносу насоса, коливань контуру, дрейфу датчиків і марнування енергії — жодного "
     "разу не торкаючись ваших детермінованих засобів керування. Написано так, щоб кожен крок був "
     "зрозумілий нефахівцю."),

    ("toc", "Зміст",
     ["Стислий огляд — версія на одну сторінку",
      "Проблема: чому хороше керування все одно втрачає гроші",
      "Головна ідея: наглядати, а не замінювати",
      "Платформа в основі: фреймворк Jneopallium",
      "Огляд архітектури: як поєднуються складові",
      "Скід, над яким він наглядає",
      "Межа протоколів: OPC UA, MQTT і PLC/SIS",
      "Сигнали: спільна мова на багатьох годинниках",
      "Чотири речі, які він діагностує",
      "Навчена модель і готова до розгортання мережа",
      "Безпека за задумом: чому він не зупинить ваш завод",
      "Топологія розгортання: де це працює",
      "Наскрізний приклад: дев'ять сценаріїв",
      "Словник для нефахівців"]),

    ("h1", "Стислий огляд", "1"),
    ("p", "Хороший PID-регулятор чудово тримає одну змінну — температуру, витрату, рівень. Але завод рідко "
          "втрачає гроші на одному добре налаштованому контурі. Він втрачає їх на тому, чого **не бачить "
          "жоден окремий контур**: насос, що тихо зношується, контур, що повільно розладнується, датчик "
          "температури, що сповзає з калібрування, і комбінація регуляторів, кожен з яких поводиться "
          "правильно, а разом вони палять більше енергії, ніж мали б."),
    ("p", "**Jneopallium Industrial Loop Guardian** — це наглядовий шар, що стоїть **над** вашими PLC, "
          "PID і системами безпеки, а не замість них. Він читає телеметрію OPC UA та MQTT, яку ви вже "
          "виробляєте, корелює сигнали на багатьох часових масштабах (від мілісекунд до тижнів) і видає "
          "одну обґрунтовану рекомендацію: що відбувається, чому, наскільки терміново і скільки це коштує "
          "в грошах. Він ніколи не блокує, не зупиняє і не скасовує запобіжний інтерлок."),
    ("callout", "Одне речення, яке варто запам'ятати",
     "Ваші PLC/PID/SIS і далі виконують швидке детерміноване керування та жорстку безпеку. Loop Guardian "
     "додає згори міжконтурну, різночасову операційну інтелектуальність — діагностику, оптимізацію енергії "
     "та планування обслуговування — і лише рекомендує.", "info"),
    ("p", "Його конструкція запозичена в мозку: швидкі рефлекси й повільні судження працюють водночас, а "
          "спеціалізовані «нейрони» поєднують сигнали, які інші інструменти розглядають окремо. Результат — "
          "продукт, якому **легко довіряти**: він працює в режимі ADVISORY, лишає кожне детерміноване "
          "керування авторитетним і пояснює кожну рекомендацію."),

    ("h1", "Проблема: чому хороше керування все одно втрачає гроші", "2"),
    ("h2", "Гроші витікають не на окремому контурі"),
    ("p", "Класичне керування й порогові тривоги дивляться на одну змінну за раз. Це саме те, що треба для "
          "швидкого стабільного регулювання, — і саме те, що не годиться для дорогих, повільних проблем:"),
    ("bullet", "**Прихований знос.** Підшипник зношується тижнями. «Температура підшипника > 85 °C → "
               "тривога» спрацьовує лише наприкінці — після шкоди. Ранні докази розкидані по вібрації, "
               "тиску на всмоктуванні, потужності на одиницю витрати та напрацюванні — на різних годинниках."),
    ("bullet", "**Міжконтурне марнування.** Нагрівач підіймає температуру, поки клапан охолодження "
               "відводить тепло, поки насос крутиться швидше, ніж треба. Кожен локальний регулятор "
               "«правильний»; комбінація марнує енергію, якої не помітить жоден контур."),
    ("bullet", "**Хибні тривоги.** Те саме зростання температури є нормою на запуску і тривогою в "
               "усталеному режимі. Пороги без контексту або перетривожують, або пропускають подію."),
    ("bullet", "**Неоднозначність несправності датчика.** Процес справді гарячіший чи один датчик "
               "дрейфує? Голосування більшістю не скаже; відповідь потребує фізичної моделі, команд "
               "виконавчих механізмів та історії обслуговування разом."),
    ("h2", "Скільки це коштує"),
    ("p", "Незаплановані зупинки, зайве профілактичне обслуговування, скорочений ресурс обладнання й тихо "
          "завищені рахунки за енергію. Кожне з цього — рішення, що залежить від **кількох сигналів, які "
          "розвиваються на різних часових масштабах**, — саме той випадок, де наглядова, різночасова "
          "архітектура виправдовує себе, а окремий PID-контур не може."),

    ("h1", "Головна ідея: наглядати, а не замінювати", "3"),
    ("p", "Комерційно переконлива — і безпечна — конструкція є гібридною. Зберігаємо те, що вже працює; "
          "додаємо те, чого воно не вміє:"),
    ("code",
     "PLC / PID / SIS\n"
     "    +-- детерміноване мілісекундне керування і жорстка безпека   (без змін)\n"
     "Jneopallium Industrial Loop Guardian  (наглядовий, згори)\n"
     "    +-- координація багатьох контурів\n"
     "    +-- діагностика коливань і несправностей датчиків\n"
     "    +-- оптимізація енергії\n"
     "    +-- прогноз деградації та планування обслуговування\n"
     "    +-- обмежені рекомендації щодо уставок (рекомендаційно)"),
    ("p", "PID-регулятор зазвичай **перевершує** складний ШІ за вартістю, передбачуваністю, зусиллями на "
          "валідацію та надійністю, коли керує однією стабільною змінною. Тож там ми не конкуруємо. Loop "
          "Guardian стає цінним лише тоді, коли рішення залежить від багатьох сигналів на багатьох "
          "часових масштабах — діагностика, економічне ранжування та планування."),
    ("callout", "Де він допомагає, а де ні",
     "Висока цінність: прогнозне обслуговування, діагностика коливань, розрізнення дрейфу датчиків, "
     "оптимізація енергії й уставок, виявлення аномалій з урахуванням фази партії. Низька або відсутня: "
     "окремий стабільний контур регулювання та жорсткі інтерлоки / аварійна зупинка, що мають лишатися "
     "детермінованими. Продукт спроєктовано поважати цю межу.", "success"),

    ("h1", "Платформа в основі: фреймворк Jneopallium", "4"),
    ("p", "Loop Guardian — це один застосунок **Jneopallium**, Java-фреймворку для побудови біологічно "
          "обґрунтованих нейронних мереж (опубліковано в *International Journal of Science and Research*, "
          "2024). Три його ідеї пояснюють, чому він добрий у наглядовій роботі."),
    ("h2", "1. Типізовані сигнали — зміст, а не лише числа"),
    ("p", "Вимірювання витрати, показник вібрації, наряд на обслуговування та лічильник енергії — це "
          "*різні види сигналів* із різним змістом і терміновістю. Рушій спрямовує кожен до потрібного "
          "фахівця й зберігає докази недоторканими аж до рекомендації."),
    ("h2", "2. Багато годинників — швидкі рефлекси, повільні судження"),
    ("p", "Біологія працює на багатьох годинниках одночасно. Jneopallium це копіює: швидкі сигнали "
          "(вимірювання, команди, інтерлоки) обробляються щоцикл; повільніший контекст (деградація, "
          "ефективність, вікна обслуговування) — кожні 10–60 циклів. Нейрон здоров'я насоса може зважити "
          "миттєву гідравліку проти багатотижневого тренду зносу в одному цілісному судженні."),
    ("table", ["Сигнал", "Каданс", "Функція"],
     [["Витрата, тиск, інтерлоки", "Щотакту", "Миттєвий гідравлічний стан і стан безпеки"],
      ["Команда насоса vs фактична швидкість", "Щотакту", "Відстеження виконавчого механізму"],
      ["СКЗ вібрації", "Кожні 5–10 тактів", "Механічна аномалія"],
      ["Температура підшипника, потужність", "Кожні 10 тактів", "Тепловий/механічний стан, ефективність"],
      ["Виробниче навантаження", "Кожні ~50 тактів", "Операційний контекст"],
      ["Оцінка деградації", "Сотні тактів", "Довгострокове здоров'я"],
      ["Графік обслуговування", "Тисячі тактів", "Контекст планування"]],
     [2.5, 1.7, 2.6]),
    ("h2", "3. Фахівці без стану, що замінюються"),
    ("p", "Кожен крок обробки — малий процесор без стану, під'єднаний до нейрона через інтерфейс. Будь-який "
          "детектор можна оновити чи замінити для конкретного майданчика, не торкаючись решти — лабораторія "
          "запускає просту модель; завод під'єднує багатшу за тим самим інтерфейсом."),

    ("h1", "Огляд архітектури: як поєднуються складові", "5"),
    ("p", "Телеметрія тече в один бік через дедалі розумніші етапи; з іншого боку виходить рекомендація, "
          "яку можна перевірити аудитом:"),
    ("code",
     "телеметрія FMI-скіда / OPC UA / MQTT / Kafka\n"
     "   -> валідація швидкої телеметрії і стан контуру\n"
     "   -> навчені діагностичні «голови» висновків\n"
     "   -> економічне планування рекомендацій + фіксований запобіжник\n"
     "   -> промисловий рекомендаційний JSONL (актив, висновок, впевненість, економіка)"),
    ("p", "Навчена мережа — це п'ять рівнів із сімнадцяти справжніх нейронів:"),
    ("table", ["Рівень", "Розмір", "Завдання, простими словами"],
     [["0 — Вхід", "—", "Межа заводу + наглядового контексту (OPC UA, MQTT, FMI-відтворення, Kafka)"],
      ["1 — Швидка телеметрія", "7", "Валідація вимірювань, інтерлоки, перевизначення, стан контуру"],
      ["2 — Діагностичні голови", "4", "Навчені детектори: знос насоса, коливання, дрейф датчика, енергія"],
      ["3 — Планування рекомендацій", "4", "Планування обслуговування, обмежений трим, економіка, запобіжник"],
      ["4 — Результат", "2", "Видача рекомендацій з обслуговування та енергії як JSONL"]],
     [2.1, 0.8, 3.8]),

    ("h1", "Скід, над яким він наглядає", "6"),
    ("p", "Демо виконує детермінований **скід нагрітої циркуляції з теплообмінником**, змодельований FMU "
          "FMI 2.0 Co-Simulation і керований через справжній протокольний шлюз. Він достатньо багатий, щоб "
          "бути комерційно випробовуваним прототипом — моделює фізику там, де гроші справді витікають:"),
    ("bullet", "Процесна й виміряна температура, циркуляційна витрата, тиск на всмоктуванні."),
    ("bullet", "Швидкість і потужність насоса, положення клапана охолодження, потужність нагрівача."),
    ("bullet", "СКЗ вібрації та температура підшипника (сигнали зносу)."),
    ("bullet", "Інжектовані несправності: заклинювання клапана, знос насоса, дрейф датчика температури, "
               "тепловий розгін."),
    ("bullet", "Детерміновані локальні інтерлоки: висока температура, низька витрата, низький тиск "
               "на всмоктуванні."),
    ("p", "Модель включає теплову інерцію бака, запізнення насоса й клапана, криві витрати й потужності "
          "насоса, ефекти зносу за вібрацією й температурою підшипника та дрейф/шум датчиків — тож "
          "діагностичні голови вчаться з реалістичної, фізично зв'язаної поведінки, а не з пласкої таблиці."),

    ("h1", "Межа протоколів: OPC UA, MQTT і PLC/SIS", "7"),
    ("p", "OPC UA та MQTT доповнюють одне одного, і демо тримає сувору, комерційно розумну межу:"),
    ("table", ["Шлях", "Роль", "Повноваження"],
     [["PLC / PID / SIS", "Жорстке керування реального часу й інтерлоки", "Завжди авторитетний"],
      ["OPC UA", "Обмежений локальний шлях команд", "Єдиний шлях команд; суворо обмежений"],
      ["MQTT", "Розповсюдження телеметрії та рекомендацій", "Ніколи не керує (AUTONOMOUS відхилено)"]],
     [1.8, 3.2, 1.8]),
    ("p", "Є рівно три командні вузли OPC UA (клапан охолодження, швидкість насоса, потужність нагрівача), "
          "а конструктор Java `MqttBridgeConfig` структурно відхиляє режим AUTONOMOUS — MQTT несе вібрацію, "
          "енергію, статус і рекомендації, але його теги ніколи не збігаються з тегом команди "
          "виконавчого механізму. Коли видається команда, перед будь-яким записом вона проходить "
          "фіксований ланцюг пріоритету:"),
    ("code",
     "жорсткий інтерлок -> локальний fail-safe -> перевизначення оператора -> режим безпеки\n"
     "  -> валідація/якість -> обмеження -> ліміт рампи -> придушення різниці\n"
     "  -> запис OPC UA -> аудит"),
    ("table", ["Команда", "Безпечне значення (fail-safe)"],
     [["Клапан охолодження", "100% відкрито (максимальне охолодження)"],
      ["Швидкість насоса", "30%"],
      ["Потужність нагрівача", "0% (вимкнено)"]],
     [3.0, 3.8]),

    ("h1", "Сигнали: спільна мова на багатьох годинниках", "8"),
    ("p", "Усе, що бачить система, стає **типізованим промисловим сигналом**. Кожен оголошує, як часто його "
          "слід обробляти, втілюючи ідею «багатьох годинників» конкретно:"),
    ("table", ["Сигнал", "Що несе", "Каданс (loop)"],
     [["MeasurementSignal", "Процесні вимірювання (темп., витрата, тиск…)", "Щоцикл"],
      ["AlarmSignal", "Тривоги заводу", "Щоцикл"],
      ["InterlockSignal", "Стан жорсткого інтерлока", "Щоцикл"],
      ["OperatorOverrideSignal", "Ручне керування оператора", "Щоцикл"],
      ["ActuatorCommandSignal", "Обмежена команда виконавчого механізму", "Щоцикл"],
      ["DegradationSignal", "Докази деградації обладнання", "Кожні 10 циклів"],
      ["EfficiencySignal", "Контекст енергії / ефективності", "Кожні 10 циклів"],
      ["SetpointSignal", "Обмежена рекомендація уставки", "Кожні 10 циклів"],
      ["MaintenanceWindowSignal", "Контекст планового обслуговування", "Кожні 60 циклів"]],
     [2.6, 2.6, 1.6]),
    ("p", "Швидкі сигнали дають миттєвий стан і безпеку; повільні несуть контекст, що *змінює зміст* "
          "швидких, — жодного разу не сповільнюючи швидкий шлях."),

    ("h1", "Чотири речі, які він діагностує", "9"),
    ("p", "Навчений рівень містить чотири **діагностичні голови висновків** — вузькі, високоцінні "
          "детектори. Кожна спирається на узгоджений набір доказів (тут — простою мовою, з вивчених ваг "
          "моделі):"),
    ("h3", "1 · Знос насоса й ризик кавітації"),
    ("p", "Зростання вібрації й температури підшипника, збільшення потужності насоса та низький тиск на "
          "всмоктуванні / низька витрата вказують на знос чи кавітацію — відрізнено від простого "
          "тимчасового високого навантаження *комбінацією* та її трендом."),
    ("h3", "2 · Коливання контуру й розладнання"),
    ("p", "Проксі заклинювання клапана (команда рухається, положення відстає), щільність реверсів "
          "виконавчого механізму та шаблони переходів навантаження відділяють справжнє розладнання від "
          "нормальних перехідних процесів."),
    ("h3", "3 · Дрейф датчика температури"),
    ("p", "Фізична модель температури дає залишок; великий стійкий залишок без відповідної реакції "
          "потужності чи процесу означає, що дрейфує **датчик**, а не процес, — уникаючи зайвої зупинки."),
    ("h3", "4 · Погіршення енергії на одиницю продукції"),
    ("p", "Споживання енергії відносно виробництва, поєднане з ризиком здоров'я та активністю "
          "виконавчих механізмів, позначає контур, що тримає змінну правильно, але тихо марнує енергію."),

    ("h1", "Навчена модель і готова до розгортання мережа", "10"),
    ("p", "У комплекті з продуктом — вбудована еталонна модель `industrial-loop-guardian`. Навчання не "
          "зупиняється на наборі ваг — воно видає **повну, готову до розгортання мережу JNeopallium**: "
          "п'ять рівнів, сімнадцять справжніх нейронів, 39 ознак, 156 навчуваних ваг і 4 зсуви, записані в "
          "готові до завантаження файли конфігурації рівнів, що посилаються на конкретні класи часу "
          "виконання."),
    ("table", ["Згенерований рівень", "Справжні нейрони / роль"],
     [["Швидка телеметрія", "SensorNeuron, MeasurementValidatorNeuron, OscillationMonitorNeuron, …"],
      ["Діагностичні голови", "DegradationModelNeuron, EnergyAccountingNeuron (чотири голови висновків)"],
      ["Планування рекомендацій", "MaintenanceSchedulingNeuron, SetpointOptimiserNeuron, EconomicBasisNeuron, SafetyGateNeuron"],
      ["Результат", "Нейрони виводу рекомендаційного JSONL"]],
     [2.0, 4.8]),
    ("p", "Кожна діагностична голова також записує **логічну роль** (напр. `PumpHealthAndEfficiencyNeuron`, "
          "`OscillationDiagnosisNeuron`, `SensorFaultDiscriminationNeuron`, `EconomicBasisNeuron`, "
          "`SafetyEnvelopeNeuron`), дозволені їй ознаки (`featureGate`) та `ownedReasoning` — тож "
          "рецензент бачить, що логіка інкапсульована всередині моделі Jneopallium, а не схована в "
          "сполучному коді."),
    ("callout", "Прочитайте це чесно",
     "На вбудованому детермінованому еталонному скіді модель показує майже ідеальний результат (macro-F1 "
     "≈ 0.9995 і лише кілька хибних спрацювань енергетичної голови з тисяч). Це доводить конвеєр і "
     "діагностичне розділення — це НЕ твердження про реальну точність, яку треба заслужити на зовнішніх "
     "даних історіана, CMMS та лічильників енергії. Звіт про тестування прямо про це говорить.", "warning"),

    ("h1", "Безпека за задумом: чому він не зупинить ваш завод", "11"),
    ("num", "**Наглядовий і рекомендаційний за замовчуванням.** Стеля безпеки — ADVISORY. Loop Guardian "
            "рекомендує; він не керує. Обмежені автономні зміни уставок потребують окремого, навмисно "
            "доданого обґрунтування безпеки, процесу погодження та шляху відкату."),
    ("num", "**Детерміноване керування лишається авторитетним.** PLC/PID/SIS володіють швидким керуванням "
            "і жорсткою безпекою. Жорсткі інтерлоки, локальна логіка fail-safe та перевизначення оператора "
            "мають пріоритет над будь-чим, що пропонує модель, — структурно, в ланцюгу пріоритету команд."),
    ("num", "**MQTT не може керувати.** Шлях телеметрії/рекомендацій структурно позбавлений автономної дії; "
            "лише обмежений шлях OPC UA може командувати, і лише в межах обмежень і лімітів рампи."),
    ("num", "**Fail-safe при втраті.** При втраті OPC UA скід застосовує локальні безпечні значення; при "
            "втраті MQTT доступність швидкого контуру лишається 1.0. Втрата наглядача ніколи не загрожує "
            "заводу."),
    ("p", "І кожна рекомендація несе свій **економічний базис** і результат **запобіжної межі**, тож "
          "інженер бачить не лише *що* рекомендовано, а й *чому*, *скільки це коштує* і *що це лишається в "
          "межах налаштованої межі*."),

    ("h1", "Топологія розгортання: де це працює", "12"),
    ("table", ["Режим", "Опис", "Типове застосування"],
     [["Локальний", "Один Java-процес", "Офлайн-відтворення, пілоти, периферійний пристрій на заводі"],
      ["Кластер (HTTP)", "Розподіл через HTTP", "Майданчики з багатьма активами й лініями"],
      ["Кластер (gRPC)", "Розподіл через gRPC; підтримка FPGA", "Високопродуктивні парки"]],
     [1.8, 3.2, 1.8]),
    ("p", "Телеметрія надходить через **мости**, що перекладають протокол на типізовані сигнали: входи "
          "вимірювань/тривог OPC UA, входи телеметрії MQTT і тіньовий потік Kafka. Специфічний для "
          "майданчика адаптер входу перетворює ваш історіан чи живі потоки на ті самі типізовані сигнали, "
          "зберігаючи час події, тож та сама модель працює і на відтворенні на ноутбуці, і на "
          "кластерному заводі."),

    ("h1", "Наскрізний приклад: дев'ять сценаріїв", "13"),
    ("p", "Детермінований раннер проганяє дев'ять сценаріїв і фіксує докази для кожного. Кілька показують "
          "характер архітектури:"),
    ("bullet", "**pump-wear** — докази деградації накопичуються; голова здоров'я насоса піднімає "
               "рекомендацію з обслуговування з економічним базисом (докази несправності з'являються "
               "приблизно на 58-й секунді запуску)."),
    ("bullet", "**oscillation** — голова коливань відділяє розладнання від нормальних переходів "
               "навантаження."),
    ("bullet", "**temperature-sensor-drift** — голова дрейфу датчика приписує аномалію датчику, а не "
               "процесу, уникаючи зайвої зупинки."),
    ("bullet", "**high-temperature-interlock** — жорсткий інтерлок записує fail-safe охолодження й "
               "нагрівача; затримка інтерлока ~0.1 с, належить детермінованому рівню, а не моделі."),
    ("bullet", "**mqtt-outage** — доступність швидкого контуру лишається **1.0**; рекомендації "
               "призупиняються, керування — ні."),
    ("bullet", "**opcua-outage** — застосовуються локальні безпечні значення; завод лишається безпечним "
               "без наглядача."),
    ("callout", "Сенс сценаріїв",
     "У кожному разі модель додає діагностику й економічний контекст, а детерміноване керування тримає "
     "завод безпечним і працюючим. Наглядач покращує рішення; він ніколи не володіє безпекою.", "success"),

    ("h1", "Словник для нефахівців", "14"),
    ("table", ["Термін", "Значення простою мовою"],
     [["Режим ADVISORY", "Система рекомендує; рішення приймають люди й PLC. Вона ніколи не зупиняє завод."],
      ["Кавітація", "Утворення парових бульбашок на всмоктуванні насоса, що його руйнують — через низький тиск."],
      ["Інтерлок", "Жорстке детерміноване правило безпеки (напр. висока темп. → fail-safe охолодження)."],
      ["FMI / FMU", "Стандартний спосіб упакувати симуляцію заводу; тут — для керування демо-скідом."],
      ["OPC UA", "Промисловий стандарт інформації/зв'язку; тут — обмежений шлях команд."],
      ["MQTT", "Легкий протокол публікації/підписки; тут — лише телеметрія й рекомендації."],
      ["PLC / PID / SIS", "Детерміновані регулятори й система безпеки, що володіють швидким керуванням."],
      ["Уставка (setpoint)", "Цільове значення, до якого прагне регулятор (напр. бажана температура)."],
      ["Заклинювання (stiction)", "Статичне тертя в клапані, через яке його положення відстає від команди."],
      ["Наглядовий шар", "Шар, що міркує між контурами й часовими масштабами, над регуляторами."],
      ["Економічний базис", "Грошова цінність, яку система приписує висновку (напр. уникнена зупинка)."]],
     [2.0, 4.8]),
    ("spacer", 8),
    ("pi", "Jneopallium Industrial Loop Guardian · Демо FMI-скіда. Архітектурна стаття для технічних і "
           "нетехнічних читачів. Наглядовий над PLC/PID/SIS. Режим безпеки: ADVISORY. Ліцензія: BSD 3-Clause."),
]
