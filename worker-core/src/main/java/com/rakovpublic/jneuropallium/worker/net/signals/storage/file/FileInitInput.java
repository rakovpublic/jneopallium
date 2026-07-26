/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals.storage.file;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.HashMap;
import java.util.List;

//TODO: add implementation
public class FileInitInput implements IInitInput {
    private String path;

    public FileInitInput(String path) {
        this.path = path;
    }

    public FileInitInput() {
    }

    @Override
    public List<IInputSignal> readSignals() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }


    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return null;
    }

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() {
        return null;
    }
}
