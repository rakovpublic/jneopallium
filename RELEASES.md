# Releases

How Jneopallium is versioned, released, supported and deprecated.

The mechanical steps for publishing to Maven Central — account setup, namespace verification, GPG
key handling, portal tokens — are in [docs/publishing-to-maven-central.md](docs/publishing-to-maven-central.md).
This document covers policy: what a version number promises, when releases happen, and how long
they are supported.

## Versioning

Jneopallium follows [Semantic Versioning 2.0.0](https://semver.org/) — `MAJOR.MINOR.PATCH`.

All 47 modules share a single version, inherited from the aggregator POM
(`io.github.rakovpublic.jneopallium:wrapper`). Module versions are never bumped independently; a
release of one module is a release of all of them. This keeps cross-module compatibility trivially
verifiable at the cost of some version churn in modules that did not change.

**The public API** — what the compatibility promise covers — is:

- the neuron, signal and processor contracts in `worker-core`
  (`INeuron`, `ISignal`, `ISignalProcessor`, `ISignalChain`, `Neuron`, `Axon`, `Dendrites`,
  `IInitInput`, `IOutputAggregator`, `INeuronNetInput`, `IConnectionGenerator`, and the
  `worker.net.signals.control.*` neutral control contracts);
- the bridge SPI in `bridge-api` (`AbstractBridgeOutputAggregator`, `OverrideRegistry`,
  `AbstractBridgeAuditOutput`, `BridgeReconnectPolicy`, `BridgeAuditRecord`,
  `BridgeBindingDirection`);
- the shared neuromodulatory base types in `agi-base`;
- the on-disk formats: the neuron-network structure file, the configuration file, and the
  bridge audit record schema;
- the worker entrypoint's command line
  (`com.rakovpublic.jneuropallium.worker.application.Entry`).

Everything else — `domain-*` internals, `bridge-*` implementation classes, `demos`,
`integration-tests`, anything in a package containing `.internal.` — is **not** covered and may
change in a minor release.

| Change | Bump |
| --- | --- |
| Breaking change to any public API or on-disk format above | MAJOR |
| New capability, new module, new bridge, deprecation, backwards-compatible API addition | MINOR |
| Bug fix, dependency update, documentation, performance work with no API change | PATCH |

Two rules that override the table:

- **A change that weakens a safety invariant is never a PATCH.** Altering the universal write
  algorithm, raising a protocol's declared safety ceiling, removing an audit point, changing the
  `SHADOW`-mode default, or relaxing a harm-discriminator hard constraint is at minimum a MINOR
  release, is called out explicitly in the release notes, and requires the Maintainer approval
  described in [GOVERNANCE.md](GOVERNANCE.md) §6.
- **Security fixes are backported as PATCH releases** to every supported line, even when the fix
  itself would otherwise warrant a larger bump.

`0.x` and pre-`1.0.0` snapshots carry no compatibility promise.

## Cadence

Jneopallium releases when there is something worth releasing, not on a calendar.

- **PATCH** — as needed; always immediately for a security fix.
- **MINOR** — roughly quarterly, when a meaningful group of changes has landed and been exercised
  by the demo suites.
- **MAJOR** — rarely, and never without a deprecation cycle (see below) and an announced migration
  path.

`master` is always releasable: CI must be green, and `mvn -B -U clean verify` on JDK 17 must pass
for every pull request before merge.

## Support window

| Line | Supported |
| --- | --- |
| Latest MINOR of the current MAJOR | Yes — bug fixes and security fixes |
| Previous MINOR of the current MAJOR | Security fixes only, until the next MINOR ships |
| Previous MAJOR | Security fixes for six months after the new MAJOR ships |
| Anything older | No |

The supported-version table in [SECURITY.md](SECURITY.md) is kept in sync with this and is the one
to check when reporting a vulnerability.

Java baseline: **Java 17 (LTS)**. Raising the baseline is a MAJOR change and will be announced at
least one MINOR release in advance.

## Deprecation policy

Nothing in the public API is removed without warning.

1. The API is marked `@Deprecated`, with `@deprecated` Javadoc naming the replacement, in release
   *N*.
2. It keeps working, unchanged, for at least two MINOR releases *and* at least six months.
3. It is removed no earlier than the next MAJOR release.

Deprecations are listed in the release notes of the release that introduces them and again in the
release that removes them. Configuration keys and on-disk format fields follow the same rules:
a removed key is accepted-and-warned-about for the deprecation window, never silently ignored.

## Cutting a release

Releases are cut by a Maintainer from `master`.

1. **Confirm the tree is ready** — CI green on `master`, no open issue labelled
   `release-blocker`, and the demo suites run clean:

   ```bash
   mvn -B -U clean verify
   ```

2. **Set the release version across the reactor:**

   ```bash
   mvn versions:set -DnewVersion=1.0.0 -DprocessAllModules=true
   mvn versions:commit
   ```

3. **Draft the release notes** — assemble them from the merged pull requests since the previous
   tag, using the structure under [Release notes](#release-notes) below. Do this before publishing,
   not after: it is the last chance to notice an undocumented breaking or safety-relevant change.

4. **Build, sign and upload:**

   ```bash
   export MAVEN_GPG_PASSPHRASE='...'
   mvn -Prelease clean deploy
   ```

   The `release` profile adds the source jar, the Javadoc jar, GPG signatures and the Central
   upload. With `autoPublish=false` the deployment waits in the portal as *validated* until it is
   published by hand — nothing is public until a Maintainer presses Publish.

5. **Verify, then publish** in the [Central portal](https://central.sonatype.com). A validated
   deployment can be dropped and re-uploaded under the same version; a *published* one can never be
   changed or removed. Check before publishing.

6. **Tag:**

   ```bash
   git tag -a v1.0.0 -m "1.0.0"
   git push origin v1.0.0
   ```

7. **Publish release notes** as a GitHub Release against the tag (see below).

8. **Return to development:**

   ```bash
   mvn versions:set -DnewVersion=1.0.1-SNAPSHOT -DprocessAllModules=true
   mvn versions:commit
   ```

## Release notes

Every release gets a GitHub Release on its tag, containing:

- **Breaking changes** first, with migration instructions — empty for MINOR and PATCH releases;
- **Safety-relevant changes** — anything touching the write algorithm, safety ceilings, audit
  records, override handling or the harm discriminator, whether or not it is breaking;
- **Security fixes**, with CVE identifiers and advisory links where they exist;
- new features, bug fixes, and dependency updates;
- deprecations introduced and removals performed;
- the Java baseline, if it changed.

## What is published

| Published to Maven Central | Not published |
| --- | --- |
| `wrapper` (root POM), `worker-core`, `agi-base`, `bridge-api`, `master` | `demos` and all `demo-*` modules |
| `domains` and the 14 `domain-*` modules | `integration-tests` |
| `bridges` and the 16 `bridge-*` modules | |

Aggregator POMs are published because modules inherit from them — `worker-core` cannot be resolved
without `wrapper`. Demos and integration tests set `maven.deploy.skip` inside the `release` profile
only, so they still reach the GitLab package registry.

## Artifact integrity

- Every published artifact is GPG-signed; the public key is on `keyserver.ubuntu.com`.
- The signing key is held by a Maintainer and is not present in the repository or in CI secrets.
- Artifacts are published under the verified namespace `io.github.rakovpublic`, as
  `io.github.rakovpublic.jneopallium`.
- A published version is never re-published or overwritten. A bad release is superseded by the next
  PATCH.

## Snapshots

`-SNAPSHOT` builds are published to the GitLab package registry for development use. They carry no
compatibility promise and are not mirrored to Maven Central. Do not depend on a snapshot in
anything you ship.
