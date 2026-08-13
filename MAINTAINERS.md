# Maintainers

This file is the authoritative list of who holds which role in Jneopallium. The rules for gaining
and losing these roles are in [GOVERNANCE.md](GOVERNANCE.md).

## Roles

**Contributor** — anyone who opens an issue, a pull request, or a review. No permission needed;
you are a Contributor the moment you contribute.

**Committer** — holds write access to the repository. Approves and merges pull requests in an area
of the codebase they know. Expected to review others' work, not only land their own.

**Maintainer** — everything a Committer does, plus: cuts releases, holds the signing key, manages
repository settings and secrets, handles security reports, adds and removes Committers, and speaks
for the project externally. Maintainers are accountable for the project's safety-critical
invariants (see [GOVERNANCE.md](GOVERNANCE.md) §6).

## Current Maintainers

| Name | GitHub | Email | Area | Since |
| --- | --- | --- | --- | --- |
| Dmytro Rakovskyi | [@rakovpublic](https://github.com/rakovpublic) | rakovpublic@gmail.com | All — engine, cluster, domains, bridges, releases, security | June 2018 |

## Current Committers

None yet, beyond the Maintainer above.

**The project is actively looking for Committers.** Jneopallium is a single-maintainer project and
that is the main thing it needs to change. If you have been contributing and want a larger role,
say so in an issue — you do not have to wait to be noticed. Areas where help is most useful right
now:

- **Bridges** — implementing the specified-but-not-yet-built adapters (DICOM, HL7 FHIR, Lab
  Streaming Layer, LTI/xAPI, Kafka, OpenTelemetry), or hardening the shipped ones against real
  protocol stacks.
- **Engine** — the gRPC cluster mode, the Kafka input source, and scheduler performance.
- **Domains** — new domain modules, or deepening the existing fourteen.
- **Build and release** — Maven Central publishing, reproducible builds, CI, Docker/Kubernetes
  packaging.
- **Documentation and examples** — the fastest way into the project, and the most immediately
  valuable to new users.

## Emeritus

None.

Emeritus Maintainers and Committers are people who stepped back after significant contribution.
The status is honourable and reversible — see [GOVERNANCE.md](GOVERNANCE.md) §4.

## Reaching the maintainers

- **General questions and help** — see [SUPPORT.md](SUPPORT.md).
- **Bugs and features** — [GitHub Issues](https://github.com/rakovpublic/jneopallium/issues).
- **Security vulnerabilities** — do **not** open a public issue. Follow
  [SECURITY.md](SECURITY.md).
- **Code of Conduct concerns** — see [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).
