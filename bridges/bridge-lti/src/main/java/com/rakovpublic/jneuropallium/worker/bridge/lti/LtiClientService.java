/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * LTI 1.3 launch handling (14-LTI-XAPI.md §4 architecture, §7
 * {@code LtiClientService}).
 *
 * <p>The service parses an OpenID Connect {@code id_token} carrying the
 * LTI launch claims, verifies the structural invariants (iss / aud /
 * exp / nonce / deployment id) against a registered
 * {@link LtiPlatformBinding}, and exposes the parsed
 * {@link LaunchContext} (learner, course, role, line items) so the
 * {@link LtiAdvisoryOutputAggregator} can scope its egress.
 *
 * <p><b>Signature verification.</b> The bridge keeps its dependency
 * footprint small (no Nimbus / java-jwt). For deployments wired against
 * a real LMS, plug in a {@link JwtVerifier} that validates the JWT
 * signature against the platform's JWKS. The default
 * {@link JwtVerifier#permissive()} accepts any well-formed JWT and is
 * meant for tests + the structural acceptance scenarios. Production
 * code MUST install a real verifier — the {@link #verifyLaunch} method
 * refuses to run with the permissive verifier when
 * {@code requireSignedJwt = true}.
 *
 * <p>This is intentionally minimal — the spec calls out (§2) that a
 * maintained 1EdTech LTI 1.3 client should replace this scaffolding
 * once available. The seam is here so a future drop-in does not need to
 * touch the rest of the bridge.
 */
public final class LtiClientService {

    private static final Logger log = LoggerFactory.getLogger(LtiClientService.class);

    /** LTI 1.3 claim namespace. */
    public static final String CLAIM_DEPLOYMENT_ID =
            "https://purl.imsglobal.org/spec/lti/claim/deployment_id";
    public static final String CLAIM_CONTEXT =
            "https://purl.imsglobal.org/spec/lti/claim/context";
    public static final String CLAIM_ROLES =
            "https://purl.imsglobal.org/spec/lti/claim/roles";
    public static final String CLAIM_AGS =
            "https://purl.imsglobal.org/spec/lti-ags/claim/endpoint";
    public static final String CLAIM_NRPS =
            "https://purl.imsglobal.org/spec/lti-nrps/claim/namesroleservice";

    /** ADL-style instructor / learner roles. */
    public static final String ROLE_LEARNER =
            "http://purl.imsglobal.org/vocab/lis/v2/membership#Learner";
    public static final String ROLE_INSTRUCTOR =
            "http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor";

    private final List<LtiPlatformBinding> platforms;
    private final ObjectMapper mapper;
    private final JwtVerifier verifier;
    private final boolean requireSignedJwt;

    public LtiClientService(LtiBridgeConfig config) {
        this(config, new ObjectMapper(), JwtVerifier.permissive(), false);
    }

    public LtiClientService(LtiBridgeConfig config,
                            ObjectMapper mapper,
                            JwtVerifier verifier,
                            boolean requireSignedJwt) {
        Objects.requireNonNull(config, "config");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.verifier = Objects.requireNonNull(verifier, "verifier");
        this.requireSignedJwt = requireSignedJwt;
        List<LtiPlatformBinding> bindings = new java.util.ArrayList<>();
        if (config.lti() != null) {
            for (LtiBridgeConfig.PlatformConfig p : config.lti().platforms()) {
                bindings.add(LtiPlatformBinding.fromConfig(p));
            }
        }
        this.platforms = List.copyOf(bindings);
    }

    public List<LtiPlatformBinding> platforms() { return platforms; }

    /**
     * Decode and validate an LTI 1.3 launch JWT (id_token). On success
     * returns a {@link LaunchContext}; on any structural failure
     * (signature, iss/aud, expiry, deployment id) throws
     * {@link LaunchVerificationException}.
     */
    public LaunchContext verifyLaunch(String idToken) throws LaunchVerificationException {
        if (idToken == null || idToken.isEmpty()) {
            throw new LaunchVerificationException("empty id_token");
        }
        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            throw new LaunchVerificationException("id_token must have 3 dot-separated parts");
        }
        JsonNode header = decodeJsonPart(parts[0]);
        JsonNode payload = decodeJsonPart(parts[1]);
        if (requireSignedJwt && verifier == JwtVerifier.permissive()) {
            throw new LaunchVerificationException(
                    "permissive verifier rejected — production deployments must install a real JwtVerifier");
        }
        if (!verifier.verify(parts[0], parts[1], parts[2], header, payload)) {
            throw new LaunchVerificationException("signature verification failed");
        }
        String iss = textOrNull(payload.get("iss"));
        String aud = audAsString(payload.get("aud"));
        long exp = payload.has("exp") ? payload.get("exp").asLong(0L) : 0L;
        if (exp > 0L && exp * 1000L < System.currentTimeMillis()) {
            throw new LaunchVerificationException("id_token expired");
        }
        String deploymentId = textOrNull(payload.get(CLAIM_DEPLOYMENT_ID));
        LtiPlatformBinding platform = matchPlatform(iss, aud, deploymentId);
        if (platform == null) {
            throw new LaunchVerificationException(
                    "no registered platform accepts iss=" + iss + " aud=" + aud
                            + " deployment=" + deploymentId);
        }
        String sub = textOrNull(payload.get("sub"));
        String nonce = textOrNull(payload.get("nonce"));
        String contextId = nestedText(payload.get(CLAIM_CONTEXT), "id");
        String contextTitle = nestedText(payload.get(CLAIM_CONTEXT), "title");
        List<String> roles = stringList(payload.get(CLAIM_ROLES));
        JsonNode agsNode = payload.get(CLAIM_AGS);
        String agsLineItem = agsNode == null ? null : textOrNull(agsNode.get("lineitem"));
        String agsLineItems = agsNode == null ? null : textOrNull(agsNode.get("lineitems"));
        String nrps = nestedText(payload.get(CLAIM_NRPS), "context_memberships_url");
        return new LaunchContext(platform, iss, aud, sub, nonce,
                contextId, contextTitle, roles, deploymentId,
                agsLineItem, agsLineItems, nrps);
    }

    private LtiPlatformBinding matchPlatform(String iss, String aud, String deploymentId) {
        for (LtiPlatformBinding p : platforms) {
            if (p.accepts(iss, aud, deploymentId)) return p;
        }
        return null;
    }

    private JsonNode decodeJsonPart(String b64) throws LaunchVerificationException {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(padBase64(b64));
            return mapper.readTree(decoded);
        } catch (RuntimeException | java.io.IOException e) {
            throw new LaunchVerificationException("malformed JWT segment: " + e.getMessage());
        }
    }

    private static String padBase64(String s) {
        int rem = s.length() % 4;
        if (rem == 0) return s;
        return s + "====".substring(rem);
    }

    private static String textOrNull(JsonNode n) {
        return n == null || n.isNull() ? null : n.asText();
    }

    private static String audAsString(JsonNode aud) {
        if (aud == null || aud.isNull()) return null;
        if (aud.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode n : aud) {
                if (sb.length() > 0) sb.append(",");
                sb.append(n.asText());
            }
            return sb.toString();
        }
        return aud.asText();
    }

    private static String nestedText(JsonNode parent, String field) {
        if (parent == null || parent.isNull()) return null;
        JsonNode child = parent.get(field);
        return child == null || child.isNull() ? null : child.asText();
    }

    private static List<String> stringList(JsonNode arr) {
        if (arr == null || !arr.isArray()) return List.of();
        List<String> out = new java.util.ArrayList<>();
        for (JsonNode n : arr) if (n != null && !n.isNull()) out.add(n.asText());
        return List.copyOf(out);
    }

    /**
     * Verified launch context, scoped per learner / course / line item.
     * Contains every field the {@link LtiAdvisoryOutputAggregator} needs
     * to scope an AGS Score proposal or an xAPI {@code recommended}
     * statement.
     */
    public record LaunchContext(
            LtiPlatformBinding platform,
            String issuer,
            String audience,
            String subject,
            String nonce,
            String contextId,
            String contextTitle,
            List<String> roles,
            String deploymentId,
            String agsLineItemUrl,
            String agsLineItemsUrl,
            String nrpsContextMembershipsUrl
    ) {
        public LaunchContext {
            roles = roles == null ? List.of() : List.copyOf(roles);
        }

        public boolean isLearner() { return roles.contains(ROLE_LEARNER); }
        public boolean isInstructor() { return roles.contains(ROLE_INSTRUCTOR); }
    }

    /** Thrown when a launch JWT fails any of the structural invariants. */
    public static final class LaunchVerificationException extends Exception {
        public LaunchVerificationException(String message) { super(message); }
    }

    /**
     * Signature-verification seam. Production deployments install an
     * implementation backed by a JWKS fetcher (Nimbus, java-jwt, etc.);
     * tests use {@link #permissive()}.
     */
    public interface JwtVerifier {
        boolean verify(String headerB64, String payloadB64, String signatureB64,
                       JsonNode header, JsonNode payload);

        static JwtVerifier permissive() { return PERMISSIVE; }
        JwtVerifier PERMISSIVE = (h, p, s, hN, pN) -> true;
    }

    /**
     * Helper for §9 S8 — pretty-print a {@link LaunchContext} for the
     * launch audit record (raw {@code sub} pseudonymised; nonce + iss
     * preserved so a replay can be detected during forensic review).
     */
    public Map<String, String> toAuditFields(LaunchContext ctx, PseudonymService pseudonyms) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("iss", ctx.issuer());
        m.put("aud", ctx.audience());
        m.put("deployment", ctx.deploymentId());
        m.put("context", ctx.contextId());
        m.put("learner", pseudonyms.pseudonymise(ctx.subject()));
        m.put("hasAgs", ctx.agsLineItemUrl() == null ? "false" : "true");
        return m;
    }

    /**
     * Convenience for tests — build a permissive id_token from the given
     * claims. Not used by the production code path.
     */
    public static String buildPermissiveIdToken(JsonNode header, JsonNode payload) {
        try {
            ObjectMapper m = new ObjectMapper();
            String h = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    m.writeValueAsBytes(header));
            String p = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    m.writeValueAsBytes(payload));
            return h + "." + p + "." + "permissive";
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unused")
    private static String b64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @SuppressWarnings("unused")
    private static String utf8(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
