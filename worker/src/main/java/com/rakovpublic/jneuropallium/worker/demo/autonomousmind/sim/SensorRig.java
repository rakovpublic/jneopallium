package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.OwnerTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SensorRig {
    public record SensorSource(String signalType, String sourceId, String modality, String frequency) {
    }

    private static final List<SensorSource> SOURCES = List.of(
            new SensorSource("VisibleCameraSignal", "visible-camera-1", "VISIBLE", "fast"),
            new SensorSource("LidarPointCloudSignal", "lidar-1", "LIDAR", "fast"),
            new SensorSource("DepthCameraSignal", "depth-camera-1", "DEPTH", "medium"),
            new SensorSource("InfraredSignal", "ir-1", "IR", "fast"),
            new SensorSource("ThermalSignal", "thermal-1", "THERMAL", "medium"),
            new SensorSource("UltravioletSignal", "uv-1", "UV", "medium"),
            new SensorSource("RadiationSignal", "radiation-1", "RADIATION", "fast"),
            new SensorSource("RadioSignal", "radio-1", "RADIO", "fast"),
            new SensorSource("RadarSignal", "radar-1", "RADAR", "medium"),
            new SensorSource("SoundSignal", "microphone-1", "SOUND", "fast"),
            new SensorSource("UltrasoundSignal", "ultrasound-1", "ULTRASOUND", "medium"),
            new SensorSource("MagneticFieldSignal", "magnetometer-1", "MAGNETIC", "medium"),
            new SensorSource("ChemicalGasSignal", "gas-sensor-1", "CHEMICAL", "medium"),
            new SensorSource("PressureSignal", "pressure-1", "PRESSURE", "slow"),
            new SensorSource("TemperatureSignal", "temperature-1", "TEMPERATURE", "slow"),
            new SensorSource("HumiditySignal", "humidity-1", "HUMIDITY", "slow"),
            new SensorSource("VibrationSignal", "vibration-1", "VIBRATION", "medium"),
            new SensorSource("NetworkTelemetrySignal", "network-telemetry-1", "NETWORK", "slow"),
            new SensorSource("TextInstructionSignal", "owner-command-channel", "TEXT", "fast"),
            new SensorSource("MapSignal", "map-memory-1", "MAP", "fast"),
            new SensorSource("ClockSignal", "clock-1", "CLOCK", "fast"),
            new SensorSource("SelfDiagnosticsSignal", "diagnostics-1", "SELF_DIAGNOSTICS", "fast"),
            new SensorSource("EnergyLevelSignal", "energy-meter-1", "ENERGY", "fast"),
            new SensorSource("ChargingStateSignal", "charger-state-1", "CHARGING", "fast"));

    public List<SensorSource> sources() {
        return SOURCES;
    }

    public List<String> selectedSources(OwnerTask task, String scenarioId, int tick) {
        Set<String> selected = new LinkedHashSet<>();
        if (task != null && task.requiredSensors != null) {
            selected.addAll(task.requiredSensors);
        }
        if (selected.isEmpty()) {
            selected.addAll(List.of("VISIBLE", "LIDAR", "RADIATION", "RADIO", "SOUND"));
        }
        if ("sensor_conflict".equals(scenarioId)) {
            selected.add("DEPTH");
            selected.add("RADAR");
        }
        if ("sound_radio_investigation".equals(scenarioId)) {
            selected.add("RADIO");
            selected.add("SOUND");
        }
        if (tick % 5 == 0) {
            selected.add("SELF_DIAGNOSTICS");
        }
        return new ArrayList<>(selected);
    }

    public Map<String, Double> confidenceBySource(String scenarioId, int tick) {
        Map<String, Double> confidence = new LinkedHashMap<>();
        for (SensorSource source : SOURCES) {
            double value = 0.95;
            if ("sensor_conflict".equals(scenarioId)) {
                value = tick < 3 ? 0.82 : 0.88;
                if ("VISIBLE".equals(source.modality()) || "LIDAR".equals(source.modality())) {
                    value = tick < 3 ? 0.52 : 0.7;
                }
            }
            if ("emergency_safe_mode".equals(scenarioId) && "SELF_DIAGNOSTICS".equals(source.modality())) {
                value = 0.2;
            }
            confidence.put(source.modality().toLowerCase(Locale.ROOT), value);
        }
        return confidence;
    }
}
