# Jneopallium campaign automation

This directory contains a separate Python 3.12+ application for evidence-grounded market research,
prospect discovery, proposition generation, compliance-gated Gmail outreach, recipient-agreed Google
Calendar scheduling, reply processing, follow-ups, experiments, and analytics. It does not modify or
run inside the Jneopallium neuron-network runtime.

`DRY_RUN` is the default. It performs the complete workflow using recorded official-site research,
deterministic templates, mock Gmail, mock Calendar, simulated replies, and a local SQLite database.
External Gmail or Calendar writes require both `CAMPAIGN_MODE=LIVE` and
`CAMPAIGN_LIVE_SEND=true`, valid OAuth credentials, complete sender identity, a supported legal
ruleset, fresh public contact evidence, and a verified recipient timezone.

## Quick start

Python 3.12 or newer is required.

### Windows PowerShell

```powershell
cd campaign
py -3.12 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -e ".[dev]"
Copy-Item .env.example .env
jneo-campaign doctor
jneo-campaign run --once
jneo-campaign dashboard
```

If PowerShell activation is disabled, invoke the interpreter directly:

```powershell
.\.venv\Scripts\python.exe -m pip install -e ".[dev]"
.\.venv\Scripts\jneo-campaign.exe run --once
```

From the repository root, after installation:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-campaign.ps1 once
```

### Linux/macOS

```bash
cd campaign
python3.12 -m venv .venv
. .venv/bin/activate
python -m pip install -e '.[dev]'
cp .env.example .env
jneo-campaign doctor
jneo-campaign run --once
jneo-campaign dashboard
```

Repository-root convenience command:

```bash
scripts/run-campaign.sh once
```

## Commands

```text
jneo-campaign doctor [--validate-credentials]
jneo-campaign auth gmail
jneo-campaign auth calendar
jneo-campaign auth validate
jneo-campaign inventory build
jneo-campaign domains research
jneo-campaign prospects discover
jneo-campaign prospects verify
jneo-campaign offers generate
jneo-campaign demos match
jneo-campaign outreach prepare
jneo-campaign outreach send
jneo-campaign inbox sync
jneo-campaign followups run
jneo-campaign meetings sync
jneo-campaign analytics report
jneo-campaign run --once
jneo-campaign run --continuous --interval-minutes 60
jneo-campaign dashboard
```

The complete acceptance path is `jneo-campaign run --once`. The continuous command schedules the
same repeat-safe stages and persists each job run and failure. A failed stage is retried with
exponential backoff and recorded without terminating later independent stages.

## Configuration

Copy `.env.example` to `.env`. YAML files control domain weights, campaigns, limits, regions,
offers, demo matching, and compliance:

- `config/domains.yml`
- `config/campaigns.yml`
- `config/compliance.yml`
- `config/offers.yml`
- `config/demo-matching.yml`

The database defaults to SQLite at `campaign/campaign.db`. Set `CAMPAIGN_DATABASE_URL` to a
PostgreSQL SQLAlchemy URL and install `.[postgres]` for persistent deployment. Initialize or upgrade
with:

```bash
alembic upgrade head
```

Reports are generated under `campaign/reports/`; the public domain ranking is also refreshed at
`docs/domain-research-report.md`.

## Google OAuth setup

1. Create or select a Google Cloud project owned by the campaign operator.
2. Enable the Gmail API and Google Calendar API.
3. Configure the OAuth consent screen for the intended test/production account.
4. Create an OAuth 2.0 Desktop application credential and download its JSON file outside version
   control, for example `campaign/secrets/google-oauth-client.json`.
5. Set `GOOGLE_OAUTH_CLIENT_FILE`, `GOOGLE_OAUTH_TOKEN_FILE`, and optionally
   `GOOGLE_OAUTH_ACCOUNT` in `.env`.
6. Run `jneo-campaign auth gmail`. The Gmail and Calendar aliases request only the complete
   campaign's required scopes: Gmail modify/send and Calendar events/free-busy.
7. Run `jneo-campaign auth validate`.

OAuth access and refresh tokens are encrypted with Fernet before being written. The envelope key is
stored in the operating-system credential store through `keyring`. On a headless deployment without
an OS store, set `CAMPAIGN_TOKEN_ENCRYPTION_KEY` through the deployment secret manager to a Fernet
key; never place it in source control. Google credentials refresh automatically and the refreshed
envelope is saved again.

## Enabling a LIVE pilot

LIVE mode is fail-closed. Configure a truthful sender name/email, reply address, organization and
postal address, fresh official contact evidence, target/excluded regions, and a verified recipient
timezone. Then set:

```dotenv
CAMPAIGN_MODE=LIVE
CAMPAIGN_LIVE_SEND=true
```

Run `jneo-campaign doctor --validate-credentials`, inspect the local dashboard, and begin with the
safe defaults: at most 10 new contacts and 20 total outbound messages per day, one initial contact per
organization, no more than two value-adding follow-ups, and a minimum four business days between
follow-ups. Jurisdictions without an implemented legal ruleset are marked
`MANUAL_LEGAL_REVIEW_REQUIRED`; they are not sent automatically. Calendar events are created only
after the recipient explicitly selects one of three rechecked options.

## Safety and evidence boundary

- Every proposition cites official prospect evidence and capability-registry identifiers.
- Websites and received mail are untrusted data. Prompt-injection-like content is quarantined and
  escalated; it cannot change policy, credentials, limits, or tool permissions.
- The system never guesses email addresses, accesses private social-network data, bypasses access
  controls/CAPTCHAs, or crawls a site through the default fixture provider.
- Unsubscribe and complaint replies immediately pause the thread and add contact plus organization
  suppression entries.
- Medical work is synthetic/de-identified, advisory, human-reviewed, and never diagnostic or
  prescriptive. UAV/BCI/high-consequence work remains simulation/advisory unless separately validated.
- Repository demos do not establish production readiness, adoption, accreditation, regulatory
  approval, guaranteed performance, or business outcomes.

See the repository documents `docs/campaign-architecture.md`, `docs/campaign-operations.md`,
`docs/campaign-security.md`, and `docs/campaign-compliance.md` for deployment and operating details.
