/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrialfmi;

import com.rakovpublic.jneuropallium.worker.bridge.mqtt.DefaultMqttTransport;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttAdvisoryOutputAggregator;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttBridgeConfig;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttBridgeConfigLoader;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttClientService;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttEventInput;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttMetricInput;
import com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttSignalMapper;
import com.rakovpublic.jneuropallium.worker.input.opcua.OpcUaAlarmInput;
import com.rakovpublic.jneuropallium.worker.input.opcua.OpcUaMeasurementInput;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.MiloOpcUaClientService;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaBridgeConfig;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaBridgeConfigLoader;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.output.opcua.OpcUaCommandOutputAggregator;
import com.rakovpublic.jneuropallium.worker.output.opcua.OpcUaTransparencyLogOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Runs the Jneopallium side of the FMI skid demo against real OPC UA and MQTT bridges. */
public final class IndustrialFmiDemoMain {
    private IndustrialFmiDemoMain() {}

    public static void main(String[] args) throws Exception {
        Args parsed = Args.parse(args);
        long run = System.currentTimeMillis();
        IndustrialFmiScenario scenario = IndustrialFmiScenario.load(parsed.scenario);
        IndustrialFmiControllerConfig controllerConfig = IndustrialFmiControllerConfig.load(parsed.controller);
        IndustrialFmiManifestWriter.write(parsed.outputDir, scenario, run);

        OpcUaBridgeConfig opcCfg = OpcUaBridgeConfigLoader.load(parsed.opcua);
        MqttBridgeConfig mqttCfg = MqttBridgeConfigLoader.load(parsed.mqtt);
        IndustrialFmiController controller = IndustrialFmiNetworkFactory.controller(controllerConfig);
        EquipmentHealthProcessor health = IndustrialFmiNetworkFactory.equipmentHealthProcessor();

        try (MiloOpcUaClientService opcSvc = new MiloOpcUaClientService(opcCfg);
             OpcUaTransparencyLogOutput opcAudit = new OpcUaTransparencyLogOutput(opcCfg.audit(), opcSvc);
             MqttAuditOutput mqttAudit = new MqttAuditOutput(Path.of(mqttCfg.audit().localAuditFile()));
             MqttClientService mqttSvc = new MqttClientService(mqttCfg, new DefaultMqttTransport(mqttCfg),
                     new MqttSignalMapper(mqttCfg), mqttAudit);
             IndustrialFmiResultAggregator resultLog =
                     new IndustrialFmiResultAggregator(parsed.outputDir.resolve("controller_results.jsonl"))) {

            mqttSvc.start();
            MqttAdvisoryOutputAggregator mqttAgg = new MqttAdvisoryOutputAggregator(mqttSvc, mqttAudit);
            OpcUaCommandOutputAggregator opcAgg = new OpcUaCommandOutputAggregator(opcCfg, opcSvc, opcAudit);
            OpcUaMeasurementInput opcMeasurements = new OpcUaMeasurementInput("industrial-fmi-opcua",
                    null, opcSvc, IndustrialFmiInputRegistry.readBindings(opcCfg));
            OpcUaAlarmInput opcAlarms = new OpcUaAlarmInput("industrial-fmi-opcua-alarms", null, opcSvc);
            MqttMetricInput mqttMetrics = new MqttMetricInput("industrial-fmi-mqtt",
                    mqttSvc, mqttCfg.reads().stream().map(MqttBridgeConfig.ReadBindingConfig::bindingId).toList());
            MqttEventInput mqttEvents = new MqttEventInput("industrial-fmi-mqtt-events", mqttSvc);

            long tickMillis = Math.max(20L, Math.round(controllerConfig.fastLoopSeconds() * 1000.0));
            long slowEveryTicks = Math.max(1L, Math.round(controllerConfig.slowLoopSeconds() / controllerConfig.fastLoopSeconds()));
            long ticks = Math.max(1L, Math.round(scenario.durationSeconds() / controllerConfig.fastLoopSeconds()));
            IndustrialFmiDemoContext context = new IndustrialFmiDemoContext(scenario.name(), parsed.outputDir);

            for (long tick = 0; tick < ticks; tick++) {
                long ts = System.currentTimeMillis();
                List<IInputSignal> inputs = new ArrayList<>();
                inputs.addAll(opcMeasurements.readSignals());
                inputs.addAll(opcAlarms.readSignals());
                List<IResult> fastResults = controller.tick(inputs, ts);
                resultLog.append(fastResults, ts, run);
                opcAgg.save(fastResults, ts, run, context);

                if (tick % slowEveryTicks == 0) {
                    List<IInputSignal> healthInputs = new ArrayList<>(mqttMetrics.readSignals());
                    healthInputs.addAll(mqttEvents.readSignals());
                    EquipmentHealthSignal healthSignal = health.observe(healthInputs, ts);
                    List<IResult> slowResults = health.advisories(healthSignal, controllerConfig);
                    controller.applySlowPumpRecommendation(health.lastRecommendedPumpSpeed());
                    resultLog.append(slowResults, ts, run);
                    mqttAgg.save(slowResults, ts, run, context);
                }
                Thread.sleep(tickMillis);
            }
        }
    }

    private record Args(Path opcua, Path mqtt, Path controller, Path scenario, Path outputDir) {
        static Args parse(String[] args) {
            Path opcua = Path.of("scripts/demo-industrial-fmi/config/opcua.yaml");
            Path mqtt = Path.of("scripts/demo-industrial-fmi/config/mqtt.yaml");
            Path controller = Path.of("scripts/demo-industrial-fmi/config/controller.yaml");
            Path scenario = Path.of("scripts/demo-industrial-fmi/config/scenarios/normal.yaml");
            Path outputDir = Path.of("target/jneopallium-industrial-fmi/normal");
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--opcua" -> opcua = Path.of(args[++i]);
                    case "--mqtt" -> mqtt = Path.of(args[++i]);
                    case "--controller" -> controller = Path.of(args[++i]);
                    case "--scenario" -> scenario = Path.of(args[++i]);
                    case "--output-dir" -> outputDir = Path.of(args[++i]);
                    default -> throw new IllegalArgumentException("unknown argument: " + args[i]);
                }
            }
            return new Args(opcua, mqtt, controller, scenario, outputDir);
        }
    }
}
