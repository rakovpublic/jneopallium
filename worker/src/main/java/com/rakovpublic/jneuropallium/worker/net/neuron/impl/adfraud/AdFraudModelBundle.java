/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdFraudModelBundle {
    public static final String DEFAULT_RESOURCE_DIR = "model/advertising-fraud";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String modelId = "advertising-fraud";
    private String version = "0.0.0";
    private String schemaVersion = "1.0";
    private List<String> featureNames = new ArrayList<>();
    private List<String> labels = new ArrayList<>();
    private Map<String, double[]> weights = new LinkedHashMap<>();
    private Map<String, Double> biases = new LinkedHashMap<>();
    private Map<String, Double> thresholds = new LinkedHashMap<>();
    private Map<String, double[]> calibration = new LinkedHashMap<>();
    private List<String> inputNames = new ArrayList<>();
    private List<String> hiddenOutputs = new ArrayList<>();
    private double[][] hiddenWeights;
    private double[] hiddenBias;
    private boolean verified;

    public static AdFraudModelBundle loadDefault() {
        try {
            return loadFromResources(DEFAULT_RESOURCE_DIR);
        } catch (RuntimeException e) {
            return rulesOnlyFallback();
        }
    }

    public static AdFraudModelBundle rulesOnlyFallback() {
        AdFraudModelBundle bundle = new AdFraudModelBundle();
        bundle.modelId = "advertising-fraud-rules-only";
        bundle.version = "rules-only";
        bundle.schemaVersion = "1.0";
        bundle.featureNames = List.of(
                "integrity_risk", "bot_risk", "sequence_risk", "attribution_risk",
                "supply_chain_risk", "graph_risk", "quality_risk", "unknown_risk");
        bundle.labels = List.of(
                "bot", "incentivized", "clickFarm", "eventSpoofing", "clickSpam",
                "clickInjection", "attributionHijack", "inventorySpoofing",
                "accidentalOrLowValue", "unknownSuspicious");
        for (String label : bundle.labels) {
            bundle.weights.put(label, new double[bundle.featureNames.size()]);
            bundle.biases.put(label, -2.0);
            bundle.thresholds.put(label, 0.5);
            bundle.calibration.put(label, new double[]{1.0, 0.0});
        }
        return bundle;
    }

    public static AdFraudModelBundle loadFromResources(String resourceDir) {
        try {
            verifyChecksums(resourceDir);
            JsonNode descriptor = readJson(resourceDir + "/model-descriptor.json");
            JsonNode fallback = readJson(resourceDir + "/fallback-model.json");
            JsonNode thresholds = readJson(resourceDir + "/thresholds.json");
            JsonNode calibration = readJson(resourceDir + "/calibration.json");
            AdFraudModelBundle bundle = new AdFraudModelBundle();
            bundle.modelId = descriptor.path("modelId").asText("advertising-fraud");
            bundle.version = descriptor.path("version").asText("0.0.0");
            bundle.schemaVersion = descriptor.path("schemaVersion").asText("1.0");
            fallback.path("featureNames").forEach(n -> bundle.featureNames.add(n.asText()));
            fallback.path("labels").forEach(n -> bundle.labels.add(n.asText()));
            JsonNode heads = fallback.path("heads");
            for (String label : bundle.labels) {
                List<Double> ws = new ArrayList<>();
                heads.path(label).path("weights").forEach(n -> ws.add(n.asDouble()));
                double[] arr = new double[ws.size()];
                for (int i = 0; i < ws.size(); i++) arr[i] = ws.get(i);
                bundle.weights.put(label, arr);
                bundle.biases.put(label, heads.path(label).path("bias").asDouble());
                bundle.thresholds.put(label, thresholds.path("thresholds").path(label).asDouble(0.5));
                JsonNode c = calibration.path("calibration").path(label);
                bundle.calibration.put(label, new double[]{
                        c.path("scale").asDouble(1.0),
                        c.path("offset").asDouble(0.0)
                });
            }
            fallback.path("inputNames").forEach(n -> bundle.inputNames.add(n.asText()));
            JsonNode hidden = fallback.path("hidden");
            if (hidden.has("weights")) {
                if (bundle.inputNames.isEmpty()) hidden.path("inputNames").forEach(n -> bundle.inputNames.add(n.asText()));
                hidden.path("outputNames").forEach(n -> bundle.hiddenOutputs.add(n.asText()));
                List<double[]> rows = new ArrayList<>();
                hidden.path("weights").forEach(r -> {
                    List<Double> rr = new ArrayList<>();
                    r.forEach(v -> rr.add(v.asDouble()));
                    double[] arr = new double[rr.size()];
                    for (int i = 0; i < rr.size(); i++) arr[i] = rr.get(i);
                    rows.add(arr);
                });
                bundle.hiddenWeights = rows.toArray(new double[0][]);
                List<Double> bs = new ArrayList<>();
                hidden.path("bias").forEach(v -> bs.add(v.asDouble()));
                bundle.hiddenBias = new double[bs.size()];
                for (int i = 0; i < bs.size(); i++) bundle.hiddenBias[i] = bs.get(i);
            }
            bundle.verified = true;
            return bundle;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load advertising fraud model bundle", e);
        }
    }

    public double score(String label, Map<String, Double> features) {
        double[] w = weights.get(label);
        if (w == null) return 0.0;
        Map<String, Double> f = augment(features);
        double z = biases.getOrDefault(label, 0.0);
        for (int i = 0; i < Math.min(w.length, featureNames.size()); i++) {
            z += w[i] * f.getOrDefault(featureNames.get(i), 0.0);
        }
        double p = sigmoid(z);
        double[] c = calibration.getOrDefault(label, new double[]{1.0, 0.0});
        return clamp(sigmoid((logit(p) * c[0]) + c[1]));
    }

    // Computes the non-linear feature-interaction (hidden) layer and merges its
    // outputs into the feature map, so the runtime evaluates the same model the
    // trainer fitted. Rules-only / legacy bundles have no hidden block.
    private Map<String, Double> augment(Map<String, Double> features) {
        if (hiddenWeights == null || hiddenOutputs.isEmpty() || inputNames.isEmpty()) {
            return features;
        }
        Map<String, Double> f = new LinkedHashMap<>(features);
        for (int u = 0; u < hiddenOutputs.size() && u < hiddenWeights.length; u++) {
            double z = (hiddenBias != null && u < hiddenBias.length) ? hiddenBias[u] : 0.0;
            double[] row = hiddenWeights[u];
            for (int i = 0; i < inputNames.size() && i < row.length; i++) {
                z += row[i] * features.getOrDefault(inputNames.get(i), 0.0);
            }
            f.put(hiddenOutputs.get(u), Math.tanh(z));
        }
        return f;
    }

    public String getModelId() { return modelId; }
    public String getVersion() { return version; }
    public String getSchemaVersion() { return schemaVersion; }
    public List<String> getFeatureNames() { return featureNames; }
    public List<String> getLabels() { return labels; }
    public Map<String, Double> getThresholds() { return thresholds; }
    public boolean isVerified() { return verified; }

    private static void verifyChecksums(String resourceDir) throws IOException {
        String text = readText(resourceDir + "/checksums.sha256");
        for (String line : text.split("\\R")) {
            if (line.isBlank()) continue;
            String[] parts = line.trim().split("\\s+", 2);
            if (parts.length != 2) continue;
            String expected = parts[0];
            String file = parts[1];
            if ("checksums.sha256".equals(file)) continue;
            String actual = sha256(readBytes(resourceDir + "/" + file));
            if (!expected.equalsIgnoreCase(actual)) {
                throw new IllegalStateException("Checksum mismatch for " + file);
            }
        }
    }

    private static JsonNode readJson(String resource) throws IOException {
        return MAPPER.readTree(readBytes(resource));
    }

    private static String readText(String resource) throws IOException {
        return new String(readBytes(resource), StandardCharsets.UTF_8);
    }

    private static byte[] readBytes(String resource) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream in = loader.getResourceAsStream(resource)) {
            if (in == null) throw new IOException("Missing resource " + resource);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            in.transferTo(out);
            return out.toByteArray();
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot calculate checksum", e);
        }
    }

    private static double sigmoid(double value) {
        if (value >= 0.0) {
            double z = Math.exp(-value);
            return 1.0 / (1.0 + z);
        }
        double z = Math.exp(value);
        return z / (1.0 + z);
    }

    private static double logit(double p) {
        double x = Math.max(1e-6, Math.min(1.0 - 1e-6, p));
        return Math.log(x / (1.0 - x));
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }
}
