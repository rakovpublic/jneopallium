# Campaign security and privacy

## Threat model

The application handles public professional contact data, external web excerpts, email bodies,
OAuth credentials, outbound content, and meeting metadata. Websites and incoming messages are
untrusted data. Relevant threats include credential disclosure, prompt injection, server-side URL
abuse, malicious HTML or attachments, formula injection, unrelated mailbox ingestion, unauthorized
sends, excessive outreach, duplicate messages, and unsafe high-consequence claims.

## Controls

External writes need two explicit LIVE flags and valid Google credentials. The compliance and send
services independently recheck eligibility. Per-day, per-organization, follow-up, business-hour,
suppression, pause, bounce, and complaint gates constrain the write surface. The dashboard binds to
loopback by default.

Google OAuth uses Gmail modify/send and Calendar events/free-busy scopes. Tokens are encrypted with
Fernet; the envelope key is stored through the operating-system credential store or injected from a
deployment secret manager. Tokens, client secrets, API keys, `.env`, databases, and `secrets/` are
ignored by Git. Logs redact bearer tokens and fields whose names indicate keys, secrets, passwords,
or tokens.

The Gmail sync query is restricted to campaign-labelled mail and excludes the sender's own messages.
Records are accepted only when their provider thread ID maps to a persisted campaign thread. The
application does not scan or retain unrelated personal mail.

External text is length-limited, control characters are removed, and common instruction-injection
phrases are detected. Research results with suspected injection are rejected before storage. Email
classification is deterministic and treats the body only as data; it has no tool or policy-changing
authority. Any future LLM processing must pass the delimited untrusted-data block to a structured
schema and must never concatenate it into system instructions.

HTML output is escaped and then sanitized against a small tag and attribute allowlist. Outbound URLs
must be credential-free HTTPS URLs and cannot target localhost, private, or otherwise non-global IP
addresses. Attachments are limited to PDF, text, Markdown, PNG, and JPEG and to 5 MB. Spreadsheet
exports prefix cells beginning with formula metacharacters. Search providers cannot request private
social-network data, guessed email, CAPTCHA bypass, or authentication bypass.

Medical, UAV, BCI, security, and similar high-consequence offers are restricted to synthetic,
simulation, read-only, monitoring, or advisory work with human review. Prohibited claim patterns
block sending. Incoming requests involving contracts, warranties, clinical/regulatory status,
patient data, export control, classified information, investment terms, IP transfer, exclusivity,
production promises, or unsupported performance are escalated without an automatic substantive
answer.

## Data minimization and retention

Only professional outreach data with an official/public or authorized provenance record is stored.
The system does not infer sensitive or protected personal traits and does not use them in scoring.
Prefer role mailboxes and official forms over named individuals. Store only the excerpt necessary to
support relevance and contact provenance.

Operators must configure a retention schedule appropriate to their jurisdiction and legal basis.
Delete expired research, contact, message-content, and meeting data; preserve a minimal hashed or
normalized suppression record as needed to prevent future contact. Backups require the same
retention and access controls. Never store real patient data in campaign demos.

## Deployment checklist

- Use a dedicated Google account and Cloud project with reviewed consent-screen users.
- Restrict filesystem permissions on `.env`, OAuth client JSON, encrypted tokens, database, and
  backups.
- Keep the Fernet key separate from encrypted token storage and rotate credentials after suspected
  exposure.
- Put PostgreSQL and the dashboard behind authenticated private infrastructure before any shared
  deployment; the local dashboard has no built-in user authentication.
- Pin and review dependencies, run `pip-audit`, and use Maven/Java dependency scanning in CI.
- Review provider endpoint allowlists and TLS; do not point JSON providers at arbitrary URLs.
- Monitor failed authentication, provider failures, pauses, complaints, bounces, and unusual send
  volume.
- Exercise token revocation and database-restore procedures before LIVE use.

## Residual risks

The injection detector is defense in depth, not a proof that external text is benign. Gmail labels
and provider APIs can change behavior. The FastAPI dashboard is intended for a trusted local
operator and needs authentication and CSRF protection before network exposure. Fernet protects the
token file at rest but cannot protect credentials from a compromised running process. Regulatory
and outreach law must be reviewed for every new jurisdiction and campaign purpose.
