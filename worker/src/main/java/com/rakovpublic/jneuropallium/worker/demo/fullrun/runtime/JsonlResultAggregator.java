package com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
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

public class JsonlResultAggregator implements IOutputAggregator {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void save(List<IResult> results, long timestamp, long run, IContext context) {
        String demo = context.getProperty("configuration.demo.id");
        long effectiveTimestamp = deterministicTimestamp(timestamp, run, context);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("demo", demo);
        row.put("run", run);
        row.put("timestamp", effectiveTimestamp);
        List<Map<String, Object>> resultRows = new ArrayList<>();
        if (results != null) {
            for (IResult result : results) {
                resultRows.add(resultToMap(result));
            }
        }
        row.put("results", resultRows);
        appendJsonLine(context.getProperty("configuration.demo.output.path"), row);

        String auditPath = context.getProperty("configuration.demo.audit.path");
        if (auditPath != null && !auditPath.isBlank()) {
            Map<String, Object> audit = new LinkedHashMap<>(row);
            audit.put("audit", true);
            audit.put("localMode", context.getProperty("configuration.demo.entry.mode"));
            appendJsonLine(auditPath, audit);
        }
    }

    private static long deterministicTimestamp(long timestamp, long run, IContext context) {
        if (Boolean.parseBoolean(context.getProperty("configuration.demo.deterministicTimestamp"))) {
            String base = context.getProperty("configuration.demo.timestampBase");
            long baseValue = base == null ? 1_700_000_000_000L : Long.parseLong(base);
            return baseValue + run;
        }
        return timestamp;
    }

    private static Map<String, Object> resultToMap(IResult result) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("neuronId", result.getNeuronId());
        IResultSignal signal = result.getResult();
        if (signal == null) {
            row.put("signalType", "null");
            return row;
        }
        row.put("signalClass", signal.getClass().getName());
        row.put("signalType", signal.getClass().getSimpleName());
        row.put("sourceLayerId", signal.getSourceLayerId());
        row.put("sourceNeuronId", signal.getSourceNeuronId());
        if (signal instanceof DemoSignal demoSignal) {
            row.put("demo", demoSignal.getDemoId());
            row.put("tick", demoSignal.getTick());
            row.put("entityId", demoSignal.getEntityId());
            row.put("resultType", demoSignal.getResultType());
            row.put("value", demoSignal.getNumericValue());
            row.put("confidence", demoSignal.getConfidence());
            row.put("mode", demoSignal.getMode());
            row.put("decision", demoSignal.getDecision());
            row.put("reason", demoSignal.getReason());
            row.put("attributes", demoSignal.getAttributes());
        } else {
            row.put("resultType", signal.getDescription());
            row.put("confidence", null);
        }
        return row;
    }

    private static void appendJsonLine(String path, Map<String, Object> row) {
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("Missing JSONL output path");
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
            throw new IllegalStateException("Cannot write demo JSONL output to " + path, e);
        }
    }

    private static String toJson(Map<String, Object> row) {
        try {
            return MAPPER.writeValueAsString(row);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize demo result row", e);
        }
    }
}
