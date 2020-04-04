package net.storages.signalstorages.db;

import net.signals.IResultSignal;
import net.signals.ISignal;
import net.storages.IInputMeta;
import net.storages.ISerializer;

import java.util.HashMap;
import java.util.List;

public class DbInputMeta implements IInputMeta {


    @Override
    public HashMap<Long, List<ISignal>> readInputs(int layerId) {
        return null;
    }

    @Override
    public List<ISignal> readInputsForNeuron(int layerId, Long neuronId) {
        return null;
    }

    @Override
    public void cleanInputs() {

    }

    @Override
    public IResultSignal getDesiredResult() {
        return null;
    }


    @Override
    public void copySignalToNextStep(int layerId, Long neuronId, ISignal signal) {

    }

    @Override
    public void nextStep() {

    }

    @Override
    public void copyInputsToNextStep() {

    }

    @Override
    public void mergeResults(HashMap signals, String path) {

    }


    @Override
    public void saveResults(HashMap signals, int layerId) {

    }
}
