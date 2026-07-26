package com.rakovpublic.jneuropallium.worker.demo.industrialfmi.runtime;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineWaveformSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class IndustrialLoopGuardianReplayInput implements IInitInput {
    private String name = "industrial-loop-guardian-iinitinput";
    private String assetId = "pump-17";
    private int ticks = 80;
    private int cursor = 0;
    private int faultStartTick = 20;
    private int faultRiseTicks = 14;
    private long runOnceInMs = 1L;
    private long epoch = 1L;
    private int loop = 1;
    private double sampleRateHz = 4096.0;
    private double rotationalSpeedRpm = 1785.0;
    private String auditPath;

    @Override
    public List<IInputSignal> readSignals() {
        if (cursor >= ticks) {
            return Collections.emptyList();
        }
        long timestamp = Math.max(0L, cursor) * Math.max(1L, runOnceInMs);
        double severity = severity(cursor);
        List<IInputSignal> signals = new ArrayList<>();
        signals.add(signal(MachineWaveformSignal.CHANNEL_ACOUSTIC, severity, timestamp));
        signals.add(signal(MachineWaveformSignal.CHANNEL_VIBRATION, severity, timestamp));
        appendAudit(cursor, timestamp, severity);
        cursor++;
        return signals;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return new HashMap<>();
    }

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() {
        return new ProcessingFrequency(epoch, loop);
    }

    private MachineWaveformSignal signal(String channel, double severity, long timestamp) {
        MachineWaveformSignal signal = new MachineWaveformSignal(assetId, channel,
                waveform(channel, severity), sampleRateHz, rotationalSpeedRpm, timestamp);
        signal.setInputName(name);
        signal.setName(channel.toLowerCase() + "-bridge-frame");
        signal.setSourceLayerId(Integer.MIN_VALUE);
        signal.setSourceNeuronId(0L);
        return signal;
    }

    private double[] waveform(String channel, double severity) {
        int samples = 96;
        double[] result = new double[samples];
        boolean vibration = MachineWaveformSignal.CHANNEL_VIBRATION.equals(channel);
        double normalAmplitude = vibration ? 0.035 : 0.045;
        double faultAmplitude = vibration ? 0.34 : 0.22;
        double amplitude = normalAmplitude + faultAmplitude * severity;
        double phase = cursor * (vibration ? 0.19 : 0.13);
        int impulsePeriod = vibration ? 11 : 17;
        for (int i = 0; i < samples; i++) {
            double carrier = Math.sin((i + 1) * 0.31 + phase);
            double harmonic = 0.42 * Math.sin((i + 1) * 0.73 + phase * 0.5);
            double rub = 0.16 * severity * Math.sin((i + 1) * 2.40 + phase);
            double impulse = (severity > 0.0 && i % impulsePeriod == 0)
                    ? amplitude * severity * (vibration ? 3.8 : 2.5)
                    : 0.0;
            result[i] = amplitude * (carrier + harmonic + rub) + impulse;
        }
        return result;
    }

    private double severity(int tick) {
        if (tick < faultStartTick) {
            return 0.0;
        }
        return Math.min(1.0, (tick - faultStartTick + 1.0) / Math.max(1, faultRiseTicks));
    }

    public void setName(String name) {
        if (name != null && !name.isBlank()) this.name = name;
    }

    public void setAssetId(String assetId) {
        if (assetId != null && !assetId.isBlank()) this.assetId = assetId;
    }

    public void setTicks(int ticks) {
        this.ticks = Math.max(1, ticks);
    }

    public void setCursor(int cursor) {
        this.cursor = Math.max(0, cursor);
    }

    public void setFaultStartTick(int faultStartTick) {
        this.faultStartTick = Math.max(0, faultStartTick);
    }

    public void setFaultRiseTicks(int faultRiseTicks) {
        this.faultRiseTicks = Math.max(1, faultRiseTicks);
    }

    public void setRunOnceInMs(long runOnceInMs) {
        this.runOnceInMs = Math.max(1L, runOnceInMs);
    }

    public void setEpoch(long epoch) {
        this.epoch = Math.max(1L, epoch);
    }

    public void setLoop(int loop) {
        this.loop = Math.max(1, loop);
    }

    public void setSampleRateHz(double sampleRateHz) {
        this.sampleRateHz = Math.max(1.0, sampleRateHz);
    }

    public void setRotationalSpeedRpm(double rotationalSpeedRpm) {
        this.rotationalSpeedRpm = Math.max(0.0, rotationalSpeedRpm);
    }

    public void setAuditPath(String auditPath) {
        this.auditPath = auditPath;
    }

    private void appendAudit(int tick, long timestamp, double severity) {
        if (auditPath == null || auditPath.isBlank()) {
            return;
        }
        try {
            Path path = Path.of(auditPath);
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            String row = "{\"tick\":" + tick + ",\"timestamp\":" + timestamp
                    + ",\"severity\":" + severity + "}" + System.lineSeparator();
            Files.writeString(path, row, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // Input audit is diagnostic evidence only; it must not affect the runtime source.
        }
    }
}
