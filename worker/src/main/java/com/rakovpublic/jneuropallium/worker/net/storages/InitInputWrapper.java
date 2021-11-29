package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

public class InitInputWrapper implements IInitInput {
    private IInitInput initInput;
    private Class<? extends IInitInput> clazz;

    public InitInputWrapper(IInitInput initInput) {
        this.initInput = initInput;
        clazz = initInput.getClass();
    }

    @Override
    public List<IInputSignal> readSignals() {
        return initInput.readSignals();
    }

    @Override
    public String getName() {
        return initInput.getName();
    }

    @Override
    public INeuronNetInput getNeuronNetInput() {
        return initInput.getNeuronNetInput();
    }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return initInput.getDesiredResults();
    }
}
