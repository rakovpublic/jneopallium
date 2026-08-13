# OpenSSF Best Practices Badge — self-assessment worksheet

LF AI & Data requires a **Passing** [OpenSSF Best Practices Badge](https://www.bestpractices.dev/)
for Sandbox projects. The badge is earned by filling in a self-assessment questionnaire on
bestpractices.dev, not by anything in this repository — but most of the answers are facts about
this repository, so they are collected here once.

**Status:** not yet registered. Registration is an account action only the project owner can
perform.

## How to register

1. Sign in at <https://www.bestpractices.dev/> with the GitHub account that owns the repository.
2. Add a new project with the URL `https://github.com/rakovpublic/jneopallium`.
3. Work through the questionnaire using the answers below.
4. When the Passing level is reached, add the badge to `README.md`:

   ```markdown
   [![OpenSSF Best Practices](https://www.bestpractices.dev/projects/<ID>/badge)](https://www.bestpractices.dev/projects/<ID>)
   ```

   Do not add the badge before it is earned.

## Answers — Passing level

Criteria are grouped as they appear in the questionnaire. `Met` means the evidence already exists;
`To do` means an action is outstanding.

### Basics

| Criterion | Answer | Evidence |
| --- | --- | --- |
| Project website describes what it does | Met | [`README.md`](../README.md) |
| Website states how to get help | Met | [`SUPPORT.md`](../SUPPORT.md) |
| Website states how to contribute | Met | [`CONTRIBUTING.md`](../CONTRIBUTING.md) |
| Contribution requirements documented | Met | `CONTRIBUTING.md` — PR expectations, tests, DCO, dependency policy |
| English used for interaction | Met | Repository, issues and docs are in English |
| Maintained (not archived, responsive) | Met | ~700 commits since 2018; active development |

### Change control

| Criterion | Answer | Evidence |
| --- | --- | --- |
| Public version-controlled source repository | Met | <https://github.com/rakovpublic/jneopallium> (GitLab mirror) |
| Interim versions available between releases | Met | `master` is public and always releasable |
| Uses a common distributed VCS | Met | git |
| Unique version numbering | Met | Semantic Versioning, single reactor-wide version — [`RELEASES.md`](../RELEASES.md) |
| Release notes for each release | To do | Policy defined in `RELEASES.md`; produced from the first Maven Central release onward |
| Release notes identify fixed vulnerabilities | Met (policy) | `RELEASES.md` § Release notes; `SECURITY.md` § Disclosure |

### Reporting

| Criterion | Answer | Evidence |
| --- | --- | --- |
| Process for bug reports documented | Met | `SUPPORT.md`; issue templates in `.github/ISSUE_TEMPLATE/` |
| Bug reports acknowledged | Met (policy) | `SUPPORT.md` § Response times — 2 weeks |
| Archive of reports available | Met | GitHub Issues |
| Vulnerability report process documented | Met | [`SECURITY.md`](../SECURITY.md) |
| Private vulnerability reporting supported | Met | GitHub private advisories + maintainer email |
| Vulnerability reports acknowledged within 14 days | Met (policy) | `SECURITY.md` — 3 working days |

### Quality

| Criterion | Answer | Evidence |
| --- | --- | --- |
| Working build system | Met | Maven 3.9+, 47-module reactor, `mvn clean install` |
| Build uses common tools | Met | Maven, JDK 17 (Temurin) |
| Build is reproducible from source | Met | Public repository, pinned dependency versions in the root `pom.xml` |
| Automated test suite | Met | JUnit 5 + Mockito; 127 test classes; `integration-tests` module |
| Test suite invocable by a standard command | Met | `mvn -B -U clean verify` |
| Tests run on every change | Met | `.github/workflows/maven-verify.yml` on every push and PR |
| Policy that new functionality adds tests | Met | `CONTRIBUTING.md` § Making a change, step 3 |
| Evidence the policy is followed | Met | Tests accompany feature modules across the reactor |
| Compiler warnings addressed | Partial | Javadoc `doclint` is currently disabled for the release profile — see `docs/publishing-to-maven-central.md` § 6. Re-enabling it is tracked work. |

### Security

| Criterion | Answer | Evidence |
| --- | --- | --- |
| At least one primary developer knows secure design | Met | Bridge safety architecture: universal write algorithm, safety ceilings, `SHADOW` default, audit trail |
| At least one knows common implementation vulnerabilities | Met | Scope list in `SECURITY.md`; CodeQL enabled |
| Uses standard cryptography, no bespoke algorithms | Met | GPG for release signing; TLS via the JDK and gRPC; no custom crypto implemented |
| Publicly documented process for reporting vulnerabilities | Met | `SECURITY.md` |
| Vulnerabilities fixed within 60 days of report | Met (policy) | `SECURITY.md` — critical actuation-path issues in days; others within 90 days. **Note:** the badge asks for 60 days for *publicly known* vulnerabilities; `SECURITY.md`'s 90-day figure is the coordinated-disclosure embargo, and publicly known issues are handled immediately. |
| No unpatched medium+ vulnerabilities older than 60 days | To confirm | Verify against the Dependabot alert list before submitting |

### Analysis

| Criterion | Answer | Evidence |
| --- | --- | --- |
| Static analysis applied before each release | Met | CodeQL workflow (`.github/workflows/codeql.yml`) |
| Static analysis covers common vulnerabilities | Met | CodeQL Java queries |
| Static analysis findings fixed | Met (policy) | Findings triaged before release per `RELEASES.md` |
| Dependency vulnerability monitoring | Met | Dependabot |
| Dynamic analysis | Optional at Passing | Demo suites produce deterministic traces used as behavioural regression evidence |

### Documentation

| Criterion | Answer | Evidence |
| --- | --- | --- |
| Basic user documentation | Met | `README.md`, `doc/`, `docs/`, nine runnable demo suites |
| Reference documentation of the external interface | Partial | Core abstractions table in `README.md`; per-bridge specs in `doc/`. Hosted Javadoc is on the roadmap. |

### Licensing

| Criterion | Answer | Evidence |
| --- | --- | --- |
| OSI-approved license | Met | BSD 3-Clause — [`LICENSE.MD`](../LICENSE.MD) |
| License in a standard location | Met | Repository root |
| FLOSS dependencies only | Met | `CONTRIBUTING.md` forbids proprietary dependencies; dependency licenses enumerated in the LF AI proposal |

### Other

| Criterion | Answer | Evidence |
| --- | --- | --- |
| Delivered over HTTPS | Met | GitHub, GitLab, Maven Central |
| Signed releases | Met | GPG-signed artifacts — `RELEASES.md` § Artifact integrity |
| Two-factor authentication on accounts with write access | To do | Enforce on the GitHub account and any future organization |
| Code of Conduct | Met | [`CODE_OF_CONDUCT.md`](../CODE_OF_CONDUCT.md) — Contributor Covenant |
| Governance documented | Met | [`GOVERNANCE.md`](../GOVERNANCE.md), [`MAINTAINERS.md`](../MAINTAINERS.md) |
| Roles documented | Met | `MAINTAINERS.md` |

## Outstanding actions

These are the only items standing between the repository as it is and a Passing badge. All but the
first are account or process actions, not code:

1. **Register the project** on bestpractices.dev and complete the questionnaire.
2. **Enable two-factor authentication** on every account with write access.
3. **Confirm no unpatched medium-or-higher dependency vulnerabilities** are outstanding (check
   Dependabot alerts).
4. **Cut the first release with release notes**, which closes the release-notes criterion.
5. **Add the badge to `README.md`** once earned.

## Beyond Passing

Incubation requires **Silver** and Graduation requires **Gold**. The main additional demands, worth
knowing about early because they shape ordinary work:

- **Silver** — a documented project governance model (done), a bus factor greater than one
  (the binding constraint: the project needs more Maintainers), documented architecture, a
  documented security assurance case, a test suite with measured statement coverage of at least
  80%, and signed releases (done).
- **Gold** — at least two unassociated significant contributors, all changes reviewed by someone
  other than the author before merge, at least 90% statement coverage, and continuous integration
  with automated dynamic analysis.

Both levels are gated on community growth rather than on repository hygiene, which is the reason
the project is applying to LF AI & Data at the Sandbox stage.
