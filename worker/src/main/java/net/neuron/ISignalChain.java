package net.neuron;

import net.signals.ISignal;

import java.io.Serializable;
import java.util.List;

/**
 * This class represents order of signal processing
 */
public interface ISignalChain extends Serializable {

    /**
     * @return list of signal classes which represents order of signals processing
     **/
    List<Class<? extends ISignal>> getProcessingChain();

    /**
     * @return description
     **/
    String getDescription();
}
