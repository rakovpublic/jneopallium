# Security Policy

Jneopallium is not only a modelling library. Through its bridge framework it can read from and
write to industrial controllers, power-grid equipment, clinical systems, drones and embedded
devices. A vulnerability in the write path, the safety ceilings, the override handling or the audit
trail can have physical consequences. Please treat security reports here accordingly — and we will
too.

## Reporting a vulnerability

**Do not open a public GitHub issue for a security vulnerability.**

Report it privately, by either route:

1. **GitHub private vulnerability reporting** (preferred) —
   [open a draft advisory](https://github.com/rakovpublic/jneopallium/security/advisories/new).
   This keeps the report, the discussion and the eventual advisory in one place.
2. **Email** — `rakovpublic@gmail.com`, with `SECURITY` in the subject line. If you want to
   encrypt, ask for the maintainer's PGP key in a first message containing no details; the same key
   identity signs the project's Maven Central releases.

You will get an acknowledgement even if the report turns out not to be a vulnerability.

### What to include

The more of this you can provide, the faster the fix:

- affected version or commit SHA, and which modules are involved;
- the deployment mode — local, HTTP cluster, or gRPC cluster;
- for bridge issues: the protocol, the configured safety mode (`SHADOW`, `ADVISORY`,
  `AUTONOMOUS`, …), and whether the issue lets a write bypass the interlock, override, clamp,
  rate-limit or audit stage;
- a minimal reproduction, or a clear description of the attack path if a reproduction would require
  real equipment;
- the impact as you see it, and any mitigation you have found;
- whether you intend to disclose publicly, and on what timeline.

Reports in any of English, Ukrainian or Russian are fine.

## What to expect

Jneopallium is a volunteer project with a single active maintainer. These are honest targets, not a
contractual SLA:

| Stage | Target |
| --- | --- |
| Acknowledgement of your report | within 3 working days |
| Initial assessment — accepted, needs more information, or not a vulnerability | within 10 working days |
| Progress updates while a fix is being developed | at least every 14 days |
| Fix released for a critical issue affecting the actuation path | as fast as possible; days, not months |
| Fix released for other accepted issues | within 90 days |
| Public advisory | with the fix release, or at the end of the embargo — see below |

If you have not heard anything within 10 working days, please send a reminder. A dropped report is
far more likely to be an overloaded maintainer than a deliberate silence.

## Disclosure

We follow coordinated disclosure with a **90-day** default embargo, starting from the
acknowledgement of your report.

- We aim to release a fix and publish the advisory well within that window, and will publish
  earlier if a fix is ready earlier.
- If a vulnerability is being actively exploited, or is already public, we will move immediately
  and publish alongside the fix.
- If we need more than 90 days, we will say so and explain why, before the deadline — not after.
  You are under no obligation to agree.

Advisories are published as [GitHub Security Advisories](https://github.com/rakovpublic/jneopallium/security/advisories)
and referenced in the release notes of the release that fixes them, per [RELEASES.md](RELEASES.md).

## Credit

Reporters are credited by name and, if they wish, affiliation in the advisory and the release
notes, unless they ask to remain anonymous. The project has no bug bounty and no funding from which
to pay one; credit and thanks are what we can offer.

## Supported versions

Security fixes are backported as PATCH releases according to the support window in
[RELEASES.md](RELEASES.md):

| Version line | Security fixes |
| --- | --- |
| Latest MINOR of the current MAJOR | Yes |
| Previous MINOR of the current MAJOR | Yes, until the next MINOR ships |
| Previous MAJOR | Yes, for six months after the new MAJOR ships |
| Anything older | No |
| Pre-`1.0.0` and `-SNAPSHOT` builds | No — these carry no compatibility or support promise |

The project has not yet cut its first Maven Central release. Until it does, security fixes land on
`master` and this table describes the policy that takes effect from `1.0.0`.

## Scope

**In scope** — anything that lets an attacker, or a mistake, produce an unsafe or unaccountable
effect:

- bypassing any stage of the universal bridge write algorithm (interlock → override → clamp →
  rate-limit → diff-suppress → audit);
- escalating a bridge past its declared safety ceiling — for example causing a `READ-ONLY`,
  `EXPORT-ONLY`, `ADVISORY` or `SIM-ONLY` bridge to perform a real write;
- defeating the `SHADOW`-mode default, the override registry, or the harm discriminator's veto and
  hard constraints;
- forging, suppressing or tampering with `BridgeAuditRecord` entries;
- remote code execution through the runtime class resolution (`Class.forName`) of user-supplied
  neuron, signal and processor classes, or through model-JAR loading;
- authentication, authorisation, injection or deserialization flaws in the HTTP and gRPC cluster
  transports, or in the master node;
- credential or key leakage through configuration handling, logs or audit output;
- denial of service against a master or worker node that is reachable in a documented deployment.

**Out of scope:**

- vulnerabilities in third-party dependencies — report those upstream; tell us too, and we will
  bump the dependency (Dependabot already tracks them);
- misconfiguration by an operator that the documentation warns against, such as granting a bridge
  `AUTONOMOUS` mode on equipment whose protocol spec declares a lower ceiling;
- the demo suites and simulators under `demos/` and `scripts/`, which are explicitly
  non-production, unless the flaw also exists in the engine or a bridge;
- results from automated scanners with no demonstrated exploit path;
- anything requiring an attacker who already has full control of the host running the worker.

## Operator responsibilities

Jneopallium's safety mechanisms are defence in depth, not a substitute for the safety engineering
of the system it is connected to. If you deploy it against real equipment:

- keep independent, non-software interlocks and emergency stops in the plant, aircraft or device;
- start every new loop in `SHADOW` mode and keep it there until the audit trail shows you what it
  would have done;
- never raise a bridge above the safety ceiling its protocol specification declares;
- retain and monitor the audit records — they are the accountability mechanism, and they only work
  if someone reads them;
- treat model JARs as executable code from a trusted source, because that is what they are.

## Security practices in the project

- Every pull request runs `mvn -B -U clean verify` on JDK 17 in GitHub Actions.
- CodeQL static analysis runs on the repository.
- Dependabot proposes dependency updates; only OSI-licensed dependencies are accepted
  ([CONTRIBUTING.md](CONTRIBUTING.md)).
- Released artifacts are GPG-signed and published under the verified `io.github.rakovpublic`
  namespace ([RELEASES.md](RELEASES.md)).
- Changes to the bridge write path, safety ceilings, audit schema or harm discriminator require
  explicit Maintainer approval and a written rationale ([GOVERNANCE.md](GOVERNANCE.md) §6).
