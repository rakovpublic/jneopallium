package com.rakovpublic.jneuropallium.worker.net.storages.signalstorages.file;

import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.INeuronNetInput;

import java.util.HashMap;
import java.util.List;

public class FileInitInput implements IInitInput {
    @Override
    public List<ISignal> readSignals() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public INeuronNetInput getNeuronNetInput() {
        return null;
    }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return null;
    }
}
