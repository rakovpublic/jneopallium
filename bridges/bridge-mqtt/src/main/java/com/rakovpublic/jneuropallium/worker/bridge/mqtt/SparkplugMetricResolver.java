/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mqtt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves Sparkplug B addresses (02-MQTT-SPARKPLUG.md §1) to a
 * {@link MqttBridgeConfig.ReadBindingConfig}.
 *
 * <p>A Sparkplug address has the form {@code group/edge/device/metric}; the
 * {@code sparkplugMetric} field on a read binding is matched verbatim or as
 * a {@code +} / {@code *} pattern (single-segment wildcard). Wildcard
 * matching is intentionally minimal and explicit so a typo in the YAML can
 * never silently match too much.
 *
 * <p>Topic-level subscriptions are computed by collapsing every binding's
 * Sparkplug address to its broker subscription string
 * (e.g. {@code spBv1.0/Plant1/+/+/+}) — see {@link #subscriptionTopicsFor}.
 */
public final class SparkplugMetricResolver {

    /** {@code group/edge/device/metric} key (no {@code spBv1.0/} prefix). */
    record Address(String group, String edge, String device, String metric) {
        Address {
            Objects.requireNonNull(group, "group");
            Objects.requireNonNull(edge, "edge");
        }
        static Address parse(String s) {
            String[] p = s.split("/");
            if (p.length < 4) {
                throw new IllegalArgumentException(
                        "Sparkplug address '" + s + "' must have form group/edge/device/metric");
            }
            return new Address(p[0], p[1], p[2], p[3]);
        }
    }

    private final Map<String, MqttBridgeConfig.ReadBindingConfig> byExact = new HashMap<>();
    private final List<MqttBridgeConfig.ReadBindingConfig> patterns = new java.util.ArrayList<>();

    public SparkplugMetricResolver(List<MqttBridgeConfig.ReadBindingConfig> reads) {
        for (MqttBridgeConfig.ReadBindingConfig r : reads) {
            if (r.sparkplugMetric() == null) continue;
            if (containsWildcard(r.sparkplugMetric())) {
                patterns.add(r);
            } else {
                byExact.put(r.sparkplugMetric(), r);
            }
        }
    }

    /** Find the binding for a delivered metric name; {@code null} when no binding matches. */
    public MqttBridgeConfig.ReadBindingConfig resolve(String group, String edge, String device, String metric) {
        String full = group + "/" + edge + "/" + device + "/" + metric;
        MqttBridgeConfig.ReadBindingConfig exact = byExact.get(full);
        if (exact != null) return exact;
        for (MqttBridgeConfig.ReadBindingConfig p : patterns) {
            if (matches(p.sparkplugMetric(), group, edge, device, metric)) return p;
        }
        return null;
    }

    /**
     * MQTT topic filters that subscribe to every Sparkplug DBIRTH/DDATA/DDEATH
     * needed by the configured read bindings, plus the matching N-level
     * messages on the same group.
     */
    public static List<String> subscriptionTopicsFor(MqttBridgeConfig config) {
        java.util.LinkedHashSet<String> topics = new java.util.LinkedHashSet<>();
        if (config.sparkplug() != null && config.sparkplug().enabled()) {
            String group = config.sparkplug().groupId();
            // Wildcard subscription for every Sparkplug message type under the group:
            topics.add("spBv1.0/" + group + "/NBIRTH/+");
            topics.add("spBv1.0/" + group + "/NDEATH/+");
            topics.add("spBv1.0/" + group + "/DBIRTH/+/+");
            topics.add("spBv1.0/" + group + "/DDEATH/+/+");
            topics.add("spBv1.0/" + group + "/NDATA/+");
            topics.add("spBv1.0/" + group + "/DDATA/+/+");
        }
        for (MqttBridgeConfig.ReadBindingConfig r : config.reads()) {
            if (r.plainMqttTopic() != null) topics.add(r.plainMqttTopic());
        }
        return List.copyOf(topics);
    }

    /* ===== matching =================================================== */

    private static boolean containsWildcard(String s) {
        return s.contains("+") || s.contains("*");
    }

    static boolean matches(String pattern, String group, String edge, String device, String metric) {
        String[] p = pattern.split("/");
        String[] v = new String[]{group, edge, device, metric};
        if (p.length != v.length) return false;
        for (int i = 0; i < p.length; i++) {
            String seg = p[i];
            if ("+".equals(seg) || "*".equals(seg)) continue;
            if (seg.endsWith("*")) {
                String prefix = seg.substring(0, seg.length() - 1);
                if (!v[i].startsWith(prefix)) return false;
            } else if (!seg.equals(v[i])) {
                return false;
            }
        }
        return true;
    }
}
