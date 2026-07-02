# -*- coding: utf-8 -*-
"""Self-Supervised Maintenance Guardian — deployment guide (EN + UK)."""

from __future__ import annotations

DATE = "2 July 2026"
DATE_UK = "2 липня 2026"


def deployment(lang: str) -> list:
    return _EN if lang == "en" else _UK


_META_EN = [
    ("Document", "Setup & Deployment Guide"),
    ("Product", "Jneopallium Self-Supervised Maintenance Guardian"),
    ("Entry point", "com.rakovpublic.jneuropallium.worker.application.Entry"),
    ("Bundle", "model/self-supervised-maintenance"),
    ("Modes", "local · http · grpc"),
    ("Safety", "ADVISORY / read-only"),
    ("Author", "Dmytro Rakovskyi — Kharkiv, Ukraine"),
    ("Date", DATE),
    ("License", "BSD 3-Clause"),
]

_EN = [
    ("cover", "Jneopallium · Self-Supervised Maintenance Guardian",
     "Setup & Deployment Guide",
     "Load the label-free network, wire telemetry and operator feedback, and run the advisory scorer in "
     "production",
     _META_EN, "Deployment Guide",
     "The production runbook: how the Entry runner loads the emitted bundle, how to configure telemetry "
     "inputs and the feedback channel, how advisories leave the system, how continuous learning runs "
     "without a redeploy, and the safety gates you must keep in place."),

    ("toc", "Contents",
     ["What you are deploying",
      "Prerequisites",
      "Step 1 — the bundle and the IContext",
      "Step 2 — run the Entry point",
      "Step 3 — wire telemetry inputs",
      "Step 4 — wire the feedback channel",
      "Step 5 — where advisories go",
      "Step 6 — deployment modes",
      "Step 7 — continuous learning in production",
      "Rollout: shadow → advisory",
      "Operations and monitoring",
      "Safety checklist",
      "Troubleshooting"]),

    ("h1", "What you are deploying", "1"),
    ("p", "A running Jneopallium worker that ingests asset telemetry, scores each frame through the "
          "label-free maintenance network, and emits read-only advisories with a fault family and a lead "
          "time. It also consumes operator feedback and adapts its own alert thresholds in place — no "
          "restart, no redeploy. It never actuates a device."),
    ("callout", "The one rule that never changes",
     "This model is advisory. It recommends inspection; it does not schedule work, trip an interlock, or "
     "move an actuator. The hard safety layer stays entirely outside the learned path.", "warning"),

    ("h1", "Prerequisites", "2"),
    ("bullet", "JDK 17 and the built worker artifact (worker-1.0-SNAPSHOT.jar) or your packaged model JAR."),
    ("bullet", "The trained bundle from the Training Guide under model/self-supervised-maintenance/."),
    ("bullet", "A telemetry source (OPC UA or MQTT) and, for learning, a feedback source (the UI/CMMS event "
               "the operator marks)."),
    ("bullet", "Writable storage for persisted neuron state (so learning survives restarts)."),

    ("h1", "Step 1 — the bundle and the IContext", "3"),
    ("p", "The deployable model is the bundle directory. production-context.json is the IContext the runner "
          "deserializes; its key entries:"),
    ("table", ["IContext key", "Value / meaning"],
     [["configuration.input.layermeta", "the five layers (0–4)"],
      ["neuronnet.classes", "the four ssmaint neuron classes (validated at startup)"],
      ["input.inputs", "AssetTelemetrySignal source (OPC UA / MQTT)"],
      ["outputAggregator", "where advisories are written (JSONL / Kafka / MQTT)"],
      ["storage.json", "model/self-supervised-maintenance (persisted state)"],
      ["processing.frequency.map / slowfast.ratio", "fast telemetry vs slow feedback cadence"],
      ["isteacherstudying=false, discriminatorsAmount=0, infiniteRun=true", "advisory inference loop"],
      ["safetyMode / advisoryOnly", "ADVISORY / true — enforced"]],
     [3.1, 3.2]),
    ("p", "The runner validates neuronnet.classes at startup and fails fast if a class is missing — so a "
          "misconfigured bundle never half-starts."),

    ("h1", "Step 2 — run the Entry point", "4"),
    ("code", "java -cp worker-1.0-SNAPSHOT.jar \\\n"
             "  com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
             "  local <model-jar-url> <IContext-class> \\\n"
             "  worker/src/main/resources/model/self-supervised-maintenance/production-context.json"),
    ("p", "The Runner deserializes the IContext, loads your model JAR via the JAR class loader, selects the "
          "IApplication for the chosen mode, and begins the inference loop. From here it is telemetry in, "
          "advisories out."),

    ("h1", "Step 3 — wire telemetry inputs", "5"),
    ("p", "Map each asset's live tags to an AssetTelemetrySignal per tick: the eight sensor channels plus "
          "the operating-regime integer. Use the standard industrial inputs:"),
    ("bullet", "**OPC UA** — OpcUaMeasurementInput for process tags (pressure, flow, temperature, power, "
               "valve position)."),
    ("bullet", "**MQTT / Sparkplug B** — MqttMetricInput for IIoT gateways and vibration/acoustic feeds."),
    ("bullet", "Derive the **regime** from a load or power band if you do not publish it directly."),
    ("callout", "Get the sensor names right",
     "The reconstruction weights are keyed by sensor name. The tag→sensor mapping must match the "
     "sensorOrder in layer-1 exactly, or the residuals are meaningless. Validate the mapping once at "
     "commissioning.", "info"),

    ("h1", "Step 4 — wire the feedback channel", "6"),
    ("p", "This is what makes the model improve. When an operator dispositions an advisory in your UI, CMMS, "
          "or SIEM, publish an OperatorFeedbackSignal carrying: the asset, the fault family, "
          "confirmed / false-positive, the operator identity, and the domain-shift value observed when the "
          "advisory fired (so the adapter can freeze during novelty). Feedback runs on the slow loop; it "
          "need not be real-time."),
    ("bullet", "**Confirmed** advisories slightly relax that family's threshold."),
    ("bullet", "**False-positive** advisories raise it — bounded and rate-limited."),
    ("bullet", "Only **authenticated** operator feedback should be admitted; tag provenance for audit."),

    ("h1", "Step 5 — where advisories go", "7"),
    ("p", "Advisories leave through an IOutputAggregator, instantiated by the runtime. Choose per site:"),
    ("table", ["Aggregator", "Use it for"],
     [["JsonlResultAggregator", "file/SIEM ingest, simplest first deployment"],
      ["KafkaAdvisoryOutputAggregator", "streaming into an enterprise bus / historian"],
      ["MqttAdvisoryOutputAggregator", "publishing back to an IIoT broker / dashboard"]],
     [3.1, 3.2]),
    ("p", "Each MaintenanceAdvisorySignal carries the asset, fault family, severity, estimated lead time, "
          "uncertainty, and a human-readable recommendation. advisoryOnly is always true on the wire."),

    ("h1", "Step 6 — deployment modes", "8"),
    ("table", ["Mode", "Shape", "When"],
     [["local", "single worker in one JVM", "a plant, a cell, an edge box"],
      ["http", "master + HTTP workers", "several lines, central coordination"],
      ["grpc (client/master)", "master + gRPC workers", "larger fleets, lower-latency fan-out"]],
     [1.8, 2.4, 2.6]),
    ("p", "The model is identical across modes; only the transport and worker topology change. Start local, "
          "scale out when the asset count warrants it."),

    ("h1", "Step 7 — continuous learning in production", "9"),
    ("p", "Learning at runtime lives in neuron state and is persisted to storage.json, so it survives "
          "restarts without a rebuild. The recommended discipline for evolving anything heavier than "
          "thresholds is champion/challenger:"),
    ("bullet", "the **champion** (current, stable) serves advisories;"),
    ("bullet", "a **challenger** adapts on the harvested feedback in shadow;"),
    ("bullet", "promote the challenger only when it beats the champion on **independent** outcomes (actual "
               "work orders / confirmed failures), and keep the champion snapshot for instant rollback."),
    ("callout", "No redeploy required",
     "Threshold adaptation is applied in place via ThresholdUpdateSignal into the running gate. Promotion "
     "of a challenger is an internal, audited state swap — not a JAR redeploy.", "info"),

    ("h1", "Rollout: shadow → advisory", "10"),
    ("bullet", "**Shadow (weeks 1–n).** Advisories are recorded but not shown to operators; you learn the "
               "false-positive rate and let baselines settle. Feedback can still be harvested from actual "
               "outcomes."),
    ("bullet", "**Advisory (go-live).** Operators see advisories and disposition them; the feedback loop "
               "starts driving nuisance alerts down."),
    ("bullet", "There is no autonomous stage. The model never graduates out of advisory."),

    ("h1", "Operations and monitoring", "11"),
    ("bullet", "Track the **false-positive rate per family** and confirm it trends down as feedback "
               "accumulates."),
    ("bullet", "Watch **domain-shift / uncertainty**; a rising trend means the plant has moved away from "
               "the training window — schedule a refit (Training Guide)."),
    ("bullet", "Snapshot persisted neuron state on a schedule so a promotion can be rolled back."),
    ("bullet", "Alert on **detection lead time** shrinking — advisories firing too close to failure suggest "
               "the trend or thresholds need attention."),

    ("h1", "Safety checklist", "12"),
    ("bullet", "advisoryOnly is true and cannot be disabled (SsMaintConfig invariant)."),
    ("bullet", "No output path reaches an actuator; interlocks and the hard safety gate are outside this "
               "model."),
    ("bullet", "Feedback is authenticated and provenance-tagged; adaptation freezes during domain shift."),
    ("bullet", "Persisted state is snapshotted; challenger promotions are audited and reversible."),

    ("h1", "Troubleshooting", "13"),
    ("table", ["Symptom", "Likely cause", "Fix"],
     [["Startup fails fast", "neuronnet.classes / bundle mismatch", "Re-emit bundle; check classpath"],
      ["Residuals nonsensical", "tag→sensor mapping wrong", "Align mapping with layer-1 sensorOrder"],
      ["Thresholds never move", "feedback not arriving / frozen", "Check feedback wiring & domain-shift"],
      ["Learning lost on restart", "storage not persisted", "Set writable storage.json"]],
     [1.9, 2.3, 2.5]),
    ("pi", "Deploy it in shadow, wire the feedback, watch the false-positive rate fall — and never let it "
           "leave advisory. The safety layer is deterministic and lives outside the model, exactly as it "
           "should."),
]


_META_UK = [
    ("Документ", "Посібник з налаштування та розгортання"),
    ("Продукт", "Jneopallium Self-Supervised Maintenance Guardian"),
    ("Точка входу", "com.rakovpublic.jneuropallium.worker.application.Entry"),
    ("Збірка", "model/self-supervised-maintenance"),
    ("Режими", "local · http · grpc"),
    ("Безпека", "ADVISORY / лише читання"),
    ("Автор", "Дмитро Раковський — Харків, Україна"),
    ("Дата", DATE_UK),
    ("Ліцензія", "BSD 3-Clause"),
]

_UK = [
    ("cover", "Jneopallium · Self-Supervised Maintenance Guardian",
     "Посібник з налаштування та розгортання",
     "Завантажте мережу без міток, підключіть телеметрію та зворотний зв'язок операторів і запустіть "
     "рекомендаційний скорер у продакшені",
     _META_UK, "Посібник з розгортання",
     "Виробничий регламент: як запускач Entry завантажує згенеровану збірку, як налаштувати входи "
     "телеметрії та канал зворотного зв'язку, як рекомендації виходять із системи, як безперервне навчання "
     "працює без переустановлення та які шлюзи безпеки слід зберігати."),

    ("toc", "Зміст",
     ["Що ви розгортаєте",
      "Передумови",
      "Крок 1 — збірка та IContext",
      "Крок 2 — запустити точку входу",
      "Крок 3 — підключити входи телеметрії",
      "Крок 4 — підключити канал зворотного зв'язку",
      "Крок 5 — куди йдуть рекомендації",
      "Крок 6 — режими розгортання",
      "Крок 7 — безперервне навчання у продакшені",
      "Розкатка: тінь → рекомендація",
      "Експлуатація та моніторинг",
      "Контрольний список безпеки",
      "Усунення несправностей"]),

    ("h1", "Що ви розгортаєте", "1"),
    ("p", "Робочий вузол Jneopallium, що приймає телеметрію активів, оцінює кожен кадр через мережу "
          "обслуговування без міток і видає рекомендації лише для читання із сімейством несправності та "
          "запасом часу. Він також споживає зворотний зв'язок операторів і адаптує власні пороги тривог на "
          "місці — без перезапуску, без переустановлення. Він ніколи не керує пристроєм."),
    ("callout", "Єдине правило, що ніколи не змінюється",
     "Ця модель рекомендаційна. Вона радить огляд; вона не планує роботи, не спрацьовує блокуванням і не "
     "рухає виконавчий механізм. Жорсткий шар безпеки лишається цілком поза навченим шляхом.", "warning"),

    ("h1", "Передумови", "2"),
    ("bullet", "JDK 17 і зібраний артефакт worker (worker-1.0-SNAPSHOT.jar) або ваш упакований JAR моделі."),
    ("bullet", "Навчена збірка з посібника з навчання у model/self-supervised-maintenance/."),
    ("bullet", "Джерело телеметрії (OPC UA чи MQTT) та, для навчання, джерело зворотного зв'язку (подія "
               "UI/CMMS, яку позначає оператор)."),
    ("bullet", "Записуване сховище для збереженого стану нейронів (щоб навчання переживало перезапуски)."),

    ("h1", "Крок 1 — збірка та IContext", "3"),
    ("p", "Розгортувана модель — це каталог збірки. production-context.json — це IContext, який десеріалізує "
          "запускач; його ключові записи:"),
    ("table", ["Ключ IContext", "Значення / зміст"],
     [["configuration.input.layermeta", "п'ять шарів (0–4)"],
      ["neuronnet.classes", "чотири класи нейронів ssmaint (перевіряються на старті)"],
      ["input.inputs", "джерело AssetTelemetrySignal (OPC UA / MQTT)"],
      ["outputAggregator", "куди записуються рекомендації (JSONL / Kafka / MQTT)"],
      ["storage.json", "model/self-supervised-maintenance (збережений стан)"],
      ["processing.frequency.map / slowfast.ratio", "каданс швидкої телеметрії проти повільного зв'язку"],
      ["isteacherstudying=false, discriminatorsAmount=0, infiniteRun=true", "цикл рекомендаційного висновку"],
      ["safetyMode / advisoryOnly", "ADVISORY / true — примусово"]],
     [3.1, 3.2]),
    ("p", "Запускач перевіряє neuronnet.classes на старті й швидко падає, якщо класу бракує — тож хибно "
          "налаштована збірка ніколи не стартує наполовину."),

    ("h1", "Крок 2 — запустити точку входу", "4"),
    ("code", "java -cp worker-1.0-SNAPSHOT.jar \\\n"
             "  com.rakovpublic.jneuropallium.worker.application.Entry \\\n"
             "  local <model-jar-url> <IContext-class> \\\n"
             "  worker/src/main/resources/model/self-supervised-maintenance/production-context.json"),
    ("p", "Runner десеріалізує IContext, завантажує ваш JAR моделі через завантажувач класів JAR, обирає "
          "IApplication для вибраного режиму й починає цикл висновку. Далі — телеметрія на вході, "
          "рекомендації на виході."),

    ("h1", "Крок 3 — підключити входи телеметрії", "5"),
    ("p", "Зіставте живі теги кожного активу з AssetTelemetrySignal на такт: вісім каналів сенсорів плюс "
          "цілочисельний режим роботи. Використовуйте стандартні промислові входи:"),
    ("bullet", "**OPC UA** — OpcUaMeasurementInput для тегів процесу (тиск, витрата, температура, "
               "потужність, положення клапана)."),
    ("bullet", "**MQTT / Sparkplug B** — MqttMetricInput для IIoT-шлюзів і потоків вібрації/акустики."),
    ("bullet", "Виведіть **режим** зі смуги навантаження чи потужності, якщо не публікуєте його напряму."),
    ("callout", "Правильно задайте назви сенсорів",
     "Ваги відновлення прив'язані до назв сенсорів. Зіставлення тег→сенсор має точно збігатися з "
     "sensorOrder у шарі 1, інакше залишки безглузді. Перевірте зіставлення один раз при введенні в "
     "експлуатацію.", "info"),

    ("h1", "Крок 4 — підключити канал зворотного зв'язку", "6"),
    ("p", "Саме це змушує модель вдосконалюватися. Коли оператор виносить вердикт щодо рекомендації у вашому "
          "UI, CMMS чи SIEM, опублікуйте OperatorFeedbackSignal, що несе: актив, сімейство несправності, "
          "підтверджено / хибне, особу оператора та значення зсуву домену, спостережене коли рекомендація "
          "спрацювала (щоб адаптер міг заморозитися під час новизни). Зворотний зв'язок працює на "
          "повільному циклі; він не мусить бути в реальному часі."),
    ("bullet", "**Підтверджені** рекомендації трохи послаблюють поріг цього сімейства."),
    ("bullet", "**Хибні** рекомендації підвищують його — обмежено й з обмеженням частоти."),
    ("bullet", "Слід приймати лише **автентифікований** зворотний зв'язок оператора; позначайте походження "
               "для аудиту."),

    ("h1", "Крок 5 — куди йдуть рекомендації", "7"),
    ("p", "Рекомендації виходять через IOutputAggregator, який створює середовище виконання. Оберіть під "
          "майданчик:"),
    ("table", ["Агрегатор", "Використання"],
     [["JsonlResultAggregator", "прийом у файл/SIEM, найпростіше перше розгортання"],
      ["KafkaAdvisoryOutputAggregator", "потік у корпоративну шину / історіан"],
      ["MqttAdvisoryOutputAggregator", "публікація назад у IIoT-брокер / дашборд"]],
     [3.1, 3.2]),
    ("p", "Кожен MaintenanceAdvisorySignal несе актив, сімейство несправності, тяжкість, оцінений запас "
          "часу, невизначеність і зрозумілу людині рекомендацію. advisoryOnly завжди true на дроті."),

    ("h1", "Крок 6 — режими розгортання", "8"),
    ("table", ["Режим", "Форма", "Коли"],
     [["local", "один вузол в одному JVM", "установка, комірка, крайовий вузол"],
      ["http", "майстер + HTTP-вузли", "кілька ліній, централізована координація"],
      ["grpc (client/master)", "майстер + gRPC-вузли", "більші парки, менша затримка"]],
     [1.8, 2.4, 2.6]),
    ("p", "Модель однакова в усіх режимах; змінюються лише транспорт і топологія вузлів. Почніть з local, "
          "масштабуйтеся, коли кількість активів того вимагає."),

    ("h1", "Крок 7 — безперервне навчання у продакшені", "9"),
    ("p", "Навчання під час роботи живе у стані нейронів і зберігається в storage.json, тож переживає "
          "перезапуски без перезбирання. Рекомендована дисципліна для розвитку чогось важчого за пороги — "
          "«чемпіон/претендент»:"),
    ("bullet", "**чемпіон** (поточний, стабільний) видає рекомендації;"),
    ("bullet", "**претендент** адаптується на зібраному зворотному зв'язку в тіні;"),
    ("bullet", "просувайте претендента лише коли він перевершує чемпіона за **незалежними** наслідками "
               "(реальні наряди / підтверджені відмови), і зберігайте знімок чемпіона для миттєвого "
               "відкату."),
    ("callout", "Переустановлення не потрібне",
     "Адаптація порогів застосовується на місці через ThresholdUpdateSignal у робочий шлюз. Просування "
     "претендента — внутрішня, аудитована заміна стану, а не переустановлення JAR.", "info"),

    ("h1", "Розкатка: тінь → рекомендація", "10"),
    ("bullet", "**Тінь (тижні 1–n).** Рекомендації записуються, але не показуються операторам; ви вивчаєте "
               "частку хибних спрацювань і даєте базовим рівням усталитися. Зворотний зв'язок усе одно "
               "можна збирати з реальних наслідків."),
    ("bullet", "**Рекомендація (запуск).** Оператори бачать рекомендації й виносять вердикти; цикл "
               "зворотного зв'язку починає знижувати зайві тривоги."),
    ("bullet", "Автономної стадії немає. Модель ніколи не виходить за межі рекомендації."),

    ("h1", "Експлуатація та моніторинг", "11"),
    ("bullet", "Відстежуйте **частку хибних спрацювань за сімейством** і переконайтесь, що вона спадає в "
               "міру накопичення зворотного зв'язку."),
    ("bullet", "Слідкуйте за **зсувом домену / невизначеністю**; зростання означає, що установка відійшла "
               "від навчального вікна — заплануйте перепасування (посібник з навчання)."),
    ("bullet", "Робіть знімки збереженого стану нейронів за розкладом, щоб просування можна було відкотити."),
    ("bullet", "Сигналізуйте про скорочення **запасу часу виявлення** — рекомендації, що спрацьовують "
               "надто близько до відмови, натякають на потребу уваги до тренду чи порогів."),

    ("h1", "Контрольний список безпеки", "12"),
    ("bullet", "advisoryOnly дорівнює true й не може бути вимкнено (інваріант SsMaintConfig)."),
    ("bullet", "Жоден вихідний шлях не сягає виконавчого механізму; блокування й жорсткий шлюз безпеки поза "
               "цією моделлю."),
    ("bullet", "Зворотний зв'язок автентифіковано й позначено походженням; адаптація замерзає під час "
               "зсуву домену."),
    ("bullet", "Збережений стан має знімки; просування претендентів аудитуються й оборотні."),

    ("h1", "Усунення несправностей", "13"),
    ("table", ["Симптом", "Ймовірна причина", "Виправлення"],
     [["Швидке падіння на старті", "невідповідність neuronnet.classes / збірки", "Перегенерувати збірку"],
      ["Залишки безглузді", "неправильне зіставлення тег→сенсор", "Узгодити з sensorOrder шару 1"],
      ["Пороги не рухаються", "зворотний зв'язок не надходить / заморожено", "Перевірити підключення й зсув"],
      ["Навчання втрачено при перезапуску", "стан не збережено", "Задати записуване storage.json"]],
     [1.9, 2.3, 2.5]),
    ("pi", "Розгорніть у тіні, підключіть зворотний зв'язок, дивіться, як частка хибних спрацювань спадає — "
           "і ніколи не виводьте це за межі рекомендації. Шар безпеки детермінований і живе поза моделлю, "
           "як і має бути."),
]
