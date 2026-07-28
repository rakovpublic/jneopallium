package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerObservationSignal;

import java.util.List;

public interface IFlockingNeuron extends IModulatableNeuron {
    void setWeights(double separation, double alignment, double cohesion);
    double getSeparationWeight();
    double getAlignmentWeight();
    double getCohesionWeight();
    void setRadius(double r);
    double getRadius();
    /**
     * Reynolds-rules steering vector from a list of recent neighbour
     * observations.
     */
    double[] steer(List<PeerObservationSignal> neighbours);
}
