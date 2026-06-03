package com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record DemoDefinition(
        String id,
        String packageName,
        String title,
        String safetyMode,
        int defaultTicks,
        List<String> inputSignalClasses,
        List<String> layerOutputSignalClasses,
        List<String> layerStages,
        List<Integer> layerSizes,
        String inputClassName,
        String neuronClassName,
        String resultNeuronClassName,
        String processorClassName
) {
    public boolean isResultLayer(int layerIndex) {
        return layerIndex == layerSizes.size() - 1;
    }

    public List<String> acceptedSignalClasses(int layerIndex) {
        if (layerIndex == 0) {
            return inputSignalClasses;
        }
        return List.of(layerOutputSignalClasses.get(layerIndex - 1));
    }

    public String outputSignalClass(int layerIndex) {
        return layerOutputSignalClasses.get(layerIndex);
    }

    public List<String> allSignalClasses() {
        Set<String> names = new LinkedHashSet<>();
        names.addAll(inputSignalClasses);
        names.addAll(layerOutputSignalClasses);
        return new ArrayList<>(names);
    }

    public List<String> modelClasses() {
        Set<String> names = new LinkedHashSet<>();
        names.add(inputClassName);
        names.add(neuronClassName);
        names.add(resultNeuronClassName);
        names.add(processorClassName);
        names.addAll(allSignalClasses());
        return new ArrayList<>(names);
    }
}
