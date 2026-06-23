# -*- coding: utf-8 -*-
"""Ad-Fraud Guardian — architecture article (EN + UK), block DSL."""

from __future__ import annotations

DATE = "23 June 2026"
DATE_UK = "23 червня 2026"


def architecture(lang: str) -> list:
    return _EN if lang == "en" else _UK


_EN = [
    ("cover", "Jneopallium · Ad-Fraud Guardian",
     "Stop Paying for Traffic That Never Happened",
     "Multi-label invalid-traffic detection — complete architecture, explained for everyone",
     [("Document", "Architecture & Technical Overview"),
      ("Product", "Jneopallium Ad-Fraud Guardian (advertising-fraud module)"),
      ("Model", "advertising-fraud 1.0.0 · macro-F1 ≈ 0.97"),
      ("Safety mode", "ADVISORY / SHADOW (recommend-only)"),
      ("Author", "Dmytro Rakovskyi — Kharkiv, Ukraine"),
      ("Date", DATE),
      ("Audience", "Marketers, ad-ops, anti-fraud teams, engineers, newcomers"),
      ("License", "BSD 3-Clause")],
     "Architecture Article",
     "How a brain-inspired engine reads every ad event across many timescales, names the exact kind of "
     "invalid traffic, shows its evidence, and recommends an action — without ever silently blocking a "
     "publisher or withholding a payout. Written so a non-specialist can follow every step."),

    ("toc", "What's inside",
     ["Executive summary — the one-page version",
      "The problem: where the ad budget leaks",
      "The big idea: name the fraud, show the evidence",
      "The platform underneath: the Jneopallium framework",
      "Architecture overview: the eight-layer network",
      "What it sees: the canonical ad event",
      "The ten fraud families it scores",
      "Signals on many clocks",
      "From raw event to verdict: the evidence pipeline",
      "The trained model and the deployable network",
      "Safety and privacy by design",
      "How good is it? (and how honest)",
      "Where it fits in your stack",
      "Glossary for non-specialists"]),

    ("h1", "Executive summary", "1"),
    ("p", "A large share of digital advertising is paid for, but never seen by a real interested human. "
          "Bots click ads, click farms manufacture installs, fraudsters hijack the credit for conversions "
          "that would have happened anyway, and \"made-for-advertising\" inventory soaks up budget. "
          "Industry estimates put the global loss in the **tens of billions of dollars a year**."),
    ("p", "The **Jneopallium Ad-Fraud Guardian** scores every advertising event — a bid, an impression, a "
          "click, an install, a postback — and answers three questions a single \"fraud score\" cannot: "
          "**which** kind of invalid traffic this is, **why** (the evidence), and **what to do** (a "
          "proportionate, candidate action). It is **multi-label**: one event can be flagged as several "
          "things at once, each with its own calibrated probability."),
    ("callout", "The one sentence to remember",
     "It does not output a vague \"risk %.\" It says \"this looks like click injection and attribution "
     "hijack, here is the evidence, recommend HOLD_PAYOUT for review\" — and it never executes that action "
     "on its own.", "info"),
    ("p", "Crucially, it runs **advisory-first** (SHADOW then ADVISORY): it recommends, it never "
          "auto-blocks a publisher, withholds a payout, or accuses a person. It is private by design — "
          "identifiers are hashed before training, and raw IPs, precise location, and user-agent strings "
          "are never exported. That makes it safe to run next to your existing measurement and billing."),

    ("h1", "The problem: where the ad budget leaks", "2"),
    ("h2", "A single \"fraud / not-fraud\" verdict is not enough"),
    ("p", "Invalid traffic is not one thing. It is a family of very different behaviours, each needing a "
          "different response and a different owner:"),
    ("bullet", "**Bots** — automated clicks and views with no human behind them."),
    ("bullet", "**Click farms & incentivized installs** — real but motivated humans paid to click or "
               "install, who never become real customers."),
    ("bullet", "**Attribution fraud** — click spam and click injection that steal the *credit* for a "
               "conversion that would have happened anyway, so the wrong partner gets paid."),
    ("bullet", "**Event spoofing** — forged or replayed impressions, clicks, and postbacks."),
    ("bullet", "**Inventory / supply-path spoofing** — a publisher pretending to be premium inventory it "
               "is not (ads.txt / sellers.json mismatches)."),
    ("p", "A tool that returns one number — \"73% fraud\" — cannot tell an analyst whether to withhold a "
          "payout (attribution fraud), discount the traffic (low value), rate-limit a source (bots), or do "
          "nothing (a genuine accidental click). The expensive mistakes come from treating all of these as "
          "the same thing."),
    ("h2", "Why this is hard, honestly"),
    ("p", "The evidence for these families lives on **different timescales**: a forged signature is "
          "instant, a click-injection timing anomaly is per-session, a click-farm pattern emerges over "
          "minutes, and the truth about *quality* — did the install ever become a retained, paying user? — "
          "only arrives days later as retention, refund, and chargeback data. No single fast threshold can "
          "see across all of that."),

    ("h1", "The big idea: name the fraud, show the evidence", "3"),
    ("p", "The Guardian borrows its shape from the brain: many specialised \"neurons\" running at "
          "different clock speeds, each producing one kind of evidence, feeding a final correlator that "
          "fuses everything into named, calibrated verdicts. Three design choices make it commercially "
          "credible:"),
    ("bullet", "**Multi-label, not one score.** Ten independent fraud labels, each with its own calibrated "
               "probability, so the right action attaches to the right problem."),
    ("bullet", "**Evidence-first.** Every verdict carries the contributing signals and a human-readable "
               "reason, so an analyst — or an auditor, or a publisher appeal — can see *why*."),
    ("bullet", "**Advisory by construction.** It produces *candidate* actions only. Withholding money or "
               "blocking a partner requires first-party validation, legal review, and an appeal path — "
               "never the model alone."),
    ("callout", "Why advisory-first is the selling point, not a limitation",
     "Wrongly withholding a legitimate publisher's payout is a lawsuit and a broken partnership. A system "
     "that recommends and explains — and lets a human pull the trigger — is the only kind a network can "
     "actually deploy at scale.", "success"),

    ("h1", "The platform underneath: the Jneopallium framework", "4"),
    ("p", "The Guardian is one application of **Jneopallium**, a Java framework for biologically-grounded "
          "neuron networks (published in the *International Journal of Science and Research*, 2024). Three "
          "of its ideas explain why it suits this job."),
    ("h2", "1. Typed signals — meaning, not just numbers"),
    ("p", "A bid, a click, a postback, and a refund are *different kinds of signal* with different meaning "
          "and urgency. The engine routes each to the right specialist and preserves the evidence all the "
          "way to the verdict — which is what lets every advisory carry a reason."),
    ("h2", "2. Many clocks — instant checks, slow truth"),
    ("p", "Jneopallium processes fast signals every tick and slower context every N ticks. Integrity "
          "checks fire instantly; behavioural aggregation is slower; the delayed truth about traffic "
          "quality (retention, refunds, chargebacks) arrives slowest of all. One neuron can weigh an "
          "instant signature failure against a multi-day quality outcome in a single coherent judgement."),
    ("h2", "3. Single-purpose, swappable neurons"),
    ("p", "Each neuron does one job and can be upgraded or replaced for a particular deployment without "
          "touching the rest. Capacity is added by adding **layers and neurons**, not by stuffing logic "
          "into one place — which is exactly how this model was recently extended (see Section 10)."),

    ("h1", "Architecture overview: the eight-layer network", "5"),
    ("p", "Events flow one way through increasingly intelligent layers; an explainable advisory flows out "
          "the other end. The deployable network is **8 layers of 22 neurons over 21 features**:"),
    ("table", ["Layer", "Size", "Job, in plain language"],
     [["0 — Input", "—", "Receive multi-source ad events (bids, impressions, clicks, postbacks…)"],
      ["1 — Event integrity", "4", "Signature/replay/clock integrity, human-interaction, session order, attribution"],
      ["2 — Entity / graph / quality", "3", "Publisher baselines, click-farm graph fanout, delayed traffic quality"],
      ["3 — Behavioural evidence", "4", "Family-specific evidence that separates the look-alike fraud types"],
      ["4 — Feature interaction", "8", "Non-linear combinations of the evidence (one neuron per unit)"],
      ["5 — Trained correlation", "1", "Fuses everything into ten calibrated label probabilities"],
      ["6 — Response gate", "1", "Maps probabilities to a proportionate candidate action (advisory)"],
      ["7 — Result", "1", "Emit the auditable decision: labels, evidence, reason, action"]],
     [2.1, 0.7, 4.0]),

    ("h1", "What it sees: the canonical ad event", "6"),
    ("p", "Everything is normalised into one versioned **canonical event** that keeps both event time and "
          "ingestion time, and supports fifteen event types across the full funnel:"),
    ("code",
     "BID_REQUEST · IMPRESSION · VIEWABLE_IMPRESSION · CLICK · LANDING\n"
     "INTERACTION · INSTALL · REGISTRATION · PURCHASE · POSTBACK\n"
     "REFUND · CHARGEBACK · UNINSTALL · RETENTION · PAYOUT"),
    ("p", "Each event carries integrity fields (signatures, nonces, attestation), behavioural evidence "
          "(dwell, pointer dynamics, session counts), supply-chain context (ads.txt / sellers.json), and "
          "**delayed quality outcomes** (retention, refund, chargeback). Identifiers are pseudonymous: raw "
          "personal identifiers are HMAC-hashed before the model ever sees them, and a missing value is "
          "kept distinct from a zero-risk value."),

    ("h1", "The ten fraud families it scores", "7"),
    ("table", ["Label", "Plain-language meaning"],
     [["bot", "Automated, non-human clicks or views"],
      ["incentivized", "Real but motivated users paid to act; low downstream value"],
      ["clickFarm", "Coordinated human/device farms manufacturing engagement"],
      ["eventSpoofing", "Forged or replayed impressions / clicks / postbacks"],
      ["clickSpam", "Mass low-quality clicks claiming broad attribution credit"],
      ["clickInjection", "A click fired at install time to steal attribution"],
      ["attributionHijack", "Taking credit for a conversion that would have happened anyway"],
      ["inventorySpoofing", "Publisher / supply-path misrepresentation (ads.txt / sellers.json)"],
      ["accidentalOrLowValue", "Genuine but accidental or low-value traffic — not malicious"],
      ["unknownSuspicious", "Anomalous traffic the model cannot yet name"]],
     [2.0, 4.8]),
    ("p", "Note `accidentalOrLowValue` and `unknownSuspicious`: the model is built to say *\"this is odd "
          "but not necessarily fraud\"* rather than over-accuse. That distinction is what keeps it usable "
          "without burning publisher relationships."),

    ("h1", "Signals on many clocks", "8"),
    ("p", "Each signal family declares how often it is processed — the \"many clocks\" idea made concrete. "
          "Fast deterministic evidence runs every tick; the truth about quality arrives slowest:"),
    ("table", ["Signal family", "Cadence (loop / epoch)", "What it captures"],
     [["Event integrity, bid, impression, click, postback", "1 / 1", "Fast deterministic evidence"],
      ["User-interaction aggregation", "1 / 2", "Behavioural aggregation"],
      ["Session sequence & attribution", "1 / 5", "Causal-chain checks"],
      ["Entity baseline", "2 / 1", "Adaptive per-publisher baselines"],
      ["Graph cluster update", "2 / 3", "Rolling click-farm fanout graph"],
      ["Traffic quality", "2 / 5", "Delayed quality evidence"],
      ["Retention / refund / billing", "2 / 10", "Slow delayed truth"],
      ["Model drift", "2 / 60", "Monitoring cadence"]],
     [2.9, 1.6, 2.3]),
    ("p", "The physical duration of a tick is deployment configuration; these are the *semantic* "
          "scheduling cadences that let one model react instantly yet still incorporate evidence that only "
          "becomes available days later."),

    ("h1", "From raw event to verdict: the evidence pipeline", "9"),
    ("p", "Walk one event through the network:"),
    ("num", "**Integrity receptors (Layer 1)** check the signature, nonce, client/server agreement, and "
            "session ordering, and produce fast risk evidence."),
    ("num", "**Entity, graph and quality (Layer 2)** compare the event to per-publisher baselines, update "
            "a rolling device/account fanout graph, and fold in delayed retention/refund evidence."),
    ("num", "**Behavioural-evidence neurons (Layer 3)** each emit one family-specific signal — click "
            "volume (spam), conversion timing (injection), incentive pattern (incentivized), low-value "
            "quality — which is what separates fraud types that otherwise look identical."),
    ("num", "**Feature-interaction neurons (Layer 4)** form non-linear combinations of the evidence, so "
            "the final stage can express rules like \"high attribution *and* low quality *and* high click "
            "rate.\""),
    ("num", "**The correlation neuron (Layer 5)** fuses all of it into ten **calibrated** probabilities — "
            "one per fraud label."),
    ("num", "**The response gate (Layer 6)** maps those probabilities to a proportionate **candidate** "
            "action, and the **result layer (Layer 7)** emits the auditable decision with its evidence and "
            "reason."),
    ("callout", "Response bands → candidate actions",
     "log (0.0–0.30) → ALLOW/LOG · review (0.30–0.60) → REVIEW · rate-limit candidate (0.60–0.75) · "
     "hold-payout candidate (0.75–0.90) · escalate-to-analyst (0.90–1.0). Every action is a *candidate*; "
     "none is executed automatically.", "info"),

    ("h1", "The trained model and the deployable network", "10"),
    ("p", "Training does not stop at a set of weights — it emits the **actual deployable JNeopallium "
          "network**: the eight layer files plus a descriptor, with the trained, calibrated heads embedded "
          "and referencing concrete runtime classes. The current network reflects a recent, deliberate "
          "upgrade that lifted quality sharply (see the Test Report):"),
    ("table", ["Component", "What it adds"],
     [["21 features", "8 base risks + 5 behavioural-evidence features + 8 non-linear interaction units"],
      ["Behavioural-evidence layer", "Single-purpose neurons that de-conflate look-alike fraud families"],
      ["Feature-interaction layer", "A non-linear hidden layer (added as its own layer, one neuron per unit)"],
      ["Fitted, calibrated heads", "Class-weighted logistic heads with real per-label Platt calibration"],
      ["Cost-aware thresholds", "Operating points chosen by expected utility (false positives cost money)"]],
     [2.4, 4.4]),
    ("p", "Because the trained heads, calibration, and thresholds all travel inside the network files, "
          "**what was trained is exactly what runs** — there is no translation step between the model and "
          "the deployed scorer."),

    ("h1", "Safety and privacy by design", "11"),
    ("num", "**Advisory / shadow by default.** The response gate produces candidate actions only; "
            "automatic billing rejection, payout withholding, account blocking, or accusations are "
            "structurally disabled until a separate validation, legal-review, and appeal process exists."),
    ("num", "**No accusation of a person.** The model emits invalid-traffic *evidence*; it never asserts "
            "that a named individual intentionally committed fraud."),
    ("num", "**Privacy-preserving.** Device and account identifiers are pseudonymised (HMAC) before "
            "training; raw IPs, precise geolocation, and user-agent strings are never exported; a missing "
            "value is kept distinct from a zero-risk value."),
    ("num", "**Leakage-safe evaluation.** Data is split by scenario, campaign episode, and deterministic "
            "replicate block — never by random row — with an adversarial holdout, so reported quality is "
            "not inflated by near-duplicates."),

    ("h1", "How good is it? (and how honest)", "12"),
    ("p", "On the bundled reference corpus the model reaches **macro-F1 ≈ 0.97** across the ten labels, "
          "with most families at or near perfect separation and calibrated probabilities. A recent upgrade "
          "raised macro-F1 from **0.66 to 0.97** by adding the behavioural-evidence and interaction layers "
          "and fixing calibration and thresholds."),
    ("callout", "Read this honestly",
     "That score is on a deterministic simulator plus weak public-source metadata, evaluated leakage-safe. "
     "It proves the pipeline and the label separation — it is NOT a claim of real-world accuracy, which "
     "must be earned on first-party labelled production traffic. The model stays SHADOW/ADVISORY until "
     "then. The Test Report states this plainly.", "warning"),

    ("h1", "Where it fits in your stack", "13"),
    ("p", "The Guardian sits *beside* your existing pipeline as an advisory scorer, not in the critical "
          "serving path:"),
    ("code",
     "ad server / SSP / DSP / MMP  -> canonical ad events\n"
     "   -> Ad-Fraud Guardian (scores every event, advisory)\n"
     "         -> decisions: labels + evidence + candidate action\n"
     "   -> your SIEM / data warehouse / billing review / analyst queue"),
    ("p", "It deploys local, as an HTTP scoring service, or distributed; it reads a Kafka-style event "
          "stream or a direct feed; and it runs in three deployment shapes (single process, HTTP cluster, "
          "gRPC) so it fits a pilot on a laptop and an enterprise event firehose alike. Because correlation "
          "is by event time, delayed and out-of-order events still land in the right place."),

    ("h1", "Glossary for non-specialists", "14"),
    ("table", ["Term", "Plain-language meaning"],
     [["Invalid traffic (IVT)", "Ad events with no genuine interested human behind them"],
      ["Multi-label", "One event can carry several independent fraud labels at once"],
      ["Attribution", "Deciding which click/partner gets credit (and payment) for a conversion"],
      ["Click injection", "Firing a fake click at install time to steal that credit"],
      ["Postback", "A server-to-server message confirming a conversion happened"],
      ["ads.txt / sellers.json", "Public files declaring who is authorised to sell an ad slot"],
      ["Calibration", "Making a 0.9 score really mean ~90% likely"],
      ["Advisory / shadow", "The system recommends; humans decide. It never blocks or withholds on its own"],
      ["Pseudonymous / HMAC", "Identifiers are hashed so the model never sees raw personal data"],
      ["Macro-F1", "The detection quality averaged equally across all ten fraud labels"]],
     [2.1, 4.7]),
    ("spacer", 8),
    ("pi", "Jneopallium Ad-Fraud Guardian · advertising-fraud module. Architecture article for technical "
           "and non-technical readers. Safety mode: ADVISORY / SHADOW. License: BSD 3-Clause."),
]


_UK = [
    ("cover", "Jneopallium · Ad-Fraud Guardian",
     "Припиніть платити за трафік, якого не було",
     "Багатоміткове виявлення невалідного трафіку — повна архітектура, пояснена для всіх",
     [("Документ", "Архітектурний і технічний огляд"),
      ("Продукт", "Jneopallium Ad-Fraud Guardian (модуль advertising-fraud)"),
      ("Модель", "advertising-fraud 1.0.0 · macro-F1 ≈ 0.97"),
      ("Режим безпеки", "ADVISORY / SHADOW (лише рекомендації)"),
      ("Автор", "Дмитро Раковський — Харків, Україна"),
      ("Дата", DATE_UK),
      ("Аудиторія", "Маркетологи, ad-ops, антифрод-команди, інженери, новачки"),
      ("Ліцензія", "BSD 3-Clause")],
     "Архітектурна стаття",
     "Як рушій, натхненний роботою мозку, читає кожну рекламну подію на багатьох часових масштабах, "
     "називає точний вид невалідного трафіку, показує свої докази й рекомендує дію — жодного разу тихо не "
     "блокуючи паблішера й не утримуючи виплату. Написано так, щоб кожен крок був зрозумілий нефахівцю."),

    ("toc", "Зміст",
     ["Стислий огляд — версія на одну сторінку",
      "Проблема: де витікає рекламний бюджет",
      "Головна ідея: назви фрод, покажи докази",
      "Платформа в основі: фреймворк Jneopallium",
      "Огляд архітектури: восьмирівнева мережа",
      "Що він бачить: канонічна рекламна подія",
      "Десять родин фроду, які він оцінює",
      "Сигнали на багатьох годинниках",
      "Від сирої події до вердикту: конвеєр доказів",
      "Навчена модель і готова до розгортання мережа",
      "Безпека й приватність за задумом",
      "Наскільки він хороший? (і наскільки чесний)",
      "Де він вписується у ваш стек",
      "Словник для нефахівців"]),

    ("h1", "Стислий огляд", "1"),
    ("p", "Велика частка цифрової реклами оплачується, але її ніколи не бачить справжня зацікавлена людина. "
          "Боти клікають по рекламі, клік-ферми виробляють інсталяції, шахраї привласнюють заслугу за "
          "конверсії, які сталися б і так, а «зроблений для реклами» інвентар поглинає бюджет. Галузеві "
          "оцінки визначають світові втрати в **десятки мільярдів доларів на рік**."),
    ("p", "**Jneopallium Ad-Fraud Guardian** оцінює кожну рекламну подію — ставку, покази, клік, "
          "інсталяцію, постбек — і відповідає на три питання, на які не може відповісти єдиний «бал "
          "фроду»: **який** це вид невалідного трафіку, **чому** (докази) і **що робити** (пропорційна, "
          "кандидатна дія). Він **багатомітковий**: одну подію можна позначити кількома мітками одразу, "
          "кожна зі своєю каліброваною ймовірністю."),
    ("callout", "Одне речення, яке варто запам'ятати",
     "Він не видає розпливчастий «% ризику». Він каже «це схоже на click injection та attribution hijack, "
     "ось докази, рекомендую HOLD_PAYOUT на перевірку» — і ніколи не виконує цю дію сам.", "info"),
    ("p", "Найважливіше: він працює **рекомендаційно** (SHADOW, потім ADVISORY): радить, але ніколи "
          "автоматично не блокує паблішера, не утримує виплату й не звинувачує людину. Він приватний за "
          "задумом — ідентифікатори хешуються перед навчанням, а сирі IP, точна геолокація й рядки "
          "user-agent ніколи не експортуються. Це робить його безпечним поруч із наявними вимірюванням і "
          "білінгом."),

    ("h1", "Проблема: де витікає рекламний бюджет", "2"),
    ("h2", "Єдиного вердикту «фрод / не фрод» недостатньо"),
    ("p", "Невалідний трафік — не одне явище. Це родина дуже різних поведінок, кожна з яких потребує іншої "
          "відповіді та іншого власника:"),
    ("bullet", "**Боти** — автоматизовані кліки й перегляди без людини."),
    ("bullet", "**Клік-ферми та мотивовані інсталяції** — справжні, але мотивовані люди, яким платять за "
               "клік чи інсталяцію і які ніколи не стають реальними клієнтами."),
    ("bullet", "**Фрод атрибуції** — click spam і click injection, що крадуть *заслугу* за конверсію, яка "
               "сталася б і так, тож платять не тому партнеру."),
    ("bullet", "**Підробка подій** — підроблені чи повторені покази, кліки й постбеки."),
    ("bullet", "**Підробка інвентаря / шляху постачання** — паблішер видає себе за преміум-інвентар, яким "
               "не є (невідповідності ads.txt / sellers.json)."),
    ("p", "Інструмент, що повертає одне число — «73% фроду» — не скаже аналітику, чи утримати виплату "
          "(фрод атрибуції), знизити цінність трафіку (низька якість), обмежити джерело (боти) чи нічого не "
          "робити (справжній випадковий клік). Дорогі помилки виникають, коли все це трактують однаково."),
    ("h2", "Чому це складно, чесно"),
    ("p", "Докази цих родин живуть на **різних часових масштабах**: підроблений підпис — миттєвий, "
          "часова аномалія click injection — посесійна, шаблон клік-ферми проявляється за хвилини, а "
          "правда про *якість* — чи стала інсталяція утриманим платним користувачем? — приходить лише за "
          "дні як дані про утримання, повернення та чарджбеки. Жоден швидкий поріг не бачить усього цього."),

    ("h1", "Головна ідея: назви фрод, покажи докази", "3"),
    ("p", "Guardian запозичує форму в мозку: багато спеціалізованих «нейронів», що працюють на різних "
          "швидкостях годинника, кожен виробляє один вид доказів, живлячи фінальний корелятор, який "
          "поєднує все в названі, каліброві вердикти. Три рішення роблять його комерційно переконливим:"),
    ("bullet", "**Багатоміткова, не один бал.** Десять незалежних міток фроду, кожна зі своєю каліброваною "
               "ймовірністю, тож правильна дія прив'язується до правильної проблеми."),
    ("bullet", "**Спершу докази.** Кожен вердикт несе сигнали, що долучилися, і причину, зрозумілу людині, "
               "тож аналітик — або аудитор, або апеляція паблішера — бачить *чому*."),
    ("bullet", "**Рекомендаційний за побудовою.** Він видає лише *кандидатні* дії. Утримання грошей чи "
               "блокування партнера потребує валідації на власних даних, юридичного перегляду й шляху "
               "апеляції — ніколи лише моделі."),
    ("callout", "Чому рекомендаційність — це перевага, а не обмеження",
     "Помилково утримати виплату законному паблішеру — це позов і зруйноване партнерство. Система, що "
     "рекомендує й пояснює, а спускати курок дає людині, — єдина, яку мережа може справді розгорнути в "
     "масштабі.", "success"),

    ("h1", "Платформа в основі: фреймворк Jneopallium", "4"),
    ("p", "Guardian — це один застосунок **Jneopallium**, Java-фреймворку для біологічно обґрунтованих "
          "нейронних мереж (опубліковано в *International Journal of Science and Research*, 2024). Три його "
          "ідеї пояснюють, чому він пасує до цієї задачі."),
    ("h2", "1. Типізовані сигнали — зміст, а не лише числа"),
    ("p", "Ставка, клік, постбек і повернення — це *різні види сигналів* із різним змістом і терміновістю. "
          "Рушій спрямовує кожен до потрібного фахівця й зберігає докази аж до вердикту — саме це дає "
          "кожній рекомендації нести причину."),
    ("h2", "2. Багато годинників — миттєві перевірки, повільна правда"),
    ("p", "Jneopallium обробляє швидкі сигнали щотакту, а повільніший контекст — кожні N тактів. Перевірки "
          "цілісності спрацьовують миттєво; поведінкова агрегація повільніша; відкладена правда про якість "
          "трафіку (утримання, повернення, чарджбеки) приходить найповільніше. Один нейрон може зважити "
          "миттєву невдачу підпису проти багатоденного результату якості в одному цілісному судженні."),
    ("h2", "3. Однопризначені нейрони, що замінюються"),
    ("p", "Кожен нейрон виконує одну роботу й може бути оновлений чи замінений для конкретного розгортання, "
          "не торкаючись решти. Потужність додається додаванням **рівнів і нейронів**, а не вкладанням "
          "логіки в одне місце — саме так цю модель нещодавно розширили (див. розділ 10)."),

    ("h1", "Огляд архітектури: восьмирівнева мережа", "5"),
    ("p", "Події течуть в один бік через дедалі розумніші рівні; з іншого боку виходить пояснювана "
          "рекомендація. Готова до розгортання мережа — це **8 рівнів із 22 нейронів над 21 ознакою**:"),
    ("table", ["Рівень", "Розмір", "Завдання, простими словами"],
     [["0 — Вхід", "—", "Прийом багатоджерельних рекламних подій (ставки, покази, кліки, постбеки…)"],
      ["1 — Цілісність подій", "4", "Підпис/повтор/час, людська взаємодія, порядок сесії, атрибуція"],
      ["2 — Сутність / граф / якість", "3", "Базові лінії паблішерів, граф фанауту клік-ферм, відкладена якість"],
      ["3 — Поведінкові докази", "4", "Специфічні для родини докази, що розділяють схожі види фроду"],
      ["4 — Взаємодія ознак", "8", "Нелінійні комбінації доказів (один нейрон на одиницю)"],
      ["5 — Навчена кореляція", "1", "Поєднує все в десять каліброваних ймовірностей міток"],
      ["6 — Запобіжник відповіді", "1", "Відображає ймовірності на пропорційну кандидатну дію (рекомендація)"],
      ["7 — Результат", "1", "Видає аудитоване рішення: мітки, докази, причина, дія"]],
     [2.2, 0.7, 3.9]),

    ("h1", "Що він бачить: канонічна рекламна подія", "6"),
    ("p", "Усе нормалізується в одну версіоновану **канонічну подію**, що зберігає і час події, і час "
          "прийому, та підтримує п'ятнадцять типів подій по всій воронці:"),
    ("code",
     "BID_REQUEST · IMPRESSION · VIEWABLE_IMPRESSION · CLICK · LANDING\n"
     "INTERACTION · INSTALL · REGISTRATION · PURCHASE · POSTBACK\n"
     "REFUND · CHARGEBACK · UNINSTALL · RETENTION · PAYOUT"),
    ("p", "Кожна подія несе поля цілісності (підписи, нонси, атестація), поведінкові докази (час "
          "перебування, динаміка вказівника, лічильники сесії), контекст постачання (ads.txt / "
          "sellers.json) та **відкладені результати якості** (утримання, повернення, чарджбек). "
          "Ідентифікатори псевдонімні: сирі персональні ідентифікатори хешуються HMAC, перш ніж модель їх "
          "побачить, а відсутнє значення тримається окремо від значення нульового ризику."),

    ("h1", "Десять родин фроду, які він оцінює", "7"),
    ("table", ["Мітка", "Значення простою мовою"],
     [["bot", "Автоматизовані, нелюдські кліки чи перегляди"],
      ["incentivized", "Справжні, але мотивовані користувачі; низька подальша цінність"],
      ["clickFarm", "Скоординовані ферми людей/пристроїв, що виробляють залученість"],
      ["eventSpoofing", "Підроблені чи повторені покази / кліки / постбеки"],
      ["clickSpam", "Масові низькоякісні кліки, що претендують на широку атрибуцію"],
      ["clickInjection", "Клік, запущений у момент інсталяції, щоб украсти атрибуцію"],
      ["attributionHijack", "Привласнення заслуги за конверсію, що сталася б і так"],
      ["inventorySpoofing", "Спотворення паблішера / шляху постачання (ads.txt / sellers.json)"],
      ["accidentalOrLowValue", "Справжній, але випадковий чи низькоцінний трафік — не зловмисний"],
      ["unknownSuspicious", "Аномальний трафік, який модель ще не може назвати"]],
     [2.0, 4.8]),
    ("p", "Зверніть увагу на `accidentalOrLowValue` та `unknownSuspicious`: модель створена казати *«це "
          "дивно, але не обов'язково фрод»*, а не перезвинувачувати. Саме ця відмінність тримає її "
          "придатною без руйнування стосунків із паблішерами."),

    ("h1", "Сигнали на багатьох годинниках", "8"),
    ("p", "Кожна родина сигналів оголошує, як часто її обробляють — ідея «багатьох годинників» втілена "
          "конкретно. Швидкі детерміновані докази йдуть щотакту; правда про якість приходить найповільніше:"),
    ("table", ["Родина сигналів", "Каданс (loop / epoch)", "Що захоплює"],
     [["Цілісність, ставка, покази, клік, постбек", "1 / 1", "Швидкі детерміновані докази"],
      ["Агрегація взаємодії користувача", "1 / 2", "Поведінкова агрегація"],
      ["Послідовність сесії й атрибуція", "1 / 5", "Перевірки причинного ланцюга"],
      ["Базова лінія сутності", "2 / 1", "Адаптивні базові лінії паблішерів"],
      ["Оновлення графа кластерів", "2 / 3", "Рухомий граф фанауту клік-ферм"],
      ["Якість трафіку", "2 / 5", "Відкладені докази якості"],
      ["Утримання / повернення / білінг", "2 / 10", "Повільна відкладена правда"],
      ["Дрейф моделі", "2 / 60", "Каданс моніторингу"]],
     [2.9, 1.6, 2.3]),
    ("p", "Фізична тривалість такту — це конфігурація розгортання; це *семантичні* каданси планувальника, "
          "що дають одній моделі реагувати миттєво й водночас враховувати докази, доступні лише за дні."),

    ("h1", "Від сирої події до вердикту: конвеєр доказів", "9"),
    ("p", "Проведемо одну подію крізь мережу:"),
    ("num", "**Рецептори цілісності (Рівень 1)** перевіряють підпис, нонс, узгодженість клієнт/сервер і "
            "порядок сесії та виробляють швидкі докази ризику."),
    ("num", "**Сутність, граф і якість (Рівень 2)** порівнюють подію з базовими лініями паблішерів, "
            "оновлюють рухомий граф фанауту пристроїв/акаунтів і додають відкладені докази "
            "утримання/повернення."),
    ("num", "**Поведінкові нейрони (Рівень 3)** кожен видає один специфічний для родини сигнал — обсяг "
            "кліків (спам), час конверсії (injection), шаблон мотивації (incentivized), низькоцінну якість "
            "— що й розділяє види фроду, які інакше виглядають однаково."),
    ("num", "**Нейрони взаємодії (Рівень 4)** утворюють нелінійні комбінації доказів, тож фінальний етап "
            "може виразити правила на кшталт «висока атрибуція *і* низька якість *і* високий темп кліків»."),
    ("num", "**Нейрон кореляції (Рівень 5)** поєднує все в десять **каліброваних** ймовірностей — по одній "
            "на мітку фроду."),
    ("num", "**Запобіжник відповіді (Рівень 6)** відображає ці ймовірності на пропорційну **кандидатну** "
            "дію, а **рівень результату (Рівень 7)** видає аудитоване рішення з доказами й причиною."),
    ("callout", "Смуги відповіді → кандидатні дії",
     "log (0.0–0.30) → ALLOW/LOG · review (0.30–0.60) → REVIEW · кандидат на rate-limit (0.60–0.75) · "
     "кандидат на hold-payout (0.75–0.90) · ескалація аналітику (0.90–1.0). Кожна дія — *кандидат*; жодна "
     "не виконується автоматично.", "info"),

    ("h1", "Навчена модель і готова до розгортання мережа", "10"),
    ("p", "Навчання не зупиняється на наборі ваг — воно видає **власне готову до розгортання мережу "
          "JNeopallium**: вісім файлів рівнів плюс опис, із вбудованими навченими каліброваними головами, "
          "що посилаються на конкретні класи часу виконання. Поточна мережа відображає нещодавнє навмисне "
          "оновлення, що різко підняло якість (див. Звіт про тестування):"),
    ("table", ["Складова", "Що додає"],
     [["21 ознака", "8 базових ризиків + 5 поведінкових + 8 нелінійних одиниць взаємодії"],
      ["Рівень поведінкових доказів", "Однопризначені нейрони, що розділяють схожі родини фроду"],
      ["Рівень взаємодії ознак", "Нелінійний прихований рівень (доданий як окремий рівень, нейрон на одиницю)"],
      ["Припасовані каліброві голови", "Класово-зважені логістичні голови зі справжнім Platt-калібруванням"],
      ["Пороги з урахуванням вартості", "Робочі точки за очікуваною користю (хибні спрацювання коштують грошей)"]],
     [2.4, 4.4]),
    ("p", "Оскільки навчені голови, калібрування й пороги — усе всередині файлів мережі, **те, що навчили, "
          "— це саме те, що працює**; між моделлю й розгорнутим скорером немає кроку перекладу."),

    ("h1", "Безпека й приватність за задумом", "11"),
    ("num", "**Рекомендаційно / тінь за замовчуванням.** Запобіжник відповіді видає лише кандидатні дії; "
            "автоматичне відхилення білінгу, утримання виплат, блокування акаунтів чи звинувачення "
            "структурно вимкнені, доки не існує окремого процесу валідації, юридичного перегляду й "
            "апеляції."),
    ("num", "**Без звинувачення людини.** Модель видає *докази* невалідного трафіку; вона ніколи не "
            "стверджує, що названа особа навмисно скоїла фрод."),
    ("num", "**Збереження приватності.** Ідентифікатори пристроїв і акаунтів псевдонімізуються (HMAC) перед "
            "навчанням; сирі IP, точна геолокація й рядки user-agent ніколи не експортуються; відсутнє "
            "значення тримається окремо від нульового ризику."),
    ("num", "**Оцінка без витоку.** Дані розбиваються за сценарієм, епізодом кампанії й детермінованим "
            "блоком реплік — ніколи за випадковим рядком — з ворожим холдаутом, тож заявлена якість не "
            "завищена майже-дублікатами."),

    ("h1", "Наскільки він хороший? (і наскільки чесний)", "12"),
    ("p", "На вбудованому еталонному корпусі модель сягає **macro-F1 ≈ 0.97** по десяти мітках, де більшість "
          "родин на рівні або близько ідеального розділення з каліброваними ймовірностями. Нещодавнє "
          "оновлення підняло macro-F1 з **0.66 до 0.97**, додавши рівні поведінкових доказів і взаємодії та "
          "виправивши калібрування й пороги."),
    ("callout", "Прочитайте це чесно",
     "Ця оцінка — на детермінованому симуляторі плюс слабких публічних метаданих, з оцінкою без витоку. "
     "Вона доводить конвеєр і розділення міток — це НЕ твердження про реальну точність, яку треба "
     "заслужити на власному розміченому промисловому трафіку. До того часу модель лишається SHADOW/ADVISORY. "
     "Звіт про тестування прямо про це говорить.", "warning"),

    ("h1", "Де він вписується у ваш стек", "13"),
    ("p", "Guardian стоїть *поруч* із вашим наявним конвеєром як рекомендаційний скорер, а не в критичному "
          "шляху показу:"),
    ("code",
     "ad server / SSP / DSP / MMP  -> канонічні рекламні події\n"
     "   -> Ad-Fraud Guardian (оцінює кожну подію, рекомендаційно)\n"
     "         -> рішення: мітки + докази + кандидатна дія\n"
     "   -> ваш SIEM / сховище даних / перегляд білінгу / черга аналітика"),
    ("p", "Він розгортається локально, як HTTP-служба скорингу або розподілено; читає потік подій у стилі "
          "Kafka чи прямий канал; працює у трьох формах (один процес, HTTP-кластер, gRPC), тож пасує і "
          "пілоту на ноутбуці, і корпоративному потоку подій. Оскільки кореляція ведеться за часом події, "
          "запізнілі й невпорядковані події все одно потрапляють у правильне місце."),

    ("h1", "Словник для нефахівців", "14"),
    ("table", ["Термін", "Значення простою мовою"],
     [["Невалідний трафік (IVT)", "Рекламні події без справжньої зацікавленої людини"],
      ["Багатоміткова", "Одна подія може нести кілька незалежних міток фроду одразу"],
      ["Атрибуція", "Визначення, який клік/партнер отримує заслугу (і оплату) за конверсію"],
      ["Click injection", "Запуск фейкового кліку в момент інсталяції, щоб украсти заслугу"],
      ["Постбек", "Серверне повідомлення, що підтверджує конверсію"],
      ["ads.txt / sellers.json", "Публічні файли, що декларують, хто має право продавати рекламне місце"],
      ["Калібрування", "Зробити так, щоб бал 0.9 справді означав ~90% імовірності"],
      ["Рекомендаційно / тінь", "Система радить; рішення приймають люди. Сама не блокує й не утримує"],
      ["Псевдонімно / HMAC", "Ідентифікатори хешуються, тож модель ніколи не бачить сирих персональних даних"],
      ["Macro-F1", "Якість виявлення, усереднена порівну по всіх десяти мітках фроду"]],
     [2.1, 4.7]),
    ("spacer", 8),
    ("pi", "Jneopallium Ad-Fraud Guardian · модуль advertising-fraud. Архітектурна стаття для технічних і "
           "нетехнічних читачів. Режим безпеки: ADVISORY / SHADOW. Ліцензія: BSD 3-Clause."),
]
