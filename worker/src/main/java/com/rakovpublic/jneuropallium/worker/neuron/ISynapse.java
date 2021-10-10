package com.rakovpublic.jneuropallium.worker.neuron;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.impl.NeuronSynapse;

import java.io.Serializable;

/***
 * Created by Rakovskyi Dmytro on 02.11.2017.
 * This class represents connection between neurons
 */
@JsonDeserialize(as = NeuronSynapse.class)
public interface ISynapse<S extends ISignal> extends Serializable {

    /**
     * @return target layer id
     **/
    int getTargetLayerId();

    /**
     * @return source layer id
     **/
    int getSourceLayerId();

    /**
     * @return target neuron id
     **/
    Long getTargetNeuronId();

    /**
     * @return source layer id
     **/
    Long getSourceNeuronId();

    /**
     *
     **/
    String toJSON();

    /**
     * @return description
     **/
    String getDescription();

    /**
     * @return weight object
     **/
    IWeight<S, ? extends ISignal> getWeight();

    void setWeight(IWeight<S, ? extends ISignal> weight);


}
