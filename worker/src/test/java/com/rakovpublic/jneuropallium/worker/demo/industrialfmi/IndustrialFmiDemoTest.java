/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrialfmi;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttBridgeConfig;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttSignalMapper;
import com.rakovpublic.jneuropallium.worker.input.opcua.OpcUaMeasurementInput;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.SafetyMode;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.MiloOpcUaClientService;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaBridgeConfig;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaNodeBinding;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;
import com.rakovpublic.jneuropallium.worker.output.opcua.OpcUaCommandOutputAggregator;
import com.rakovpublic.jneuropallium.worker.output.opcua.OpcUaTransparencyLogOutput;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class IndustrialFmiDemoTest {

    @TempDir Path tempDir;

    @Test
    void opcUaMeasurementsMapIntoIndustrialSignals() {
        OpcUaBridgeConfig cfg = opcConfig(SafetyMode.AUTONOMOUS, tempDir.resolve("audit.jsonl"));
        FakeMiloService svc = new FakeMiloService(cfg);
        svc.putLatest(IndustrialFmiTags.TEMP_PV, 71.25);
        OpcUaMeasurementInput input = new OpcUaMeasurementInput(
                "opc", null, svc, List.of(new OpcUaNodeBinding(cfg.reads().get(0))));

        List<IInputSignal> signals = input.readSignals();
        assertEquals(1, signals.size());
        MeasurementSignal m = (MeasurementSignal) signals.get(0);
        assertEquals(IndustrialFmiTags.TEMP_PV, m.getTag());
        assertEquals(71.25, m.getMeasurement(), 1e-6);
        assertEquals(Quality.GOOD, m.getQuality());
    }

    @Test
    void mqttHealthTelemetryMapsIntoMeasurementSignals() {
        MqttBridgeConfig cfg = mqttConfig(Map.of());
        MqttSignalMapper mapper = new MqttSignalMapper(cfg);
        IInputSignal signal = mapper.fromPlainJson(cfg.reads().get(0),
                "{\"value\":4.2,\"quality\":\"GOOD\",\"timestamp\":1760000000000}".getBytes(StandardCharsets.UTF_8),
                1L);
        MeasurementSignal m = (MeasurementSignal) signal;
        assertEquals(IndustrialFmiTags.VIBRATION, m.getTag());
        assertEquals(4.2, m.getMeasurement(), 1e-6);
    }

    @Test
    void equipmentHealthRiskAndBoundedAdvisoryAreCalculated() {
        double risk = EquipmentHealthProcessor.calculateRisk(8.0, 80.0, 7.0);
        assertTrue(risk > 0.5);
        EquipmentHealthProcessor processor = new EquipmentHealthProcessor();
        EquipmentHealthSignal signal = processor.observe(List.of(
                new MeasurementSignal(IndustrialFmiTags.VIBRATION, 8.0, Quality.GOOD, 1L),
                new MeasurementSignal(IndustrialFmiTags.BEARING_TEMP, 80.0, Quality.GOOD, 1L),
                new MeasurementSignal(IndustrialFmiTags.PUMP_POWER_KW, 7.0, Quality.GOOD, 1L)
        ), 1L);
        List<IResult> advisories = processor.advisories(signal, IndustrialFmiControllerConfig.defaults());
        SetpointSignal pump = (SetpointSignal) advisories.get(0).getResult();
        assertEquals(IndustrialFmiTags.ADVISORY_PUMP_SPEED, pump.getTag());
        assertTrue(pump.getSetpoint() <= 53.0, "slow-loop recommendation must be bounded");
    }

    @Test
    void hardInterlockBeatsPidAndOperatorOverride() {
        IndustrialFmiController controller = new IndustrialFmiController(IndustrialFmiControllerConfig.defaults());
        List<IResult> results = controller.tick(List.of(
                new MeasurementSignal(IndustrialFmiTags.TEMP_PV, 95.0, Quality.GOOD, 1L),
                new MeasurementSignal(IndustrialFmiTags.OPERATOR_MANUAL_MODE, 1.0, Quality.GOOD, 1L),
                new MeasurementSignal(IndustrialFmiTags.OPERATOR_MANUAL_VALVE, 5.0, Quality.GOOD, 1L)
        ), 1L);

        assertTrue(results.stream().anyMatch(r -> r.getResult() instanceof InterlockSignal il
                && IndustrialFmiTags.LOOP_COOLING.equals(il.getInterlockId())));
        assertTrue(results.stream().anyMatch(r -> r.getResult() instanceof OperatorOverrideSignal));
        ActuatorCommandSignal valve = command(results, IndustrialFmiTags.VALVE_CMD);
        ActuatorCommandSignal heater = command(results, IndustrialFmiTags.HEATER_POWER_CMD);
        assertEquals(100.0, valve.getTargetValue(), 1e-6);
        assertEquals(0.0, heater.getTargetValue(), 1e-6);
    }

    @Test
    void shadowModePreventsOpcUaWritesAndCreatesAudit() throws Exception {
        Path auditFile = tempDir.resolve("shadow-audit.jsonl");
        OpcUaBridgeConfig cfg = opcConfig(SafetyMode.SHADOW, auditFile);
        FakeMiloService svc = new FakeMiloService(cfg);
        try (OpcUaTransparencyLogOutput audit = new OpcUaTransparencyLogOutput(cfg.audit(), null)) {
            OpcUaCommandOutputAggregator agg = new OpcUaCommandOutputAggregator(cfg, svc, audit);
            agg.save(List.of(new IndustrialFmiResult(
                    new ActuatorCommandSignal(IndustrialFmiTags.VALVE_CMD, 60.0, 35.0, true), 1L)),
                    1000L, 1L, null);
            audit.flush();
        }
        assertEquals(0, svc.writeCount);
        assertTrue(Files.readString(auditFile).contains("SHADOW_MODE"));
    }

    @Test
    void mqttAutonomousModeIsRejectedByConfig() {
        assertThrows(IllegalArgumentException.class,
                () -> mqttConfig(Map.of("ADVISORY-PUMP-SPEED", BridgeSafetyMode.AUTONOMOUS)));
    }

    @Test
    void commandClampingAndRampRateAreApplied() throws Exception {
        Path auditFile = tempDir.resolve("apply-audit.jsonl");
        OpcUaBridgeConfig cfg = opcConfig(SafetyMode.AUTONOMOUS, auditFile);
        FakeMiloService svc = new FakeMiloService(cfg);
        try (OpcUaTransparencyLogOutput audit = new OpcUaTransparencyLogOutput(cfg.audit(), null)) {
            OpcUaCommandOutputAggregator agg = new OpcUaCommandOutputAggregator(cfg, svc, audit);
            agg.save(List.of(new IndustrialFmiResult(
                    new ActuatorCommandSignal(IndustrialFmiTags.VALVE_CMD, 150.0, 35.0, true), 1L)),
                    1000L, 1L, null);
            agg.save(List.of(new IndustrialFmiResult(
                    new ActuatorCommandSignal(IndustrialFmiTags.VALVE_CMD, 0.0, 35.0, true), 1L)),
                    1100L, 2L, null);
            audit.flush();
        }
        assertEquals(2, svc.writeCount);
        assertEquals(98.5, svc.lastDouble, 1e-6, "15 percent/sec for 0.1 s from clamped 100");
        String audit = Files.readString(auditFile);
        assertTrue(audit.contains("CLAMPED_HIGH"));
        assertTrue(audit.contains("RATE_LIMITED"));
    }

    @Test
    void interlockFailSafeWritesAndAudits() throws Exception {
        Path auditFile = tempDir.resolve("interlock-audit.jsonl");
        OpcUaBridgeConfig cfg = opcConfig(SafetyMode.AUTONOMOUS, auditFile);
        FakeMiloService svc = new FakeMiloService(cfg);
        try (OpcUaTransparencyLogOutput audit = new OpcUaTransparencyLogOutput(cfg.audit(), null)) {
            OpcUaCommandOutputAggregator agg = new OpcUaCommandOutputAggregator(cfg, svc, audit);
            agg.save(List.of(
                    new IndustrialFmiResult(new InterlockSignal(IndustrialFmiTags.LOOP_COOLING, true, List.of("trip")), 1L),
                    new IndustrialFmiResult(new ActuatorCommandSignal(IndustrialFmiTags.VALVE_CMD, 10.0, 35.0, true), 1L)
            ), 1000L, 1L, null);
            audit.flush();
        }
        assertEquals(1, svc.writeCount);
        assertEquals(100.0, svc.lastDouble, 1e-6);
        String audit = Files.readString(auditFile);
        assertTrue(audit.contains("INTERLOCK_TRIP"));
        assertTrue(audit.contains("INTERLOCK_HOLD"));
    }

    private static ActuatorCommandSignal command(List<IResult> results, String tag) {
        return results.stream()
                .map(IResult::getResult)
                .filter(ActuatorCommandSignal.class::isInstance)
                .map(ActuatorCommandSignal.class::cast)
                .filter(signal -> tag.equals(signal.getTag()))
                .findFirst()
                .orElseThrow();
    }

    private static OpcUaBridgeConfig opcConfig(SafetyMode mode, Path auditFile) {
        OpcUaBridgeConfig.NodeBindingConfig read = new OpcUaBridgeConfig.NodeBindingConfig(
                "TIC-101", "ns=2;s=Skid.TIC101.PV", IndustrialFmiTags.TEMP_PV,
                OpcUaBridgeConfig.NodeBindingConfig.Direction.READ, null, null, null, null);
        OpcUaBridgeConfig.NodeBindingConfig valve = new OpcUaBridgeConfig.NodeBindingConfig(
                IndustrialFmiTags.LOOP_COOLING, "ns=2;s=Skid.CV101.PositionCMD", IndustrialFmiTags.VALVE_CMD,
                OpcUaBridgeConfig.NodeBindingConfig.Direction.WRITE, 100.0, 15.0, 0.0, 100.0);
        return new OpcUaBridgeConfig(
                new OpcUaBridgeConfig.ConnectionConfig("opc.tcp://test", null, null,
                        Duration.ofSeconds(1), Duration.ofSeconds(60), 3),
                new OpcUaBridgeConfig.SecurityConfig(
                        OpcUaBridgeConfig.SecurityConfig.SecurityPolicy.NONE,
                        OpcUaBridgeConfig.SecurityConfig.MessageSecurityMode.NONE,
                        null, null, null,
                        new OpcUaBridgeConfig.SecurityConfig.Anonymous()),
                List.of(read),
                List.of(valve),
                List.of(),
                new OpcUaBridgeConfig.AuditConfig(auditFile.toString(), null, true),
                Map.of(IndustrialFmiTags.LOOP_COOLING, mode),
                Duration.ofMillis(100));
    }

    private static MqttBridgeConfig mqttConfig(Map<String, BridgeSafetyMode> modes) {
        return new MqttBridgeConfig(
                new MqttBridgeConfig.ConnectionConfig("mqtt://127.0.0.1:1883", "test", true,
                        Duration.ofSeconds(30), 100),
                new MqttBridgeConfig.SecurityConfig(MqttBridgeConfig.SecurityType.None,
                        null, null, null, null, null),
                new MqttBridgeConfig.SparkplugConfig(false, "g", "e", "advisory"),
                List.of(new MqttBridgeConfig.ReadBindingConfig("P101-VIB", null,
                        "jneopallium/demo/skid/P101/vibration", "$.value",
                        IndustrialFmiTags.VIBRATION, MqttBridgeConfig.ReadSignalKind.MEASUREMENT)),
                List.of(new MqttBridgeConfig.WriteBindingConfig("ADVISORY-PUMP-SPEED",
                        "jneopallium/demo/skid/advisory/recommended-pump-speed",
                        IndustrialFmiTags.ADVISORY_PUMP_SPEED, null, 20.0, 100.0, 1)),
                new MqttBridgeConfig.AuditConfig("target/test-mqtt-audit.jsonl", null, 1),
                modes,
                Map.of(),
                Duration.ofSeconds(1));
    }

    static final class FakeMiloService extends MiloOpcUaClientService {
        private final Map<String, DataValue> latest = new ConcurrentHashMap<>();
        int writeCount;
        double lastDouble;

        FakeMiloService(OpcUaBridgeConfig cfg) { super(cfg, null, null); }

        void putLatest(String tag, double value) {
            latest.put(tag, new DataValue(Variant.ofDouble(value), StatusCode.GOOD));
        }

        @Override public DataValue latest(String signalTag) { return latest.get(signalTag); }

        @Override
        public StatusCode writeValue(NodeId nodeId, DataValue value) {
            writeCount++;
            lastDouble = ((Number) value.getValue().getValue()).doubleValue();
            return StatusCode.GOOD;
        }
    }
}
