package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * {@link IInitInput} that feeds the movement policy layer.
 *
 * <p>It is the network-input boundary for autonomous movement: the standalone runner (or any
 * bridge host) assembles a {@link MovementObservationSignal} each control tick from the latest
 * MAVLink {@code ProprioceptiveSignal} pose, the recognition layer's target detections, the
 * configured search area and the running reward, then calls {@link #offer(MovementObservationSignal)}.
 * The network drains it through {@link #readSignals()} exactly like every other init input — the
 * movement neurons never touch the transport directly.
 */
public final class MovementObservationInitInput implements IInitInput {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private final String name;
    private final ConcurrentLinkedQueue<MovementObservationSignal> pending = new ConcurrentLinkedQueue<>();

    public MovementObservationInitInput(String name) {
        this.name = name == null ? "uav-movement-observation-init" : name;
    }

    /** Hand the next assembled observation to the network input boundary. */
    public void offer(MovementObservationSignal observation) {
        if (observation != null) {
            observation.setInputName(name);
            pending.add(observation);
        }
    }

    @Override
    public List<IInputSignal> readSignals() {
        List<IInputSignal> signals = new ArrayList<>();
        MovementObservationSignal observation;
        while ((observation = pending.poll()) != null) {
            signals.add(observation);
        }
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
        return PROCESSING_FREQUENCY;
    }
}
