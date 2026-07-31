# Campaign compliance policy

This document describes product controls, not legal advice. An organization operating the campaign
must obtain counsel for its identity, jurisdiction, recipients, legal basis, record retention, and
message content.

## Fail-closed decision model

Every introductory asset receives one of four decisions:

- `APPROVED_DRY_RUN`: evidence and content checks pass, but external writes are disabled.
- `APPROVED`: the implemented US professional B2B ruleset and all LIVE identity/source checks pass.
- `MANUAL_LEGAL_REVIEW_REQUIRED`: facts may be sufficient, but no approved automated ruleset covers
  the jurisdiction or uncertainty.
- `BLOCKED` or `DO_NOT_CONTACT`: a required fact/control is absent or suppression applies.

Only `APPROVED` can be sent in LIVE. EU, UK, Canada, global/unknown recipients, sole traders, personal
mailboxes, and any unimplemented jurisdiction require a documented human legal decision; the
software does not guess how GDPR, ePrivacy, PECR, CASL, or local laws apply.

## Required evidence

An eligible recipient must be an allowlisted professional organization type with a verified
official site and an official/public or authorized professional contact channel. Organization and
contact facts retain URL, source type, retrieval time, and excerpt. LIVE evidence older than 90 days
requires review. Dictionary-generated email addresses, bought/questionable lists, individual
consumers, private social-network data, access-control bypass, and CAPTCHA bypass are forbidden.

The organization must fall within configured regions and outside excluded regions. The application
must have a recipient timezone, truthful sender name and email, valid reply address, project and
organization identity, and the required postal/business address.

## Content rules

The subject and body must be accurate, explain why the organization is being contacted, identify
Jneopallium and the sender, give a simple unsubscribe route, propose a bounded next step, distinguish
existing implementation from proposed integration, and cite evidence. Deceptive threading,
misleading urgency, empty “checking in” follow-ups, and volume optimization are prohibited.

The system blocks unsupported statements including guaranteed outcomes, production readiness,
customers or traction, patents, certifications, regulatory approval, clinical effectiveness,
military deployment, MRC accreditation, IAB certification, or proven ROI unless a future policy adds
authoritative evidence and approval. Existing repo demos are technical evidence, not evidence of
adoption or suitability for a recipient's production system.

## Suppression and health

The contact and organization suppression lists are checked before approval, queueing, and sending.
Unsubscribe or spam-complaint replies immediately pause the sequence, stop scheduled follow-ups, and
add contact- and organization-level suppression. Bounces and not-interested replies stop the
sequence. A complaint halts at the configured first indicator; a bounce rate above 5% halts outbound
processing. Suppressions are retained independently of ordinary contact-data deletion.

One initial contact per organization per campaign is the default. The safe ceiling is 10 new
contacts and 20 outbound messages per day, at most two automated follow-ups, and at least four
business days between follow-ups. LIVE sends occur only in the verified recipient business window
with deterministic randomized spacing.

## High-consequence domains

Medical material uses synthetic, de-identified, or openly licensed data; remains advisory; requires
human review; never diagnoses, prescribes, claims effectiveness/approval, or substitutes for a
clinician. DICOM work remains read-only and does not infer a diagnosis from pixels. UAV, defence,
BCI, critical-infrastructure, fraud, and cybersecurity material remains simulation, monitoring,
advisory, or safety validation unless a separately reviewed engagement establishes otherwise.

Pricing, contracts, warranties, regulatory or clinical claims, patient information, defence/export
controls, classified information, investment terms, IP transfer, exclusivity, production deployment
promises, and unsupported performance claims require escalation. The rest of the campaign may
continue for unrelated recipients, but that thread stays paused where appropriate.

## Calendar and reply conduct

Only campaign-labelled, persisted threads are processed. A reply stops automated follow-ups.
Calendar options are offered after genuine interest, with both parties' timezones considered. An
event is created only after explicit selection, a free/busy recheck, and a stored agreement record.
Unsolicited invitations are forbidden.

## Operator record

Before LIVE, record the campaign purpose, controller/sender identity, target and excluded regions,
recipient categories, source policy, lawful-basis assessment, retention period, escalation owner,
stop conditions, approved variants, daily limits, and legal review version. Preserve compliance
decisions, source references, suppressions, sends, classifications, calendar agreements, pauses, and
audit events. Re-review the policy whenever the audience, jurisdiction, data source, provider, or
message purpose changes.
