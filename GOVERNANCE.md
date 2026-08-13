# Jneopallium Governance

This document describes how the Jneopallium project is governed: who decides what, how those
people are chosen, and how disagreements are resolved.

It is deliberately written to work at the project's current size — one active maintainer — while
describing the structure the project grows into as more contributors arrive. Nothing here assumes
a headcount the project does not have.

## 1. Principles

- **Open by default.** Technical decisions are made in public, in GitHub issues and pull requests.
  Anything decided elsewhere is summarised back into an issue before it takes effect.
- **Merit, not affiliation.** Influence follows sustained, useful contribution. Employer,
  nationality and seniority are irrelevant.
- **No proprietary dependencies.** Jneopallium does not accept proprietary code, or dependencies
  under licenses that are not OSI-approved. See [CONTRIBUTING.md](CONTRIBUTING.md).
- **Safety-critical code gets stricter review.** Changes to the bridge write path, the safety
  ceilings, the audit trail, or the harm discriminator are held to a higher bar than the rest of
  the codebase. See §6.

## 2. Roles

Roles are defined in [MAINTAINERS.md](MAINTAINERS.md), which is the authoritative list of who
currently holds each one. In summary:

| Role | Can | Becomes one by |
| --- | --- | --- |
| **Contributor** | Open issues, open pull requests, review, participate in discussion | Contributing anything |
| **Committer** | Everything a Contributor can, plus approve and merge pull requests in an area of the codebase | Nomination and Maintainer approval — see §3 |
| **Maintainer** | Everything a Committer can, plus release, manage repository settings, add and remove Committers, and represent the project externally | Nomination and Maintainer approval — see §3 |

Anyone may act as a Contributor without asking permission.

## 3. Becoming a Committer or Maintainer

**Committer.** Any Maintainer or Committer may nominate a Contributor by opening an issue titled
`Committer nomination: <name>`. Expected evidence, as a guide rather than a checklist:

- a sustained contribution history — roughly ten or more merged, non-trivial pull requests over at
  least three months;
- demonstrated review judgement, including on other people's changes;
- familiarity with an identifiable area of the codebase (a domain module, a bridge, the engine,
  the build);
- adherence to [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

The nomination stays open for at least one week. It carries if a majority of Maintainers approve
and no Maintainer objects. An objection must state a reason and a path to resolving it.

**Maintainer.** Same process, nominated only by an existing Maintainer, with a two-week window and
a higher bar: sustained Committer activity for at least six months, plus willingness to take on
release duties and security response.

**While the project has a single Maintainer**, that Maintainer decides nominations directly, and
records the decision and its reasoning in the nomination issue. This clause expires automatically
once there are three or more Maintainers.

## 4. Losing a role

- **Voluntarily.** Open a pull request moving yourself to the Emeritus section of
  [MAINTAINERS.md](MAINTAINERS.md). No discussion needed; thank you for your work.
- **Through inactivity.** A Committer or Maintainer with no contribution and no review activity for
  twelve months is moved to Emeritus. The move is announced in an issue first, with two weeks to
  object; objecting is enough to stay.
- **For cause.** Sustained Code of Conduct violations, or repeatedly merging changes against the
  process in this document, can result in removal by a two-thirds vote of the remaining
  Maintainers. Conduct matters are handled under [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) first.

Emeritus status is honourable and reversible: a returning Emeritus Committer or Maintainer is
restored on request by any Maintainer, without a new nomination.

## 5. Technical Steering Committee

Once the project has three or more Maintainers from at least two organizations, they constitute
the **Technical Steering Committee (TSC)**. The TSC:

- owns the technical roadmap and the release schedule;
- decides anything that cannot be resolved at pull-request level (§6);
- elects a **TSC Chair** annually from among its members, by simple majority;
- meets publicly, with an agenda posted in advance and notes committed to the repository.

No single organization may hold more than one third of TSC seats once the TSC has six or more
members; if an acquisition or job change breaks that rule, the affected members decide among
themselves who steps down, and the TSC picks by lot if they cannot agree.

Until the TSC exists, its powers rest with the Maintainers listed in
[MAINTAINERS.md](MAINTAINERS.md).

## 6. How decisions are made

**Default: lazy consensus.** A pull request may be merged when it has at least one approving review
from a Committer other than its author, CI is green, and no Committer or Maintainer has requested
changes. Silence is agreement.

**Single-maintainer exception.** While the project has one Maintainer, that Maintainer may merge
their own changes after CI passes. Such changes are still opened as pull requests, so the reasoning
is on the record and reviewable after the fact.

**Escalation.** If lazy consensus fails — a Committer objects and the objection is not resolved in
review — the change is escalated to an issue tagged `decision`. The TSC (or, before it exists, the
Maintainers) resolves it, preferring consensus and falling back to a simple majority vote of TSC
members. Votes are cast in public in the issue. The Chair breaks ties.

**Changes needing broader agreement**, regardless of who wrote them:

- anything altering the universal bridge write algorithm (interlock → override → clamp →
  rate-limit → diff-suppress → audit), the `SHADOW`-mode default, a protocol's declared safety
  ceiling, the audit record schema, or the harm discriminator's hard constraints;
- adding, removing or changing a runtime dependency;
- backwards-incompatible changes to public API in `worker-core`, `bridge-api` or `agi-base`;
- changes to this document, [MAINTAINERS.md](MAINTAINERS.md), [RELEASES.md](RELEASES.md),
  [SECURITY.md](SECURITY.md) or [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

These require two Committer approvals where two are available, and are announced in an issue before
merge. **A change that weakens a safety ceiling, removes an audit point, or lets a write bypass the
universal algorithm requires explicit Maintainer approval and a written rationale in the pull
request, without exception.**

## 7. Security decisions

Vulnerability reports are handled privately by the Maintainers under [SECURITY.md](SECURITY.md),
outside the normal public process, until a fix is released. Embargoed fixes may be merged without
public discussion; the reasoning is published with the advisory.

## 8. Code of Conduct

All participation is subject to [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md). Enforcement is a
Maintainer responsibility. Reports about a Maintainer go to the other Maintainers, or — if the
project is under Linux Foundation hosting — to `conduct@lfai.foundation`, so no one adjudicates a
report about themselves.

## 9. Amending this document

Changes to this document follow §6's broader-agreement rule: a pull request, announced in an issue,
open at least two weeks, approved by a majority of Maintainers with no Maintainer objecting.

## 10. Foundation relationship

Jneopallium has applied to the LF AI & Data Foundation as a Sandbox project. If accepted, the
project's Technical Charter and the Foundation's policies take precedence over this document
wherever the two conflict, and this document will be revised to match rather than left
contradictory.
