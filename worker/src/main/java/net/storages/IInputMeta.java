package net.storages;

import net.signals.IResultSignal;
import net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

public interface IInputMeta<K> extends IStorageMeta {

    HashMap<Long, List<ISignal>> readInputs(int layerId);

    List<ISignal> readInputsForNeuron(int layerId, Long neuronId);

    void cleanInputs();

    void saveResults(HashMap<Long, List<ISignal>> signals, int layerId);

    void mergeResults(HashMap<Long, List<ISignal>> signals, String path);

    IResultSignal getDesiredResult();

    void copySignalToNextStep(int layerId, Long neuronId, ISignal signal);

    void nextStep();

    void copyInputsToNextStep();


}
