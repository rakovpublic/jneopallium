# -*- coding: utf-8 -*-
"""Industrial Loop Guardian — pitch deck (EN + UK), slide DSL."""

from __future__ import annotations


def pitch(lang: str) -> list:
    return _EN if lang == "en" else _UK


_EN = [
    ("cover", "Jneopallium · Industrial Loop Guardian",
     "Your Plant Already Sends the Signals. Start Reading Them.",
     "Cross-loop, cross-timescale operational intelligence from your existing OPC UA & MQTT telemetry",
     "On-premises · Shadow → Advisory · Supervisory above PLC/PID/SIS · BSD-3 core"),

    ("bullets", "01", "Your PID holds the variable. The plant still loses money.",
     "The expensive problems are the ones no single loop can see.",
     ["A pump wears out over **weeks** — the \"bearing > 85 °C\" alarm fires only after the damage.",
      "Three controllers each behave correctly while together **wasting energy** no loop detects.",
      "The same rising temperature is normal at startup and alarming at steady state — **nuisance alarms**.",
      "Is the process hotter, or is one sensor drifting? Majority voting **can't tell**.",
      "Each of these is a decision across **several signals on different timescales** — not a single loop."]),

    ("twocol", "02", "We supervise. We don't replace.",
     "Keep — unchanged", [
         "PLC / PID / SIS deterministic control",
         "Hard interlocks and emergency shutdown",
         "Your existing OPC UA & MQTT telemetry",
         "Operator overrides, authoritative",
     ],
     "Add — Loop Guardian (above)", [
         "Multi-loop, multi-timescale diagnosis",
         "Energy & bounded setpoint optimisation",
         "Degradation prediction & maintenance planning",
         "Advisories with an **economic basis**",
     ]),

    ("bullets", "03", "It watches across timescales",
     "A single neuron weighs the instant against the trend.",
     ["**Every tick:** flow, pressure, interlock state, pump command vs. actual speed — immediate condition.",
      "**Every 5–10 ticks:** vibration RMS — mechanical anomaly.",
      "**Every 10 ticks:** bearing temperature, power — thermal condition and efficiency.",
      "**Every 50+ ticks:** production load, degradation, maintenance schedule — operating and planning context.",
      "A PID sees one variable now. The Loop Guardian sees the **whole asset over time**."]),

    ("bullets", "04", "Four detectors that pay for themselves",
     "Narrow, high-value, and already trained in the demo.",
     ["**Pump wear & cavitation risk** — distinguishes real degradation from a temporary high load.",
      "**Control-loop oscillation & tuning degradation** — separates valve stiction from normal transients.",
      "**Temperature-sensor drift** — tells a drifting sensor from a genuine process rise, avoiding a needless trip.",
      "**Energy-per-unit-production deterioration** — flags a loop that holds its variable while wasting energy."]),

    ("bullets", "05", "Now it also hears and feels your machines",
     "New: a multimodal machine-health subsystem — acoustic + vibration condition monitoring.",
     ["**Listens and feels** — turns sound and vibration into a calibrated health score and named faults.",
      "**Names the fault** — bearing damage, cavitation, imbalance, sensor fault, or an unknown anomaly.",
      "**Knows when it's unsure** — a domain-shift score and an uncertainty value, so an unfamiliar machine "
      "reads as \"not sure,\" not a false alarm.",
      "**Learns from public benchmarks** — MIMII, DCASE, and the Paderborn bearing dataset, plus your own "
      "telemetry once reviewed.",
      "**Read-only by construction** — it advises; it never touches an actuator."]),

    ("bullets", "06", "Every advisory carries its economic basis",
     "Not just \"there's a problem\" — what it is, why, and what it's worth.",
     ["`finding`: pump wear and cavitation risk · `confidence`: 0.73",
      "`evidence`: PumpHealthAndEfficiencyNeuron, max vibration 4.3 mm/s",
      "`recommendation`: SCHEDULE_PUMP_INSPECTION · `urgencyHours`: 48",
      "`economicBasis`: estimated avoided-shutdown value **$20,000**, safety envelope satisfied",
      "`autonomousAction`: **false** — and the PLC/PID/SIS boundary is stated in every record."]),

    ("bullets", "07", "The profitability is simple to compute",
     "Annual value V = Energy + Downtime + Quality + Maintenance + Operator − Cost. Illustrative example:",
     ["Energy: 2% of an $800k bill  →  **$16,000**",
      "Avoided shutdowns: 2 × $20,000  →  **$40,000**",
      "Reduced maintenance / scrap  →  **$15,000**",
      "Gross annual value  →  **$71,000**  ·  platform & support  →  −$20,000",
      "**Net annual value ≈ $51,000.** Price the product at ~10–20% of verified savings."]),

    ("metrics", "08", "Proof: the pipeline works",
     "Near-perfect separation on the reference skid — machinery proven, ready for your data.",
     [("0.9995", "Macro-F1 across\nthe four detectors"),
      ("~0.0005", "Macro false-\npositive rate"),
      ("1.0", "Fast-control availability\nthrough an MQTT outage"),
      ("0.1 s", "Hard interlock latency\n(owned by PLC/SIS)")]),

    ("bullets", "09", "Safe by construction",
     "Easy to demonstrate, validate, insure, and sell.",
     ["**Advisory by default.** It recommends; PLC/PID/SIS and operators decide. It never trips the plant.",
      "**Deterministic controls stay authoritative** — interlocks and overrides outrank any suggestion.",
      "**MQTT cannot actuate** (AUTONOMOUS is structurally rejected); only a bounded OPC UA path can command.",
      "**Fail-safe on loss:** lose the supervisor and fast control stays up; lose OPC UA and local fail-safe applies.",
      "It is **simulation/HIL evidence** today — not a certified SIS, and it never claims to be."]),

    ("bullets", "10", "Land and expand — paid at every step",
     "A low-risk path from a data export to a recurring subscription. (Indicative pricing.)",
     ["**Phase A — Offline replay.** Analyse a historian export; deliver anomalies, timeline, and €-impact. **€3–8k / assessment.**",
      "**Phase B — Shadow pilot.** Read-only, no writes, 6–12 weeks; compare against real outcomes. **€7.5–20k**, credited to year one.",
      "**Phase C — Production advisory subscription.** Operator-visible advisories, one edge deployment. **€15–40k / site / year.**",
      "**Phase D — Bounded autonomous optimisation.** Only after evidence + a site safety case. **€30–100k+ / site / year.**"]),

    ("twocol", "11", "Where we don't compete — and where we do",
     "Keep your PID (low/no value)", [
         "A single stable regulatory loop",
         "Hard interlocks & emergency shutdown",
         "\"pressure > limit → shut valve\"",
         "Cheap downtime, little telemetry",
     ],
     "Add Loop Guardian (high value)", [
         "Predictive maintenance",
         "Oscillation & sensor-fault diagnosis",
         "Energy / setpoint optimisation",
         "Batch-context anomaly detection",
     ]),

    ("bullets", "12", "Built to fit the plant you already run",
     "On-premises, open core, integrator-friendly.",
     ["**Reads what you already produce** — OPC UA process tags and MQTT IIoT telemetry; optional Kafka, CMMS, energy meters.",
      "**On-premises edge deployment** — local, HTTP cluster, or gRPC; no cloud required.",
      "**Open BSD-3 core** — no lock-in; the moat is the model packs, integrations, and support, not the engine.",
      "**Integrator channel** — you bring the plant relationship; we bring the supervisory intelligence and releases.",
      "**Train-to-deploy in one step** — what you train is the exact network you run; every weight is inspectable."]),

    ("closing", "Send us your data. We'll show you the money.",
     ["Export an anonymised historian sample, or point us at a simulated OPC UA endpoint.",
      "We return discovered anomalies, an event timeline, an estimated €-impact, and a false-positive review.",
      "Open-source demo today · paid assessment in weeks · shadow pilot next quarter."],
     "Book a paid operational assessment.",
     ["Dmytro Rakovskyi · Kharkiv, Ukraine",
      "github.com/rakovpublic/jneopallium",
      "IJSR 13(7), 2024 · DOI 10.21275/SR24703042047"]),
]


_UK = [
    ("cover", "Jneopallium · Industrial Loop Guardian",
     "Ваш завод уже надсилає сигнали. Почніть їх читати.",
     "Міжконтурна, різночасова операційна інтелектуальність з наявної телеметрії OPC UA та MQTT",
     "Локально · Тінь → Рекомендації · Наглядовий над PLC/PID/SIS · Відкрите ядро BSD-3"),

    ("bullets", "01", "Ваш PID тримає змінну. Завод усе одно втрачає гроші.",
     "Дорогі проблеми — це ті, яких не бачить жоден окремий контур.",
     ["Насос зношується **тижнями** — тривога «підшипник > 85 °C» спрацьовує лише після шкоди.",
      "Три регулятори кожен поводиться правильно, а разом **марнують енергію**, якої не помітить жоден контур.",
      "Те саме зростання температури нормальне на запуску й тривожне в усталеному режимі — **хибні тривоги**.",
      "Процес гарячіший чи один датчик дрейфує? Голосування більшістю **не скаже**.",
      "Кожне з цього — рішення по **кількох сигналах на різних часових масштабах**, а не один контур."]),

    ("twocol", "02", "Ми наглядаємо. Ми не замінюємо.",
     "Зберегти — без змін", [
         "Детерміноване керування PLC / PID / SIS",
         "Жорсткі інтерлоки й аварійна зупинка",
         "Вашу наявну телеметрію OPC UA та MQTT",
         "Перевизначення оператора, авторитетні",
     ],
     "Додати — Loop Guardian (згори)", [
         "Багатоконтурна, різночасова діагностика",
         "Енергія та обмежена оптимізація уставок",
         "Прогноз деградації й планування обслуговування",
         "Рекомендації з **економічним базисом**",
     ]),

    ("bullets", "03", "Він стежить на багатьох часових масштабах",
     "Один нейрон зважує миттєвість проти тренду.",
     ["**Щотакту:** витрата, тиск, стан інтерлока, команда насоса vs фактична швидкість — миттєвий стан.",
      "**Кожні 5–10 тактів:** СКЗ вібрації — механічна аномалія.",
      "**Кожні 10 тактів:** температура підшипника, потужність — тепловий стан і ефективність.",
      "**Кожні 50+ тактів:** виробниче навантаження, деградація, графік обслуговування — контекст.",
      "PID бачить одну змінну зараз. Loop Guardian бачить **увесь актив у часі**."]),

    ("bullets", "04", "Чотири детектори, що окуповують себе",
     "Вузькі, високоцінні й уже навчені в демо.",
     ["**Знос насоса й ризик кавітації** — відрізняє справжню деградацію від тимчасового високого навантаження.",
      "**Коливання контуру й розладнання** — відділяє заклинювання клапана від нормальних перехідних процесів.",
      "**Дрейф датчика температури** — відрізняє дрейфуючий датчик від справжнього зростання, уникаючи зайвої зупинки.",
      "**Погіршення енергії на одиницю продукції** — позначає контур, що тримає змінну, але марнує енергію."]),

    ("bullets", "05", "Тепер він ще й чує та відчуває ваші машини",
     "Нове: мультимодальна підсистема здоров'я машини — акустичний + вібраційний моніторинг стану.",
     ["**Слухає й відчуває** — перетворює звук і вібрацію на калібрований бал здоров'я й названі несправності.",
      "**Називає несправність** — пошкодження підшипника, кавітація, дисбаланс, несправність датчика чи невідома аномалія.",
      "**Знає, коли не впевнений** — бал зсуву домену й значення невпевненості, тож незнайома машина читається "
      "як «не впевнений», а не як хибна тривога.",
      "**Вчиться на публічних бенчмарках** — MIMII, DCASE й набір підшипників Paderborn, плюс ваша телеметрія "
      "після перегляду.",
      "**Лише для читання за побудовою** — він радить; ніколи не торкається виконавчого механізму."]),

    ("bullets", "06", "Кожна рекомендація несе свій економічний базис",
     "Не просто «є проблема» — що, чому й скільки це коштує.",
     ["`finding`: знос насоса й ризик кавітації · `confidence`: 0.73",
      "`evidence`: PumpHealthAndEfficiencyNeuron, макс. вібрація 4.3 мм/с",
      "`recommendation`: SCHEDULE_PUMP_INSPECTION · `urgencyHours`: 48",
      "`economicBasis`: оцінена вартість уникненої зупинки **$20 000**, межу безпеки дотримано",
      "`autonomousAction`: **false** — і межа PLC/PID/SIS зазначена в кожному записі."]),

    ("bullets", "07", "Прибутковість легко порахувати",
     "Річна цінність V = Енергія + Простій + Якість + Обслуговування + Оператор − Витрати. Ілюстративно:",
     ["Енергія: 2% від рахунку $800k  →  **$16 000**",
      "Уникнені зупинки: 2 × $20 000  →  **$40 000**",
      "Менше обслуговування / браку  →  **$15 000**",
      "Валова річна цінність  →  **$71 000**  ·  платформа й підтримка  →  −$20 000",
      "**Чиста річна цінність ≈ $51 000.** Ціна продукту ~10–20% від перевірених заощаджень."]),

    ("metrics", "08", "Доказ: конвеєр працює",
     "Майже ідеальне розділення на еталонному скіді — механізм доведено, готовий до ваших даних.",
     [("0.9995", "Macro-F1 по\nчотирьох детекторах"),
      ("~0.0005", "Macro рівень\nхибних спрацювань"),
      ("1.0", "Доступність керування\nпід час відмови MQTT"),
      ("0.1 с", "Затримка інтерлока\n(належить PLC/SIS)")]),

    ("bullets", "09", "Безпечний за побудовою",
     "Легко продемонструвати, валідувати, застрахувати й продати.",
     ["**Рекомендаційний за замовчуванням.** Він радить; PLC/PID/SIS та оператори вирішують. Ніколи не зупиняє завод.",
      "**Детерміноване керування лишається авторитетним** — інтерлоки й перевизначення вищі за будь-яку пропозицію.",
      "**MQTT не може керувати** (AUTONOMOUS структурно відхилено); командувати може лише обмежений шлях OPC UA.",
      "**Fail-safe при втраті:** втратите наглядача — швидке керування лишається; втратите OPC UA — локальний fail-safe.",
      "Сьогодні це **доказ симуляції/HIL** — не сертифікована SIS, і він ніколи цього не стверджує."]),

    ("bullets", "10", "Заходь і розширюйся — оплата на кожному кроці",
     "Низькоризиковий шлях від експорту даних до регулярної підписки. (Орієнтовні ціни.)",
     ["**Фаза A — Офлайн-відтворення.** Аналіз експорту історіана; аномалії, хронологія, €-вплив. **€3–8k / оцінка.**",
      "**Фаза B — Тіньовий пілот.** Лише читання, без записів, 6–12 тижнів; порівняння з реальністю. **€7.5–20k**, зараховується в рік 1.",
      "**Фаза C — Промислова рекомендаційна підписка.** Видимі оператору рекомендації, одне розгортання. **€15–40k / майданчик / рік.**",
      "**Фаза D — Обмежена автономна оптимізація.** Лише після доказів + обґрунтування безпеки. **€30–100k+ / майданчик / рік.**"]),

    ("twocol", "11", "Де ми не конкуруємо — і де конкуруємо",
     "Залиште свій PID (низька/нульова цінність)", [
         "Окремий стабільний контур регулювання",
         "Жорсткі інтерлоки й аварійна зупинка",
         "«тиск > межі → закрити клапан»",
         "Дешевий простій, мало телеметрії",
     ],
     "Додайте Loop Guardian (висока цінність)", [
         "Прогнозне обслуговування",
         "Діагностика коливань і несправностей датчиків",
         "Оптимізація енергії / уставок",
         "Виявлення аномалій з урахуванням фази партії",
     ]),

    ("bullets", "12", "Створено вписатися в завод, який ви вже маєте",
     "Локально, відкрите ядро, дружньо до інтеграторів.",
     ["**Читає те, що ви вже виробляєте** — теги OPC UA та телеметрію MQTT; за бажанням Kafka, CMMS, лічильники енергії.",
      "**Локальне периферійне розгортання** — local, HTTP-кластер або gRPC; хмара не потрібна.",
      "**Відкрите ядро BSD-3** — без прив'язки; перевага — у пакетах моделей, інтеграціях і підтримці, а не в рушії.",
      "**Канал інтеграторів** — ви приносите стосунки із заводом; ми — наглядову інтелектуальність і релізи.",
      "**Навчив — і розгорнув** — те, що ви навчаєте, і є мережею, яку ви запускаєте; кожну вагу можна перевірити."]),

    ("closing", "Надішліть нам ваші дані. Ми покажемо вам гроші.",
     ["Експортуйте анонімізований зразок історіана або вкажіть нам змодельований ендпоінт OPC UA.",
      "Ми повертаємо виявлені аномалії, хронологію подій, оцінений €-вплив і перегляд хибних спрацювань.",
      "Відкрите демо сьогодні · платна оцінка за тижні · тіньовий пілот наступного кварталу."],
     "Замовте платну операційну оцінку.",
     ["Дмитро Раковський · Харків, Україна",
      "github.com/rakovpublic/jneopallium",
      "IJSR 13(7), 2024 · DOI 10.21275/SR24703042047"]),
]
