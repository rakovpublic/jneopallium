package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.FormationSignal;

public interface IFormationKeepingNeuron extends IModulatableNeuron {
    void setSlot(FormationSignal slot);
    FormationSignal currentSlot();
    /**
     * Returns the steering vector required to reach the assigned slot
     * given current relative position to the formation reference.
     */
    double[] steer(double[] currentRelativePosition);
}
