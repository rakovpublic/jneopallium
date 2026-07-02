# -*- coding: utf-8 -*-
"""Self-Supervised Maintenance Guardian — pitch deck (EN + UK)."""

from __future__ import annotations


def pitch(lang: str) -> list:
    return _EN if lang == "en" else _UK


_EN = [
    ("cover", "Jneopallium · Self-Supervised Maintenance Guardian",
     "Predict Failures Before You Have a Failure History",
     "Label-free predictive maintenance that learns from your telemetry — and gets sharper from your "
     "operators, without a redeploy",
     "On-premises · Shadow → Advisory · No labels required · Never actuates · BSD-3 core"),

    ("bullets", "01", "You have the data. You don't have the labels.",
     "Predictive maintenance usually asks for the one thing you don't have: a clean history of past "
     "failures.",
     ["You have **years of telemetry** — pressure, flow, vibration, temperature, power.",
      "You have a maintenance log that is **patchy, free-text, or missing.**",
      "Classic supervised models need **labelled failures** you can't supply.",
      "So the project stalls before it starts — or ships a black box nobody trusts.",
      "There is a better starting point than 'wait until we have labels.'"]),

    ("twocol", "02", "Two ways to do predictive maintenance",
     "Supervised (the usual ask)", [
         "Needs a labelled failure history",
         "Blind to failure modes it never saw labelled",
         "Months of data collection before value",
         "Hard to start on a new asset",
     ],
     "Self-supervised (this)", [
         "Needs only ordinary telemetry",
         "Learns 'normal', flags any drift from it",
         "Value from day one, in shadow",
         "Cold-starts from fleet peers",
     ]),

    ("bullets", "03", "The solution: learn 'normal', flag the drift",
     "It predicts each sensor from the others. When reality drifts from what the neighbours imply, that gap "
     "is your early warning.",
     ["**No labels.** Every training target is another sensor — so it trains on plain operating data.",
      "**Multi-sensor.** A real fault is a pattern across sensors, not one needle in the red.",
      "**Patient.** It flags only persistent, trending, consistent drift — not a one-second blip.",
      "**Calibrated to each asset** against its own healthy history, not a generic threshold.",
      "**Honest.** On an unfamiliar asset it says 'not sure' instead of guessing."]),

    ("bullets", "04", "It names the problem — and the lead time",
     "Not one anonymous 'anomaly score', but an actionable advisory.",
     ["**Which** kind: bearing, cavitation, sensor fault, energy loss, oscillation — or an honest 'unknown'.",
      "**How urgent:** an estimated lead time from the degradation trend.",
      "**Why:** the sensors driving the call, so a human can act on it.",
      "**One advisory per developing fault**, not an alarm every second.",
      "Everything a planner needs to schedule an inspection — and nothing that touches the machine."]),

    ("bullets", "05", "It gets smarter every shift — with no redeploy",
     "The one form of supervision that arrives for free: your operators.",
     ["Mark an advisory **confirmed** or **false-positive** — that's the whole interface.",
      "A false alarm **raises** that threshold; a real one keeps it keen — bounded and rate-limited.",
      "It learns **in place**: no rebuild, no restart, no downtime.",
      "It **freezes** learning during abnormal periods so a bad patch can't poison it.",
      "Learning is **persisted** — it survives a restart and can be rolled back."]),

    ("metrics", "06", "Proof on the bench (be honest about what it means)",
     "Deterministic tests on a realistic synthetic corpus — not a field accuracy claim.",
     [("0", "Failure labels used\nin training"),
      ("5 / 5", "Fault families separated\nwithout labels"),
      ("14 + 5", "Java + Python tests\npassing"),
      ("ADVISORY", "Safety ceiling,\nnever exceeded")]),

    ("twocol", "07", "What it will do — and what it won't claim",
     "It will", [
         "Flag developing degradation early",
         "Name the likely family + a lead time",
         "Cut its own false alarms from feedback",
         "Run entirely on your premises",
     ],
     "It won't (yet / ever)", [
         "Claim a field rate from a bench test",
         "Be certain of a family before confirmations",
         "Ever actuate, schedule, or trip a device",
         "Work without a mostly-healthy baseline",
     ]),

    ("bullets", "08", "Safe by construction",
     "The safety story is not a policy — it's in the code.",
     ["**Advisory-only invariant** that cannot be switched off.",
      "**No path to an actuator** anywhere in the model.",
      "The **hard safety layer** (interlocks, gates) is deterministic and lives outside the learned path.",
      "**Domain-shift and uncertainty** are first-class, so it can decline to over-claim.",
      "**Open core (BSD-3)** — auditable end to end."]),

    ("bullets", "09", "Deploys where your data already is",
     "It meets your plant, not the other way around.",
     ["Ingests **OPC UA** and **MQTT / Sparkplug B** — the tags you already publish.",
      "Runs **local**, or **HTTP / gRPC** across a fleet.",
      "Emits advisories to **JSONL, Kafka, or MQTT** — into your SIEM, historian, or dashboard.",
      "Starts in **shadow**, graduates to **advisory** — and never further.",
      "Trained from your historian in an afternoon; improving from feedback thereafter."]),

    ("closing", "Send us a month of telemetry. We'll show you the drift.",
     ["No labels needed. No agent on the PLC. No actuation — ever.",
      "Shadow-mode pilot on one asset class, on your premises, with your operators in the loop."],
     "Start a label-free pilot",
     ["Jneopallium · Self-Supervised Maintenance Guardian",
      "Dmytro Rakovskyi — Kharkiv, Ukraine · BSD 3-Clause core"]),
]


_UK = [
    ("cover", "Jneopallium · Self-Supervised Maintenance Guardian",
     "Прогнозуйте відмови ще до того, як маєте історію відмов",
     "Прогнозне обслуговування без міток, що вчиться з вашої телеметрії — і гострішає завдяки вашим "
     "операторам, без переустановлення",
     "На місці · Тінь → Рекомендація · Мітки не потрібні · Ніколи не керує · Ядро BSD-3"),

    ("bullets", "01", "Дані у вас є. Міток — немає.",
     "Прогнозне обслуговування зазвичай вимагає єдиного, чого у вас немає: чистої історії минулих відмов.",
     ["У вас **роки телеметрії** — тиск, витрата, вібрація, температура, потужність.",
      "У вас журнал обслуговування, що **уривчастий, у вільній формі або відсутній.**",
      "Класичні керовані моделі потребують **розмічених відмов**, яких ви не можете надати.",
      "Тож проєкт застрягає ще до старту — або постачає чорну скриньку, якій ніхто не довіряє.",
      "Є краща відправна точка, ніж «зачекаємо, доки з'являться мітки»."]),

    ("twocol", "02", "Два способи робити прогнозне обслуговування",
     "Кероване (звичайна вимога)", [
         "Потрібна розмічена історія відмов",
         "Сліпе до режимів, яких не бачило в мітках",
         "Місяці збору даних до цінності",
         "Важко почати на новому активі",
     ],
     "Самокероване (це)", [
         "Потрібна лише звичайна телеметрія",
         "Вчить «норму», позначає будь-який дрейф",
         "Цінність з першого дня, у тіні",
         "Холодний старт від однолітків парку",
     ]),

    ("bullets", "03", "Рішення: вивчити «норму», позначити дрейф",
     "Воно передбачає кожен сенсор за іншими. Коли реальність відхиляється від того, що передбачають "
     "сусіди, цей розрив і є вашим раннім попередженням.",
     ["**Без міток.** Кожна ціль навчання — інший сенсор, тож воно вчиться на звичайних робочих даних.",
      "**Багатосенсорне.** Справжня несправність — це закономірність між сенсорами, а не одна стрілка.",
      "**Терпляче.** Позначає лише стійкий, трендовий, узгоджений дрейф — не миттєвий сплеск.",
      "**Каліброване під кожен актив** за його власною здоровою історією, не загальним порогом.",
      "**Чесне.** На незнайомому активі каже «не впевнений» замість здогадок."]),

    ("bullets", "04", "Воно називає проблему — і запас часу",
     "Не один анонімний «бал аномалії», а придатна до дії рекомендація.",
     ["**Який** тип: підшипник, кавітація, відмова сенсора, втрата енергії, коливання — чи чесне «невідоме».",
      "**Наскільки терміново:** оцінений запас часу за трендом деградації.",
      "**Чому:** сенсори, що зумовили висновок, щоб людина могла діяти.",
      "**Одна рекомендація на несправність, що розвивається**, а не тривога щосекунди.",
      "Усе, що потрібно планувальнику для огляду — і нічого, що торкається машини."]),

    ("bullets", "05", "Воно розумнішає щозміни — без переустановлення",
     "Єдина форма нагляду, що надходить безкоштовно: ваші оператори.",
     ["Позначте рекомендацію як **підтверджену** чи **хибну** — ось і весь інтерфейс.",
      "Хибна тривога **підіймає** цей поріг; справжня тримає його гострим — обмежено й з обмеженням частоти.",
      "Воно вчиться **на місці**: без перезбирання, без перезапуску, без простою.",
      "Воно **заморожує** навчання під час аномальних періодів, щоб поганий відтинок не отруїв його.",
      "Навчання **зберігається** — переживає перезапуск і може бути відкочене."]),

    ("metrics", "06", "Доказ на стенді (чесно про те, що це означає)",
     "Детерміновані тести на реалістичному синтетичному корпусі — не заявка про польову точність.",
     [("0", "Міток відмов\nу навчанні"),
      ("5 / 5", "Сімейств несправностей\nвідокремлено без міток"),
      ("14 + 5", "Java + Python тести\nуспішні"),
      ("ADVISORY", "Стеля безпеки,\nніколи не перевищена")]),

    ("twocol", "07", "Що воно зробить — і чого не заявлятиме",
     "Воно зробить", [
         "Рано позначить деградацію, що розвивається",
         "Назве ймовірне сімейство + запас часу",
         "Знизить власні хибні тривоги зі зв'язку",
         "Працюватиме цілком на вашому майданчику",
     ],
     "Воно не (поки / ніколи)", [
         "Не заявить польову частку зі стендового тесту",
         "Не буде певним щодо сімейства до підтверджень",
         "Не керуватиме, не планує, не спрацює пристроєм",
         "Не працює без переважно здорового базового рівня",
     ]),

    ("bullets", "08", "Безпечне за побудовою",
     "Історія безпеки — не політика, а код.",
     ["**Інваріант «лише рекомендація»**, який не можна вимкнути.",
      "**Жодного шляху до виконавчого механізму** ніде в моделі.",
      "**Жорсткий шар безпеки** (блокування, шлюзи) детермінований і живе поза навченим шляхом.",
      "**Зсув домену й невизначеність** повноправні, тож воно може відмовитися перебільшувати.",
      "**Відкрите ядро (BSD-3)** — аудитоване від початку до кінця."]),

    ("bullets", "09", "Розгортається там, де вже є ваші дані",
     "Воно підлаштовується під вашу установку, а не навпаки.",
     ["Приймає **OPC UA** та **MQTT / Sparkplug B** — теги, які ви вже публікуєте.",
      "Працює **local**, або **HTTP / gRPC** по парку.",
      "Видає рекомендації в **JSONL, Kafka чи MQTT** — у ваш SIEM, історіан чи дашборд.",
      "Стартує в **тіні**, переходить у **рекомендацію** — і не далі.",
      "Навчається з вашого історіана за пів дня; далі вдосконалюється зі зворотного зв'язку."]),

    ("closing", "Надішліть нам місяць телеметрії. Ми покажемо дрейф.",
     ["Мітки не потрібні. Жодного агента на ПЛК. Жодного керування — ніколи.",
      "Пілот у режимі тіні на одному класі активів, на вашому майданчику, з вашими операторами в циклі."],
     "Почати пілот без міток",
     ["Jneopallium · Self-Supervised Maintenance Guardian",
      "Дмитро Раковський — Харків, Україна · Ядро BSD 3-Clause"]),
]
