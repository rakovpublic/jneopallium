/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.MiloOpcUaClientService;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaBridgeConfig;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaNodeBinding;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * In-process {@link MiloOpcUaClientService} backed by a
 * {@link ReactorPlantSimulator} instead of a live OPC UA transport.
 *
 * <p>It mirrors the pattern the worker test-suite already uses for the
 * bridge (a {@code MiloOpcUaClientService} subclass that overrides the
 * transport methods), so the <em>entire</em> production bridge code path —
 * {@code OpcUaMeasurementInput}, {@code OpcUaAlarmInput},
 * {@code OpcUaSignalMapper}, {@code OpcUaCommandOutputAggregator},
 * {@code OpcUaTransparencyLogOutput}, clamp / rate-limit / interlock /
 * override — runs unchanged against the simulated plant. The only thing
 * stubbed is the Milo socket.
 *
 * <p>For a real over-the-wire run, swap this for a plain
 * {@code new MiloOpcUaClientService(cfg)} pointed at the {@code asyncua}
 * plant in {@code src/test/python/reactor}; nothing else changes.
 */
public final class SimulatedReactorOpcUaService extends MiloOpcUaClientService {

    private final ReactorPlantSimulator plant;
    private final String tempTag;
    private final String flowTag;
    private final String valveTag;
    private final OpcUaBridgeConfig.NodeBindingConfig alarmCfg;

    private final NodeId valveNodeId;
    private final Map<String, OpcUaNodeBinding> bindingsByTag = new HashMap<>();

    private int valveWrites;
    private double lastValveWrite = Double.NaN;
    private boolean alarmLatched;   // edge-trigger the alarm queue

    public SimulatedReactorOpcUaService(OpcUaBridgeConfig cfg,
                                        ReactorPlantSimulator plant,
                                        String tempTag,
                                        String flowTag,
                                        String valveTag) {
        super(cfg, null, null);
        this.plant = Objects.requireNonNull(plant);
        this.tempTag = Objects.requireNonNull(tempTag);
        this.flowTag = Objects.requireNonNull(flowTag);
        this.valveTag = Objects.requireNonNull(valveTag);

        List<OpcUaBridgeConfig.NodeBindingConfig> all = new ArrayList<>();
        all.addAll(cfg.reads());
        all.addAll(cfg.writes());
        all.addAll(cfg.alarms());
        for (OpcUaBridgeConfig.NodeBindingConfig c : all) {
            bindingsByTag.put(c.signalTag(), new OpcUaNodeBinding(c));
        }
        this.alarmCfg = cfg.alarms().isEmpty() ? null : cfg.alarms().get(0);
        OpcUaNodeBinding valve = bindingsByTag.get(valveTag);
        this.valveNodeId = valve != null ? valve.nodeId : null;
    }

    public ReactorPlantSimulator plant() { return plant; }

    /** Steps the simulated plant forward by one bridge tick. */
    public void advance(double dtSeconds) { plant.step(dtSeconds); }

    public int valveWriteCount() { return valveWrites; }

    public double lastValveWrite() { return lastValveWrite; }

    @Override
    public DataValue latest(String signalTag) {
        double v;
        if (tempTag.equals(signalTag)) {
            v = plant.getReactorTempPV();
        } else if (flowTag.equals(signalTag)) {
            v = plant.getCoolantFlowPV();
        } else {
            return null;
        }
        return new DataValue(Variant.ofDouble(v), StatusCode.GOOD, DateTime.now());
    }

    @Override
    public StatusCode writeValue(NodeId nodeId, DataValue value) {
        Object raw = value.getValue() == null ? null : value.getValue().getValue();
        if (valveNodeId != null && valveNodeId.equals(nodeId) && raw instanceof Number n) {
            plant.setValve(n.doubleValue());
            lastValveWrite = n.doubleValue();
            valveWrites++;
        }
        // Audit-mirror writes and any other node are accepted silently.
        return StatusCode.GOOD;
    }

    @Override
    public List<AlarmEvent> drainAlarms() {
        boolean tripped = plant.isHiTempInterlock();
        List<AlarmEvent> out = new ArrayList<>();
        if (tripped && !alarmLatched && alarmCfg != null) {
            // Code >= 700 maps to AlarmPriority.URGENT in OpcUaSignalMapper.
            DataValue dv = new DataValue(new Variant(800), StatusCode.GOOD, DateTime.now());
            out.add(new AlarmEvent(bindingsByTag.get(alarmCfg.signalTag()), dv));
        }
        alarmLatched = tripped;
        return out;
    }

    @Override
    public OpcUaNodeBinding bindingBySignalTag(String tag) {
        return bindingsByTag.get(tag);
    }

    @Override
    public void close() {
        // No transport to release.
    }
}
