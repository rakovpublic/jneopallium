# -*- coding: utf-8 -*-
"""Architecture article content (EN + UK), block DSL."""

from __future__ import annotations

DATE = "23 June 2026"
DATE_UK = "23 червня 2026"


def architecture(lang: str) -> list:
    return _EN if lang == "en" else _UK


# ===========================================================================
# ENGLISH
# ===========================================================================
_EN = [
    ("cover", "Jneopallium · Demo 06",
     "A Digital Immune System for Cybersecurity",
     "Temporal Threat Correlation — Complete Architecture, Explained",
     [("Document", "Architecture & Technical Overview"),
      ("Product", "Jneopallium Cybersecurity Module (Demo 06)"),
      ("Model", "cybersecurity-temporal-threat-correlator 1.0.0"),
      ("Safety mode", "ADVISORY (recommend-only)"),
      ("Author", "Dmytro Rakovskyi — Kharkiv, Ukraine"),
      ("Date", DATE),
      ("Audience", "Executives, security leads, engineers, newcomers"),
      ("License", "BSD 3-Clause")],
     "Architecture Article",
     "How a brain- and immune-system-inspired engine spots multi-step cyber attacks "
     "that traditional, single-alert tools miss — written so a non-specialist can follow every step."),

    ("toc", "What's inside",
     ["Executive summary — the one-page version",
      "The problem: why modern attacks slip through",
      "The big idea: borrowing from biology",
      "The platform underneath: the Jneopallium framework",
      "Architecture overview: how the pieces fit together",
      "The seven layers, explained in plain language",
      "Signals: the common language of the system",
      "Temporal correlation: connecting the dots over time",
      "The trained model: what it actually learned",
      "Safety by design: why it cannot break your network",
      "Data foundation: the datasets behind the model",
      "Deployment topology: where it runs",
      "End-to-end walkthrough: three streams, three verdicts",
      "Glossary for non-specialists"]),

    ("h1", "Executive summary", "1"),
    ("p", "Most security tools look at one event at a time. They ask, \"Is this single login, "
          "this single file, this single network connection dangerous?\" Real attackers do not work "
          "that way. They move in **quiet, patient steps**: a suspicious login, then a script, then a "
          "lookup, then a jump to another machine, then a slow trickle of data leaving the building. "
          "Each step on its own looks almost normal. The damage is in the **sequence**."),
    ("p", "This product — Demo 06 of the Jneopallium platform — is built to watch the sequence. It is a "
          "**temporal threat-correlation engine**: it connects events across time and across many "
          "different data sources, recognises the shape of an attack as it unfolds, and raises a single, "
          "well-evidenced advisory instead of a thousand disconnected alarms."),
    ("p", "Its design is borrowed from the human **immune system** and the human **brain**. Just as your "
          "body has fast first-responders, slower learning defences, a memory of past infections, and a "
          "hard rule never to attack its own healthy cells, this engine has fast detectors, adaptive "
          "learning, an attack memory, and inviolable safety gates that protect critical systems."),
    ("callout", "The one sentence to remember",
     "It does not just ask \"is this event bad?\" — it asks \"do these events, in this order, over this "
     "stretch of time, add up to an attack?\" — and it answers with evidence, never with a blind block.",
     "info"),
    ("p", "Crucially, the system runs in **ADVISORY mode**: it recommends, it never silently isolates a "
          "host or blocks traffic on its own. Every recommendation carries a full evidence trail. That "
          "makes it safe to deploy alongside the tools you already trust."),

    ("h1", "The problem: why modern attacks slip through", "2"),
    ("h2", "Three weaknesses of one-event-at-a-time tools"),
    ("p", "Classic intrusion-detection systems score each event in isolation. This creates three "
          "well-known failures that attackers exploit on purpose:"),
    ("bullet", "**The low-and-slow blind spot.** An attacker who steals data a little at a time — a few "
               "megabytes an hour over days — never trips a single big threshold. Each transfer looks like "
               "ordinary traffic."),
    ("bullet", "**Alert fatigue.** When every event is scored alone, a busy network produces thousands of "
               "low-confidence alerts a day. Analysts cannot read them all, so the real attack hides in "
               "the noise."),
    ("bullet", "**Lost context.** A PowerShell script during an approved maintenance window is routine. The "
               "exact same script at 3 a.m. on a finance server, right after an unusual login, is an "
               "emergency. A single-event tool cannot tell the two apart."),
    ("h2", "What \"dwell time\" costs"),
    ("p", "The industry term for how long an attacker stays undetected is **dwell time**. The longer the "
          "dwell time, the more credentials are stolen, the more machines are compromised, and the more "
          "data leaves. Every hour of earlier detection directly reduces the blast radius. The entire "
          "purpose of temporal correlation is to **collapse dwell time** by recognising the attack while "
          "it is still in progress, not after the data is gone."),

    ("h1", "The big idea: borrowing from biology", "3"),
    ("p", "Your immune system is the most successful intrusion-detection system on Earth. It has been "
          "field-tested for hundreds of millions of years. Jneopallium's cybersecurity module deliberately "
          "copies its architecture, because the problems are the same: detect threats fast, learn new ones, "
          "remember old ones, respond in proportion, and — above all — never destroy healthy tissue."),
    ("table", ["Biological defence", "What it does", "In this product"],
     [["Macrophages / neutrophils", "Fast, hard-wired first responders that recognise known danger signs",
       "**Innate signature detectors** — match known attack patterns instantly"],
      ["T-cells", "Adaptive defenders that learn what is abnormal for *your* body",
       "**Anomaly detectors** — learn each user's and host's normal behaviour"],
      ["Memory B-cells", "Remember past infections so the next response is faster",
       "**Attack memory** — recalls campaigns and techniques seen before"],
      ["Thymic negative selection", "A hard rule: never attack the body's own healthy cells",
       "**Hard safety gate** — fixed allow-list, never learned, protects critical assets"],
      ["Inflammation staging", "Response scales up gradually, never all-or-nothing",
       "**Graduated response** — log → alert → quarantine candidate → escalate"],
      ["Resolution of inflammation", "Defences stand down so tissue can heal",
       "**Auto-lift** — every quarantine has a deadline and expires automatically"]],
     [1.5, 2.6, 2.6]),
    ("p", "Two of these analogues deserve special attention because they are what make the system "
          "trustworthy:"),
    ("bullet", "**Self-tolerance (never attack yourself).** The biological immune system is dangerous when "
               "it turns on healthy cells — that is what auto-immune disease is. The product's hard safety "
               "gate is the digital equivalent of negative selection: a fixed, construction-time list of "
               "protected systems that **no runtime signal can override**. An attacker who somehow gets "
               "inside the data flow can, at most, un-block something previously trusted — they can never "
               "turn the system into a weapon against your own critical servers."),
    ("bullet", "**Resolution (defences always stand down).** Inflammation that never resolves kills the "
               "patient. So in this product, **quarantine is never permanent by construction**: every "
               "containment recommendation carries a positive time limit and an automatic lift fires when it "
               "expires, unless the threat is independently re-confirmed."),

    ("h1", "The platform underneath: the Jneopallium framework", "4"),
    ("p", "The cybersecurity module is one application of a general-purpose engine called **Jneopallium** — "
          "a Java framework for building biologically-grounded neuron networks, published in the "
          "*International Journal of Science and Research* (2024). You do not need to understand "
          "neuroscience to use the product, but three of the framework's ideas explain *why* it is good at "
          "this job."),
    ("h2", "1. Typed signals — a programmable nervous system"),
    ("p", "In an ordinary neural network, everything is a number. In Jneopallium, you define what a "
          "**signal** is. A network packet, a login event, a threat-intelligence indicator, and a "
          "maintenance notice are *different kinds of signal* with different meaning and different urgency. "
          "The engine routes each kind to the right specialist. This is exactly why the system can keep "
          "evidence — it never flattens a login and a packet into the same anonymous number."),
    ("h2", "2. Two clocks — fast reflexes and slow judgement"),
    ("p", "Biology runs on many clocks at once: nerve impulses travel in milliseconds, while hormones "
          "diffuse over seconds to minutes. Jneopallium copies this with a **fast loop** (every tick) and a "
          "**slow loop** (every N ticks). Network packets and system calls are processed on the fast loop "
          "for instant reaction; slower context like threat intelligence, asset criticality, and "
          "maintenance windows is processed on the slow loop. The result: detection stays fast, while "
          "context that should *modify* a verdict is applied at its own natural pace."),
    ("h2", "3. Stateless, swappable specialists"),
    ("p", "Each processing step is a small, stateless **processor** wired to a neuron through an interface, "
          "not a hard-coded class. In practice this means any detector — the signature engine, the anomaly "
          "model, the correlation logic — can be upgraded or replaced for a particular deployment without "
          "touching the rest of the system. A lab can run a simple matcher; a bank can plug in a "
          "high-performance commercial engine behind the same interface."),

    ("h1", "Architecture overview: how the pieces fit together", "5"),
    ("p", "At the highest level, telemetry flows in one direction through a pipeline of increasingly "
          "intelligent stages, and an auditable advisory flows out the other end:"),
    ("code",
     "multi-source telemetry\n"
     "   -> canonical event adapters      (translate everything into one language)\n"
     "   -> fast host & network receptors  (cheap, instant evidence scoring)\n"
     "   -> temporal threat correlation    (connect events over time)\n"
     "   -> response planner               (choose a proportionate recommendation)\n"
     "   -> fixed hard safety gate         (protect critical assets, enforce mode)\n"
     "   -> advisory output + audit trail  (a verdict you can explain)"),
    ("p", "The same shape appears inside the runnable demo as a small four-layer neuron network. Each layer "
          "is a team of neurons with a clear job:"),
    ("table", ["Layer", "Size", "Job, in plain language"],
     [["Layer 0 — Input", "7", "Receive seven kinds of raw security telemetry"],
      ["Layer 1 — Normalisation", "4", "Clean it up and give each event a fast evidence score"],
      ["Layer 2 — Correlation", "3", "Connect events across time into a threat hypothesis"],
      ["Layer 3 — Planning", "2", "Turn the hypothesis into an advisory investigation action"]],
     [2.3, 0.8, 3.6]),
    ("p", "The full production module is richer — it adds memory, homeostasis (self-regulation), and a "
          "dedicated response layer — described next."),

    ("h1", "The seven layers, explained in plain language", "6"),
    ("p", "The production cybersecurity module is organised like the body's defences, from the skin inward. "
          "Here is each layer and the everyday job it does."),
    ("h3", "Layer 0 — Ingestion (the senses)"),
    ("p", "Rate-limited intake of packets, system calls, and logs. Like a doorway with a turnstile, it "
          "accepts telemetry at a controlled rate so a flood of traffic — accidental or a deliberate "
          "denial-of-service attempt — cannot overwhelm everything behind it."),
    ("h3", "Layer 1 — Innate detection (the first responders)"),
    ("p", "Fast, hard-wired matchers: known-bad signatures, forbidden sequences of system calls, and "
          "per-connection traffic accounting. A **soft allow-list** here filters known-good patterns. This "
          "layer is cheap and instant — it catches the obvious."),
    ("h3", "Layer 2 — Adaptive detection (the learners)"),
    ("p", "This is where the system learns *your* normal. It keeps a moving baseline of each user's and "
          "host's behaviour and flags meaningful deviation. It includes a **beaconing detector** (spotting "
          "the metronome-like regularity of malware phoning home) and a **lateral-movement detector** "
          "(spotting one account suddenly authenticating to many machines). Critically, this layer's "
          "learning **freezes during an active attack**, so the attacker's behaviour is never quietly "
          "absorbed into the definition of \"normal.\""),
    ("h3", "Layer 3 — Memory (the immune memory)"),
    ("p", "Remembers attack campaigns and the techniques that made them up, and binds scattered pieces of "
          "evidence onto a single incident timeline. This is how a faint new signal can be recognised "
          "quickly as part of a pattern the system has seen before."),
    ("h3", "Layer 4 — Hypothesis & planning (the diagnosis)"),
    ("p", "Combines signature evidence and anomaly evidence into a single probability — a **posterior** — "
          "that an attack is underway, then maps that probability to a proportionate response band. This is "
          "the layer that turns scattered clues into a verdict with a confidence level."),
    ("h3", "Layer 5 — Response (the proportionate reaction)"),
    ("p", "Owns the **hard safety gate** (the un-overridable allow-list and critical-asset protection), the "
          "**quarantine logic** (positive-duration only, automatic lift), and optional snapshot rollback. "
          "In ADVISORY mode this layer produces *recommendations and evidence*, not enforced actions."),
    ("h3", "Layer 7 — Homeostasis (self-regulation)"),
    ("p", "Keeps the system healthy. An **alert-fatigue monitor** raises detection thresholds when false "
          "positives drift up, and an **exhaustion limiter** acts as a budget on rule evaluation so a "
          "flood cannot starve the important stages. Biology calls this homeostasis; engineering calls it "
          "graceful degradation under load."),
    ("callout", "Why the layers are numbered 0–7 with a gap",
     "The numbering mirrors the broader Jneopallium cognitive architecture, where some layers (such as "
     "affect and curiosity) are deliberately switched off for security work — a defensive system should "
     "not get \"bored\" or \"emotional.\" The gaps are intentional, not missing pieces.",
     "info"),

    ("h1", "Signals: the common language of the system", "7"),
    ("p", "Everything the system sees is converted into a **typed signal** — a small, well-described object "
          "carrying both data and meaning. This is what lets the engine keep an evidence trail from raw "
          "telemetry all the way to the final advisory. The cybersecurity demo defines ten signal types:"),
    ("table", ["Signal", "Carries", "Think of it as"],
     [["AuthenticationEventSignal", "Logins, their result and context", "Who tried to get in, and did it work"],
      ["ProcessEventSignal", "Program execution, parent/child chains", "What ran, and what launched it"],
      ["DnsLookupSignal", "Domain-name lookups", "Who the machine tried to phone"],
      ["NetworkFlowSignal", "Connection byte/packet summaries", "How much data went where"],
      ["ThreatIntelContextSignal", "Known-bad indicators with confidence", "An external tip-off"],
      ["AssetContextSignal", "How critical and owned an asset is", "How much this machine matters"],
      ["MaintenanceWindowSignal", "Approved-change context", "\"This was planned\" context"],
      ["RiskScoreSignal", "A computed risk number", "The running risk tally"],
      ["ThreatHypothesisSignal", "A correlated attack hypothesis", "The diagnosis-in-progress"],
      ["SecurityAdvisorySignal", "The final recommendation + evidence", "The verdict you act on"]],
     [2.2, 2.4, 2.2]),
    ("p", "Each signal type also declares **how often** it should be processed. Fast, urgent signals "
          "(packets, system calls) run every tick; slower context signals (threat intel, maintenance, "
          "self-tolerance updates) run on the slow loop. This is the \"two clocks\" idea made concrete: the "
          "system reacts instantly but reflects slowly, exactly where each is appropriate."),

    ("h1", "Temporal correlation: connecting the dots over time", "8"),
    ("p", "This is the heart of the product. A real intrusion is not one event — it is an ordered chain. "
          "The classic enterprise attack looks like this:"),
    ("code",
     "unusual authentication\n"
     "      ->  process execution\n"
     "            ->  DNS lookup\n"
     "                  ->  lateral movement (jump to another host)\n"
     "                        ->  command-and-control beaconing\n"
     "                              ->  data exfiltration"),
    ("p", "The engine watches several overlapping time windows at once, each tuned to a different rhythm of "
          "attacker behaviour:"),
    ("table", ["Window", "Span", "What it catches"],
     [["Fast window", "1–10 seconds", "Immediate, sharp signals"],
      ["Behaviour window", "1–5 minutes", "A burst of related actions"],
      ["Incident window", "30–120 minutes", "A full attack chain unfolding"],
      ["Baseline window", "Hours to days", "What \"normal\" looks like over time"]],
     [1.8, 1.8, 3.2]),
    ("p", "Within these windows the model rewards **ordered transitions** — a login *followed by* an "
          "execution *followed by* lateral movement scores far higher than the same events in a random "
          "order or spread randomly across unrelated machines. It also recognises the **low-and-slow** "
          "pattern: weak, repeated outbound transfers that mean nothing individually but, correlated over "
          "an incident window and combined with threat-intelligence context, reveal a quiet data leak."),
    ("callout", "Why event *time* matters more than arrival time",
     "Telemetry arrives late, out of order, and replayed. The engine correlates by the time an event "
     "*actually happened* (its event-tick), not the time it was received. That is what lets it reconstruct "
     "the true sequence of an attack even when the data is messy — a property single-event tools lack.",
     "info"),

    ("h1", "The trained model: what it actually learned", "9"),
    ("p", "Bundled with the product is a checked-in reference model, "
          "`cybersecurity-temporal-threat-correlator`. It is deliberately a **transparent, reproducible "
          "baseline**: a class-balanced logistic model over 34 temporal-window features. Transparency is a "
          "feature — every weight is inspectable, so a security team can see *why* a verdict was reached "
          "before moving on to larger sequence models (GRU/TCN/transformer) later."),
    ("p", "The model condenses each time-window into 34 numbers (features) such as: how ordered the events "
          "are, how diverse the data sources are, the strongest threat-intelligence hit, the average asset "
          "criticality, and whether a maintenance window was active. The features it leaned on most tell a "
          "coherent story:"),
    ("h3", "Top evidence the model treats as suspicious"),
    ("table", ["Feature", "Weight", "Plain meaning"],
     [["technique_command_and_control", "+0.43", "Signs of a machine phoning home to an attacker"],
      ["network_receptor_score", "+0.42", "Strong network-side evidence"],
      ["max_threat_intel", "+0.42", "A high-confidence external threat tip"],
      ["slow_context_score", "+0.39", "Threat intel plus asset criticality together"],
      ["mean_evidence", "+0.39", "Consistently strong evidence across the window"]],
     [2.8, 0.9, 3.1]),
    ("h3", "Top evidence the model treats as reassuring"),
    ("table", ["Feature", "Weight", "Plain meaning"],
     [["maintenance_ratio", "-0.45", "Activity happened during approved maintenance"],
      ["benign_context_ratio", "-0.39", "Mostly benign, expected context"],
      ["technique_unusual_login", "-0.27", "An isolated odd login, with nothing following it"],
      ["source_diversity", "-0.18", "Evidence from only one source, not a coordinated chain"]],
     [2.8, 0.9, 3.1]),
    ("h3", "From model to deployable network"),
    ("p", "The trainer does not stop at a set of weights — it emits a **complete, deployable JNeopallium "
          "network**: five layers of eight real neurons, with the learned weights, the decision threshold, "
          "the temporal sequence gates, and the safety policy all written into ready-to-load layer "
          "configuration files. What was trained is exactly what runs in production."),
    ("table", ["Generated layer", "Real neurons", "Job"],
     [["Input", "—", "Multi-source event boundary"],
      ["Fast evidence", "NetworkFlowNeuron, SignaturePatternNeuron, ProcessBehaviourNeuron, EntityBehaviourBaselineNeuron", "Instant scoring"],
      ["Correlation", "TemporalThreatCorrelationNeuron", "Trained temporal correlation"],
      ["Planning + gate", "ResponsePlanningNeuron, ResponseGateNeuron", "Advisory bands + fixed gate"],
      ["Result", "ResponsePlanningNeuron", "Advisory output"]],
     [1.4, 3.6, 1.8]),
    ("p", "The correlation layer also carries a **baseline-adaptation policy** that freezes learning when "
          "the posterior reaches 0.3 or signature confidence reaches 0.8, adapting only from trusted benign "
          "periods — the anti-poisoning rule, encoded directly into the deployable network."),
    ("callout", "Read this honestly",
     "On the bundled, deterministic reference data the model scores perfectly (precision and recall of "
     "1.0). That demonstrates the pipeline works end to end — it is NOT a claim of real-world accuracy. "
     "Real performance must be earned on external datasets. The accompanying Test Report is explicit about "
     "this distinction, and we keep it front-and-centre rather than buried.",
     "warning"),

    ("h1", "Safety by design: why it cannot break your network", "10"),
    ("p", "A detection system with the power to block traffic is also a system that can take your business "
          "offline if it is wrong. This product is engineered so that being wrong is *safe*. Four "
          "structural guarantees do the heavy lifting:"),
    ("num", "**Advisory by default.** The safety ceiling is ADVISORY. The system recommends investigation "
            "or containment candidates; it does not isolate hosts or block traffic on its own. Active "
            "enforcement requires a separate, deliberately added safety case, approval workflow, and "
            "rollback path."),
    ("num", "**Hard gates are fixed, not learned.** The allow-list of protected systems and the "
            "critical-asset list are set at wiring time and cannot be changed by any runtime signal. The "
            "model can learn what looks suspicious; it can never learn to stand down protection of a "
            "critical server."),
    ("num", "**Baseline freeze during attacks.** When the system's confidence reaches a watch/alert state, "
            "adaptive learning freezes, so an attacker cannot patiently teach the system that their "
            "behaviour is normal (a \"baseline-poisoning\" attack)."),
    ("num", "**Containment always expires.** Every quarantine recommendation has a positive duration and an "
            "automatic lift. Nothing the system recommends is permanent unless a human independently "
            "re-confirms it."),
    ("p", "On top of these, every advisory preserves a full **evidence lineage**: the entity involved, the "
          "exact time range, which sources and techniques contributed, the posterior probability, the "
          "response band, whether the baseline was frozen, and the precise model version and checksum used. "
          "If a regulator, an auditor, or an analyst asks \"why did the system say this?\", there is always "
          "a complete, replayable answer."),

    ("h1", "Data foundation: the datasets behind the model", "11"),
    ("p", "A temporal correlator is only as good as the variety of attacks it has seen. The training design "
          "deliberately spans seven complementary public and lab sources, each mapped onto the system's "
          "typed signals so the model learns from many angles rather than one narrow table:"),
    ("table", ["Dataset", "Role"],
     [["LANL Comprehensive Multi-Source Cyber-Security Events", "Realistic enterprise temporal validation with red-team labels"],
      ["ToN_IoT", "Multi-source Windows/Linux/network/IoT with ground truth"],
      ["DARPA OpTC", "Advanced endpoint and APT provenance validation"],
      ["CIC-IDS2017 / CSE-CIC-IDS2018", "Network-flow detector pre-training"],
      ["UNSW-NB15", "Fast network classifier and throughput validation"],
      ["MITRE CALDERA lab output", "Exact labels for controlled, repeatable attack chains"]],
     [3.3, 3.5]),
    ("p", "A strict **split policy** prevents the most common way machine-learning security claims are "
          "inflated: the data is never split by random individual rows (which leaks near-duplicate events "
          "of the same campaign into both training and testing). Instead it is split by time period, "
          "campaign, host group, and attack type — so the test always measures performance on genuinely "
          "unseen attacks."),

    ("h1", "Deployment topology: where it runs", "12"),
    ("p", "The same model runs in three deployment shapes, so it fits a laptop demo and a clustered "
          "enterprise alike:"),
    ("table", ["Mode", "Description", "Typical use"],
     [["Local", "Single Java process", "Demos, pilots, edge sites"],
      ["Cluster (HTTP)", "Distributed across worker nodes over HTTP", "Enterprise-scale telemetry"],
      ["Cluster (gRPC)", "Distributed over gRPC; FPGA targets supported", "High-throughput, low-latency"]],
     [1.8, 3.2, 1.8]),
    ("p", "Telemetry reaches the system through **bridges** — adapters that translate a real-world protocol "
          "into typed signals. The cybersecurity module is designed around a Kafka-style event stream, so a "
          "real Kafka bridge can replace the demo's input while keeping the exact same typed event "
          "contract. Because correlation is by event time, delayed or out-of-order telemetry still lands in "
          "the right place in the sequence."),

    ("h1", "End-to-end walkthrough: three streams, three verdicts", "13"),
    ("p", "The clearest way to understand the system is to watch it judge three simultaneous streams that a "
          "naive tool would get wrong. All three run through the same pipeline; the difference is entirely "
          "in the **correlation and context**."),
    ("h3", "Stream A — the real attack (correctly raised)"),
    ("p", "`user:backup-service@workstation-17`: an unusual login, then an encoded PowerShell command, then "
          "authentication fan-out to many hosts, then a rare DNS lookup, then periodic outbound traffic. "
          "Individually, forgivable. In this **order**, within the incident window, it is a textbook "
          "intrusion. Verdict: **TEMPORAL_THREAT_ADVISORY**, with the baseline frozen so the attack cannot "
          "poison \"normal.\""),
    ("h3", "Stream B — planned maintenance (correctly calmed)"),
    ("p", "`svc:deployment-agent@web-tier`: service-account retries and signed deployment activity during "
          "an approved maintenance window. The maintenance context **lowers the score** — but it does not "
          "erase the evidence; the system still records auditable observations. Verdict: "
          "**CONTEXT_SUPPRESSED_OBSERVATION**. This is the case single-event tools get most wrong, in both "
          "directions."),
    ("h3", "Stream C — the quiet data leak (correctly surfaced)"),
    ("p", "`host:finance-file-01`: weak, repeated outbound transfers that are individually meaningless. "
          "Correlated over the incident window and combined with threat-intelligence context, they become "
          "a credible slow exfiltration. Verdict: **LOW_AND_SLOW_CORRELATION** — precisely the case the "
          "low-and-slow blind spot was designed to exploit."),
    ("callout", "The point of the three streams",
     "The attack scores higher than the benign maintenance; the maintenance is calmed without losing its "
     "audit trail; the quiet leak is surfaced. And in every case the output is an advisory with evidence — "
     "never a silent block. That combination is what a one-event-at-a-time tool cannot deliver.",
     "success"),

    ("h1", "Glossary for non-specialists", "14"),
    ("table", ["Term", "Plain-language meaning"],
     [["Advisory mode", "The system recommends; humans decide. It never blocks on its own."],
      ["Anomaly detection", "Spotting behaviour that is unusual for a specific user or machine."],
      ["Baseline", "The learned picture of what \"normal\" looks like for an entity."],
      ["Baseline poisoning", "An attacker slowly teaching a system that their behaviour is normal."],
      ["Beaconing", "Malware contacting its controller at regular intervals, like a heartbeat."],
      ["Dwell time", "How long an attacker stays in the network before being detected."],
      ["Evidence lineage", "The full, replayable trail from raw data to a verdict."],
      ["Exfiltration", "Stealing data out of the organisation."],
      ["Lateral movement", "Jumping from one compromised machine to others."],
      ["Posterior", "The model's probability that an attack is underway, given the evidence."],
      ["Quarantine", "Temporarily containing an entity — here, always with an expiry."],
      ["Signature detection", "Matching against known, named bad patterns."],
      ["Temporal correlation", "Connecting events across time into one coherent picture."],
      ["Telemetry", "The stream of raw security data: logins, processes, DNS, network flows."]],
     [2.0, 4.8]),
    ("spacer", 8),
    ("pi", "Jneopallium Cybersecurity Module · Demo 06 · Temporal Threat Correlation. "
           "Architecture article prepared for technical and non-technical readers alike. "
           "Safety mode: ADVISORY. License: BSD 3-Clause."),
]


# ===========================================================================
# UKRAINIAN
# ===========================================================================
_UK = [
    ("cover", "Jneopallium · Демо 06",
     "Цифрова імунна система для кібербезпеки",
     "Часова кореляція загроз — повна архітектура, пояснена просто",
     [("Документ", "Архітектурний і технічний огляд"),
      ("Продукт", "Модуль кібербезпеки Jneopallium (Демо 06)"),
      ("Модель", "cybersecurity-temporal-threat-correlator 1.0.0"),
      ("Режим безпеки", "ADVISORY (лише рекомендації)"),
      ("Автор", "Дмитро Раковський — Харків, Україна"),
      ("Дата", DATE_UK),
      ("Аудиторія", "Керівники, фахівці з безпеки, інженери, новачки"),
      ("Ліцензія", "BSD 3-Clause")],
     "Архітектурна стаття",
     "Як рушій, натхненний роботою мозку та імунної системи, виявляє багатокрокові кібератаки, "
     "які пропускають традиційні інструменти з одиничними сповіщеннями — написано так, щоб кожен крок "
     "був зрозумілий нефахівцю."),

    ("toc", "Зміст",
     ["Стислий огляд — версія на одну сторінку",
      "Проблема: чому сучасні атаки проходять непоміченими",
      "Головна ідея: запозичення в біології",
      "Платформа в основі: фреймворк Jneopallium",
      "Огляд архітектури: як поєднуються складові",
      "Сім рівнів, пояснені простою мовою",
      "Сигнали: спільна мова системи",
      "Часова кореляція: поєднання подій у часі",
      "Навчена модель: чого вона насправді навчилася",
      "Безпека за задумом: чому вона не зашкодить вашій мережі",
      "Основа даних: набори даних за моделлю",
      "Топологія розгортання: де це працює",
      "Наскрізний приклад: три потоки, три вердикти",
      "Словник для нефахівців"]),

    ("h1", "Стислий огляд", "1"),
    ("p", "Більшість засобів безпеки аналізують події по одній. Вони запитують: «Чи небезпечний цей "
          "окремий вхід, цей окремий файл, це окреме мережеве з'єднання?» Справжні зловмисники діють "
          "інакше. Вони рухаються **тихими, терплячими кроками**: підозрілий вхід, потім скрипт, потім "
          "запит, потім перехід на іншу машину, потім повільний витік даних назовні. Кожен крок окремо "
          "виглядає майже нормально. Шкода — у **послідовності**."),
    ("p", "Цей продукт — Демо 06 платформи Jneopallium — створений, щоб стежити за послідовністю. Це "
          "**рушій часової кореляції загроз**: він поєднує події в часі та з багатьох різних джерел даних, "
          "розпізнає форму атаки, поки вона розгортається, і видає одну добре обґрунтовану рекомендацію "
          "замість тисячі розрізнених тривог."),
    ("p", "Його конструкція запозичена в людської **імунної системи** та **мозку**. Як ваше тіло має "
          "швидких першочергових захисників, повільніший навчальний захист, пам'ять про минулі інфекції та "
          "жорстке правило ніколи не атакувати власні здорові клітини — так і цей рушій має швидкі "
          "детектори, адаптивне навчання, пам'ять про атаки та непорушні запобіжники, що захищають "
          "критичні системи."),
    ("callout", "Одне речення, яке варто запам'ятати",
     "Він не просто запитує «чи погана ця подія?» — він запитує «чи складаються ці події, у цьому порядку, "
     "за цей проміжок часу, в атаку?» — і відповідає доказами, а не сліпим блокуванням.",
     "info"),
    ("p", "Найважливіше: система працює в режимі **ADVISORY**: вона рекомендує, але ніколи не ізолює вузол "
          "і не блокує трафік самостійно. Кожна рекомендація супроводжується повним ланцюгом доказів. Це "
          "робить її безпечною для розгортання поруч із засобами, яким ви вже довіряєте."),

    ("h1", "Проблема: чому сучасні атаки проходять непоміченими", "2"),
    ("h2", "Три слабкості інструментів «одна подія за раз»"),
    ("p", "Класичні системи виявлення вторгнень оцінюють кожну подію ізольовано. Це створює три добре "
          "відомі вади, які зловмисники використовують навмисне:"),
    ("bullet", "**Сліпа зона «повільно й тихо».** Зловмисник, який краде дані потроху — кілька мегабайтів "
               "на годину впродовж днів — ніколи не перетне жодного великого порогу. Кожне передавання "
               "виглядає як звичайний трафік."),
    ("bullet", "**Втома від сповіщень.** Коли кожну подію оцінюють окремо, завантажена мережа щодня "
               "породжує тисячі тривог низької впевненості. Аналітики не можуть прочитати їх усі, тож "
               "справжня атака ховається в шумі."),
    ("bullet", "**Втрата контексту.** Скрипт PowerShell під час погодженого вікна обслуговування — це "
               "рутина. Той самий скрипт о 3-й ночі на фінансовому сервері одразу після незвичного входу — "
               "це надзвичайна ситуація. Інструмент «одна подія» не відрізнить ці два випадки."),
    ("h2", "Скільки коштує «час перебування»"),
    ("p", "Галузевий термін для того, скільки зловмисник лишається невиявленим, — **час перебування** "
          "(dwell time). Що довший час перебування, то більше вкрадено облікових даних, то більше машин "
          "скомпрометовано, то більше даних виходить назовні. Кожна година раннішого виявлення прямо "
          "зменшує радіус ураження. Уся мета часової кореляції — **скоротити час перебування**, "
          "розпізнаючи атаку, поки вона ще триває, а не після того, як дані вже зникли."),

    ("h1", "Головна ідея: запозичення в біології", "3"),
    ("p", "Ваша імунна система — найуспішніша система виявлення вторгнень на Землі. Її випробовували в "
          "польових умовах сотні мільйонів років. Модуль кібербезпеки Jneopallium навмисно копіює її "
          "архітектуру, бо задачі однакові: швидко виявляти загрози, вчитися новим, пам'ятати старі, "
          "відповідати пропорційно і — насамперед — ніколи не руйнувати здорову тканину."),
    ("table", ["Біологічний захист", "Що він робить", "У цьому продукті"],
     [["Макрофаги / нейтрофіли", "Швидкі вроджені захисники, що впізнають відомі ознаки небезпеки",
       "**Вроджені сигнатурні детектори** — миттєво зіставляють відомі шаблони атак"],
      ["T-клітини", "Адаптивні захисники, що вчаться, що є ненормальним саме для *вашого* тіла",
       "**Детектори аномалій** — вивчають нормальну поведінку кожного користувача й вузла"],
      ["B-клітини пам'яті", "Пам'ятають минулі інфекції, тож наступна відповідь швидша",
       "**Пам'ять про атаки** — згадує раніше бачені кампанії та техніки"],
      ["Негативна селекція в тимусі", "Жорстке правило: ніколи не атакувати власні здорові клітини",
       "**Жорсткий запобіжник** — фіксований білий список, ніколи не навчається, захищає критичні активи"],
      ["Стадійність запалення", "Відповідь наростає поступово, ніколи не «все або нічого»",
       "**Градуйована відповідь** — журнал → тривога → кандидат на карантин → ескалація"],
      ["Розв'язання запалення", "Захист відступає, щоб тканина загоїлася",
       "**Автозняття** — кожен карантин має термін і завершується автоматично"]],
     [1.6, 2.5, 2.6]),
    ("p", "Два з цих аналогів заслуговують особливої уваги, бо саме вони роблять систему гідною довіри:"),
    ("bullet", "**Самотолерантність (ніколи не атакувати себе).** Біологічна імунна система небезпечна, "
               "коли обертається проти здорових клітин — це і є автоімунне захворювання. Жорсткий "
               "запобіжник продукту — цифровий відповідник негативної селекції: фіксований, заданий під час "
               "побудови перелік захищених систем, який **жоден сигнал під час роботи не може скасувати**. "
               "Зловмисник, який якось проник у потік даних, у найгіршому разі може лише розблокувати щось "
               "раніше довірене — але ніколи не зможе перетворити систему на зброю проти ваших критичних "
               "серверів."),
    ("bullet", "**Розв'язання (захист завжди відступає).** Запалення, яке не вщухає, вбиває пацієнта. Тому "
               "в цьому продукті **карантин ніколи не є постійним за побудовою**: кожна рекомендація щодо "
               "стримування має додатне обмеження в часі, і автоматичне зняття спрацьовує після його "
               "завершення, якщо загрозу не підтверджено незалежно."),

    ("h1", "Платформа в основі: фреймворк Jneopallium", "4"),
    ("p", "Модуль кібербезпеки — це один із застосунків універсального рушія **Jneopallium** — Java-"
          "фреймворку для побудови біологічно обґрунтованих нейронних мереж, опублікованого в "
          "*International Journal of Science and Research* (2024). Щоб користуватися продуктом, нейронауку "
          "знати не потрібно, але три ідеї фреймворку пояснюють, *чому* він добре справляється з цією "
          "задачею."),
    ("h2", "1. Типізовані сигнали — програмована нервова система"),
    ("p", "У звичайній нейромережі все є числом. У Jneopallium ви визначаєте, що таке **сигнал**. Мережевий "
          "пакет, подія входу, індикатор кіберрозвідки та повідомлення про обслуговування — це *різні види "
          "сигналів* із різним змістом і різною терміновістю. Рушій спрямовує кожен вид до потрібного "
          "фахівця. Саме тому система здатна зберігати докази — вона ніколи не зводить вхід і пакет до "
          "одного знеособленого числа."),
    ("h2", "2. Два годинники — швидкі рефлекси й повільні судження"),
    ("p", "Біологія працює на багатьох годинниках одночасно: нервові імпульси долають відстань за "
          "мілісекунди, тоді як гормони дифундують секунди-хвилини. Jneopallium копіює це **швидким циклом** "
          "(щотакту) і **повільним циклом** (кожні N тактів). Мережеві пакети та системні виклики "
          "обробляються у швидкому циклі для миттєвої реакції; повільніший контекст — кіберрозвідка, "
          "критичність активу, вікна обслуговування — у повільному. Результат: виявлення лишається швидким, "
          "а контекст, що має *змінити* вердикт, застосовується у власному природному темпі."),
    ("h2", "3. Фахівці без стану, що замінюються"),
    ("p", "Кожен крок обробки — це малий **процесор без стану**, під'єднаний до нейрона через інтерфейс, а "
          "не жорстко закодований клас. На практиці це означає, що будь-який детектор — сигнатурний рушій, "
          "модель аномалій, логіку кореляції — можна оновити чи замінити для конкретного розгортання, не "
          "торкаючись решти системи. Лабораторія може запустити простий зіставлювач; банк може під'єднати "
          "високопродуктивний комерційний рушій за тим самим інтерфейсом."),

    ("h1", "Огляд архітектури: як поєднуються складові", "5"),
    ("p", "На найвищому рівні телеметрія тече в одному напрямку через конвеєр дедалі розумніших етапів, а з "
          "іншого боку виходить рекомендація, яку можна перевірити аудитом:"),
    ("code",
     "багатоджерельна телеметрія\n"
     "   -> канонічні адаптери подій      (переклад усього однією мовою)\n"
     "   -> швидкі рецептори хоста й мережі (дешева миттєва оцінка доказів)\n"
     "   -> часова кореляція загроз         (поєднання подій у часі)\n"
     "   -> планувальник відповіді          (вибір пропорційної рекомендації)\n"
     "   -> фіксований жорсткий запобіжник  (захист критичних активів, режим)\n"
     "   -> рекомендація + журнал аудиту    (вердикт, який можна пояснити)"),
    ("p", "Та сама форма з'являється всередині демо як невелика чотирирівнева нейронна мережа. Кожен рівень "
          "— це команда нейронів із чіткою роботою:"),
    ("table", ["Рівень", "Розмір", "Завдання, простими словами"],
     [["Рівень 0 — Вхід", "7", "Прийняти сім видів сирої телеметрії безпеки"],
      ["Рівень 1 — Нормалізація", "4", "Очистити її та дати кожній події швидку оцінку доказів"],
      ["Рівень 2 — Кореляція", "3", "Поєднати події в часі в гіпотезу про загрозу"],
      ["Рівень 3 — Планування", "2", "Перетворити гіпотезу на рекомендовану дію з розслідування"]],
     [2.3, 0.9, 3.5]),
    ("p", "Повний промисловий модуль багатший — він додає пам'ять, гомеостаз (саморегуляцію) та окремий "
          "рівень відповіді, описані далі."),

    ("h1", "Сім рівнів, пояснені простою мовою", "6"),
    ("p", "Промисловий модуль кібербезпеки організований як захист тіла — від шкіри всередину. Ось кожен "
          "рівень і повсякденна робота, яку він виконує."),
    ("h3", "Рівень 0 — Прийом (органи чуття)"),
    ("p", "Прийом пакетів, системних викликів і журналів з обмеженням швидкості. Як двері з турнікетом, він "
          "приймає телеметрію з контрольованою швидкістю, тож потік трафіку — випадковий чи навмисна атака "
          "на відмову в обслуговуванні — не може перевантажити все, що стоїть за ним."),
    ("h3", "Рівень 1 — Вроджене виявлення (першочергові захисники)"),
    ("p", "Швидкі вроджені зіставлювачі: сигнатури відомого зловмисного, заборонені послідовності системних "
          "викликів та облік трафіку для кожного з'єднання. **М'який білий список** тут відфільтровує "
          "відомо доброякісні шаблони. Цей рівень дешевий і миттєвий — він ловить очевидне."),
    ("h3", "Рівень 2 — Адаптивне виявлення (учні)"),
    ("p", "Саме тут система вивчає *вашу* норму. Вона тримає рухому базову лінію поведінки кожного "
          "користувача й вузла та позначає суттєві відхилення. Сюди входить **детектор маяків** (виявляє "
          "метрономну регулярність зловмисного ПЗ, що «телефонує додому») та **детектор бічного руху** "
          "(виявляє, коли один обліковий запис раптом автентифікується на багатьох машинах). Найважливіше: "
          "навчання цього рівня **заморожується під час активної атаки**, тож поведінка зловмисника ніколи "
          "тихо не вбирається у визначення «норми»."),
    ("h3", "Рівень 3 — Пам'ять (імунна пам'ять)"),
    ("p", "Пам'ятає кампанії атак і техніки, з яких вони складалися, і прив'язує розрізнені шматки доказів "
          "до єдиної хронології інциденту. Саме так слабкий новий сигнал можна швидко розпізнати як частину "
          "шаблону, який система вже бачила."),
    ("h3", "Рівень 4 — Гіпотеза й планування (діагноз)"),
    ("p", "Поєднує сигнатурні докази та докази аномалій в одну ймовірність — **апостеріорну** — того, що "
          "атака триває, і відображає цю ймовірність на пропорційну смугу відповіді. Це рівень, який "
          "перетворює розрізнені підказки на вердикт із рівнем впевненості."),
    ("h3", "Рівень 5 — Відповідь (пропорційна реакція)"),
    ("p", "Володіє **жорстким запобіжником** (білий список, який не можна скасувати, і захист критичних "
          "активів), **логікою карантину** (лише додатна тривалість, автоматичне зняття) та опційним "
          "відкатом зі знімка. У режимі ADVISORY цей рівень видає *рекомендації та докази*, а не примусові "
          "дії."),
    ("h3", "Рівень 7 — Гомеостаз (саморегуляція)"),
    ("p", "Підтримує систему здоровою. **Монітор втоми від сповіщень** підвищує пороги виявлення, коли "
          "хибні спрацювання зростають, а **обмежувач виснаження** діє як бюджет на оцінку правил, тож "
          "потік не може заморити важливі етапи. Біологія називає це гомеостазом; інженерія — плавною "
          "деградацією під навантаженням."),
    ("callout", "Чому рівні нумеруються 0–7 з пропуском",
     "Нумерація віддзеркалює ширшу когнітивну архітектуру Jneopallium, де деякі рівні (як-от афект і "
     "цікавість) навмисно вимкнені для роботи з безпекою — захисна система не повинна «нудьгувати» чи "
     "«емоціонувати». Пропуски навмисні, а не відсутні частини.",
     "info"),

    ("h1", "Сигнали: спільна мова системи", "7"),
    ("p", "Усе, що бачить система, перетворюється на **типізований сигнал** — невеликий, добре описаний "
          "об'єкт, що несе і дані, і зміст. Саме це дає рушію змогу тримати ланцюг доказів від сирої "
          "телеметрії аж до фінальної рекомендації. Демо кібербезпеки визначає десять типів сигналів:"),
    ("table", ["Сигнал", "Що несе", "Розуміти як"],
     [["AuthenticationEventSignal", "Входи, їх результат і контекст", "Хто намагався увійти й чи вдалося"],
      ["ProcessEventSignal", "Запуск програм, ланцюги «батько/нащадок»", "Що запустилося і що його запустило"],
      ["DnsLookupSignal", "Запити доменних імен", "Кому машина намагалася «зателефонувати»"],
      ["NetworkFlowSignal", "Зведення байтів/пакетів з'єднання", "Скільки даних і куди пішло"],
      ["ThreatIntelContextSignal", "Відомо-погані індикатори з впевненістю", "Зовнішня підказка"],
      ["AssetContextSignal", "Наскільки актив критичний і чий він", "Наскільки ця машина важлива"],
      ["MaintenanceWindowSignal", "Контекст погоджених змін", "Контекст «це було заплановано»"],
      ["RiskScoreSignal", "Обчислене число ризику", "Поточний підсумок ризику"],
      ["ThreatHypothesisSignal", "Корельована гіпотеза про атаку", "Діагноз у процесі"],
      ["SecurityAdvisorySignal", "Фінальна рекомендація + докази", "Вердикт, за яким ви дієте"]],
     [2.3, 2.4, 2.1]),
    ("p", "Кожен тип сигналу також оголошує, **як часто** його слід обробляти. Швидкі, термінові сигнали "
          "(пакети, системні виклики) виконуються щотакту; повільніші контекстні сигнали (кіберрозвідка, "
          "обслуговування, оновлення самотолерантності) — у повільному циклі. Це втілення ідеї «двох "
          "годинників»: система реагує миттєво, але розмірковує повільно — саме там, де кожне доречне."),

    ("h1", "Часова кореляція: поєднання подій у часі", "8"),
    ("p", "Це серце продукту. Справжнє вторгнення — не одна подія, а впорядкований ланцюг. Класична "
          "корпоративна атака має такий вигляд:"),
    ("code",
     "незвичний вхід\n"
     "      ->  запуск процесу\n"
     "            ->  DNS-запит\n"
     "                  ->  бічний рух (перехід на інший вузол)\n"
     "                        ->  маяки командного центру (C2)\n"
     "                              ->  витік даних"),
    ("p", "Рушій водночас стежить за кількома перекривними часовими вікнами, кожне з яких налаштоване на "
          "інший ритм поведінки зловмисника:"),
    ("table", ["Вікно", "Тривалість", "Що ловить"],
     [["Швидке вікно", "1–10 секунд", "Миттєві, різкі сигнали"],
      ["Поведінкове вікно", "1–5 хвилин", "Сплеск пов'язаних дій"],
      ["Вікно інциденту", "30–120 хвилин", "Повний ланцюг атаки, що розгортається"],
      ["Базове вікно", "Години-дні", "Який вигляд має «норма» в часі"]],
     [1.9, 1.8, 3.1]),
    ("p", "У межах цих вікон модель винагороджує **впорядковані переходи** — вхід, *за яким іде* запуск, "
          "*за яким іде* бічний рух, набирає значно більше, ніж ті самі події у випадковому порядку чи "
          "розкидані випадково по непов'язаних машинах. Вона також розпізнає шаблон **«повільно й тихо»**: "
          "слабкі, повторювані вихідні передавання, що окремо нічого не означають, але, корельовані за "
          "вікно інциденту й поєднані з контекстом кіберрозвідки, виявляють тихий витік даних."),
    ("callout", "Чому *час події* важливіший за час надходження",
     "Телеметрія надходить із запізненням, не за порядком, із повторами. Рушій корелює за часом, коли подія "
     "*насправді сталася* (її event-tick), а не за часом отримання. Саме це дає змогу відтворити справжню "
     "послідовність атаки навіть тоді, коли дані безладні — властивість, якої бракує інструментам «одна "
     "подія».",
     "info"),

    ("h1", "Навчена модель: чого вона насправді навчилася", "9"),
    ("p", "У комплекті з продуктом — вбудована еталонна модель "
          "`cybersecurity-temporal-threat-correlator`. Це навмисно **прозора, відтворювана база**: "
          "класово-збалансована логістична модель над 34 ознаками часового вікна. Прозорість — це перевага: "
          "кожну вагу можна перевірити, тож команда безпеки бачить, *чому* досягнуто вердикту, перш ніж "
          "пізніше переходити до більших послідовнісних моделей (GRU/TCN/трансформер)."),
    ("p", "Модель згортає кожне часове вікно у 34 числа (ознаки), як-от: наскільки впорядковані події, "
          "наскільки різноманітні джерела даних, найсильніше влучання кіберрозвідки, середня критичність "
          "активу та чи було активне вікно обслуговування. Ознаки, на які вона спиралася найбільше, "
          "розповідають узгоджену історію:"),
    ("h3", "Найвагоміші докази, які модель вважає підозрілими"),
    ("table", ["Ознака", "Вага", "Простий зміст"],
     [["technique_command_and_control", "+0.43", "Ознаки, що машина «телефонує» зловмиснику"],
      ["network_receptor_score", "+0.42", "Сильні докази з боку мережі"],
      ["max_threat_intel", "+0.42", "Зовнішня підказка високої впевненості"],
      ["slow_context_score", "+0.39", "Кіберрозвідка плюс критичність активу разом"],
      ["mean_evidence", "+0.39", "Стабільно сильні докази по всьому вікну"]],
     [2.8, 0.9, 3.1]),
    ("h3", "Найвагоміші докази, які модель вважає заспокійливими"),
    ("table", ["Ознака", "Вага", "Простий зміст"],
     [["maintenance_ratio", "-0.45", "Активність відбувалася під час погодженого обслуговування"],
      ["benign_context_ratio", "-0.39", "Здебільшого доброякісний, очікуваний контекст"],
      ["technique_unusual_login", "-0.27", "Поодинокий дивний вхід, за яким нічого не йде"],
      ["source_diversity", "-0.18", "Докази лише з одного джерела, не злагоджений ланцюг"]],
     [2.8, 0.9, 3.1]),
    ("h3", "Від моделі до готової до розгортання мережі"),
    ("p", "Тренер не зупиняється на наборі ваг — він видає **повну, готову до розгортання мережу "
          "JNeopallium**: п'ять рівнів із восьми справжніх нейронів, де навчені ваги, поріг рішення, "
          "часові гейти послідовності та політику безпеки записано в готові до завантаження файли "
          "конфігурації рівнів. Те, що навчили, — це саме те, що працює в промислі."),
    ("table", ["Згенерований рівень", "Справжні нейрони", "Робота"],
     [["Вхід", "—", "Межа багатоджерельних подій"],
      ["Швидкі докази", "NetworkFlowNeuron, SignaturePatternNeuron, ProcessBehaviourNeuron, EntityBehaviourBaselineNeuron", "Миттєва оцінка"],
      ["Кореляція", "TemporalThreatCorrelationNeuron", "Навчена часова кореляція"],
      ["Планування + запобіжник", "ResponsePlanningNeuron, ResponseGateNeuron", "Смуги рекомендацій + фіксований запобіжник"],
      ["Результат", "ResponsePlanningNeuron", "Рекомендаційний вивід"]],
     [1.6, 3.4, 1.8]),
    ("p", "Рівень кореляції також несе **політику адаптації базової лінії**, що заморожує навчання, коли "
          "апостеріорна сягає 0.3 або впевненість сигнатури сягає 0.8, адаптуючись лише з довірених "
          "доброякісних періодів — правило проти отруєння, закодоване прямо в готовій до розгортання "
          "мережі."),
    ("callout", "Прочитайте це чесно",
     "На вбудованих детермінованих еталонних даних модель показує ідеальний результат (точність і повнота "
     "1.0). Це доводить, що конвеєр працює наскрізно — це НЕ твердження про реальну точність. Справжню "
     "якість треба заслужити на зовнішніх наборах даних. Супровідний звіт про тестування прямо про це "
     "говорить, і ми тримаємо це на видноті, а не ховаємо.",
     "warning"),

    ("h1", "Безпека за задумом: чому вона не зашкодить вашій мережі", "10"),
    ("p", "Система виявлення, яка має силу блокувати трафік, — це також система, яка може вивести ваш бізнес "
          "з ладу, якщо помиляється. Цей продукт сконструйовано так, щоб помилка була *безпечною*. Чотири "
          "структурні гарантії виконують основну роботу:"),
    ("num", "**Рекомендації за замовчуванням.** Стеля безпеки — ADVISORY. Система рекомендує кандидатів на "
            "розслідування чи стримування; вона не ізолює вузли й не блокує трафік самостійно. Активне "
            "примусове виконання потребує окремого, навмисно доданого обґрунтування безпеки, процесу "
            "погодження та шляху відкату."),
    ("num", "**Жорсткі запобіжники фіксовані, а не навчені.** Білий список захищених систем і перелік "
            "критичних активів задаються під час побудови і не можуть бути змінені жодним сигналом під час "
            "роботи. Модель може вчитися, що виглядає підозріло; вона ніколи не зможе навчитися знімати "
            "захист критичного сервера."),
    ("num", "**Заморожування базової лінії під час атак.** Коли впевненість системи сягає стану "
            "спостереження/тривоги, адаптивне навчання заморожується, тож зловмисник не може терпляче "
            "навчити систему, що його поведінка нормальна (атака «отруєння базової лінії»)."),
    ("num", "**Стримування завжди завершується.** Кожна рекомендація карантину має додатну тривалість і "
            "автоматичне зняття. Ніщо рекомендоване системою не є постійним, доки людина не підтвердить це "
            "незалежно."),
    ("p", "Понад це кожна рекомендація зберігає повний **родовід доказів**: задіяну сутність, точний "
          "часовий діапазон, які джерела й техніки долучилися, апостеріорну ймовірність, смугу відповіді, "
          "чи була заморожена базова лінія, а також точну версію та контрольну суму використаної моделі. "
          "Якщо регулятор, аудитор чи аналітик запитає «чому система так сказала?», завжди є повна, "
          "відтворювана відповідь."),

    ("h1", "Основа даних: набори даних за моделлю", "11"),
    ("p", "Часовий корелятор хороший рівно настільки, наскільки різноманітні атаки він бачив. Конструкція "
          "навчання навмисно охоплює сім взаємодоповнювальних публічних і лабораторних джерел, кожне з яких "
          "відображене на типізовані сигнали системи, тож модель вчиться з багатьох ракурсів, а не з однієї "
          "вузької таблиці:"),
    ("table", ["Набір даних", "Роль"],
     [["LANL Comprehensive Multi-Source Cyber-Security Events", "Реалістична корпоративна часова валідація з мітками red-team"],
      ["ToN_IoT", "Багатоджерельні Windows/Linux/мережа/IoT з істинними мітками"],
      ["DARPA OpTC", "Валідація просунутих кінцевих точок і провенансу APT"],
      ["CIC-IDS2017 / CSE-CIC-IDS2018", "Попереднє навчання детектора мережевих потоків"],
      ["UNSW-NB15", "Швидкий мережевий класифікатор і валідація пропускної здатності"],
      ["Вихід лабораторії MITRE CALDERA", "Точні мітки для контрольованих, відтворюваних ланцюгів атак"]],
     [3.4, 3.4]),
    ("p", "Сувора **політика розбиття** запобігає найпоширенішому способу завищення тверджень про "
          "машинне навчання в безпеці: дані ніколи не розбиваються за випадковими окремими рядками (що "
          "просочує майже однакові події тієї самої кампанії і в навчання, і в тест). Натомість їх "
          "розбивають за періодом часу, кампанією, групою вузлів і типом атаки — тож тест завжди вимірює "
          "якість на справді небачених атаках."),

    ("h1", "Топологія розгортання: де це працює", "12"),
    ("p", "Та сама модель працює у трьох формах розгортання, тож пасує і до демо на ноутбуці, і до "
          "кластерного підприємства:"),
    ("table", ["Режим", "Опис", "Типове застосування"],
     [["Локальний", "Один Java-процес", "Демо, пілоти, периферійні майданчики"],
      ["Кластер (HTTP)", "Розподіл по робочих вузлах через HTTP", "Телеметрія масштабу підприємства"],
      ["Кластер (gRPC)", "Розподіл через gRPC; підтримка цілей FPGA", "Висока пропускність, низька затримка"]],
     [1.8, 3.2, 1.8]),
    ("p", "Телеметрія доходить до системи через **мости** — адаптери, що перекладають реальний протокол на "
          "типізовані сигнали. Модуль кібербезпеки спроєктовано довкола потоку подій у стилі Kafka, тож "
          "справжній міст Kafka може замінити вхід демо, зберігши той самий типізований контракт подій. "
          "Оскільки кореляція ведеться за часом події, запізніла чи невпорядкована телеметрія все одно "
          "потрапляє в правильне місце послідовності."),

    ("h1", "Наскрізний приклад: три потоки, три вердикти", "13"),
    ("p", "Найясніший спосіб зрозуміти систему — побачити, як вона судить три одночасні потоки, які наївний "
          "інструмент оцінив би хибно. Усі три проходять той самий конвеєр; різниця цілком у "
          "**кореляції та контексті**."),
    ("h3", "Потік A — справжня атака (правильно піднята)"),
    ("p", "`user:backup-service@workstation-17`: незвичний вхід, потім закодована команда PowerShell, потім "
          "розгалуження автентифікації на багато вузлів, потім рідкісний DNS-запит, потім періодичний "
          "вихідний трафік. Окремо — простимо. У цьому **порядку**, у межах вікна інциденту, це хрестоматійне "
          "вторгнення. Вердикт: **TEMPORAL_THREAT_ADVISORY**, із замороженою базовою лінією, тож атака не "
          "може отруїти «норму»."),
    ("h3", "Потік B — планове обслуговування (правильно заспокоєне)"),
    ("p", "`svc:deployment-agent@web-tier`: повтори службового облікового запису та підписана активність "
          "розгортання під час погодженого вікна обслуговування. Контекст обслуговування **знижує оцінку** "
          "— але не стирає докази; система все одно записує спостереження для аудиту. Вердикт: "
          "**CONTEXT_SUPPRESSED_OBSERVATION**. Це випадок, який інструменти «одна подія» помиляють "
          "найчастіше, причому в обидва боки."),
    ("h3", "Потік C — тихий витік даних (правильно виявлений)"),
    ("p", "`host:finance-file-01`: слабкі, повторювані вихідні передавання, які окремо нічого не означають. "
          "Корельовані за вікно інциденту й поєднані з контекстом кіберрозвідки, вони стають вірогідним "
          "повільним витоком. Вердикт: **LOW_AND_SLOW_CORRELATION** — саме той випадок, який мала "
          "використовувати сліпа зона «повільно й тихо»."),
    ("callout", "Сенс трьох потоків",
     "Атака набирає більше, ніж доброякісне обслуговування; обслуговування заспокоєне без втрати журналу "
     "аудиту; тихий витік виявлено. І в кожному разі вихід — це рекомендація з доказами, а не тихе "
     "блокування. Саме цього не може дати інструмент «одна подія за раз».",
     "success"),

    ("h1", "Словник для нефахівців", "14"),
    ("table", ["Термін", "Значення простою мовою"],
     [["Режим ADVISORY", "Система рекомендує; рішення приймають люди. Вона ніколи не блокує сама."],
      ["Виявлення аномалій", "Помічання поведінки, незвичної для конкретного користувача чи машини."],
      ["Базова лінія", "Вивчена картина того, який вигляд має «норма» для сутності."],
      ["Отруєння базової лінії", "Зловмисник повільно вчить систему, що його поведінка нормальна."],
      ["Маяки (beaconing)", "Зловмисне ПЗ контактує з керівним сервером через рівні проміжки, як пульс."],
      ["Час перебування", "Скільки зловмисник лишається в мережі до виявлення."],
      ["Родовід доказів", "Повний, відтворюваний шлях від сирих даних до вердикту."],
      ["Ексфільтрація", "Викрадення даних за межі організації."],
      ["Бічний рух", "Перехід з однієї скомпрометованої машини на інші."],
      ["Апостеріорна (ймовірність)", "Ймовірність атаки за моделлю з огляду на докази."],
      ["Карантин", "Тимчасове стримування сутності — тут завжди з терміном завершення."],
      ["Сигнатурне виявлення", "Зіставлення з відомими, названими поганими шаблонами."],
      ["Часова кореляція", "Поєднання подій у часі в одну узгоджену картину."],
      ["Телеметрія", "Потік сирих даних безпеки: входи, процеси, DNS, мережеві потоки."]],
     [2.2, 4.6]),
    ("spacer", 8),
    ("pi", "Модуль кібербезпеки Jneopallium · Демо 06 · Часова кореляція загроз. "
           "Архітектурна стаття, підготовлена як для технічних, так і для нетехнічних читачів. "
           "Режим безпеки: ADVISORY. Ліцензія: BSD 3-Clause."),
]
