package com.rakovpublic.jneuropallium.worker.demo.industrialfmi.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineHealthAdvisorySignal;
import com.rakovpublic.jneuropallium.worker.util.IContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IndustrialLoopGuardianResultAggregator implements IOutputAggregator {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private long advisoryCount;
    private long faultAdvisoryCount;
    private Long firstAdvisoryTimestampMs;
    private Long firstAdvisoryRun;
    private Long firstFaultTimestampMs;
    private Long firstFaultRun;
    private Map<String, Object> firstFaultAdvice;

    @Override
    public void save(List<IResult> results, long timestamp, long run, IContext context) {
        long faultStartTimestampMs = longProperty(context, "configuration.industrial.faultStartTimestampMs", 20L);
        double anomalyThreshold = doubleProperty(context, "configuration.industrial.detection.anomaly.threshold", 0.60);
        double faultThreshold = doubleProperty(context, "configuration.industrial.detection.fault.threshold", 0.55);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("run", run);
        row.put("wallClockTimestampMs", timestamp);
        row.put("runOnceInMs", longProperty(context, "configuration.runoncein", 1L));
        row.put("faultStartTimestampMs", faultStartTimestampMs);
        List<Map<String, Object>> advices = new ArrayList<>();

        if (results != null) {
            for (IResult result : results) {
                IResultSignal signal = result.getResult();
                if (signal instanceof MachineHealthAdvisorySignal advisory) {
                    Map<String, Object> advice = adviceToMap(result.getNeuronId(), advisory);
                    advices.add(advice);
                    advisoryCount++;
                    boolean nonMonitor = advisory.getRecommendedAction() != null
                            && !"MONITOR".equals(advisory.getRecommendedAction());
                    if (nonMonitor && firstAdvisoryTimestampMs == null) {
                        firstAdvisoryTimestampMs = advisory.getTimestamp();
                        firstAdvisoryRun = run;
                    }
                    if (isFaultDetection(advisory, anomalyThreshold, faultThreshold)) {
                        faultAdvisoryCount++;
                        if (firstFaultTimestampMs == null) {
                            firstFaultTimestampMs = advisory.getTimestamp();
                            firstFaultRun = run;
                            firstFaultAdvice = advice;
                        }
                    }
                }
            }
        }

        row.put("advices", advices);
        appendJsonLine(context.getProperty("configuration.demo.output.path"), row);
        writeSummary(context, faultStartTimestampMs);
    }

    private void writeSummary(IContext context, long faultStartTimestampMs) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", firstFaultTimestampMs == null ? "NO_FAULT_DETECTED" : "PASS");
        summary.put("workflow", "Entry local -> IInitInput -> industrial layers -> IOutputAggregator");
        summary.put("runOnceInMs", longProperty(context, "configuration.runoncein", 1L));
        summary.put("assetId", context.getProperty("configuration.industrial.assetId"));
        summary.put("faultStartTimestampMs", faultStartTimestampMs);
        summary.put("firstAdvisoryTimestampMs", firstAdvisoryTimestampMs);
        summary.put("firstAdvisoryRun", firstAdvisoryRun);
        summary.put("firstAdvisoryDelayMs", firstAdvisoryTimestampMs == null
                ? null : firstAdvisoryTimestampMs - faultStartTimestampMs);
        summary.put("firstFaultDetectionTimestampMs", firstFaultTimestampMs);
        summary.put("firstFaultDetectionRun", firstFaultRun);
        summary.put("pumpWearFaultDetectionDelayMs", firstFaultTimestampMs == null
                ? null : firstFaultTimestampMs - faultStartTimestampMs);
        summary.put("advisoryCount", advisoryCount);
        summary.put("faultAdvisoryCount", faultAdvisoryCount);
        summary.put("firstFaultAdvice", firstFaultAdvice);
        writeJson(context.getProperty("configuration.demo.summary.path"), summary);
    }

    private static boolean isFaultDetection(MachineHealthAdvisorySignal advisory,
                                            double anomalyThreshold,
                                            double faultThreshold) {
        if (advisory.getAnomalyProbability() >= anomalyThreshold) {
            return true;
        }
        if (advisory.getRecommendedAction() != null && advisory.getRecommendedAction().startsWith("INSPECT")) {
            return true;
        }
        return advisory.getFaultProbabilities().values().stream().anyMatch(value -> value >= faultThreshold);
    }

    private static Map<String, Object> adviceToMap(long neuronId, MachineHealthAdvisorySignal advisory) {
        Map<String, Object> row = new LinkedHashMap<>(advisory.getResultObject());
        row.put("neuronId", neuronId);
        row.put("signalClass", advisory.getClass().getName());
        row.put("signalType", advisory.getClass().getSimpleName());
        row.put("sourceLayerId", advisory.sourceLayer);
        row.put("sourceNeuronId", advisory.sourceNeuron);
        return row;
    }

    private static long longProperty(IContext context, String name, long fallback) {
        String value = context.getProperty(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Long.parseLong(value);
    }

    private static double doubleProperty(IContext context, String name, double fallback) {
        String value = context.getProperty(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Double.parseDouble(value);
    }

    private static void appendJsonLine(String path, Map<String, Object> row) {
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("Missing industrial JSONL output path");
        }
        try {
            Path output = Path.of(path);
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(output, toJson(row) + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write industrial JSONL output to " + path, e);
        }
    }

    private static void writeJson(String path, Map<String, Object> row) {
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("Missing industrial summary output path");
        }
        try {
            Path output = Path.of(path);
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(output, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(row),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write industrial summary output to " + path, e);
        }
    }

    private static String toJson(Map<String, Object> row) {
        try {
            return MAPPER.writeValueAsString(row);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize industrial result row", e);
        }
    }
}
