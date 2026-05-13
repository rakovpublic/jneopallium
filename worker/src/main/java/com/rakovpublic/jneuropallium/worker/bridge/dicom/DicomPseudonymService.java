/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Pseudonymises DICOM patient identifiers (07-DICOM.md §6 {@code privacy:},
 * §9 S9, §10 R1).
 *
 * <p>The bridge never persists the raw {@code PatientID} or
 * {@code PatientName}; every signal and audit record carries
 * {@code SHA-256(rawId || ":" || salt)} truncated to {@link #ID_LEN}
 * hexadecimal characters. The salt is sourced from an operator-controlled
 * environment variable so the pseudonym mapping is non-reversible without
 * the salted secret.
 */
public final class DicomPseudonymService {

    /** Length of the pseudonymous id surfaced to signals (hex chars). */
    public static final int ID_LEN = 24;

    private final boolean enabled;
    private final byte[] salt;

    public DicomPseudonymService(boolean enabled, String salt) {
        this.enabled = enabled;
        this.salt = (salt == null ? "" : salt).getBytes(StandardCharsets.UTF_8);
    }

    /** Convenience constructor — reads the salt from {@code System.getenv}. */
    public static DicomPseudonymService fromConfig(DicomBridgeConfig.PrivacyConfig privacy) {
        Objects.requireNonNull(privacy, "privacy");
        if (!privacy.pseudonymise()) {
            return new DicomPseudonymService(false, null);
        }
        String saltEnv = privacy.saltEnv();
        String salt = saltEnv == null ? "" : System.getenv(saltEnv);
        if (salt == null) salt = "";
        return new DicomPseudonymService(true, salt);
    }

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
