# Contributing to Jneopallium

Contributors are genuinely welcome — this is a single-maintainer project actively looking for
help. You do not need permission to start, and you do not need to be a neuroscientist.

By participating you agree to the [Code of Conduct](CODE_OF_CONDUCT.md).

## Quick start

```bash
git clone https://github.com/rakovpublic/jneopallium.git
cd jneopallium
mvn clean install
```

You need **Java 17 (LTS)** and **Maven 3.9+**. The build is a 47-module reactor; a full
`clean install` takes a while the first time.

Run the tests the way CI does:

```bash
mvn -B -U clean verify
```

## Where to start

- **Good first contributions** — documentation fixes, new demo scenarios, test coverage for an
  existing domain module, or implementing one of the specified-but-not-yet-built bridges.
- **Areas needing help** are listed in [MAINTAINERS.md](MAINTAINERS.md).
- **What is planned** is in the [Roadmap](README.md#roadmap) and [WorkDiary.md](WorkDiary.md).
- **Where things live** — the module map and the reasoning behind it are in
  [docs/refactor/worker-modularization-plan.md](docs/refactor/worker-modularization-plan.md).

For anything larger than a bug fix, **open an issue first** and describe what you intend to do.
This is not bureaucracy — it avoids you writing something that conflicts with work already in
progress, and it lets the design be discussed while changing it is still cheap.

## Making a change

1. **Fork** the repository and create a branch off `master`. Branch names are of the form
   `feat/<short-description>`, `fix/<short-description>`, `docs/<short-description>` or
   `chore/<short-description>`.
2. **Write the change**, matching the style of the code around it.
3. **Add tests.** Unit tests live in each module's `src/test`; tests spanning modules go in the
   `integration-tests` module. A bug fix should come with a test that fails without it.
4. **Run `mvn -B -U clean verify`** and make sure it passes before opening the pull request.
5. **Sign off every commit** — see [Developer Certificate of Origin](#developer-certificate-of-origin)
   below. This is mandatory.
6. **Open a pull request** against `master`.

### Pull request expectations

Every pull request must:

- **describe the problem it solves**, not only what it changes. If it fixes an issue, link it;
- be scoped to one thing — unrelated changes belong in separate pull requests;
- keep the build green: `mvn -B -U clean verify` on JDK 17, plus CodeQL;
- update the documentation it invalidates, in the same pull request;
- carry a DCO sign-off on every commit.

Reviews aim for a first response within two weeks; see [SUPPORT.md](SUPPORT.md). A polite nudge
after that is fine.

How a pull request gets merged — lazy consensus, who approves, and what happens when there is
disagreement — is described in [GOVERNANCE.md](GOVERNANCE.md) §6.

## Developer Certificate of Origin

Jneopallium requires a **DCO sign-off** on every commit. This certifies that you wrote the change,
or otherwise have the right to submit it under the project's license. The full text is at
<https://developercertificate.org/>.

Add the sign-off with `-s`:

```bash
git commit -s -m "Fix rate limiter overflow in the MQTT bridge"
```

which appends:

```
Signed-off-by: Your Name <your.email@example.com>
```

The name and email must be real and must match the commit author. To fix commits you already made:

```bash
git rebase --signoff HEAD~3
```

## Licensing and dependencies

Jneopallium is licensed under the [BSD 3-Clause License](LICENSE.MD), and your contributions are
accepted under that license.

**Proprietary code and proprietary dependencies are not accepted.** Do not submit code you cannot
license under BSD-3-Clause, code copied from a source with an incompatible license, or code
generated from material you do not have the rights to.

**Adding or changing a runtime dependency requires prior agreement** — open an issue before writing
the code. New dependencies must be under an OSI-approved license, be actively maintained, and be
pinned centrally in the root `pom.xml` where the version properties live. Optional per-bridge
dependencies belong only in the `bridge-*` module that needs them, never in `worker-core`.

## Code conventions

- **Java 17**, compiled with `<release>17</release>`. Do not use APIs newer than the baseline.
- **Match the surrounding code** — its naming, its comment density, its idiom. Consistency with the
  neighbours beats personal preference.
- **`worker-core` stays domain-free.** The engine resolves neuron, signal and processor classes at
  runtime by fully-qualified name, so it must carry no compile-time dependency on any `domain-*`
  module. Likewise `bridge-api` programs against the neutral control contracts in
  `worker.net.signals.control.*` and depends on no domain.
- **New domain code** goes in a `domains/domain-<id>/` module; **new bridges** go in a
  `bridges/bridge-<id>/` module, package `worker.bridge.<id>`.
- **Logging** through SLF4J, backed by Log4j 2. Never log credentials, tokens, or raw payloads that
  might contain them.

## Contributing a bridge

Bridges are the part of the project that touches real equipment, so they carry extra rules. Read
`00-FRAMEWORK.md` and the per-protocol specs in [`doc/`](doc) before starting. Every new bridge
must:

- live in its own `bridges/bridge-<id>/` module, depending on `bridge-api` plus only the `domain-*`
  modules whose signals it maps;
- extend `AbstractBridgeOutputAggregator` so writes go through the universal §2.2 write algorithm —
  interlock → override → clamp → rate-limit → diff-suppress → audit. **Do not reimplement it, and
  do not route around it.**;
- emit a `BridgeAuditRecord` for every write, whether it was applied, clamped or rejected;
- support the override registry hooks;
- declare a **safety ceiling** appropriate to the protocol, enforced both at configuration load and
  at runtime;
- **start every loop in `SHADOW` mode**;
- ship with tests and a specification document in `doc/`.

Changes that weaken a safety ceiling, remove an audit point, or let a write bypass the universal
algorithm require explicit Maintainer approval and a written rationale in the pull request —
[GOVERNANCE.md](GOVERNANCE.md) §6. Expect these to be reviewed slowly and sceptically. That is not
distrust of you; it is the nature of code that can move a valve.

## Security issues

**Do not report vulnerabilities through issues or pull requests.** Follow
[SECURITY.md](SECURITY.md) for private reporting.

## Getting help

See [SUPPORT.md](SUPPORT.md). Questions asked as GitHub issues are welcome.

## Project roles

Contributor, Committer and Maintainer are defined in [MAINTAINERS.md](MAINTAINERS.md); how you move
between them is in [GOVERNANCE.md](GOVERNANCE.md). If you have been contributing and want a larger
role, say so — you do not have to wait to be noticed.
