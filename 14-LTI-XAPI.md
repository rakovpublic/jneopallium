# Bridge 14 — LTI / xAPI (adaptive tutoring and learning analytics)

> **Prerequisite:** `00-FRAMEWORK.md`.

**Priority:** medium-high. **Safety ceiling:** `ADVISORY`. The bridge produces content recommendations, hints, pacing suggestions, and intervention proposals — it never auto-grades, never auto-enrols, never modifies a learner's record without instructor confirmation.

## 1. Domain context

Two complementary standards from 1EdTech:

* **LTI** — Learning Tools Interoperability — the standard way an LMS (Canvas, Moodle, Blackboard, Brightspace) launches an external tool. LTI 1.3 uses OpenID Connect and OAuth 2.0; it carries learner identity, course context, and roles, plus deep-linking and grading services (AGS) and Names and Roles services (NRPS).
* **xAPI** — Experience API — the learning-record format for capturing what a learner did, anywhere. An *xAPI statement* is `{actor, verb, object, result, context}` and is sent to a Learning Record Store (LRS).

The combination is the modern foundation of adaptive tutoring: LTI gets Jneopallium *into* the learning environment with the right context, xAPI lets it *observe* what learners are doing, and the LMS / LRS round-trip lets it *advise* without bypassing the instructor.

The repo's `tutoring/` package already holds every signal type the bridge produces and consumes: `ResponseSignal`, `MasteryUpdateSignal`, `EngagementSignal`, `HintSignal`, `ItemPresentationSignal`, `InterventionSignal`, `ScaffoldingSignal`, `ContentRecommendationSignal`, `AffectObservationSignal`, `ReviewScheduleSignal`. Affect inputs reuse `affect/AffectStateSignal`/`InteroceptiveSignal`/`AppraisalSignal` (when paired with the LSL bridge for physiology).

## 2. Maven dependencies

```xml
<!-- xAPI Java client (Rustici tincan-java) -->
<dependency>
    <groupId>com.rusticisoftware.tincan</groupId>
    <artifactId>tincan</artifactId>
    <version>1.0.3</version>
</dependency>

<!-- LTI 1.3 / OAuth2 / JWT — use the standard library set rather than a
     dedicated LTI artifact, which keeps the bridge framework-agnostic. -->
<dependency>
    <groupId>com.nimbusds</groupId>
    <artifactId>nimbus-jose-jwt</artifactId>
    <version>9.40</version>
</dependency>
<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>java-jwt</artifactId>
    <version>4.4.0</version>
</dependency>
```

If a maintained LTI 1.3 client artifact is available at the time of implementation (1EdTech publishes a Java reference implementation of the AGS / NRPS clients), prefer that over hand-rolled JWT plumbing.

## 3. Why advisory

* Auto-grading is an instructor decision. The bridge can *propose* a score via AGS but the deployment policy must require instructor confirmation before the proposal is recorded.
* Content recommendation that bypasses the instructor undermines course design intent. The bridge's egress is "suggest next item to instructor" or "render a hint *to the learner* through an existing instructor-approved hint mechanism".
* xAPI verbs include `failed`, `attempted`, `mastered` — Jneopallium can *infer* mastery, but only the LMS can *record* it. The bridge stays on the inference side.

## 4. Architecture

```
LMS (Canvas / Moodle / …)        Tool (this bridge / Jneopallium UI)
    │                                  │
    │  LTI 1.3 launch (OIDC + JWT)    │
    │ ────────────────────────────────▶│
    │                                  │
    │  xAPI statements (learner acts) │
    │ ◀──────────────────────────────  │  (when bridge is the LRS endpoint)
    │   OR                             │
    │  bridge polls LRS for new        │
    │  statements                      │
    │                                  │
    │  Names & Roles, AGS proposals    │
    │ ◀──────────────────────────────  │
    │  (instructor-confirmation gated) │

           bridge internals:
           ┌────────────────────────┐
           │ LtiClientService       │  parses launch JWT, holds tool-platform
           │                        │  registration
           │ XapiClientService      │  pulls statements from LRS or accepts
           │                        │  POST /xapi/statements as an LRS
           └────────────────────────┘
                  │
        ┌─────────▼─────────┐
        │ XapiInput         │   → ResponseSignal, MasteryUpdateSignal,
        │                   │     EngagementSignal, AffectObservationSignal
        └───────────────────┘
                  ▼
[Pipeline → LtiAdvisoryOutputAggregator]
                  ▼
        ContentRecommendation, Hint, Intervention, Scaffolding
        →  rendered via tool UI / posted to AGS as proposed score
           with "instructor must confirm" flag
```

## 5. Signal mapping

| xAPI / LTI input | Decoder | Jneopallium signal |
|---|---|---|
| xAPI `attempted` / `answered` statement on an item | extract result.score, result.success, item id | `ResponseSignal` |
| xAPI `mastered` / `failed` on a competency | competency id + outcome | `MasteryUpdateSignal` |
| xAPI `interacted` / `experienced` activity | dwell time, focus state | `EngagementSignal` |
| xAPI extension carrying affect (when paired with LSL bridge providing affect data via xAPI extension) | affect dimensions | `AffectObservationSignal` |
| LTI launch claims | learner role, course context | bridge metadata; not a signal |
| LTI Names and Roles | enrolment list | bridge metadata |
| LTI Assignment-and-Grades line items | item descriptors | bridge metadata |

Egress (advisory, instructor-mediated):

| Jneopallium signal | LTI/xAPI output | Notes |
|---|---|---|
| `ContentRecommendationSignal` | xAPI statement (verb=`recommended`, actor=Jneopallium) | Bridge does not change LMS state. |
| `HintSignal` | rendered through tool UI when learner is active | Or as xAPI `experienced` for record. |
| `InterventionSignal` | xAPI statement + optional notification to instructor (LMS-specific) | High-severity items prompt instructor; never bypass them. |
| `MasteryUpdateSignal` (proposed) | LTI AGS `Score` with `gradingProgress=PendingManual` | Instructor-confirmation gate enforced on the LMS side. |
| `ScaffoldingSignal` | tool-UI rendering only | Same path as hints. |

## 6. Configuration

```yaml
lti:
  toolName: "jneopallium-tutor"
  platforms:
    - issuer: "https://canvas.example.edu"
      clientId: "10000000000123"
      authLoginUrl: "https://canvas.example.edu/api/lti/authorize_redirect"
      authTokenUrl: "https://canvas.example.edu/login/oauth2/token"
      keysetUrl: "https://canvas.example.edu/api/lti/security/jwks"
      deploymentIds: ["1:abcdef"]
  toolPrivateKeyPath: "/etc/jneopallium/lti/tool-private.pem"
  toolPublicJwksUrl: "https://jneo-tutor.example.edu/.well-known/jwks.json"

xapi:
  mode: "PULL"                        # or "PUSH" (bridge acts as LRS endpoint)
  lrs:
    endpoint: "https://lrs.example.edu/xapi"
    auth:
      type: "BasicAuth"
      usernameEnv: "XAPI_USER"
      passwordEnv: "XAPI_PASSWORD"
    pollIntervalSeconds: 60

cohort:
  courseIds: ["course-101", "course-202"]

reads:
  - bindingId: "ATTEMPTS"
    xapiVerb: "http://adlnet.gov/expapi/verbs/attempted"
    targetSignal: "RESPONSE"
    signalTagPrefix: "TUTOR.ATTEMPT"

  - bindingId: "ENGAGEMENT"
    xapiVerb: "http://adlnet.gov/expapi/verbs/interacted"
    targetSignal: "ENGAGEMENT"
    signalTagPrefix: "TUTOR.ENGAGE"

writes:
  - bindingId: "RECOMMENDATIONS"
    xapiVerb: "http://activitystrea.ms/recommend"
    targetActor: "Jneopallium-Tutor"
    signalTag: "TUTOR.RECOMMEND"

  - bindingId: "PROPOSED-SCORES"
    ltiAgs: true
    gradingProgress: "PendingManual"     # locked — bridge cannot set "FullyGraded"
    signalTag: "TUTOR.SCORE.PROPOSAL"

privacy:
  pseudonymise: true                   # actor-id → SHA256(actor + saltEnv)
  saltEnv: "TUTOR_PSEUDO_SALT"
  redactFreeText: true                  # strip statement.result.response

audit:
  localAuditFile: "/var/log/jneopallium/lti-audit.jsonl"

perTagSafetyMode:
  RECOMMENDATIONS: ADVISORY
  PROPOSED-SCORES: ADVISORY            # AUTONOMOUS rejected by validator
```

## 7. Package layout

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/lti/
├── LtiBridgeConfig.java
├── LtiBridgeConfigLoader.java
├── LtiPlatformBinding.java
├── XapiVerbBinding.java
├── XapiStatementMapper.java
├── PseudonymService.java                 (shared with FHIR if both deployed)
├── LtiClientService.java                 (OIDC + JWT, AGS + NRPS clients)
├── XapiClientService.java                (PULL or PUSH mode)
├── XapiResponseInput.java
├── XapiEngagementInput.java
├── XapiAffectInput.java                  (for LSL-paired deployments)
└── LtiAdvisoryOutputAggregator.java
```

## 8. Phase plan

| Phase | Goal |
|-------|------|
| 1 | xAPI PULL from a free LRS (Yet Analytics' SQL LRS open source, or Watershed / Veracity sandbox). Read-only mapping of `attempted`/`interacted` to `ResponseSignal`/`EngagementSignal`. |
| 2 | LTI 1.3 launch flow against a Canvas test instance or `lti-1.3-spec/test-tool`. Course context wiring. |
| 3 | Egress: xAPI `recommended` statements + AGS proposed scores with `PendingManual`. Validator rejects `FullyGraded`. |
| 4 | **Not pursued.** Auto-grading and auto-enrolment are out of scope. |

## 9. Bridge-specific scenarios

| # | Scenario | Setup | Expected |
|---|----------|-------|----------|
| **S7** | LRS pull | Yet Analytics SQL LRS with seeded statements | Bridge emits `ResponseSignal`s within `pollIntervalSeconds` |
| **S8** | LTI launch | 1EdTech LTI 1.3 reference platform configured against this tool | Successful OIDC handshake, JWT verified, course context decoded |
| **S9** | AGS proposal | Pipeline emits `MasteryUpdateSignal` for a learner | xAPI statement + AGS Score posted with `gradingProgress=PendingManual` |
| **S10** | FullyGraded rejected | Config attempts `gradingProgress: FullyGraded` | `LtiBridgeConfigLoader.load()` throws |
| **S11** | Pseudonymisation | xAPI statement carries `actor.account.name = "alice@example.edu"` | Emitted signal carries hashed id; raw email never appears in audit JSONL |
| **S12** | Free-text redaction | `statement.result.response = "I think the answer is X because…"` | Mapped signal carries no response text; bridge log records that a redaction occurred |

## 10. Risks

| # | Risk | Mitigation |
|---|------|------------|
| R1 | Learner PII in xAPI statements (email, full name) | Pseudonymisation on by default; raw identifiers never leave the bridge JVM. |
| R2 | LTI launch JWT replay or signature spoof | Use Nimbus / Auth0 JWT libraries; verify nonce + iss + aud + exp; rotate tool keys quarterly. |
| R3 | Auto-grading scope creep | Validator forbids `FullyGraded`; PRs that touch this gate require explicit privacy/instruction-policy review. |
| R4 | xAPI LRS dialect drift (verb URIs vary across vendors) | Per-deployment verb mapping; unknown verbs emitted as `EngagementSignal(modality=UNKNOWN)` with the original IRI as a tag. |
| R5 | FERPA / GDPR-art-9 sensitive data in statement extensions | Free-text redaction on by default; statement extensions on a per-deployment allow list. |

## 11. Regulatory and ethical posture

The bridge is **not** a high-stakes assessment tool. It does not determine grades, certifications, eligibility, or admissions outcomes. Each deployment is responsible for:

* Disclosing to learners that an algorithmic system is generating recommendations.
* Maintaining instructor authority over content sequencing and grading.
* Honouring local data-protection laws (FERPA in the US, GDPR in the EU, PIPEDA in Canada, equivalent regimes elsewhere).

These are deployment policy obligations the bridge does not enforce on its own — but its architectural choices (advisory ceiling, pseudonymisation default, redaction default, instructor-confirmation gate on AGS) make the policy easier to honour, not harder.

## 12. References

* LTI 1.3 — `https://www.imsglobal.org/spec/lti/v1p3`.
* xAPI specification — `https://github.com/adlnet/xAPI-Spec`.
* 1EdTech LTI tools and reference platforms — `https://www.1edtech.org/standards/lti`.
* Rustici tincan-java — `https://github.com/RusticiSoftware/TinCanJava`.
