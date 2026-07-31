# Campaign operations runbook

## Install and initialize

Use Python 3.12 or newer. From the repository root on Windows:

```powershell
cd campaign
py -3.12 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -e ".[dev]"
Copy-Item .env.example .env
.\.venv\Scripts\alembic.exe upgrade head
.\.venv\Scripts\jneo-campaign.exe doctor
```

On Linux or macOS:

```bash
cd campaign
python3.12 -m venv .venv
. .venv/bin/activate
python -m pip install -e '.[dev]'
cp .env.example .env
alembic upgrade head
jneo-campaign doctor
```

The root helpers run one cycle or the continuous loop after the package is installed:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-campaign.ps1 once
powershell -ExecutionPolicy Bypass -File scripts\run-campaign.ps1 continuous
```

```bash
scripts/run-campaign.sh once
scripts/run-campaign.sh continuous
```

## DRY_RUN acceptance and inspection

`jneo-campaign run --once` is the single-command acceptance path. It rebuilds the repository
registry, scores all 60 initial domains, discovers and verifies recorded organizations and official
professional channels, scores prospects, selects offers and demos, creates assets, applies
compliance, queues mock email, simulates representative replies, offers and agrees a meeting,
updates experiments, and writes analytics. No external provider can be called in this mode.

Start the local dashboard with `jneo-campaign dashboard`, then open
`http://127.0.0.1:8765`. Review the domain ranking, top prospects, generated propositions, compliance
decisions, simulated threads, suppressions, meeting, provider failures, and audit trail.

Individual recovery or diagnostic commands are listed in `campaign/README.md`. They are
repeat-safe; for example, running inventory or outreach preparation again updates existing records
rather than duplicating them.

## Google OAuth

Create a Google Cloud Desktop OAuth client, enable Gmail and Calendar APIs, and keep the downloaded
client JSON outside Git. Configure `GOOGLE_OAUTH_CLIENT_FILE`, `GOOGLE_OAUTH_TOKEN_FILE`, and the
optional account identifier in `.env`. Then run:

```bash
jneo-campaign auth gmail
jneo-campaign auth validate
```

The authorization covers Gmail modify/send and Calendar events/free-busy scopes required by the
complete workflow. The token envelope is encrypted. The encryption key comes from the OS keyring or
`CAMPAIGN_TOKEN_ENCRYPTION_KEY` supplied by a deployment secret manager. Access tokens refresh
automatically and the refreshed encrypted envelope is saved. `auth calendar` is an equivalent alias
for the same combined minimum workflow authorization.

## LIVE pilot checklist

Do not enable LIVE until every item below is true:

1. Select one bounded campaign and only US professional B2B recipients under the implemented
   ruleset.
2. Re-verify official contact evidence less than 90 days old and each recipient timezone.
3. Configure truthful sender name/email, organization, postal address, reply address, and escalation
   address.
4. Run `doctor --validate-credentials` and resolve every failure.
5. Inspect every approved proposition, suppression entry, and pause control in the dashboard.
6. Keep the first pilot at 5 new contacts/day and 10 total messages/day. Increase no higher than the
   configured safe ceiling of 10 new/20 total only after a meaningful clean sample.
7. Define stop conditions: any complaint, bounce rate above 5%, unsupported-claim challenge,
   authentication failure, or unexpected provider behavior.
8. Set both `CAMPAIGN_MODE=LIVE` and `CAMPAIGN_LIVE_SEND=true`, then run one cycle. Do not begin with
   the continuous service.

The first recommended live campaign is industrial automation and digital-twin integration. It has
the strongest combination of repository demos, OPC UA/FMI integration evidence, bounded synthetic
evaluation, and a non-medical/non-defence safety profile. Use 20–30 researched organizations, 10–15
fresh official professional contacts, and at least three proposition framings. Ad-fraud is a useful
second pilot but must retain the simulation/advisory claims in `adfraud-campaign.md`.

## Normal operation

After a reviewed one-cycle pilot, run `jneo-campaign run --continuous --interval-minutes 60` under a
service manager. Back up the database, encrypted token envelope, YAML configuration, and reports.
Never put the encryption key in the same backup archive as the token envelope.

At the start of each day, review:

- unresolved provider failures and compliance blocks;
- bounce, complaint, unsubscribe, and reply metrics;
- replies requiring legal, medical, security, pricing, contract, or unsupported-claim escalation;
- proposed calendar options and explicit agreement evidence;
- source age and the remaining daily send limit.

Pause controls are available through `POST /api/pause/{scope}/{value}` for `campaign`, `domain`,
`region`, `organization`, or `sequence`. The payload is `{"paused": true, "reason": "..."}`. A pause
is checked again immediately before sending.

## Failure and recovery

Stage failures are recorded in `provider_failures` and `job_runs`; subsequent independent stages
continue. Correct credentials, configuration, or provider availability and rerun the relevant CLI
command or a complete cycle. Idempotency keys prevent duplicate initial messages.

If bounce or complaint health limits trigger, leave the campaign paused, inspect the underlying
messages and source quality, add suppressions where required, and document the decision before
resuming. Never delete an opt-out merely to restart outreach. If Gmail thread identity cannot be
reconciled, disqualify or manually suppress that recipient rather than starting a new thread.

Calendar events are created only when an inbound campaign reply matches an offered option and
free/busy is rechecked. On a transient calendar failure, rerun `meetings sync`; the deterministic
event key prevents duplicate creation.

## Maintenance and retention

- Refresh contact evidence at least every 90 days before LIVE use.
- Run `alembic upgrade head` for each release.
- Run `ruff check src tests`, `pytest`, and the root Maven test reactor before deployment.
- Run a Python dependency audit in CI, for example `python -m pip install pip-audit` followed by
  `pip-audit`.
- Apply a documented retention period to research excerpts, messages, and audit records. Preserve
  suppressions for as long as needed to honor opt-outs; pseudonymize or delete other contact data at
  expiry.
- PostgreSQL is recommended for persistent production operation. SQLite is appropriate for a
  single local operator, not concurrent multi-process workers.
