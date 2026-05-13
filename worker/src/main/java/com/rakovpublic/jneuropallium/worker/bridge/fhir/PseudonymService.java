/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Pseudonymises FHIR patient identifiers (06-FHIR.md §3 rule 3, §9 S9,
 * §10 R1).
 *
 * <p>The bridge never persists the raw cohort identifier; every signal,
 * audit record and advisory line carries
 * {@code SHA-256(rawId || ":" || salt)} truncated to {@link #ID_LEN}
 * hexadecimal characters. The salt is sourced from an operator-controlled
 * environment variable so the pseudonym mapping is non-reversible without
 * access to the salted secret.
 *
 * <p>If pseudonymisation is disabled in config the service returns the
 * raw id unchanged; doing so is the operator's explicit choice (e.g. a
 * fully synthetic dataset), and the choice is reflected in every audit
 * line so an external review can detect it.
 */
public final class PseudonymService {

    /** Length of the pseudonymous id surfaced to signals (hex chars). */
    public static final int ID_LEN = 24;

    private final boolean enabled;
    private final byte[] salt;

    public PseudonymService(boolean enabled, String salt) {
        this.enabled = enabled;
        this.salt = (salt == null ? "" : salt).getBytes(StandardCharsets.UTF_8);
    }

    /** Convenience constructor — reads the salt from {@code System.getenv}. */
    public static PseudonymService fromConfig(FhirBridgeConfig.PrivacyConfig privacy) {
        Objects.requireNonNull(privacy, "privacy");
        if (!privacy.pseudonymise()) {
            return new PseudonymService(false, null);
        }
        String saltEnv = privacy.saltEnv();
        String salt = saltEnv == null ? "" : System.getenv(saltEnv);
        if (salt == null) salt = "";
        return new PseudonymService(true, salt);
    }

    /**
     * Apply pseudonymisation to a raw cohort/patient identifier. Empty or
     * {@code null} input is mapped to {@code null} so callers do not
     * accidentally emit empty pseudonyms downstream.
     */
    public String pseudonymise(String rawId) {
        if (rawId == null || rawId.isEmpty()) return null;
        if (!enabled) return rawId;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(rawId.getBytes(StandardCharsets.UTF_8));
            md.update((byte) ':');
            md.update(salt);
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(ID_LEN);
            int hexChars = Math.min(ID_LEN, digest.length * 2);
            for (int i = 0; i < (hexChars + 1) / 2; i++) {
                int v = digest[i] & 0xFF;
                sb.append(Character.forDigit(v >>> 4, 16));
                sb.append(Character.forDigit(v & 0x0F, 16));
            }
            return sb.substring(0, ID_LEN);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public boolean isEnabled() { return enabled; }
}
