# Campaign compliance policy

This document describes product controls, not legal advice. An organization operating the campaign
must obtain counsel for its identity, jurisdiction, recipients, legal basis, record retention, and
message content.

## Fail-closed decision model

Every introductory asset receives one of four decisions:

- `APPROVED_DRY_RUN`: evidence and content checks pass, but external writes are disabled.
- `APPROVED`: the applicable region-specific evidence rule and all LIVE identity/source checks pass.
- `MANUAL_LEGAL_REVIEW_REQUIRED`: facts may be sufficient, but no approved automated ruleset covers
  the jurisdiction or uncertainty.
- `BLOCKED` or `DO_NOT_CONTACT`: a required fact/control is absent or suppression applies.

Only `APPROVED` can be sent in LIVE. The configured region codes are `US`, `EU`, `CA` (Canada), `JP`
(Japan), `KR` (South Korea), `UA` (Ukraine), `IL` (Israel), `GB` (United Kingdom), and `AU`
(Australia). Global/unknown recipients, personal mailboxes, and evidence that does not meet the
applicable rule require documented human review.

## Region-specific automation gates

These are conservative product gates, not a substitute for legal advice:

| Region | Evidence required for automated LIVE approval |
|---|---|
| US | Professional B2B relevance, truthful identification, postal address, and working opt-out. |
| GB | Verified corporate subscriber, generic organizational inbox, identification, and opt-out. Sole traders and non-corporate subscribers require review. |
| CA | Conspicuously published business address, no accompanying solicitation restriction, documented role relevance, identification, and unsubscribe. |
| JP | Address intentionally published by the organization/business, no adjacent refusal notice, documented relevance, and opt-out. |
| AU | Inferred-consent evidence for a directly published work address, documented relevance, no refusal notice, and unsubscribe. |
| EU | Explicit consent evidence. Member-state exceptions are not inferred automatically. |
| KR | Explicit prior consent evidence. |
| IL | Explicit prior consent evidence. |
| UA | Explicit consent, or reviewed Ukrainian-language opt-out evidence for the no-consent exception. |

The implementation follows the evidence distinctions described by the
[US FTC CAN-SPAM guide](https://www.ftc.gov/business-guidance/resources/can-spam-act-compliance-guide-business),
[UK ICO B2B marketing guidance](https://ico.org.uk/for-organisations/direct-marketing-and-privacy-and-electronic-communications/business-to-business-marketing/),
[Canada's CRTC CASL guidance](https://www.crtc.gc.ca/eng/com500/faq500.htm),
[Australia's ACMA Spam Act guidance](https://www.acma.gov.au/Industry/Marketers/Anti-Spam/Ensuring-you-dont-spam/key-elements-of-the-spam-act-ensuring-you-dont-spam-i-acma),
[EU ePrivacy Directive Article 13](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32002L0058),
[Japan's specified-email rules](https://www.caa.go.jp/policies/policy/consumer_transaction/specifed_email/),
[Korea's Network Act Article 50](https://www.law.go.kr/LSW/lsLawLinkInfo.do?chrClsCd=010202&lsId=000030&lsJoLnkSeq=1000688185&print=print),
[Ukraine's E-Commerce Law Article 10](https://zakon.rada.gov.ua/laws/show/en/675-19), and
[Israel's Communications Law section 30A](https://main.knesset.gov.il/Activity/Legislation/Laws/pages/lawbill.aspx?lawitemid=544591&t=lawreshumot). Permission
evidence has its own URL, excerpt, basis, entity type, review date, relevance flag, and record of
whether the publication contained a solicitation restriction. It expires after 90 days for this
campaign even where the underlying law may allow a longer period.

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
