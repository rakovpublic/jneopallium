/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrialfmi;

import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/** Writes controller decisions to controller_results.jsonl. */
public final class IndustrialFmiResultAggregator implements Closeable {
    private final Path file;

    public IndustrialFmiResultAggregator(Path file) throws IOException {
        this.file = file;
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        Files.writeString(file, "", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void append(List<IResult> results, long timestamp, long run) {
        if (results == null) return;
        StringBuilder sb = new StringBuilder();
        for (IResult result : results) {
            if (result == null || result.getResult() == null) continue;
            IResultSignal signal = result.getResult();
            sb.append('{');
            field(sb, "ts", timestamp).append(',');
            field(sb, "run", run).append(',');
            quoted(sb, "type", signal.getClass().getSimpleName()).append(',');
            if (signal instanceof ActuatorCommandSignal ac) {
                quoted(sb, "tag", ac.getTag()).append(',');
                field(sb, "target", ac.getTargetValue()).append(',');
                field(sb, "execute", ac.isExecute());
            } else if (signal instanceof InterlockSignal il) {
                quoted(sb, "tag", il.getInterlockId()).append(',');
                field(sb, "tripped", il.isTripped()).append(',');
                quoted(sb, "causes", String.join("|", il.getCauses()));
            } else if (signal instanceof OperatorOverrideSignal ov) {
                quoted(sb, "tag", ov.getTag()).append(',');
                field(sb, "manualValue", ov.getManualValue()).append(',');
                quoted(sb, "reason", ov.getReason());
            } else if (signal instanceof SetpointSignal sp) {
                quoted(sb, "tag", sp.getTag()).append(',');
                field(sb, "setpoint", sp.getSetpoint()).append(',');
                quoted(sb, "source", sp.getSource());
            } else {
                quoted(sb, "tag", signal.getDescription());
            }
            sb.append("}\n");
        }
        try {
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("failed to write " + file, e);
        }
    }

    @Override public void close() {}

    private static StringBuilder quoted(StringBuilder sb, String key, String value) {
        sb.append('"').append(key).append("\":");
        if (value == null) return sb.append("null");
        return sb.append('"').append(escape(value)).append('"');
    }

    private static StringBuilder field(StringBuilder sb, String key, double value) {
        return sb.append('"').append(key).append("\":").append(value);
    }

    private static StringBuilder field(StringBuilder sb, String key, long value) {
        return sb.append('"').append(key).append("\":").append(value);
    }

    private static StringBuilder field(StringBuilder sb, String key, boolean value) {
        return sb.append('"').append(key).append("\":").append(value);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
