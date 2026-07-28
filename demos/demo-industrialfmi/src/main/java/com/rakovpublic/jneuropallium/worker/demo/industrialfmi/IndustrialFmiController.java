/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrialfmi;

import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.MeasurementValidatorNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.OscillationIntervention;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.OscillationMonitorNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.OverrideKind;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fast-loop cascade controller and safety signal producer for the skid. */
public final class IndustrialFmiController {
    private final IndustrialFmiControllerConfig config;
    private final MeasurementValidatorNeuron validator = new MeasurementValidatorNeuron();
    private final OscillationMonitorNeuron oscillation = new OscillationMonitorNeuron();
    private double coolingIntegral;
    private double boundedSlowPumpAdjustment;
    private double lastOscillationSeverity;
    private OscillationIntervention lastIntervention = OscillationIntervention.NONE;
    private int validationRejects;

    public IndustrialFmiController(IndustrialFmiControllerConfig config) {
        this.config = config == null ? IndustrialFmiControllerConfig.defaults() : config;
        validator.setRange(IndustrialFmiTags.TEMP_PV, -20.0, 130.0);
        validator.setRange(IndustrialFmiTags.FLOW_PV, 0.0, 2.0);
        validator.setRange(IndustrialFmiTags.SUCTION_PRESSURE, 0.0, 2.0);
        validator.setMaxRateOfChange(IndustrialFmiTags.TEMP_PV, 3.0);
        validator.setMaxRateOfChange(IndustrialFmiTags.FLOW_PV, 1.0);
        oscillation.setAcfWindowTicks(24);
    }

    public List<IResult> tick(List<? extends IInputSignal> inputSignals, long timestamp) {
        Map<String, MeasurementSignal> measurements = new LinkedHashMap<>();
        List<AlarmSignal> alarms = new ArrayList<>();
        if (inputSignals != null) {
            for (IInputSignal signal : inputSignals) {
                if (signal instanceof MeasurementSignal m) {
                    MeasurementSignal checked = validator.validate(m);
                    if (checked.getQuality() != Quality.GOOD) validationRejects++;
                    measurements.put(checked.getTag(), checked);
                    if (IndustrialFmiTags.TEMP_PV.equals(checked.getTag())) oscillation.observe(checked);
                } else if (signal instanceof AlarmSignal alarm) {
                    alarms.add(alarm);
                }
            }
        }

        List<IResult> out = new ArrayList<>();
        boolean highTempTrip = booleanMeasurement(measurements, IndustrialFmiTags.HIGH_TEMP_INTERLOCK)
                || alarmActive(alarms, IndustrialFmiTags.HIGH_TEMP_INTERLOCK)
                || value(measurements, IndustrialFmiTags.TEMP_PV, config.temperatureSetpoint()) >= config.highTemperatureTrip();
        boolean lowFlowTrip = booleanMeasurement(measurements, IndustrialFmiTags.LOW_FLOW_INTERLOCK)
                || value(measurements, IndustrialFmiTags.FLOW_PV, config.minimumFlow()) < config.lowFlowTrip();
        boolean lowSuctionTrip = booleanMeasurement(measurements, IndustrialFmiTags.LOW_SUCTION_INTERLOCK)
                || value(measurements, IndustrialFmiTags.SUCTION_PRESSURE, 1.0) < config.lowSuctionTrip();

        if (highTempTrip) {
            out.add(result(new InterlockSignal(IndustrialFmiTags.LOOP_COOLING, true, List.of("HIGH_TEMPERATURE"))));
            out.add(result(new InterlockSignal(IndustrialFmiTags.LOOP_HEATER, true, List.of("HIGH_TEMPERATURE"))));
        }
        if (lowFlowTrip || lowSuctionTrip) {
            out.add(result(new InterlockSignal(IndustrialFmiTags.LOOP_PUMP, true,
                    List.of(lowFlowTrip ? "LOW_FLOW" : "LOW_SUCTION"))));
        }

        boolean manual = value(measurements, IndustrialFmiTags.OPERATOR_MANUAL_MODE, 0.0) >= 0.5;
        if (manual) {
            out.add(result(new OperatorOverrideSignal(IndustrialFmiTags.VALVE_CMD, OverrideKind.MANUAL,
                    "operator", "manual valve hold",
                    value(measurements, IndustrialFmiTags.OPERATOR_MANUAL_VALVE, 35.0))));
            out.add(result(new OperatorOverrideSignal(IndustrialFmiTags.PUMP_SPEED_SP, OverrideKind.MANUAL,
                    "operator", "manual pump hold",
                    value(measurements, IndustrialFmiTags.OPERATOR_MANUAL_PUMP, 45.0))));
        }

        double temp = value(measurements, IndustrialFmiTags.TEMP_PV, config.temperatureSetpoint());
        double sp = value(measurements, IndustrialFmiTags.TEMP_SP, config.temperatureSetpoint());
        double flow = value(measurements, IndustrialFmiTags.FLOW_PV, config.minimumFlow());
        double valvePv = value(measurements, IndustrialFmiTags.VALVE_POSITION_PV, 35.0);
        double pumpPv = value(measurements, IndustrialFmiTags.PUMP_SPEED_PV, 45.0);
        double heaterPv = value(measurements, IndustrialFmiTags.HEATER_POWER_PV, 35.0);

        lastOscillationSeverity = oscillation.severity(IndustrialFmiTags.TEMP_PV);
        lastIntervention = oscillation.intervention(IndustrialFmiTags.TEMP_PV);
        double gainScale = lastOscillationSeverity >= config.oscillationSeverityThreshold()
                ? config.oscillationGainScale()
                : 1.0;

        double error = temp - sp;
        coolingIntegral = clamp(coolingIntegral + error * config.coolingKi() * config.fastLoopSeconds(), -25.0, 25.0);
        double cooling = clamp(config.coolingBase() + gainScale * config.coolingKp() * error + coolingIntegral, 0.0, 100.0);
        double pump = clamp(config.pumpBase() + config.pumpKp() * Math.max(0.0, config.minimumFlow() - flow)
                + boundedSlowPumpAdjustment, 20.0, 100.0);
        double heater = clamp(config.heaterBase() + config.heaterKp() * (sp - temp), 0.0, 100.0);

        if (highTempTrip) {
            cooling = 100.0;
            heater = 0.0;
        }
        if (lowFlowTrip || lowSuctionTrip) {
            pump = Math.max(pump, 30.0);
        }

        out.add(result(new ActuatorCommandSignal(IndustrialFmiTags.VALVE_CMD, cooling, valvePv, true)));
        out.add(result(new ActuatorCommandSignal(IndustrialFmiTags.PUMP_SPEED_SP, pump, pumpPv, true)));
        out.add(result(new ActuatorCommandSignal(IndustrialFmiTags.HEATER_POWER_CMD, heater, heaterPv, true)));
        return out;
    }

    public void applySlowPumpRecommendation(double recommendedPumpSpeed) {
        boundedSlowPumpAdjustment = clamp(recommendedPumpSpeed - 45.0,
                -config.slowLoopPumpAdjustmentLimit(), config.slowLoopPumpAdjustmentLimit());
    }

    public int validationRejects() { return validationRejects; }
    public double lastOscillationSeverity() { return lastOscillationSeverity; }
    public OscillationIntervention lastIntervention() { return lastIntervention; }

    private static IndustrialFmiResult result(com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal signal) {
        return new IndustrialFmiResult(signal, 210L);
    }

    private static boolean alarmActive(List<AlarmSignal> alarms, String tag) {
        return alarms.stream().anyMatch(a -> tag.equals(a.getTag()) && "ALARM_ACTIVE".equals(a.getConditionCode()));
    }

    private static boolean booleanMeasurement(Map<String, MeasurementSignal> measurements, String tag) {
        MeasurementSignal m = measurements.get(tag);
        return m != null && m.getMeasurement() >= 0.5;
    }

    private static double value(Map<String, MeasurementSignal> measurements, String tag, double fallback) {
        MeasurementSignal m = measurements.get(tag);
        return m == null ? fallback : m.getMeasurement();
    }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }
}
