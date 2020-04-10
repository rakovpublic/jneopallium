package net.neuron;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.signals.ISignal;
import sample.SimpleSignalChain;

import java.io.Serializable;
import java.util.List;

/**
 * This class represents order of signal processing
 */

//TODO: refactor with StdConverter
@JsonDeserialize(as = SimpleSignalChain.class)
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
