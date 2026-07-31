# Ad-fraud campaign

## Evidence-backed proposition

Jneopallium currently contains an advertising-fraud domain module, typed event and decision schemas,
an advisory runtime scorer, a streaming HTTP demo service, and a deterministic synthetic training and
evaluation workflow. The credible proposition is a bounded technical evaluation of multi-timescale
invalid-traffic evidence: event integrity, human-interaction signals, attribution sequences,
publisher/device/campaign context, supply-path consistency, entity graphs, traffic quality,
uncertainty, and human-review routing.

The proposition is not “fraud solved.” The repository does not establish production effectiveness,
customer adoption, MRC accreditation, IAB certification, financial savings, or safe automated
billing enforcement. Customer-specific integration, first-party labeled traffic, calibration,
operational review, appeal/rollback design, privacy review, and legal review remain required.

Relevant public interoperability context includes IAB Tech Lab's OpenRTB, ads.txt, app-ads.txt,
sellers.json, SupplyChain Object, and taxonomy work. The campaign records the official
[supply-chain standards overview](https://iabtechlab.com/standards/supply-chain-foundations/) and
[sellers.json specification page](https://dev.iabtechlab.com/sellers-json/) as market evidence. It
does not crawl or harvest IAB resources; research is recorded from approved sources or an authorized
provider.

## Current implementation

The primary evidence is:

- `domains/domain-adfraud`: canonical events, signal/processor layers, advisory scoring, response
  gate, tests, and bundled model resources.
- `demos/demo-adfraud`: a deterministic command-line example and an HTTP service with health,
  readiness, model metadata, scoring, feedback, and Prometheus-style metrics endpoints.
- `scripts/demo-ad-fraud`: a seeded, bounded synthetic data workflow that creates dataset, leakage,
  calibration, threshold, drift, per-class/group, latency, explanation, model-card, and readiness
  artifacts under `target/jneopallium-ad-fraud/`.

Run the Java tests from the repository root:

```bash
mvn -pl demos/demo-adfraud -am -Dtest=AdFraudModuleTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Run the bounded offline evidence workflow on Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\demo-ad-fraud\run_all.ps1 `
  -quick -offline -maxRows 2000 -maxMemoryMb 768 -seed 1729
```

On Linux/macOS:

```bash
scripts/demo-ad-fraud/run_all.sh --quick --offline --max-rows 2000 --max-memory-mb 768 --seed 1729
```

The workflow now packages its model bundle under `target/jneopallium-ad-fraud/model-bundle`, does not
rewrite a legacy source-tree model location, and runs the current modular Maven reactor command.

## Input schema

`AdFraudEvent` is JSON-deserializable and versioned. Identifiers are expected to be HMACed or
pseudonymous before ingestion. Field groups include:

- identity and time: schema/event/type, event and ingest time, impression/click/session IDs;
- pseudonymous context: user, device, fingerprint, IP-prefix hash, ASN;
- inventory context: publisher, site/app, placement, creative, campaign, advertiser, exchange,
  seller, and supply-chain string;
- environment: country, device, OS, browser, SDK;
- integrity: signature/key/nonce, source and receive time, client/server evidence, device
  attestation, ads.txt authorization, sellers.json match, and supply-chain completeness;
- interaction: visibility, dwell, scroll, pointer/touch/keyboard/focus, click position, automation,
  headless, cookie age, and session count;
- downstream quality: retention, meaningful actions, purchase/refund/chargeback, uninstall delay,
  customer/analyst labels, source and label provenance, scenario, and numeric extensions.

Synthetic features are separated into integrity, bot, sequence, attribution, supply-chain, graph,
quality, unknown, click-volume, conversion-timing, incentive, retention, and accidental-risk groups.
Missing values are distinct from observed zero.

## Output schema

`AdFraudDecision` returns the event and model version, runtime mode, per-label probabilities, overall
invalid-traffic probability, estimated loss input/output, uncertainty, evidence completeness,
recommended action, human-readable reasons, duplicate status, and fallback status. The default mode
is `ADVISORY`; reasons provide the evidence trail. The streaming endpoints are
`POST /v1/ad-fraud/events` and `POST /v1/ad-fraud/score`. Health, readiness, model metadata, feedback,
and metrics endpoints are also available.

## Reproduced synthetic metrics

The 2026-07-31 quick/offline run used seed 1729, a 2,000-row synthetic corpus, and 228 combined held-
out evaluation rows. It produced macro F1 `0.981223`, micro F1 `0.985`, and p95 in-process scoring
latency `0.3217 ms` in that local run. The workflow reported Java tests passed and readiness as
engineering/shadow/advisory true, automated action false.

These are deterministic simulator results, not external benchmarks or expected production
performance. Several synthetic classes are intentionally separable. There were zero first-party
holdout rows. Attribution-hijack precision (`0.761905`) was materially weaker than most synthetic
classes and is a useful challenge case. Automated action remains blocked by absent first-party
production labels, legal/operational review, and an appeal/rollback process.

## Customer mapping and pilot asks

- Verification and fraud vendors: compare explainable multi-label evidence and review routing on a
  de-identified, labeled replay supplied under agreement.
- DSPs, SSPs, exchanges, and publisher monetization platforms: test OpenRTB/supply-path context
  mapping and consistency checks in shadow mode.
- Mobile/CTV/attribution platforms: evaluate event sequence, replay, postback integrity, device
  attestation, and delayed-conversion handling with an agreed taxonomy.
- Anti-bot and marketplace-integrity teams: test fast interaction evidence plus slower entity/graph
  context without sharing raw identifiers.
- Standards and research organizations: collaborate on reproducible synthetic scenario definitions,
  limitations, and evaluation protocols rather than a commercial effectiveness claim.

The call to action is a 30-minute schema review followed, if relevant, by a small synthetic or
de-identified shadow replay. No production enforcement, payout blocking, or accusations about a
person are in scope.

## Missing functionality and safety ceiling

There is no production OpenRTB ingestion adapter, first-party label set, customer policy layer,
real-time feature store, distributed deployment benchmark, independent validation, accreditation,
case-management integration, or proven privacy model. Public interfaces must be mapped with the
prospect; GIVT/SIVT and CTV/SSAI taxonomies require domain-owner review. Never produce offensive
evasion tooling or reveal detector thresholds for abuse. All decisions remain explainable,
reversible, advisory, and human-reviewed until the documented automated-action blockers are closed.
