package com.rakovpublic.jneuropallium.worker.net.neuron;

import java.io.Serializable;

/**
 * This class represents rule for neuron validation
 **/
public interface IRule extends Serializable {
    /**
     * @return true if neuron valid
     */
    Boolean validate(INeuron neuron);

    /**
     * @return description
     **/
    String getDescription();
}
