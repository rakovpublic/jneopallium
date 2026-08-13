# Getting help with Jneopallium

Jneopallium is maintained by a very small team — one active maintainer at the time of writing.
Help is given gladly, but not instantly. Setting expectations honestly up front is better than
leaving you wondering.

## Start with the documentation

Most questions are answered by something already in the repository:

| Question | Where to look |
| --- | --- |
| What is this and how do I build it? | [README.md](README.md) |
| How do I define signals, neurons and processors? | [README.md § Modelling Process](README.md#modelling-process) |
| How do the fast and slow loops schedule work? | [README.md § Signal Processing Frequency](README.md#signal-processing-frequency) |
| How do I connect to a real device or protocol? | `00-FRAMEWORK.md` and the per-protocol specs in [`doc/`](doc) |
| What does a working end-to-end model look like? | [README.md § Demos](README.md#demos), and the runnable suites under `scripts/` |
| How do I release / publish artifacts? | [RELEASES.md](RELEASES.md), [docs/publishing-to-maven-central.md](docs/publishing-to-maven-central.md) |
| What is planned next? | [README.md § Roadmap](README.md#roadmap), [WorkDiary.md](WorkDiary.md) |

A complete worked example — four signal types, three neuron types, full configuration — lives on
the [`test/alfaTestAndGettingStarted`](https://github.com/rakovpublic/jneopallium/tree/test/alfaTestAndGettingStarted)
branch.

## Where to ask

**[GitHub Issues](https://github.com/rakovpublic/jneopallium/issues)** — the right place for
everything public: bugs, feature requests, and usage questions. Issue templates for bug reports and
feature requests are provided. There is no separate forum; a question filed as an issue is welcome
and is not a misuse of the tracker.

**Email the maintainer** (rakovpublic@gmail.com) for anything that does not belong in public — but
please prefer an issue where you can, so the answer helps the next person too.

The project does not yet have a mailing list or chat channel. Adding both is a priority; this file
will be updated when they exist.

## Reporting a security vulnerability

**Do not open a public issue.** Follow [SECURITY.md](SECURITY.md) — it describes private
reporting, what to include, and what response times to expect.

This matters more than usual here: Jneopallium can drive industrial, clinical and aerial equipment
through its bridges. A flaw in the write path, the safety ceilings or the audit trail is not
theoretical.

## Writing a good report

The more of this you include, the faster you get a useful answer:

- **Version** — release version or commit SHA, and which modules you are using.
- **Environment** — JDK vendor and version (the project targets Java 17 LTS), OS, Maven version.
- **Deployment mode** — local, HTTP cluster, or gRPC cluster.
- **What you expected and what happened**, with the exact error and stack trace as text rather
  than a screenshot.
- **A minimal reproduction** — the smallest structure file, configuration and model classes that
  show the problem. This is the single biggest factor in how quickly something gets fixed.
- **For bridge issues** — the protocol, the bridge's configured safety mode, the relevant audit
  records, and whether the write was applied, clamped or rejected.
- **For demo issues** — the demo name, the scenario, and the generated artifacts under `target/`
  (`manifest.json` first).

## Response times

These are goals from a volunteer project, not a contractual commitment:

| | Target |
| --- | --- |
| Security reports | Acknowledged within 3 working days — see [SECURITY.md](SECURITY.md) |
| Bug reports | First response within 2 weeks |
| Feature requests and questions | First response within 2 weeks; may sit open much longer |
| Pull requests | First review within 2 weeks |

If something has gone quiet for longer than that, a polite comment on the issue is a reasonable
nudge and will not annoy anyone.

## Commercial support

There is none. No organization currently offers paid support, training or consulting for
Jneopallium, and the project has no funding or sponsorship.

## Helping out

Jneopallium is actively looking for contributors, and the fastest way to get something you need is
often to build it. [CONTRIBUTING.md](CONTRIBUTING.md) explains how to start;
[MAINTAINERS.md](MAINTAINERS.md) lists the areas where help is most useful.
