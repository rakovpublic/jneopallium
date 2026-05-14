/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;

import com.fasterxml.jackson.databind.JsonNode;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.EngagementSource;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.AffectObservationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.EngagementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.MasteryUpdateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ResponseSignal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Pure functions converting xAPI statement JSON to Jneopallium tutoring
 * signals (14-LTI-XAPI.md §5 mapping table). Stateless apart from a
 * counter of redactions performed in the current poll cycle so the
 * bridge can record a count without leaking the redacted content.
 *
 * <p>14-LTI-XAPI.md §10 R1 + §11 S11 — the mapper:
 * <ul>
 *   <li>Pseudonymises every actor identifier
 *       ({@code actor.account.name}, {@code actor.mbox},
 *       {@code actor.openid}).</li>
 *   <li>Drops {@code result.response} when
 *       {@link LtiBridgeConfig.PrivacyConfig#redactFreeText()} is set
 *       and increments the redaction counter.</li>
 * </ul>
 *
 * <p>R4 — unknown verbs are emitted as {@link EngagementSignal} with
 * {@link EngagementSource#MULTI_MODAL} so the pipeline still sees some
 * activity signal rather than silently dropping the statement.
 */
public final class XapiStatementMapper {

    /** §5 — well-known xAPI verb IRIs. */
    public static final String VERB_ATTEMPTED   = "http://adlnet.gov/expapi/verbs/attempted";
    public static final String VERB_ANSWERED    = "http://adlnet.gov/expapi/verbs/answered";
    public static final String VERB_MASTERED    = "http://adlnet.gov/expapi/verbs/mastered";
    public static final String VERB_FAILED      = "http://adlnet.gov/expapi/verbs/failed";
    public static final String VERB_PASSED      = "http://adlnet.gov/expapi/verbs/passed";
    public static final String VERB_INTERACTED  = "http://adlnet.gov/expapi/verbs/interacted";
    public static final String VERB_EXPERIENCED = "http://adlnet.gov/expapi/verbs/experienced";

    /** xAPI extension carrying behavioural affect (§5). */
    public static final String EXT_AFFECT =
            "https://jneopallium.rakov.org/xapi/extensions/affect";

    private final PseudonymService pseudonyms;
    private final boolean redactFreeText;
    private int redactionCount;

    public XapiStatementMapper(PseudonymService pseudonyms, boolean redactFreeText) {
        this.pseudonyms = pseudonyms;
        this.redactFreeText = redactFreeText;
    }

    /** Returns and resets the count of free-text fields redacted since the last call. */
    public synchronized int drainRedactionCount() {
        int c = redactionCount;
        redactionCount = 0;
        return c;
    }

    /* ==== Statement-list helpers ========================================= */

    /**
     * Iterate the statements of an LRS response. The LRS spec returns
     * statements either as a top-level array or wrapped in
     * {@code {"statements": [...]}}; both are handled.
     */
    public List<JsonNode> statements(JsonNode response) {
        List<JsonNode> out = new ArrayList<>();
        if (response == null || response.isMissingNode() || response.isNull()) return out;
        JsonNode arr = response.isArray() ? response : response.get("statements");
        if (arr == null || !arr.isArray()) return out;
        for (Iterator<JsonNode> it = arr.elements(); it.hasNext(); ) {
            JsonNode n = it.next();
            if (n != null && !n.isNull()) out.add(n);
        }
        return out;
    }

    /* ==== Decoders ======================================================= */

    /** Dispatch a statement to the correct decoder for {@code targetSignal}. */
    public IInputSignal map(JsonNode statement, LtiBridgeConfig.TargetSignal targetSignal) {
        if (statement == null || statement.isNull() || targetSignal == null) return null;
        return switch (targetSignal) {
            case RESPONSE   -> mapResponse(statement);
            case MASTERY    -> mapMastery(statement);
            case ENGAGEMENT -> mapEngagement(statement);
            case AFFECT     -> mapAffect(statement);
        };
    }

    /** §5 — {@code attempted} / {@code answered} statement on an item. */
    public ResponseSignal mapResponse(JsonNode stmt) {
        String verb = verbId(stmt);
        if (verb == null) return null;
        // Accept the canonical attempted/answered verbs, and treat passed/failed as
        // response statements as well — they carry result.success too.
        if (!VERB_ATTEMPTED.equals(verb) && !VERB_ANSWERED.equals(verb)
                && !VERB_PASSED.equals(verb) && !VERB_FAILED.equals(verb)) {
            return null;
        }
        String itemId = objectId(stmt);
        JsonNode result = stmt.get("result");
        boolean correct;
        if (VERB_PASSED.equals(verb)) {
            correct = true;
        } else if (VERB_FAILED.equals(verb)) {
            correct = false;
        } else {
            correct = booleanOrFalse(result == null ? null : result.get("success"));
        }
        long latencyMs = parseDurationMillis(result == null ? null : result.get("duration"));
        String responsePayload = freeTextOrNull(result == null ? null : result.get("response"));
        ResponseSignal r = new ResponseSignal(itemId, correct, latencyMs, responsePayload);
        r.setName(pseudonyms.pseudonymise(actorId(stmt)));
        return r;
    }

    /** §5 — {@code mastered} / {@code failed} on a competency. */
    public MasteryUpdateSignal mapMastery(JsonNode stmt) {
        String verb = verbId(stmt);
        if (verb == null) return null;
        if (!VERB_MASTERED.equals(verb) && !VERB_FAILED.equals(verb)
                && !VERB_PASSED.equals(verb)) {
            return null;
        }
        String competencyId = objectId(stmt);
        double newLevel;
        if (VERB_MASTERED.equals(verb) || VERB_PASSED.equals(verb)) {
            newLevel = 1.0;
        } else {
            newLevel = 0.0;
        }
        JsonNode result = stmt.get("result");
        Double scaled = result == null ? null : numberOrNull(
                result.has("score") ? result.get("score").get("scaled") : null);
        if (scaled != null) {
            newLevel = Math.max(0.0, Math.min(1.0, scaled));
        }
        MasteryUpdateSignal m = new MasteryUpdateSignal(competencyId, newLevel);
        m.setName(pseudonyms.pseudonymise(actorId(stmt)));
        return m;
    }

    /** §5 — {@code interacted} / {@code experienced} → engagement. */
    public EngagementSignal mapEngagement(JsonNode stmt) {
        String verb = verbId(stmt);
        if (verb == null) return null;
        double score;
        if (VERB_INTERACTED.equals(verb)) {
            score = 0.8;
        } else if (VERB_EXPERIENCED.equals(verb)) {
            score = 0.5;
        } else {
            // §10 R4 — unknown verbs surface as low-confidence engagement so the
            // pipeline keeps awareness without inventing semantics.
            score = 0.3;
        }
        JsonNode result = stmt.get("result");
        long dwellMs = parseDurationMillis(result == null ? null : result.get("duration"));
        if (dwellMs > 0L) {
            // Convert dwell (clamped to 10 minutes) into a [0..1] activity score and
            // average with the verb-based prior.
            double dwellScore = Math.min(1.0, dwellMs / (10.0 * 60_000.0));
            score = (score + dwellScore) / 2.0;
        }
        EngagementSignal e = new EngagementSignal(score, EngagementSource.MULTI_MODAL);
        e.setName(pseudonyms.pseudonymise(actorId(stmt)));
        return e;
    }

    /** §5 — affect extension (paired with LSL deployments). */
    public AffectObservationSignal mapAffect(JsonNode stmt) {
        JsonNode context = stmt == null ? null : stmt.get("context");
        JsonNode ext = context == null ? null : context.get("extensions");
        JsonNode affect = ext == null ? null : ext.get(EXT_AFFECT);
        if (affect == null || affect.isNull()) return null;
        double valence = doubleOrZero(affect.get("valence"));
        double arousal = doubleOrZero(affect.get("arousal"));
        double confidence = doubleOrZero(affect.get("confidence"));
        if (confidence == 0.0) confidence = 0.5;
        AffectObservationSignal a = new AffectObservationSignal(valence, arousal, confidence);
        a.setName(pseudonyms.pseudonymise(actorId(stmt)));
        return a;
    }

    /* ==== Field extractors ============================================== */

    private static String verbId(JsonNode stmt) {
        if (stmt == null) return null;
        JsonNode verb = stmt.get("verb");
        if (verb == null || verb.isNull()) return null;
        return textOrNull(verb.get("id"));
    }

    private static String objectId(JsonNode stmt) {
        if (stmt == null) return null;
        JsonNode obj = stmt.get("object");
        if (obj == null || obj.isNull()) return null;
        String id = textOrNull(obj.get("id"));
        if (id != null) return id;
        // ActivityDefinition / SubStatement / Agent fallback.
        return textOrNull(obj.get("name"));
    }

    /**
     * Extract the raw actor identifier. Priority order matches the xAPI
     * Inverse Functional Identifier rules: {@code mbox} →
     * {@code account.name} → {@code openid} → {@code name}.
     */
    static String actorId(JsonNode stmt) {
        if (stmt == null) return null;
        JsonNode actor = stmt.get("actor");
        if (actor == null || actor.isNull()) return null;
        String mbox = textOrNull(actor.get("mbox"));
        if (mbox != null) return mbox;
        JsonNode account = actor.get("account");
        if (account != null && !account.isNull()) {
            String acctName = textOrNull(account.get("name"));
            String homePage = textOrNull(account.get("homePage"));
            if (acctName != null && homePage != null) return homePage + "#" + acctName;
            if (acctName != null) return acctName;
        }
        String openid = textOrNull(actor.get("openid"));
        if (openid != null) return openid;
        return textOrNull(actor.get("name"));
    }

    private synchronized String freeTextOrNull(JsonNode value) {
        if (value == null || value.isNull()) return null;
        if (redactFreeText) {
            redactionCount++;
            return null;
        }
        return value.asText();
    }

    private static boolean booleanOrFalse(JsonNode n) {
        return n != null && !n.isNull() && n.isBoolean() && n.asBoolean();
    }

    private static Double numberOrNull(JsonNode n) {
        if (n == null || n.isNull() || !n.isNumber()) return null;
        return n.asDouble();
    }

    private static double doubleOrZero(JsonNode n) {
        if (n == null || n.isNull() || !n.isNumber()) return 0.0;
        return n.asDouble();
    }

    private static String textOrNull(JsonNode n) {
        return n == null || n.isNull() ? null : n.asText();
    }

    /**
     * Parse an ISO-8601 duration ({@code PT12.34S}, {@code PT1M30S}, …)
     * into milliseconds. Returns {@code 0L} when the field is absent or
     * unparseable — the bridge degrades gracefully rather than failing
     * the whole poll cycle on one malformed statement.
     */
    static long parseDurationMillis(JsonNode duration) {
        String iso = textOrNull(duration);
        if (iso == null || iso.isEmpty()) return 0L;
        try {
            return java.time.Duration.parse(iso).toMillis();
        } catch (RuntimeException e) {
            return 0L;
        }
    }
}
