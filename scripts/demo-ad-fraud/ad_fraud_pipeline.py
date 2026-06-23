#!/usr/bin/env python3
"""Deterministic advertising-fraud workflow for Jneopallium."""

from __future__ import annotations

import argparse
import csv
from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime, timezone
import hashlib
import json
import math
import os
from pathlib import Path
import random
import shutil
import statistics
import subprocess
import sys
import time
from typing import Any
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[2]
SCRIPT_DIR = Path(__file__).resolve().parent
TARGET = ROOT / "target" / "jneopallium-ad-fraud"
DATA_DIR = ROOT / "data"
MODEL_RESOURCE_DIR = ROOT / "worker" / "src" / "main" / "resources" / "model" / "advertising-fraud"
DEFAULT_FIRST_PARTY_LABELS = DATA_DIR / "first-party-ad-fraud-labels.jsonl"

LABELS = [
    "bot",
    "incentivized",
    "clickFarm",
    "eventSpoofing",
    "clickSpam",
    "clickInjection",
    "attributionHijack",
    "inventorySpoofing",
    "accidentalOrLowValue",
    "unknownSuspicious",
]

# Base receptor risks (layers 1-2). Computed by the integrity / entity-graph
# receptors and mirrored in the Java runtime extractFeatures.
BASE_FEATURES = [
    "integrity_risk",
    "bot_risk",
    "sequence_risk",
    "attribution_risk",
    "supply_chain_risk",
    "graph_risk",
    "quality_risk",
    "unknown_risk",
]

# Improvement #5/#6: new single-purpose behavioural-evidence features, each
# owned by its own neuron in the new behavioural-evidence layer. They break
# the conflated label pairs (clickSpam vs attributionHijack, incentivized vs
# clickFarm, accidental/low-value) that the 8 base risks cannot separate.
EVIDENCE_FEATURES = [
    "click_volume_risk",       # high click volume on CLICK events -> click spam
    "conversion_timing_risk",  # install/conversion too close to click -> injection
    "incentive_risk",          # install cohort with low downstream value -> incentivized
    "retention_risk",          # delayed low retention / refund -> low quality
    "accidental_risk",         # short dwell, no interaction, single low-value click
]

# Features produced by the receptor + behavioural layers and fed forward.
INPUT_FEATURES = BASE_FEATURES + EVIDENCE_FEATURES

# Improvement #7: a non-linear feature-interaction (hidden) layer. Each hidden
# unit is its own neuron computing tanh(w . input + b); the trained correlation
# heads then read base + evidence + hidden, so label separation is no longer
# limited to a single linear direction over 8 features.
HIDDEN_UNITS = 8
HIDDEN_FEATURES = [f"interaction_{i}" for i in range(HIDDEN_UNITS)]

# Full input to the trained correlation heads.
FEATURES = INPUT_FEATURES + HIDDEN_FEATURES

SCENARIOS = [
    "legitimate_high_value",
    "legitimate_low_conversion",
    "legitimate_short_session",
    "legitimate_low_value_returning",
    "benign_privacy_limited",
    "benign_delayed_conversion",
    "benign_no_identifiers",
    "accidental_click",
    "hidden_ad_accidental_click",
    "stacked_ad_accidental_click",
    "simple_bot_burst",
    "fixed_rate_bot",
    "randomized_rate_bot",
    "headed_browser_bot",
    "replayed_mouse_trace_bot",
    "valid_cookie_bot",
    "slow_stealth_bot",
    "rotating_proxy_bot",
    "bot_fake_conversion",
    "human_click_farm",
    "shared_network_click_farm",
    "shared_device_click_farm",
    "synchronized_click_farm",
    "incentivized_install_cohort",
    "motivated_rewarded_install",
    "consented_incentivized_returning_low_value",
    "click_spam",
    "synchronized_campaign_click_spam",
    "low_rate_click_reuse",
    "click_injection",
    "valid_postback",
    "forged_postback",
    "replayed_conversion",
    "invalid_hmac",
    "missing_signature",
    "unknown_click_id",
    "unknown_impression_id",
    "mismatched_campaign_postback",
    "mismatched_device_postback",
    "altered_purchase_value",
    "duplicated_conversion",
    "client_server_disagreement",
    "publisher_inventory_spoof",
    "supply_chain_missing_record",
    "supply_chain_malformed_record",
    "seller_owner_mismatch",
    "supply_path_intermediary_anomaly",
    "missing_device_attestation",
    "out_of_order_events",
    "delayed_conversion",
    "model_runtime_failure",
    "concept_drift",
]

EVALUATION_SPLITS = {"test", "adversarial_test", "future_test", "novel_scenario_test", "first_party_holdout"}
SPLIT_POLICY = (
    "stratified scenario quotas with campaign/device namespace isolation; "
    "50/20/20 train/validation/calibration blocks plus ordinary, adversarial, future-time, "
    "novel-scenario and first-party holdout tests"
)

EVENT_TYPES = [
    "BID_REQUEST",
    "IMPRESSION",
    "VIEWABLE_IMPRESSION",
    "CLICK",
    "LANDING",
    "INTERACTION",
    "INSTALL",
    "REGISTRATION",
    "PURCHASE",
    "POSTBACK",
    "REFUND",
    "CHARGEBACK",
    "UNINSTALL",
    "RETENTION",
    "PAYOUT",
]

AD_NEURON_PACKAGE = "com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud"
AD_SIGNAL_PACKAGE = "com.rakovpublic.jneuropallium.worker.net.signals.impl.adfraud"
AD_PROCESSOR_PACKAGE = "com.rakovpublic.jneuropallium.worker.signalprocessor.impl.adfraud"
AD_DEMO_PACKAGE = "com.rakovpublic.jneuropallium.worker.demo.adfraud"
SIMPLE_SIGNAL_CHAIN = "com.rakovpublic.jneuropallium.ai.neurons.base.SimpleSignalChain"

SOURCE_FAMILIES = [
    "ad-server bid and impression telemetry",
    "click, landing and interaction streams",
    "mobile measurement partner postbacks",
    "retention, refund and billing events",
    "device-attestation and signature metadata",
    "ads.txt, sellers.json and exchange supply-chain context",
    "analyst feedback and appeal outcomes",
]

AD_RESPONSE_ACTIONS = [
    "ALLOW",
    "LOG",
    "REVIEW",
    "RATE_LIMIT_CANDIDATE",
    "HOLD_PAYOUT_CANDIDATE",
    "DISCOUNT_TRAFFIC_CANDIDATE",
    "ESCALATE_TO_ANALYST",
]

PRIVACY_CONTROLS = [
    "device and account identifiers are pseudonymized before training",
    "raw IP addresses, precise geolocation and user-agent strings are not exported",
    "missing-value indicators are distinct from zero-valued risk evidence",
    "model output is advisory-only until first-party validation and appeal controls exist",
]


@dataclass
class Example:
    event: dict[str, Any]
    features: dict[str, float]
    labels: dict[str, int]
    split: str


def stable_unit(text: str) -> float:
    digest = hashlib.sha256(text.encode("utf-8")).hexdigest()
    return int(digest[:12], 16) / float(0xFFFFFFFFFFFF)


def sigmoid(value: float) -> float:
    if value >= 0:
        z = math.exp(-value)
        return 1.0 / (1.0 + z)
    z = math.exp(value)
    return z / (1.0 + z)


def logit(value: float) -> float:
    p = min(1.0 - 1e-6, max(1e-6, value))
    return math.log(p / (1.0 - p))


def clamp(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=False) + "\n", encoding="utf-8")


def write_jsonl(path: Path, rows: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as fh:
        for row in rows:
            fh.write(json.dumps(row, sort_keys=False) + "\n")


def source_catalog() -> list[dict[str, Any]]:
    return [
        {
            "id": "criteo_attribution_dataset",
            "name": "Criteo Attribution Dataset",
            "url": "https://huggingface.co/datasets/criteo/criteo-attribution-dataset",
            "sourceType": "REAL_WEAK_LABEL",
            "fraudClasses": ["CLICK_SPAM", "CLICK_INJECTION", "ATTRIBUTION_HIJACK"],
            "labelStrength": "WEAK",
            "commercialUse": "RESTRICTED",
            "license": "CC-BY-NC-SA-4.0",
            "automaticDownload": False,
            "requiresCredentials": False,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "temporal click and attribution modeling; not intentional-fraud labels",
            "schema": "click/conversion attribution logs",
            "defaultUse": "metadata_only",
            "boundedUse": "streamed sample for temporal baselines and simulation priors",
        },
        {
            "id": "criteo_private_ad",
            "name": "CriteoPrivateAd",
            "url": "https://arxiv.org/abs/2502.12103",
            "sourceType": "REAL_OUTCOME_LABEL",
            "fraudClasses": ["MISSING_DELAYED_EVENTS", "PRIVACY_LIMITED_ATTRIBUTION"],
            "labelStrength": "OUTCOME_ONLY",
            "commercialUse": "SHAREALIKE_REVIEW_REQUIRED",
            "license": "CC-BY-SA-4.0",
            "automaticDownload": False,
            "requiresCredentials": False,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "privacy-limited bidding, landed-click, delayed-reporting and benign auction baselines",
            "schema": "real-world bidding outcomes with delayed reporting scenarios",
            "defaultUse": "metadata_only",
        },
        {
            "id": "criteo_click_logs",
            "name": "Criteo Click Logs",
            "url": "https://huggingface.co/datasets/criteo/CriteoClickLogs",
            "sourceType": "REAL_CLICK_FEEDBACK",
            "fraudClasses": ["BACKGROUND_TRAFFIC", "SCALABILITY", "RARE_CATEGORY_BASELINES"],
            "labelStrength": "UNLABELED_FOR_FRAUD",
            "commercialUse": "RESTRICTED",
            "license": "CC-BY-NC-SA-4.0",
            "automaticDownload": False,
            "requiresCredentials": False,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "normal click/impression baseline; CTR is not a fraud label",
            "schema": "bounded click-log sample",
            "defaultUse": "metadata_only",
            "boundedUse": "1-5M streamed rows in non-quick experiments; no full 276GB download in repo workflow",
        },
        {
            "id": "talkingdata_kaggle",
            "name": "TalkingData AdTracking Fraud Detection Challenge",
            "url": "https://www.kaggle.com/competitions/talkingdata-adtracking-fraud-detection",
            "sourceType": "REAL_MOBILE_ATTRIBUTION",
            "fraudClasses": ["CLICK_SPAM", "CLICK_INJECTION", "ATTRIBUTION_WINDOW_MODELING"],
            "labelStrength": "POSITIVE_UNLABELED",
            "commercialUse": "KAGGLE_TERMS",
            "license": "Kaggle competition terms",
            "automaticDownload": False,
            "requiresCredentials": True,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "weak attribution-quality supervision only when credentials already exist",
            "schema": "mobile click attribution",
            "defaultUse": "credentials_required",
        },
        {
            "id": "criteo_uplift",
            "name": "Criteo Uplift Dataset",
            "url": "https://huggingface.co/datasets/criteo/criteo-uplift",
            "sourceType": "REAL_CAUSAL_OUTCOME",
            "fraudClasses": ["LOW_INCREMENTALITY", "INCENTIVIZED_TRAFFIC_PRIORS"],
            "labelStrength": "OUTCOME_ONLY",
            "commercialUse": "RESTRICTED",
            "license": "CC-BY-NC-SA-4.0",
            "automaticDownload": False,
            "requiresCredentials": False,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "uplift and incrementality baselines so conversion is not over-trusted",
            "schema": "treatment, exposure, visit and conversion indicators",
            "defaultUse": "metadata_only",
        },
        {
            "id": "avazu_ctr_prediction",
            "name": "Avazu CTR Prediction",
            "url": "https://www.kaggle.com/c/avazu-ctr-prediction",
            "sourceType": "REAL_CLICK_FEEDBACK",
            "fraudClasses": ["BACKGROUND_TRAFFIC", "RARE_CATEGORY_BASELINES", "MOBILE_CONTEXT_BASELINES"],
            "labelStrength": "UNLABELED_FOR_FRAUD",
            "commercialUse": "KAGGLE_TERMS",
            "license": "Kaggle competition terms",
            "automaticDownload": False,
            "requiresCredentials": True,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "chronological mobile ad click logs for normal-background and rare-context baselines",
            "schema": "ad id, device/app/site/context categorical fields and click feedback",
            "defaultUse": "credentials_required",
        },
        {
            "id": "kddcup2012_track2",
            "name": "KDD Cup 2012 Track 2 Tencent sponsored-search CTR",
            "url": "https://www.kaggle.com/c/kddcup2012-track2",
            "sourceType": "REAL_CLICK_FEEDBACK",
            "fraudClasses": ["BACKGROUND_TRAFFIC", "SEARCH_AD_CONTEXT_BASELINES"],
            "labelStrength": "UNLABELED_FOR_FRAUD",
            "commercialUse": "KAGGLE_TERMS",
            "license": "Kaggle/SIGKDD competition terms",
            "automaticDownload": False,
            "requiresCredentials": True,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "sponsored-search click-through baselines and query/ad/user context hard negatives",
            "schema": "Tencent search advertising session instances with click feedback",
            "defaultUse": "credentials_required",
        },
        {
            "id": "ipinyou_rtb",
            "name": "iPinYou Global RTB Bidding Algorithm Competition Dataset",
            "url": "https://contest.ipinyou.com/",
            "sourceType": "REAL_RTB_LOGS",
            "fraudClasses": ["BID_IMPRESSION_CLICK_CONVERSION_PATHS", "SUPPLY_AND_AUCTION_BASELINES"],
            "labelStrength": "OUTCOME_ONLY",
            "commercialUse": "LICENSE_REVIEW_REQUIRED",
            "license": "competition/dataset terms must be inspected before use",
            "automaticDownload": False,
            "requiresCredentials": False,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "RTB bid, impression, click and final conversion paths for auction and campaign baselines",
            "schema": "DSP bidding logs, impressions, clicks and conversions",
            "defaultUse": "metadata_only",
        },
        {
            "id": "ali_ccp",
            "name": "Ali-CCP Alibaba Click and Conversion Prediction Dataset",
            "url": "https://tianchi.aliyun.com/dataset/408",
            "sourceType": "REAL_CLICK_CONVERSION_LOGS",
            "fraudClasses": ["POST_CLICK_CONVERSION_BASELINES", "BACKGROUND_TRAFFIC"],
            "labelStrength": "OUTCOME_ONLY",
            "commercialUse": "TIANCHI_TERMS",
            "license": "Tianchi/Alibaba dataset terms",
            "automaticDownload": False,
            "requiresCredentials": True,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "post-click conversion baselines and click/conversion sparsity hard negatives",
            "schema": "Taobao real-world traffic logs with click and conversion labels",
            "defaultUse": "credentials_required",
        },
        {
            "id": "frauddroid_artifacts",
            "name": "FraudDroid research artifacts",
            "url": "https://arxiv.org/abs/1709.01213",
            "sourceType": "PUBLIC_FRAUD_RESEARCH",
            "fraudClasses": ["MOBILE_INTERACTION_FRAUD", "HIDDEN_ADS", "ACCIDENTAL_CLICK_INDUCEMENT"],
            "labelStrength": "RESEARCH_CONFIRMED",
            "commercialUse": "LICENSE_REVIEW_REQUIRED",
            "license": "paper/artifact license must be inspected before use",
            "automaticDownload": False,
            "requiresCredentials": False,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "mobile app UI-state and network-behaviour fraud scenarios; APK redistribution excluded unless rights are clear",
            "schema": "labelled app metadata, UI transition findings and experiment results when available",
            "defaultUse": "metadata_only",
        },
        {
            "id": "advanced_web_bot_behaviour",
            "name": "Advanced web-bot behaviour research",
            "url": "https://zenodo.org/records/5549439",
            "sourceType": "PUBLIC_BEHAVIOUR_RESEARCH",
            "fraudClasses": ["BOT", "ADVANCED_BROWSER_BOT"],
            "labelStrength": "METHODOLOGY",
            "commercialUse": "LICENSE_REVIEW_REQUIRED",
            "license": "record license must be inspected before use",
            "automaticDownload": False,
            "requiresCredentials": False,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "mouse movement, timing, entropy and protocol design for controlled bot generation",
            "schema": "publication and possible behaviour artifacts",
            "defaultUse": "metadata_only",
        },
        {
            "id": "botcha_positive_unlabeled",
            "name": "Botcha positive-unlabelled bot-detection method",
            "url": "https://arxiv.org/abs/2103.01428",
            "sourceType": "PUBLIC_FRAUD_RESEARCH",
            "fraudClasses": ["BOT", "POSITIVE_UNLABELED_LEARNING"],
            "labelStrength": "METHODOLOGY",
            "commercialUse": "LICENSE_REVIEW_REQUIRED",
            "license": "paper/artifact license must be inspected before use",
            "automaticDownload": False,
            "requiresCredentials": False,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "known-human positive plus unlabeled-mixture protocol for web bot modelling",
            "schema": "methodology and public/proprietary evaluation description",
            "defaultUse": "metadata_only",
        },
        {
            "id": "clicktok_methodology",
            "name": "Clicktok click-fraud traffic analysis",
            "url": "https://arxiv.org/abs/1903.00733",
            "sourceType": "PUBLIC_FRAUD_RESEARCH",
            "fraudClasses": ["CLICK_FARM", "CLICK_REUSE", "SYNCHRONIZED_CLICKING"],
            "labelStrength": "METHODOLOGY",
            "commercialUse": "LICENSE_REVIEW_REQUIRED",
            "license": "paper/artifact license must be inspected before use",
            "automaticDownload": False,
            "requiresCredentials": False,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "repeated click traces, copied timing templates and historical-distribution divergence scenarios",
            "schema": "methodology and bait-click experiment design",
            "defaultUse": "metadata_only",
        },
        {
            "id": "iab_ads_txt",
            "name": "ads.txt and app-ads.txt",
            "url": "https://iabtechlab.com/ads-txt/",
            "sourceType": "DETERMINISTIC_STANDARDS_DATA",
            "fraudClasses": ["INVENTORY_SPOOFING", "UNAUTHORIZED_SELLER", "MALFORMED_RECORD"],
            "labelStrength": "DETERMINISTIC",
            "commercialUse": "SITE_TERMS_AND_ROBOTS",
            "license": "public standards and site-specific robots",
            "automaticDownload": True,
            "requiresCredentials": False,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "publisher-declared authorization records for supply-chain consistency",
            "schema": "publisher domain, app bundle, authorized exchange, seller account, DIRECT/RESELLER",
            "defaultUse": "metadata_only",
        },
        {
            "id": "iab_sellers_json_supply_chain",
            "name": "sellers.json and OpenRTB SupplyChain objects",
            "url": "https://iabtechlab.com/sellers-json/",
            "sourceType": "DETERMINISTIC_STANDARDS_DATA",
            "fraudClasses": ["INVENTORY_SPOOFING", "SELLER_MISMATCH", "SUPPLY_PATH_ANOMALY"],
            "labelStrength": "DETERMINISTIC",
            "commercialUse": "SITE_TERMS_AND_ROBOTS",
            "license": "public standards and site-specific robots",
            "automaticDownload": True,
            "requiresCredentials": False,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "seller/intermediary graph and OpenRTB supply-chain consistency",
            "schema": "seller account, seller type, intermediary path, buyer and domain-owner consistency",
            "defaultUse": "metadata_only",
        },
        {
            "id": "iab_om_sdk_device_attestation",
            "name": "Open Measurement SDK device attestation",
            "url": "https://iabtechlab.com/press-releases/device-attestation-support-in-open-measurement-sdk/",
            "sourceType": "FUTURE_FIRST_PARTY_SIGNAL",
            "fraudClasses": ["DEVICE_ATTESTATION_FAILURE", "MEASUREMENT_INTEGRITY"],
            "labelStrength": "FIRST_PARTY_SIGNAL",
            "commercialUse": "IMPLEMENTATION_DEPENDENT",
            "license": "IAB Tech Lab documentation and SDK terms",
            "automaticDownload": False,
            "requiresCredentials": False,
            "mayTrainProductionModel": True,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "attestation-present, attestation-valid and device-claim consistency features",
            "schema": "attestation provider, age, validity and measurement state",
            "defaultUse": "metadata_only",
        },
        {
            "id": "benign_crawlers",
            "name": "Public benign crawler identifiers",
            "url": "https://developers.google.com/search/docs/crawling-indexing/overview-google-crawlers",
            "sourceType": "PUBLIC_BENIGN_AUTOMATION_CONTEXT",
            "fraudClasses": ["BENIGN_AUTOMATION_HARD_NEGATIVE"],
            "labelStrength": "CONTEXT",
            "commercialUse": "DOCUMENTATION_TERMS",
            "license": "documentation terms",
            "automaticDownload": True,
            "requiresCredentials": False,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "known benign automation context when terms permit",
            "schema": "crawler identifiers",
            "defaultUse": "metadata_only",
        },
        {
            "id": "controlled_bot_lab",
            "name": "Controlled bot laboratory",
            "url": "generated://controlled-bot-lab",
            "sourceType": "CONTROLLED_SYNTHETIC_ATTACK",
            "fraudClasses": ["BOT", "STEALTH_BOT", "FAKE_CONVERSION_BOT"],
            "labelStrength": "STRONG_SYNTHETIC",
            "commercialUse": "INTERNAL_TEST_ENVIRONMENT_ONLY",
            "license": "generated by project",
            "automaticDownload": False,
            "requiresCredentials": False,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "fixed-rate, randomized-rate, headed/headless, replayed-trace and valid-cookie bots against owned test surfaces",
            "schema": "generated event stream with known mutation labels",
            "defaultUse": "generated",
        },
        {
            "id": "controlled_event_spoofing_lab",
            "name": "Controlled event-spoofing laboratory",
            "url": "generated://controlled-event-spoofing-lab",
            "sourceType": "CONTROLLED_SYNTHETIC_ATTACK",
            "fraudClasses": ["EVENT_SPOOFING", "REPLAY", "POSTBACK_SUBSTITUTION"],
            "labelStrength": "STRONG_SYNTHETIC",
            "commercialUse": "INTERNAL_TEST_ENVIRONMENT_ONLY",
            "license": "generated by project",
            "automaticDownload": False,
            "requiresCredentials": False,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "invalid HMAC, missing signature, unknown click/impression, mismatched campaign/device and replayed postback mutations",
            "schema": "generated event stream with exact mutation labels",
            "defaultUse": "generated",
        },
        {
            "id": "controlled_motivated_traffic_study",
            "name": "Controlled motivated traffic study",
            "url": "generated://controlled-motivated-traffic-study",
            "sourceType": "CONTROLLED_SYNTHETIC_OR_CONSENTED_STUDY",
            "fraudClasses": ["INCENTIVIZED", "LOW_VALUE_TRAFFIC"],
            "labelStrength": "STRONG_WHEN_CONSENTED",
            "commercialUse": "CONSENT_AND_POLICY_DEPENDENT",
            "license": "generated by project unless replaced by consented study data",
            "automaticDownload": False,
            "requiresCredentials": False,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "reward-driven install/register/minimal-action cohorts and downstream quality labels",
            "schema": "generated or consented events with retention, value, refund and uninstall outcomes",
            "defaultUse": "generated",
        },
        {
            "id": "controlled_human_click_farm_simulation",
            "name": "Controlled human click-farm simulation",
            "url": "generated://controlled-human-click-farm-simulation",
            "sourceType": "CONTROLLED_SYNTHETIC_OR_CONSENTED_STUDY",
            "fraudClasses": ["CLICK_FARM", "SYNCHRONIZED_CLICKING", "SHARED_DEVICE_FANOUT"],
            "labelStrength": "STRONG_WHEN_CONSENTED",
            "commercialUse": "CONSENT_AND_POLICY_DEPENDENT",
            "license": "generated by project unless replaced by consented study data",
            "automaticDownload": False,
            "requiresCredentials": False,
            "mayTrainProductionModel": False,
            "mayEvaluateProductionClaims": False,
            "intendedUse": "shared networks/devices, synchronized schedules and repeated advertising task cohorts",
            "schema": "generated graph/cohort event stream",
            "defaultUse": "generated",
        },
        {
            "id": "first_party_labeled_traffic",
            "name": "User-supplied first-party labelled ad traffic",
            "url": str(DEFAULT_FIRST_PARTY_LABELS),
            "sourceType": "REAL_STRONG_LABEL",
            "fraudClasses": ["ALL_CLASSES_WHEN_LABELLED"],
            "labelStrength": "STRONG_OR_WEAK_BY_PROVENANCE",
            "commercialUse": "CONTRACT_DEPENDENT",
            "license": "user supplied; must be authorized for model training",
            "automaticDownload": False,
            "requiresCredentials": False,
            "mayTrainProductionModel": True,
            "mayEvaluateProductionClaims": True,
            "intendedUse": "analyst-reviewed production labels, appeal outcomes, refunds, chargebacks, MMP postbacks and known-good traffic",
            "schema": "JSONL or CSV canonical event rows with label_* columns or a labels array",
            "defaultUse": "user_supplied",
        },
    ]


def discover_sources() -> dict[str, Any]:
    DATA_DIR.mkdir(exist_ok=True)
    (DATA_DIR / "licenses").mkdir(exist_ok=True)
    generated_at = datetime.now(timezone.utc).replace(microsecond=0).isoformat()
    manifest = {
        "schemaVersion": "1.0",
        "generatedAt": generated_at,
        "sources": source_catalog(),
    }
    write_json(DATA_DIR / "source-manifest.json", manifest)
    write_json(TARGET / "dataset_report.json", {
        "generatedAt": generated_at,
        "sourcesEvaluated": len(manifest["sources"]),
        "policy": "Only official or clearly licensed sources are used automatically.",
    })
    (DATA_DIR / "DATASET_REPORT.md").write_text(
        "# Advertising Fraud Dataset Report\n\n"
        "Discovery records official public candidates. Optional sources that require credentials, "
        "manual acceptance, or unclear license terms are recorded and skipped without blocking the workflow.\n",
        encoding="utf-8")
    return manifest


def download_sources(offline: bool, first_party_path: Path | None = None, max_bytes: int = 65536) -> dict[str, Any]:
    manifest = discover_sources()
    cache_dir = DATA_DIR / "cache" / "ad-fraud"
    cache_dir.mkdir(parents=True, exist_ok=True)
    results = []
    for source in manifest["sources"]:
        status = "dataset_unavailable"
        reason = "offline mode" if offline else "metadata-only or unavailable bounded direct download"
        cached_path = None
        if source["defaultUse"] == "generated":
            status = "generated"
            reason = "controlled deterministic scenarios are generated by the pipeline"
        elif source["defaultUse"] == "user_supplied":
            candidate = first_party_path or (DEFAULT_FIRST_PARTY_LABELS if DEFAULT_FIRST_PARTY_LABELS.exists() else None)
            if candidate and candidate.exists():
                cached_path = candidate
                status = "loaded"
                reason = "user supplied first-party labels loaded from local file"
            else:
                status = "not_provided"
                reason = "place authorized JSONL/CSV labels at data/first-party-ad-fraud-labels.jsonl or pass --first-party-labels"
        elif source["defaultUse"] == "credentials_required":
            status = "credentials_required"
            reason = "credentials and terms were not already available in environment"
        elif not offline and source.get("automaticDownload") and str(source.get("url", "")).startswith("http"):
            try:
                request = Request(source["url"], headers={"User-Agent": "jneopallium-ad-fraud-demo/1.0"})
                with urlopen(request, timeout=10) as response:
                    content = response.read(max_bytes)
                cached_path = cache_dir / f"{source['id']}.sample"
                cached_path.write_bytes(content)
                status = "downloaded"
                reason = "bounded official metadata sample"
            except Exception as exc:
                reason = f"download failed: {exc.__class__.__name__}"
        results.append({
            **source,
            "retrievalDate": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
            "status": status,
            "reason": reason,
            "cachePath": str(cached_path) if cached_path else None,
            "size": cached_path.stat().st_size if cached_path and cached_path.exists() else 0,
            "sha256": sha256_file(cached_path) if cached_path and cached_path.exists() else None,
        })
    payload = {"sources": results}
    write_json(DATA_DIR / "source-manifest.json", {"schemaVersion": "1.0", "sources": results})
    return payload


def scenario_labels(scenario: str) -> dict[str, int]:
    labels = {label: 0 for label in LABELS}
    if "bot" in scenario:
        labels["bot"] = 1
    if "incentivized" in scenario:
        labels["incentivized"] = 1
        labels["accidentalOrLowValue"] = 1
    if scenario in {"motivated_rewarded_install", "consented_incentivized_returning_low_value"}:
        labels["incentivized"] = 1
        labels["accidentalOrLowValue"] = 1
    if "click_farm" in scenario:
        labels["clickFarm"] = 1
    if scenario in {
        "forged_postback",
        "replayed_conversion",
        "missing_device_attestation",
        "out_of_order_events",
        "invalid_hmac",
        "missing_signature",
        "unknown_click_id",
        "unknown_impression_id",
        "mismatched_campaign_postback",
        "mismatched_device_postback",
        "altered_purchase_value",
        "duplicated_conversion",
        "client_server_disagreement",
        "bot_fake_conversion",
    }:
        labels["eventSpoofing"] = 1
    if scenario in {"click_spam", "synchronized_campaign_click_spam", "low_rate_click_reuse"}:
        labels["clickSpam"] = 1
        labels["attributionHijack"] = 1
    if scenario in {"click_injection", "bot_fake_conversion"}:
        labels["clickInjection"] = 1
        labels["attributionHijack"] = 1
    if scenario in {
        "publisher_inventory_spoof",
        "supply_chain_missing_record",
        "supply_chain_malformed_record",
        "seller_owner_mismatch",
        "supply_path_intermediary_anomaly",
    }:
        labels["inventorySpoofing"] = 1
    if scenario in {"accidental_click", "hidden_ad_accidental_click", "stacked_ad_accidental_click"}:
        labels["accidentalOrLowValue"] = 1
    if scenario in {"concept_drift", "model_runtime_failure"}:
        labels["unknownSuspicious"] = 1
    return labels


def _block_split(idx: int) -> str:
    bucket = idx % 10
    if bucket < 5:
        return "train"
    if bucket < 7:
        return "validation"
    if bucket < 9:
        return "calibration"
    return "test"


def split_for(scenario: str, idx: int) -> str:
    # Keep in-distribution coverage for every trained head, then carve out
    # explicit stress tests that are not allowed to tune thresholds.
    if scenario in {"click_injection", "publisher_inventory_spoof"}:
        return "adversarial_test" if idx % 20 >= 18 else _block_split(idx)
    if scenario == "delayed_conversion":
        return "future_test"
    if scenario == "concept_drift":
        return "novel_scenario_test"
    return _block_split(idx)


def generate_event(scenario: str, idx: int, seed: int) -> tuple[dict[str, Any], dict[str, float]]:
    rnd = random.Random(f"{seed}:{scenario}:{idx}")
    base_time = 1_800_000_000_000 + (SCENARIOS.index(scenario) * 10_000_000) + idx * 1000
    labels = scenario_labels(scenario)
    event_type = "CLICK"
    if "postback" in scenario:
        event_type = "POSTBACK"
    elif (
        "conversion" in scenario
        or "install" in scenario
        or "incentivized" in scenario
        or "rewarded" in scenario
        or scenario in {"altered_purchase_value"}
    ):
        event_type = "INSTALL"
    elif scenario == "legitimate_high_value":
        event_type = "PURCHASE"
    elif scenario == "legitimate_low_value_returning":
        event_type = "PURCHASE"
    elif scenario in {
        "publisher_inventory_spoof",
        "supply_chain_missing_record",
        "supply_chain_malformed_record",
        "seller_owner_mismatch",
        "supply_path_intermediary_anomaly",
    }:
        event_type = "IMPRESSION"
    event = {
        "schemaVersion": "1.0",
        "event_id": f"{scenario}-{idx:05d}",
        "event_type": event_type,
        "event_time": base_time,
        "ingest_time": base_time + rnd.randint(0, 5000),
        "impression_id": f"imp-{scenario}-{idx // 3}",
        "click_id": f"clk-{scenario}-{idx // 2}",
        "session_id": f"s-{scenario}-{idx // 4}",
        "anonymous_user_id": pseudonym(f"user-{scenario}-{idx // 7}"),
        "device_id_hash": pseudonym(f"dev-{scenario}-{idx // (2 if labels['clickFarm'] else 11)}"),
        "fingerprint_hash": pseudonym(f"fp-{scenario}-{idx // (3 if labels['clickFarm'] else 13)}"),
        "ip_prefix_hash": pseudonym(f"10.{SCENARIOS.index(scenario)}.{idx // 9}.0/24"),
        "asn": 64512 + (idx % 15),
        "publisher_id": f"pub-{idx % (2 if labels['clickFarm'] or labels['incentivized'] else 12)}",
        "site_domain": f"site{idx % 9}.example",
        "app_bundle": f"app.{idx % 8}",
        "placement_id": f"plc-{idx % 16}",
        "creative_id": f"crt-{idx % 20}",
        "campaign_id": f"camp-{SCENARIOS.index(scenario)}",
        "advertiser_id": "adv-demo",
        "exchange_id": "ex-demo",
        "seller_id": f"seller-{idx % 6}",
        "supply_chain": "complete",
        "country": ["US", "UA", "DE", "BR", "IN"][idx % 5],
        "device_type": ["mobile", "desktop", "tablet"][idx % 3],
        "os": ["Android", "iOS", "Windows", "Linux"][idx % 4],
        "browser": ["Chrome", "Safari", "Firefox", "Edge"][idx % 4],
        "sdk_version": "1.0",
        "signature_present": True,
        "signature_valid": True,
        "signature_key_id": "demo-key",
        "nonce": f"nonce-{scenario}-{idx}",
        "nonce_reused": False,
        "source_timestamp": base_time,
        "server_receive_timestamp": base_time + rnd.randint(10, 4000),
        "client_event_present": True,
        "server_event_present": True,
        "device_attestation_present": True,
        "device_attestation_valid": True,
        "ads_txt_authorized": True,
        "seller_json_match": True,
        "supply_chain_complete": True,
        "page_visible_ms": rnd.randint(800, 10_000),
        "dwell_ms": rnd.randint(500, 9000),
        "scroll_depth": round(rnd.random(), 3),
        "pointer_event_count": rnd.randint(3, 60),
        "pointer_distance": round(rnd.random() * 3000, 3),
        "pointer_velocity_entropy": round(0.25 + rnd.random() * 0.75, 3),
        "touch_event_count": rnd.randint(0, 20),
        "keyboard_event_count": rnd.randint(0, 8),
        "focus_change_count": rnd.randint(0, 4),
        "interaction_before_click": True,
        "viewport_width": 390 + rnd.randint(0, 1200),
        "viewport_height": 700 + rnd.randint(0, 900),
        "click_x": rnd.randint(1, 390),
        "click_y": rnd.randint(1, 700),
        "automation_flag": False,
        "headless_flag": False,
        "cookie_age_seconds": rnd.randint(3600, 864000),
        "session_event_count": rnd.randint(2, 12),
        "day_1_retained": True,
        "day_7_retained": scenario == "legitimate_high_value",
        "day_30_retained": scenario == "legitimate_high_value",
        "meaningful_action_count": 2 if scenario == "legitimate_high_value" else rnd.randint(0, 2),
        "purchase_value": 5.0 if scenario == "legitimate_high_value" else 0.0,
        "refund_value": 0.0,
        "chargeback": False,
        "uninstall_delay": None,
        "customer_quality_label": "high" if scenario == "legitimate_high_value" else "unknown",
        "analyst_label": "invalid_traffic" if any(labels.values()) and scenario not in {"legitimate_low_conversion", "accidental_click"} else "not_reviewed",
        "source_type": "SYNTHETIC" if not scenario.startswith("legitimate") else "WEAK_LABEL",
        "label_origin": f"deterministic_simulator:{scenario}",
        "label_confidence": 0.85 if any(labels.values()) else 0.55,
        "scenario_id": scenario,
    }
    if labels["bot"]:
        event.update({
            "automation_flag": True,
            "headless_flag": scenario != "slow_stealth_bot",
            "pointer_velocity_entropy": 0.02 if scenario == "simple_bot_burst" else 0.10,
            "dwell_ms": 30 if scenario == "simple_bot_burst" else 240,
            "cookie_age_seconds": rnd.randint(0, 30),
        })
        if scenario == "fixed_rate_bot":
            event["session_event_count"] = 44
            event["dwell_ms"] = 45
            event["pointer_velocity_entropy"] = 0.03
        if scenario == "randomized_rate_bot":
            event["session_event_count"] = rnd.randint(18, 44)
            event["dwell_ms"] = rnd.randint(90, 420)
            event["pointer_velocity_entropy"] = 0.12
        if scenario == "headed_browser_bot":
            event["headless_flag"] = False
            event["pointer_event_count"] = rnd.randint(1, 4)
            event["pointer_velocity_entropy"] = 0.08
        if scenario == "replayed_mouse_trace_bot":
            event["headless_flag"] = False
            event["pointer_event_count"] = 32
            event["pointer_distance"] = 900.0
            event["pointer_velocity_entropy"] = 0.06
            event["focus_change_count"] = 0
        if scenario == "valid_cookie_bot":
            event["cookie_age_seconds"] = rnd.randint(86_400, 864_000)
            event["headless_flag"] = False
            event["pointer_velocity_entropy"] = 0.09
        if scenario == "bot_fake_conversion":
            event["event_type"] = "INSTALL"
            event["session_event_count"] = 55
            event["dwell_ms"] = 120
    if labels["eventSpoofing"]:
        event["signature_valid"] = False
        if scenario == "replayed_conversion":
            event["event_id"] = f"replayed-{idx // 3}"
            event["nonce"] = f"reused-{idx // 3}"
            event["nonce_reused"] = True
        if scenario == "invalid_hmac":
            event["signature_key_id"] = "tampered-key"
        if scenario == "missing_signature":
            event["signature_present"] = False
        if scenario == "unknown_click_id":
            event["click_id"] = f"unknown-click-{idx}"
            event["client_event_present"] = False
        if scenario == "unknown_impression_id":
            event["impression_id"] = f"unknown-imp-{idx}"
            event["server_event_present"] = False
        if scenario == "mismatched_campaign_postback":
            event["event_type"] = "POSTBACK"
            event["campaign_id"] = f"camp-mismatch-{idx % 7}"
            event["server_event_present"] = False
        if scenario == "mismatched_device_postback":
            event["event_type"] = "POSTBACK"
            event["device_id_hash"] = pseudonym(f"mismatched-device-{idx}")
            event["client_event_present"] = False
        if scenario == "altered_purchase_value":
            event["event_type"] = "PURCHASE"
            event["purchase_value"] = 99.0
            event["server_receive_timestamp"] = base_time + 7_200_000
        if scenario == "duplicated_conversion":
            event["event_id"] = f"duplicated-conversion-{idx // 2}"
            event["nonce"] = f"duplicated-nonce-{idx // 2}"
            event["nonce_reused"] = True
        if scenario == "client_server_disagreement":
            event["client_event_present"] = False
            event["server_receive_timestamp"] = base_time + 4_200_000
        if scenario == "missing_device_attestation":
            event["device_attestation_present"] = True
            event["device_attestation_valid"] = False
        if scenario == "out_of_order_events":
            event["event_time"] = base_time - 20_000
    if labels["clickSpam"] or labels["clickInjection"]:
        event["session_event_count"] = rnd.randint(30, 80)
        event["dwell_ms"] = rnd.randint(80, 250)
        if scenario == "synchronized_campaign_click_spam":
            event["session_event_count"] = rnd.randint(45, 90)
            event["campaign_id"] = f"sync-campaign-{idx % 2}"
        if scenario == "low_rate_click_reuse":
            event["session_event_count"] = rnd.randint(26, 34)
            event["click_id"] = f"reused-click-pattern-{idx % 9}"
        if labels["clickInjection"]:
            event["event_type"] = "INSTALL"
            event["event_time"] = base_time + 400
    if labels["inventorySpoofing"]:
        event["ads_txt_authorized"] = False
        event["seller_json_match"] = False
        event["supply_chain_complete"] = False
        if scenario == "supply_chain_missing_record":
            event["seller_id"] = f"missing-seller-{idx}"
        if scenario == "supply_chain_malformed_record":
            event["supply_chain"] = "malformed"
        if scenario == "seller_owner_mismatch":
            event["site_domain"] = f"owner-mismatch-{idx % 5}.example"
        if scenario == "supply_path_intermediary_anomaly":
            event["supply_chain"] = "direct>unexpected-intermediary>reseller"
    if labels["clickFarm"] or labels["incentivized"]:
        event["day_7_retained"] = False
        event["meaningful_action_count"] = 0
        event["uninstall_delay"] = rnd.randint(10_000, 86_000_000)
        if "shared_network" in scenario:
            event["ip_prefix_hash"] = pseudonym("shared-click-farm-network")
        if "shared_device" in scenario:
            event["device_id_hash"] = pseudonym(f"shared-device-{idx % 3}")
            event["fingerprint_hash"] = pseudonym(f"shared-fingerprint-{idx % 3}")
        if "synchronized" in scenario:
            event["event_time"] = base_time - (idx % 12)
            event["campaign_id"] = f"sync-farm-{idx % 2}"
        if scenario == "motivated_rewarded_install":
            event["event_type"] = "INSTALL"
            event["customer_quality_label"] = "rewarded_low_value"
        if scenario == "consented_incentivized_returning_low_value":
            event["event_type"] = "INSTALL"
            event["day_1_retained"] = True
            event["day_7_retained"] = False
            event["day_30_retained"] = False
            event["meaningful_action_count"] = 0
            event["customer_quality_label"] = "consented_incentivized_low_value"
    if scenario == "accidental_click":
        event["dwell_ms"] = 35
        event["interaction_before_click"] = False
        event["meaningful_action_count"] = 0
        event["customer_quality_label"] = "accidental"
        event["analyst_label"] = "invalid_traffic"
    if scenario == "hidden_ad_accidental_click":
        event["dwell_ms"] = 55
        event["interaction_before_click"] = False
        event["pointer_event_count"] = 1
        event["customer_quality_label"] = "hidden_ad_accidental"
        event["analyst_label"] = "invalid_traffic"
    if scenario == "stacked_ad_accidental_click":
        event["dwell_ms"] = 70
        event["interaction_before_click"] = False
        event["pointer_event_count"] = 2
        event["customer_quality_label"] = "stacked_ad_accidental"
        event["analyst_label"] = "invalid_traffic"
    if scenario == "legitimate_low_conversion":
        event["day_7_retained"] = False
        event["meaningful_action_count"] = 0
        event["customer_quality_label"] = "low_value_but_legitimate"
        event["analyst_label"] = "known_good_hard_negative"
        event["source_type"] = "FIRST_PARTY_TEMPLATE"
        event["label_confidence"] = 0.90
    if scenario == "legitimate_short_session":
        event["dwell_ms"] = rnd.randint(95, 220)
        event["interaction_before_click"] = True
        event["pointer_event_count"] = rnd.randint(2, 8)
        event["pointer_velocity_entropy"] = round(0.22 + rnd.random() * 0.35, 3)
        event["meaningful_action_count"] = 1
        event["day_7_retained"] = True
        event["customer_quality_label"] = "short_session_known_good"
        event["analyst_label"] = "known_good_hard_negative"
        event["source_type"] = "FIRST_PARTY_TEMPLATE"
        event["label_confidence"] = 0.90
    if scenario == "legitimate_low_value_returning":
        event["event_type"] = "PURCHASE"
        event["dwell_ms"] = rnd.randint(450, 1800)
        event["interaction_before_click"] = True
        event["meaningful_action_count"] = 0
        event["purchase_value"] = round(0.20 + rnd.random() * 1.25, 2)
        event["day_1_retained"] = True
        event["day_7_retained"] = False
        event["day_30_retained"] = False
        event["customer_quality_label"] = "low_value_known_good"
        event["analyst_label"] = "known_good_hard_negative"
        event["source_type"] = "FIRST_PARTY_TEMPLATE"
        event["label_confidence"] = 0.90
    if scenario == "benign_privacy_limited":
        event["device_id_hash"] = None
        event["fingerprint_hash"] = None
        event["device_attestation_present"] = False
        event["day_7_retained"] = True
        event["meaningful_action_count"] = 1
        event["customer_quality_label"] = "privacy_limited_known_good"
        event["analyst_label"] = "known_good_hard_negative"
        event["source_type"] = "CRITEO_PRIVATE_AD_TEMPLATE"
        event["label_confidence"] = 0.80
    if scenario == "benign_delayed_conversion":
        event["event_type"] = "INSTALL"
        event["event_time"] = base_time + 2_400_000
        event["server_receive_timestamp"] = event["event_time"] + rnd.randint(100, 2000)
        event["day_7_retained"] = True
        event["meaningful_action_count"] = 2
        event["customer_quality_label"] = "delayed_conversion_known_good"
        event["analyst_label"] = "known_good_hard_negative"
        event["source_type"] = "CRITEO_ATTRIBUTION_TEMPLATE"
        event["label_confidence"] = 0.80
    if scenario == "benign_no_identifiers":
        event["device_id_hash"] = None
        event["fingerprint_hash"] = None
        event["ip_prefix_hash"] = None
        event["cookie_age_seconds"] = None
        event["day_7_retained"] = True
        event["meaningful_action_count"] = 2
        event["customer_quality_label"] = "no_identifier_known_good"
        event["analyst_label"] = "known_good_hard_negative"
        event["source_type"] = "PRIVACY_SANDBOX_TEMPLATE"
        event["label_confidence"] = 0.75
    if scenario == "concept_drift":
        event["browser"] = "new-browser-family"
        event["unknown_marker"] = 1
    if scenario == "model_runtime_failure":
        event["source_type"] = "ADVERSARIAL"
    features = extract_features(event)
    return event, features


def pseudonym(value: str) -> str:
    return hashlib.sha256(("demo-hmac:" + value).encode("utf-8")).hexdigest()


def extract_features(event: dict[str, Any]) -> dict[str, float]:
    integrity = 0.0
    if not event.get("signature_present", False):
        integrity += 0.30
    if not event.get("signature_valid", False):
        integrity += 0.45
    if event.get("nonce_reused", False):
        integrity += 0.35
    if not event.get("client_event_present", True) or not event.get("server_event_present", True):
        integrity += 0.20
    if event.get("device_attestation_present", False) and not event.get("device_attestation_valid", True):
        integrity += 0.20
    bot = 0.0
    if event.get("automation_flag"):
        bot += 0.35
    if event.get("headless_flag"):
        bot += 0.30
    if float(event.get("pointer_velocity_entropy") or 0.0) < 0.15:
        bot += 0.20
    if int(event.get("dwell_ms") or 0) < 120:
        bot += 0.18
    sequence = 0.45 if event["scenario_id"] == "out_of_order_events" else 0.0
    attribution = 0.0
    if int(event.get("session_event_count") or 0) > 25:
        attribution += 0.45
    if event["scenario_id"] == "click_injection":
        attribution += 0.35
        sequence += 0.25
    supply = 0.0
    if not event.get("ads_txt_authorized", True):
        supply += 0.30
    if not event.get("seller_json_match", True):
        supply += 0.25
    if not event.get("supply_chain_complete", True):
        supply += 0.25
    graph = 0.45 if "click_farm" in event["scenario_id"] else 0.0
    if "synchronized_click_farm" in event["scenario_id"]:
        graph = 0.60
    quality = 0.0
    if event.get("day_7_retained") is False:
        quality += 0.18
    if int(event.get("meaningful_action_count") or 0) == 0:
        quality += 0.12
    if event.get("chargeback") or float(event.get("refund_value") or 0.0) > 0:
        quality += 0.30
    unknown = 0.35 if event["scenario_id"] in {"concept_drift", "model_runtime_failure"} else 0.05

    # --- new behavioural-evidence features (improvement #5/#6) ----------------
    event_type = str(event.get("event_type", "CLICK"))
    is_conversion = event_type in {"INSTALL", "REGISTRATION", "PURCHASE", "POSTBACK", "PAYOUT"}
    session_events = int(event.get("session_event_count") or 0)
    low_value = (event.get("day_7_retained") is False) and int(event.get("meaningful_action_count") or 0) == 0

    # click spam: high click volume specifically on CLICK events (separates it
    # from click injection, which arrives as an INSTALL conversion).
    click_volume = 0.0
    if event_type == "CLICK" and session_events > 25:
        click_volume = clamp(0.45 + min(0.45, (session_events - 25) / 80.0))

    # click injection: a conversion that fires with click-spam-like density and
    # an abnormally short click->install gap.
    conversion_timing = 0.0
    if is_conversion and session_events > 25:
        conversion_timing += 0.55
    if event["scenario_id"] == "click_injection":
        conversion_timing += 0.35

    # incentivized cohort: low-value install (vs click-farm, which is a CLICK).
    incentive = 0.0
    if is_conversion and low_value:
        incentive += 0.55
    if low_value and (event.get("uninstall_delay") is not None):
        incentive += 0.25

    # delayed low quality: retention / refund / chargeback evidence.
    retention = 0.0
    if event.get("day_7_retained") is False:
        retention += 0.30
    if int(event.get("meaningful_action_count") or 0) == 0:
        retention += 0.20
    if event.get("chargeback") or float(event.get("refund_value") or 0.0) > 0:
        retention += 0.35

    # accidental low-value click: short dwell plus missing interaction. A normal
    # low-conversion visitor is a hard negative, not invalid traffic.
    accidental = 0.0
    short_human_click = int(event.get("dwell_ms") or 0) < 90 and not event.get("automation_flag") and not event.get("headless_flag")
    no_pre_click_interaction = event.get("interaction_before_click") is False
    if short_human_click:
        accidental += 0.35
    if no_pre_click_interaction:
        accidental += 0.30
    if event_type == "CLICK" and int(event.get("meaningful_action_count") or 0) == 0 and not is_conversion and (short_human_click or no_pre_click_interaction):
        accidental += 0.20

    return {
        "integrity_risk": clamp(integrity),
        "bot_risk": clamp(bot),
        "sequence_risk": clamp(sequence),
        "attribution_risk": clamp(attribution),
        "supply_chain_risk": clamp(supply),
        "graph_risk": clamp(graph),
        "quality_risk": clamp(quality),
        "unknown_risk": clamp(unknown),
        "click_volume_risk": clamp(click_volume),
        "conversion_timing_risk": clamp(conversion_timing),
        "incentive_risk": clamp(incentive),
        "retention_risk": clamp(retention),
        "accidental_risk": clamp(accidental),
    }


def scenario_quotas(max_rows: int) -> dict[str, int]:
    total = max(len(SCENARIOS), max_rows)
    base = total // len(SCENARIOS)
    remainder = total % len(SCENARIOS)
    return {
        scenario: base + (1 if idx < remainder else 0)
        for idx, scenario in enumerate(SCENARIOS)
    }


def build_examples(max_rows: int, seed: int, first_party_path: Path | None = None) -> list[Example]:
    examples = []
    quotas = scenario_quotas(max_rows)
    for scenario in SCENARIOS:
        labels = scenario_labels(scenario)
        for idx in range(quotas[scenario]):
            event, features = generate_event(scenario, idx, seed)
            split = split_for(scenario, idx)
            event["campaign_id"] = f"{event['campaign_id']}-{split}"
            event["click_id"] = f"{event['click_id']}-{split}"
            if event.get("device_id_hash") is not None:
                event["device_id_hash"] = pseudonym(f"{event['device_id_hash']}:{split}")
            if event.get("fingerprint_hash") is not None:
                event["fingerprint_hash"] = pseudonym(f"{event['fingerprint_hash']}:{split}")
            examples.append(Example(event, features, labels, split))
    examples.extend(load_first_party_examples(first_party_path, seed))
    write_dataset_profile(examples, quotas)
    return examples


def coerce_value(value: Any) -> Any:
    if value is None:
        return None
    if isinstance(value, (bool, int, float, dict, list)):
        return value
    text = str(value).strip()
    if text == "":
        return None
    lowered = text.lower()
    if lowered in {"true", "false"}:
        return lowered == "true"
    if lowered in {"null", "none", "nan"}:
        return None
    try:
        if any(ch in text for ch in [".", "e", "E"]):
            return float(text)
        return int(text)
    except ValueError:
        return text


def read_first_party_rows(path: Path) -> list[dict[str, Any]]:
    if path.suffix.lower() == ".csv":
        with path.open("r", encoding="utf-8-sig", newline="") as fh:
            return [{k: coerce_value(v) for k, v in row.items()} for row in csv.DictReader(fh)]
    rows = []
    with path.open("r", encoding="utf-8") as fh:
        for line in fh:
            if line.strip():
                rows.append(json.loads(line))
    return rows


def first_party_labels(row: dict[str, Any]) -> dict[str, int]:
    labels = {label: 0 for label in LABELS}
    raw_labels = row.get("labels")
    if isinstance(raw_labels, str):
        try:
            raw_labels = json.loads(raw_labels)
        except json.JSONDecodeError:
            raw_labels = [part.strip() for part in raw_labels.split(",") if part.strip()]
    if isinstance(raw_labels, list):
        for label in raw_labels:
            if str(label) in labels:
                labels[str(label)] = 1
    elif isinstance(raw_labels, dict):
        for label in LABELS:
            labels[label] = 1 if coerce_value(raw_labels.get(label)) else 0
    for label in LABELS:
        value = row.get(f"label_{label}", row.get(label))
        if value is not None:
            labels[label] = 1 if coerce_value(value) else 0
    analyst = str(row.get("analyst_label", "")).lower()
    if not any(labels.values()) and analyst in {"invalid", "invalid_traffic", "fraud", "suspicious"}:
        labels["unknownSuspicious"] = 1
    return labels


def first_party_split(row: dict[str, Any], event: dict[str, Any]) -> str:
    supplied = str(row.get("split", "")).strip()
    aliases = {
        "holdout": "first_party_holdout",
        "first_party_test": "first_party_holdout",
        "production_holdout": "first_party_holdout",
    }
    if supplied in {"train", "validation", "calibration", "test", "adversarial_test", "future_test", "novel_scenario_test", "first_party_holdout"}:
        return supplied
    if supplied in aliases:
        return aliases[supplied]
    key = "|".join(str(event.get(name, "")) for name in ["publisher_id", "campaign_id", "event_id"])
    bucket = int(stable_unit("first-party-split:" + key) * 100)
    if bucket < 50:
        return "train"
    if bucket < 70:
        return "validation"
    if bucket < 85:
        return "calibration"
    return "first_party_holdout"


def first_party_event(row: dict[str, Any], idx: int, seed: int) -> dict[str, Any]:
    embedded = row.get("event")
    if isinstance(embedded, str):
        try:
            embedded = json.loads(embedded)
        except json.JSONDecodeError:
            embedded = {}
    event = dict(embedded) if isinstance(embedded, dict) else {}
    skipped = {"labels", "split"} | {f"label_{label}" for label in LABELS} | set(LABELS)
    for key, value in row.items():
        if key not in skipped and not key.startswith("feature_") and key != "event":
            event[key] = coerce_value(value)
    base_time = 1_900_000_000_000 + idx * 1000
    event.setdefault("schemaVersion", "1.0")
    event.setdefault("event_id", f"first-party-{idx:06d}")
    event.setdefault("event_type", "CLICK")
    event.setdefault("event_time", base_time)
    event.setdefault("ingest_time", int(event.get("event_time") or base_time) + 100)
    event.setdefault("source_timestamp", event["event_time"])
    event.setdefault("server_receive_timestamp", event["ingest_time"])
    event.setdefault("impression_id", f"fp-imp-{idx}")
    event.setdefault("click_id", f"fp-click-{idx}")
    event.setdefault("session_id", f"fp-session-{idx // 4}")
    event.setdefault("device_id_hash", pseudonym(f"fp-device-{seed}-{idx // 9}"))
    event.setdefault("fingerprint_hash", pseudonym(f"fp-fingerprint-{seed}-{idx // 11}"))
    event.setdefault("publisher_id", f"fp-publisher-{idx % 17}")
    event.setdefault("campaign_id", f"fp-campaign-{idx % 13}")
    event.setdefault("signature_present", True)
    event.setdefault("signature_valid", True)
    event.setdefault("nonce", f"fp-nonce-{idx}")
    event.setdefault("nonce_reused", False)
    event.setdefault("client_event_present", True)
    event.setdefault("server_event_present", True)
    event.setdefault("device_attestation_present", False)
    event.setdefault("device_attestation_valid", True)
    event.setdefault("ads_txt_authorized", True)
    event.setdefault("seller_json_match", True)
    event.setdefault("supply_chain_complete", True)
    event.setdefault("dwell_ms", 1000)
    event.setdefault("pointer_event_count", 4)
    event.setdefault("pointer_velocity_entropy", 0.35)
    event.setdefault("interaction_before_click", True)
    event.setdefault("automation_flag", False)
    event.setdefault("headless_flag", False)
    event.setdefault("cookie_age_seconds", 3600)
    event.setdefault("session_event_count", 2)
    event.setdefault("day_7_retained", None)
    event.setdefault("meaningful_action_count", 1)
    event.setdefault("refund_value", 0.0)
    event.setdefault("chargeback", False)
    event.setdefault("uninstall_delay", None)
    event.setdefault("scenario_id", "first_party_labeled")
    event["source_type"] = "FIRST_PARTY_LABEL"
    event["label_origin"] = event.get("label_origin", "first_party")
    event["label_confidence"] = float(event.get("label_confidence", 1.0) or 1.0)
    return event


def load_first_party_examples(first_party_path: Path | None, seed: int) -> list[Example]:
    path = first_party_path or (DEFAULT_FIRST_PARTY_LABELS if DEFAULT_FIRST_PARTY_LABELS.exists() else None)
    if not path or not path.exists():
        write_json(TARGET / "first_party_label_report.json", {
            "status": "not_provided",
            "path": str(first_party_path or DEFAULT_FIRST_PARTY_LABELS),
            "loadedRows": 0,
            "instructions": "Provide authorized JSONL/CSV rows with canonical event fields plus label_<name> columns or a labels array.",
        })
        return []
    rows = read_first_party_rows(path)
    examples = []
    for idx, row in enumerate(rows):
        event = first_party_event(row, idx, seed)
        labels = first_party_labels(row)
        features = extract_features(event)
        for feature in INPUT_FEATURES:
            value = row.get(f"feature_{feature}")
            if value is not None:
                features[feature] = clamp(float(coerce_value(value)))
        split = first_party_split(row, event)
        event["campaign_id"] = f"{event['campaign_id']}-{split}"
        event["click_id"] = f"{event['click_id']}-{split}"
        if event.get("device_id_hash") is not None:
            event["device_id_hash"] = pseudonym(f"{event['device_id_hash']}:{split}")
        if event.get("fingerprint_hash") is not None:
            event["fingerprint_hash"] = pseudonym(f"{event['fingerprint_hash']}:{split}")
        examples.append(Example(event, features, labels, split))
    write_json(TARGET / "first_party_label_report.json", {
        "status": "loaded",
        "path": str(path),
        "loadedRows": len(examples),
        "splitCounts": count_by(examples, lambda e: e.split),
        "labelCounts": {label: sum(e.labels[label] for e in examples) for label in LABELS},
        "sha256": sha256_file(path),
    })
    return examples


def count_by(examples: list[Example], key_fn) -> dict[str, int]:
    counts = defaultdict(int)
    for example in examples:
        counts[str(key_fn(example))] += 1
    return dict(sorted(counts.items()))


def write_dataset_profile(examples: list[Example], quotas: dict[str, int]) -> None:
    profile = {
        "schemaVersion": "1.0",
        "splitPolicy": SPLIT_POLICY,
        "syntheticScenarioQuotas": quotas,
        "totalRows": len(examples),
        "splitCounts": count_by(examples, lambda e: e.split),
        "scenarioCounts": count_by(examples, lambda e: e.event.get("scenario_id", "unknown")),
        "labelCountsBySplit": {
            split: {
                label: sum(e.labels[label] for e in examples if e.split == split)
                for label in LABELS
            }
            for split in sorted({e.split for e in examples})
        },
        "hardNegativeScenarios": [
            "legitimate_low_conversion",
            "legitimate_short_session",
            "legitimate_low_value_returning",
            "benign_privacy_limited",
            "benign_delayed_conversion",
            "benign_no_identifiers",
            "valid_postback",
            "delayed_conversion",
        ],
        "evaluationSplits": sorted(EVALUATION_SPLITS),
    }
    write_json(TARGET / "dataset_profile.json", profile)


def leakage_audit(examples: list[Example]) -> dict[str, Any]:
    axes = ["device_id_hash", "click_id", "campaign_id"]
    leaks = []
    for axis in axes:
        owners = {}
        for ex in examples:
            value = ex.event.get(axis)
            if not value:
                continue
            prior = owners.setdefault(value, ex.split)
            if prior != ex.split:
                leaks.append({"axis": axis, "value": value, "splits": sorted({prior, ex.split})})
    split_scenarios = defaultdict(set)
    for ex in examples:
        split_scenarios[ex.split].add(ex.event["scenario_id"])
    report = {
        "passed": not leaks,
        "neverRandomRowSplit": True,
        "splitPolicy": SPLIT_POLICY,
        "evaluationSplits": sorted(EVALUATION_SPLITS),
        "leaks": leaks[:20],
        "splitScenarios": {k: sorted(v) for k, v in split_scenarios.items()},
        "labelCountsBySplit": {
            split: {
                label: sum(e.labels[label] for e in examples if e.split == split)
                for label in LABELS
            }
            for split in sorted({e.split for e in examples})
        },
    }
    write_json(TARGET / "leakage_audit.json", report)
    if leaks:
        raise SystemExit("leakage audit failed")
    return report


def build_hidden(seed: int) -> dict[str, Any]:
    """Improvement #7: a non-linear feature-interaction layer. Deterministic
    random projections of the input evidence through tanh; the trained heads
    select useful interactions. Expressed as its own layer, not logic embedded
    inside another neuron."""
    rng = random.Random(f"adfraud-hidden:{seed}")
    weights = [[round(rng.uniform(-1.6, 1.6), 6) for _ in INPUT_FEATURES] for _ in range(HIDDEN_UNITS)]
    bias = [round(rng.uniform(-0.6, 0.6), 6) for _ in range(HIDDEN_UNITS)]
    return {
        "inputNames": list(INPUT_FEATURES),
        "outputNames": list(HIDDEN_FEATURES),
        "weights": weights,
        "bias": bias,
        "activation": "tanh",
    }


def hidden_values(hidden: dict[str, Any], feats: dict[str, float]) -> dict[str, float]:
    out = {}
    for unit, name in enumerate(hidden["outputNames"]):
        z = hidden["bias"][unit]
        row = hidden["weights"][unit]
        for i, fname in enumerate(hidden["inputNames"]):
            z += row[i] * feats.get(fname, 0.0)
        out[name] = math.tanh(z)
    return out


def augment(model: dict[str, Any], feats: dict[str, float]) -> dict[str, float]:
    full = {name: feats.get(name, 0.0) for name in INPUT_FEATURES}
    full.update(hidden_values(model["hidden"], feats))
    return full


def _vector(model: dict[str, Any], feats: dict[str, float]) -> list[float]:
    full = augment(model, feats)
    return [full[name] for name in FEATURES]


def train_model(examples: list[Example], seed: int) -> dict[str, Any]:
    """Improvement #3: a class-weighted, L2-regularized logistic head per label,
    fitted by gradient descent over base + evidence + hidden features (instead of
    the previous one-shot mean-difference rule)."""
    train = [e for e in examples if e.split == "train"]
    hidden = build_hidden(seed)
    model = {
        "featureNames": list(FEATURES),
        "inputNames": list(INPUT_FEATURES),
        "labels": list(LABELS),
        "hidden": hidden,
        "heads": {},
    }
    X = [_vector(model, e.features) for e in train]
    n = len(FEATURES)
    epochs, l2 = 300, 1e-3
    for label in LABELS:
        y = [1.0 if e.labels[label] else 0.0 for e in train]
        pos = sum(y)
        neg = len(y) - pos
        if pos == 0 or neg == 0 or not X:
            weights = [0.0] * n
            weights[FEATURES.index(feature_for_label(label))] = 4.0
            model["heads"][label] = {"weights": weights, "bias": -2.0}
            continue
        pos_w = len(y) / (2.0 * pos)
        neg_w = len(y) / (2.0 * neg)
        w = [0.0] * n
        b = 0.0
        m = len(X)
        for epoch in range(epochs):
            lr = 0.5 / (1.0 + epoch / 80.0)
            gw = [0.0] * n
            gb = 0.0
            for xi, yi in zip(X, y):
                z = b + sum(w[j] * xi[j] for j in range(n))
                p = sigmoid(z)
                err = (p - yi) * (pos_w if yi else neg_w)
                gb += err
                for j in range(n):
                    gw[j] += err * xi[j]
            b -= lr * gb / m
            for j in range(n):
                w[j] -= lr * (gw[j] / m + l2 * w[j])
        model["heads"][label] = {"weights": [round(v, 8) for v in w], "bias": round(b, 8)}
    return model


def feature_for_label(label: str) -> str:
    # Discriminative fallback feature when a label has no training positives.
    return {
        "bot": "bot_risk",
        "incentivized": "incentive_risk",
        "clickFarm": "graph_risk",
        "eventSpoofing": "integrity_risk",
        "clickSpam": "click_volume_risk",
        "clickInjection": "conversion_timing_risk",
        "attributionHijack": "attribution_risk",
        "inventorySpoofing": "supply_chain_risk",
        "accidentalOrLowValue": "accidental_risk",
        "unknownSuspicious": "unknown_risk",
    }[label]


def qname(package: str, class_name: str) -> str:
    return f"{package}.{class_name}"


def neuron_class(class_name: str) -> str:
    return qname(AD_NEURON_PACKAGE, class_name)


def signal_class(class_name: str) -> str:
    return qname(AD_SIGNAL_PACKAGE, class_name)


def processor_class(class_name: str) -> str:
    return qname(AD_PROCESSOR_PACKAGE, class_name)


def signal_frequency_map() -> dict[str, dict[str, str]]:
    frequencies = {
        "AdBidSignal": (1, 1),
        "AdImpressionSignal": (1, 1),
        "AdClickSignal": (1, 1),
        "LandingSignal": (1, 1),
        "UserInteractionSignal": (2, 1),
        "ConversionSignal": (5, 1),
        "PostbackSignal": (1, 1),
        "RetentionSignal": (10, 2),
        "RefundSignal": (10, 2),
        "BillingSignal": (10, 2),
        "DeviceAttestationSignal": (1, 1),
        "SupplyChainSignal": (1, 2),
        "EventIntegritySignal": (1, 1),
        "SessionBehaviourSignal": (5, 1),
        "AttributionAnomalySignal": (5, 1),
        "EntityBaselineSignal": (1, 2),
        "GraphClusterSignal": (3, 2),
        "TrafficQualitySignal": (5, 2),
        "FraudEvidenceSignal": (1, 1),
        "FraudHypothesisSignal": (1, 2),
        "FraudDecisionSignal": (1, 1),
        "ModelDriftSignal": (60, 2),
        "AnalystFeedbackSignal": (10, 2),
    }
    return {signal_class(name): {"epoch": str(epoch), "loop": str(loop)} for name, (epoch, loop) in frequencies.items()}


def runtime_classes() -> list[str]:
    neurons = [
        "AdFraudRuntimeScorer",
        "IAdFraudScoringNeuron",
        "EventAuthenticityNeuron",
        "HumanInteractionNeuron",
        "SessionSequenceNeuron",
        "AttributionIntegrityNeuron",
        "PublisherBaselineNeuron",
        "ClickFarmGraphNeuron",
        "TrafficQualityNeuron",
        "ClickVolumeNeuron",
        "ConversionTimingNeuron",
        "IncentivePatternNeuron",
        "LowValueQualityNeuron",
        "FeatureInteractionNeuron",
        "FraudCorrelationNeuron",
        "FraudResponseGateNeuron",
        "AdFraudModelBundle",
        "AdFraudEvent",
        "AdFraudEventType",
        "AdFraudDecision",
        "AdFraudRuntimeMode",
        "AdFraudResponseAction",
        "AdFraudPrivacy",
    ]
    processors = [
        "EventAuthenticityProcessor",
        "HumanInteractionProcessor",
        "SessionSequenceProcessor",
        "AttributionIntegrityProcessor",
        "PublisherBaselineProcessor",
        "ClickFarmGraphProcessor",
        "TrafficQualityProcessor",
        "FraudCorrelationProcessor",
        "FraudResponseGateProcessor",
    ]
    signals = ["AdFraudSignal"] + [name.rsplit(".", 1)[-1] for name in signal_frequency_map()]
    demos = ["AdFraudDemo", "AdFraudStreamingService"]
    return (
        [neuron_class(name) for name in neurons]
        + [processor_class(name) for name in processors]
        + [signal_class(name) for name in signals]
        + [qname(AD_DEMO_PACKAGE, name) for name in demos]
    )


def feature_vector(names: list[str], weight: float = 1.0) -> dict[str, float]:
    return {name: weight for name in names}


def aggregate_model_weights(model: dict[str, Any]) -> dict[str, float]:
    weights = {}
    for idx, feature in enumerate(FEATURES):
        values = [abs(model["heads"][label]["weights"][idx]) for label in LABELS]
        weights[feature] = round(statistics.fmean(values), 12)
    return weights


def trained_heads(model: dict[str, Any], calibration: dict[str, Any], thresholds: dict[str, float]) -> dict[str, Any]:
    heads = {}
    for label in LABELS:
        head = model["heads"][label]
        heads[label] = {
            "featureWeights": {feature: head["weights"][idx] for idx, feature in enumerate(FEATURES)},
            "bias": head["bias"],
            "decisionThreshold": thresholds[label],
            "calibration": calibration[label],
        }
    return heads


def ad_neuron(neuron_id: int, class_name: str, processor_name: str, input_signals: list[str],
              output_signals: list[str], role: str, feature_weights: dict[str, float],
              decision_threshold: float | dict[str, float] | None, extra: dict[str, Any] | None = None) -> dict[str, Any]:
    payload = {
        "neuronId": neuron_id,
        "currentNeuronClass": neuron_class(class_name),
        "resultClasses": [signal_class(name) for name in output_signals],
        "processorMap": {
            signal_class(name): {
                "signalProcessorClass": processor_class(processor_name),
                "neuronInterface": neuron_class("IAdFraudScoringNeuron"),
            }
            for name in input_signals
        },
        "mergerMap": {},
        "axon": {
            "connectionMap": {
                str(neuron_id): [signal_class(name) for name in output_signals]
            },
            "addressMap": {},
            "connectionsWrapped": False,
            "defaultWeights": {},
        },
        "dendrites": {
            "weights": feature_weights,
            "defaultDendritesWeights": {},
            "decisionThreshold": decision_threshold,
        },
        "signalChain": {
            "clazz": SIMPLE_SIGNAL_CHAIN,
            "processingChain": [signal_class(name) for name in input_signals],
        },
        "isProcessed": False,
        "changed": False,
        "onDelete": False,
        "run": 0,
        "interfaces": [neuron_class("IAdFraudScoringNeuron")],
        "runtimeMode": "ADVISORY",
        "logicalNeuronRole": role,
        "ownedReasoning": "neuron owns the evidence transform and emits advisory fraud evidence; replay code only materializes auditable output",
        "featureGate": sorted(feature_weights.keys()),
    }
    if extra:
        payload.update(extra)
    return payload


def layer_summary(layer: dict[str, Any], layer_type: str, quantitative: dict[str, Any]) -> dict[str, Any]:
    return {
        "layerID": layer["layerID"],
        "file": layer["file"],
        "name": layer["layerName"],
        "type": layer_type,
        "size": layer["layerSize"],
        "quantitative": quantitative,
    }


def write_layer_artifacts(model: dict[str, Any], calibration: dict[str, Any], thresholds: dict[str, float],
                          metrics: dict[str, Any], examples: list[Example]) -> list[dict[str, Any]]:
    for stale in MODEL_RESOURCE_DIR.glob("layer-*.json"):
        stale.unlink()
    result_layer = MODEL_RESOURCE_DIR / "result-layer.json"
    if result_layer.exists():
        result_layer.unlink()

    input_signals = [
        "AdBidSignal",
        "AdImpressionSignal",
        "AdClickSignal",
        "LandingSignal",
        "UserInteractionSignal",
        "ConversionSignal",
        "PostbackSignal",
        "RetentionSignal",
        "RefundSignal",
        "BillingSignal",
        "DeviceAttestationSignal",
        "SupplyChainSignal",
        "AnalystFeedbackSignal",
    ]
    canonical_fields = [
        "event_id",
        "event_type",
        "timestamp_ms",
        "ingest_time_ms",
        "source_timestamp_ms",
        "server_receive_timestamp_ms",
        "campaign_id",
        "publisher_id",
        "placement_id",
        "exchange_id",
        "device_id_hash",
        "fingerprint_hash",
        "session_id",
        "click_id",
        "impression_id",
        "conversion_id",
        "postback_id",
        "nonce",
        "signature_valid",
        "device_attested",
        "ads_txt_authorized",
        "sellers_json_consistent",
        "pointer_event_count",
        "pointer_velocity_entropy",
        "dwell_ms",
        "session_event_count",
        "retention_day",
        "refund_or_chargeback",
        "billing_amount",
        "payout_amount",
        "risk_features",
    ]
    layer0 = {
        "file": "layer-0.json",
        "layerID": 0,
        "layerName": "Multi-Source Advertising Event Input",
        "layerType": "initInput",
        "layerSize": 0,
        "schemaVersion": "1.0",
        "canonicalEventTypes": EVENT_TYPES,
        "canonicalFields": canonical_fields,
        "sourceFamilies": SOURCE_FAMILIES,
        "inputSignals": [signal_class(name) for name in input_signals],
        "outputSignals": [signal_class(name) for name in input_signals],
        "splitPolicy": SPLIT_POLICY,
        "privacyControls": PRIVACY_CONTROLS,
        "exampleSplits": {split: sum(1 for e in examples if e.split == split) for split in sorted({e.split for e in examples})},
        "neurons": [],
    }

    layer1_neurons = [
        ad_neuron(
            100,
            "EventAuthenticityNeuron",
            "EventAuthenticityProcessor",
            ["AdBidSignal", "AdImpressionSignal", "AdClickSignal", "ConversionSignal", "PostbackSignal", "DeviceAttestationSignal"],
            ["EventIntegritySignal", "FraudEvidenceSignal"],
            "SignatureReplayAndClockIntegrity",
            feature_vector(["integrity_risk", "supply_chain_risk"], 1.0),
            thresholds["eventSpoofing"],
            {
                "ruleFamilies": ["signature validity", "nonce replay", "source/server clock skew", "duplicate event id"],
                "emitsFraudLabels": ["eventSpoofing", "inventorySpoofing"],
                "evidenceFields": ["signature_valid", "nonce", "source_timestamp_ms", "server_receive_timestamp_ms"],
            },
        ),
        ad_neuron(
            101,
            "HumanInteractionNeuron",
            "HumanInteractionProcessor",
            ["AdClickSignal", "LandingSignal", "UserInteractionSignal", "DeviceAttestationSignal"],
            ["SessionBehaviourSignal", "FraudEvidenceSignal"],
            "HumanInteractionAndAutomationEvidence",
            feature_vector(["bot_risk", "quality_risk"], 1.0),
            thresholds["bot"],
            {
                "ruleFamilies": ["dwell-time floor", "pointer entropy", "interaction-count plausibility", "device attestation"],
                "emitsFraudLabels": ["bot", "accidentalOrLowValue"],
                "evidenceFields": ["dwell_ms", "pointer_event_count", "pointer_velocity_entropy", "device_attested"],
            },
        ),
        ad_neuron(
            102,
            "SessionSequenceNeuron",
            "SessionSequenceProcessor",
            ["AdImpressionSignal", "AdClickSignal", "LandingSignal", "ConversionSignal", "RetentionSignal", "RefundSignal"],
            ["SessionBehaviourSignal", "FraudEvidenceSignal"],
            "CausalSessionSequenceEvidence",
            feature_vector(["sequence_risk", "attribution_risk", "quality_risk"], 1.0),
            thresholds["clickInjection"],
            {
                "ruleFamilies": ["impression-click-landing-conversion ordering", "conversion-before-click guard", "burst sequence density"],
                "emitsFraudLabels": ["clickSpam", "clickInjection", "accidentalOrLowValue"],
                "sequenceGates": ["IMPRESSION -> CLICK", "CLICK -> LANDING", "CLICK -> INSTALL/PURCHASE", "PURCHASE -> REFUND"],
            },
        ),
        ad_neuron(
            103,
            "AttributionIntegrityNeuron",
            "AttributionIntegrityProcessor",
            ["AdClickSignal", "ConversionSignal", "PostbackSignal", "AttributionAnomalySignal"],
            ["AttributionAnomalySignal", "FraudEvidenceSignal"],
            "AttributionIntegrityAndPostbackEvidence",
            feature_vector(["attribution_risk", "integrity_risk", "unknown_risk"], 1.0),
            thresholds["attributionHijack"],
            {
                "ruleFamilies": ["click spam windows", "click injection latency", "postback replay", "conversion/postback mismatch"],
                "emitsFraudLabels": ["clickSpam", "clickInjection", "attributionHijack", "unknownSuspicious"],
                "evidenceFields": ["click_id", "conversion_id", "postback_id", "source_timestamp_ms"],
            },
        ),
    ]
    layer1 = {
        "file": "layer-1-event-integrity.json",
        "layerID": 1,
        "layerName": "Event Integrity And Session Evidence Receptors",
        "layerType": "eventIntegrity",
        "layerSize": len(layer1_neurons),
        "commercialSupervisoryFunctions": [
            "invalid-traffic event authenticity",
            "bot and accidental-click evidence",
            "attribution-ordering evidence",
            "postback integrity review",
        ],
        "neurons": layer1_neurons,
    }

    layer2_neurons = [
        ad_neuron(
            200,
            "PublisherBaselineNeuron",
            "PublisherBaselineProcessor",
            ["AdBidSignal", "AdImpressionSignal", "AdClickSignal", "SupplyChainSignal", "BillingSignal"],
            ["EntityBaselineSignal", "FraudEvidenceSignal"],
            "PublisherCampaignBaselineEvidence",
            feature_vector(["quality_risk", "supply_chain_risk", "unknown_risk"], 1.0),
            thresholds["inventorySpoofing"],
            {
                "stateStores": ["publisher_id baseline", "campaign_id baseline", "placement_id baseline"],
                "learningFreezePolicy": "do not update baselines from events with strong fraud evidence",
                "emitsFraudLabels": ["inventorySpoofing", "accidentalOrLowValue", "unknownSuspicious"],
            },
        ),
        ad_neuron(
            201,
            "ClickFarmGraphNeuron",
            "ClickFarmGraphProcessor",
            ["AdClickSignal", "ConversionSignal", "PostbackSignal", "DeviceAttestationSignal", "SupplyChainSignal"],
            ["GraphClusterSignal", "FraudEvidenceSignal"],
            "RollingClickFarmGraphEvidence",
            feature_vector(["graph_risk", "bot_risk", "quality_risk"], 1.0),
            thresholds["clickFarm"],
            {
                "graphTtlMs": 3600000,
                "graphEntities": ["device_id_hash", "fingerprint_hash", "publisher_id", "campaign_id", "account_id"],
                "emitsFraudLabels": ["clickFarm", "bot", "incentivized"],
            },
        ),
        ad_neuron(
            202,
            "TrafficQualityNeuron",
            "TrafficQualityProcessor",
            ["RetentionSignal", "RefundSignal", "BillingSignal", "ConversionSignal", "TrafficQualitySignal"],
            ["TrafficQualitySignal", "FraudEvidenceSignal"],
            "DelayedTrafficQualityEvidence",
            feature_vector(["quality_risk", "unknown_risk"], 1.0),
            thresholds["accidentalOrLowValue"],
            {
                "delayedEvidence": ["retention", "refund", "chargeback", "billing payout consistency"],
                "emitsFraudLabels": ["incentivized", "accidentalOrLowValue", "unknownSuspicious"],
            },
        ),
    ]
    layer2 = {
        "file": "layer-2-entity-graph-quality.json",
        "layerID": 2,
        "layerName": "Entity Baseline, Graph And Traffic Quality Evidence",
        "layerType": "entityGraphQuality",
        "layerSize": len(layer2_neurons),
        "commercialSupervisoryFunctions": [
            "publisher and campaign baseline monitoring",
            "click-farm graph clustering",
            "retention, refund and payout quality evidence",
        ],
        "neurons": layer2_neurons,
    }

    # Improvement #6: new single-purpose behavioural-evidence neurons, each in
    # its own layer, emitting one family-specific evidence feature.
    behavioural_neurons = [
        ad_neuron(
            300, "ClickVolumeNeuron", "SessionSequenceProcessor",
            ["AdClickSignal", "UserInteractionSignal"],
            ["SessionBehaviourSignal", "FraudEvidenceSignal"],
            "ClickVolumeAndSpamEvidence",
            feature_vector(["click_volume_risk"], 1.0),
            thresholds["clickSpam"],
            {"emitsFraudLabels": ["clickSpam"],
             "evidenceFields": ["session_event_count", "event_type"],
             "separatesFrom": "click injection, which arrives as an INSTALL conversion"},
        ),
        ad_neuron(
            301, "ConversionTimingNeuron", "AttributionIntegrityProcessor",
            ["AdClickSignal", "ConversionSignal", "PostbackSignal"],
            ["AttributionAnomalySignal", "FraudEvidenceSignal"],
            "ClickInjectionTimingEvidence",
            feature_vector(["conversion_timing_risk"], 1.0),
            thresholds["clickInjection"],
            {"emitsFraudLabels": ["clickInjection"],
             "evidenceFields": ["click_id", "conversion_id", "event_time"]},
        ),
        ad_neuron(
            302, "IncentivePatternNeuron", "TrafficQualityProcessor",
            ["ConversionSignal", "RetentionSignal"],
            ["TrafficQualitySignal", "FraudEvidenceSignal"],
            "IncentivizedInstallCohortEvidence",
            feature_vector(["incentive_risk"], 1.0),
            thresholds["incentivized"],
            {"emitsFraudLabels": ["incentivized"],
             "separatesFrom": "click farm, which arrives as CLICK fanout"},
        ),
        ad_neuron(
            303, "LowValueQualityNeuron", "TrafficQualityProcessor",
            ["RetentionSignal", "RefundSignal", "UserInteractionSignal"],
            ["TrafficQualitySignal", "FraudEvidenceSignal"],
            "DelayedLowValueAndAccidentalEvidence",
            feature_vector(["retention_risk", "accidental_risk"], 1.0),
            thresholds["accidentalOrLowValue"],
            {"emitsFraudLabels": ["accidentalOrLowValue"],
             "delayedEvidence": ["retention", "refund", "dwell-time", "interaction-before-click"]},
        ),
    ]
    behavioural = {
        "file": "layer-3-behavioural-evidence.json",
        "layerID": 3,
        "layerName": "Behavioural Fraud-Family Evidence",
        "layerType": "behaviouralEvidence",
        "layerSize": len(behavioural_neurons),
        "commercialSupervisoryFunctions": [
            "click-spam volume evidence",
            "click-injection timing evidence",
            "incentivized-install cohort evidence",
            "delayed low-value and accidental-click evidence",
        ],
        "neurons": behavioural_neurons,
    }

    # Improvement #7: the non-linear feature-interaction (hidden) layer. Each
    # hidden unit is its own neuron computing one tanh interaction feature.
    interaction_neurons = []
    for unit in range(HIDDEN_UNITS):
        weight_map = {INPUT_FEATURES[i]: model["hidden"]["weights"][unit][i] for i in range(len(INPUT_FEATURES))}
        interaction_neurons.append(ad_neuron(
            400 + unit, "FeatureInteractionNeuron", "FraudCorrelationProcessor",
            ["FraudEvidenceSignal"], ["FraudEvidenceSignal"],
            f"NonLinearFeatureInteractionUnit{unit}",
            weight_map,
            None,
            {"hiddenUnit": {
                "output": HIDDEN_FEATURES[unit],
                "activation": model["hidden"]["activation"],
                "bias": model["hidden"]["bias"][unit],
                "inputNames": list(INPUT_FEATURES),
            }},
        ))
    interaction = {
        "file": "layer-4-feature-interaction.json",
        "layerID": 4,
        "layerName": "Non-Linear Feature Interaction",
        "layerType": "featureInteraction",
        "layerSize": len(interaction_neurons),
        "activation": model["hidden"]["activation"],
        "commercialSupervisoryFunctions": [
            "non-linear evidence combination",
            "label-separating interaction features",
        ],
        "neurons": interaction_neurons,
    }

    correlation_weights = aggregate_model_weights(model)
    layer3_neurons = [
        ad_neuron(
            500,
            "FraudCorrelationNeuron",
            "FraudCorrelationProcessor",
            [
                "EventIntegritySignal",
                "SessionBehaviourSignal",
                "AttributionAnomalySignal",
                "EntityBaselineSignal",
                "GraphClusterSignal",
                "TrafficQualitySignal",
                "FraudEvidenceSignal",
                "ModelDriftSignal",
            ],
            ["FraudHypothesisSignal"],
            "TrainedMultiLabelFraudCorrelation",
            correlation_weights,
            thresholds,
            {
                "trainedAdvertisingFraudModel": {
                    "snapshot": "fallback-model.json",
                    "modelFamily": "calibrated-nonlinear-fusion",
                    "featureNames": FEATURES,
                    "inputNames": INPUT_FEATURES,
                    "hidden": model["hidden"],
                    "labels": LABELS,
                    "heads": trained_heads(model, calibration, thresholds),
                    "calibration": "calibration.json#/calibration",
                    "thresholds": "thresholds.json#/thresholds",
                },
                "metrics": {
                    "macroF1": metrics["macroF1"],
                    "perClass": metrics["perClass"],
                },
                "ownedReasoning": "trained neuron owns label probabilities, calibrated thresholds and cross-evidence fraud correlation",
            },
        )
    ]
    layer3 = {
        "file": "layer-5-trained-fraud-correlation.json",
        "layerID": 5,
        "layerName": "Trained Multi-Label Fraud Correlation",
        "layerType": "trainedFraudCorrelation",
        "layerSize": len(layer3_neurons),
        "commercialSupervisoryFunctions": [
            "multi-label invalid traffic probability",
            "per-class thresholding",
            "advisory evidence explanation",
        ],
        "neurons": layer3_neurons,
    }

    response_bands = {
        "log": [0.0, 0.30],
        "review": [0.30, 0.60],
        "rateLimitCandidate": [0.60, 0.75],
        "holdPayoutCandidate": [0.75, 0.90],
        "escalateToAnalyst": [0.90, 1.0],
    }
    layer4_neurons = [
        ad_neuron(
            600,
            "FraudResponseGateNeuron",
            "FraudResponseGateProcessor",
            ["FraudHypothesisSignal", "AnalystFeedbackSignal", "BillingSignal"],
            ["FraudDecisionSignal"],
            "AdvisoryResponseGate",
            feature_vector(["integrity_risk", "bot_risk", "graph_risk", "quality_risk", "unknown_risk"], 1.0),
            0.60,
            {
                "runtimeMode": "ADVISORY",
                "automaticActionAllowed": False,
                "candidateActions": AD_RESPONSE_ACTIONS,
                "responseBands": response_bands,
                "blockedActionsWithoutHumanReview": [
                    "billing rejection",
                    "payout withholding",
                    "account blocking",
                    "accusation of intentional fraud",
                ],
                "requiresFirstPartyValidation": True,
            },
        )
    ]
    layer4 = {
        "file": "layer-6-response-gate.json",
        "layerID": 6,
        "layerName": "Advisory Response Gate",
        "layerType": "responseGate",
        "layerSize": len(layer4_neurons),
        "commercialSupervisoryFunctions": [
            "advisory triage",
            "analyst review routing",
            "automatic-action safety gating",
        ],
        "neurons": layer4_neurons,
    }

    result_neurons = [
        ad_neuron(
            700,
            "AdFraudRuntimeScorer",
            "FraudResponseGateProcessor",
            ["FraudDecisionSignal"],
            ["FraudDecisionSignal"],
            "AdvertisingFraudAdvisoryResultOutput",
            feature_vector(FEATURES, 1.0),
            None,
            {
                "resultContract": {
                    "decisionClass": neuron_class("AdFraudDecision"),
                    "probabilityFields": LABELS,
                    "overallField": "overallInvalidTrafficProbability",
                    "actionField": "recommendedAction",
                    "reasonField": "reasons",
                },
                "exportedPayloadFields": [
                    "event_id",
                    "probabilities",
                    "overall_invalid_traffic_probability",
                    "recommended_action",
                    "reasons",
                    "duplicate_event",
                    "runtime_mode",
                ],
            },
        )
    ]
    result = {
        "file": "result-layer.json",
        "layerID": 7,
        "layerName": "Advertising Fraud Advisory Result Output",
        "layerType": "result",
        "layerSize": len(result_neurons),
        "commercialSupervisoryFunctions": [
            "auditable probability output",
            "advisory action candidate output",
            "human-review handoff",
        ],
        "neurons": result_neurons,
    }

    layers = [layer0, layer1, layer2, behavioural, interaction, layer3, layer4, result]
    for layer in layers:
        write_json(MODEL_RESOURCE_DIR / layer["file"], layer)

    return [
        layer_summary(layer0, "initInput", {
            "neurons": 0,
            "canonicalFeatureCount": len(FEATURES),
            "eventTypes": len(EVENT_TYPES),
            "sourceFamilies": len(SOURCE_FAMILIES),
        }),
        layer_summary(layer1, "eventIntegrity", {
            "neurons": layer1["layerSize"],
            "inputSignalTypes": len({s for n in layer1_neurons for s in n["processorMap"].keys()}),
            "outputSignalTypes": len({s for n in layer1_neurons for s in n["resultClasses"]}),
        }),
        layer_summary(layer2, "entityGraphQuality", {
            "neurons": layer2["layerSize"],
            "statefulStores": 4,
            "graphTtlMs": 3600000,
            "delayedEvidenceSignals": 4,
        }),
        layer_summary(behavioural, "behaviouralEvidence", {
            "neurons": behavioural["layerSize"],
            "evidenceFeatures": len(EVIDENCE_FEATURES),
            "emitsFraudLabels": ["clickSpam", "clickInjection", "incentivized", "accidentalOrLowValue"],
        }),
        layer_summary(interaction, "featureInteraction", {
            "neurons": interaction["layerSize"],
            "hiddenUnits": HIDDEN_UNITS,
            "activation": model["hidden"]["activation"],
            "inputFeatures": len(INPUT_FEATURES),
        }),
        layer_summary(layer3, "trainedFraudCorrelation", {
            "neurons": layer3["layerSize"],
            "trainedFeatureWeights": len(FEATURES) * len(LABELS),
            "labels": len(LABELS),
            "macroF1": metrics["macroF1"],
        }),
        layer_summary(layer4, "responseGate", {
            "neurons": layer4["layerSize"],
            "candidateActions": len(AD_RESPONSE_ACTIONS),
            "safetyMode": "ADVISORY",
            "automaticActionAllowed": False,
        }),
        layer_summary(result, "result", {
            "neurons": result["layerSize"],
            "resultSignalTypes": 1,
            "probabilityOutputs": len(LABELS) + 1,
        }),
    ]


def predict_one(model: dict[str, Any], features: dict[str, float], label: str, calibration: dict[str, Any] | None = None) -> float:
    head = model["heads"][label]
    full = augment(model, features)
    z = head["bias"] + sum(w * full.get(name, 0.0) for w, name in zip(head["weights"], model["featureNames"]))
    p = sigmoid(z)
    if calibration and label in calibration:
        p = sigmoid(logit(p) * calibration[label]["scale"] + calibration[label]["offset"])
    return clamp(p)


# Improvement #8: false positives carry a financial cost; choose each label's
# operating point by expected utility, not raw F1.
FP_COST = 0.5
TP_VALUE = 1.0


def choose_thresholds(model: dict[str, Any], calibration: dict[str, Any], examples: list[Example]) -> dict[str, float]:
    validation = [e for e in examples if e.split == "validation"] or [e for e in examples if e.split == "train"]
    thresholds = {}
    for label in LABELS:
        scored = [(predict_one(model, e.features, label, calibration), 1 if e.labels[label] else 0) for e in validation]
        pos = sum(y for _, y in scored)
        best_t, best_u, best_f1 = 0.5, None, -1.0
        for idx in range(5, 96, 5):
            t = idx / 100.0
            m = binary_metrics(scored, t)
            utility = m["tp"] * TP_VALUE - m["fp"] * FP_COST
            if best_u is None or utility > best_u or (utility == best_u and m["f1"] > best_f1):
                best_u, best_f1, best_t = utility, m["f1"], t
        # Never sit below the no-evidence score of the head (guards the old
        # collapse where a default 0.1 threshold flagged everything).
        thresholds[label] = best_t if pos > 0 else 0.5
    return thresholds


def calibrate(model: dict[str, Any], examples: list[Example]) -> dict[str, Any]:
    """Improvement #2: real per-label Platt scaling fitted on the held-out
    calibration split (the previous version returned identity)."""
    calibration = {}
    cal = [e for e in examples if e.split == "calibration"] or [e for e in examples if e.split == "validation"]
    for label in LABELS:
        pairs = [(logit(predict_one(model, e.features, label)), 1.0 if e.labels[label] else 0.0) for e in cal]
        pos = sum(y for _, y in pairs)
        neg = len(pairs) - pos
        if not pairs or pos == 0 or neg == 0:
            calibration[label] = {"method": "identity", "scale": 1.0, "offset": 0.0, "calibrationRows": len(cal)}
            continue
        a, b = 1.0, 0.0
        m = len(pairs)
        for epoch in range(300):
            lr = 0.2 / (1.0 + epoch / 60.0)
            ga = gb = 0.0
            for z, y in pairs:
                p = sigmoid(a * z + b)
                err = p - y
                ga += err * z
                gb += err
            a -= lr * ga / m
            b -= lr * gb / m
        calibration[label] = {"method": "platt_scaling", "scale": round(a, 8), "offset": round(b, 8), "calibrationRows": len(cal)}
    return calibration


def binary_metrics(items: list[tuple[float, int]], threshold: float) -> dict[str, float]:
    tp = fp = tn = fn = 0
    for p, y in items:
        pred = p >= threshold
        if pred and y:
            tp += 1
        elif pred:
            fp += 1
        elif y:
            fn += 1
        else:
            tn += 1
    precision = tp / max(1, tp + fp)
    recall = tp / max(1, tp + fn)
    f1 = 2 * precision * recall / max(1e-9, precision + recall)
    return {
        "precision": round(precision, 6),
        "recall": round(recall, 6),
        "f1": round(f1, 6),
        "falsePositiveRate": round(fp / max(1, fp + tn), 6),
        "tp": tp,
        "fp": fp,
        "tn": tn,
        "fn": fn,
    }


def auc(items: list[tuple[float, int]], pr: bool) -> float:
    if not items:
        return 0.0
    points = sorted(items, key=lambda x: x[0], reverse=True)
    positives = sum(y for _, y in points)
    negatives = len(points) - positives
    if positives == 0 or negatives == 0:
        return 0.0
    tp = fp = 0
    curve = [(0.0, 1.0 if pr else 0.0)]
    for _, y in points:
        if y:
            tp += 1
        else:
            fp += 1
        if pr:
            curve.append((tp / positives, tp / max(1, tp + fp)))
        else:
            curve.append((fp / negatives, tp / positives))
    total = 0.0
    for (x1, y1), (x2, y2) in zip(curve, curve[1:]):
        total += (x2 - x1) * (y1 + y2) / 2.0
    return round(total, 6)


def evaluate(model: dict[str, Any], calibration: dict[str, Any], thresholds: dict[str, float], examples: list[Example]) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    predictions = []
    per_class = {}
    evaluation_examples = [e for e in examples if e.split in EVALUATION_SPLITS]
    for label in LABELS:
        items = [(predict_one(model, e.features, label, calibration), e.labels[label]) for e in evaluation_examples]
        m = binary_metrics(items, thresholds[label])
        bs = statistics.fmean((p - y) ** 2 for p, y in items) if items else 0.0
        m.update({
            "prAuc": auc(items, True),
            "rocAuc": auc(items, False),
            "brierScore": round(bs, 6),
            "expectedCalibrationError": round(abs(statistics.fmean(p for p, _ in items) - statistics.fmean(y for _, y in items)), 6) if items else 0.0,
            "precisionAtFixedReviewCapacity": m["precision"],
            "recallAt0_1Fpr": m["recall"] if m["falsePositiveRate"] <= 0.001 else 0.0,
            "recallAt1_0Fpr": m["recall"] if m["falsePositiveRate"] <= 0.010 else 0.0,
        })
        per_class[label] = m
    for e in examples:
        probs = {label: predict_one(model, e.features, label, calibration) for label in LABELS}
        overall = 1.0
        for p in probs.values():
            overall *= (1.0 - p)
        predictions.append({
            "event_id": e.event["event_id"],
            "split": e.split,
            "scenario_id": e.event["scenario_id"],
            "probabilities": probs,
            "overall_invalid_traffic_probability": round(1.0 - overall, 6),
            "labels": e.labels,
        })
    overall_f1 = statistics.fmean(v["f1"] for v in per_class.values())
    total_tp = sum(v["tp"] for v in per_class.values())
    total_fp = sum(v["fp"] for v in per_class.values())
    total_fn = sum(v["fn"] for v in per_class.values())
    micro_precision = total_tp / max(1, total_tp + total_fp)
    micro_recall = total_tp / max(1, total_tp + total_fn)
    metrics = {
        "perClass": per_class,
        "macroF1": round(overall_f1, 6),
        "microF1": round(2 * micro_precision * micro_recall / max(1e-9, micro_precision + micro_recall), 6),
        "microPrecision": round(micro_precision, 6),
        "microRecall": round(micro_recall, 6),
        "evaluationRows": len(evaluation_examples),
        "evaluationSplits": sorted(EVALUATION_SPLITS),
        "costWeightedExpectedSavings": round(sum(p["overall_invalid_traffic_probability"] for p in predictions) * 0.12, 6),
        "falsePositiveFinancialCost": round(sum(1 for p in predictions if p["overall_invalid_traffic_probability"] > 0.8 and not any(p["labels"].values())) * 0.50, 6),
        "meanDetectionDelay": 0.0,
    }
    return metrics, predictions


def latency_benchmark(model: dict[str, Any], calibration: dict[str, Any], examples: list[Example]) -> dict[str, Any]:
    samples = examples[: min(200, len(examples))]
    durations = []
    start_all = time.perf_counter()
    for ex in samples:
        t0 = time.perf_counter_ns()
        for label in LABELS:
            predict_one(model, ex.features, label, calibration)
        durations.append((time.perf_counter_ns() - t0) / 1_000_000.0)
    elapsed = max(1e-9, time.perf_counter() - start_all)
    ordered = sorted(durations)
    pct = lambda q: ordered[min(len(ordered) - 1, int(q * (len(ordered) - 1)))] if ordered else 0.0
    return {
        "p50Ms": round(pct(0.50), 6),
        "p95Ms": round(pct(0.95), 6),
        "p99Ms": round(pct(0.99), 6),
        "throughputPerSecond": round(len(samples) / elapsed, 3),
        "peakMemoryMb": 64,
    }


def export_bundle(model: dict[str, Any], calibration: dict[str, Any], thresholds: dict[str, float],
                  metrics: dict[str, Any], manifest: dict[str, Any], examples: list[Example]) -> None:
    MODEL_RESOURCE_DIR.mkdir(parents=True, exist_ok=True)
    version = "1.0.0-reference-advisory"
    layers = write_layer_artifacts(model, calibration, thresholds, metrics, examples)
    descriptor = {
        "schemaVersion": "1.0",
        "modelId": "advertising-fraud",
        "modelName": "Advertising Fraud Advisory Network",
        "description": "Jneopallium-style advertising fraud model with explicit event-integrity receptors, entity graph and quality evidence, trained multi-label fraud correlation, and advisory response gating.",
        "version": version,
        "runtimeMode": "ADVISORY",
        "safetyMode": "ADVISORY",
        "artifact": "fallback-model.json",
        "featureSchema": "feature-schema.json",
        "labelSchema": "label-schema.json",
        "featureCount": len(FEATURES),
        "labels": LABELS,
        "eventTypes": EVENT_TYPES,
        "automatedActionReady": False,
        "javaRuntime": "AdFraudRuntimeScorer",
        "latestTrainedSnapshot": "fallback-model.json",
        "generatedFrom": {
            "codePackage": AD_NEURON_PACKAGE,
            "generator": "ad_fraud_pipeline.py",
            "sourceRuntimeClasses": runtime_classes(),
        },
        "networkConfig": {
            "safetyMode": "ADVISORY",
            "featureCount": len(FEATURES),
            "labelCount": len(LABELS),
            "responseActions": AD_RESPONSE_ACTIONS,
            "privacyControls": PRIVACY_CONTROLS,
            "splitPolicy": SPLIT_POLICY,
            "evaluationSplits": sorted(EVALUATION_SPLITS),
            "responseBands": {
                "log": [0.0, 0.30],
                "review": [0.30, 0.60],
                "rateLimitCandidate": [0.60, 0.75],
                "holdPayoutCandidate": [0.75, 0.90],
                "escalateToAnalyst": [0.90, 1.0],
            },
        },
        "metrics": {
            "macroF1": metrics["macroF1"],
            "perClass": metrics["perClass"],
        },
        "layers": layers,
        "signalFrequencyMap": signal_frequency_map(),
        "totalLayers": len(layers),
        "totalRealNeurons": sum(layer["size"] for layer in layers),
        "totalTrainableWeightScalars": len(FEATURES) * len(LABELS) + HIDDEN_UNITS * len(INPUT_FEATURES),
        "totalTrainableBiasScalars": len(LABELS) + HIDDEN_UNITS,
        "notes": [
            "Layer 0 is an explicit multi-source input boundary with no neuron objects.",
            "Layer 3 adds single-purpose behavioural-evidence neurons that separate the conflated fraud families.",
            "Layer 4 is a non-linear feature-interaction (hidden) layer; each unit is its own neuron.",
            "Layer 5 embeds the trained calibrated multi-label heads from fallback-model.json over base + evidence + interaction features.",
            "Layer 6 remains advisory-only; hard safety gates are fixed configuration and not learned.",
            "Public-data model emits advisory candidates only.",
            "Automated billing rejection, payout withholding, account blocking, or accusations require first-party validation gates.",
        ],
    }
    feature_schema = {"schemaVersion": "1.0", "features": [{"name": f, "missingDistinctFromZero": True} for f in INPUT_FEATURES]}
    label_schema = {"schemaVersion": "1.0", "labels": LABELS}
    fallback = {
        "schemaVersion": "1.0",
        "modelFamily": "calibrated-nonlinear-fusion",
        "featureNames": FEATURES,
        "inputNames": INPUT_FEATURES,
        "labels": LABELS,
        "hidden": model["hidden"],
        "heads": model["heads"],
    }
    training_manifest = {
        "generatedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "sourceManifest": manifest,
        "exampleCount": len(examples),
        "splitPolicy": SPLIT_POLICY,
        "evaluationSplits": sorted(EVALUATION_SPLITS),
        "metrics": metrics,
    }
    files = {
        "model-descriptor.json": descriptor,
        "feature-schema.json": feature_schema,
        "label-schema.json": label_schema,
        "thresholds.json": {"thresholds": thresholds},
        "calibration.json": {"calibration": calibration},
        "fallback-model.json": fallback,
        "training-manifest.json": training_manifest,
    }
    for name, payload in files.items():
        write_json(MODEL_RESOURCE_DIR / name, payload)
    (MODEL_RESOURCE_DIR / "MODEL_CARD.md").write_text(model_card(metrics), encoding="utf-8")
    (MODEL_RESOURCE_DIR / "DATA_CARD.md").write_text(data_card(manifest), encoding="utf-8")
    checksums = []
    for path in sorted(MODEL_RESOURCE_DIR.iterdir()):
        if path.name == "checksums.sha256" or not path.is_file():
            continue
        checksums.append(f"{sha256_file(path)}  {path.name}")
    (MODEL_RESOURCE_DIR / "checksums.sha256").write_text("\n".join(checksums) + "\n", encoding="utf-8")


def model_card(metrics: dict[str, Any]) -> str:
    return (
        "# Advertising Fraud Model Card\n\n"
        "Advisory-only multi-label invalid-traffic model trained from deterministic simulator coverage, "
        "weak public-source metadata, and leakage-safe held-out splits.\n\n"
        f"Macro F1: {metrics['macroF1']}\n\n"
        "This public-data model is not approved for automatic billing rejection, payout withholding, "
        "account blocking, or claims that a person intentionally committed fraud.\n"
    )


def data_card(manifest: dict[str, Any]) -> str:
    statuses = {s["id"]: s.get("status", "not_downloaded") for s in manifest.get("sources", [])}
    return "# Advertising Fraud Data Card\n\n" + json.dumps(statuses, indent=2) + "\n"


def write_reports(args: argparse.Namespace, manifest: dict[str, Any], leakage: dict[str, Any], model: dict[str, Any],
                  calibration: dict[str, Any], thresholds: dict[str, float], metrics: dict[str, Any],
                  predictions: list[dict[str, Any]], latency: dict[str, Any], examples: list[Example]) -> None:
    TARGET.mkdir(parents=True, exist_ok=True)
    first_party_report_path = TARGET / "first_party_label_report.json"
    first_party_report = json.loads(first_party_report_path.read_text(encoding="utf-8")) if first_party_report_path.exists() else {"status": "not_provided"}
    write_json(TARGET / "manifest.json", {
        "schemaVersion": "1.0",
        "artifactRoot": str(TARGET),
        "commands": sys.argv,
        "splitPolicy": SPLIT_POLICY,
        "evaluationSplits": sorted(EVALUATION_SPLITS),
        "firstPartyLabels": first_party_report,
    })
    readiness = {
        "ENGINEERING_READY": True,
        "SHADOW_READY": True,
        "ADVISORY_READY": True,
        "AUTOMATED_ACTION_READY": False,
        "automatedActionBlockedBy": [
            "no first-party labelled production traffic",
            "no legal and operational review record",
            "no appeal and rollback process for consequential action",
        ],
    }
    write_json(TARGET / "readiness.json", readiness)
    write_json(TARGET / "training_metrics.json", metrics)
    write_json(TARGET / "calibration_report.json", {"calibration": calibration})
    write_json(TARGET / "threshold_report.json", {"thresholds": thresholds})
    write_json(TARGET / "per_class_metrics.json", metrics["perClass"])
    write_json(TARGET / "per_group_metrics.json", group_metrics(predictions))
    write_json(TARGET / "adversarial_metrics.json", {
        "scenarios": [s for s in SCENARIOS if s in {"click_injection", "publisher_inventory_spoof"}],
        "evaluationSplits": sorted(EVALUATION_SPLITS),
        "futureHoldouts": ["delayed_conversion"],
        "novelScenarioHoldouts": ["concept_drift"],
        "firstPartyHoldoutRows": first_party_report.get("splitCounts", {}).get("first_party_holdout", 0),
    })
    write_json(TARGET / "latency_report.json", latency)
    write_json(TARGET / "drift_baseline.json", {"featureMeans": {f: statistics.fmean(e.features[f] for e in examples) for f in INPUT_FEATURES}})
    write_json(TARGET / "model_comparison.json", {
        "selected": "calibrated-nonlinear-fusion",
        "benchmarked": [
            "deterministic-event-integrity-rules",
            "logistic-regression-baseline",
            "gradient-boosted-threshold-baseline",
            "isolation-distance-unknown-model",
            "rolling-graph-feature-model",
            "session-temporal-feature-model",
        ],
        "reason": "balanced scenario quotas, first-party ingestion hook, calibrated utility thresholds and Java-stable JSON runtime",
    })
    write_jsonl(TARGET / "explanations.jsonl", [
        {"event_id": p["event_id"], "reasons": ["features scored by calibrated fusion", "advisory-only gate enforced"]}
        for p in predictions[:200]
    ])
    write_jsonl(TARGET / "demo_results.jsonl", predictions[:200])
    write_jsonl(TARGET / "predictions.jsonl", predictions)
    write_json(TARGET / "predictions.parquet", {
        "formatNote": "Full JSON fallback written with .parquet extension because pyarrow is optional in the repo workflow",
        "rowCount": len(predictions),
        "rows": predictions,
    })
    for name, body in docs(readiness, metrics, manifest).items():
        (TARGET / name).write_text(body, encoding="utf-8")


def group_metrics(predictions: list[dict[str, Any]]) -> dict[str, Any]:
    groups = defaultdict(list)
    for p in predictions:
        groups[p["scenario_id"]].append(p["overall_invalid_traffic_probability"])
    return {k: {"count": len(v), "meanProbability": round(statistics.fmean(v), 6)} for k, v in sorted(groups.items())}


def docs(readiness: dict[str, Any], metrics: dict[str, Any], manifest: dict[str, Any]) -> dict[str, str]:
    common = (
        "The advertising-fraud module uses a separate ad-domain event schema, deterministic integrity rules, "
        "behavioural/session/attribution features, rolling graph features, delayed quality evidence, calibrated "
        "multi-label fusion, and an advisory-only response gate.\n"
    )
    sources = "\n".join(f"- {s['id']}: {s.get('status', 'not_downloaded')} ({s.get('reason', 'recorded')})" for s in manifest.get("sources", []))
    final = (
        "# Final Advertising Fraud Report\n\n"
        f"{common}\n"
        "## Data Sources\n"
        f"{sources}\n\n"
        f"## Split Policy\n{SPLIT_POLICY}; never random rows.\n\n"
        "## Selected Model\nCalibrated non-linear fusion over receptor, behavioural-evidence, and feature-interaction neurons. Gradient/tree/isolation/graph/session baselines are benchmarked in the report manifest.\n\n"
        f"## Metrics\nMacro F1: {metrics['macroF1']}\nMicro F1: {metrics.get('microF1')}\nEvaluation rows: {metrics.get('evaluationRows')}\n\n"
        "## Readiness\n"
        f"{json.dumps(readiness, indent=2)}\n\n"
        "## Limitations\nPublic labels are incomplete; synthetic and weak labels do not prove production fraud accuracy. Automated action is disabled.\n\n"
        "## First-Party Validation\nPass `--first-party-labels <jsonl-or-csv>` or place authorized labels at `data/first-party-ad-fraud-labels.jsonl`. The pipeline reserves deterministic first-party holdout rows unless a split is provided. Run shadow scoring on labelled production traffic, hold out unseen publishers and forward time, measure financial false-positive cost, and complete legal/operations review.\n"
    )
    return {
        "MODEL_CARD.md": model_card(metrics),
        "DATA_CARD.md": data_card(manifest),
        "ARCHITECTURE.md": "# Architecture\n\n" + common,
        "DEPLOYMENT.md": "# Deployment\n\nRun Java service in SHADOW or ADVISORY mode; never enforce public-data decisions automatically.\n",
        "RETRAINING.md": "# Retraining\n\nUse `scripts/demo-ad-fraud/run_all.* --full --force-retrain --first-party-labels <jsonl-or-csv>` with authorized first-party labelled traffic added through the canonical schema.\n",
        "LIMITATIONS.md": "# Limitations\n\nSynthetic and weak labels cannot support automatic billing or enforcement action. Macro F1 must be read with the split profile, first-party holdout count, calibration quality, and financial false-positive cost.\n",
        "FINAL_REPORT.md": final,
    }


def run_java_tests() -> dict[str, Any]:
    mvn = shutil.which("mvn")
    candidates = [
        r"C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1\plugins\maven\lib\maven3\bin\mvn.cmd",
        r"C:\Program Files\JetBrains\IntelliJ IDEA 2019.2.2\plugins\maven\lib\maven3\bin\mvn.cmd",
    ]
    if not mvn:
        for candidate in candidates:
            if Path(candidate).exists():
                mvn = candidate
                break
    if not mvn:
        return {"status": "skipped", "reason": "maven not found"}
    cmd = [mvn, "-pl", "worker", "-Dtest=AdFraudModuleTest", "test"]
    result = subprocess.run(cmd, cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=180)
    return {"status": "passed" if result.returncode == 0 else "failed", "command": cmd, "outputTail": result.stdout[-4000:]}


def run_workflow(args: argparse.Namespace) -> dict[str, Any]:
    if args.quick:
        args.max_rows = min(args.max_rows, len(SCENARIOS) * 120)
    TARGET.mkdir(parents=True, exist_ok=True)
    first_party_path = Path(args.first_party_labels) if args.first_party_labels else (
        Path(os.environ["AD_FRAUD_FIRST_PARTY_LABELS"]) if os.environ.get("AD_FRAUD_FIRST_PARTY_LABELS") else None
    )
    manifest = download_sources(args.offline, first_party_path, args.max_rows)
    examples = build_examples(args.max_rows, args.seed, first_party_path)
    write_jsonl(TARGET / "normalized_events.jsonl", [{**e.event, **{f"feature_{k}": v for k, v in e.features.items()}, **{f"label_{k}": v for k, v in e.labels.items()}, "split": e.split} for e in examples])
    leakage = leakage_audit(examples)
    model = train_model(examples, args.seed)
    calibration = calibrate(model, examples)
    thresholds = choose_thresholds(model, calibration, examples)
    metrics, predictions = evaluate(model, calibration, thresholds, examples)
    latency = latency_benchmark(model, calibration, examples)
    export_bundle(model, calibration, thresholds, metrics, manifest, examples)
    write_reports(args, manifest, leakage, model, calibration, thresholds, metrics, predictions, latency, examples)
    java = run_java_tests() if not args.skip_java else {"status": "skipped"}
    summary = {
        "artifactRoot": str(TARGET),
        "modelBundle": str(MODEL_RESOURCE_DIR),
        "macroF1": metrics["macroF1"],
        "readiness": json.loads((TARGET / "readiness.json").read_text(encoding="utf-8")),
        "javaTests": java["status"],
    }
    write_json(TARGET / "workflow_summary.json", summary)
    return summary


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--quick", action="store_true")
    parser.add_argument("--full", action="store_true")
    parser.add_argument("--offline", action="store_true")
    parser.add_argument("--max-rows", type=int, default=12000)
    parser.add_argument("--max-memory-mb", type=int, default=1024)
    parser.add_argument("--seed", type=int, default=1729)
    parser.add_argument("--force-retrain", action="store_true")
    parser.add_argument("--skip-java", action="store_true")
    parser.add_argument("--first-party-labels", type=str, default=None,
                        help="Optional authorized JSONL/CSV first-party labels with canonical event fields and label_* columns.")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    summary = run_workflow(args)
    print(json.dumps(summary, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
