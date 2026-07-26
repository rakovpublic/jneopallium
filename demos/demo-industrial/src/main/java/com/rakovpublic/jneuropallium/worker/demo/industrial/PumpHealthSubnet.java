/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrial;

import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttEventInput;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttMetricInput;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmAggregationNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.DegradationModelNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.MaintenanceSchedulingNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.MeasurementValidatorNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.SafetyGateNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.SafetyMode;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.DegradationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MaintenanceWindowSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The demo-02 health sub-net wired exactly as drawn in
 * {@code demo-02-pump-fleet-predictive-maintenance.md}:
 * validator → per-asset degradation model → maintenance scheduling →
 * alarm aggregation → safety gate (ADVISORY).
 *
 * <p>One {@link #tick} drains MQTT measurements and events through the
 * {@link MqttMetricInput} / {@link MqttEventInput} drivers, runs them
 * through the neuron pipeline, and returns the {@link IResult} list for
 * {@code MqttAdvisoryOutputAggregator} to publish (or reject).
 *
 * <p>The sub-net never publishes itself — every advisory leaves through
 * the aggregator, which is the only thing that crosses the bridge ceiling.
 */
public final class PumpHealthSubnet {

    private static final long SCHEDULER_NEURON_ID = 140L;

    private final MeasurementValidatorNeuron validator = new MeasurementValidatorNeuron();
    private final DegradationModelNeuron degradation = new DegradationModelNeuron();
    private final MaintenanceSchedulingNeuron scheduler = new MaintenanceSchedulingNeuron();
    private final AlarmAggregationNeuron alarmAgg = new AlarmAggregationNeuron();
    private final SafetyGateNeuron gate = new SafetyGateNeuron();

    private final List<String> pumpIds;
    private final double schedulingHorizonHours;

    /** Hours of RUL last emitted for each pump — used by acceptance assertions. */
    private final Map<String, Double> lastRul = new LinkedHashMap<>();

    /** Per-asset proposal count — used to assert "scheduled before EOL" semantics. */
    private final Map<String, Integer> proposalCount = new HashMap<>();

    /**
     * Edge-triggered proposal latch per pump: stays {@code true} while the
     * asset is "above horizon"; flipping to {@code false} fires one proposal
     * (the doc's "RUL crosses the horizon" semantics). Re-arms when RUL
     * recovers above the horizon.
     */
    private final Map<String, Boolean> aboveHorizon = new HashMap<>();

    private long currentTick = 0L;

    public PumpHealthSubnet(List<String> pumpIds) {
        this.pumpIds = List.copyOf(pumpIds);
        this.schedulingHorizonHours = Demo02Config.SCHEDULING_HORIZON_HOURS;
        degradation.setWearPerUnit(Demo02Config.WEAR_PER_VIB_UNIT);
        scheduler.setTicksPerHour(Demo02Config.TICKS_PER_HOUR);
        scheduler.setMinLeadTimeTicks(Demo02Config.MIN_LEAD_TIME_TICKS);
        alarmAgg.setSuppressionWindowTicks(Demo02Config.ALARM_SUPPRESSION_TICKS);
        gate.setDefaultMode(SafetyMode.ADVISORY);

        for (String pumpId : pumpIds) {
            String vibTag = Demo02Config.vibrationTag(pumpId);
            String tempTag = Demo02Config.bearingTempTag(pumpId);
            validator.setRange(vibTag, 0.0, 20.0);            // mm/s
            validator.setRange(tempTag, 0.0, 150.0);          // °C
            validator.setMaxRateOfChange(vibTag, 5.0);        // mm/s per second
            validator.setMaxRateOfChange(tempTag, 10.0);      // °C per second
            degradation.seedAsset(pumpId, Demo02Config.INITIAL_RUL_HOURS);
            gate.setModeFor(Demo02Config.maintenanceTag(pumpId), SafetyMode.ADVISORY);
            aboveHorizon.put(pumpId, true);
        }
    }

    /* ---- accessors used by the runner / tests ---- */

    public DegradationModelNeuron degradation() { return degradation; }
    public MaintenanceSchedulingNeuron scheduler() { return scheduler; }
    public MeasurementValidatorNeuron validator() { return validator; }
    public AlarmAggregationNeuron alarmAggregator() { return alarmAgg; }
    public SafetyGateNeuron safetyGate() { return gate; }

    public double rulHours(String pumpId)  { return degradation.rulFor(pumpId); }
    public Double lastEmittedRul(String pumpId) { return lastRul.get(pumpId); }
    public int proposalsFor(String pumpId) { return proposalCount.getOrDefault(pumpId, 0); }
    public long currentTick() { return currentTick; }

    /* ---- one bridge tick ---- */

    public List<IResult> tick(List<MqttMetricInput> vibInputs,
                              List<MqttMetricInput> tempInputs,
                              MqttEventInput eventInput,
                              long ts) {
        currentTick++;
        List<IResult> results = new ArrayList<>();

        // Drain vibration measurements per pump and feed validator → degradation.
        Map<String, MeasurementSignal> vibByPump = drainByPump(vibInputs);
        Map<String, MeasurementSignal> tempByPump = drainByPump(tempInputs);

        for (String pumpId : pumpIds) {
            MeasurementSignal vib = vibByPump.get(pumpId);
            MeasurementSignal temp = tempByPump.get(pumpId);
            if (vib != null) validator.validate(vib);
            if (temp != null) validator.validate(temp);

            // Per-asset RUL update + edge-triggered proposal on horizon crossing.
            if (vib != null) {
                DegradationSignal rul = degradation.observe(pumpId, vib);
                if (rul != null) {
                    double hours = rul.getRemainingUsefulLifeHours();
                    lastRul.put(pumpId, hours);
                    boolean wasAbove = aboveHorizon.getOrDefault(pumpId, true);
                    boolean nowAbove = hours >= schedulingHorizonHours;
                    if (wasAbove && !nowAbove) {
                        MaintenanceWindowSignal mw = scheduler.schedule(
                                rul, currentTick, Demo02Config.MIN_LEAD_TIME_TICKS);
                        if (mw != null) {
                            proposalCount.merge(pumpId, 1, Integer::sum);
                            results.add(toMaintenanceSetpoint(mw));
                        }
                    }
                    aboveHorizon.put(pumpId, nowAbove);
                }
            }
        }

        // Drain events (alarms, DEVICE_OFFLINE, BRIDGE_RECONNECTED). Forwarded
        // alarms come out gated and go to the aggregator as transparency-style
        // observations rather than DCMDs — wire them into results if we ever
        // grow an AlarmAdvisoryOutput. For now the test inspects the audit
        // file and the aggregator inspects ADVISORY published topics.
        if (eventInput != null) {
            for (IInputSignal e : eventInput.readSignals()) {
                if (e instanceof AlarmSignal a) {
                    alarmAgg.observe(a); // suppression / rate-limit bookkeeping
                }
            }
        }

        return results;
    }

    /* ---- helpers ---- */

    private Map<String, MeasurementSignal> drainByPump(List<MqttMetricInput> inputs) {
        Map<String, MeasurementSignal> out = new LinkedHashMap<>();
        if (inputs == null) return out;
        for (MqttMetricInput in : inputs) {
            for (IInputSignal s : in.readSignals()) {
                if (s instanceof MeasurementSignal m) {
                    String pumpId = pumpIdFromTag(m.getTag());
                    if (pumpId != null) out.put(pumpId, m);
                }
            }
        }
        return out;
    }

    /** Recover the pump id from {@code PLANT.PUMP.Pxx.{VIB|BTEMP}}. */
    private static String pumpIdFromTag(String tag) {
        if (tag == null) return null;
        // PLANT.PUMP.P01.VIB  →  P01
        String[] parts = tag.split("\\.");
        if (parts.length < 4) return null;
        if (!"PLANT".equals(parts[0]) || !"PUMP".equals(parts[1])) return null;
        return parts[2];
    }

    private ResultWrapper toMaintenanceSetpoint(MaintenanceWindowSignal mw) {
        // hours-ahead horizon for the operator HMI/CMMS to render.
        double hoursAhead = Math.max(0.0,
                (mw.getScheduledTick() - currentTick) / (double) Demo02Config.TICKS_PER_HOUR);
        SetpointSignal sp = new SetpointSignal(
                Demo02Config.maintenanceTag(mw.getAssetId()),
                hoursAhead, 0.0, "PumpHealthSubnet");
        return new ResultWrapper(sp, SCHEDULER_NEURON_ID);
    }

}
