# -*- coding: utf-8 -*-
"""Pitch deck content (EN + UK), slide DSL."""

from __future__ import annotations


def pitch(lang: str) -> list:
    return _EN if lang == "en" else _UK


_EN = [
    ("cover", "Jneopallium · Cybersecurity",
     "Your Network Deserves an Immune System",
     "Temporal threat correlation that catches the attack, not just the alert",
     "Demo 06 · cybersecurity-temporal-threat-correlator · ADVISORY-safe · BSD-3"),

    ("bullets", "01", "The attacker is already inside — for weeks",
     "Detection is a race against dwell time, and most teams are losing it.",
     ["The average intruder dwells for **weeks** before anyone notices — long enough to steal credentials, "
      "spread, and exfiltrate.",
      "Every wasted hour widens the blast radius: more hosts, more data, more cost.",
      "By the time a single alert is convincing on its own, **the damage is already done**.",
      "The question that matters is not \"is this event bad?\" — it is \"is an attack unfolding **right "
      "now**?\""]),

    ("twocol", "02", "Why today's tools keep missing it",
     "One event at a time", [
         "Scores each login, file, or packet **in isolation**",
         "**Low-and-slow** theft never trips a threshold",
         "Drowns analysts in **thousands of daily alerts**",
         "Can't tell a 3 a.m. finance-server script from routine maintenance",
     ],
     "What's actually needed", [
         "Connect events **across time and sources**",
         "Recognise the **shape of an attack chain**",
         "One evidenced advisory, **not a thousand alarms**",
         "Apply context **without erasing the evidence**",
     ]),

    ("bullets", "03", "The big idea: a digital immune system",
     "We borrowed the best intrusion-detection system on Earth — biology's.",
     ["**Fast first-responders** match known threats instantly (like macrophages).",
      "**Adaptive learners** model what's normal for each user and host (like T-cells).",
      "**Immune memory** recalls past campaigns so new ones are caught faster (like memory B-cells).",
      "**Self-tolerance** is a hard, un-overridable rule never to harm your own critical systems.",
      "**Inflammation always resolves** — every containment expires automatically. Nothing is permanent."]),

    ("bullets", "04", "How it works — in three moves",
     None,
     ["**1 · Translate.** Every login, process, DNS lookup, network flow, and threat-intel tip becomes a "
      "typed signal in one common language.",
      "**2 · Correlate.** The engine connects those signals across overlapping time windows — seconds, "
      "minutes, and the full 30–120 minute incident window — using **event time**, even when data arrives "
      "late or out of order.",
      "**3 · Advise.** It outputs a single recommendation with a probability, a response band, and a full "
      "evidence trail you can explain to any auditor."]),

    ("bullets", "05", "It sees the chain, not the link",
     "A real intrusion is an ordered story. We read the whole story.",
     ["unusual login **→** encoded PowerShell **→** lateral movement **→** DNS **→** C2 beaconing **→** "
      "data exfiltration",
      "Each step alone looks almost normal. **In sequence, within the window, it's an intrusion.**",
      "Ordered transitions score far higher than the same events scattered at random.",
      "And the **low-and-slow leak** — a few megabytes an hour — finally becomes visible when correlated "
      "over the incident window."]),

    ("bullets", "06", "Three streams. Three correct calls. Zero blocks.",
     "The exact scenario one-event tools get wrong — handled right.",
     ["**The real attack** is raised as a temporal threat advisory, with learning frozen so it can't "
      "poison \"normal.\"",
      "**Planned maintenance** is calmed by context — but its evidence is still recorded for audit.",
      "**The quiet data leak** is surfaced by correlation, not by a single threshold.",
      "In every case the output is an **advisory with evidence** — never a silent, business-breaking "
      "block."]),

    ("bullets", "07", "Why we win",
     "What competitors structurally cannot copy overnight.",
     ["**Temporal correlation by design** — built around the attack chain, not a single-row classifier.",
      "**Safe by construction** — ADVISORY by default; hard safety gates are fixed, never learned.",
      "**Glass-box, not black-box** — training emits the actual deployable network; every weight, gate, "
      "and threshold is inspectable, and every advisory carries its evidence lineage.",
      "**Train-to-deploy in one step** — what you train is the exact network you run; no model-translation "
      "gap.",
      "**Multi-source from day one** — authentication, process, DNS, flow, threat-intel, asset, and "
      "maintenance context, fused.",
      "**Built on published science** — the Jneopallium framework, peer-reviewed in IJSR (2024)."]),

    ("bullets", "08", "It cannot take your business offline",
     "The number-one objection to autonomous security — answered structurally.",
     ["**Advisory by default.** It recommends; humans decide. Enforcement needs a separate, approved "
      "safety case.",
      "**Critical assets are protected by a fixed gate** that no runtime signal — and no attacker in the "
      "data path — can override.",
      "**Baseline freezes during attacks**, defeating slow \"baseline-poisoning\" tricks.",
      "**Every quarantine expires automatically.** Being wrong is, by design, safe."]),

    ("metrics", "09", "Proof: the pipeline is real",
     "Perfect separation on the reference corpus — machinery proven, ready for your data.",
     [("1.0", "Precision & recall\n(reference corpus)"),
      ("0.0", "False-positive rate"),
      ("100 GiB", "Logical-corpus scale\nproven on a workstation"),
      ("ADVISORY", "Safety ceiling,\nnever exceeded")]),

    ("bullets", "10", "Honest about the evidence",
     "Because a vendor who hides this can't be trusted with your network.",
     ["Those perfect scores are **pipeline evidence** on a deterministic reference corpus — they prove the "
      "system is wired correctly end to end.",
      "Real-world accuracy is **earned on your data**: LANL, ToN_IoT, OpTC, CIC/CSE-CIC, UNSW-NB15, and "
      "controlled CALDERA campaigns plug straight in.",
      "The training pipeline already enforces leakage-safe splits, calibration, and an F1 quality gate.",
      "**We lead with this caveat on purpose.** The honesty is part of the product."]),

    ("bullets", "11", "Fits where you already are",
     "Built to slot into the enterprise, not replace it.",
     ["**Kafka-native** event stream design — point it at your existing telemetry bus.",
      "Deploys **local, HTTP cluster, or gRPC** (FPGA-capable) — laptop pilot to enterprise scale.",
      "**Shadow-mode rollout:** run silently, compare against your incident tickets, then promote to "
      "operator-visible advisory.",
      "Detectors are **swappable behind interfaces** — bring your own signature engine or anomaly model.",
      "**Open source, BSD-3-Clause** — no lock-in, full auditability."]),

    ("twocol", "12", "Where it pays off",
     "Best fit", [
         "SOCs drowning in alert fatigue",
         "Enterprises chasing **dwell-time** reduction",
         "Regulated sectors needing **explainable** verdicts",
         "MSSPs needing multi-tenant, auditable triage",
     ],
     "What you get", [
         "Fewer, **higher-quality** advisories",
         "Earlier detection of **multi-step** attacks",
         "A full **evidence trail** for every call",
         "A safe path from **shadow → advisory**",
     ]),

    ("bullets", "13", "Where it's going",
     "A clear roadmap on a proven foundation.",
     ["Validation on external multi-source datasets to convert pipeline evidence into **field accuracy**.",
      "Larger sequence models (**GRU / TCN / transformer**) behind the same transparent interface.",
      "Production Kafka bridge, containerised cluster deployment, and hosted model packaging.",
      "Optional, non-blocking **LLM knowledge-base** advisory for analyst context — never in the "
      "critical path."]),

    ("closing", "Let's catch the attack — not just the alert",
     ["A digital immune system for your network: temporal, transparent, and safe by construction.",
      "Pilot it in shadow mode on your own telemetry in weeks, not quarters.",
      "Open source today. Production-ready advisory on your roadmap."],
     "Let's run a pilot on your data.",
     ["Dmytro Rakovskyi · Kharkiv, Ukraine",
      "github.com/rakovpublic/jneopallium",
      "IJSR 13(7), 2024 · DOI 10.21275/SR24703042047"]),
]


_UK = [
    ("cover", "Jneopallium · Кібербезпека",
     "Ваша мережа заслуговує на імунну систему",
     "Часова кореляція загроз, що ловить атаку, а не лише сповіщення",
     "Демо 06 · cybersecurity-temporal-threat-correlator · безпечний режим ADVISORY · BSD-3"),

    ("bullets", "01", "Зловмисник уже всередині — тижнями",
     "Виявлення — це перегони з часом перебування, і більшість команд їх програє.",
     ["Середній зловмисник перебуває в мережі **тижнями**, перш ніж його помітять — досить, щоб украсти "
      "облікові дані, поширитися й вивести дані.",
      "Кожна змарнована година розширює радіус ураження: більше вузлів, більше даних, більше витрат.",
      "Коли окреме сповіщення стає переконливим саме по собі, **шкоду вже завдано**.",
      "Важливе питання не «чи погана ця подія?», а «чи розгортається атака **просто зараз**?»"]),

    ("twocol", "02", "Чому сьогоднішні інструменти її пропускають",
     "Одна подія за раз", [
         "Оцінює кожен вхід, файл чи пакет **ізольовано**",
         "**Повільний** витік ніколи не перетинає поріг",
         "Топить аналітиків у **тисячах щоденних тривог**",
         "Не відрізнить нічний скрипт на фінсервері від рутини",
     ],
     "Що насправді потрібно", [
         "Поєднувати події **в часі та між джерелами**",
         "Розпізнавати **форму ланцюга атаки**",
         "Одна обґрунтована рекомендація, **а не тисяча тривог**",
         "Застосовувати контекст **без стирання доказів**",
     ]),

    ("bullets", "03", "Головна ідея: цифрова імунна система",
     "Ми запозичили найкращу систему виявлення вторгнень на Землі — біологічну.",
     ["**Швидкі першочергові захисники** миттєво зіставляють відомі загрози (як макрофаги).",
      "**Адаптивні учні** моделюють норму кожного користувача й вузла (як T-клітини).",
      "**Імунна пам'ять** згадує минулі кампанії, тож нові ловляться швидше (як B-клітини пам'яті).",
      "**Самотолерантність** — жорстке правило, яке не можна скасувати: ніколи не шкодити власним "
      "критичним системам.",
      "**Запалення завжди вщухає** — кожне стримування завершується автоматично. Ніщо не постійне."]),

    ("bullets", "04", "Як це працює — у три ходи",
     None,
     ["**1 · Переклад.** Кожен вхід, процес, DNS-запит, мережевий потік і підказка кіберрозвідки стає "
      "типізованим сигналом єдиною спільною мовою.",
      "**2 · Кореляція.** Рушій поєднує ці сигнали в перекривних часових вікнах — секунди, хвилини й повне "
      "вікно інциденту 30–120 хвилин — за **часом події**, навіть коли дані надходять із запізненням чи не "
      "за порядком.",
      "**3 · Рекомендація.** Він видає одну рекомендацію з імовірністю, смугою відповіді та повним ланцюгом "
      "доказів, який можна пояснити будь-якому аудитору."]),

    ("bullets", "05", "Він бачить ланцюг, а не ланку",
     "Справжнє вторгнення — впорядкована історія. Ми читаємо всю історію.",
     ["незвичний вхід **→** закодований PowerShell **→** бічний рух **→** DNS **→** маяки C2 **→** витік даних",
      "Кожен крок окремо виглядає майже нормально. **У послідовності, у межах вікна — це вторгнення.**",
      "Впорядковані переходи набирають значно більше, ніж ті самі події, розкидані випадково.",
      "А **повільний витік** — кілька мегабайтів на годину — нарешті стає видимим, коли корелюється за "
      "вікно інциденту."]),

    ("bullets", "06", "Три потоки. Три правильні рішення. Нуль блокувань.",
     "Саме той сценарій, який інструменти «одна подія» помиляють — оброблено правильно.",
     ["**Справжня атака** піднята як часова рекомендація про загрозу, із замороженим навчанням, тож вона не "
      "може отруїти «норму».",
      "**Планове обслуговування** заспокоєне контекстом — але його докази все одно записано для аудиту.",
      "**Тихий витік даних** виявлено кореляцією, а не одним порогом.",
      "У кожному разі вихід — це **рекомендація з доказами**, а не тихе блокування, що ламає бізнес."]),

    ("bullets", "07", "Чому ми перемагаємо",
     "Те, що конкуренти структурно не скопіюють за ніч.",
     ["**Часова кореляція за задумом** — побудовано довкола ланцюга атаки, а не класифікатора одного рядка.",
      "**Безпечний за побудовою** — ADVISORY за замовчуванням; жорсткі запобіжники фіксовані, ніколи не "
      "навчені.",
      "**Скляна скриня, а не чорна** — навчання видає власне готову до розгортання мережу; кожну вагу, "
      "гейт і поріг можна перевірити, а кожна рекомендація несе свій родовід доказів.",
      "**Навчив — і розгорнув** — те, що ви навчаєте, і є мережею, яку ви запускаєте; без розриву на "
      "переклад моделі.",
      "**Багатоджерельність із першого дня** — автентифікація, процеси, DNS, потоки, кіберрозвідка, активи "
      "й контекст обслуговування, поєднані.",
      "**На опублікованій науці** — фреймворк Jneopallium, рецензований у IJSR (2024)."]),

    ("bullets", "08", "Він не може вивести ваш бізнес з ладу",
     "Заперечення №1 щодо автономної безпеки — відповідь структурна.",
     ["**Рекомендації за замовчуванням.** Він радить; рішення приймають люди. Примус потребує окремого "
      "погодженого обґрунтування безпеки.",
      "**Критичні активи захищені фіксованим запобіжником**, який не може скасувати жоден сигнал — і жоден "
      "зловмисник у потоці даних.",
      "**Базова лінія заморожується під час атак**, перемагаючи повільні трюки «отруєння базової лінії».",
      "**Кожен карантин завершується автоматично.** Помилка — за задумом — безпечна."]),

    ("metrics", "09", "Доказ: конвеєр справжній",
     "Ідеальне розділення на еталонному корпусі — механізм доведено, готовий до ваших даних.",
     [("1.0", "Точність і повнота\n(еталонний корпус)"),
      ("0.0", "Рівень хибних спрацювань"),
      ("100 ГіБ", "Логічний масштаб корпусу,\nдоведено на робочій станції"),
      ("ADVISORY", "Стеля безпеки,\nніколи не перевищена")]),

    ("bullets", "10", "Чесно про докази",
     "Бо постачальнику, який це ховає, не можна довірити вашу мережу.",
     ["Ті ідеальні оцінки — це **докази конвеєра** на детермінованому еталонному корпусі: вони доводять, що "
      "система правильно з'єднана наскрізно.",
      "Реальна точність **заслуговується на ваших даних**: LANL, ToN_IoT, OpTC, CIC/CSE-CIC, UNSW-NB15 і "
      "контрольовані кампанії CALDERA під'єднуються напряму.",
      "Конвеєр навчання вже застосовує розбиття без витоку, калібрування та критерій якості за F1.",
      "**Ми починаємо з цього застереження навмисно.** Чесність — частина продукту."]),

    ("bullets", "11", "Вписується туди, де ви вже є",
     "Створено, щоб вбудуватися в підприємство, а не замінити його.",
     ["**Нативний для Kafka** дизайн потоку подій — спрямуйте на наявну шину телеметрії.",
      "Розгортання **локально, HTTP-кластером або gRPC** (підтримка FPGA) — від пілота на ноутбуці до "
      "масштабу підприємства.",
      "**Розгортання в тіньовому режимі:** працює тихо, порівнюється з вашими тікетами інцидентів, потім "
      "переходить у видимий оператору рекомендаційний режим.",
      "Детектори **замінні за інтерфейсами** — приносьте власний сигнатурний рушій чи модель аномалій.",
      "**Відкритий код, BSD-3-Clause** — без прив'язки, повна можливість аудиту."]),

    ("twocol", "12", "Де це окупається",
     "Найкраще пасує", [
         "SOC, що тонуть у втомі від сповіщень",
         "Підприємства, що скорочують **час перебування**",
         "Регульовані галузі, яким потрібні **пояснювані** вердикти",
         "MSSP, яким потрібен багатоорендний аудитований тріаж",
     ],
     "Що ви отримуєте", [
         "Менше, але **якісніших** рекомендацій",
         "Раніше виявлення **багатокрокових** атак",
         "Повний **ланцюг доказів** для кожного рішення",
         "Безпечний шлях від **тіні → рекомендацій**",
     ]),

    ("bullets", "13", "Куди це рухається",
     "Чітка дорожня карта на доведеній основі.",
     ["Валідація на зовнішніх багатоджерельних наборах даних, щоб перетворити докази конвеєра на "
      "**польову точність**.",
      "Більші послідовнісні моделі (**GRU / TCN / трансформер**) за тим самим прозорим інтерфейсом.",
      "Промисловий міст Kafka, контейнерне кластерне розгортання й хостоване пакування моделей.",
      "Опційна, неблокувальна рекомендаційна **база знань на LLM** для контексту аналітика — ніколи на "
      "критичному шляху."]),

    ("closing", "Ловімо атаку — а не лише сповіщення",
     ["Цифрова імунна система для вашої мережі: часова, прозора й безпечна за побудовою.",
      "Пілот у тіньовому режимі на вашій телеметрії — за тижні, не за квартали.",
      "Відкритий код сьогодні. Промислові рекомендації — у вашій дорожній карті."],
     "Запустімо пілот на ваших даних.",
     ["Дмитро Раковський · Харків, Україна",
      "github.com/rakovpublic/jneopallium",
      "IJSR 13(7), 2024 · DOI 10.21275/SR24703042047"]),
]
