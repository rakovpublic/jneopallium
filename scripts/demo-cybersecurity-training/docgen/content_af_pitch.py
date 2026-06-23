# -*- coding: utf-8 -*-
"""Ad-Fraud Guardian — pitch deck (EN + UK), slide DSL."""

from __future__ import annotations


def pitch(lang: str) -> list:
    return _EN if lang == "en" else _UK


_EN = [
    ("cover", "Jneopallium · Ad-Fraud Guardian",
     "Stop Paying for Clicks That Never Happened",
     "Multi-label invalid-traffic intelligence — names the fraud, shows the evidence, recommends the action",
     "On-premises · Shadow → Advisory · Privacy by design · macro-F1 ≈ 0.97 · BSD-3 core"),

    ("bullets", "01", "A chunk of your ad budget buys nothing",
     "Industry estimates put invalid-traffic losses in the tens of billions of dollars a year.",
     ["**Bots** click and view ads no human ever sees.",
      "**Click farms & incentivized installs** look like users but never become customers.",
      "**Attribution fraud** (click spam, click injection) pays the wrong partner for conversions you'd "
      "have won anyway.",
      "**Made-for-advertising & spoofed inventory** soak up spend behind faked supply paths.",
      "Every one of these quietly drains ROAS — and a single \"fraud %\" can't tell them apart."]),

    ("twocol", "02", "One \"fraud score\" can't run your business",
     "A single risk number", [
         "\"73% fraud\" — but **which** kind?",
         "No evidence, so no defensible action",
         "Wrong action withholds a real payout",
         "One threshold over- or under-flags everything",
     ],
     "What you actually need", [
         "**Which** fraud family, named",
         "**Why** — the contributing evidence",
         "**What to do** — a proportionate action",
         "**Calibrated** probabilities you can trust",
     ]),

    ("bullets", "03", "The solution: name it, prove it, recommend it",
     "A multi-label invalid-traffic engine — not one score, ten calibrated verdicts.",
     ["**Multi-label.** One event can be flagged as several things at once, each with its own probability.",
      "**Evidence-first.** Every verdict carries the signals and a human-readable reason.",
      "**Advisory by construction.** Candidate actions only — it never blocks a publisher or withholds a "
      "payout on its own.",
      "**Private by design.** Identifiers are hashed; raw IPs, location, and user-agents are never "
      "exported.",
      "**Explainable, on-premises, open core** — it fits review, audit, and a publisher appeal."]),

    ("bullets", "04", "It reads every event across many clocks",
     "Instant checks plus the slow truth that only arrives days later.",
     ["**Instant:** forged signatures, replayed events, click-before-impression — caught on the spot.",
      "**Per-session:** click-injection timing, click-spam volume, attribution anomalies.",
      "**Minutes:** click-farm device/account fanout graphs.",
      "**Days:** the real truth about quality — retention, refunds, chargebacks, uninstalls.",
      "One engine weighs an instant signal against a multi-day quality outcome in a single verdict."]),

    ("bullets", "05", "Ten fraud families, named",
     "Each gets its own calibrated probability — and the right action attaches to the right problem.",
     ["**bot** · **clickFarm** · **incentivized** — manufactured or low-value engagement",
      "**clickSpam** · **clickInjection** · **attributionHijack** — stealing conversion credit",
      "**eventSpoofing** · **inventorySpoofing** — forged events and faked supply paths",
      "**accidentalOrLowValue** · **unknownSuspicious** — the model says \"odd, not necessarily fraud\" "
      "instead of over-accusing."]),

    ("bullets", "06", "Every verdict carries its evidence",
     "Not a black box — an auditable decision your analysts and partners can trust.",
     ["`probabilities`: clickInjection 0.94, attributionHijack 0.91, bot 0.03 …",
      "`overallInvalidTrafficProbability`: 0.95",
      "`recommendation`: **HOLD_PAYOUT_CANDIDATE** (a candidate — never auto-executed)",
      "`reasons`: \"conversion observed before click\", \"click injection timing anomaly\"",
      "`mode`: **ADVISORY** — and the model never accuses a named person."]),

    ("metrics", "07", "Proof: we measured, diagnosed, and lifted it",
     "A deliberate model upgrade raised detection quality sharply on the reference corpus.",
     [("0.97", "Macro-F1 across the\nten fraud labels"),
      ("0.66 → 0.97", "Before → after\nthe model upgrade"),
      ("12 / 12", "Java runtime tests\npassing"),
      ("ADVISORY", "Safety ceiling,\nnever exceeded")]),

    ("bullets", "08", "Safe and private by construction",
     "Easy to demonstrate, validate, and deploy next to billing.",
     ["**Advisory / shadow by default.** It recommends; humans decide. It never blocks or withholds on "
      "its own.",
      "**Never an accusation.** It emits invalid-traffic *evidence*, not a claim that a person committed "
      "fraud.",
      "**Privacy first.** HMAC-hashed identifiers; no raw IP, geo, or user-agent ever exported; missing ≠ "
      "zero.",
      "**Safe degradation.** If the model is unavailable it falls back to transparent rules — no broken "
      "request path.",
      "**Honest about evidence.** Reference metrics are pipeline evidence; field accuracy is earned on "
      "your data."]),

    ("bullets", "09", "The money math is simple",
     "Recover wasted spend and stop paying for fraudulent conversions. Illustrative example:",
     ["Annual ad / payout spend  →  **$50,000,000**",
      "Invalid-traffic share (conservative)  →  **10%**  =  $5,000,000 exposed",
      "Detectable, actionable share recovered  →  **20–40%**",
      "**Recovered value  →  $1,000,000 – $2,000,000 / year**",
      "Price the product at a small fraction of verified savings — the ROI is not close."]),

    ("bullets", "10", "Land and expand — paid at every step",
     "A low-risk path from a log export to a recurring subscription. (Indicative pricing.)",
     ["**Offline replay.** Score an exported event log; deliver named findings, evidence, and a wasted-"
      "spend estimate. **€5–15k / assessment.**",
      "**Shadow pilot.** Read-only scoring beside production; compare to your chargebacks and MMP rulings. "
      "**€15–40k**, credited to year one.",
      "**Advisory subscription.** Operator-visible verdicts to analysts and billing review. **Per "
      "event-volume band, per site / year.**",
      "**Bounded enforcement.** Only after first-party validation + legal review + appeal/rollback — "
      "narrowly scoped, never silent blocking."]),

    ("twocol", "11", "Where we fit — and where we won't overreach",
     "We add (high value)", [
         "Naming the exact invalid-traffic family",
         "Evidence for every advisory",
         "Calibrated, multi-label probabilities",
         "A safe shadow → advisory path",
     ],
     "We won't do (by design)", [
         "Silently block a publisher",
         "Auto-withhold a payout",
         "Accuse a named person of fraud",
         "Replace your MMP / SSP / billing",
     ]),

    ("closing", "Send us a log export. We'll show you the leak.",
     ["Export an anonymised event log, or point us at a feed — read-only, privacy-preserving.",
      "We return named invalid-traffic findings, the evidence, and an estimated wasted-spend impact.",
      "Open-source demo today · paid assessment in weeks · shadow pilot next quarter."],
     "Book a paid invalid-traffic assessment.",
     ["Dmytro Rakovskyi · Kharkiv, Ukraine",
      "github.com/rakovpublic/jneopallium",
      "IJSR 13(7), 2024 · DOI 10.21275/SR24703042047"]),
]


_UK = [
    ("cover", "Jneopallium · Ad-Fraud Guardian",
     "Припиніть платити за кліки, яких не було",
     "Багатоміткова інтелектуальність невалідного трафіку — називає фрод, показує докази, рекомендує дію",
     "Локально · Тінь → Рекомендації · Приватність за задумом · macro-F1 ≈ 0.97 · Відкрите ядро BSD-3"),

    ("bullets", "01", "Частина вашого рекламного бюджету не купує нічого",
     "Галузеві оцінки визначають втрати від невалідного трафіку в десятки мільярдів доларів на рік.",
     ["**Боти** клікають і переглядають рекламу, якої не бачить жодна людина.",
      "**Клік-ферми та мотивовані інсталяції** виглядають як користувачі, але ніколи не стають клієнтами.",
      "**Фрод атрибуції** (click spam, click injection) платить не тому партнеру за конверсії, які ви б "
      "виграли й так.",
      "**Зроблений для реклами та підроблений інвентар** поглинають бюджет за фейковими шляхами постачання.",
      "Кожне з цього тихо виснажує ROAS — а єдиний «% фроду» не відрізнить їх."]),

    ("twocol", "02", "Єдиний «бал фроду» не керуватиме вашим бізнесом",
     "Одне число ризику", [
         "«73% фроду» — але **який** вид?",
         "Без доказів — без захищеної дії",
         "Неправильна дія утримує реальну виплату",
         "Один поріг пере- чи недопозначає все",
     ],
     "Що вам справді потрібно", [
         "**Яка** родина фроду, названа",
         "**Чому** — докази, що долучилися",
         "**Що робити** — пропорційна дія",
         "**Каліброві** ймовірності, яким можна вірити",
     ]),

    ("bullets", "03", "Рішення: назви, доведи, рекомендуй",
     "Багатоміткова мережа невалідного трафіку — не один бал, а десять каліброваних вердиктів.",
     ["**Багатоміткова.** Одну подію можна позначити кількома мітками одразу, кожна зі своєю ймовірністю.",
      "**Спершу докази.** Кожен вердикт несе сигнали й причину, зрозумілу людині.",
      "**Рекомендаційна за побудовою.** Лише кандидатні дії — сама ніколи не блокує паблішера й не утримує "
      "виплату.",
      "**Приватна за задумом.** Ідентифікатори хешуються; сирі IP, локація й user-agent ніколи не "
      "експортуються.",
      "**Пояснювана, локальна, відкрите ядро** — пасує перегляду, аудиту й апеляції паблішера."]),

    ("bullets", "04", "Вона читає кожну подію на багатьох годинниках",
     "Миттєві перевірки плюс повільна правда, що приходить лише за дні.",
     ["**Миттєво:** підроблені підписи, повторені події, клік-перед-показом — ловляться одразу.",
      "**Посесійно:** час click injection, обсяг click spam, аномалії атрибуції.",
      "**Хвилини:** графи фанауту пристроїв/акаунтів клік-ферм.",
      "**Дні:** справжня правда про якість — утримання, повернення, чарджбеки, видалення.",
      "Один рушій зважує миттєвий сигнал проти багатоденного результату якості в одному вердикті."]),

    ("bullets", "05", "Десять родин фроду, названих",
     "Кожна отримує власну калібровану ймовірність — і правильна дія прив'язується до правильної проблеми.",
     ["**bot** · **clickFarm** · **incentivized** — вироблена чи низькоцінна залученість",
      "**clickSpam** · **clickInjection** · **attributionHijack** — крадіжка заслуги за конверсію",
      "**eventSpoofing** · **inventorySpoofing** — підроблені події й фейкові шляхи постачання",
      "**accidentalOrLowValue** · **unknownSuspicious** — модель каже «дивно, не обов'язково фрод», а не "
      "перезвинувачує."]),

    ("bullets", "06", "Кожен вердикт несе свої докази",
     "Не чорна скриня — аудитоване рішення, якому довіряють ваші аналітики й партнери.",
     ["`probabilities`: clickInjection 0.94, attributionHijack 0.91, bot 0.03 …",
      "`overallInvalidTrafficProbability`: 0.95",
      "`recommendation`: **HOLD_PAYOUT_CANDIDATE** (кандидат — ніколи не автовиконується)",
      "`reasons`: «конверсія спостережена до кліку», «часова аномалія click injection»",
      "`mode`: **ADVISORY** — і модель ніколи не звинувачує названу особу."]),

    ("metrics", "07", "Доказ: ми виміряли, діагностували й підняли",
     "Навмисне оновлення моделі різко підняло якість виявлення на еталонному корпусі.",
     [("0.97", "Macro-F1 по десяти\nмітках фроду"),
      ("0.66 → 0.97", "До → після\nоновлення моделі"),
      ("12 / 12", "Тести середовища\nвиконання Java"),
      ("ADVISORY", "Стеля безпеки,\nніколи не перевищена")]),

    ("bullets", "08", "Безпечна й приватна за побудовою",
     "Легко продемонструвати, валідувати й розгорнути поруч із білінгом.",
     ["**Рекомендаційно / тінь за замовчуванням.** Вона радить; рішення приймають люди. Сама ніколи не "
      "блокує й не утримує.",
      "**Ніколи не звинувачення.** Вона видає *докази* невалідного трафіку, а не твердження, що людина "
      "скоїла фрод.",
      "**Спершу приватність.** HMAC-хешовані ідентифікатори; сирі IP, гео чи user-agent ніколи не "
      "експортуються; відсутнє ≠ нуль.",
      "**Безпечна деградація.** Якщо модель недоступна, переходить на прозорі правила — без зламаного "
      "шляху запиту.",
      "**Чесна щодо доказів.** Еталонні метрики — це докази конвеєра; польова точність заслуговується на "
      "ваших даних."]),

    ("bullets", "09", "Грошова математика проста",
     "Поверніть змарнований бюджет і припиніть платити за шахрайські конверсії. Ілюстративний приклад:",
     ["Річні витрати на рекламу / виплати  →  **$50 000 000**",
      "Частка невалідного трафіку (консервативно)  →  **10%**  =  $5 000 000 під ризиком",
      "Виявлена, придатна до дії частка, повернена  →  **20–40%**",
      "**Повернена цінність  →  $1 000 000 – $2 000 000 / рік**",
      "Ціна продукту — мала частка перевірених заощаджень; ROI навіть не близький."]),

    ("bullets", "10", "Заходь і розширюйся — оплата на кожному кроці",
     "Низькоризиковий шлях від експорту логів до регулярної підписки. (Орієнтовні ціни.)",
     ["**Офлайн-відтворення.** Оцініть експортований лог подій; названі знахідки, докази й оцінка "
      "змарнованого бюджету. **€5–15k / оцінка.**",
      "**Тіньовий пілот.** Скоринг лише для читання поруч із промислом; порівняння з вашими чарджбеками й "
      "рішеннями MMP. **€15–40k**, зараховується в рік 1.",
      "**Рекомендаційна підписка.** Видимі оператору вердикти аналітикам і перегляду білінгу. **За смугою "
      "обсягу подій, на майданчик / рік.**",
      "**Обмежене виконання.** Лише після валідації на власних даних + юр. перегляду + апеляції/відкату — "
      "вузько, ніколи тихе блокування."]),

    ("twocol", "11", "Де ми вписуємося — і де не перевищуємо",
     "Ми додаємо (висока цінність)", [
         "Назву точної родини невалідного трафіку",
         "Докази для кожної рекомендації",
         "Каліброві багатомиткові ймовірності",
         "Безпечний шлях тінь → рекомендації",
     ],
     "Ми не робимо (за задумом)", [
         "Тихо блокувати паблішера",
         "Авто-утримувати виплату",
         "Звинувачувати названу особу у фроді",
         "Замінювати ваш MMP / SSP / білінг",
     ]),

    ("closing", "Надішліть нам експорт логів. Ми покажемо витік.",
     ["Експортуйте анонімізований лог подій або вкажіть канал — лише для читання, зі збереженням "
      "приватності.",
      "Ми повертаємо названі знахідки невалідного трафіку, докази й оцінений вплив змарнованого бюджету.",
      "Відкрите демо сьогодні · платна оцінка за тижні · тіньовий пілот наступного кварталу."],
     "Замовте платну оцінку невалідного трафіку.",
     ["Дмитро Раковський · Харків, Україна",
      "github.com/rakovpublic/jneopallium",
      "IJSR 13(7), 2024 · DOI 10.21275/SR24703042047"]),
]
