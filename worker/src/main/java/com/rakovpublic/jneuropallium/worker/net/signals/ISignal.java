package com.rakovpublic.jneuropallium.worker.net.signals;

import java.io.Serializable;

/***
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */
public interface ISignal<T> extends Serializable {
    /**
     * @return value of signal
     */
    T getValue();

    /**
     * @return Signal class
     */
    Class<? extends ISignal<T>> getCurrentSignalClass();

    /**
     * @return value class
     */
    Class<T> getParamClass();

    String toJSON();

    /**
     * @return description
     */
    String getDescription();

    /**
     * @return true if processor for parent signals can be used for processing this signal
     */
    boolean canUseProcessorForParent();

    ISignal<T> prepareSignalToNextStep();

    int getSourceLayerId();

    void setSourceLayerId(int layerId);

    Long getSourceNeuronId();

    void setSourceNeuronId(Long neuronId);

    int getTimeAlive();

    boolean isFromExternalNet();

    void setFromExternalNet(boolean fromExternalNet);

    String getInputName();

    void setInputName(String inputName);

    void setCurrentInnerLoop(Integer loop);
    Integer getCurrentInnerLoop();

    void setInnerLoop(Integer loop);

    Integer getInnerLoop();
}
