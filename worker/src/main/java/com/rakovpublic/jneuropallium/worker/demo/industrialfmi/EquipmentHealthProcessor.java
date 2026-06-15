/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrialfmi;

import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;

import java.util.ArrayList;
import java.util.List;

/** Slow-loop health and advisory calculation for P-101. */
public final class EquipmentHealthProcessor {
    private double vibrationRms = 0.55;
    private double bearingTemperature = 36.0;
    private double pumpPowerKw = 1.0;
    private double lastRisk;
    private double lastRecommendedPumpSpeed = 45.0;

    public EquipmentHealthSignal observe(List<? extends IInputSignal> signals, long timestamp) {
        if (signals != null) {
            for (IInputSignal signal : signals) {
                if (signal instanceof MeasurementSignal m) {
                    switch (m.getTag()) {
                        case IndustrialFmiTags.VIBRATION -> vibrationRms = m.getMeasurement();
                        case IndustrialFmiTags.BEARING_TEMP -> bearingTemperature = m.getMeasurement();
                        case IndustrialFmiTags.PUMP_POWER_KW -> pumpPowerKw = m.getMeasurement();
                        default -> { }
                    }
                }
            }
        }
        lastRisk = calculateRisk(vibrationRms, bearingTemperature, pumpPowerKw);
        return new EquipmentHealthSignal("P-101", vibrationRms, bearingTemperature, pumpPowerKw, lastRisk, timestamp);
    }

    public List<IResult> advisories(EquipmentHealthSignal health, IndustrialFmiControllerConfig config) {
        if (health == null) return List.of();
        double risk = health.getRisk();
        double healthPumpBoost = risk > 0.65 ? 10.0 : risk > 0.40 ? 5.0 : 0.0;
        double energyTrim = risk < 0.25 ? -4.0 : 0.0;
        lastRecommendedPumpSpeed = clamp(45.0 + healthPumpBoost + energyTrim,
                30.0, 45.0 + config.slowLoopPumpAdjustmentLimit());
        double maintenancePriority = clamp(risk * 100.0, 0.0, 100.0);
        double energyMode = risk > 0.65 ? 0.0 : 1.0;

        List<IResult> out = new ArrayList<>();
        out.add(new IndustrialFmiResult(new SetpointSignal(
                IndustrialFmiTags.ADVISORY_PUMP_SPEED, lastRecommendedPumpSpeed, 0.0, "EquipmentHealthProcessor"), 220L));
        out.add(new IndustrialFmiResult(new SetpointSignal(
                IndustrialFmiTags.ADVISORY_MAINTENANCE_PRIORITY, maintenancePriority, 0.0, "EquipmentHealthProcessor"), 221L));
        out.add(new IndustrialFmiResult(new SetpointSignal(
                IndustrialFmiTags.ADVISORY_BEARING_RISK, risk, 0.0, "EquipmentHealthProcessor"), 222L));
        out.add(new IndustrialFmiResult(new SetpointSignal(
                IndustrialFmiTags.ADVISORY_ENERGY_MODE, energyMode, 0.0, "EquipmentHealthProcessor"), 223L));
        return out;
    }

    public static double calculateRisk(double vibrationRms, double bearingTemperature, double pumpPowerKw) {
        double vib = clamp((vibrationRms - 2.0) / 7.0, 0.0, 1.0);
        double bearing = clamp((bearingTemperature - 45.0) / 45.0, 0.0, 1.0);
        double power = clamp((pumpPowerKw - 2.0) / 8.0, 0.0, 1.0);
        return clamp(0.45 * vib + 0.35 * bearing + 0.20 * power, 0.0, 1.0);
    }

    public double lastRisk() { return lastRisk; }
    public double lastRecommendedPumpSpeed() { return lastRecommendedPumpSpeed; }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }
}
