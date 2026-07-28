/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public final class AdFraudPrivacy {
    private AdFraudPrivacy() {
    }

    public static String hmacIdentifier(String key, String value) {
        if (value == null) return null;
        String actualKey = key == null || key.isBlank()
                ? "jneopallium-ad-fraud-demo-key-not-for-production"
                : key;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(actualKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot HMAC advertising identifier", e);
        }
    }

    public static String truncateIpPrefix(String ipAddress, int ipv4PrefixBits) {
        if (ipAddress == null || ipAddress.isBlank()) return null;
        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) return hmacIdentifier("ip-prefix", ipAddress);
        int octets = Math.max(1, Math.min(3, ipv4PrefixBits / 8));
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < octets; i++) {
            if (i > 0) prefix.append('.');
            prefix.append(parts[i]);
        }
        return prefix.toString();
    }
}
